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
package com.collabnet.svnedge.controller

import org.codehaus.groovy.grails.plugins.springsecurity.Secured
import org.springframework.web.servlet.support.RequestContextUtils as RCU

import com.collabnet.svnedge.CantBindPortException;
import com.collabnet.svnedge.domain.Repository 
import com.collabnet.svnedge.domain.Server 
import com.collabnet.svnedge.domain.ServerMode 
import com.collabnet.svnedge.domain.integration.ApprovalState 
import com.collabnet.svnedge.domain.integration.CtfServer 
import com.collabnet.svnedge.domain.integration.ReplicaConfiguration 

import java.text.SimpleDateFormat

@Secured(['ROLE_USER'])
class StatusController {

    def authenticateService
    def operatingSystemService
    def networkingService
    def svnRepoService
    def quartzScheduler
    def serverConfService
    def statisticsService
    def lifecycleService
    def packagesUpdateService
    def setupReplicaService
    def setupTeamForgeService
    def replicaServerStatusService
    def securityService

    // start and stop actions use POST requests
    static allowedMethods = [start:'POST', stop:'POST',
                             showCertificate:'GET',
                             acceptCertificate:'POST',
                             restartConsole:'POST']

    def getUpdateMessage() {
        return message(code: 'packagesUpdate.status.updates.available.download', 
            args: ['/csvn/packagesUpdate/available'])
    }

    def index = {
        def server = Server.getServer()
        prepareStatusViewModel(server)
    }

    def showCertificate = {
        def server = Server.getServer()
        prepareStatusViewModel(server)
    }


    /**
     * Returns TRUE if server is in replica mode and the master is in SSL mode
     */
    boolean isReplicaOfSSLMaster(Server server,
                                 ReplicaConfiguration replicaConfiguration) {
        if (server.mode != ServerMode.REPLICA || !replicaConfiguration || !replicaConfiguration?.svnMasterUrl) {
            return false
        }
        else {
            return replicaConfiguration.svnMasterUrl[0..4] == 'https'
        }
    }

    /**
     * Persists the finger print since the certificate has been accepted
     */
    @Secured(['ROLE_ADMIN','ROLE_ADMIN_SYSTEM'])
    def acceptCertificate = {
        setupReplicaService.saveCertificate(params.currentlyAcceptedFingerPrint)
        redirect(action:'index')
     }

    private Map prepareStatusViewModel(Server server) {
        def ctfUrl
        def currentReplica = ReplicaConfiguration.getCurrentConfig()
        def ctfServer = CtfServer.getServer()
        String certHostname
        String certValidity
        String certIssuer
        String certFingerPrint
        String acceptedFingerPrint = null


        if (server.mode == ServerMode.REPLICA) {

            if (!currentReplica) {
               flash.warn = message(code: 'replica.error.notStarted')
            }
            if (currentReplica.approvalState == ApprovalState.PENDING) {
               flash.warn = message(code: 'replica.error.notApproved')
            } else if (currentReplica.approvalState == ApprovalState.DENIED) {
               flash.warn = message(code: 'replica.error.denied')
            } else if (currentReplica.approvalState == ApprovalState.NOT_FOUND
                      || currentReplica.approvalState == ApprovalState
                      .REGISTRATION_FAILED) {
               flash.error = message(code: 'replica.error.cantRegister')

            } else if (currentReplica.approvalState == ApprovalState.APPROVED &&
                    setupReplicaService.hasRegistrationErrors()) {

                flash.error = message(
                    code: 'replica.error.registration.serverCantRestart')
                // clear the message until the user has restarted.
                setupReplicaService.clearRegistrationError()

            }
                    
            if (!setupTeamForgeService.confirmApiSecurityKey()) {
                
                request.unfiltered_warn = message(code: "replica.error.apiSecurityKey",
                        args: [createLink(controller: 'setupReplica', 
                                          action: 'editCredentials')])
            }
                
            ctfUrl = ctfServer.getWebAppUrl()

            /* get the certificate details of the master */
            try {
                def replicaDetails = setupReplicaService.getCertDetailsOfMaster()

                certHostname = replicaDetails.hostname
                certValidity = replicaDetails.validity
                certIssuer = replicaDetails.issuer
                certFingerPrint = replicaDetails.fingerprint
            }
            catch (Exception e) {
                log.error("Fault encountered parsing the CTF SSL certificate", e)
            }
        }
        else if (server.mode == ServerMode.MANAGED) {
            ctfUrl = ctfServer.getWebAppUrl()
        }

        // if this is a "reverted" replica, add a flash message and then delete 
        // ReplicaConfiguration
        if (currentReplica?.approvalState == ApprovalState.REMOVED) {
            flash.warn = message(code: 'replica.error.removed')
            currentReplica.delete()
        }

        boolean isStarted = lifecycleService.isStarted()
        Repository[] repos = Repository.list([max: 1])
        Repository sampleRepo = (repos.length > 0) ? repos[0] : null

        def sfVersion = this.packagesUpdateService.getInstalledVersionNumber()
        def svnVer = this.packagesUpdateService.getInstalledSvnVersionNumber()
        try {
           if (this.packagesUpdateService.hasBeenBootstraped()) {
               if (this.packagesUpdateService.areThereUpdatesAvailable()) {
                   if (!flash.error) {
                       flash.unfiltered_warn = getUpdateMessage()
                   }
               }
               //system restart has priority over the updates
               if (this.packagesUpdateService.systemNeedsRestart()) {
                   flash.unfiltered_warn = message(
                       code: 'packagesUpdate.status.updates.requiresRestart')
               }
               //if the system has recently been updated
               if (this.packagesUpdateService.hasTheSystemBeenUpdated() || 
                       this.packagesUpdateService.hasTheSvnServerBeenUpdated()){
                   this.packagesUpdateService.setTheSystemToNotBeenUpdated()
                   flash.message = message(
                       code: 'packagesUpdate.success.installed.updates')
               } else
               if (this.packagesUpdateService.hasNewPackagesBeenInstalled()) {
                   this.packagesUpdateService.setTheSystemToNotBeenUpdated()
                   flash.message = message(
                       code: 'packagesUpdate.success.installed.newPackages')
               }
           }

       } catch (NoRouteToHostException nrth) {
           if (!flash.error) {
               flash.error = message(
                   code: 'packagesUpdate.error.server.noConnection')
           }
       } catch (Exception e) {
           e.printStackTrace()
           def msg = message(code: 'packagesUpdate.error.general')
           flash.error = msg + ":" + e.getMessage()
       }
       
       if(authenticateService.ifAnyGranted("ROLE_ADMIN,ROLE_ADMIN_SYSTEM") &&
               packagesUpdateService.isIncompleteWindowsUpdate()) {
               
           String msg = message(
               code: 'packagesUpdate.error.incomplete.windows.update',
               args: [createLink(controller: 'log', action: 'show', 
                                 params: [fileName: 'updates.log'])
                     ])
           if (flash.unfiltered_error) {
               flash.unfiltered_error += ' ' + msg
           } else {
               flash.unfiltered_error = msg
           }
       }
        
       def runningCmdsSize = 0
       if (server.mode == ServerMode.REPLICA) {
           acceptedFingerPrint = currentReplica.acceptedCertFingerPrint

           def ctfusername = ctfServer.ctfUsername
           def ctfpassword = securityService.decrypt(ctfServer.ctfPassword)
           def svnUrl = currentReplica.svnMasterUrl + "/_junkrepos"

           def isIssuerUntrusted = setupReplicaService.checkIssuer(svnUrl,
                                                                   ctfusername,
                                                                   ctfpassword)

           if (isReplicaOfSSLMaster(server, currentReplica) && certFingerPrint
                   && isIssuerUntrusted) {
               if(acceptedFingerPrint != certFingerPrint) {
                   flash.unfiltered_warn = message(code: 'status.page.certificate.accept',
                       args: ['/csvn/status/showCertificate'])
               }
           }
           runningCmdsSize = replicaServerStatusService.getAllCommandsSize()
       }

       return [isStarted: isStarted,
               isDefaultPortAllowed: lifecycleService.isDefaultPortAllowed(),
               sampleRepo: sampleRepo,
               isReplicaMode: server.mode == ServerMode.REPLICA,
               currentReplica: currentReplica,
               server: server,
               perfStats: getPerfStats(currentReplica, server),
               softwareVersion: sfVersion,
               svnVersion: svnVer,
               ctfUrl: ctfUrl,
               isReplicaOfSSLMaster:
                   isReplicaOfSSLMaster(server, currentReplica),
               acceptedCertFingerPrint : acceptedFingerPrint,
               certHostname : certHostname,
               certValidity : certValidity,
               certIssuer : certIssuer,
               certFingerPrint : certFingerPrint, 
               replicaCommandsSize: runningCmdsSize]
    }

    def getPerfStats(currentConfig, server) {
       def dateTimeFormat = message(code:"default.dateTime.format.withZone")
       def runningSinceDate = quartzScheduler.getMetaData().runningSince
       runningSinceDate = runningSinceDate ?: new Date()
       def currentLocale = RCU.getLocale(request)
       def model = [
           [label: message(code: 'status.page.status.running_since'),
            value: new SimpleDateFormat(dateTimeFormat, 
                currentLocale).format(runningSinceDate)]]
       if (!server.managedByCtf()) {
           model << [label: message(code: 'status.page.status.repo_health'), 
               value: svnRepoService.formatRepoStatus(statisticsService
                   ?.getReposStatus(), currentLocale)]
       }
        if (operatingSystemService.isReady()) {

            def timestampDate = statisticsService.getTimestampFileSystemData()
            String timestampString = timestampDate ?
                message(code: "status.page.status.timestamp",
                        args: [new SimpleDateFormat(dateTimeFormat,
                        currentLocale).format(timestampDate)]) :
                message(code: "status.page.status.noData")

            def usedDisk = operatingSystemService.formatBytes(
                     statisticsService.getSystemUsedDiskspace())
            def usedRepo = operatingSystemService.formatBytes(
                     statisticsService.getRepoUsedDiskspace())
            def freeRepo = operatingSystemService.formatBytes(
                     statisticsService.getRepoAvailableDiskspace())

            model << [label: message(code: 'status.page.status.throughput'),
                value: networkingService.formatThroughput(
                    statisticsService.getThroughput(), currentLocale)]

            model << [label: message(code: 'status.page.status.space.header'),
                 value: timestampString]
            model << [label: message(code: 'status.page.status.space.system'),
                 value: usedDisk]
            model << [label: message(code: 'status.page.status.space.repos'),
                 value: usedRepo]
            model << [label: message(code: 'status.page.status.space.avail'),
                 value: freeRepo]
        }
        return model
   }

    @Secured(['ROLE_ADMIN','ROLE_ADMIN_SYSTEM'])
    def start = {
        def server = lifecycleService.getServer()
        if (!server) {
            flash.error = message(code: 'server.error.general')
            redirect action: 'index', id: params.id
            return
        }

        server.properties = params
        if (!server.hasErrors() && server.save()) {
            try {
                def result = lifecycleService.startServer()
                if (result < 0) {
                    flash.warn = message(code: 'server.status.alreadyRunning')
                } else if (result == 0) {
                    flash.message = message(code: 'server.status.isRunning')
                } else {
                    if (serverConfService.httpdUser == 'root') {
                        flash.unfiltered_error = message(
                                code: 'server.status.errorStarting.repoParentDirOwnedByRoot',
                                args: [createLink(controller: 'server', action: 'edit')])
                    } else {
                        flash.error = message(code: 'server.status.errorStarting')
                    }
                }
            } catch (CantBindPortException startServiceException) {
                flash.error = startServiceException.getMessage(
                    RCU.getLocale(request))
            }

        } else {
            flash.error = message(code: 'server.status.invalidSettings')
        }

        redirect(action:'index')
    }

    @Secured(['ROLE_ADMIN','ROLE_ADMIN_SYSTEM'])
    def stop = {
        lifecycleService.stopServer()
        flash.message = message(code: 'server.status.stopped')
        redirect(action:'index')
    }

    def replicationInfo = {
        def runningCmdsSize = replicaServerStatusService.getAllCommandsSize()
        render(contentType:"text/json") {
            relicaServerInfo(runningCmdsSize: runningCmdsSize)
        }
    }

    @Secured(['ROLE_ADMIN','ROLE_ADMIN_SYSTEM'])
    def restartConsole = {

        runAsync{
            Thread.sleep(2000)
            // restart the csvn app server -- exit-code 5 instructs
            // the wrapper to install updates and restart
            System.exit(5)
        }

        render(contentType: "text/json") {
            result(restart: "ok")
        }
    }

}
