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

# tarpkgs - script for extracting IPS packages from a repo as a .tgz file

import os
import sys
import traceback
import tarfile
from archivepkgs import makeArchive

def usage(msg):
    ret = """Usage: %s src-repo-dir pkg-names...
Overview:
    DEPRECATED: Please use archivepkgs instead.
    
    Extracts packages from a repository and pack it into a .tgz file.

Options:
    src-repo-dir     repository directory to read packages from
    pkg-names        packages to extract
    """ % (__file__,)
    if msg is not None:
        ret = "%s\n\n%s" % (msg, ret)

    print >> sys.stderr, ret

def main_func():
    dest = sys.stdout
    sys.stdout = sys.stderr
    
    if len(sys.argv)<3:
        usage("invalid arguments: no package names listed")
        return 1

    print "tarpkgs is DEPRECATED. Please use archivepkgs instead."
    tar = tarfile.open(mode="w:gz", fileobj=dest, name="")
    makeArchive(sys.argv[1], tar, sys.argv[2:])
    tar.close()
    
    print "copy complete."
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

