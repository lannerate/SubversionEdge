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
package com.collabnet.svnedge.console.services

import grails.test.*
import grails.util.Environment

import com.collabnet.svnedge.domain.Server 
import com.collabnet.svnedge.domain.ServerMode 
import com.collabnet.svnedge.domain.integration.ApprovalState;
import com.collabnet.svnedge.domain.integration.CtfServer 
import com.collabnet.svnedge.domain.integration.ReplicaConfiguration
import com.collabnet.svnedge.domain.integration.ReplicatedRepository
import com.collabnet.svnedge.util.ConfigUtil
import com.collabnet.svnedge.replication.command.CommandTestsHelper
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import com.collabnet.svnedge.domain.Repository
import org.junit.Ignore;
import com.collabnet.svnedge.TestSSLServer

/**
 * this test class validates the configuration files being modified
 * by the ServerConfService
 */
class ServerConfServiceIntegrationTests extends GrailsUnitTestCase {

    def securityService
    def serverConfService
    def svnRepoService
    def ctfRemoteClientService
    def lifecycleService
    def setupReplicaService
    def grailsApplication
    

    protected void setUp() {
        super.setUp()
    }

    protected void tearDown() {
        super.tearDown()

    }

    /**
     * Test that the subversion server is protected against CRIME and BEAST attacks.
     * Server configuration can only protect against BEAST; CRIME protection is
     * enabled in the binaries.  But confirming both here, since the data is
     * available from the same library.
     */
    void testSvnServerProtectedBeastCrime() {

        Server server = Server.server
        server.useSsl = true
        server.save()
        // don't use restart here, as the cert data may not exist when performing
        // a graceful restart
        lifecycleService.stopServer()
        lifecycleService.startServer()

        def systemOut = System.out
        def baos = new ByteArrayOutputStream(4096)
        try {
            new PrintStream(baos, true).withStream() {
                System.out = it
                TestSSLServer.main(["localhost", "${server.port}"] as String[])
            }
        } finally {
            System.out = systemOut
        }
        def result = baos.toString()
        assertTrue("Server is not protected from BEAST SSL exploit", result.contains("BEAST status: protected"))
        assertTrue("Server is not protected from CRIME SSL exploit", result.contains("CRIME status: protected"))
        
        lifecycleService.stopServer()
    }

    void testViewvcConf() {

        // write viewvc.conf for the server in Standalone
        def server = Server.get(1)
        server.setMode(ServerMode.STANDALONE)

        serverConfService.writeConfigFiles();

        // validate expectations
        def confFile = new File(ConfigUtil.confDirPath(), "viewvc.conf")
        String viewVcConf = confFile?.text

        // verfiy file is created and all placeholder tokens are replaced
        assertNotNull("The viewvc.conf file should exist", confFile)
        assertEquals("No replacement tokens should be found", -1, viewVcConf.lastIndexOf("__CSVN"))

        // spot-check some properties
        assertTrue("root_parents should equal ${server.repoParentDir}",
            validateProperty(confFile, "root_parents", server.repoParentDir ))

        assertTrue("csvn_servermode should equal ${ServerMode.STANDALONE.toString()}",
            validateProperty(confFile, "csvn_servermode", ServerMode.STANDALONE.toString() ))

        String views = "annotate, co, diff, markup, roots"
        assertTrue("allowed_views should equal ${views}",
            validateProperty(confFile, "allowed_views", views ))

        // now regen conf in managed mode
        server.setMode(ServerMode.MANAGED)

        serverConfService.writeConfigFiles();

        // verfiy file is created and all placeholder tokens are replaced
        assertNotNull("The viewvc.conf file should exist", confFile)
        assertEquals("No replacement tokens should be found", -1, viewVcConf.lastIndexOf("__CSVN"))

        // and spot check
        assertTrue("csvn_servermode should equal ${ServerMode.MANAGED.toString()}",
            validateProperty(confFile, "csvn_servermode", ServerMode.MANAGED.toString() ))

        views = "annotate, co, diff, markup"
        assertTrue("allowed_views should equal ${views}",
            validateProperty(confFile, "allowed_views", views ))


    }

    void testSvnViewvcHttpdConf() {
        serverConfService.writeConfigFiles();

        def confFile = new File(ConfigUtil.confDirPath(), "svn_viewvc_httpd.conf")
	def content = confFile.text

	def ctfConfFileName = "ctf_httpd.conf"
	assertTrue("Missing include of ctf_httpd.conf", content.indexOf(ctfConfFileName) > 0)

        confFile = new File(ConfigUtil.confDirPath(), ctfConfFileName)
	assertTrue("Missing ctf_httpd.conf", confFile.exists())
    }

    /**
     * Test a repo url for svn server version. Confirms that the ctf test instance is 1.7,
     * while the local svn is 1.8.
     */
    void testGetSvnMasterDirectiveIfReplica() {

        Server server = Server.getServer()
        server.mode = ServerMode.REPLICA
        server.save()
        
        // evaluate CTF instance; currently svn 1.7
        def config = grailsApplication.config
        def ctfUrl = CommandTestsHelper.makeCtfBaseUrl(config)
        def svnUrl = ctfUrl + "/svn/repos/"
        def testRepo = CommandTestsHelper
            .createTestRepository(config, ctfRemoteClientService)
        def repoUrl = svnUrl + testRepo.repoName
        Repository repo = new Repository(name: testRepo.repoName)
        repo.validate()
        assertFalse "Unable to save Repository: " + repo.errors, repo.hasErrors()
        repo.save()
        ReplicatedRepository rr = new ReplicatedRepository(repo: repo, enabled: true)
        rr.validate()
        assertFalse "Unable to save ReplicatedRepository: " + rr.errors, rr.hasErrors()
        rr.save()
        
        def masterConfig = config.svnedge.ctfMaster
        CtfServer ctfServer = CtfServer.getServer()
        ctfServer.baseUrl = ctfUrl
        ctfServer.ctfUsername = masterConfig.username
        ctfServer.ctfPassword = securityService.encrypt(masterConfig.password)
        ctfServer.validate()
        assertFalse "Unable to save ctfServer: " + ctfServer.errors + '\n\n' + CtfServer.list(), ctfServer.hasErrors()
        ctfServer.save()
        
        ReplicaConfiguration rc = new ReplicaConfiguration(svnMasterUrl: svnUrl,
                name: 'foo', description: 'bar', systemId: 'exsy9999', 
                approvalState: ApprovalState.APPROVED)
        rc.validate()
        assertFalse "Unable to save ReplicaConfiguration: " + rc.errors, rc.hasErrors()
        rc.save()
        
        String v = serverConfService.getSvnMasterDirectiveIfReplica(server)
        
        // CTF 6.1.1 includes 1.7
        assertNotNull("the CTF v6.1.1 test instance should show svn 1.7 " + v, 
                v.find(~/SVNMasterVersion 1.7.0/))
        
        // test override
        File overridesConf = new File(ConfigUtil.confDirPath(), 'overrides.properties')
        overridesConf.text = "svnedge.replica.masterSvnVersion=1.7.9"
        try {
            loadConfig()
            v = serverConfService.getSvnMasterDirectiveIfReplica(server)
            assertNotNull("the overridden configuration should show should show svn 1.7.9",
                    v.find(~/SVNMasterVersion 1.7.9/))
        } finally {
            overridesConf.delete()
        }
    }

    private void loadConfig() {
        GroovyClassLoader classLoader = new GroovyClassLoader(this.class.classLoader)
        ConfigSlurper slurper = new ConfigSlurper(Environment.current.name)
        ConfigurationHolder.config = slurper.parse(classLoader.loadClass("Config"))
        def extraConfig = ConfigurationHolder.config.grails.config.locations
        for (c in extraConfig) {
            Properties p = new Properties()
            new File(new URI(c).path).withReader { p.load(it) }
            ConfigurationHolder.config.merge(slurper.parse(p))
        }
        ConfigUtil.configuration = ConfigurationHolder.config
    }

    /**        
     * the local svn is 1.8.
     */
    void testSvnServerVersion() {
        // evaluate the local SvnEdge instance version
        def testRepoName = "httpv2-test-" + Math.round(Math.random() * 1000)
        Repository repo = new Repository(name: testRepoName)
        repo.save(flush:true)
        svnRepoService.createRepository(repo, true)
        
        if (Server.getServer().mode == ServerMode.REPLICA) {
            setupReplicaService.undoReplicaModeConfiguration([], Locale.defaultLocale)
        }
        lifecycleService.restartServer()
        
        
        Server s = Server.getServer()
        def repoUrl = s.svnURL() + testRepoName
        def v = serverConfService.svnServerVersion(repoUrl, "admin", "admin")
        assertEquals("the local subversion server should show svn 1.8.0", '1.8.0', v)
        
        svnRepoService.removeRepository(repo)
        svnRepoService.deletePhysicalRepository(repo)
        
    }
    
    private boolean validateProperty(File f, String propertyName, String expectedValue) {

        boolean valueMatchFound = false
        f.eachLine {
            it -> if (it.startsWith(propertyName)) {
                valueMatchFound = it.contains(expectedValue)
            }
        }

        return valueMatchFound
    }




}
