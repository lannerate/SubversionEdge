/*
 * CollabNet Subversion Edge
 * Copyright (C) 2010, CollabNet Inc. All rights reserved.
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
package com.collabnet.svnedge.controller


import org.codehaus.groovy.grails.plugins.springsecurity.Secured
import org.codehaus.groovy.grails.plugins.springsecurity.AuthorizeTools
import com.collabnet.svnedge.domain.integration.CloudServicesConfiguration
import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.Method.GET
import static groovyx.net.http.ContentType.TEXT
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials

/**
 * OpenCollabNet view controller
 */
@Secured(['ROLE_USER'])
class OcnController {

    def networkingService

    def index = {
        def cloudConfig = CloudServicesConfiguration.getCurrentConfig()
        boolean cloudEnabled = cloudConfig?.enabled
        try {
            def page = cloudEnabled ?
                    AuthorizeTools.ifAnyGranted('ROLE_ADMIN,ROLE_ADMIN_REPO,ROLE_ADMIN_SYSTEM,ROLE_ADMIN_USERS') ?
                    'svnedge-banner.html' : 'svnedge-banner-user.html' : 'svnedge-banner.html'
            def ocnContent = getPageContent('http://tab.open.collab.net/nonav/' + page)
            return [ocnContent: ocnContent, cloudEnabled: cloudEnabled]

        } catch (Exception e) {
            //No connection to the host... Possibly because the user is behind
            //a proxy or there's no Internet connectivity. 
            log.debug "Unable to contact url, using proxy", e
            return [cloudEnabled: cloudEnabled]
        }
    }

    private getPageContent(String url) throws IOException {
        def httpBuilder = new HTTPBuilder(url)

         // if needed, add proxy and proxy auth support
        def netCfg = networkingService.networkConfiguration
        if (netCfg?.httpProxyHost) {
            httpBuilder.setProxy(netCfg.httpProxyHost, netCfg.httpProxyPort, "http")
            if (netCfg.httpProxyUsername) {
                def httpClient = httpBuilder.getClient()
                httpClient.getCredentialsProvider().setCredentials(
                    new AuthScope(netCfg.httpProxyHost, netCfg.httpProxyPort),
                    new UsernamePasswordCredentials(netCfg.httpProxyUsername, netCfg.httpProxyPassword)
                )
            }
        }

        httpBuilder.request(GET, TEXT) { req ->
            headers.'User-Agent' = 'Mozilla/5.0'
            req.getParams().setParameter("http.connection.timeout", new Integer(3000))
            req.getParams().setParameter("http.socket.timeout", new Integer(3000))

            response.success = { resp, reader ->
                assert resp.status == 200
                return reader.text
            }

            // called only for a 404 (not found) status code:
            response.failure = { resp ->
                throw new IOException("Could not fetch page")
            }
        }
    }
}