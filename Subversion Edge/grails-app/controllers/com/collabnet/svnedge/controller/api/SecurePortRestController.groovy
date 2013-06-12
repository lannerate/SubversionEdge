/*
 * CollabNet Subversion Edge
 * Copyright (C) 2011, CollabNet Inc. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */



package com.collabnet.svnedge.controller.api

import com.collabnet.svnedge.domain.Server
import grails.converters.JSON
import grails.converters.XML
import org.codehaus.groovy.grails.plugins.springsecurity.Secured

/**
 * REST API controller for retrieving the SSL settings
 * <p><bold>URL:</bold></p>
 * <code>
 *   /csvn/api/1/logging
 * </code>
 */
@Secured(['ROLE_USER'])
class SecurePortRestController extends AbstractRestController {

    /**
     * <p>REST method to view the SSL port for the console, and whether or not its use is required. No request body
     * is expected</p>
     * 
     * <p><bold>HTTP Method:</bold></p>
     * <code>
     *     GET
     * </code>
     * 
     */
    @Secured(['IS_AUTHENTICATED_ANONYMOUSLY'])
    def restRetrieve = {
        String port = System.getProperty("jetty.ssl.port", "4434")
        Server s = Server.getServer()
        def result = [SSLPort: port, SSLRequired: s.useSslConsole]
        withFormat {
            json { render result as JSON }
            xml { render result as XML }
        }
    }
}
