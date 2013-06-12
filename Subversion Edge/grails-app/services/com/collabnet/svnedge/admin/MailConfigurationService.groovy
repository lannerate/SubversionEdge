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
package com.collabnet.svnedge.admin

import com.collabnet.svnedge.console.AbstractSvnEdgeService
import com.collabnet.svnedge.domain.MailConfiguration
import com.collabnet.svnedge.domain.MailAuthMethod
import com.collabnet.svnedge.domain.MailSecurityMethod

import org.springframework.beans.factory.InitializingBean
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ApplicationContext
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.net.ConnectException
import java.net.SocketException
import java.net.UnknownHostException
import javax.mail.AuthenticationFailedException
import javax.mail.MessagingException

/**
 * Business logic related to setup of a mail server and sending notifications
 * of important information related to asynchronous operations
 */
class MailConfigurationService extends AbstractSvnEdgeService 
        implements InitializingBean, ApplicationContextAware {

    ApplicationContext applicationContext
    def securityService

    /**
     * @see InitializingBean#afterPropertiesSet
     * initializing bean -- after injection, we need to update the configuration
     */
    void afterPropertiesSet() {
        updateConfig(MailConfiguration.getConfiguration())
    }
    
    boolean saveMailConfiguration(MailConfiguration config) {
        boolean b = false
        config.validate()
        if (!config.hasErrors() && config.save()) {
            updateConfig(config)
            b = true
        } else {
            config.discard()
        }
        return b
    }
    
    void updateConfig(MailConfiguration dynamicConfig) {
        ConfigObject config = ConfigurationHolder.config.grails.mail
        log.debug "ConfigObject in updateConfig: " + config
        config['host'] = dynamicConfig.serverName
        config['port'] = dynamicConfig.port
        config['username'] = dynamicConfig.authUser
        config['password'] = dynamicConfig.authPass ?
                securityService.decrypt(dynamicConfig.authPass) : ''
        config['disabled'] = !dynamicConfig.enabled
        def props = [:]
        switch (dynamicConfig.securityMethod) {
            case MailSecurityMethod.NONE:
            break
            
            case MailSecurityMethod.SSL:
            props["mail.smtp.socketFactory.port"] = dynamicConfig.port as String
            props["mail.smtp.socketFactory.class"] =
                    "javax.net.ssl.SSLSocketFactory"
            props["mail.smtp.socketFactory.fallback"] = "false"
            break
            
            case MailSecurityMethod.STARTTLS:
            props["mail.smtp.starttls.enable"] = "true"
            props["mail.smtp.port"] = dynamicConfig.port as String
        }
        if (dynamicConfig.authUser && dynamicConfig.authPass) {
            props["mail.smtp.auth"] = "true"
            // this might allow kerberos, but it is not tested
            props["mail.smtp.sasl.enable"] = "true"
        } else {
            props["mail.smtp.auth"] = "false"
        }
        // if we are sending to a user and the server admin, the email should
        // be sent to the admin, even if the user address is invalid.
        props["mail.smtp.sendpartial"] = "true"

        config['props'] = props
        configureMailSession(config)
    }
    
    private void configureMailSession(ConfigObject config) {

        def ms = applicationContext.getBean('mailSender')
        ms.host = config.host ?: "localhost"
        ms.defaultEncoding = config.encoding ?: "utf-8"
        ms.port = config.port
        ms.username = config.username ?: null
        ms.password = config.password ?: null
        if (config.protocol)
            ms.protocol = config.protocol
        if (config.props instanceof Map && config.props)
            ms.javaMailProperties = config.props
    }
    
    /**
     * Asynchronously sends an email to the given address with the given 
     * subject and body. Mail is sent async since it is testing the 
     * mail server configuration and could take a long time, if the server
     * is misconfigured.
     * 
     * @return Future 
     */
    def sendTestMail(String toAddress, String mailSubject, String mailBody) {
        MailConfiguration config = MailConfiguration.getConfiguration()    
        return callAsync {
            try {
                sendMail {
                    to toAddress
                    from config.createFromAddress()
                    subject mailSubject
                    body mailBody
                }
                
            } catch (Exception e) {
                log.debug("Caught Exception when testing mail server settings: "
                          + e.getClass(), e)
                while (e.cause) {
                    log.debug e.cause.message
                    e = e.cause
                }
                
                def error
                switch (e) {
                    case ConnectException:
                    error = [code: "server.action.testMail.connectException"]
                    break
                    
                    case UnknownHostException:
                    error = [code: "server.action.testMail.unknownHostException"]
                    break

                    case SocketException:
                    error = [code: "server.action.testMail.socketException",
                             args: [e.class, e.message]]
                    break

                    case AuthenticationFailedException:
                    error = [code:"server.action.testMail.authenticationFailedException"]
                    break
                    
                    case MessagingException:
                    error = [code: "server.action.testMail.messagingException",
                             args: [e.class, e.message]]
                    break
                    
                    default:
                    if (e.message.toLowerCase().contains("authentication")) {
                        error = [code: "server.action.testMail.maybeAuthenticationFailedException",
                                 args: [e.class, e.message]]
                    } else {
                        error = [code: "server.action.testMail.unknownException",
                                 args: [e.class, e.message]]
                    }
                }
                return error
            }
            return null
        }
    }
}
