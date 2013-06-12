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
package com.collabnet.svnedge


class SecurityFunctionalTests extends LoggedOutAbstractSvnEdgeFunctionalTests {

    void setUp() {
        super.setUp()
    }

    void tearDown() {
        super.tearDown()
    }

    void testAdminAuthority() {
        this.loginAdmin()

        get('/user/index')
        assertStatus 200
        assertContentContains getMessage("user.page.list.header")
    }

    void testUserAuthority() {
        this.loginUser()

        get('/server/index')
        assertContentContains getMessage("user.page.denied.error")
    }

    void testUserWithDotAuthority() {
        this.login("user.new", "admin")

        get('/server/index')
        assertContentContains getMessage("user.page.denied.error")
    }

    void testAdminUsersAuthority() {
        this.login("adminUsers", "admin")

        get('/user/index')
        assertStatus 200
        assertContentContains getMessage("user.page.list.header")

        get('/server/index')
        assertContentContains getMessage("user.page.denied.error")
    }

    void testAdminSystemAuthority() {
        this.login("adminSystem", "admin")

        get('/user/index')
        assertStatus 200
        assertContentContains getMessage("user.username.label")

        get('/server/edit')
        assertStatus 200
        assertContentContains getMessage('admin.page.leftNav.settings')
    }

    void testAdminReposAuthority() {
        this.login("adminRepo", "admin")

        get('/server/index')
        assertContentContains getMessage("user.page.denied.error")

        get('/repo/create')
        assertStatus 200
        assertContentContains getMessage('repository.page.create.title')
    }

}
