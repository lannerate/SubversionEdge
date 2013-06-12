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
package com.collabnet.svnedge.console.services;

import grails.test.GrailsUnitTestCase;

import java.util.Locale;

import com.collabnet.svnedge.domain.Repository 
import com.collabnet.svnedge.domain.Server 
import com.collabnet.svnedge.domain.ServerMode 
import com.collabnet.svnedge.integration.CtfConversionBean 

class SetupTeamForgeServiceIntegrationTests extends GrailsUnitTestCase {

    def grailsApplication
    def ctfRemoteClientService
    def setupTeamForgeService
    def svnRepoService    
    def config

    protected void setUp() {
        super.setUp()
        this.config = grailsApplication.config
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testProjectExists() {
        def usr = config.svnedge.ctfMaster.username
        def pwd = config.svnedge.ctfMaster.password
        def ctfProtocol = config.svnedge.ctfMaster.ssl ? "https://" : "http://"
        def ctfURL = ctfProtocol + config.svnedge.ctfMaster.domainName

        def sessionId = ctfRemoteClientService.login(ctfURL, usr, pwd, 
            Locale.getDefault())

        CtfConversionBean ctfProps = new CtfConversionBean(ctfURL: ctfURL, 
            soapSessionId: sessionId)

        assertTrue "Project look should exist.", 
            setupTeamForgeService.projectExists(ctfProps, "look") == "look"
    }
    
    void skip_testRegisterIntegrationServer() {
        def server = Server.getServer()
        //server.hostname = "cu113.cubit.sp.collab.net"
        println "Registering cu052"
        def result = registerIntegrationServer(server)
        def conversionData = result.bean
        def systemId = result.systemId
        assertNotNull systemId
        println "SystemId=" + systemId
        
        // cleanup
        println "Deleting cu052"
        def conn = setupTeamForgeService
            .openPostUrl(conversionData.ctfURL + 
            "/sf/sfmain/do/selectSystems;jsessionid=" + 
            conversionData.webSessionId,
            [_listItem: systemId,
             sfsubmit: "delete"], false)
        
        setupTeamForgeService.debugHeaders(conn)
        println conn.inputStream.text
    }

    private def registerIntegrationServer(server) {
        // server singleton will be used in the service
        CtfConversionBean conversionData = new CtfConversionBean(
            ctfURL:(config.svnedge.ctfMaster.ssl ? 
                "https://" : "http://") + config.svnedge.ctfMaster.domainName, 
            ctfUsername:config.svnedge.ctfMaster.username, 
            ctfPassword:config.svnedge.ctfMaster.password)
        return [bean: conversionData, 
         systemId: setupTeamForgeService
         .registerIntegrationServer(conversionData)]
    }
    
    void skip_testAddReposToProjects() {
        def suffix = String.valueOf(System.currentTimeMillis())
        def projectName = "p" + suffix
        CtfConversionBean conversionData = new CtfConversionBean(
                        ctfURL: "http://cu231.cubit.sp.collab.net",
                        ctfUsername: "admin",
                        ctfPassword: "!Q1q1q1q",
                        ctfProject: projectName,
                        exSystemId: "exsy1002")
        
        saveTestRepos(suffix)
        setupTeamForgeService.addReposToProjects(conversionData)
        
        // cleanup - TODO Delete repos 
        //def url = "/sf/scm/do/selectRepository/projects." + projectName + "/scm"
        /*
        def conn = setupTeamForgeService.openPostUrl(url + ";jsessionid=" + 
         need to login?   conversionData.webSessionId,
                        [_listItem: systemId,
                        sfsubmit: "Delete"], false)
        */
    }
    
    private void saveTestRepos(suffix) {
        Repository repo = new Repository(name: "r1_" + suffix)
        svnRepoService.createRepository(repo, true)
        repo.save()
        repo = new Repository(name: "r2_" + suffix)
        svnRepoService.createRepository(repo, true)
        repo.save()        
    }
    
    void skip_testAddServerAndRepos() {
        def server = Server.getServer()
        println "Registering localhost"
        def result = registerIntegrationServer(server)
        def conversionData = result.bean
        assertNotNull result.systemId
        conversionData.exSystemId = result.systemId
                
        def suffix = String.valueOf(System.currentTimeMillis())
        def projectName = "p" + suffix
        conversionData.ctfProject = projectName
        svnRepoService.syncRepositories()
        setupTeamForgeService.addReposToProjects(conversionData)
        server.mode = ServerMode.MANAGED
        server.save()
    }

    /**
     * This test can be made to work once, but is not repeatable against the
     * same ctf host.
     */
    void manual_testConvert() {
        svnRepoService.syncRepositories()
        CtfConversionBean conversionData = new CtfConversionBean(
            ctfURL:(config.svnedge.ctfMaster.ssl ? 
                "https://" : "http://") + config.svnedge.ctfMaster.domainName, 
            ctfUsername:config.svnedge.ctfMaster.username, 
            ctfPassword:config.svnedge.ctfMaster.password)
        def suffix = String.valueOf(System.currentTimeMillis())
        def projectName = "p" + suffix
        conversionData.ctfProject = projectName
        setupTeamForgeService.convert(conversionData)
        assertNotNull conversionData.exSystemId
        def server = Server.getServer()
        assertEquals "Should be in managed mode", ServerMode.MANAGED,
            server.mode
    }
}
