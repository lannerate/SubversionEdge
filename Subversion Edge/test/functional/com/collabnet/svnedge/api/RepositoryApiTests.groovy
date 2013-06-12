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
import com.collabnet.svnedge.domain.Repository
import com.collabnet.svnedge.domain.ServerMode
import com.collabnet.svnedge.util.ConfigUtil
import com.collabnet.svnedge.domain.RepoTemplate

class RepositoryApiTests extends AbstractSvnEdgeFunctionalTests {

    def svnRepoService
    def commandLineService
    def repoTemplateService
    
    def url = "/api/1/repository"
    
    void testRepositoryGet() {
        
        def server = Server.getServer()
        def testRepoName = "api-test" + Math.random() * 10000
        def repo = new Repository(name: testRepoName)
        assertEquals "Failed to create repository.", 0,
                svnRepoService.createRepository(repo, true)
        repo.save(flush: true)
        repo = Repository.findByName(testRepoName) 
        assertNotNull "Repo should be created", repo

        // without auth, GET is protected
        get("${url}?format=xml")
        assertStatus 401

        // authorized request contains repo information
        get("${url}?format=json") {
            headers['Authorization'] = "Basic ${ApiTestHelper.makeUserAuthorization()}"
        }
        assertContentContains '{"repositories":['
        assertContentContains "\"id\":${repo.id},"        
        assertContentContains "\"name\":\"${repo.name}\","        
        assertContentContains "\"status\":\"OK\","        
        assertContentContains "\"svnUrl\":\"${server.svnURL()}${repo.name}\","        
        assertContentContains "\"viewvcUrl\":\"${server.viewvcURL(repo.name)}\""     
        
        svnRepoService.removeRepository(repo)
        svnRepoService.deletePhysicalRepository(repo)
    }

    /**
     * Tests the POST method for creating a Repo
     */
    void testRepositoryPost() {

        def server = Server.getServer()
        def repoParentDir = server.repoParentDir
        def repoName = "api-test" + Math.random() * 10000
        
        // test new repo creation, no template
        def requestBody =
"""<?xml version="1.0" encoding="UTF-8"?>
<map>
  <entry key="name">${repoName}</entry>
  <entry key="applyStandardLayout">false</entry>
</map>
"""
        post("${url}?format=xml") {
            headers["Authorization"] = "Basic ${ApiTestHelper.makeAdminAuthorization()}"
            body { requestBody }
        }
        assertStatus 201
        assertContentContains "<entry key=\"message\">${getMessage("api.message.201")}</entry>"
        assertContentContains "<entry key=\"repository\">"
        assertContentContains "<entry key=\"viewvcUrl\">${server.viewvcURL(repoName)}</entry>"

        def output = commandLineService.executeWithOutput(
                ConfigUtil.svnPath(), "info",
                "--no-auth-cache", "--non-interactive",
                commandLineService.createSvnFileURI(new File(repoParentDir, repoName)) + "trunk")

        assertFalse("The new repo should not have a trunk directory", output.contains("Node Kind: directory"))
        cleanupRepo(repoName)

        // test new repo creation, with standard layout
        repoName = "api-test" + Math.random() * 10000
        requestBody =
            """<?xml version="1.0" encoding="UTF-8"?>
<map>
  <entry key="name">${repoName}</entry>
  <entry key="applyStandardLayout">true</entry>
</map>
"""
        post("${url}?format=xml") {
            headers["Authorization"] = "Basic ${ApiTestHelper.makeAdminAuthorization()}"
            body { requestBody }
        }
        assertStatus 201
        assertContentContains "<entry key=\"message\">${getMessage("api.message.201")}</entry>"
        assertContentContains "<entry key=\"repository\">"
        assertContentContains "<entry key=\"viewvcUrl\">${server.viewvcURL(repoName)}</entry>"

        output = commandLineService.executeWithOutput(
                ConfigUtil.svnPath(), "info",
                "--no-auth-cache", "--non-interactive",
                commandLineService.createSvnFileURI(new File(repoParentDir, repoName)) + "trunk")

        assertTrue("The new repo should have a trunk directory", output.contains("Node Kind: directory"))
        cleanupRepo(repoName)

        // test new repo with template
        RepoTemplate rt = ApiTestHelper.createTemplate(repoTemplateService, svnRepoService)
        repoName = "api-test" + Math.random() * 10000
        requestBody =
            """<?xml version="1.0" encoding="UTF-8"?>
<map>
  <entry key="name">${repoName}</entry>
  <entry key="applyStandardLayout">false</entry>
  <entry key="applyTemplateId">${rt.id}</entry>
</map>
"""
        post("${url}?format=xml") {
            headers["Authorization"] = "Basic ${ApiTestHelper.makeAdminAuthorization()}"
            body { requestBody }
        }
        assertStatus 201
        assertContentContains "<entry key=\"message\">${getMessage("api.message.201")}</entry>"
        assertContentContains "<entry key=\"repository\">"
        assertContentContains "<entry key=\"viewvcUrl\">${server.viewvcURL(repoName)}</entry>"

        // wait for async job to load the template
        // verify that repo load has run (trunk/tags/branches which should have been imported)
        boolean loadSuccess = false
        def timeLimit = System.currentTimeMillis() + 60000
        while (!loadSuccess && System.currentTimeMillis() < timeLimit) {
            Thread.sleep(3000)
            output = commandLineService.executeWithOutput(
                    ConfigUtil.svnPath(), "info",
                    "--no-auth-cache", "--non-interactive",
                    commandLineService.createSvnFileURI(new File(repoParentDir, repoName)) + "trunk")

            loadSuccess = output.contains("Node Kind: directory")
        }      
        
        assertTrue("The new repo should have a trunk directory from template", output.contains("Node Kind: directory"))
        cleanupRepo(repoName)
    }

    private void cleanupRepo(repoName) {
        Repository repo = Repository.findByName(repoName)
        svnRepoService.removeRepository(repo)
        svnRepoService.deletePhysicalRepository(repo)
    }

    void testRepositoryPostFaultyRequest() {

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

        // missing element value (name) should receive 400 (bad request)
        def requestBody = 
"""<?xml version="1.0" encoding="UTF-8"?>
<map>
  <entry key="naame">myName</entry>
  <entry key="applyStandardLayout">false</entry>
</map>
"""
        post("${url}?format=xml") {
            headers["Authorization"] = "Basic ${ApiTestHelper.makeAdminAuthorization()}"
            body { requestBody }
        }
        assertStatus 400
        assertContentContains "<entry key=\"errorMessage\">"

        // malformed XML should receive 400
        requestBody =
"""<?xml version="1.0" encoding="UTF-8"?>
<map>
  <entry key="naame">myName</entry>
  <entry key="applyStandardLayout">false</entry>
<map>
"""
        post("${url}?format=xml") {
            headers["Authorization"] = "Basic ${ApiTestHelper.makeAdminAuthorization()}"
            body { requestBody }
        }
        assertStatus 400
        assertContentContains "<entry key=\"errorMessage\">"

        // non-existent template id should receive 400 error
        def repoName = "api-test" + Math.random() * 10000
        requestBody =
            """<?xml version="1.0" encoding="UTF-8"?>
<map>
  <entry key="name">${repoName}</entry>
  <entry key="applyStandardLayout">false</entry>
  <entry key="applyTemplateId">100</entry>
</map>
"""
        post("${url}?format=xml") {
            headers["Authorization"] = "Basic ${ApiTestHelper.makeAdminAuthorization()}"
            body { requestBody }
        }
        assertStatus 400
        assertContentContains "<entry key=\"errorMessage\">"
    }

    void testRepositoryUnsupportedMethods() {
        // unauthorized calls receive 401
        put(url) {
            body { "" }
        }
        assertStatus 401

        delete(url)
        assertStatus 401

        // authorized calls receive 405 (not implemented)
        put(url) {
            headers['Authorization'] = "Basic ${ApiTestHelper.makeUserAuthorization()}"
            body { "" }
        }
        assertStatus 405

        delete(url) {
            headers['Authorization'] = "Basic ${ApiTestHelper.makeUserAuthorization()}"
        }
        assertStatus 405

        // a repo detail view is not yet supported
        get("${url}/1") {
            headers['Authorization'] = "Basic ${ApiTestHelper.makeUserAuthorization()}"
        }
        assertStatus 405
    }
    
    void testRepositoryIntegratedServer() {
        
        ApiTestHelper.executeSql("UPDATE SERVER SET MODE = 'MANAGED'")
        
        // without auth, GET is protected
        get("${url}?format=xml")
        assertStatus 401
        
        // authorized request should respond 405
        get("${url}?format=xml") {
            headers['Authorization'] = "Basic ${ApiTestHelper.makeUserAuthorization()}"
        }
        assertStatus 405
         
        ApiTestHelper.executeSql("UPDATE SERVER SET MODE = 'REPLICA'")
         
        // authorized request should respond 405
        get("${url}?format=json") {
            headers['Authorization'] = "Basic ${ApiTestHelper.makeUserAuthorization()}"
        }
        assertStatus 405
         
        ApiTestHelper.executeSql("UPDATE SERVER SET MODE = 'STANDALONE'")
    }
}
