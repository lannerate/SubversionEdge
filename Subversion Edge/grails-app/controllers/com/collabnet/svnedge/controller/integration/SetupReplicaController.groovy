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
package com.collabnet.svnedge.controller.integration

import com.collabnet.svnedge.CantBindPortException;
import com.collabnet.svnedge.domain.MailConfiguration;
import com.collabnet.svnedge.domain.Repository 
import com.collabnet.svnedge.domain.Server 
import com.collabnet.svnedge.domain.ServerMode;
import com.collabnet.svnedge.domain.integration.CtfServer 
import com.collabnet.svnedge.domain.integration.ReplicaConfiguration
import com.collabnet.svnedge.integration.CtfAuthenticationException;
import com.collabnet.svnedge.integration.CtfConnectionException
import com.collabnet.svnedge.integration.CtfConnectionBean;
import com.collabnet.svnedge.integration.CtfSessionExpiredException;
import com.collabnet.svnedge.integration.InvalidSecurityKeyException;
import com.collabnet.svnedge.integration.RemoteMasterException;
import com.collabnet.svnedge.integration.ReplicaConversionBean 
import org.codehaus.groovy.grails.plugins.springsecurity.Secured

import org.springframework.beans.BeanUtils
import javax.net.ssl.SSLHandshakeException


@Secured(['ROLE_ADMIN', 'ROLE_ADMIN_SYSTEM'])
class SetupReplicaController {

    private static String CTF_CONNECTION_SESSION_KEY = "ctfConnectionCommand"
    private static String REPLICA_INFO_SESSION_KEY = "replicaInfoCommand"
    private static String REPLICA_CONVERSION_BEAN_SESSION_KEY = "replicaConversionBean"

    def setupReplicaService
    def setupTeamForgeService
    def authenticateService
    def securityService
    
    /**
     * default view is actually the "TeamForge Mode" intro, so forward 
     */
    def index = {
        forward(controller: "setupTeamForge", action: "index")
    }

    /**
     * Collect CTF credentials 
     */
    def ctfInfo = {
        def conversion = getConversionBean()
        if (conversion?.registrationError) {
            flash.error = conversion.registrationError
        }
        [cmd: getCtfConnectionCommand(), encodeMessageHtml: true]
    }

    /**
     * Collect replica info 
     */
    def replicaSetup = { CtfConnectionCommand input ->

        def externalSystems;
        boolean encodeMessageHtml = true
        
        MailConfiguration mailConfig = MailConfiguration.getConfiguration()
        if (!mailConfig?.enabled) {
            mailConfig = null
        }
        
        if (!input.hasErrors() && !mailConfig?.hasErrors()) {

            try {
                // copy input params to the conversion bean
                def bean = getConversionBean(input)
                // verify connection
                setupReplicaService.confirmCtfConnection(bean.ctfConn)
                
                // save form input to session (in case tab is re-enterd)
                def cmd = getCtfConnectionCommand()
                // if returning because user clicks tab, then input will be empty
                if (input.ctfURL) {
                    BeanUtils.copyProperties(input, cmd)
                }
                // fetch available external systems (error if none available)
                externalSystems = fetchIntegrationServers()
                if (!externalSystems) {
                    input.errors.rejectValue('ctfURL', 'ctfRemoteClientService.externalSystems.error',
                        [input.ctfURL] as Object[], 'no replicable masters')
                }

                // validate that svnedge is running ssl if the TF remote is
                if (new URL(cmd.ctfURL).getProtocol() == "https" && !Server.getServer().useSsl) {
                    input.errors.rejectValue('ctfURL', 'ctfConversion.svnedge.ssl.required',
                        [input.ctfURL] as Object[], 'SvnEdge should be ssl in order to replicate an ssl master')
                    encodeMessageHtml = false
                }
            }
            catch (MalformedURLException e) {
                input.errors.rejectValue('ctfURL', 'ctfRemoteClientService.host.malformedUrl',
                        [input.ctfURL] as Object[], 'bad url')
            }
            catch (UnknownHostException e) {
                input.errors.rejectValue('ctfURL', 'ctfRemoteClientService.host.unknown.error',
                        [new URL(input.ctfURL).host] as Object[], 'unknown host')
            }
            catch (NoRouteToHostException e) {
                input.errors.rejectValue('ctfURL', 'ctfRemoteClientService.host.unreachable.error',
                        [input.ctfURL] as Object[], 'no route')
            }
            catch (SSLHandshakeException e) {
                def msg = message(code:"ctfRemoteClientService.ssl.error", args:
                    ["http://help.collab.net/index.jsp?topic=/csvn/action/csvntotf_ssl.html"])
                log.warn(msg)
                input.errors.rejectValue("ctfURL", "ctfRemoteClientService.ssl.error",
                       ["http://help.collab.net/index.jsp?topic=/csvn/action/csvntotf_ssl.html"] as Object[], msg )
                // we want to display url in the error message for this field, so override the message encoding
                encodeMessageHtml = false
            }
            catch (CtfConnectionException e) {
                // some other problem connecting to the CTF instance
                input.errors.rejectValue('ctfURL', e.messageKey,
                        [input.ctfURL] as Object[], 'bad url')
            }
            catch (CtfAuthenticationException e) {
                // FIXME: note we're hardcoding ctfURL as the argument used
                // in the msg.  We may want to consider adding the param array
                // to the exceptions themselves, so they can be used here.
                input.errors.rejectValue('ctfUsername', e.messageKey,
                        [input.ctfURL] as Object[], 'bad credentials')
            }
            catch (CtfSessionExpiredException e) {
                session.setAttribute(REPLICA_CONVERSION_BEAN_SESSION_KEY, null)
                input.errors.rejectValue('ctfURL', 'ctfRemoteClientService.remote.sessionExpired',
                        [input.ctfURL] as Object[], 'ctf session expired')
            }
            catch (RemoteMasterException e) {
                session.setAttribute(REPLICA_CONVERSION_BEAN_SESSION_KEY, null)
                input.errors.rejectValue('ctfURL', e.messageKey,
                        [input.ctfURL] as Object[], e.message)
            }
        }

        if (input.hasErrors()) {
            // return to input view with errors
            render([view: "ctfInfo", model: [cmd: input, encodeMessageHtml: encodeMessageHtml]])
            return
        }

        // success logging in 
        [cmd: getReplicaInfoCommand(), integrationServers: externalSystems, mailConfig: mailConfig]
    }
    
    /**
     * Verify information
     */
    def confirm = { ReplicaInfoCommand input -> 

        MailConfiguration mailConfig = MailConfiguration.getConfiguration()
        if (mailConfig?.enabled) {
            mailConfig.repoSyncToAddress = params['repoSyncToAddress']
            mailConfig.save()
        } else {
            mailConfig = null
        }
        

        def scmList = fetchIntegrationServers()
        if (!input.hasErrors() && !mailConfig?.hasErrors()) {
            
            try {
                // save input for form re-entry
                def cmd = getReplicaInfoCommand()
                BeanUtils.copyProperties(input, cmd)

                def selectedScm = null
                for (scmServer in scmList) {
                    if (scmServer.id == input.masterExternalSystemId) {
                        selectedScm = scmServer
                        cmd.masterExternalSystemId = scmServer.id
                        break
                    }
                }                            
                flash.warn=message(code: 'setupReplica.page.localReposDelete.warning')
                return [ctfURL: getCtfConnectionCommand().ctfURL,
                        ctfUsername: getCtfConnectionCommand().ctfUsername,
                        selectedScmServer: selectedScm,
                        replicaTitle: getReplicaInfoCommand().name,
                        replicaDescription: getReplicaInfoCommand().description,
                        replicaMessageForAdmin: getReplicaInfoCommand().message
                        ]
            }
            catch (Exception e) {
                log.error("Unable to register replica: " + e.getMessage(), e)
            }
        }
        
        // return to input view with errors
        render([view: "replicaSetup", model: [cmd: input, integrationServers: scmList, mailConfig: mailConfig]])
    }
    

    /**
     * Do conversion, show confirmation 
     */
    def convert = { 

        def server
        def repoName
        def userName
        def input = getReplicaInfoCommand()
        // copy input params to the conversion bean
        ReplicaConversionBean bean = getConversionBean(input)
        try {
            // register the replica
            setupReplicaService.registerReplica(bean)
            session.setAttribute(REPLICA_CONVERSION_BEAN_SESSION_KEY, null)

            // prepare confirmation data
            server = Server.getServer()
            def ctfServer = CtfServer.getServer()
            def repos = Repository.list([max: 1])
            repoName = repos ? repos[0].name : "example"
            userName = authenticateService.principal().getUsername()
            def ctfusername = ctfServer.ctfUsername
            def ctfpassword = securityService.decrypt(ctfServer.ctfPassword)
            def svnUrl = getCtfConnectionCommand().ctfURL + "/_junkrepos"

            flash.message = message(code: 'setupReplica.action.confirm.success')

            def isIssuerUntrusted = setupReplicaService.checkIssuer(svnUrl,
                                                                    ctfusername,
                                                                    ctfpassword)
            if (isIssuerUntrusted &&
                    (getCtfConnectionCommand().ctfURL[0..4] == 'https')) {
                flash.unfiltered_warn = message(code: 'status.page.certificate.accept',
                    args: ['/csvn/status/showCertificate'])
            }

            return [ctfURL: getCtfConnectionCommand().ctfURL,
                    ctfUsername: getCtfConnectionCommand().ctfUsername,
                    svnReplicaCheckout: "svn co ${server.svnURL()}${repoName} ${repoName} --username=${userName}"
                    ]

        } catch (CantBindPortException cantBind) {
            setupReplicaService.serverCantRestartAfterRegistration()
            flash.message = message(code: 'setupReplica.action.confirm.success')
            flash.error = message(
                code: 'replica.error.registration.serverCantRestart')
            server = Server.getServer()
            return [ctfURL: getCtfConnectionCommand().ctfURL,
                    ctfUsername: getCtfConnectionCommand().ctfUsername,
                    svnReplicaCheckout: "svn co ${server.svnURL()}${repoName}" +
                        " ${repoName} --username=${userName}"]
        } catch (Exception e) {
            log.error("Unable to register replica: " + (e.getMessage() ?: 
                e.getCause().getMessage()), e)
            def msg = message(code: 'replica.error.registration') + " " +
                (e.getMessage() ?: e.getCause().getMessage())
            bean.registrationError = msg
            forward(action: 'ctfInfo')
        }

        // return to input view with errors
        render([view: "replicaSetup", model: [cmd: input, 
            integrationServers: fetchIntegrationServers()]])
    }

    /**
     * Edit CTF credentials
     */
    def editCredentials = {
        def command = getCtfConnectionCommand()
        def ctfServer = CtfServer.getServer()
        command.ctfUsername = ctfServer.ctfUsername
        command.ctfURL = ctfServer.baseUrl
        command.serverKey = ctfServer.internalApiKey
        def server = Server.getServer()
        boolean isReplica = (server.mode == ServerMode.REPLICA)
        if (!setupTeamForgeService.confirmApiSecurityKey()) {
            command.errors.rejectValue('serverKey',
                'setupReplica.action.updateCredentials.invalidApiKey')
        }

        [cmd: command, isReplica: isReplica]
    }

    /**
     * Persist updated CTF connection
     */
    def updateCredentials = { CtfConnectionCommand input ->

        def server = Server.getServer()
        boolean isReplica = (server.mode == ServerMode.REPLICA)
        if (!input.hasErrors()) {

            try {
                // copy input params to the conversion bean
                def bean = getConversionBean(input)
                // persist the connection
                if (isReplica) {
                    setupReplicaService.updateCtfConnection(bean.ctfConn)
                } else {
                    setupTeamForgeService.updateCtfConnection(bean.ctfConn)
                }
                
                // save form input to session (in case tab is re-enterd)
                def cmd = getCtfConnectionCommand()
                BeanUtils.copyProperties(input, cmd)

                // success changing the credentials
                flash.message = message(code:"setupReplica.action.updateCredentials.success")
            }
            catch (MalformedURLException e) {
                input.errors.rejectValue('ctfURL', 'ctfRemoteClientService.host.malformedUrl',
                        [input.ctfURL] as Object[], 'bad url')
            }
            catch (UnknownHostException e) {
                input.errors.rejectValue('ctfURL', 'ctfRemoteClientService.host.unknown.error',
                        [new URL(input.ctfURL).host] as Object[], 'unknown host')
            }
            catch (NoRouteToHostException e) {
                input.errors.rejectValue('ctfURL', 'ctfRemoteClientService.host.unreachable.error',
                        [input.ctfURL] as Object[], 'no route')
            }
            catch (CtfAuthenticationException e) {
                input.errors.rejectValue('ctfURL', 'ctfRemoteClientService.auth.error',
                        [input.ctfURL] as Object[], 'bad credentials')
            }
            catch (IllegalStateException e) {
                CtfServer ctfServer = CtfServer.getServer()
                input.errors.rejectValue('ctfUsername', 
                        'setupReplica.action.old.auth.error',
                        [ctfServer.ctfUsername] as Object[], 
                        'previous credentials are invalid')
            }
            catch (InvalidSecurityKeyException e) {
                input.errors.rejectValue('serverKey', 
                        'setupReplica.action.updateCredentials.invalidApiKey')
            }
        }

        // provide ctf url to the CtfConnectionBean, needed for information message
        def ctfServer = CtfServer.getServer()
        input.ctfURL = ctfServer.baseUrl

        // return to input view with success or errors
        render([view: "editCredentials", 
                model: [cmd: input, isReplica: isReplica]])
    }

    def editConfig = {
        return prepareConfigModel()
    }

    def updateConfig = {
        ReplicaConfiguration config = ReplicaConfiguration.currentConfig
        bindData(config, params)
        if (config.save()) {
            flash.message = message(code:"setupReplica.action.updateConfig.success")
            redirect(action: 'editConfig')
        }
        else {
            request.error = message(code:"setupReplica.action.updateConfig.invalidSettings")
            render(view: "editConfig", model: prepareConfigModel(config))
        }
    }

    private def prepareConfigModel(config = ReplicaConfiguration.currentConfig) {
        println "ctfURL = " + CtfServer.getServer().baseUrl
        return [config: config, ctfURL: CtfServer.getServer().baseUrl]
    }
    
    private List fetchIntegrationServers() throws RemoteMasterException {

        CtfConnectionBean conn = getConversionBean().ctfConn
        return setupReplicaService.getIntegrationServers(conn)

    }

    private CtfConnectionCommand getCtfConnectionCommand() {

        CtfConnectionCommand cmd = session.getAttribute(CTF_CONNECTION_SESSION_KEY)
        if (!cmd) {
            cmd = new CtfConnectionCommand()
            session.setAttribute(CTF_CONNECTION_SESSION_KEY, cmd)
        }
        return cmd

    }

    private ReplicaInfoCommand getReplicaInfoCommand() {

        ReplicaInfoCommand cmd = session.getAttribute(REPLICA_INFO_SESSION_KEY)
        if (!cmd) {
            cmd = new ReplicaInfoCommand()
            session.setAttribute(REPLICA_INFO_SESSION_KEY, cmd)
        }
        return cmd
    }
    
    /**
     * obtains a ctf conversion bean from the session 
     * @return 
     */
    private ReplicaConversionBean getConversionBean() {
        
        ReplicaConversionBean bean = session.getAttribute(REPLICA_CONVERSION_BEAN_SESSION_KEY)
        if (!bean) {
            bean = new ReplicaConversionBean()
            bean.ctfConn = new CtfConnectionBean(userLocale: request.locale)
            session.setAttribute(REPLICA_CONVERSION_BEAN_SESSION_KEY, bean)
        }
        return bean
        
    }

    /**
     * obtain a ctf conversion bean from the session, and apply properties from command
     * @param cmd the controller action command bean 
     */
    private ReplicaConversionBean getConversionBean(CtfConnectionCommand cmd) {

        ReplicaConversionBean b = getConversionBean();
        b.ctfConn.ctfURL = cmd.ctfURL ?: b.ctfConn.ctfURL
        b.ctfConn.ctfUsername = cmd.ctfUsername ?: b.ctfConn.ctfUsername
        b.ctfConn.ctfPassword = cmd.ctfPassword ?: b.ctfConn.ctfPassword
        b.ctfConn.serverKey = cmd.serverKey ?: b.ctfConn.serverKey
        return b
    }

    /**
     * obtain a ctf conversion bean from the session, and apply properties from command
     * @param cmd the controller action command bean 
     */
    private ReplicaConversionBean getConversionBean(ReplicaInfoCommand cmd) {

        ReplicaConversionBean b = getConversionBean();
        b.masterExternalSystemId = cmd.masterExternalSystemId ?: b.masterExternalSystemId
        b.name = cmd.name ?: b.name
        b.description = cmd.description ?: b.description
        b.message = cmd.message ?: b.message
        return b
    }

}

class CtfConnectionCommand {
    
    String ctfURL
    String ctfUsername
    String ctfPassword
    String serverKey
    
    static constraints = {
        ctfURL(blank: false)
        ctfUsername(blank: false)
        ctfPassword(blank: false)
        serverKey(blank: true)
    }
}

class ReplicaInfoCommand {
    
    String masterExternalSystemId
    String name
    String description
    String message
    
    static constraints = {
        masterExternalSystemId(blank: false)
        name(blank: false)
        description(blank: false)
    }
}
