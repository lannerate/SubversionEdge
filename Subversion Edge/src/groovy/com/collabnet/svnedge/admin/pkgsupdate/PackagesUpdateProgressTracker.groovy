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
package com.collabnet.svnedge.admin.pkgsupdate;

import java.text.DecimalFormat

import grails.converters.JSON
import org.codehaus.groovy.grails.commons.ApplicationHolder as AH

import org.apache.log4j.Logger

import org.cometd.Client
import org.cometd.Channel

import com.sun.pkg.client.Fmri
import com.sun.pkg.client.ImagePlanProgressTracker
import com.sun.pkg.client.Action

/**
 * Outputs the progress of the actions from the pkg.
 * 

 startDownloadPhase
 for(each fmri that requires download) {
   startPackageDownload
   for(each file in this fmri that requires download) {
     startFileDownload
     while(transfer is in progress)
       onFileDownloadProgress
     endFileDownload
   }
   endPackageDownload
 }
 endDownloadPhase
 startActions
 startRemovalPhase
 for(each removal action) {
   onRemovalAction
 }
 endRemovalPhase
 startUpdatePhase
 for(each Update action) {
   onUpdateAction
 }
 endUpdatePhase
 startInstallPhase
 for(each Install action) {
   onInstallAction
 }
 endInstallPhase
 
 * 
 * @author Marcello de Sales (mdesales@collab.net)
 *
 */
class PackagesUpdateProgressTracker extends ImagePlanProgressTracker {

    Logger log = Logger.getLogger(getClass()) // log4j
    /**
     * The bayeux publisher client
     */
    private Client bayeuxPublisherClient
    /**
     * The Bayeux publisher Status message progress channel
     */
    private Channel statusMessageChannel
    /**
     * The Bayeux publisher percentage messages progress channel
     */
    private Channel percentageMessageChannel
    /**
     * The upgrade release number being updated
     */
    private String upgradeReleaseNumber
    /**
     * The upgrade branch number being updated
     */
    private String upgradeBranchNumber
    /**
     * The status of the progress if it has finished
     */
    private boolean finishedProcess
    /**
     * Overall percentage
     */
    private int overallPercentage
    /**
     * Current percentage
     */
    private int currentPercentage
    /**
     * Current phase
     */
    private String currentPhase
    /**
     * The total number of files to be downloaded
     */
    private int totalDownloadFiles
    /**
     * The total number of files to be removed
     */
    private int totalRemovalActions
    /**
     * Current number of removal action
     */
    private int currentRemovalAction
    /**
     * The total number of files to be updated
     */
    private int totalUpdateActions
    /**
     * Current number of update action
     */
    private int currentUpdateAction
    /**
     * The total number of files to be updated
     */
    private int totalInstallActions
    /**
     * Current number of update action
     */
    private int currentInstallAction
    /**
     * If the package csvn was updated
     */
    private boolean csvnUpdated
    /**
     * If the package csvn-svn was updated
     */
    private boolean subversionUpdated
    /**
     * The svn updated version
     */
    private String svnUpdatedVersion
    /**
     * The path of the pkg image, that is, the current application path.
     * This path is used to write to update information on file system.
     */
    private String imagePath
    /**
     * If new packages were installed.
     */
    private boolean newPackagesInstalled

    /**
     * Locale for messages that emanate from this class
     */
    private Locale locale

    /**
     * Builds a new progress tracker with the given print writer
     * @param outputWriter is the print writer where the output must be 
     * redirected.
     */
    private PackagesUpdateProgressTracker(String imagePath, Client client, 
                Channel statusChannel, Channel percentagesChannel, 
                boolean newPackages, Locale locale) {

        this.imagePath = imagePath
        this.bayeuxPublisherClient = client
        this.statusMessageChannel = statusChannel
        this.percentageMessageChannel = percentagesChannel
        this.newPackagesInstalled = newPackages
        this.locale = locale
        if (!this.locale) {
           this.locale = Locale.getDefault()
        }
    }

    /**
     * Factory method to create a new progress tracker
     * @param client is the client interface for the bayeux server
     * @param channel is the channel interface for the bayeux server
     * @return a new instance of the progress tracker
     */
    public static PackagesUpdateProgressTracker makeNew(String imagePath, 
            Client client, Channel statusChannel, Channel percentagesChannel,
            boolean newPackages, Locale locale) {

        return new PackagesUpdateProgressTracker(imagePath, client, 
                statusChannel, percentagesChannel, newPackages, locale)
    }

    /**
     * A synchronous publisher that publishes the given message in the 
     * Bayeux server with the following Json doc:
     * 
     *  {
     *      phase: String = the current phase
     *      statusMessage: String = status message
     *  }
     *  
     * 
     * @param message is a string containing the message to be published.
     */
    private void publishStatusMessage(message) {
        this.publishPercentageMessage()
        def response = [phase: this.currentPhase,
                        statusMessage: message
                       ]
        def jsonRes = (response as JSON).toString()
        log.debug("Status JSON: " + jsonRes)
        this.statusMessageChannel.publish(this.bayeuxPublisherClient, 
                    jsonRes, null)
    }

    /**
     * A synchronous publisher that publishes the given message in the 
     * Bayeux server with the following Json doc:
     * 
     *  {
     *      overallPercentage: int = the overall percentage step
     *  }
     *  
     */
    private void publishPercentageMessage() {
        def response = [overallPercentage: this.overallPercentage]
        def jsonRes = (response as JSON).toString()
        log.debug("Percentage JSON: " + jsonRes)
        this.percentageMessageChannel.publish(this.bayeuxPublisherClient, 
                    jsonRes, null)
    }

    /**
     * @param fileSize the total file size in bytes
     * @return the formatted size of the file size in KB or MB, depending on
     * the size of the file.
     */
    def getFormattedFileSize(fileSize) {
        def kBytes = (float)fileSize / (float)1024
        if (fileSize > 1024 * 1024) {
            def mBytes = kBytes /(float)1024
            return new DecimalFormat("###,##0.00" ).format(mBytes) + " MB"
        } else {
            return new DecimalFormat("###,##0.00" ).format(kBytes) + " KB"
        }
    }

    /* (non-Javadoc)
     * @see com.sun.pkg.client.ImagePlanProgressTracker#s     *  
     * @param message is the tartDownloadPhase(int)
     */
    public void startDownloadPhase(int totalPackages) {

        String total
        if (totalPackages > 1) {
            total = getMessage("packagesUpdate.progressTracker.packageCount.plural", [totalPackages])
        } else {
            total = getMessage("packagesUpdate.progressTracker.packageCount.singular", [totalPackages])
        }
        this.overallPercentage = 0
        this.currentPercentage = 0
        this.currentPhase = getMessage("packagesUpdate.progressTracker.phase.downloading")
        this.publishStatusMessage(getMessage("packagesUpdate.progressTracker.phase.downloading.status", [total]))
    }

    /* (non-Javadoc)
     * @see com.sun.pkg.client.ImagePlanProgressTracker#startPackageDownload(com.sun.pkg.client.Fmri, int, long)
     */
    public void startPackageDownload(Fmri pkg, int totalDownloadFiles, 
            long totalDownloadSize) {

        if (pkg.getName().equals("csvn")) {
            this.upgradeReleaseNumber = pkg.getVersion().getRelease().toString()
            this.upgradeBranchNumber = pkg.getVersion().getBranch().toString()
            this.csvnUpdated = true
        }

        if (pkg.getName().equals("csvn-svn")) {
            this.svnUpdatedVersion = pkg.getVersion().getRelease().toString() +
                    "-" + pkg.getVersion().getBranch().toString()
            this.subversionUpdated = true
        }

        String totalFiles
        if (totalDownloadFiles > 1) {
            totalFiles = getMessage("packagesUpdate.progressTracker.fileCount.plural", [totalDownloadFiles])
        } else {
            totalFiles = getMessage("packagesUpdate.progressTracker.fileCount.singular", [totalDownloadFiles])
        }
        this.totalDownloadFiles = totalDownloadFiles
        def formattedSize = this.getFormattedFileSize(totalDownloadSize)
        this.publishStatusMessage (getMessage("packagesUpdate.progressTracker.download.package", [pkg.getName(), totalFiles, formattedSize]))
    }

    /* (non-Javadoc)
     * @see com.sun.pkg.client.ImagePlanProgressTracker#startFileDownload(int, long)
     */
    public void startFileDownload(int index, long fileSize) {
        this.overallPercentage = 15
        this.currentPercentage = Math.round(
                    (index / this.totalDownloadFiles) * 100)
        this.publishStatusMessage (getMessage("packagesUpdate.progressTracker.download.file",
                [this.getFormattedFileSize(fileSize), ++index, this.totalDownloadFiles]))
    }

    /* (non-Javadoc)
     * @see com.sun.pkg.client.ImagePlanProgressTracker#endPackageDownload(com.sun.pkg.client.Fmri, int)
     */
    public void endPackageDownload(Fmri pkg, int totalDownloadFiles) {
        this.overallPercentage = 20
        this.currentPercentage = 100
        this.publishStatusMessage(getMessage("packagesUpdate.progressTracker.download.finished", [pkg.getName()]))
    }

    /* (non-Javadoc)
     * @see com.sun.pkg.client.ImagePlanProgressTracker#endDownloadPhase()
     */
    public void endDownloadPhase() {
        this.overallPercentage = 25
        this.currentPercentage = 0
        this.currentPhase = getMessage("packagesUpdate.progressTracker.phase.downloading.finished")
        this.publishStatusMessage(getMessage("packagesUpdate.progressTracker.phase.downloading.finished.status"))
    }
    
    /* (non-Javadoc)
     * @see com.sun.pkg.client.ImagePlanProgressTracker#startActions(int)
     */
    public void startActions(int totalActions) {
        this.overallPercentage = 30
        this.currentPercentage = 0
        this.currentPhase = getMessage("packagesUpdate.progressTracker.phase.preparing")
        this.publishStatusMessage(getMessage("packagesUpdate.progressTracker.phase.preparing.status"))
    }

    /* (non-Javadoc)
     * @see com.sun.pkg.client.ImagePlanProgressTracker#startRemovalPhase(int)
     */
    public void startRemovalPhase(int totalRemovalActions) {
        this.overallPercentage = 35
        this.currentPercentage = 0
        this.currentPhase = getMessage("packagesUpdate.progressTracker.phase.removing")
        this.totalRemovalActions = totalRemovalActions 
        this.publishStatusMessage(getMessage("packagesUpdate.progressTracker.phase.removing.status", [totalRemovalActions]))
    }

    /* (non-Javadoc)
     * @see com.sun.pkg.client.ImagePlanProgressTracker#onRemovalAction(com.sun.pkg.client.Action)
     */
    public void onRemovalAction(Action a) {
        this.overallPercentage = 35
        this.currentPercentage = Math.round((
                ++this.currentRemovalAction / this.totalRemovalActions) * 100)
        this.totalRemovalActions = totalRemovalActions
        this.publishPercentageMessage()
    }

    /* (non-Javadoc)
     * @see com.sun.pkg.client.ImagePlanProgressTracker#endRemovalPhase()
     */
    public void endRemovalPhase() {
        this.overallPercentage = 45
        this.currentPercentage = 100
        this.currentPhase = getMessage("packagesUpdate.progressTracker.phase.removing.finished")
        this.publishStatusMessage(getMessage("packagesUpdate.progressTracker.phase.removing.finished.status"))
    }

    /* (non-Javadoc)
     * @see com.sun.pkg.client.ImagePlanProgressTracker#startUpdatePhase(int)
     */
    public void startUpdatePhase(int totalUpdateActions) {
        this.overallPercentage = 60
        this.currentPercentage = 0
        this.totalUpdateActions = totalUpdateActions
        this.currentPhase = getMessage("packagesUpdate.progressTracker.phase.updating")
        this.publishStatusMessage(getMessage("packagesUpdate.progressTracker.phase.updating.status"))
    }

    /* (non-Javadoc)
     * @see com.sun.pkg.client.ImagePlanProgressTracker#onUpdateAction(com.sun.pkg.client.Action, com.sun.pkg.client.Action)
     */
    public void onUpdateAction(Action from, Action to) {
        this.overallPercentage = 70
        this.currentPercentage = Math.round((
                ++this.currentUpdateAction / this.totalUpdateActions) * 100)
        this.publishPercentageMessage()
    }

    /* (non-Javadoc)
     * @see com.sun.pkg.client.ImagePlanProgressTracker#endUpdatePhase()
     */
    public void endUpdatePhase() {
        this.overallPercentage = 75
        this.currentPercentage = 100
        this.currentPhase = getMessage("packagesUpdate.progressTracker.phase.updating.finished")
        this.publishStatusMessage(getMessage("packagesUpdate.progressTracker.phase.updating.finished.status"))
    }

    /* (non-Javadoc)
     * @see com.sun.pkg.client.ImagePlanProgressTracker#startInstallPhase(int)
     */
    public void startInstallPhase(int totalInstallActions) {
        this.overallPercentage = 80
        this.currentPercentage = 0
        this.currentPhase = getMessage("packagesUpdate.progressTracker.phase.installing")
        this.totalInstallActions = totalInstallActions
        if (this.newPackagesInstalled) {
            this.publishStatusMessage(getMessage("packagesUpdate.progressTracker.phase.installing.package.status"))
        } else {
            this.publishStatusMessage(getMessage("packagesUpdate.progressTracker.phase.installing.file.status"))
        }
    }

    /* (non-Javadoc)
     * @see com.sun.pkg.client.ImagePlanProgressTracker#onInstallAction(com.sun.pkg.client.Action)
     */
    public void onInstallAction(Action a) {
        this.overallPercentage = 90
        this.currentPercentage = Math.round((
                ++this.currentInstallAction / this.totalInstallActions) * 100)
        this.publishPercentageMessage()
    }

    /* (non-Javadoc)
     * @see com.sun.pkg.client.ImagePlanProgressTracker#endInstallPhase()
     */
    public void endInstallPhase() {
        this.overallPercentage = 100
        this.currentPercentage = 100
        this.currentPhase = getMessage("packagesUpdate.progressTracker.phase.installing.finished")
        this.publishStatusMessage(getMessage("packagesUpdate.progressTracker.phase.installing.finished.status"))

        this.overallPercentage = 100
        this.currentPercentage = 100
        this.currentPhase = this.newPackagesInstalled ?
                getMessage("packagesUpdate.progressTracker.phase.final.package") :
                getMessage("packagesUpdate.progressTracker.phase.final.file")
        def upgrVrs = "${this.upgradeReleaseNumber}-${this.upgradeBranchNumber}"
        if (!this.newPackagesInstalled) {
            this.publishStatusMessage(getMessage("packagesUpdate.progressTracker.phase.final.file.status", [upgrVrs]))
        } else {
            this.publishStatusMessage(getMessage("packagesUpdate.progressTracker.phase.final.package.status"))
        }
        this.finishedProcess = true
        //Write the updated version on the flag file
        this.writeSoftwareUpdateFlagFile(upgrVrs)
    }

    /**
     * Write the software updated version in file.
     */
    private void writeSoftwareUpdateFlagFile(version) {
        def statusUpdateFilePath = this.imagePath + "/data/run/"
        def updatedFile = null
        if (!this.newPackagesInstalled) {
            updatedFile = new File(statusUpdateFilePath, "csvn.updated")
        } else {
            updatedFile = new File(statusUpdateFilePath, "csvn.pckgs.installed")
        }
        log.debug("Writing the update flag file ${updatedFile}")
        try {
            //the update version of the csvn package.
            updatedFile.write("${version}")
            if (updatedFile.exists()) {
                log.debug("Flag file ${updatedFile} successfully written...")
            }
            //the update version of subversion
            if (!this.newPackagesInstalled && this.subversionUpdated) {
                log.debug("Subversion package also updated... Another flag")
                updatedFile = new File(statusUpdateFilePath, "csvn-svn.updated")
                updatedFile.write(this.svnUpdatedVersion)
                if (updatedFile.exists()) {
                    log.debug("Subversion flag file ${updatedFile} " +
                            "successfully written...")
                }
            }

        } catch (Exception e) {
            log.error("Error writing the flag file ${updatedFile}", e)
        }
    }

    /**
     * @return if the process has reached the end install phase
     */
    public boolean hasFinished() {
        return this.finishedProcess
    }

    /**
     * <li>install-updates.bat: Assumes only the web app was updated. It 
     * waits for it to end, copies files and restarts the one service.
     * <li>install-updates.bat apache: Assumes everything was updated. Waits 
     * for both services to end, then copies files and restarts both services.
     * <li>install-updates.bat justapache: Assumes only the Apache app was 
     * updated.  It waits for it to end, copies files and restarts the one 
     * service.
     * 
     * @return the command to be executed to restart the windows server.
     */
    public String getWindowsUpdateCommand() {
        if (this.csvnUpdated && this.subversionUpdated) {
            return "install-updates.bat apache"
        } else
        if (this.csvnUpdated && !this.subversionUpdated) {
            return "install-updates.bat"
        } else
        if (!this.csvnUpdated && this.subversionUpdated) {
            return "install-updates.bat justapache"
        } else return null
    }


    /**
     * Gets an i18n message from the messages.properties file
     * @param key is the key in the messages.properties file
     * (excluding prefix "packagesUpdate.progressTracker" if desired)
     * @param params is the list of parameters to provide the i18n.
     * @return the message related to the key in the messages.properties file
     * using the default locale.
     */
    private def getMessage(String key, params) {

        String keyPrefix = "packagesUpdate.progressTracker."
        if (!key.startsWith(keyPrefix)) {
            key = keyPrefix + key
        }
        def ctx = AH.application.mainContext
        return ctx.getMessage(key, params as String[], this.locale)
    }

    private def getMessage(String key) {
        return getMessage(key, [])
    }
}
