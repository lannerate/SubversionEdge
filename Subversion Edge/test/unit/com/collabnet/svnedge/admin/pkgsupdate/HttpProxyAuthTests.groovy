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
package com.collabnet.svnedge.admin.pkgsupdate

import com.collabnet.svnedge.net.HttpProxyAuth
import grails.test.*


/**
 * The tests proxy auth tests.
 *  
 * @author Marcello de Sales (mdesales@collab.net)
 *
 */
class HttpProxyAuthTests extends GrailsUnitTestCase {

    void testProxyAuthCreationWithNullURL() {
        def url = null
        try {
            HttpProxyAuth.newInstance(url)
            fail("No proxy auth can be created with a null URL")
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException)
        }
    }

    void testProxyAuthCreationWithoutAuthentication() {
        def url = new URL("http://myproxy.com:8022")
        def proxyAuth = HttpProxyAuth.newInstance(url)
        assertEquals("myproxy.com", proxyAuth.address().getHostName())
        assertEquals(8022, proxyAuth.address().getPort())
        assertEquals(null, proxyAuth.username)
        assertEquals(null, proxyAuth.password)
        assertEquals("http://myproxy.com:8022", proxyAuth.toString())
        assertNull(proxyAuth.pwdAuth)
    }

    void testProxyAuthCreationWithAuthentication() {
        def url = new URL("http://marcello:cubit123@myproxy.com:8022")
        def proxyAuth = HttpProxyAuth.newInstance(url)
        assertEquals("myproxy.com", proxyAuth.address().getHostName())
        assertEquals(8022, proxyAuth.address().getPort())
        assertEquals("marcello", proxyAuth.username)
        assertEquals("cubit123", proxyAuth.password)
        //the number of stars (*) are the same as the number of characters in 
        //the password
        assertEquals("http://marcello:********@myproxy.com:8022", 
                proxyAuth.toString())

        //The password authenticator is not null and holds the usr and pwd.
        assertNotNull(proxyAuth.pwdAuth)
        assertEquals("marcello", proxyAuth.pwdAuth.getUserName())
        assertEquals("cubit123".toCharArray(), proxyAuth.pwdAuth.getPassword())
    }
}
