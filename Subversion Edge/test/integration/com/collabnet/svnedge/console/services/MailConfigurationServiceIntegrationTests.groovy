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

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.mail.Message
import javax.mail.internet.MimeMultipart

import grails.test.GrailsUnitTestCase

import com.collabnet.svnedge.TestUtil
import com.collabnet.svnedge.domain.MailConfiguration;
import com.collabnet.svnedge.domain.Server
import com.collabnet.svnedge.domain.User;
import org.codehaus.groovy.grails.commons.ConfigurationHolder;

import com.icegreen.greenmail.util.GreenMailUtil
import com.icegreen.greenmail.util.ServerSetupTest

class MailConfigurationServiceIntegrationTests extends GrailsUnitTestCase {

    private static final int ADMIN_USER_ID = 1

    def mailConfigurationService
    def greenMail
    def grailsApplication
    MailConfiguration mailConfig
    
    protected void setUp() {
        super.setUp()

        Server server = Server.getServer()
        server.adminEmail = 'testAdminMail@example.com'
        server.save()
        User user = User.get(ADMIN_USER_ID)
        user.email = "testUserMail@example.com"
        user.save()
        
        ConfigurationHolder.config = grailsApplication.config
        mailConfig = new MailConfiguration(
                serverName: 'localhost',
                port: ServerSetupTest.SMTP.port,
                enabled: true)
        mailConfigurationService.saveMailConfiguration(mailConfig)
    }

    protected void tearDown() {
        super.tearDown()
        greenMail.deleteAllMessages()
    }

    public void testSuccessfulTestEmail() {
        def future = mailConfigurationService
                .sendTestMail('testuser@testmail.com', "Test Subject", "Test Body")
        assertContents(future) {
            assertEquals("Expected one mail message", 1,
                greenMail.getReceivedMessages().length)
            def message = greenMail.getReceivedMessages()[0]
            assertEquals("Message Subject did not match",
                    "Test Subject", message.subject)
            assertEquals("Message Body did not match",
                    "Test Body", GreenMailUtil.getBody(message))
            assertEquals("Message From did not match", "SubversionEdge@localhost",
                    GreenMailUtil.getAddressList(message.from))
            assertEquals("Message To did not match", 'testuser@testmail.com',
                    GreenMailUtil.getAddressList(
                    message.getRecipients(Message.RecipientType.TO)))
            assertNull("Message not expected to have CC recipients",
                    GreenMailUtil.getAddressList(
                    message.getRecipients(Message.RecipientType.CC)))            
        }
    }
    
    public void testUsernameIsEmail() {
        MailConfiguration mailConfig = MailConfiguration.configuration
        mailConfig.authUser = 'joe.smith@example.com'
        mailConfigurationService.saveMailConfiguration(mailConfig)
        
        def future = mailConfigurationService
                .sendTestMail('testuser@testmail.com', "Test Subject", "Test Body")
        assertContents(future) {
            assertEquals("Expected one mail message", 1,
                    greenMail.getReceivedMessages().length)
            def message = greenMail.getReceivedMessages()[0]
            assertEquals("Message Subject did not match",
                    "Test Subject", message.subject)
            assertEquals("Message Body did not match",
                    "Test Body", GreenMailUtil.getBody(message))
            assertEquals("Message From did not match", "joe.smith@example.com",
                    GreenMailUtil.getAddressList(message.from))
            assertEquals("Message To did not match", 'testuser@testmail.com',
                    GreenMailUtil.getAddressList(
                    message.getRecipients(Message.RecipientType.TO)))
            assertNull("Message not expected to have CC recipients",
                    GreenMailUtil.getAddressList(
                    message.getRecipients(Message.RecipientType.CC)))
        }
    }
    
    private void assertContents(future, assertions) {
        try {
            def result = future.get(5, TimeUnit.SECONDS)
            if (result) {
                fail('Test email returned a result: ' + result)
            } else {
                assertions()
            }
        } catch (CancellationException e) {
            fail("Test email async execution was cancelled")
        } catch (TimeoutException e) {
            fail('Test email was not sent within time limit')
        }
    }
}
