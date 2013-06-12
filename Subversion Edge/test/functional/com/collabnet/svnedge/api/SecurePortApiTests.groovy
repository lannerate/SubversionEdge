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
package com.collabnet.svnedge.api

import com.collabnet.svnedge.AbstractSvnEdgeFunctionalTests
import com.collabnet.svnedge.domain.Server


class SecurePortApiTests extends AbstractSvnEdgeFunctionalTests {

    void testSecurePortGet() {

        get('/api/1/securePort?format=xml')
        assertStatus 200
        assertContentContains '<entry key="SSLPort">4434</entry>'
        assertContentContains '<entry key="SSLRequired">false</entry>'
    }

    void testSecurePortUnsupportedMethods() {
        // unauthorized calls receive 401
        put('/api/1/securePort') {
            body { "" }
        }
        assertStatus 401

        post('/api/1/securePort') {
            body { "" }
        }
        assertStatus 401

        delete('/api/1/securePort')
        assertStatus 401
        
        // authorized calls receive 405 (not implemented)
        put('/api/1/securePort') {
            headers['Authorization'] = "Basic ${ApiTestHelper.makeUserAuthorization()}"
            body { "" }
        }
        assertStatus 405
        
        post('/api/1/securePort') {
            headers['Authorization'] = "Basic ${ApiTestHelper.makeUserAuthorization()}"
            body { "" }
        }
        assertStatus 405
        
        delete('/api/1/securePort') {
            headers['Authorization'] = "Basic ${ApiTestHelper.makeUserAuthorization()}"
        }
        assertStatus 405
    }

    /**
     * Tests the API behavior when the console is set to useSsl
     */
    void testSecurePortSslRedirect() {

        ApiTestHelper.executeSql("UPDATE SERVER SET USE_SSL_CONSOLE = 'TRUE'")

        // we should receive redirect to the 4434 url on other api methods 
        redirectEnabled = false
        put('/api/1/securePort') {
            headers['Authorization'] = "Basic ${ApiTestHelper.makeUserAuthorization()}"
            body { "" }
        }
        assertStatus 302
        assertHeader("Location", "https://localhost:4434/csvn/api/1/securePort")
      
        // but the GET method should continue to function on the non-secure port 
        get('/api/1/securePort?format=xml')
        assertStatus 200
        assertContentContains '<entry key="SSLPort">4434</entry>'
        assertContentContains '<entry key="SSLRequired">true</entry>'
        
        ApiTestHelper.executeSql("UPDATE SERVER SET USE_SSL_CONSOLE = 'FALSE'")
    }


}
