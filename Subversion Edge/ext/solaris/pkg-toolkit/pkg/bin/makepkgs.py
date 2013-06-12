#!/usr/bin/env python
# -*- coding: utf-8 -*-
#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright 2009 Sun Microsystems, Inc. All rights reserved.
#
# The contents of this file are subject to the terms of either the GNU
# General Public License Version 2 only ("GPL") or the Common Development
# and Distribution License("CDDL") (collectively, the "License").  You
# may not use this file except in compliance with the License. You can obtain
# a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
# or updatetool/LICENSE.txt.  See the License for the specific
# language governing permissions and limitations under the License.
#
# When distributing the software, include this License Header Notice in each
# file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
# Sun designates this particular file as subject to the "Classpath" exception
# as provided by Sun in the GPL Version 2 section of the License file that
# accompanied this code.  If applicable, add the following below the License
# Header, with the fields enclosed by brackets [] replaced by your own
# identifying information: "Portions Copyrighted [year]
# [name of copyright owner]"
#
# Contributor(s):
#
# If you wish your version of this file to be governed by only the CDDL or
# only the GPL Version 2, indicate your decision by adding "[Contributor]
# elects to include this software in this distribution under the [CDDL or GPL
# Version 2] license."  If you don't indicate a single choice of license, a
# recipient has the option to distribute your version of this file under
# either the CDDL, the GPL Version 2 or to extend the choice of license to
# its licensees as provided above.  However, if you add GPL Version 2 code
# and therefore, elected the GPL Version 2 license, then the option applies
# only if the new code is made subject to such option by the copyright
# holder.
#

# makepkgs - script for creating Update Center IPS packages

import builder
import os
import sys
import imp
import traceback
import getopt
import gettext
import urllib2
import httplib
import errno
import depotthread
import pkg.bundle
import pkg.publish.transaction as trans
import tarfile
import time
import zipfile
import os.path
from pkg.misc import versioned_urlopen, get_data_digest
from archivepkgs import makeArchive

seenfiles = []
excludefiles = []
excludedirs = []
pkg_attributes = ["pkg.icon.24px",]

def addfile(f, tr, prefix, attributes, opsys):

    global excludefiles, default_file_mode, default_file_owner, default_file_group

    mode, owner, group = default_file_mode, default_file_owner, default_file_group

    if os.path.normpath(f) in excludefiles:
        # skip this file because it's in exclude list
        print "skipping file %s because it is in exclude list" % (f)
        return

    if f in seenfiles:
        # skip this file because we've already inserted it
        return

    seenfiles.append(f)

    if attributes and "file" in attributes:
        pathname = attributes["file"]
    else:
        pathname = os.path.join(prefix, f)

    # If file is a symlink then use symlink action
    if os.path.islink(pathname):
        addlink(f, tr, prefix)
        return

    # If a file is exeuctable, default to execute permission
    if os.name == "nt":
        if os.access(pathname, os.X_OK):
            mode = "0755"
    elif os.path.exists(pathname):
       mode = oct(os.stat(pathname).st_mode & 0777)

    if (attributes != None):
        if attributes.has_key("mode"): mode = attributes["mode"]
        if attributes.has_key("owner"): owner = attributes["owner"]
        if attributes.has_key("group"): group = attributes["group"]
        if not is_valid_for_os(attributes, opsys):
            # skip this file because it is not for this os
            return

    l = ['mode=' + mode, 'owner=' + owner, 'group=' + group, 'path=' + f]

    file_parts = f.rsplit(".", 1)
    if (len(file_parts) == 2) and (file_parts[1] == "py"):
        l.append('timestamp='+time.strftime("%Y%m%dT%H%M%SZ",
                    time.gmtime(os.path.getmtime(os.path.abspath(pathname)))))

    l += attributes_to_list(attributes,['mode','owner','group','os'])

    action = pkg.actions.fromlist("file", l)
    action.data = lambda : open(pathname, "rb")
    fs = os.lstat(pathname)
    action.attrs["pkg.size"] = str(fs.st_size)
    tr.add(action)


def adddir(d, tr, prefix, attributes, opsys):

    global excludedirs, default_dir_mode, default_dir_owner, default_dir_group

    mode, owner, group = default_dir_mode, default_dir_owner, default_dir_group

    if os.path.normpath(d) in excludedirs:
        # skip this dir because it's in exclude list
        print "skipping dir %s because it is in exclude list" % (d)
        return

    if (attributes != None):
        if attributes.has_key("mode"): mode = attributes["mode"]
        if attributes.has_key("owner"): owner = attributes["owner"]
        if attributes.has_key("group"): group = attributes["group"]
        if not is_valid_for_os(attributes, opsys):
            # skip this file because it is not for this os
            return

    # If directory is a symlink then use symlink action
    if os.path.islink(os.path.join(prefix, d)):
       addlink(d, tr, prefix)
       return

    l = ['mode=' + mode, 'owner=' + owner, 'group=' + group, 'path=' + d]
    l += attributes_to_list(attributes,['mode','owner','group','os'])
    action = pkg.actions.fromlist("dir", l)
    tr.add(action)

def addlink(path, tr, prefix):

    target_path = os.readlink(os.path.join(prefix, path))

    action = pkg.actions.fromlist("link", ['path=' + path, \
                                           'target=' + target_path])
    tr.add(action)

def addlicense(f, tr, prefix, attributes, opsys):

    pathname = os.path.join(prefix, f)

    if not is_valid_for_os(attributes, opsys):
        # skip this file because it is not for this os
        return

    l = attributes_to_list(attributes,['os'])
    action = pkg.actions.fromlist("license", l)
    action.data = lambda : open(os.path.join(prefix, f), "rb")
    fs = os.lstat(os.path.join(prefix, f))
    action.attrs["pkg.size"] = str(fs.st_size)
    tr.add(action)

# parse attributes into a list in preparation for pkgsend execution
# by ignoring attributes that are handled otherwise.
def attributes_to_list(attributes,reserved_properties):
    l = []
    if attributes != None:
        for k,v in attributes.iteritems():
            if not k in reserved_properties:
                l.append(k+'='+v)
    return l

def addmanifest(l, tr, basedir):
    # This code comes from the function trans_include in publish.py

        for filename in l:
                # If we are passed a relative path that we can't open, try
                # prepending with the basdir
                if not os.path.isabs(filename) and not os.path.exists(filename):
                    if os.path.exists(os.path.join(basedir, filename)):
                        filename = os.path.join(basedir, filename)
                print "Include manifest: %s" % filename,
                f = file(filename)
                for line in f:
                        line = line.strip() #
                        if not line or line[0] == '#':
                                continue
                        args = line.split()
                        if args[0] in ("file", "license"):
                                try:
                                        # ignore local pathname
                                        line = line.replace(args[1], "NOHASH", 1)
                                        action = pkg.actions.fromstr(line)
                                except ValueError, e:
                                        print >> sys.stderr, e[0]
                                        sys.exit(1)

                                if basedir:
                                        fullpath = args[1].lstrip(os.path.sep)
                                        fullpath = os.path.join(basedir,
                                            fullpath)
                                else:
                                        fullpath = args[1]

                                def opener():
                                        return open(fullpath, "rb")
                                action.data = opener
                                fs = os.lstat(fullpath)
                                action.attrs["pkg.size"] = str(fs.st_size)
                        else:
                                try:
                                        action = pkg.actions.fromstr(line)
                                except ValueError, e:
                                        print >> sys.stderr, e[0]
                                        sys.exit(1)

                        # cleanup any leading / in path to prevent problems
                        if "path" in action.attrs:
                                np = action.attrs["path"].lstrip(os.path.sep)
                                action.attrs["path"] = np

                        tr.add(action)
                        sys.stdout.write(".")
                        sys.stdout.flush()
                print


def is_valid_for_os(attributes, opsys):
    '''
    Check if the entry is valid for the os we are building the package for
    '''

    # No "os" attribute, then valid for any OS
    if not attributes.has_key("os"):
        return True

    os_value = attributes["os"]

    # os value could be a list or a single item
    if isinstance(os_value, list):
        # If any os name in list matches then valid
        for os_str in os_value:
            if os_str in opsys:
                return True
        return False
    else:
        return os_value in opsys

def ping_repo(url):
    try:
        c, v = versioned_urlopen(url, "catalog", [0])
    except urllib2.HTTPError, e:
        # Server returns NOT_MODIFIED if catalog is up
        # to date
        if e.code == httplib.NOT_MODIFIED:
            return True
        else:
            print >> sys.stderr, "unable to access repository catalog:", e
            return False
    return True


def sortedDictKeys(adict):
    keys = adict.keys()
    keys.sort(reverse=True)
    return keys


def resetDefaults():
    global default_file_mode, default_file_owner, default_file_group, \
           default_dir_mode, default_dir_owner, default_dir_group
    # defaults for file action
    default_file_mode = "0644"
    default_file_owner = "root"
    default_file_group = "sys"
    # defaults for dir action
    default_dir_mode = "0755"
    default_dir_owner = "root"
    default_dir_group = "sys"


def setDefaults(defaults):
    global default_file_mode, default_file_owner, default_file_group, \
           default_dir_mode, default_dir_owner, default_dir_group
    for action, action_att in defaults.iteritems():
        if action == "file":
            for an, av in action_att.iteritems():
                if an == "group":
                    default_file_group = av
                if an == "owner":
                    default_file_owner = av
                if an == "mode":
                    default_file_mode = av
        if action == "dir":
            for an, av in action_att.iteritems():
                if an == "group":
                    default_dir_group = av
                if an == "owner":
                    default_dir_owner = av
                if an == "mode":
                    default_dir_mode = av


def usage(msg):
    ret = """Usage: makepkgs {-d repodir | -s repourl} [-o sys] -b basedir [--manifest-fmri=name@version --manifest-file=manifest] [--manifest-base=basedir] proto_files...
Options:
    -s repourl   repository URL to publish to , it can be file:// or http://
    -t tgzfile   a .tgz file name to put a repository fragment into
    -z zipfile   a .zip file name to put a repository fragment into
    -b basedir   base directory containing content, default value= "."
    -o sys       operating system identifier
    -v version   a version identifier to append to package version
    -D key=value pass property value to scripts. Accessible as builder.props['key']

    --manifest-fmri    package fmri (name@version) to use for manifest
    --manifest-file    manifest file to create the package fmri
                       (multiple --manifest-file can be provided)
    --manifest-base    base directory for the source files of a manifest
    """
    if msg is not None:
        ret = "%s\n\n%s" % (msg, ret)

    print >> sys.stderr, ret

def main_func():
    # XXX /usr/lib/locale is OpenSolaris-specific.
    gettext.install("makepkgs", "/usr/lib/locale")

    try:
        opts, pargs = getopt.getopt(sys.argv[1:], "b:s:t:z:o:v:D:d:p:", ['manifest-fmri=','manifest-file=','manifest-base='])
    except getopt.GetoptError, e:
        usage("makepkgs: illegal option -- %s" % e.opt)
        return 1

    global excludefiles, excludedirs

    repo = None
    repo_port = None
    depoturl = None
    tgzfilename = None
    zipfilename = None
    refresh_index = False
    base = "."
    opsys = [ ]
    version = ""
    manifest_base = None
    manifest_fmri = None
    manifest_files = [ ]
    builder.props = dict()

    for opt, arg in opts:
        if opt == "-b":
            base = arg
        if opt == "-s":
            repo = arg
            if not(repo.startswith("http://") or repo.startswith("file://")):
                usage("invalid arguments: -s argument must start with file:// or http://")
                return 1
            if repo.startswith("http://") :
                depoturl = repo
                repo = None
            elif repo.startswith("file://"):
                repo = repo.replace("file://", "")
        if opt == "-t":
            tgzfilename = arg;
        if opt == "-z":
            zipfilename = arg;
        if opt == "-o":
            # We support multiple -o options so you can specify things like
            # -o unix -o darwin-universal
            opsys.append(arg)
        if opt == "-v":
            version = arg
        if opt == "-D":
            idx = arg.find('=')
            if idx<0:
                builder.props[arg]='true'
            else:
                builder.props[arg[:idx]]=arg[idx+1:]
        if opt == "-d":
            repo = arg
        if opt == "-p":
            # This is a hidden option to set the port of the repo
            repo_port = int(arg)
        if opt == "--manifest-base":
            # This option sets the base directory for the source files of a manifest
            # Equivalent to -d in "pkgsend include [-d basedir] manifest"
            manifest_base = arg
        if opt == "--manifest-fmri":
            # This option will create a pkg without the need of a proto file
            manifest_fmri = arg
        if opt == "--manifest-file":
            # Manifest file to use with manifest_fmri
            manifest_files.append(arg)

    if (repo != None and depoturl != None) or (repo == None and depoturl == None):
        usage("invalid arguments: exactly one of -d or -s must be specified")
        return 1

    if (len(pargs) == 0) and (manifest_fmri == None):
        usage("invalid arguments: no package modules listed")
        return 1

    seendirs = []
    pkgs = []
    pkgnames = []
    for pm in pargs:
        try:
            if os.path.exists(pm):
                module_name = pm.replace('.', '_')
                module = imp.load_source(module_name, pm)
            else:
                module = __import__(pm)

            pkgs.append(module.pkg)
            pkgnames.append(module.pkg['name'])
        except ImportError, e:
            print "\nError occured while trying to import the proto file \"",pm,"\""
            sys.exit(1)
        except Exception, e:
            print "\nInvalid proto file \"",pm,"\": %s" % (e) 
            sys.exit(1)
    if manifest_fmri:
        # Create an in-memory proto
        name, version = manifest_fmri.split("@")
        if len(manifest_files) == 0:
            usage("At least one --manifest-file must be provided with --manifest-fmri")
            return 1
        else:
            mempkg = {
                "name"          : name,
                "version"       : version,
                "manifestfiles" : manifest_files
            }
            pkgs.append(mempkg)
            pkgnames.append(mempkg['name'])

    if repo != None:
        if repo_port != None:
            dc = depotthread.DepotThread(repo=repo, port=repo_port)
        else:
            dc = depotthread.DepotThread(repo=repo)
        print "args:", dc.get_args()
        print "starting depot..."
        try:
            dc.start()
            dc.waitforup()
        except depotthread.DepotStateException, e:
            raise
            print "unable to start depot."
            sys.exit(1)
        print "depot started."
        depoturl = dc.get_depot_url()

    # check to see if the server is up
    if depoturl != None and not ping_repo(depoturl):
        sys.exit(1)

    rv = 0
    try:
        print "base directory:", base
        print "package list:", pkgnames

        for p in pkgs:

            # If "version" is a dictionary then check for the "os" attribute
            # If there is a version entry with an "os" attribute that matches
            # our opsys, then we want to use that version. Otherwise we fallback
            # to the version without an "os" attribute.
            # If "version" is not a dictionary then assume it is a simple string
            if  isinstance(p["version"], dict):
                for ver, attributes in p["version"].iteritems():
                    if attributes.has_key("os"):
                        if is_valid_for_os(attributes, opsys):
                            # We match the os. Use it
                            pversion = ver
                            break
                        else:
                            continue
                    else:
                        # Version entry not tagged with OS. Be ready to use it
                        pversion = ver
            else:
                pversion = p["version"]

            pname = ("%s@%s" % (p["name"], pversion)).replace("@{version}", version)
            t = trans.Transaction(depoturl, pkg_name=pname)
            id = t.open()
            print "open of %s: id=%s" % (pname, id)

            resetDefaults()
            if p.has_key("defaults"):
                setDefaults(p["defaults"])

            if p.has_key("excludefiles"):
                excludefiles = p["excludefiles"]
            else:
                excludefiles = [ ]

            print "excludefiles = %s" % (excludefiles)

            #Normalize the files in excludefiles
            excludefiles = map(os.path.normpath, excludefiles)

            if p.has_key("excludedirs"):
                excludedirs = p["excludedirs"]
            else:
                excludedirs = [ ]

            print "excludedirs = %s" % (excludedirs)

            #Normalize the dir in excludedirs
            excludedirs = map(os.path.normpath, excludedirs)
            for d in excludedirs:
                #Include the files and sub-dir in excludedir
                #to excludefiles and excludedir
                for root, dirs, files in os.walk(os.path.join(base, d)):
                    reldir = root[len(base)+1:]
                    for name in files:
                        excludefiles.append(os.path.normpath(os.path.join(reldir, name)))
                    for name in dirs:
                        excludedirs.append(os.path.normpath(os.path.join(reldir, name)))

            if p.has_key("attributes"):
                for an, av in p["attributes"].iteritems():
                    al = ['name=' + an, 'value=' + av]
                    if (an == pkg_attributes[0]):
                        hash, cdata = get_data_digest(os.path.join(base, av))
                        al = ['name=' + an, 'value=' + hash]
                    action = pkg.actions.fromlist("set", al)
                    t.add(action)
                    if (an == pkg_attributes[0]):
                        addfile(av, t, base, None, opsys)

            if p.has_key("depends"):
                for fmri, attributes in p["depends"].iteritems():
                    dl = ['type=' + attributes["type"], 'fmri=' + fmri]
                    action = pkg.actions.fromlist("depend", dl)
                    t.add(action)

            if p.has_key("files"):
                for path, attributes in p["files"].iteritems():
                    addfile(path, t, base, attributes, opsys)

            if p.has_key("dirs"):
                for path, attributes in p["dirs"].iteritems():
                    adddir(path, t, base, attributes, opsys)
                    seendirs.append(path)

            if p.has_key("licenses"):
                for path, attributes in p["licenses"].iteritems():
                    addlicense(path, t, base, attributes, opsys)


            if p.has_key("dirtrees"):
                # dirtrees used to be specified as a list. It is now a dict
                # like all the other sections.
                # Support a list for backwards compatibility with old proto files
                l = p["dirtrees"]
                entry = { }
                if  isinstance(l, list):
                    for d in l:
                        entry[d] = {}
                else:
                    entry = l

                for key in sortedDictKeys(entry):
                    d, attributes = key, entry[key]
                    if not is_valid_for_os(attributes, opsys):
                        # skip this dirtree because it is not for this os
                        continue
                    if d not in seendirs:
                        adddir(d, t, base, attributes, opsys)
                        seendirs.append(d)
                    for root, dirs, files in os.walk(os.path.join(base, d)):
                        reldir = root[len(base)+1:]
                        for name in files:
                            f = os.path.join(reldir, name)
                            addfile(f, t, base, attributes, opsys)
                        for name in dirs:
                            f = os.path.join(reldir, name)
                            # Only add directory if we haven't added it before
                            if f not in seendirs:
                                adddir(f, t, base, attributes, opsys)
                                seendirs.append(f)

            if p.has_key("manifestfiles"):
            	# The manifest file has the syntax used by pkgsend include
            	l = p["manifestfiles"]
                if not isinstance(l, list):
                    # New syntax supports a dict that can be tagged by os
                    l = [ ]
                    for manifest, attributes in p["manifestfiles"].iteritems():
                        if not is_valid_for_os(attributes, opsys):
                            # skip this manifest because it's not for this os
                            continue
                        else:
                            l.append(manifest)
                addmanifest(l, t, manifest_base)

            state, fmri = t.close(False)
            print "close: state=%s, fmri=%s" % (state, fmri)
    except trans.TransactionOperationError, e:
        print >> sys.stderr, "TRANSACTION ERROR:", e
        rv = 1
    except IOError, e:
        print "\n",e
        rv = 1
    except OSError, e:
        if e.errno == errno.ENOENT:
            print "\n",e.strerror+" : "+e.filename
        else:
            print "\n",e
        rv = 1
    except RuntimeError, e:
        print >> sys.stderr, "ERROR:", e
        rv = 1

    if repo != None:
        # ping the depot to make sure it has completed its last transaction
        dc.waitforup()
        print "stopping depot."
        dc.stop()

    if rv > 0:
        return rv

    if tgzfilename != None:
        if repo == None:
            print >> sys.stderr, "Archive creation skipped because -t option requires -s file://"
            sys.exit(1)
        makeArchive(repo,tarfile.open(tgzfilename,"w:gz"),pkgnames)

    if zipfilename != None:
        if repo == None:
            print >> sys.stderr, "Archive creation skipped because -z option requires -s file://"
            sys.exit(1)
        makeArchive(repo,zipfile.ZipFile(zipfilename,"w"),pkgnames)

    return 0

if __name__ == "__main__":
    try:
        ret = main_func()
    except OSError, e:
        print "\n\n", e
        sys.exit(1)
    except SystemExit, e:
        raise e
    except KeyboardInterrupt:
        print "Interrupted"
        sys.exit(1)
    except SyntaxError, e:
        print "\nSyntax Error:", e
        sys.exit(1)
    except urllib2.URLError, e:
        if hasattr(e, 'reason'):
            print 'Failed to reach the repository server.'
            print 'Reason: ', e.reason
        elif hasattr(e, 'code'):
            print 'The repository server couldn\'t fulfill the request.'
            print 'Error code: ', e.code
        sys.exit(1)
    except:
        traceback.print_exc()
        sys.exit(99)
    sys.exit(ret)
