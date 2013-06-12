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

# copypkgs - script for copying IPS packages from one repo to another (file system based)

import os
import os.path
from urlparse import urlunparse
import sys
import traceback
import getopt

# pull is the pkgrecv(1) command
import pull

def usage(msg):
    ret = """Usage: copypkgs -s src-repo -d dest-repo pkgs...
Options:
    -s src-repo    repository URL or directory to copy packages from
    -d dest-repo   repository URL or directory to copy package to
    """ 
    if msg:
        ret = "%s\n\n%s" % (msg, ret)

    print >> sys.stderr, ret


def main_func():
    try:
        opts, pargs = getopt.getopt(sys.argv[1:], "s:d:", ['force',])
    except getopt.GetoptError, e:
        usage("copypkgs: illegal option -- %s" % e.opt)
        return 1

    srcrepo = None
    destrepo = None
    for opt, arg in opts:
        if opt == "-s":
            srcrepo = arg
        if opt == "-d":
            destrepo = arg
        if opt == "--force":
            print >> sys.stderr, "copypkgs: --force option is deprecated (ignored)"

    if srcrepo == None or destrepo == None:
        usage("invalid arguments: both -s and -d must be specified")
        return 1

    if len(pargs) == 0:
        usage("invalid arguments: no package names listed")
        return 1

    pkgs = pargs

    print "source repository:", srcrepo
    print "destination repository:", destrepo
    print "package list:", pkgs

    urlprefix = ['http://', 'https://', 'file://']
    if not True in [srcrepo.startswith(i) for i in urlprefix]:
        # We need the replace statement because the urllib.url2pathname
        # command used in pull.py will wirk correctly with '/' slash in
        # windows.
        srcrepo = urlunparse(("file", os.path.abspath(srcrepo).replace('\\', '/'), '','','',''))

    if not True in [destrepo.startswith(i) for i in urlprefix]:
        destrepo = urlunparse(("file", os.path.abspath(destrepo).replace('\\', '/'), '','','',''))

    sys.argv = [sys.argv[0], '-m', 'all-timestamps', '-s', srcrepo, '-d', destrepo]
    sys.argv.extend(pkgs)
    rv = pull.main_func()
    return rv

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

