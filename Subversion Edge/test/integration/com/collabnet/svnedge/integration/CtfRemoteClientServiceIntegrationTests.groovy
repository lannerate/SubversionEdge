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
package com.collabnet.svnedge.integration

import java.util.Locale;

import grails.test.*

import com.collabnet.svnedge.domain.integration.CtfServer 
import com.collabnet.svnedge.integration.CtfAuthenticationException
import com.collabnet.svnedge.domain.integration.ReplicaConfiguration
import com.collabnet.svnedge.integration.RemoteMasterException
import org.junit.Test
import com.collabnet.svnedge.domain.integration.ApprovalState

class CtfRemoteClientServiceIntegrationTests extends GrailsUnitTestCase {

    def ctfTestUrl
    def grailsApplication
    def config
    
    @Override
    protected void setUp() {
        super.setUp()
        this.config = grailsApplication.config
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

        if (!ReplicaConfiguration.getCurrentConfig()) {
            ReplicaConfiguration rConf = new ReplicaConfiguration(svnMasterUrl: null,
                name: "Test Replica", description: "Super replica",
                message: "Auto-approved", systemId: "replica1001",
                commandPollRate: 5, approvalState: ApprovalState.APPROVED)
            rConf.save(flush:true)
        }
    }
    
    def ctfRemoteClientService

    // FIXME:  for now, skip this test on Solaris to avoid hudson breakage until confirmed
    // to work in dev environment 
    def isSkipTests = System.getProperty("os.name").substring(0,3) == "Sun"

    def makeCtfBaseUrl() {
        def ctfProto = config.svnedge.ctfMaster.ssl ? "https://" : "http://"
        def ctfHost = config.svnedge.ctfMaster.domainName
        def ctfPort = config.svnedge.ctfMaster.port == "80" ? "" : ":" +
                config.svnedge.ctfMaster.port
        ctfTestUrl = ctfProto + ctfHost + ctfPort
        return ctfTestUrl
    }

    def getCtfUrl() {
        if (!ctfTestUrl) {
            ctfTestUrl = makeCtfBaseUrl()
        }
        return ctfTestUrl
    }

    void testIsUserValidWithValidUsers() {
        if (isSkipTests) {
            return
        }
        def username = config.svnedge.ctfMaster.username
        def password = config.svnedge.ctfMaster.password

        def response = ctfRemoteClientService.authenticateUser(username, 
            password)
        assertNotNull("Authentication must succeed for valid user on CTF", 
            response)
        try {
            response = ctfRemoteClientService.authenticateUser("admin", 
                "wrong-password")
            fail("Authentication must NOT succeed for a user on " +
                "CTF with a wrong password", response)
        } catch (CtfAuthenticationException wrongCredentialsError) {
            assertNotNull wrongCredentialsError
        }
    }

    void testLoginWithCorrectValues() {
        if (isSkipTests) {
            return
        }
        def ctfUrl = this.getCtfUrl()
        def username = config.svnedge.ctfMaster.username
        def password = config.svnedge.ctfMaster.password

        println("CTF URL: ${ctfUrl}")
        println("Credentials: ${username}:${password}")

        try {
            def sessionId = ctfRemoteClientService.login(ctfUrl, username,
                password, Locale.getDefault())
            assertNotNull("The session ID must have been created with " +
                "correct credentials", sessionId)
        } catch (CtfAuthenticationException loginFailedAsExpected) {
            fail("The login must NOT throw any exception with correct values")
        }
    }

    void testLoginWithIncorrectValues() {
        if (isSkipTests) {
            return
        }
        def ctfUrl = this.getCtfUrl()
        def username = "wrongUsername"
        def password = "wrongPasswd"

        println("SOAP call URL: ${ctfUrl}")
        println("Credentials: ${username}:${password}")

        try {
            def sessionId = ctfRemoteClientService.login(ctfUrl, username,
                password, Locale.getDefault())
            fail("The login must throw a login fault", sessionId)
        } catch (CtfAuthenticationException loginFailedAsExpected) {
            println(loginFailedAsExpected.message)
        }
    }

    void testIsUserValidWithInvalidUsers() {
        if (isSkipTests) {
            return
        }
        try {
            ctfRemoteClientService.authenticateUser("non-exist", "pwd")
            fail("Authentication must NOT succeed for non-existent " +
                   "user on CTF")
        } catch (CtfAuthenticationException authError) {
            assertNotNull(authError)
        }

        try {
            ctfRemoteClientService.authenticateUser("", "")
            fail("Authentication must NOT be valid for empty user/pw " +
                   "on CTF")
        } catch (CtfAuthenticationException authError) {
            assertNotNull(authError)
        }


        try {
            ctfRemoteClientService.authenticateUser("admin", "")
            fail("Authentication must NOT be valid for empty password" +
                   " on CTF")
        } catch (CtfAuthenticationException authError) {
            assertNotNull(authError)
        }

        try {
            ctfRemoteClientService.authenticateUser("", "12345")
            fail("Authentication must NOT be valid for empty user" +
                   " on CTF")
        } catch (CtfAuthenticationException authError) {
            assertNotNull(authError)
        }
    }

    void testGetRolePathsWithWithoutAccessType() {
        if (isSkipTests) {
            return
        }
        String response = ctfRemoteClientService.getRolePaths("admin", 
                                                              "internal/", "")
        println ("Roles Paths admin/internal/ = $response")
	// FIXME! "2" is the real expected value, but CTF is currently returning an empty
        // string.  
        assertEquals("Result MUST be in the format x:x when called without " +
                "accessType.", 2, response.tokenize(":").size())

        response = ctfRemoteClientService.getRolePaths("admin", "internal/", 
                null)
        println ("Roles Paths admin/exsy1006/internal/ = $response")
	// FIXME! "2" is the real expected value, but CTF is currently returning an empty
        // string.  
        assertEquals("Result MUST be in the format x:x when called without " +
                "accessType.", 2, response.tokenize(":").size())
    }

    void testGetRolePathsWithAccessType() {
        if (isSkipTests) {
            return
        }
        String response = ctfRemoteClientService.getRolePaths("admin", 
                "internal/", "view-all")
        println ("Roles Paths admin/exsy1006/internal/ = $response")
        assertEquals("Result MUST be in the format x:x:x when called without " +
                     "accessType.", 3, response.tokenize(":").size())
    }

    void testClearCacheOnMasterCTF() {
        if (isSkipTests) {
            return
        }
        assertTrue("Clear remote cache on a Master CTF must always be possible",
                ctfRemoteClientService.clearCacheOnMasterCTF())
    }

    @Test
    void testDeleteReplica()  {

        def username = config.svnedge.ctfMaster.username
        def password = config.svnedge.ctfMaster.password

        // although the replica does not exist, we expect this to succeed
        ctfRemoteClientService.deleteReplica(username, password, [], Locale.defaultLocale)
        assertTrue("The deleteReplica method should have succeeded", true)
    }

    @Test(expected=CtfAuthenticationException.class)
    void testDeleteReplicaBadAuthentication()  {

        def username = config.svnedge.ctfMaster.username

        // provide faulty credentials, expect exception
        ctfRemoteClientService.deleteReplica(username, "badPasssword!", [], Locale.defaultLocale)
    }

    @Test(expected=RemoteMasterException.class)
    void testDeleteReplicaBadAuthorization()  {

        def username = "DeleteReplicaTestUser"
        def password = "admin"

        // real user credentials, but lacking permission (Scm admin), should throw the expected exception
        ctfRemoteClientService.deleteReplica(username, password, [], Locale.defaultLocale)
    }

}
