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
import com.collabnet.svnedge.domain.Role

class RoleApiTests extends AbstractSvnEdgeFunctionalTests{

    def url = "/api/1/role"

    void testRoleGet() {

        // fetch list of role "authorities"
        get("${url}?format=json") {
            headers['Authorization'] = "Basic ${ApiTestHelper.makeAdminAuthorization()}"
        }
        assertContentContains '{"roles":['
        assertContentContains "\"authority\":\"ROLE_USER\","
        assertContentContains "\"authority\":\"ROLE_ADMIN\","
    }
    
    void testRolePut() {

        // create a user 
        def testUsername = "apiUsername" + Math.floor(Math.random() * 1000)
        def user = new User(username: testUsername, 
                passwd: "[secret]",
                email: "c1@collab.net",
                realUserName: "Test User"
        )
        user.save(flush: true)
        user.refresh()
        
        // add new user to the role_admin
        Role role = Role.findByAuthority("ROLE_ADMIN")
        def requestBody =
            """{
    userId: ${user.id},
    action: "add"
}"""
        put("${url}/${role.id}?format=json") {
            headers["Authorization"] = "Basic ${ApiTestHelper.makeAdminAuthorization()}"
            body { requestBody }
        }
        assertStatus 201
        assertContentContains "\"message\":"

        // verify role grant
        User u = User.findByUsername(testUsername)
        assertNotNull("the user should be created", u)
        assertTrue("ROLE_ADMIN should be granted", u.authorities.contains(role))
    }

    void testRoleUnsupportedMethods() {
        // unauthorized calls receive 401
        get(url)
        assertStatus 401

        put(url) {
            body { "" }
        }
        assertStatus 401

        post(url) {
            body { "" }
        }
        assertStatus 401
        
        delete(url)
        assertStatus 401

        // authorized calls receive 405 (not implemented)
        post(url) {
            headers['Authorization'] = "Basic ${ApiTestHelper.makeAdminAuthorization()}"
            body { "" }
        }
        assertStatus 405

        delete(url) {
            headers['Authorization'] = "Basic ${ApiTestHelper.makeAdminAuthorization()}"
        }
        assertStatus 405
    }
}
