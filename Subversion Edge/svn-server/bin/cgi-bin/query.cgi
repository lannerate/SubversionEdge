#!/usr/bin/env python
# -*-python-*-
#
# Copyright (C) 1999-2013 The ViewCVS Group. All Rights Reserved.
#
# By using this file, you agree to the terms and conditions set forth in
# the LICENSE.html file which can be found at the top level of the ViewVC
# distribution or at http://viewvc.org/license-1.html.
#
# For more information, visit http://viewvc.org/
#
# -----------------------------------------------------------------------
#
# query.cgi: View CVS/SVN commit database by web browser
#
# -----------------------------------------------------------------------
#
# This is a teeny stub to launch the main ViewVC app. It checks the load
# average, then loads the (precompiled) viewvc.py file and runs it.
#
# -----------------------------------------------------------------------
#

#########################################################################
#
# INSTALL-TIME CONFIGURATION
#
# These values will be set during the installation process. During
# development, they will remain None.
#
CSVN_HOME_DIR = None
SVN_LIBRARY_DIR = None
LIBRARY_DIR = None
import os

CONF_PATHNAME = os.path.join(os.getenv("CSVN_CONF"), "viewvc.conf")

#########################################################################
#
# Adjust sys.path to include our library directory
#

import sys
import os

CSVN_HOME_DIR = os.getenv("CSVN_HOME")
if CSVN_HOME_DIR:
  SVN_LIBRARY_DIR = os.path.abspath(os.path.join(CSVN_HOME_DIR,
                                      "lib", "svn-python"))
  LIBRARY_DIR = os.path.abspath(os.path.join(CSVN_HOME_DIR,
                                  "lib")
  CONF_PATHNAME   = os.path.abspath(os.path.join(CSVN_HOME_DIR,
                                      "data", "conf", "viewvc.conf"))

if LIBRARY_DIR:
  sys.path.insert(0, SVN_LIBRARY_DIR)
  sys.path.insert(0, LIBRARY_DIR)
else:
  sys.path.insert(0, os.path.abspath(os.path.join(sys.argv[0],
                                                  "../../../lib")))

#########################################################################

import sapi
import viewvc
import query

server = sapi.CgiServer()
cfg = viewvc.load_config(CONF_PATHNAME, server)
viewvc_base_url = cfg.query.viewvc_base_url
if viewvc_base_url is None:
  viewvc_base_url = "viewvc.cgi"
query.main(server, cfg, viewvc_base_url)
