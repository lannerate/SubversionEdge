#!/usr/bin/env python
# -*- coding: utf-8 -*-
#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright 2008 Sun Microsystems, Inc. All rights reserved.
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

# archivepkgs - script for extracting IPS packages from a repo as a .tgz or .zip file

import os
import sys
import traceback
import re
import datetime
import tarfile
import zipfile
import getopt
import tempfile
import urllib

def usage(msg):
    ret = """Usage: archivepkgs [-t tgzfile | -z zipfile] [-d src-repo-dir] pkg-names...
Overview:
    Extracts packages from a repository and pack it into a .tgz or .zip file.
By default the output will be printed in .tgz format into sys.stdout, if -t or -z
option is not specified.

Options:
    -d           src-repo-dir   repository directory to read packages from. If not 
                                specified current directory(".") will be considered.
    -t | --tgz   tgzfile        .tgz file, the output file
    -z | --zip   zipfile        .zip file, the output file  
Arguments:
    pkg-names        packages to extract
    """ 
    if msg is not None:
        ret = "%s\n\n%s" % (msg, ret)

    print >> sys.stderr, ret

def hash_file_name(f):
    """Return the two-level path fragment for the given filename, which is
    assumed to be a content hash of at least 8 distinct characters."""
    return os.path.join("%s" % f[0:2], "%s" % f[2:8], "%s" % f)

def add_line_to_catalog(filename, line):
    """Add a line to a catalog file if it is not already there."""
    found = False
    if os.path.exists(filename):
        fd = file(filename, "r")
        for l in fd:
            if l == line:
                found = True
                break
        fd.close()
    if not found:
        if not os.path.exists(os.path.dirname(filename)):
            os.makedirs(os.path.dirname(filename))
        fd = file(filename, "a")
        fd.write(line)
        fd.close()
        return True
    return False

def add_line_to_updatelog(filename, line):
    """Add a line to an updatelog file if it is not already there."""
    found = False
    flds1 = line.split()
    if os.path.exists(filename):
        fd = file(filename, "r")
        for l in fd:
            flds2 = l.split()
            if flds1[3] == flds2[3]:
                found = True
                break
        fd.close()
    if not found:
        if not os.path.exists(os.path.dirname(filename)):
            os.makedirs(os.path.dirname(filename))
        fd = file(filename, "a")
        fd.write(line)
        fd.close()
        return True
    return False


def makeArchive(srcrepo, archive, pkgs):
    print "source directory:", srcrepo
    print "package list:", pkgs

    timestamp = datetime.datetime.now()
    
    copiedfiles = set()
    
    to_be_cleaned = set([])
    # copying the catalog file entry to a dist
    srccatalogdir = os.path.join(srcrepo, "catalog")
    fn = os.path.join(srccatalogdir, "catalog")
    if not os.path.exists(fn):
        print '"%s" is not a valid repository.'%(srcrepo)
        return
    catf = file(fn, "r")
    srccatalog = {}
    for line in catf:
        flds = line.split()
        srccatalog[flds[1]] = {"line" : line}

    destcatalog = tempfile.mkstemp(suffix=".tmp", prefix="catalog.", text=True)

    # read the updatelog entries from the source repo
    srcupdatelogdir = os.path.join(srcrepo, "updatelog")
    srclogfiles = [f for f in os.listdir(srcupdatelogdir)]
    updatelines = {}
    for f in srclogfiles:
        update_fn = os.path.join(srcupdatelogdir, f)
        temp_update = tempfile.mkstemp(suffix="."+f, prefix="updatelog.", text=True)
        logf = file(update_fn, "r")
        for line in logf:
            flds = line.split()
            # Update the local time in the updatelog entry to be the time of
            # the copy. The name of the destination updatelog file is changed
            # too. This is to prevent problems caused by packages published
            # to repositories running in different timezones.
            flds[1] = timestamp.isoformat()
            line = " ".join(flds) + "\n"
            updatelines[flds[3]] = { "line" : line, "file": temp_update[1]}


    srcrepo = os.path.join(srcrepo, "")
    srcfdir = os.path.join(srcrepo, "file");

    catalogupdated = False;

    for p in pkgs:
        ps = p.split('@')
        pname = urllib.quote(ps[0])
        pver = None
        if len(ps) > 1: 
            pver = urllib.quote(ps[1])
        srcpdir = os.path.join(srcrepo, "pkg", pname)

        copied = False
        
        if (not os.path.exists(srcpdir)) or p == "." or p == "..":
            print "skipping package '%s', it is not found in the source repository."% p
            continue

        # copy all versions
        for v in os.listdir(srcpdir):
            if pver and not v.startswith(pver):
                print "skipping package '%s', it is not found in the source repository."% p
                continue
            print "copying %s@%s to the archive" % (pname, v)
            
            copied = True
            # copy the manifest
            src_manifest = os.path.join(srcpdir, v)
            if isinstance(archive, zipfile.ZipFile):
                archive.write(src_manifest, src_manifest[len(srcrepo):])
            elif isinstance(archive, tarfile.TarFile):
                archive.add(src_manifest, src_manifest[len(srcrepo):])
               
            # copy each file that the manifest references
            mcontent = file(src_manifest).read()
            for l in mcontent.splitlines():
                l = l.lstrip()
                if not l or l[0] == "#":
                    continue
                type, l = l.split(" ", 1)
                filetypes = [ "file", "license" ]
                if type not in filetypes:
                    continue
                hash, l = l.split(" ", 1)
                srcpath = os.path.join(srcfdir, hash_file_name(hash))
                if srcpath not in copiedfiles:
                    copiedfiles.add(srcpath)
                    sys.stdout.write(".")
                    if isinstance(archive, zipfile.ZipFile):
                        archive.write(srcpath, srcpath[len(srcrepo):])
                    elif isinstance(archive, tarfile.TarFile):   
                        archive.add(srcpath, srcpath[len(srcrepo):])

            pkgfmri = urllib.unquote('pkg:/%s@%s' % (pname, v))
            # copy the catalog entry to the temp catalog
            try:
                c = srccatalog[pkgfmri]
                to_be_cleaned.add(destcatalog[1])
                catalogupdated |= add_line_to_catalog(destcatalog[1], c["line"])
            except KeyError:
                raise RuntimeError, "missing catalog entry for: %s" % pkgfmri

            # copy updatelog entry to the temp updatelog
            try:
                u = updatelines[pkgfmri]
                to_be_cleaned.add(u["file"])
                catalogupdated |= add_line_to_updatelog(u["file"], u["line"])
            except KeyError:
                pass

 
            print
    
    if not copied:
        archive.close()
        return 1

    # update the catalog attrs file
    cfile = open(destcatalog[1], "r")
    pkgre = re.compile('^V pkg:([^:])*:(.*)')
    npkgs = 0
    lasttm = None
    for line in cfile:
        m = pkgre.match(line)
        if m is None:
            continue
        npkgs += 1
    cfile.close()

    destattrs = tempfile.mkstemp(suffix=".tmp", prefix="attrs.", text=True)    

    afile = open(destattrs[1], "wb+")
    afile.write("S Last-Modified: %s\n" % timestamp.isoformat())
    afile.write("S prefix: CRSV\n")
    afile.write("S npkgs: %d\n" % npkgs)
    afile.close()    

    #copy the temp catalog, attrs to the archive
    if isinstance(archive, zipfile.ZipFile):
        archive.write(destcatalog[1], fn[len(srcrepo):])
        archive.write(destattrs[1], os.path.join(srccatalogdir, "attrs")[len(srcrepo):])
    elif isinstance(archive, tarfile.TarFile):   
        archive.add(destcatalog[1], fn[len(srcrepo):])
        archive.add(destattrs[1], os.path.join(srccatalogdir, "attrs")[len(srcrepo):])

    #copy the temp updatelog to the archive
    for fil in to_be_cleaned:
        if fil.find("updatelog") != -1:
            if isinstance(archive, zipfile.ZipFile):
                archive.write(fil, os.path.join(srcupdatelogdir, fil.rsplit(".", 1)[1])[len(srcrepo):])
            elif isinstance(archive, tarfile.TarFile):
                archive.add(fil, os.path.join(srcupdatelogdir, fil.rsplit(".", 1)[1])[len(srcrepo):])
        try:
            os.remove(fil)
        except OSError:
            pass

    #copy the cfg_cache to the archive
    if isinstance(archive, zipfile.ZipFile):
        archive.write(os.path.join(srcrepo, "cfg_cache"), os.path.join(srcrepo, "cfg_cache")[len(srcrepo):])
    elif isinstance(archive, tarfile.TarFile):   
        archive.add(os.path.join(srcrepo, "cfg_cache"), os.path.join(srcrepo, "cfg_cache")[len(srcrepo):])

    #close the archive    
    archive.close()

def main_func():
    dest = sys.stdout
    sys.stdout = sys.stderr
    
    try:
        opts, pargs = getopt.getopt(sys.argv[1:], "t:z:d:", ['tgz=', 'zip='])
    except getopt.GetoptError, e:
        usage("archivepkgs: illegal option -- %s" % e.opt)
        return 1
    
    tar = None
    zip = None
    src_repo_dir = "."
    for opt, arg in opts:
        if opt in ("-t", "--tgz"):
            tar = arg+".tgz"
        if opt in ("-z", "--zip"):
            zip = arg+".zip"
        if opt == "-d":
            src_repo_dir = arg
    
    if len(pargs) == 0:
        usage("invalid arguments: no package modules listed")
        return 1
    if tar and zip:
        usage("invalid arguments: either -t or -z is allowed")
        return 1
    elif (not tar) and (not zip):
        archive = tarfile.open(mode="w:gz", fileobj=dest, name="")

    if tar:
        archive = tarfile.open(tar, mode="w:gz")
    if zip:
        archive = zipfile.ZipFile(zip, mode="w")

    makeArchive(src_repo_dir, archive, pargs)
    
    print "archive creation completed."
    return 0

if __name__ == "__main__":
    try:
        ret = main_func()
    except SystemExit, e:
        raise e
    except KeyboardInterrupt:
        print "Interrupted"
        sys.exit(1)
    except:
        traceback.print_exc()
        sys.exit(99)
    sys.exit(ret)

