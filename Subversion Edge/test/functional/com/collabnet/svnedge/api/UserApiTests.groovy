/*
 * CollabNet Subversion Edge
 * Copyright (C) 2012, CollabNet Inc. All rights reserved.
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
import com.collabnet.svnedge.domain.User

class UserApiTests extends AbstractSvnEdgeFunctionalTests {

    def url = "/api/1/user"

    void testUserPost() {
        
        def testUser = "apiUsername" + Math.floor(Math.random() * 1000)
        def requestBody =
"""{
    username: "${testUser}",
    password: "apiPassword",
    fullName: "Api Tester",
    emailAddress: "g@g.com",
}"""
        post("${url}?format=json") {
            headers["Authorization"] = "Basic ${ApiTestHelper.makeAdminAuthorization()}"
            body { requestBody }
        }
        assertStatus 201  
        assertContentContains "\"userId\":"
        
        User u = User.findByUsername(testUser)
        assertNotNull("the user should be created", u)
    }
    
    void testUserPostFaultyRequest() {

        // unauthorized calls receive 401
        post(url) {
            body { "" }
        }
        assertStatus 401

        // authorized, but faulty, calls receive 400 (bad request)
        post(url) {
            headers["Authorization"] = "Basic ${ApiTestHelper.makeAdminAuthorization()}"
            body { "" }
        }
        assertStatus 400
        assertContentContains "<entry key=\"errorMessage\">"
        
        // body missing emailAddress expects 400
        def testUser = "apiUsername" + Math.floor(Math.random() * 1000)
        def requestBody =
"""{
    username: "${testUser}",
    password: "apiPassword",
    fullName: "Api Tester"
}"""
        post("${url}?format=json") {
            headers["Authorization"] = "Basic ${ApiTestHelper.makeAdminAuthorization()}"
            body { requestBody }
        }
        assertStatus 400
        assertContentContains "\"errorMessage\":"

        User u = User.findByUsername(testUser)
        assertNull("the user should not be created", u)
    }
    
    void testUserUnsupportedMethods() {
        // unauthorized calls receive 401
        get(url)
        assertStatus 401
        
        put(url) {
            body { "" }
        }
        assertStatus 401

        delete(url)
        assertStatus 401

        // authorized calls receive 405 (not implemented)
        put(url) {
            headers['Authorization'] = "Basic ${ApiTestHelper.makeAdminAuthorization()}"
            body { "" }
        }
        assertStatus 405

        delete(url) {
            headers['Authorization'] = "Basic ${ApiTestHelper.makeAdminAuthorization()}"
        }
        assertStatus 405

        // a repo detail view is not yet supported
        get(url) {
            headers['Authorization'] = "Basic ${ApiTestHelper.makeAdminAuthorization()}"
        }
        assertStatus 405
    }
}
