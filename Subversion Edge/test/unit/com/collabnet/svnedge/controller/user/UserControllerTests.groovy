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
package com.collabnet.svnedge.controller.user

import grails.test.ControllerUnitTestCase
import org.springframework.security.providers.dao.UserCache
import org.grails.plugins.springsecurity.service.AuthenticateService

import com.collabnet.svnedge.console.LifecycleService;
import com.collabnet.svnedge.domain.Role;
import com.collabnet.svnedge.domain.Server
import com.collabnet.svnedge.domain.User;
import com.collabnet.svnedge.domain.Wizard
import com.collabnet.svnedge.util.ConfigUtil

import grails.util.Environment
import org.codehaus.groovy.grails.commons.ConfigurationHolder

/**
 * This class tests the non-trivial UserController actions, although
 * some of the most complex involve user Role grants, which cannot
 * be tested in this phase (params.authorities are not automatically converted
 * to Roles as they are in the runtime/integration environment) 
 */
class UserControllerTests extends ControllerUnitTestCase {

    User testUser
    User testAdmin
    User testAdminUsers

    protected void setUp() {
        super.setUp()
        loadConfig()

        mockDomain (Role, [
                new Role(authority: "ROLE_ADMIN", id: 1),
                new Role(authority: "ROLE_USER", id: 2),
                new Role(authority: "ROLE_ADMIN_USERS", id: 3)
        ])
        testUser = new User(username: "testUser", version: 2, passwd: "encodedPasswd")
        testAdmin = new User(username: "testAdmin", version: 1, passwd: "encodedPasswd")
        testUser.authorities = [Role.findByAuthority("ROLE_USER")]
        testAdmin.authorities = [Role.findByAuthority("ROLE_ADMIN")]
        mockDomain (User, [testUser, testAdmin])

        // mock the i18n "message" map available to controller
        controller.metaClass.message = { Map p -> return "foo" }

        // mock the bindData method
        controller.metaClass.bindData = { u, p, l -> u.properties = p }
        
        mockDomain (Server, [new Server(id: 1, adminEmail: 'devnull@collabnet.com')])
        mockDomain (Wizard, [])
    }
    
    private void loadConfig() {
        GroovyClassLoader classLoader = new GroovyClassLoader(this.class.classLoader)
        ConfigSlurper slurper = new ConfigSlurper(Environment.current.name)
        ConfigurationHolder.config = slurper.parse(classLoader.loadClass("Config"))
        ConfigUtil.configuration = ConfigurationHolder.config
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testIndex() {

        def authenticateService = mockFor(AuthenticateService)

        // pass the authority check by default
        authenticateService.demand.ifAnyGranted() {
            it == "ROLE_ADMIN,ROLE_ADMIN_USERS"
        }

        controller.authenticateService = authenticateService.createMock();
        def model = controller.index()

        assertEquals("Expected redirect to user list when PASSING authority check", "list",
                controller.redirectArgs["action"])

        // fail the authority check by default
        authenticateService.demand.ifAnyGranted() { 
            it == "ROLE_ADMIN"
        }
        // mock the security
        authenticateService.demand.principal() { ->
            def p = new Expando()
            p.getUsername = { return testUser.username }
            return p
        }

        controller.authenticateService = authenticateService.createMock();
        model = controller.index()

        assertEquals("Expected render user show when FAILING authority check", "showSelf",
                controller.redirectArgs["action"])
    }

    void testSave() {

        // 1) test for validation failure
        def authenticateService = mockFor(AuthenticateService)

        // expected security calls
        authenticateService.demand.ifNotGranted(2..2) {
            it == "ROLE_ADMIN,ROLE_ADMIN_USERS"
        }

        controller.authenticateService = authenticateService.createMock();

        def model = controller.save()

        assertEquals("Expected render create when FAILING validation check", "create",
            controller.renderArgs["view"])
        assertNotNull("Expected validation userInstance in model", model["userInstance"])
        assertTrue("Expected validation errors in model", model["userInstance"].hasErrors())

        // 2) test again for validation failure, missing only passwd
        authenticateService = mockFor(AuthenticateService)
        authenticateService.demand.ifNotGranted(2..2) {
            it == "ROLE_ADMIN,ROLE_ADMIN_USERS"
        }
        controller.authenticateService = authenticateService.createMock();

        // test some params for a save
        controller.params.username = "testUser2"
        controller.params.realUserName = "test User 2"
        controller.params.email = "test@test.com"
        controller.params.passwd = ""
        controller.params.authorities = "2"

        model = controller.save()

        assertEquals("Expected render create when FAILING validation check", "create",
            controller.renderArgs["view"])
        assertNotNull("Expected validation userInstance in model", model["userInstance"])
        assertTrue("Expected validation errors in model", model["userInstance"].hasErrors())

        // 3) test for validation success and save success
        def lifecycleService = mockFor(LifecycleService)
        authenticateService = mockFor(AuthenticateService)
        authenticateService.demand.ifNotGranted(1..2) {
            it == "ROLE_ADMIN,ROLE_ADMIN_USERS"
        }

        authenticateService.demand.encodePassword(1..1) {
            "encodedPassword"
        }

        lifecycleService.demand.setSvnAuth(1..1) {
            return true
        }

        controller.authenticateService = authenticateService.createMock();
        controller.lifecycleService = lifecycleService.createMock();
 
        // test some params for a save
        controller.params.username = "testUser2"
        controller.params.realUserName = "test User 2"
        controller.params.email = "test@test.com"
        controller.params.passwd = "clearPassword"
        controller.params.passwordConfirm = "clearPassword"
        controller.params.authorities = "2"

        model = controller.save()

        assertEquals("Expected redirect to show when SUCCEEDING", "show",
            controller.redirectArgs["action"])

        assertEquals("Expected new user id in model", 3, redirectArgs["id"])
        assertNotNull("Expected new user in the domain", User.get(3))
    }

    void testUpdate() {

        // 1) test for version mismatch failure
        controller.params.id = 1
        controller.params.version = 1

        def model = controller.update()

        assertEquals("Expected render 'edit' when FAILING version check", "edit",
            controller.renderArgs["view"])
        assertNotNull("Expected validation userInstance in model", controller.renderArgs["model"]["userInstance"])
        assertTrue("Expected validation errors in model", controller.renderArgs["model"]["userInstance"].hasErrors())

        // 2) test CAN update my user
        def authenticateService = mockFor(AuthenticateService)
        def lifecycleService = mockFor(LifecycleService)
        def userCache = mockFor(UserCache)

        authenticateService.demand.principal(2..2) { ->
            def p = new Expando()
            p.getUsername = { return testUser.username }
            return p
        }

        authenticateService.demand.ifAnyGranted(1..1) {
            // fail admin auth request
            false
        }

        authenticateService.demand.encodePassword(1..1) {
            it
        }

        lifecycleService.demand.setSvnAuth(1..1) {
            return true
        }

        User.metaClass.refresh = {}

        userCache.demand.removeUserFromCache(1..1) {
        }

        controller.authenticateService = authenticateService.createMock()
        controller.lifecycleService = lifecycleService.createMock()
        controller.userCache = userCache.createMock()
        controller.params.id = "1"
        controller.params.version = "2"
        controller.params.realUserName = "test User 2"
        controller.params.email = "test@test.com"
        controller.params.passwd = "encodedPasswd"
        controller.params.confirmPasswd = "encodedPasswd"

        model = controller.update()

        assertEquals("Expected redirect to show when SUCCEEDING", "show",
            controller.redirectArgs["action"])
        assertEquals("Expected new user id in model",1, redirectArgs["id"])

        // 3) test CANNOT update another user
        authenticateService = mockFor(AuthenticateService)

        authenticateService.demand.principal(1..1) { ->
            def p = new Expando()
            p.getUsername = { return testUser.username }
            return p
        }

        authenticateService.demand.ifAllGranted(1..2) {
            // fail admin auth request
            false
        }

        controller.authenticateService = authenticateService.createMock()
        controller.lifecycleService = lifecycleService.createMock()
        controller.params.id = "2"   // NOT the id of user from authenticateService.principal()
        controller.params.version = "1"
        controller.params.realUserName = "test User 2"
        controller.params.email = "test@test.com"
        controller.params.passwd = "encodedPasswd"

        model = controller.update()

        assertEquals("Expected render 'edit' when FAILING version check", "edit",
            controller.renderArgs["view"])
        assertNotNull("Expected validation userInstance in model", controller.renderArgs["model"]["userInstance"])
        assertTrue("Expected validation errors in model", controller.renderArgs["model"]["userInstance"].hasErrors())
    }
}
