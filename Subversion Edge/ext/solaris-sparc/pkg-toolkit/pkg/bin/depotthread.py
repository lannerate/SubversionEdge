#!/usr/bin/env python
# -*- coding: utf-8 -*-
#
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
import os
import time
import urllib2
import httplib
import pkg.depot as depot
import pkg.server.config as config
import pkg.server.repository as repo
import pkg.server.depot as sdepot
import pkg.server.depotresponse as dr
from pkg.misc import port_available, versioned_urlopen
import cherrypy

class DepotStateException(Exception):

        def __init__(self, reason):
                Exception.__init__(self, reason)

class DummyHandle:
        def poll(self):
                return None

class DepotThread:

        def __init__(self, port=10001, repo='.'):
                self.__port = port
                self.__dir = repo
                self.cherrypy = None
                return

        def get_depot_url(self):
                # Code-based workaround for issue 1717 on Mac OS X: Force use of
                # 127.0.0.1 rather than localhost when file:// URL is supplied.
                return "http://127.0.0.1:%d" % self.__port

        def __network_ping(self):
                try:
                    c, v = versioned_urlopen(self.get_depot_url(), "catalog", [0])
                except urllib2.HTTPError, e:
                    # Server returns NOT_MODIFIED if catalog is up
                    # to date
                    if e.code == httplib.NOT_MODIFIED:
                        return True
                    else:
                        return False
                except urllib2.URLError:
                        return False
                return True

        def is_alive(self):
                """ Make a little HTTP request to see if the depot is
                    responsive to requests """
                return self.__network_ping()


        def get_args(self):
                """ Return the equivalent command line invocation (as an
                    array) for the depot as currently configured. """

                args =  [  ]
                if self.__port != -1:
                        args.append("-p")
                        args.append("%d" % self.__port)
                if self.__dir != None:
                        args.append("-d")
                        args.append(self.__dir)
                return args
                
        def start(self):
                if not port_available("localhost", self.__port):
                        raise DepotStateException("A depot (or some " +
                                    "other network process) seems to be " +
                                    "running on port %d already!" % self.__port)

                # this uses the repo dir as the static content dir, which means that 
                # static content will not work, but we don't need it anyway for what
                # we are doing.
                scfg = config.SvrConfig(self.__dir, self.__dir, depot.AUTH_DEFAULT,
                    auto_create=True)
                scfg.init_dirs()
                scfg.acquire_in_flight()
                # we rebuild the catalog in a strange way here to avoid rebuilding
                # the index, which doesn't work
                scfg.acquire_catalog(rebuild=False)
                # prevent this depot from indexing
                scfg.catalog.searchdb_update_handle = DummyHandle()

                root = cherrypy.Application(sdepot.DepotHTTP(scfg, None))

                cherrypy.config.update({
                    "environment": "production",
                    "checker.on": True,
                    "log.screen": False,
                    "server.socket_port": self.__port,
                    "server.thread_pool": depot.THREADS_DEFAULT,
                    "server.socket_timeout": depot.SOCKET_TIMEOUT_DEFAULT,
                    "tools.log_headers.on": True
                })

                conf = {
                    "/": {
                        # We have to override cherrypy's default response_class so that
                        # we have access to the write() callable to stream data
                        # directly to the client.
                        "wsgi.response_class": dr.DepotResponse,
                    },
                    "/robots.txt": {
                        "tools.staticfile.on": True,
                        "tools.staticfile.filename": os.path.join(scfg.web_root,
                            "robots.txt")
                    },
                }

                cherrypy.config.update(conf)
                cherrypy.tree.mount(root, "", conf)

                engine = cherrypy.engine
                if hasattr(engine, "signal_handler"):
                    engine.signal_handler.subscribe()
                if hasattr(engine, "console_control_handler"):
                    engine.console_control_handler.subscribe()

                engine.start()

        def stop(self):
            cherrypy.engine.exit()
            cherrypy.engine.block()
            
        def waitforup(self):
                sleeptime = 0.05
                contact = False
                while sleeptime <= 20.0:
                        if self.is_alive():
                                contact = True
                                break
                        time.sleep(sleeptime)
                        sleeptime *= 2
                      
                if contact == False:
                        raise DepotStateException("Depot did not respond to repeated attempts to make contact")
