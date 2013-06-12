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

import org.junit.Test

import com.collabnet.svnedge.TestUtil
import com.collabnet.svnedge.domain.integration.CloudServicesConfiguration;
import com.collabnet.svnedge.util.ConfigUtil
import org.apache.commons.logging.LogFactory
import com.collabnet.svnedge.domain.Repository
import com.collabnet.svnedge.domain.Server
import com.collabnet.svnedge.domain.User;

class CloudServicesRemoteClientServiceIntegrationTests extends GrailsUnitTestCase {

    def log = LogFactory.getLog(CloudServicesRemoteClientServiceIntegrationTests.class)

    def grailsApplication
    def config
    def cloudServicesRemoteClientService
    def securityService
    def svnRepoService
    boolean skipTests
    CloudServicesConfiguration csConf
    def repoParentDir

    
    @Override
    protected void setUp() {
        super.setUp()
        this.config = grailsApplication.config
        
        csConf = CloudServicesConfiguration.getCurrentConfig()
        if (!csConf || !csConf.domain) {
            File f = new File(ConfigUtil.appHome(), "testCred.properties")
            if (f.exists()) {
                Properties up = new Properties()
                f.withReader { up.load(it) }
                csConf = new CloudServicesConfiguration(username: up['username'], 
                        password: up['password'], domain: up['domain'], 
                        enabled: true)
                csConf.save()
                skipTests = false
            } else {
                skipTests = true
                log.warn("Skipping unit tests")
            }
        }
        // Setup a test repository parent
        repoParentDir = TestUtil.createTestDir("repo")
        Server server = Server.getServer()
        server.repoParentDir = repoParentDir.getCanonicalPath()
        server.save()
    }
    
    @Override
    protected void tearDown() {
        super.tearDown()
        repoParentDir.deleteDir()
    }



    void testCreateSvnAndDeleteProject() {
        if (skipTests) {
            return
        }
        def repo = new Repository(name: "testRepo")
        String projectId = cloudServicesRemoteClientService.createProject(repo)
        assertNotNull "Could not create test project", projectId
        
        String serviceId = cloudServicesRemoteClientService.addSvnToProject(projectId)
        
        assertTrue "Was unable to delete test project, id=" + projectId, 
            cloudServicesRemoteClientService.deleteProject(projectId)
            
        assertNotNull "Could not add svn to the test project", serviceId
    }

    void testIsDomainAvailable() {
        if (skipTests) {
            return
        }
        def domain = csConf.domain
        boolean result = cloudServicesRemoteClientService.isDomainAvailable(domain)
        assertFalse "Domain '${domain}' should not be available", result

        domain = "${domain}334324"
        result = cloudServicesRemoteClientService.isDomainAvailable(domain)
        assertTrue "Domain '${domain}' should be availabled", result
    }

    void testIsLoginAvailable() {
        if (skipTests) {
            return
        }
        def login = csConf.username
        boolean result = cloudServicesRemoteClientService.isLoginNameAvailable(login, null)
        assertFalse "Login '${login}' should not be available", result

        login = "${login}123443"
        result = cloudServicesRemoteClientService.isLoginNameAvailable(login, null)
        assertTrue "Login '${login}' should be availabled", result
    }

    void testListUsers() {
        if (skipTests) {
            return
        }
        def remoteUsers = cloudServicesRemoteClientService.listUsers()
        def matchingUser = remoteUsers.find { remoteItem -> remoteItem.login == csConf.username}
        assertTrue "should have found our sign-in user", matchingUser.login == csConf.username
    }

    void testCreateAndDeleteUser() {
        if (skipTests) {
            return
        }
        def user = new User(username: "unitTestUser", realUserName: "unit test bits", email: "unit@test.com")
        def login = user.username
        def counter = 0
        while (!cloudServicesRemoteClientService.isLoginNameAvailable(login, null) && counter < 10) {
            login = "${user.username}${++counter}"
        }

        def result = cloudServicesRemoteClientService.createUser(user, login)
        assertTrue "the test user should have been successfully created", result

        def matchingUser = cloudServicesRemoteClientService.listUsers().find { remoteItem -> remoteItem.login == login}
        result = cloudServicesRemoteClientService.deleteUser(matchingUser.userId)
        assertTrue "the test user should have been successfully deleted", result
    }
    
    void testListChannelProducts() {
        if (skipTests) {
            return
        }
        def products = cloudServicesRemoteClientService.listChannelProducts().products
        assertEquals "SvnEdge only has one associated product", 1, products.size()
        assertEquals "Unexpected channel product", config.svnedge.cloudServices.defaultProductSKU, products[0].SKU
    }
    
    void testLoadSvnrdumpProject() {
        if (skipTests) {
            return
        }
        def tempLogDir = new File(ConfigUtil.logsDirPath(), "temp")
        def progressFile = File.createTempFile("load-progress", ".txt", tempLogDir)
        progressFile.delete()
        def repo = new Repository(name: "testRepo_source")
        String projectId = cloudServicesRemoteClientService.createProject(repo)
        assertNotNull "Could not create test project", projectId
        try {
            String serviceId = cloudServicesRemoteClientService.addSvnToProject(projectId)
            assertNotNull "Could not add svn to the test project", serviceId
            
            assertEquals "Failed to create source repository.", 0,
                    svnRepoService.createRepository(repo, false)
            repo.save(flush: true)
            
            def resource = this.class.getResource("small-repo.dump")
            File dumpFile = new File(resource.toURI())
            
            // move src dump file to the expected load location for target
            File loadDir = svnRepoService.getLoadDirectory(repo)
            // delete any residual load files
            loadDir.eachFile {
                it.delete()
            }
            File loadFile = new File(loadDir, dumpFile.name)
            loadFile.withOutputStream { out -> 
                dumpFile.withInputStream { out << it } 
            }
            
            // load it
            def options = ["progressLogFile": progressFile.absolutePath,
                           "ignoreUuid": false
                ]
            svnRepoService.loadDumpFile(repo, options)

            Locale locale = Locale.defaultLocale
            cloudServicesRemoteClientService
                    .synchronizeRepository(repo, locale)

            Repository repoTarget = new Repository(name: "testRepo_target")
            assertEquals "Failed to create target repository.", 0,
                    svnRepoService.createRepository(repoTarget, false)
            repoTarget.save(flush: true)
            
            int srcRev = svnRepoService.findHeadRev(repo)
            int targetRev = -1
            cloudServicesRemoteClientService
                    .loadSvnrdumpProject(repoTarget, projectId as int)

            int sec = 0
            while (srcRev != targetRev && sec < 30) {
                Thread.sleep(1000)
                targetRev = svnRepoService.findHeadRev(repoTarget)
                sec++
            }
            assertEquals "Target repository head revision does not match source",
                    srcRev, targetRev
        } finally {
            assertTrue "Was unable to delete test project, id=" + projectId,
                    cloudServicesRemoteClientService.deleteProject(projectId)
            progressFile.delete()
        }   
    }

    def testRetrieveSvnProjects() {
        if (skipTests) {
            return
        }
        def repo = new Repository(name: "testRepo")
        String projectId = cloudServicesRemoteClientService.createProject(repo)
        assertNotNull "Could not create test project", projectId
        repo = new Repository(name: "svnTestRepo")
        String svnProjectId = cloudServicesRemoteClientService.createProject(repo)
        assertNotNull "Could not create test svn project", svnProjectId
        try {
            String serviceId = cloudServicesRemoteClientService
                    .addSvnToProject(svnProjectId)
            assertNotNull "Could not add svn to the test project", serviceId

            def success = false
            int sec = 0
            def projectMap
            while (!success && sec < 30) {
                projectMap = cloudServicesRemoteClientService
                        .retrieveSvnProjects()
                success = projectMap.containsKey(svnProjectId as Integer) &&
                        !projectMap.containsKey(projectId as Integer)
                sec++
            }
            assertTrue "Svn project " + svnProjectId + " not found or non-svn " +
                    " project " + projectId + " was found in: " + projectMap, success

        } finally {
            assertTrue "Was unable to delete test project, id=" + projectId,
                    cloudServicesRemoteClientService.deleteProject(projectId)
            assertTrue "Was unable to delete test project, id=" + svnProjectId,
                    cloudServicesRemoteClientService.deleteProject(svnProjectId)
        }
    }
}
