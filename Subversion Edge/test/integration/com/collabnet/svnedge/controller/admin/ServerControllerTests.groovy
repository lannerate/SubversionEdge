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
package com.collabnet.svnedge.controller.admin

import javax.mail.Message;

import com.collabnet.svnedge.controller.AbstractSvnEdgeControllerTests;
import com.collabnet.svnedge.domain.MonitoringConfiguration
import com.collabnet.svnedge.domain.Server 
import com.collabnet.svnedge.domain.integration.CtfServer 
import com.collabnet.svnedge.util.ConfigUtil

import grails.test.*
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import com.icegreen.greenmail.util.GreenMailUtil
import com.icegreen.greenmail.util.ServerSetupTest

class ServerControllerTests extends AbstractSvnEdgeControllerTests {

    def operatingSystemService
    def lifecycleService
    def mailConfigurationService
    def networkingService
    def serverConfService
    def grailsApplication
    def config
    def greenMail

    protected void setUp() {
        super.setUp()

        // mock the bindData method
        controller.metaClass.bindData = { obj, params, excludes = [] ->
            obj.properties = params
        }

        this.config = grailsApplication.config
        controller.lifecycleService = lifecycleService
        controller.networkingService = networkingService
        controller.serverConfService = serverConfService
        controller.mailConfigurationService = mailConfigurationService
        ConfigurationHolder.config = grailsApplication.config
    }

    protected void tearDown() {
        super.tearDown()
        greenMail.deleteAllMessages()
    }

    void testIndex() {
        controller.index()
    }
    
    void testEdit() {
        controller.edit()
    }
    
    private def defaultParams() {
        def params = controller.params
        params.hostname = "localhost"
        params.port = "80"
        params.repoParentDir = "/tmp"
        params.adminName ="Nobody"
        params.adminEmail = "devnull@example.com"
        params
    }
    
    void testUpdate() {
        lifecycleService.stopServer()
        def params = defaultParams()
        params.port = "7652"
        controller.update()

        File f = new File(config.svnedge.svn.dataDirPath, "conf/csvn_main_httpd.conf")
        assertTrue "${f.absolutePath} does not exist", f.exists()
        assertEquals "DB record not updated for port", 7652, Server.getServer().port
        assertTrue "Port directive was not updated.", (f.text.indexOf("Listen 7652") > 0)
    }
    
    void testEditAuthentication() {
        controller.editAuthentication()
    }

    void testEditIntegration() {
        if (!CtfServer.getServer()) {

            CtfServer s = new CtfServer(baseUrl: "http://ctf", mySystemId: "exsy1000",
                    internalApiKey: "testApiKey",
                    ctfUsername: "myCtfUser",
                    ctfPassword: "encrypted")
            if (!s.validate()) {
                s.errors.each { println(it)}
            }
            s.save(flush:true)
        }
        controller.editIntegration()
    }
    
    void testEditMail() {
        def model = controller.editMail()
        assertFalse "Mail should start off disabled", model.config.enabled    
    }
    
    void testUpdateMailSuccess() {
        //controller.metaClass.loggedInUserInfo = { return 1 }
        def params = controller.params
        params['enabled'] = true
        controller.updateMail()
        assertEquals "Expected redirect to 'editMail' view on success", 
                'editMail', controller.redirectArgs["action"]
        assertNotNull "Controller should provide a success message",
                controller.flash.message
        assertNull "Controller should NOT provide an error message",
                controller.flash.error
    }
    
    void testUpdateMailFail() {        
        def params = controller.params
        params['enabled'] = true
        // an invalid email address
        params['fromAddress'] = "not.an.address"
        controller.updateMail()
        assertEquals "should not redirect on failure due to invalid address",
                0, controller.redirectArgs.size()
        assertNull "Controller should NOT provide a success message",
                controller.flash.message
        assertNotNull "Controller should provide an error message",
                controller.request.error
    }
    
    void testTestMailSuccess() {
        def params = controller.params
        params['enabled'] = true
        params['port'] = ServerSetupTest.SMTP.port

        controller.testMail()

        assertEquals "Expected redirect to 'editMail' view on success",
                'editMail', controller.redirectArgs["action"]
        assertNotNull "Controller should provide a success message",
                controller.flash.message
        assertNull "Controller should NOT provide an error message",
                controller.flash.error

        assertEquals("Expected one mail message", 1,
                greenMail.getReceivedMessages().length)
        def message = greenMail.getReceivedMessages()[0]
        assertEquals("Message Subject did not match",
                "Test email from Subversion Edge", message.subject)
        assertTrue("Message Body did not match ", GreenMailUtil.getBody(message)
                .startsWith("Mail server settings are valid."))
        assertEquals("Message From did not match", "SubversionEdge@localhost",
                GreenMailUtil.getAddressList(message.from))
        assertEquals("Message To did not match", Server.getServer().adminEmail,
                GreenMailUtil.getAddressList(
                message.getRecipients(Message.RecipientType.TO)))
        assertNull("Message not expected to have CC recipients",
                GreenMailUtil.getAddressList(
                message.getRecipients(Message.RecipientType.CC)))
    }

    void testTestMailFail() {
        def params = controller.params
        params['enabled'] = true
        params['port'] = 61332
        controller.testMail()
        assertEquals "should not redirect on failure due to invalid port",
                0, controller.redirectArgs.size()
        assertNull "Controller should NOT provide a success message",
                controller.flash.message
        assertNotNull "Controller should provide an error message",
                controller.request.error
    }
    
    void testEditMonitoring() {
        def model = controller.editMonitoring()
        def networkInterfaces = model['networkInterfaces']
        assertNotNull "Expect NICs", networkInterfaces
        assertTrue "There should be at least one NIC", networkInterfaces.size() > 0
        assertTrue "Server must have a loopback NI", 
                networkInterfaces.contains('lo') || networkInterfaces.contains('lo0')
        def ipAddresses = model['ipv4Addresses']
        assertNotNull "Expect IP addresses", ipAddresses
        assertTrue "There should be at least one IP address", ipAddresses.size() > 0
        assertTrue "Expect lo IP address", 
                ipAddresses.collect({ it.getHostAddress() }).contains("127.0.0.1")
        MonitoringConfiguration config = model['config']
        assertTrue "Network statistics should be enabled", config.networkEnabled
        assertTrue "Disk statistics should be enabled", config.repoDiskEnabled
    }
    
    void testUpdateMonitoring() {
        def params = controller.params
        params.networkEnabled = false
        params.repoDiskEnabled = true
        params.frequency = 'ONE_HOUR'
        params.repoDiskFrequencyHours = '8'
        controller.updateMonitoring()
        assertEquals "Expected redirect to 'editMonitoring' view on success",
                'editMonitoring', controller.redirectArgs["action"]
        assertNotNull "Controller should provide a success message",
                controller.flash.message
        assertNull "Controller should NOT provide an error message",
                controller.flash.error

        def model = controller.editMonitoring()
        MonitoringConfiguration config = model['config']
        assertFalse "Network statistics should be disabled", 
                config.networkEnabled
        assertEquals "Frequency should be every X hours", 
                MonitoringConfiguration.Frequency.ONE_HOUR, config.frequency
        assertEquals "Frequency should be every 8 hours", 8, 
                config.repoDiskFrequencyHours
    }
    
    void testUpdateAdvancedMaximum() {
        def params = controller.params
        params.autoVersioning = true
        params.compressionLevel = 9
        params.allowBulkUpdates = true
        params.preferBulkUpdates = true
        params.useUtf8 = true
        params.hooksEnv = 'LANG=en_US.UTF-8'
        params.listParentPath = true
        params.pathAuthz = true
        params.strictAuthz = false
        params.svnBasePath = '/subversion/context'
        params.inMemoryCacheSize = 64
        params.cacheFullTexts = true
        params.cacheTextDeltas = true
        params.cacheRevProps = true
        controller.updateAdvanced()
        
        assertEquals "Expected redirect to 'advanced' view on success",
        'advanced', controller.redirectArgs["action"]
        assertNotNull "Controller should provide a success message",
                controller.flash.message
        assertNull "Controller should NOT provide an error message",
                controller.flash.error

        def expectedServerDirectivesMap = ['SVNCompressionLevel 9': false,
                'SVNUseUTF8 On': false]
        expectedServerDirectivesMap['SVNInMemoryCacheSize ' + (64 * 1024)] = false
        def expectedLocationDirectivesMap = ['SVNAutoversioning On': false, 
                'SVNAllowBulkUpdates Prefer': false,
                'SVNHooksEnv LANG=en_US.UTF-8': false,
                'SVNListParentPath On': false,
                'SVNPathAuthz short_circuit': false,
                'SVNCacheFullTexts On': false,
                'SVNCacheTextDeltas On': false,
                'SVNCacheRevProps On': false,
        ]
        def expectedServerDirectives = expectedServerDirectivesMap.keySet()
        def expectedLocationDirectives = expectedLocationDirectivesMap.keySet()
        // verify the apache conf update
        def confFile = new File(ConfigUtil.confDirPath(), "svn_viewvc_httpd.conf")
        boolean foundLocation = false
        boolean endLocation = false
        confFile.eachLine { it ->
            if (!endLocation) {
                it = it.trim()
                if (foundLocation) {
                    if (it == '</Location>') {
                        endLocation = true
                    } else {
                        assertFalse "Location directive found outside of block: " + it,
                                expectedServerDirectives.contains(it)
                        if (expectedLocationDirectives.contains(it)) {
                            expectedLocationDirectivesMap[it] = true
                        }
                    }
                } else {
                    if (it == "<Location ${params.svnBasePath}/>") {
                        foundLocation = true
                    } else {
                        assertFalse "Server directive found inside location block: " + it,
                                        expectedLocationDirectives.contains(it)
                        if (expectedServerDirectives.contains(it)) {
                            expectedServerDirectivesMap[it] = true
                        }
                    }
                }
            }
        }
        assertTrue("Apache conf should contain Location for svn context " + confFile.text, foundLocation)
        assertTrue("Apache conf should terminate Location for svn context", endLocation)
        for (entry in expectedServerDirectivesMap.entrySet()) {
            assertTrue "Did not find expected server directive: " + entry.key, entry.value
        }
        for (entry in expectedLocationDirectivesMap.entrySet()) {
            assertTrue "Did not find expected location directive: " + entry.key, entry.value
        }
        
        assertEquals "Should be able to start server", 0, lifecycleService.startServer()
        assertEquals "Should be able to stop server", 0, lifecycleService.stopServer()
    }
    
    void testUpdateAdvancedMinimum() {
        def params = controller.params
        params.autoVersioning = false
        params.compressionLevel = 0
        params.allowBulkUpdates = false
        params.preferBulkUpdates = false
        params.useUtf8 = false
        params.hooksEnv = ''
        params.listParentPath = false
        params.pathAuthz = false
        params.strictAuthz = false
        params.svnBasePath = '/svn'
        params.inMemoryCacheSize = 0
        params.cacheFullTexts = false
        params.cacheTextDeltas = false
        params.cacheRevProps = false
        controller.updateAdvanced()
        
        assertEquals "Expected redirect to 'advanced' view on success",
        'advanced', controller.redirectArgs["action"]
        assertNotNull "Controller should provide a success message",
                controller.flash.message
        assertNull "Controller should NOT provide an error message",
                controller.flash.error

        def expectedServerDirectivesMap = ['SVNCompressionLevel 0': false,
                'SVNInMemoryCacheSize 0': false]
        def expectedLocationDirectivesMap = ['SVNAllowBulkUpdates Off': false,
                'SVNPathAuthz Off': false
        ]
        def unexpectedDirectives = ['SVNAutoversioning', 'SVNHooksEnv',
                'SVNListParentPath', 'SVNCacheFullTexts', 'SVNCacheTextDeltas',
                'SVNCacheRevProps', 'SVNUseUTF8', 'SVNHooksEnv'
        ]
        def expectedServerDirectives = expectedServerDirectivesMap.keySet()
        def expectedLocationDirectives = expectedLocationDirectivesMap.keySet()
        // verify the apache conf update
        def confFile = new File(ConfigUtil.confDirPath(), "svn_viewvc_httpd.conf")
        println confFile.text
        boolean foundLocation = false
        boolean endLocation = false
        confFile.eachLine { it ->
            if (!endLocation) {
                it = it.trim()
                for (unexpectedDirective in unexpectedDirectives) {
                    assertFalse unexpectedDirective + " should not exist in conf",
                         it.startsWith(unexpectedDirective)
                }
                if (foundLocation) {
                    if (it == '</Location>') {
                        endLocation = true
                    } else {
                        assertFalse "Location directive found outside of block: " + it,
                                expectedServerDirectives.contains(it)
                        if (expectedLocationDirectives.contains(it)) {
                            expectedLocationDirectivesMap[it] = true
                        }
                    }
                } else {
                    if (it == "<Location ${params.svnBasePath}/>") {
                        foundLocation = true
                    } else {
                        assertFalse "Server directive found inside location block: " + it,
                                        expectedLocationDirectives.contains(it)
                        if (expectedServerDirectives.contains(it)) {
                            expectedServerDirectivesMap[it] = true
                        }
                    }
                }
            }
        }
        assertTrue("Apache conf should contain Location for svn context " + confFile.text, foundLocation)
        assertTrue("Apache conf should terminate Location for svn context", endLocation)
        for (entry in expectedServerDirectivesMap.entrySet()) {
            assertTrue "Did not find expected server directive: " + entry.key, entry.value
        }
        for (entry in expectedLocationDirectivesMap.entrySet()) {
            assertTrue "Did not find expected location directive: " + entry.key, entry.value
        }
        
        assertEquals "Should be able to start server", 0, lifecycleService.startServer()
        assertEquals "Should be able to stop server", 0, lifecycleService.stopServer()
    }
}
