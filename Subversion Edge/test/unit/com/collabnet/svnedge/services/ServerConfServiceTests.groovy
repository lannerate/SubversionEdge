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
package com.collabnet.svnedge.services

import com.collabnet.svnedge.admin.ServerConfService
import com.collabnet.svnedge.console.CommandLineService
import com.collabnet.svnedge.console.SecurityService
import com.collabnet.svnedge.domain.AdvancedConfiguration
import com.collabnet.svnedge.domain.LogConfiguration
import com.collabnet.svnedge.domain.NetworkConfiguration
import com.collabnet.svnedge.domain.Server
import com.collabnet.svnedge.domain.ServerMode
import com.collabnet.svnedge.domain.integration.CtfServer
import com.collabnet.svnedge.security.CsvnAuthenticationProvider
import com.collabnet.svnedge.util.ConfigUtil
import grails.test.GrailsUnitTestCase
import grails.util.Environment
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import com.collabnet.svnedge.domain.integration.ReplicaConfiguration
import com.collabnet.svnedge.domain.integration.ApprovalState
import com.collabnet.svnedge.domain.integration.ReplicatedRepository

class ServerConfServiceTests extends GrailsUnitTestCase {

    // the service to test
    def serverConfService

    // supporting resources
    NetworkConfiguration nc
    def networkingService
    def commandLineService
    def securityService

    protected void setUp() {
        super.setUp()
        loadConfig()

        mockLogging(CommandLineService)
        mockLogging(ServerConfService)

        Server s = new Server()
        s.repoParentDir = ConfigurationHolder.config.svnedge.svn.repositoriesParentPath
        s.adminEmail = "g@c.n"
        mockDomain(Server, [s])

        CtfServer ctf = new CtfServer()
        mockDomain(CtfServer, [ctf])
        CtfServer.metaClass.'static'.getServer = { ctf }
        
        mockDomain(ReplicatedRepository, [])
        mockDomain(LogConfiguration, [])
        mockDomain(AdvancedConfiguration, [])
        
        nc = new NetworkConfiguration()
        nc.httpProxyHost = "proxyhost.com"
        nc.httpProxyPort = 81
        nc.httpProxyUsername = null
        nc.httpProxyPassword = null

        networkingService = new Expando()
        networkingService.getNetworkConfiguration = { nc }
        
        def operatingSystemService = new Expando()
        operatingSystemService.isWindows = { true }
        def commandLineService = new CommandLineService()
        commandLineService.operatingSystemService = operatingSystemService
        
        serverConfService = new ServerConfService()
        serverConfService.networkingService = networkingService
        serverConfService.commandLineService = commandLineService
        serverConfService.securityService = new SecurityService()
        serverConfService.csvnAuthenticationProvider = new CsvnAuthenticationProvider()

        def grailsApplication = new Expando()
        grailsApplication.metadata = ['vendor.twitter-bootstrap.version': '2.2.1']
        serverConfService.grailsApplication = grailsApplication
    }

    protected void tearDown() {
        super.tearDown()
        CtfServer.metaClass = null
    }

    private void loadConfig() {
        GroovyClassLoader classLoader = new GroovyClassLoader(this.class.classLoader)
        ConfigSlurper slurper = new ConfigSlurper(Environment.current.name)
        ConfigurationHolder.config = slurper.parse(classLoader.loadClass("Config"))
        ConfigUtil.configuration = ConfigurationHolder.config
    }


    /**
     * This test validates that proxy settings are written appropriately to the following files in Standalone mode:
     * <ul>
     *   <li>svn_viewvc_httpd.conf</li>
     *   <li>svn_client_config/servers</li>
     * </ul>
     */
    void testProxyConfStandalone() {

        // write conf files for the server in Standalone
        def server = Server.get(1)
        server.setMode(ServerMode.STANDALONE)
        serverConfService.writeConfigFiles();

        // validate expectations svn_viewvc_httpd.conf
        def confFile = new File(ConfigUtil.confDirPath(), "svn_viewvc_httpd.conf")
        String confFileText = confFile?.text

        assertFalse("we expect no mention of proxy in svn_viewvc_httpd.conf for standalone mode",
            confFileText.contains("ProxyRemote"))

        // validate expectation of teamforge.properties
        // this file is not used in standalone mode

        // validate expectation of svn_client_config/servers
        confFile = new File(ConfigUtil.svnConfigDirPath(), "servers")
        confFileText = confFile?.text

        assertTrue("svn client config global section should exist", confFileText.contains("[global]"))
        assertTrue("svn client config http-proxy-host should have value",
                validateProperty(confFile, "http-proxy-host", nc.httpProxyHost))
        assertTrue("svn client config http-proxy-port should have value",
                validateProperty(confFile, "http-proxy-port", String.valueOf(nc.httpProxyPort)))
        assertFalse("svn client config http-proxy-username should not have value",
                validateProperty(confFile, "http-proxy-username", nc.httpProxyUsername))
        assertFalse("svn client config http-proxy-password should not have value",
                validateProperty(confFile, "http-proxy-password", nc.httpProxyPassword))

        // add username / pass to the proxy config and regenerate
        def password = "proxypass"
        nc.httpProxyUsername = "proxyuser"
        nc.httpProxyPassword = password
        serverConfService.writeConfigFiles()

        // validate expectation of svn_client_config/servers
        confFile = new File(ConfigUtil.svnConfigDirPath(), "servers")
        confFileText = confFile?.text

        assertTrue("svn client config global section should exist", confFileText.contains("[global]"))
        assertTrue("svn client config http-proxy-host should have value",
                validateProperty(confFile, "http-proxy-host", nc.httpProxyHost))
        assertTrue("svn client config http-proxy-port should have value",
                validateProperty(confFile, "http-proxy-port", String.valueOf(nc.httpProxyPort)))
        assertTrue("svn client config http-proxy-username should have value",
                validateProperty(confFile, "http-proxy-username", nc.httpProxyUsername))
        assertTrue("svn client config http-proxy-password should have value",
                validateProperty(confFile, "http-proxy-password", password))

        // remove proxy config and regenerate
        networkingService.getNetworkConfiguration = { }
        serverConfService.writeConfigFiles()

        // validate expectation of svn_client_config/servers
        confFile = new File(ConfigUtil.svnConfigDirPath(), "servers")
        confFileText = confFile?.text

        assertFalse("svn client config global section should not exist", confFileText.contains("[global]"))
        assertFalse("svn client config http-proxy-host should not have value",
                validateProperty(confFile, "http-proxy-host", nc.httpProxyHost))
        assertFalse("svn client config http-proxy-port should not have value",
                validateProperty(confFile, "http-proxy-port", String.valueOf(nc.httpProxyPort)))
        assertFalse("svn client config http-proxy-username should not have value",
                validateProperty(confFile, "http-proxy-username", nc.httpProxyUsername))
        assertFalse("svn client config http-proxy-password should not have value",
                validateProperty(confFile, "http-proxy-password", nc.httpProxyPassword))


    }

    /**
     * This test validates that proxy settings are written appropriately to the following files in TF mode:
     * <ul>
     *   <li>svn_viewvc_httpd.conf</li>
     *   <li>teamforge.properties</li>
     * </ul>
     */
    void testProxyConfIntegrationServer() {

        // setup the proxy config
        def proxyPassword = null
        nc.httpProxyPassword = proxyPassword

        CtfServer ctf = CtfServer.getServer()
        if (!ctf) {
            ctf = new CtfServer()
        }
        ctf.setBaseUrl("http://forge.collab.net")
        ctf.setCtfUsername("admin")
        ctf.setCtfPassword("admin")
        ctf.save()

        // write conf files for the server in Standalone
        def server = Server.get(1)
        server.setMode(ServerMode.MANAGED)
        serverConfService.writeConfigFiles();

        // validate expectations svn_viewvc_httpd.conf
        def confFile = new File(ConfigUtil.confDirPath(), "svn_viewvc_httpd.conf")
        String confFileText = confFile?.text

        assertFalse("we expect no mention of proxy in svn_viewvc_httpd.conf for integration server mode",
            confFileText.contains("ProxyRemote"))

        // validate expectation of teamforge.properties
        confFile = new File(ConfigUtil.confDirPath, "teamforge.properties")
        assertTrue("teamforge sfmain.integration.http_proxy_host should have value",
                validateProperty(confFile, "sfmain.integration.http_proxy_host", nc.httpProxyHost))
        assertTrue("teamforge sfmain.integration.http_proxy_port should have value",
                validateProperty(confFile, "sfmain.integration.http_proxy_port", String.valueOf(nc.httpProxyPort)))
        assertFalse("teamforge sfmain.integration.http_proxy_username should not have value",
                validateProperty(confFile, "sfmain.integration.http_proxy_username", nc.httpProxyUsername))
        assertFalse("teamforge sfmain.integration.http_proxy_password should not have value",
                validateProperty(confFile, "sfmain.integration.http_proxy_password", proxyPassword))


        // add username / pass to the proxy config and regenerate
        proxyPassword = "proxypass"
        nc.httpProxyUsername = "proxyuser"
        nc.httpProxyPassword = proxyPassword
        serverConfService.writeConfigFiles()

        // validate expectation of teamforge.properties
        confFile = new File(ConfigUtil.confDirPath, "teamforge.properties")
        assertTrue("teamforge sfmain.integration.http_proxy_host should have value",
                validateProperty(confFile, "sfmain.integration.http_proxy_host", nc.httpProxyHost))
        assertTrue("teamforge sfmain.integration.http_proxy_port should have value",
                validateProperty(confFile, "sfmain.integration.http_proxy_port", String.valueOf(nc.httpProxyPort)))
        assertTrue("teamforge sfmain.integration.http_proxy_username should have value",
                validateProperty(confFile, "sfmain.integration.http_proxy_username", nc.httpProxyUsername))
        assertTrue("teamforge sfmain.integration.http_proxy_password should have value",
                validateProperty(confFile, "sfmain.integration.http_proxy_password", proxyPassword))


        // remove proxy config and regenerate
        networkingService.getNetworkConfiguration = { }
        serverConfService.writeConfigFiles()

        // validate expectation of teamforge.properties
        confFile = new File(ConfigUtil.confDirPath, "teamforge.properties")
        assertFalse("teamforge sfmain.integration.http_proxy_host should not have value",
                validateProperty(confFile, "sfmain.integration.http_proxy_host", nc.httpProxyHost))
        assertFalse("teamforge sfmain.integration.http_proxy_port should not have value",
                validateProperty(confFile, "sfmain.integration.http_proxy_port", String.valueOf(nc.httpProxyPort)))
        assertFalse("teamforge sfmain.integration.http_proxy_username should not have value",
                validateProperty(confFile, "sfmain.integration.http_proxy_username", nc.httpProxyUsername))
        assertFalse("teamforge sfmain.integration.http_proxy_password should not have value",
                validateProperty(confFile, "sfmain.integration.http_proxy_password", proxyPassword))

    }

    /**
     * This test validates that proxy settings are written appropriately to the following files in Replica mode:
     * <ul>
     *   <li>svn_viewvc_httpd.conf</li>
     *   <li>teamforge.properties</li>
     * </ul>
     */
    void testProxyConfReplica() {

        // setup the proxy config
        def proxyPassword = null
        nc.httpProxyPassword = proxyPassword

        // mock CTF and ReplicaConfig
        CtfServer ctf = CtfServer.getServer()
        if (!ctf) {
            ctf = new CtfServer()
        }
        ctf.setBaseUrl("http://forge.collab.net")
        ctf.setCtfUsername("admin")
        ctf.setCtfPassword("admin")
        ctf.save()

        ReplicaConfiguration rc = new ReplicaConfiguration()
        mockDomain(ReplicaConfiguration, [rc])
        ReplicaConfiguration.metaClass.'static'.getCurrentConfig = { rc }

        // write conf files for the server in Standalone
        def server = Server.get(1)
        server.setMode(ServerMode.REPLICA)
        serverConfService.writeConfigFiles()

        // validate expectations svn_viewvc_httpd.conf
        def confFile = new File(ConfigUtil.confDirPath(), "svn_viewvc_httpd.conf")
        String confFileText = confFile?.text

        assertFalse("we expect no mention of proxy in svn_viewvc_httpd.conf for replica server mode before approval",
            confFileText.contains("ProxyRemote"))

        // validate expectation of teamforge.properties
        confFile = new File(ConfigUtil.confDirPath, "teamforge.properties")
        assertTrue("teamforge sfmain.integration.http_proxy_host should have value",
                validateProperty(confFile, "sfmain.integration.http_proxy_host", nc.httpProxyHost))
        assertTrue("teamforge sfmain.integration.http_proxy_port should have value",
                validateProperty(confFile, "sfmain.integration.http_proxy_port", String.valueOf(nc.httpProxyPort)))
        assertFalse("teamforge sfmain.integration.http_proxy_username should not have value",
                validateProperty(confFile, "sfmain.integration.http_proxy_username", nc.httpProxyUsername))
        assertFalse("teamforge sfmain.integration.http_proxy_password should not have value",
                validateProperty(confFile, "sfmain.integration.http_proxy_password", proxyPassword))

        // add username / pass to the proxy config and regenerate
        proxyPassword = "proxypass"
        nc.httpProxyUsername = "proxyuser"
        nc.httpProxyPassword = proxyPassword
        serverConfService.writeConfigFiles()

        // validate expectation of teamforge.properties
        confFile = new File(ConfigUtil.confDirPath, "teamforge.properties")
        assertTrue("teamforge sfmain.integration.http_proxy_host should have value",
                validateProperty(confFile, "sfmain.integration.http_proxy_host", nc.httpProxyHost))
        assertTrue("teamforge sfmain.integration.http_proxy_port should have value",
                validateProperty(confFile, "sfmain.integration.http_proxy_port", String.valueOf(nc.httpProxyPort)))
        assertTrue("teamforge sfmain.integration.http_proxy_username should have value",
                validateProperty(confFile, "sfmain.integration.http_proxy_username", nc.httpProxyUsername))
        assertTrue("teamforge sfmain.integration.http_proxy_password should have value",
                validateProperty(confFile, "sfmain.integration.http_proxy_password", proxyPassword))


        rc.setSvnMasterUrl("http://forge.collab.net/svn/repos")
        rc.approvalState = ApprovalState.APPROVED
        serverConfService.writeConfigFiles()

        // validate expectations svn_viewvc_httpd.conf
        confFile = new File(ConfigUtil.confDirPath(), "svn_viewvc_httpd.conf")
        confFileText = confFile?.text

        assertTrue("we now expect the ProxyRemote statement",
            confFileText.contains("ProxyRemote ${rc.svnMasterUrl} http://${nc.httpProxyHost}:${nc.httpProxyPort}"))

        // remove proxy config and regenerate
        networkingService.getNetworkConfiguration = { }
        serverConfService.writeConfigFiles()

        // validate expectation of teamforge.properties
        confFile = new File(ConfigUtil.confDirPath, "teamforge.properties")
        assertFalse("teamforge sfmain.integration.http_proxy_host should not have value",
                validateProperty(confFile, "sfmain.integration.http_proxy_host", nc.httpProxyHost))
        assertFalse("teamforge sfmain.integration.http_proxy_port should not have value",
                validateProperty(confFile, "sfmain.integration.http_proxy_port", String.valueOf(nc.httpProxyPort)))
        assertFalse("teamforge sfmain.integration.http_proxy_username should not have value",
                validateProperty(confFile, "sfmain.integration.http_proxy_username", nc.httpProxyUsername))
        assertFalse("teamforge sfmain.integration.http_proxy_password should not have value",
                validateProperty(confFile, "sfmain.integration.http_proxy_password", proxyPassword))

    }

    private boolean validateProperty(File f, String propertyName, String expectedValue) {

         boolean valueMatchFound = false
         f.eachLine {
             it -> if (it.startsWith(propertyName)) {
                String splitToken = (it.indexOf("=") > -1) ? "=" : " "
                def propValue = it.tokenize(splitToken).last()
                valueMatchFound = propValue?.trim().equalsIgnoreCase(expectedValue)
             }
         }
         return valueMatchFound
     }

}
