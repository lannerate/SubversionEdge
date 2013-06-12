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


import com.collabnet.svnedge.domain.MailConfiguration 
import grails.test.GrailsUnitTestCase;

/**
 * Unit tests for MailConfiguration domain object
 */
class MailConfigurationTests extends GrailsUnitTestCase {

    protected void setUp() {
        super.setUp()
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testFromAddress() {
        MailConfiguration mailConfig = new MailConfiguration(enabled: true, 
                serverName: 'serverName.com', securityMethod: null, port: 3025)
        assertEquals "Without authUser; From address is not correct", 
                'SubversionEdge@serverName.com', mailConfig.createFromAddress()
        mailConfig.authUser = 'simpleUsername'
        assertEquals "With authUser; From address is not correct",
                mailConfig.authUser + '@' + mailConfig.serverName,
                mailConfig.createFromAddress()
        mailConfig.authUser = 'emailUsername@notServerName.com'
        assertEquals "With email authUser; From address is not correct",
                mailConfig.authUser, mailConfig.createFromAddress()
    }
    
    void testConstraints() {

        // mock the grails domain class MailConfiguration
        mockForConstraintsTests(MailConfiguration, [])

        // additional mock validation rule (mockForConstraints() doesn't handle this)
        //MailConfiguration.metaClass.'static'.findByNameIlike = { null }

        // default config is valid
        def mailConfig = new MailConfiguration() //serverName: null)
        assertTrue "the MailConfiguration domain class should allow unset fields, if enabled=false ", 
                mailConfig.validate()
 
        mailConfig = new MailConfiguration(enabled: true)
        assertTrue "default config is port 25 on localhost, no auth or security.  this should be a valid enabled configuration.", 
                mailConfig.validate()
        
        
        mailConfig = new MailConfiguration(enabled: true, serverName: null, securityMethod: null, port: 0)
        mailConfig.validate()
        assertEquals "the MailConfiguration domain class should NOT ALLOW 'blank' mail server name", "blank", mailConfig.errors["serverName"]
        assertEquals "the MailConfiguration domain class should NOT ALLOW port 0", "range", mailConfig.errors["port"]
        assertEquals "the MailConfiguration domain class should NOT ALLOW 'null' security method", "nullable", mailConfig.errors["securityMethod"]
        assertNull "the MailConfiguration domain class should ALLOW 'null' authUser", mailConfig.errors["authUser"]
        assertNull "the MailConfiguration domain class should ALLOW 'null' authPass", mailConfig.errors["authPass"]
        assertNull "the MailConfiguration domain class should ALLOW 'null' fromAddress", mailConfig.errors["fromAddress"]
        
        mailConfig.fromAddress = "not.an.email.address"
        mailConfig.repoSyncToAddress = "not.an.email.address.com"
        mailConfig.validate()
        assertEquals "the MailConfiguration domain class fromAddress must be an email address", "email", mailConfig.errors["fromAddress"]
        assertEquals "the MailConfiguration domain class fromAddress must be an email address", "email", mailConfig.errors["repoSyncToAddress"]
    }
}
