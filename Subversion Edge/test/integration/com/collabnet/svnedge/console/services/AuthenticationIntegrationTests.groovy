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
package com.collabnet.svnedge.console.services

import java.util.Locale;

import com.collabnet.svnedge.domain.Server 
import com.collabnet.svnedge.domain.ServerMode 
import com.collabnet.svnedge.domain.integration.CtfServer 
import grails.test.*

import org.springframework.security.providers.UsernamePasswordAuthenticationToken
import org.springframework.security.BadCredentialsException

class AuthenticationIntegrationTests extends GrailsUnitTestCase {

    def authenticationManager
    def ctfAuthenticationProvider
    def daoAuthenticationProvider
    def anonymousAuthenticationProvider
    def Server server
    def ctfRemoteClientService
    def grailsApplication
    def config

    def TEST_USERNAME = "mdesales"
    def TEST_PASSWORD = "Coll@b123"

    protected void setUp() {
        super.setUp()
        this.config = grailsApplication.config
        server = Server.getServer()
        authenticationManager.providers = [ctfAuthenticationProvider]
        def ctfProto = config.svnedge.ctfMaster.ssl ? "https://" : "http://"
        def ctfHost = config.svnedge.ctfMaster.domainName
        def ctfPort = config.svnedge.ctfMaster.port == "80" ? "" : ":" +
                config.svnedge.ctfMaster.port
        def ctfUrl = ctfProto + ctfHost + ctfPort
        def adminUsername = config.svnedge.ctfMaster.username
        def adminPassword = config.svnedge.ctfMaster.password

        if (!CtfServer.getServer()) {
            CtfServer s = new CtfServer(baseUrl: ctfUrl, mySystemId: "exsy1000",
                    internalApiKey: "testApiKey",
                    ctfUsername: adminUsername,
                   ctfPassword: adminPassword)
            s.save(flush:true)
        }
    }

    protected void tearDown() {
        super.tearDown()
        authenticationManager.providers = [daoAuthenticationProvider,
            anonymousAuthenticationProvider]
    }

    void testRootAuthentication() {
        def username = config.svnedge.ctfMaster.username
        def password = config.svnedge.ctfMaster.password
        def authToken = new UsernamePasswordAuthenticationToken(username, 
                                                                password)
        assertFalse("The authenticationManager should not be null",
                    authenticationManager == null)
        def auth
        def origMode = server.mode
        try {
            server.mode = ServerMode.MANAGED
            auth = authenticationManager?.authenticate(authToken)
        } catch (BadCredentialsException bce) {
            fail("The root user should be able to authenticate.")
        }
        finally {
            server.mode = origMode
        }
        assertFalse("The returned authentication should not be null.",
                    auth == null)
        assertTrue("The returned authentication should be authenticated.", 
                   auth?.isAuthenticated() == true)
        assertTrue("The returned authentication should have the correct " +
                       "username.", auth.getName() == username)
    }

    void testNonRootAuthentication() {
        try {
            def ctfProto = config.svnedge.ctfMaster.ssl ? "https://" : "http://"
            def ctfHost = config.svnedge.ctfMaster.domainName
            def ctfPort = config.svnedge.ctfMaster.port == "80" ? "" : ":" +
                    config.svnedge.ctfMaster.port
            def ctfUrl = ctfProto + ctfHost + ctfPort
            def adminUsername = config.svnedge.ctfMaster.username
            def adminPassword = config.svnedge.ctfMaster.password

            def adminSessionId = ctfRemoteClientService.login(ctfUrl, 
                adminUsername, adminPassword, Locale.getDefault())
            ctfRemoteClientService.createUser(ctfUrl, adminSessionId, 
                TEST_USERNAME, TEST_PASSWORD, "mdesales@collab.net", 
                "Marcello de Sales", true, false, null)
        } catch (Exception e) {
            println(e.message)
            e.printStackTrace()
        }

        def username = TEST_USERNAME
        def password = TEST_PASSWORD

        def authToken = new UsernamePasswordAuthenticationToken(username, 
                                                                password)
        assertFalse("The authenticationManager should not be null.",
                    authenticationManager == null)
        def auth
        def origMode = server.mode
        try {
            server.mode = ServerMode.MANAGED
            auth = authenticationManager?.authenticate(authToken)
        } catch (BadCredentialsException bce) {
            fail("The non-root user should be able to authenticate. " +
                     "(Got exception: " + bce.getMessage() + " .")
        }
        finally {
            server.mode = origMode
        }
        assertFalse("The returned authentication should not be null.",
                    auth == null)
        assertTrue("The returned authentication should be authenticated.", 
                   auth?.isAuthenticated() == true)
        assertTrue("The returned authentication should have the correct " +
                       "username.", auth.getName() == username)
    }

    void testFailAuthentication() {
        def username = "marcello"
        def password = "xyzt"
        def authToken = new UsernamePasswordAuthenticationToken(username, 
                                                                password)
        def auth
        def origMode = server.mode
        try {
            server.mode = ServerMode.MANAGED
            auth = authenticationManager?.authenticate(authToken)
            fail("Incorrect authentication should throw an exception.")
        } catch (Exception e) {
            assertNotNull("An exception should be thrown on incorrect " +
                              "authentication", e)
            assertTrue("Failed authentication should throw a " +
                           "BadCredentialsException " + e.getClass().getName() + " " + e.getMessage(), 
                       e instanceof BadCredentialsException)
        }
        finally {
            server.mode = origMode
        }
    }

}
