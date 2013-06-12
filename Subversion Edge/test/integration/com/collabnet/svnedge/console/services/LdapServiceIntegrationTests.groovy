/*
 * CollabNet Subversion Edge
 * Copyright (C) 2013, CollabNet Inc. All rights reserved.
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

import com.collabnet.svnedge.domain.Server;
import com.collabnet.svnedge.domain.User 
import grails.test.*

class LdapServiceIntegrationTests extends GrailsUnitTestCase {
    def ldapService

    protected void setUp() {
        super.setUp()
    
        Server server = Server.getServer()
        server.ldapEnabled = true
        server.ldapEnabledConsole = true
        server.ldapServerHost = "localhost"
        server.ldapServerPort = 10389
        server.ldapAuthBasedn = "ou=people,o=sevenSeas,dc=collabnet,dc=com"
        server.ldapLoginAttribute = 'uid'
        server.save()
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testGetLdapEmailForUsername() {
        def username = "hhornblo"
        def email = ldapService.getLdapEmailForUsername(username)
        assertNotNull(username + " email should not be null.", email)
        assertEquals("Email value is incorrect", 'hhornblo@royalnavy.mod.uk', email)
        
        username = "wbush"
        email = ldapService.getLdapEmailForUsername(username)
        assertNull(username + " email should be null.", email)

        username = "tquist"
        email = ldapService.getLdapEmailForUsername(username)
        assertNotNull(username + " email should not be null.", email)
        assertEquals("Email value is incorrect", 'tquist@royalnavy.mod.uk', email)
    }

    void testGetEmailForLdapUser() {

        def username = "hhornblo"
        User user = User.newLdapUser(username: username, realUserName: username)
        user.save()
        
        def email = ldapService.getEmailForLdapUser(user)
        assertNotNull(username + " email should not be null.", email)
        assertEquals("Email value is incorrect", 'hhornblo@royalnavy.mod.uk', email)

        def testMail = 'horatio@gmail.com' 
        user.email = testMail
        user.save()
        email = ldapService.getEmailForLdapUser(user)
        assertNotNull(username + " email should not be null.", email)
        assertEquals("Email value should be overridden", testMail, email)
    }
}
