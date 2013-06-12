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
import com.collabnet.svnedge.admin.LogManagementService.ApacheLogLevel
import com.collabnet.svnedge.admin.LogManagementService.ConsoleLogLevel

class LoggingApiTests extends AbstractSvnEdgeFunctionalTests {

    String url = "/api/1/logging"
    
    void testLoggingGet() {
        
        redirectEnabled = false
                                
        // not public
        get("${url}?format=xml")
        assertStatus 401
        
        // not for ROLE USER
        get("${url}?format=xml") {
            headers["Authorization"] = "Basic ${ApiTestHelper.makeUserAuthorization()}"
        }
        assertStatus 401

        // but allow admins
        get("${url}?format=xml") {
            headers["Authorization"] = "Basic ${ApiTestHelper.makeAdminAuthorization()}"
        }
        assertStatus 200
        assertContentContains "<entry key=\"consoleLogLevel\">WARN</entry>"
        assertContentContains "<entry key=\"serverLogLevel\">WARN</entry>"
        assertContentContains "<entry key=\"daysToKeep\">0</entry>"
    }

    void testLoggingUnsupportedMethods() {
       
        // unsupported methods send 401 to unauthorized client
        post(url) 
        assertStatus 401

        delete(url)
        assertStatus 401
        
        // authorized, but unsupported, calls receive 405 (not allowed)
        post(url) {
            headers["Authorization"] = "Basic ${ApiTestHelper.makeAdminAuthorization()}"
            body { "" }
        }
        assertStatus 405
        
        delete(url) {
            headers["Authorization"] = "Basic ${ApiTestHelper.makeAdminAuthorization()}"
        }
        assertStatus 405
    }

    /**
     * Tests the PUT method for updating logging config
     */
    void testLoggingPut() {
        
        // authorized with xml body should succeed
        def requestBody = 
"""<?xml version="1.0" encoding="UTF-8"?>
<map>
  <entry key="consoleLogLevel">INFO</entry>
  <entry key="serverLogLevel">INFO</entry>
  <entry key="daysToKeep">5</entry>
</map>
"""
        put("${url}?format=xml") {
            headers["Authorization"] = "Basic ${ApiTestHelper.makeAdminAuthorization()}"
            body { requestBody }
        }
        assertStatus 201
        
        Server s = Server.getServer()
        s.refresh()
        assertEquals "the console level should be INFO", ConsoleLogLevel.INFO, s.consoleLogLevel
        assertEquals "the server level should be INFO", ApacheLogLevel.INFO, s.apacheLogLevel
        assertEquals "the days to keep should be 5", 5, s.pruneLogsOlderThan
        
        // authorized with json body should succeed
        requestBody = 
"""{
    consoleLogLevel: "DEBUG",
    serverLogLevel: "ERROR",
    daysToKeep: 20
}"""
        put("${url}?format=json") {
            headers["Authorization"] = "Basic ${ApiTestHelper.makeAdminAuthorization()}"
            body { requestBody }
        }
        assertStatus 201
        
        s.refresh()
        assertEquals "the console level should be DEBUG", ConsoleLogLevel.DEBUG, s.consoleLogLevel
        assertEquals "the server level should be ERROR", ApacheLogLevel.ERROR, s.apacheLogLevel
        assertEquals "the days to keep should be 20", 20, s.pruneLogsOlderThan
    }

    /**
     * Tests the PUT method for updating logging config
     */
    void testLoggingPutFaultyRequest() {

        // unauthorized calls receive 401
        put(url) {
            body { "" }
        }
        assertStatus 401

        // authorized, but faulty, calls receive 400 (bad request)
        put(url) {
            headers["Authorization"] = "Basic ${ApiTestHelper.makeAdminAuthorization()}"
            body { "" }
        }
        assertStatus 400
        assertContentContains "<entry key=\"errorMessage\">"


        // unsupported element value should receive 400 (bad request)
        def requestBody =
"""<?xml version="1.0" encoding="UTF-8"?>
<map>
  <entry key="consoleLogLevel">ERRROR</entry>
  <entry key="serverLogLevel">INFO</entry>
  <entry key="daysToKeep">5</entry>
</map>
"""
        put("${url}?format=xml") {
            headers["Authorization"] = "Basic ${ApiTestHelper.makeAdminAuthorization()}"
            body { requestBody }
        }
        assertStatus 400
        assertContentContains "<entry key=\"errorMessage\">"

        // missing keyname (consoleLogLevel) should receive 400 (bad request)
        requestBody =
"""<?xml version="1.0" encoding="UTF-8"?>
<map>
  <entry key="serverLogLevel">INFO</entry>
  <entry key="daysToKeep">5</entry>
</map>
"""
        put("${url}?format=xml") {
            headers["Authorization"] = "Basic ${ApiTestHelper.makeAdminAuthorization()}"
            body { requestBody }
        }
        assertStatus 400
        assertContentContains "<entry key=\"errorMessage\">"

        // malformed XML should receive 400
        requestBody =
"""<?xml version="1.0" encoding="UTF-8"?>
<map>
  <entry key="consoleLogLevel">ERROR</entry>
  <entry key="serverLogLevel">INFO</entry>
  <entry key="daysToKeep">5</entry>
<map>
"""
        put("${url}?format=xml") {
            headers["Authorization"] = "Basic ${ApiTestHelper.makeAdminAuthorization()}"
            body { requestBody }
        }
        assertStatus 400
        assertContentContains "<entry key=\"errorMessage\">"

        // JSON for XML content type should receive 400
        requestBody =
"""{
    consoleLogLevel: "DEBUG",
    serverLogLevel: "ERROR",
    daysToKeep: 20
}"""
        put("${url}?format=xml") {
            headers["Authorization"] = "Basic ${ApiTestHelper.makeAdminAuthorization()}"
            body { requestBody }
        }
        assertStatus 400
        assertContentContains "<entry key=\"errorMessage\">"

    }
}