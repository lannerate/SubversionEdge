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


import com.collabnet.svnedge.CantBindPortException 
import com.collabnet.svnedge.console.AbstractSvnEdgeService
import com.collabnet.svnedge.integration.CtfAuthenticationException 
import com.collabnet.svnedge.integration.CtfConnectionBean
import com.collabnet.svnedge.integration.RemoteMasterException
import static com.collabnet.svnedge.admin.JobsAdminService.REPLICA_GROUP
import com.collabnet.svnedge.domain.Repository 
import com.collabnet.svnedge.domain.Server 
import com.collabnet.svnedge.domain.ServerMode 
import com.collabnet.svnedge.domain.integration.ApprovalState 
import com.collabnet.svnedge.domain.integration.CtfServer 
import com.collabnet.svnedge.domain.integration.ReplicaConfiguration
import javax.net.ssl.SSLHandshakeException
import com.collabnet.svnedge.util.ConfigUtil

/**
 * This service handles replication-related functionality
 */
class SetupReplicaService  extends AbstractSvnEdgeService {

    boolean transactional = true

    def ctfRemoteClientService
    def setupTeamForgeService
    def jobsAdminService
    def securityService
    def serverConfService
    def svnRepoService
    def lifecycleService
    def discoveryService
    def replicaCommandExecutorService
    def replicaCommandSchedulerService
    def networkingService
    def authenticationManager    
    def csvnAuthenticationProvider
    def ctfAuthenticationProvider
    def commandLineService

    /**
     * Defines if there was any problems during the registration
     */
    def replicaRegistrationFailed

    def bootStrap = { appHome ->
        log.debug("Bootrastrapping the Setup Replica service")
        setupTeamForgeService.updateIntegrationScripts(appHome, ServerMode.REPLICA)
    }

    /**
     * @return returns if there were errors during the registration process.
     */
    def hasRegistrationErrors() {
        return replicaRegistrationFailed
    }

    /**
     * Sets the status of the error of the registation.
     */
    def serverCantRestartAfterRegistration() {
        replicaRegistrationFailed = true
    }

    /**
     * Clears the registration error.
     */
    def clearRegistrationError() {
        replicaRegistrationFailed = false
    }

    /**
     * Confirms the ctf connection
     */
    public void confirmCtfConnection(CtfConnectionBean ctfConn) throws CtfAuthenticationException,
            RemoteMasterException, UnknownHostException, NoRouteToHostException, MalformedURLException,
            SSLHandshakeException, CtfConnectionException {
        // attempt connection -- throws exception on failure
        log.debug("Verifying CTF connection")
        setupTeamForgeService.confirmConnection(ctfConn)
    }
 
    /**
     * Attempts to register (or re-register) the Replica with the Master.
     * This should be called after the Replica is first setup, or any time
     * the Replica is updated or the Master is changed.
     * @param rc the Replica Configuration data
     * @param conversion the CtfConversionBean holding connection info
     */
    public void registerReplica(ReplicaConversionBean replicaInfo) 
            throws RemoteMasterException, ReplicaConversionException,
            CantBindPortException, InvalidSecurityKeyException {

        log.debug("Attempting replica conversion...")

        def server = Server.getServer()

        // if apache encryption is set, provide the console ssl port
        // since we are not tracking a separate "consoleSsl" property
        // in CTF (see OCN artf5894)
        def consolePort = Server.getConsolePort(server.getUseSsl())

        // SVNContextPath is ignored by CTF (artf5374).  Leaving for now until the CTF
        // changes are reflected in the latest builds
	def props = ["HostName": server.getHostname(),
                     "HostPort": server.getPort(), 
                     "HostSSL": server.getUseSsl(), 
                     "ConsolePort": consolePort,
                     "ViewVCContextPath": server.getViewvcBasePath(), 
                     "SVNContextPath": server.getSvnBasePath()]

        String systemId = ctfRemoteClientService.addExternalSystemReplica(
            replicaInfo.ctfConn.ctfURL, replicaInfo.ctfConn.userSessionId, 
            replicaInfo.masterExternalSystemId, replicaInfo.name, 
            replicaInfo.description, replicaInfo.message, props,
            replicaInfo.ctfConn.userLocale)

        log.debug("Conversion successful, got ID: " + systemId)

        // with success, make modification to this instance
        server = Server.getServer()
        server.mode = ServerMode.REPLICA

        ReplicaConfiguration rc = ReplicaConfiguration.getCurrentConfig()
        if (!rc) {
            rc = new ReplicaConfiguration()
        }
        // rc.svnMasterUrl is now provided by approval command 
        rc.name = replicaInfo.name
        rc.description = replicaInfo.description
        rc.approvalState = ApprovalState.PENDING
        rc.systemId = systemId

        def ctfServer = CtfServer.getServer() 
        if (!ctfServer) {
            ctfServer = new CtfServer()
        }
        ctfServer.baseUrl = replicaInfo.ctfConn.ctfURL
        ctfServer.internalApiKey = replicaInfo.ctfConn.serverKey
        ctfServer.ctfUsername = replicaInfo.ctfConn.ctfUsername
        ctfServer.ctfPassword = securityService.encrypt(
            replicaInfo.ctfConn.ctfPassword)

        if (!rc.validate() || !ctfServer.validate() || !server.validate()) {
            log.error("could not save necessary domain objects")
            [rc, ctfServer, server].each { domainObj ->
                domainObj.errors.each { log.error(it) }
            }
            throw new ReplicaConversionException("Could not convert to replica")
        }

        rc.save(flush:true)
        ctfServer.save(flush:true)
        server.save(flush:true)
        
        // move any existing Repositories out of the way
        Repository.list().each {
            svnRepoService.archivePhysicalRepository(it) 
            svnRepoService.removeRepository(it)
        }

        log.info("Rewriting server config and restarting")
        // setupTeamForgeService.installIntegrationServer(replicaInfo)
        setupTeamForgeService.unpackIntegrationScripts(
            replicaInfo.ctfConn.userLocale)

        serverConfService.backupAndOverwriteHttpdConf()
        serverConfService.writeConfigFiles()
        
        authenticationManager.providers = [ctfAuthenticationProvider]

        log.info("starting FetchReplicaCommandsJob")
        def triggerInstance = FetchReplicaCommandsJob.makeTrigger(rc.commandPollRate)
        jobsAdminService.createOrReplaceTrigger(triggerInstance)
        log.info("Resuming replica jobs")
        jobsAdminService.resumeGroup(REPLICA_GROUP)

        setupTeamForgeService.restartServer()
    }

    /**
     * update the TeamForge credentials on file
     * @param ctfConn
     */
    public void updateCtfConnection(CtfConnectionBean ctfConn) throws 
        CtfAuthenticationException, RemoteMasterException,
        UnknownHostException, NoRouteToHostException, MalformedURLException,
        InvalidSecurityKeyException {

        // confirm the new credentials work
        confirmCtfConnection(ctfConn);

        def ctfServer = CtfServer.getServer() 
        if (ctfConn.ctfUsername != ctfServer.ctfUsername) {

            CtfConnectionBean oldCtfConn = 
                    new CtfConnectionBean(ctfURL: ctfServer.baseUrl,
                    ctfUsername: ctfServer.ctfUsername, ctfPassword:
                    securityService.decrypt(ctfServer.ctfPassword),
                    userLocale: ctfConn.userLocale)
        
            try {        
                // fill in the session id we will need. CTF does not allow replica
                // user to be updated, if the old credentials are invalid.
                // https://forge.collab.net/sf/go/artf108402
                confirmCtfConnection(oldCtfConn)
                
                ctfRemoteClientService.updateReplicaUser(
                        oldCtfConn.userSessionId, ctfConn.ctfUsername)
        
                ctfRemoteClientService.logoff(ctfServer.baseUrl, 
                        oldCtfConn.ctfUsername, oldCtfConn.soapSessionId)
            } catch (CtfAuthenticationException e) {
                throw new IllegalStateException(
                        "Previous credentials are invalid")
            }
        }

        // persist
        log.info("Updating the CTF credentials used for SVN Replication")
        ctfServer.ctfUsername = ctfConn.ctfUsername
        ctfServer.ctfPassword = securityService.encrypt(ctfConn.ctfPassword)
        ctfServer.save()

        setupTeamForgeService.updateCtfConnection(ctfConn)
        ctfRemoteClientService.logoff(ctfServer.baseUrl, 
                ctfConn.ctfUsername, ctfConn.soapSessionId)
    }


    /**
     * Revert from managed replica mode to standalone. This method will *not* notify
     * the ctf instance -- for use where the Ctf instance
     * has initiated the removal and therefore already knows
     * @param errors
     * @param locale
     */
    public void revertFromReplicaMode(errors, locale) {

        undoReplicaModeConfiguration(errors, locale)
        jobsAdminService.pauseGroup(REPLICA_GROUP)

    }

    /**
     * Revert from managed replica mode to standalone. This method will notify
     * the CTF Master using the provided credentials
     * @param ctfUsername
     * @param ctfPassword
     * @param errors collection
     * @param locale for error messaging
     */
    public void revertFromReplicaMode (String ctfUsername, String ctfPassword,
            errors, locale) throws CtfAuthenticationException, RemoteMasterException {

        unregisterReplica(ctfUsername, ctfPassword, errors, locale)
        undoReplicaModeConfiguration(errors, locale)
        jobsAdminService.pauseGroup(REPLICA_GROUP)
    }

    /**
     * Obtain a list of integration servers from the Ctf connection represented
     * in the conversion bean. Each element of the list is a map of the
     * properties of the integration server.
     * @param ctfConn is the connection bean.
     * @param locale the request locale for messaging
     * @return List of SCM integration servers which can be replicated.
     */
    public List<Map<String, String>> getIntegrationServers(ctfConn) throws RemoteMasterException {

        return ctfRemoteClientService.getReplicableScmExternalSystemList(
            ctfConn.ctfURL, ctfConn.soapSessionId, ctfConn.userLocale)
    }

    /**
     * Updates the server information with the given scmMasterUrl and updates
     * the approval status to Approved.
     * 
     * @param scmMasterUrl the scmUrl from the master server.
     */
    public void updateServerAfterApproval(scmMasterUrl, scmMasterId) {
        ReplicaConfiguration rc = ReplicaConfiguration.getCurrentConfig()
        rc.svnMasterUrl = scmMasterUrl
        rc.approvalState = ApprovalState.APPROVED
        rc.save(flush:true)

        Server server = Server.getServer()
        server.svnBasePath = rc.contextPath()
        server.save(flush:true)

        CtfServer ctfServer = CtfServer.getServer()
        ctfServer.mySystemId = scmMasterId
        ctfServer.save(flush:true)
        File idFile = new File(server.repoParentDir, ".scm.properties")
        idFile.text = "external_system_id=" + scmMasterId
     }


    private void unregisterReplica(String ctfUsername, String ctfPassword, List errors, Locale locale)
        throws CtfAuthenticationException, RemoteMasterException {

        ctfRemoteClientService.deleteReplica(
            ctfUsername, ctfPassword, errors, locale)

    }

    private void undoReplicaModeConfiguration(Collection errors, Locale locale) {

        Server server = Server.getServer()
        CtfServer ctfServer = CtfServer.getServer()
        ReplicaConfiguration replicaConfig = ReplicaConfiguration.getCurrentConfig()

        server.mode = ServerMode.STANDALONE
        server.save(flush:true)

        if (ctfServer) {
            ctfServer.delete()
        }

        if (replicaConfig) {
            replicaConfig.approvalState = ApprovalState.REMOVED
            replicaConfig.save(flush:true)
        }

        // delete all database and filesystem artifacts for Repositories
        Repository.list().each {
            svnRepoService.deletePhysicalRepository(it) 
            svnRepoService.removeRepository(it)
        }
        File idFile = new File(server.repoParentDir, ".scm.properties")
        if (idFile.exists()) {
            idFile.delete()
        }

        serverConfService.restoreHttpdConfFromBackup()
        serverConfService.writeConfigFiles()
        discoveryService.serverUpdated()
        authenticationManager.providers = [csvnAuthenticationProvider]
            
        try {
            lifecycleService.restartServer();
        }
        catch (CantBindPortException e) {
            log.error("Could not restart server", e)
            errors << getMessage("server.error.cantBindPort", locale)
        }

        replicaConfig?.delete(flush:true);

        replicaCommandSchedulerService.cleanCommands()
        replicaCommandExecutorService.resetQueues()        
    }

    def getCertDetailsOfMaster = {
        def replicaConfiguration = ReplicaConfiguration.getCurrentConfig()

        if (replicaConfiguration) {
            def svnUrl = replicaConfiguration.svnMasterUrl + "/_junkrepos"

            def ctfServer = CtfServer.getServer()
            def username = ctfServer.ctfUsername
            def password = securityService.decrypt(ctfServer.ctfPassword)

            def isIssuerNotTrusted = checkIssuer(svnUrl, username, password)

            if (isIssuerNotTrusted) {
                def certDetails = svnRepoService
                        .getCertDetails(svnUrl, username, password)
                return [hostname: certDetails.hostname,
                        validity: certDetails.validity,
                        issuer: certDetails.issuer,
                        fingerprint: certDetails.fingerprint]
            } else {
                return [null, null, null, null]
            }
        } else {
            return [null, null, null, null]
        }
    }

    /**
     * This function checks whether the issuer is trusted or not.
     */
    private def checkIssuer(String svnUrl, String username, String password) {
       /*
        * Command to check self-signed cert or not.
        * It is non-interactive otherwise it may throw up either
        * authentication challenge or cert verification message
        */
        def command = [ConfigUtil.svnPath(), "ls", svnUrl,
                       "--non-interactive", "--username", username,
                       "--password", password, "--config-dir", ConfigUtil.svnConfigDirPath()]
        String[] commandOutput =
            commandLineService.execute(command.toArray(new String[0]), null, null, true)

        return commandOutput[2].find("issuer is not trusted")
    }

    def saveCertificate(String viewedFingerprint) {
        if (!viewedFingerprint) {
            throw new IllegalArgumentException("viewedFingerprint cannot be null")
        }

        def replicaConfiguration = ReplicaConfiguration.getCurrentConfig()
        if (replicaConfiguration) {
            def ctfServer = CtfServer.getServer()
            def username = ctfServer.ctfUsername
            def password = securityService.decrypt(ctfServer.ctfPassword)
            def svnUrl = replicaConfiguration.svnMasterUrl + "/_junkrepos"
            if (svnRepoService.acceptSslCertificate(
                    svnUrl, username, password, viewedFingerprint)) {
                replicaConfiguration.acceptedCertFingerPrint = viewedFingerprint
                replicaConfiguration.save()
            }            
        }
    }
}
