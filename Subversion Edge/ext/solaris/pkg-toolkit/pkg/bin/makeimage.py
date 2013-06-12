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

# makeimage - script for creating a pre-install image based on IPS packages

import sys
import traceback
import getopt
import gettext
import shutil
import urllib2
import depotthread
import os.path
import pkg.client.image as image
import pkg.client.progress as progress
from pkg.misc import port_available
from pkg.client.api_errors import PlanCreationException, CatalogRefreshException, \
    InvalidDepotResponseException, InventoryException

DEFAULT_REPO = "repo"
DEFAULT_IMAGE_DIR = "image"
DEFAULT_PORT = 10001
DEFAULT_AUTHORITY = "example.org"
DEFAULT_TITLE = "Change Me: Image Title"
DEFAULT_DESCRIPTION = "Change Me: Image Description"
dc = None

def usage(msg=None):
    if msg is not None:
        print >> sys.stderr, msg
    print >> sys.stderr, _("""
Usage: makeimage [options] pkg1 [pkg2 ...]

Where options are:
  -s repourl repository directory to start the IPS server if file:// 
             is specified. If http:// is specified it will be used as
             the repository server.
             (Default: '%s')
  -i dir     directory to create the image in
             (Default: '%s')
  -a authority_name
             default authority name to use for the image
             (Default: '%s')
  -t title   title of the image
             (Default: '%s')
  -x description
             description of the image
             (Default: '%s')
""") % (DEFAULT_REPO, DEFAULT_IMAGE_DIR, DEFAULT_AUTHORITY, DEFAULT_TITLE, DEFAULT_DESCRIPTION)

def main_func():
    gettext.install("makeimage")
    try:
        opts, pargs = getopt.getopt(sys.argv[1:], "s:i:a:t:x:")
    except getopt.GetoptError, e:
        usage(_("makeimage: illegal option -- %s") % e.opt)
        return 1
    global dc
    repo = DEFAULT_REPO
    imagedir = DEFAULT_IMAGE_DIR
    port = DEFAULT_PORT
    authority = DEFAULT_AUTHORITY
    title = DEFAULT_TITLE
    desc = DEFAULT_DESCRIPTION
    refresh_index = False
    for opt, arg in opts:
        if (opt == "-s"):
            repo = arg
            if not(repo.startswith("http://") or repo.startswith("file://")):
                usage("invalid arguments: -s argument must start with file:// or http://")
                return 1
            if repo.startswith("http://") :
                depoturl = repo
                repo = None
            elif repo.startswith("file://"):                
                repo = repo.replace("file://", "")                
        if (opt == "-i"):
            imagedir = arg
        if (opt == "-a"):
            authority = arg
        if (opt == "-t"):
            title = arg
        if (opt == "-x"):
            desc = arg

    pkg_list = [ pat.replace("*", ".*").replace("?", ".")
            for pat in pargs ]
    if len(pkg_list) == 0:
        usage(_("makeimage: must list at least one package to install"))
        return 1

    print "image directory:", imagedir
    if repo != None:
        while not port_available("localhost", port):
                print "Since the port (",port,") is in use, I am going to try the next port."
                port = port + 1
        dc = depotthread.DepotThread(repo=repo, port=port)
        print "args:", dc.get_args()
        print "starting depot..."
        try:
            dc.start()
            dc.waitforup()
        except depotthread.DepotStateException, e:
            print "unable to start depot."
            sys.exit(1)
        print "depot started."
        depoturl = dc.get_depot_url()

    pt = progress.CommandLineProgressTracker()
    img = image.Image()
    img.history.client_name = 'makeimage'
    img.set_attrs(image.IMG_USER, imagedir, False, authority, depoturl)
    
    # create a new Image object - it doesn't work to reuse the old one
    img = image.Image()
    img.history.client_name = 'makeimage'
    img.find_root(imagedir)
    img.load_config()
    img.set_property(u'title', title)
    img.set_property(u'description', desc)
    img.load_catalogs(pt)
    filters = []
    verbose = False
    try:
        img.make_install_plan(pkg_list, pt, lambda: False, False, filters = filters)
        img.imageplan.preexecute()
        img.imageplan.execute()
    except PlanCreationException, e:
        print >> sys.stderr, ("\n%s") % e
        return 1
    except InventoryException, e:
        print >> sys.stderr, ("\n%s") % e
        return 1
    except RuntimeError, e:
        print >> sys.stderr, ("install failed: %s") % e
        return 1
    img.cleanup_downloads()
    if os.path.exists(img.cached_download_dir()):
        shutil.rmtree(img.cached_download_dir())
    
    if repo:
        print "stopping depot."
        dc.stop()

    return 0

if __name__ == "__main__":
    try:
        ret = main_func()
    except InvalidDepotResponseException, e:
        print "\n\n", e
        ret=1
    except SystemExit, e:
        raise e
    except KeyboardInterrupt:
        print "Interrupted"
        ret=1
    except:
        traceback.print_exc()
        ret=99
    if dc and dc.is_alive():
        print "stopping depot."
        dc.stop()
    sys.exit(ret)
