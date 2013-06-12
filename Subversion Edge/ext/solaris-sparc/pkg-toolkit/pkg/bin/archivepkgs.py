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
from urlparse import urlunparse



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
    # pull is the pkgrecv(1) command
    import pull

    print "source directory:", srcrepo
    print "package list:", pkgs

    urlprefix = ['http://', 'https://', 'file://']
    if not True in [srcrepo.startswith(i) for i in urlprefix]:
        # We need the replace statement because the urllib.url2pathname
        # command used in pull.py will work correctly with '/' slash in
        # windows.
        srcrepo = urlunparse(("file", os.path.abspath(srcrepo).replace('\\', '/'), '','','',''))

    destrepo = tempfile.mkdtemp()
    if not True in [destrepo.startswith(i) for i in urlprefix]:
        destrepo_url = urlunparse(("file", os.path.abspath(destrepo).replace('\\', '/'), '','','',''))

    sys.argv = [sys.argv[0], '-m', 'all-timestamps', '-s', srcrepo, '-d', destrepo_url]
    sys.argv.extend(pkgs)
    rv = pull.main_func()

    #copy the cfg_cache to the archive
    if isinstance(archive, zipfile.ZipFile):
        for root, dirs, files in os.walk(destrepo, topdown=False):
            reldir = root[len(destrepo)+1:]
            for name in files:
                archive.write(os.path.join(root, name), os.path.join(reldir, name))
    elif isinstance(archive, tarfile.TarFile):
        archive.add(destrepo, destrepo[len(destrepo):])

    #close the archive
    archive.close()
    return rv

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
            if arg.endswith(".tgz"):
                tar = arg
            else:
                tar = arg+".tgz"
        if opt in ("-z", "--zip"):
            if arg.endswith(".zip"):
                zip = arg
            else:
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

