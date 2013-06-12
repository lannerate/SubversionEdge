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

# manifest_create - script for creating manifest file for 'pkgsend include' command


import os
import os.path
import getopt
import sys

content = []
links = []
basedir = "."
owner = "root"
group = "sys"
mode = "0755"
mog_attributes = []
manifest_file = ""

def main():
    try:
        opts, pargs = getopt.getopt(sys.argv[1:], "m:o:g:f:d:", ["mode=", "owner=", "group=", "manifest=", "basedir="])
    except getopt.GetoptError, e:
        usage("makemanifest: illegal option -- %s" % e.opt)
        return 1
    global mode
    global owner
    global group
    global basedir
    global mog_attributes
    global manifest_file

    for opt, arg in opts:
        if opt in ("-m", "--mode"):
            mode = arg
            #mog_attributes.append(" mode ="+mode)

        if opt in ("-o", "--owner"):
            owner = arg
            #mog_attributes.append(" owner ="+owner)

        if opt in ("-g", "--group"):
            group = arg
            #mog_attributes.append(" group ="+group)

        if opt in ("-f", "--manifest"):
            manifest_file = arg

        if opt in ("-d", "--basedir"):
            basedir = arg

    # The following line needs to be changed once the owner and group attributes becomes
    # optional in the File and Directory action.
    mog_attributes = [" mode="+mode+" owner="+owner+" group="+group]

    if len(pargs) == 0:
        usage("invalid arguments: no directory mentioned for manifest creation.")
    else:
        start_parsing(pargs)

def usage(msg):
    ret = """Usage: makemanifest [-m | --mode][-o | --owner][-g | --group][-f | --manifest][-d | --basedir] includedir ...
Options:
    -m | --mode   mode for file and dir action, default value= "0755"
    -o | --owner  owner for file and dir action, default value= "root"
    -g | --group  group for file and dir action, default value= "sys"
    -f | --manifest the output manifest file name, by default output goes to standard output.
    -d | --basedir basedir from which files should be included, default value= "."
    """
    if msg is not None:
        ret = "%s\n\n%s" % (msg, ret)

    print >> sys.stderr, ret

def start_parsing(include_dirs):
    for include_dir in include_dirs:
        absolute_path = os.path.join(basedir, include_dir)
        adddirs(basedir, [include_dir,])
        for root, dirs, files in os.walk(absolute_path):
            adddirs(root, dirs)
            addfiles(root, files)
        addlinks()
        
    if len(manifest_file) != 0 and manifest_file != "stdout":
        abs_path = os.path.abspath(manifest_file)
        dir = os.path.dirname(abs_path)
        if not os.path.exists(dir):
            os.makedirs(dir)
        manifest = file(abs_path, "w")
        manifest.write("\n".join(content))
        manifest.close()
    else:
        print "\n".join(content)

def adddirs(root, dirs):
    global content
    path = getpath(root)
    for dir in dirs:
        dir_path = os.path.join(root,str(dir))
        rel_path = os.path.join(path,str(dir))
        if os.path.islink(dir_path):
            links.append("link path="+rel_path+" target="+os.readlink(dir_path))
        else:
            content.append("dir "+",".join(mog_attributes)+" path="+rel_path)

def getpath(root):
    path = root.split(basedir)[1]

    if path.startswith(os.sep):
        path = path.lstrip(os.sep)

    return path

def addfiles(root, files):
    global content
    path = getpath(root)
    for file in files:
        file_path = os.path.join(root,str(file))
        rel_path = os.path.join(path,str(file))
        if os.path.islink(file_path):
            links.append("link path="+rel_path+" target="+os.readlink(file_path))
        else:
            content.append("file "+rel_path+" "+",".join(mog_attributes)+" path="+rel_path)

def addlinks():
    for link in links:
        content.append(link)

if __name__ == '__main__':
        main()
