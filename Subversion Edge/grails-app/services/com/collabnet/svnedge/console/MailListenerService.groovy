/*
 * CollabNet Subversion Edge
 * Copyright (C) 2012, CollabNet Inc. All rights reserved.
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
package com.collabnet.svnedge.console

import com.collabnet.svnedge.console.AbstractSvnEdgeService
import com.collabnet.svnedge.domain.MailConfiguration
import com.collabnet.svnedge.domain.MailAuthMethod
import com.collabnet.svnedge.domain.MailSecurityMethod
import com.collabnet.svnedge.domain.Server
import com.collabnet.svnedge.domain.User
import com.collabnet.svnedge.event.LoadRepositoryEvent
import com.collabnet.svnedge.event.LoadCloudRepositoryEvent
import com.collabnet.svnedge.event.RepositoryEvent
import com.collabnet.svnedge.event.DumpRepositoryEvent
import com.collabnet.svnedge.event.VerifyRepositoryEvent

import org.springframework.beans.factory.InitializingBean
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ApplicationContext
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.context.ApplicationListener

import grails.util.GrailsUtil;

import java.net.ConnectException
import java.net.SocketException
import java.net.UnknownHostException
import javax.mail.AuthenticationFailedException
import javax.mail.MessagingException

import com.collabnet.svnedge.admin.RepoVerifyJob
import com.collabnet.svnedge.event.SyncReplicaRepositoryEvent

/**
 * The listener for events which can result in a mail notification. 
 * 
 * Implemented as a separate class from MailConfigurationService as that service
 * requires use of the afterPropertiesSet method which throws an error related
 * to the domain object not having its grails methods attached yet,
 * if transactional = false, but the proxy which is created when 
 * transactional = true, causes 2 listener registrations
 */
class MailListenerService extends AbstractSvnEdgeService
    implements ApplicationListener<RepositoryEvent> {

    // Default sample addresses shipped with the product
    private def INVALID_ADDRESSES = ['admin@example.com', 'devnull@collab.net']
        
    // this is needed so grails does not create a proxy resulting in 
    // registering the listener twice
    static transactional = false

    def ldapService
    def svnRepoService
    
    void onApplicationEvent(RepositoryEvent event) {
        MailConfiguration config = MailConfiguration.getConfiguration()
        if (config.enabled) {
            onEnabledMail(event, config)
        } else {
            log.debug "Email notifications are disabled."
        }
    }

    private void onEnabledMail(SyncReplicaRepositoryEvent event, MailConfiguration config) {
        def toAddress = config.repoSyncToAddress
        if (toAddress && !event.isSuccess) {
            def fromAddress = config.createFromAddress()
            sendSyncReplicaRepoMail(toAddress, fromAddress, event)
        }
    }
    
    private void onEnabledMail(RepositoryEvent event, MailConfiguration config) {
        def fromAddress = config.createFromAddress()
        Server server = Server.getServer()
        def defaultAddress = !INVALID_ADDRESSES.contains(server.adminEmail) ? 
                server.adminEmail : null
        def toAddress = defaultAddress 
        def ccAddress = null
        boolean sendOnSuccess = false
        boolean userLacksEmail = false
        User user = retrieveUserForEvent(event)
        if (user) {
            sendOnSuccess = true
            def userAddress = user.isLdapUser() ? 
                    ldapService.getEmailForLdapUser(user) : user.email
            if (userAddress && !INVALID_ADDRESSES.contains(userAddress)) {
                toAddress = userAddress
                if (toAddress != defaultAddress && !event.isSuccess) {
                    ccAddress = defaultAddress
                }
            } else {
                userLacksEmail = true
            }
        }
        
        // don't send email to server admin unless an error occurs
        if (toAddress && (sendOnSuccess || !event.isSuccess)) {
            switch (event) {
                case DumpRepositoryEvent:
                    sendDumpMail(toAddress, ccAddress, fromAddress, event, 
                                 userLacksEmail, user?.username)
                break
                case LoadRepositoryEvent:
                    sendLoadMail(toAddress, ccAddress, fromAddress, event, 
                                 userLacksEmail, user?.username)
                break
                case VerifyRepositoryEvent:
                    sendVerifyMail(toAddress, ccAddress, fromAddress, event,
                                   userLacksEmail, user?.username)
                break
            }
            
            // we've sent an email, so clean up the process log
            def processOutput = getProcessOutput(event)
            if (processOutput && !isPartial(processOutput)) {
                event.processOutput.delete()
            }
        }
    }

    private static final long MAX_ATTACHMENT_SIZE = 104858
    
    private byte[] getProcessOutput(RepositoryEvent event) {
        File f = event.processOutput
        byte[] s = null
        if (f?.exists()) {
            if (f.length() < MAX_ATTACHMENT_SIZE) {
                s = f.bytes
            } else {
                long skipBytes = f.length() - MAX_ATTACHMENT_SIZE
                s = new byte[MAX_ATTACHMENT_SIZE]
                f.withInputStream {
                    it.skip(skipBytes)
                    it.read(s)
                }
            }
        }
        return s
    }

    private static final int MAX_TAIL_SIZE = 500
    
    private String getProcessOutputTail(RepositoryEvent event) {
        File f = event.processOutput
        String s = null
        if (f?.exists()) {
            s = f.text
            if (s.length() > MAX_TAIL_SIZE) {
                s = s.substring(s.length() - MAX_TAIL_SIZE, s.length())
            }
        }
        return s
    }
        
    private void sendDumpMail(toAddress, ccAddress, fromAddress, event, 
                              userLacksEmail, username) {
        def repo = event.repo
        def dumpBean = event.dumpBean
        Locale locale = dumpBean.userLocale
        def mailSubject = getMessage(event.isSuccess ? 
                'mail.message.subject.success' : 'mail.message.subject.error',
                null, locale)
        mailSubject += getMessage(dumpBean.isBackup() ? 
                'mail.message.dump.subject.backup' : 
                'mail.message.dump.subject.adhoc', null, locale)
        mailSubject += getMessage('mail.message.repository', [repo.name], locale)
       
        def mailBody
        byte[] processOutput
        if (event.isSuccess) {
            def filename = svnRepoService.dumpFilename(dumpBean, repo)
            Server server = Server.getServer()
            def urlPrefix = server.consoleUrlPrefix()
            def repoLink = urlPrefix + '/repo/dumpFileList/' + repo.id
            def downloadLink = urlPrefix + '/repo/downloadDumpFile/' + repo.id +
                    "?filename=" + filename
            mailBody = getMessage('mail.message.dump.body.success', 
                [(dumpBean.hotcopy ? 1 : 0), repo.name, 
                 filename, repoLink, downloadLink, 
                 (userLacksEmail ? 1 : 0), username], locale)
        } else {
            processOutput = getProcessOutput(event)
            def processOutputTail = getProcessOutputTail(event)
            if (event.exception) {
                def e = event.exception
                GrailsUtil.deepSanitize(e)
                mailBody = getMessage('mail.message.dump.body.error',
                        [dumpBean.hotcopy ? 1 : dumpBean.cloud ? 2 : 0, 
                         dumpBean.backup ? 0 : 1, 
                         repo.name, e.message,
                         e.class.name, getStackTrace(e),
                         processOutput ? 1 : 0,
                         isPartial(processOutput) ? 1 : 0,
                         event.processOutput?.name, processOutputTail, 
                         (userLacksEmail ? 1 : 0), username], locale)
            } else {
                mailBody = getMessage('mail.message.dump.body.error',
                        [dumpBean.hotcopy ? 1 : dumpBean.cloud ? 2 : 0, 
                         dumpBean.backup ? 0 : 1, 
                         repo.name, '', '', '', processOutput ? 1 : 0,
                         isPartial(processOutput) ? 1 : 0,
                         event.processOutput?.name, processOutputTail, 
                         (userLacksEmail ? 1 : 0), username], locale)
            }
        }
        mailBody += getMessage('mail.message.footer', null, locale)
        sendMail(toAddress, ccAddress, fromAddress, 
                 mailSubject, mailBody, processOutput)
    }
    
    private String getStackTrace(Throwable e) {
        String s = ''
        while (e) {
            if (s.length() > 0) {
                s += "\nCaused by: " + e.class.name + ' ' + e.message + '\n    at '
            }
            s += e.stackTrace.join('\n    at ')
            e = e.getCause()
        }
        return s
    }
    
    private boolean isPartial(byte[] progressContent) {
        return progressContent && progressContent.length == MAX_ATTACHMENT_SIZE
    }

    private void sendLoadMail(toAddress, ccAddress, fromAddress, event,
                              userLacksEmail, username) {
        def repo = event.repo
        Locale locale = event.locale ?: Locale.getDefault()
        def mailSubject = getMessage(event.isSuccess ?
                'mail.message.subject.success' : 'mail.message.subject.error',
                null, locale)
        mailSubject += getMessage(event.messagePrefix + '.subject', null, locale)
        mailSubject += getMessage('mail.message.repository', [repo.name], locale)

        def mailBody
        byte[] processOutput
        (mailBody, processOutput) = getMailBody(event, repo, 
                event.messagePrefix + '.body.success',
                event.messagePrefix + '.body.error', 
                userLacksEmail, username, locale)

        sendMail(toAddress, ccAddress, fromAddress, 
                 mailSubject, mailBody, processOutput)
    }

    /**
     * Sends the Verify job email, but only if the event indicates an error
     * @param toAddress
     * @param ccAddress
     * @param fromAddress
     * @param event
     */
    private void sendVerifyMail(toAddress, ccAddress, fromAddress, event, 
                                userLacksEmail, username) {
        def repo = event.repo
        Locale locale = event.locale ?: Locale.getDefault()
        def mailSubject = getMessage(event.isSuccess ?
            'mail.message.subject.success' : 'mail.message.subject.error',
                null, locale)
        mailSubject += getMessage('mail.message.verify.subject', null, locale)
        mailSubject += getMessage('mail.message.repository', [repo.name], locale)

        def mailBody
        byte[] processOutput
        if (event.isSuccess && event.source == RepoVerifyJob.EVENT_SOURCE_SCHEDULED) {
            log.info("Repo '${repo.name}' verification succeeded, sending no email")
            return
        }
        (mailBody, processOutput) = getMailBody(event, repo, 
                'mail.message.verify.body.success',
                'mail.message.verify.body.error', 
                userLacksEmail, username, locale)
        sendMail(toAddress, ccAddress, fromAddress,
                mailSubject, mailBody, processOutput)
    }

    private void sendSyncReplicaRepoMail(toAddress, fromAddress, event) {
        def repo = event.repo
        Locale locale = Locale.getDefault()
        def mailSubject = getMessage('mail.message.subject.error', null, locale)
        mailSubject += getMessage('mail.message.syncReplicaRepo.subject', null, locale)
        mailSubject += getMessage('mail.message.repository', [repo.name], locale)

        def (mailBody, processOutput) = getMailBody(event, repo, 
                'mail.message.syncReplicaRepo.notImplemented',
                'mail.message.syncReplicaRepo.body.error', false, null, locale)
        
        sendMail(toAddress, null, fromAddress,
                mailSubject, mailBody, processOutput)
    }
    
    /**
     * helper to prepare the mail body for Load and Verify events
     * @param event the RepositoryEvent
     * @param repo the Repository
     * @param successKey message key for mail body success
     * @param errorKey message key for mail body error
     * @param locale
     * @return [String mailBody, byte[] processOutput]
     */
    private List getMailBody(event, repo, successKey, errorKey, 
                             userLacksEmail, username, locale) {
        def mailBody
        byte[] processOutput
        if (event.isSuccess) {
            mailBody = getMessage(successKey, [repo.name,  
                    (userLacksEmail ? 1 : 0), username], locale)
        } else {
            processOutput = getProcessOutput(event)
            def processOutputTail = getProcessOutputTail(event)
            if (event.exception) {
                def e = event.exception
                GrailsUtil.deepSanitize(e)
                mailBody = getMessage(errorKey,
                        [repo.name, e.message,
                         e.class.name, e.stackTrace.join('\n'),
                         processOutput ? 1 : 0,
                         isPartial(processOutput) ? 1 : 0,
                         event.processOutput?.name, processOutputTail, 
                         (userLacksEmail ? 1 : 0), username], locale)
            } else {
                mailBody = getMessage(errorKey,
                        [repo.name, '', '', '', processOutput ? 1 : 0,
                         isPartial(processOutput) ? 1 : 0,
                         event.processOutput?.name, processOutputTail, 
                         (userLacksEmail ? 1 : 0), username], locale)
            }
        }
        mailBody += getMessage('mail.message.footer', null, locale)
        return [mailBody, processOutput]
    }

    private void sendMail(toAddress, ccAddress, fromAddress, 
                          mailSubject, mailBody, processOutput) {
        try {
            sendMail {
                if (processOutput) {
                    multipart true
                }
                to toAddress
                if (ccAddress) {
                    cc ccAddress
                }
                from fromAddress
                subject mailSubject
                body mailBody
                if (processOutput) {
                    attachBytes "ProcessOutput.txt", "text/plain", processOutput
                }
            }
            
        } catch (Exception e) {
            log.warn("Exception while sending mail. To: " + toAddress + 
                    "\nSubject: " + mailSubject + "\nBody:\n" + mailBody, e)
        }
    }

    private User retrieveUserForEvent(RepositoryEvent event) {
        Integer userId = (event instanceof DumpRepositoryEvent && 
                          event.dumpBean.isBackup()) ? 
                null : event.userId
        User user = null
        try {
            user = userId ? User.get(event.userId) : null
        } catch (Exception e) {
            log.warn("Error in user lookup", e)
        }
        return user
    }

}

