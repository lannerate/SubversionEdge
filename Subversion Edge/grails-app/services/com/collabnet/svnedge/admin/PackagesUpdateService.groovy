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
package com.collabnet.svnedge.admin

import java.io.File;
import java.io.FileNotFoundException;
import java.net.NoRouteToHostException;
import java.net.URL;
import java.net.UnknownHostException;

import com.collabnet.svnedge.net.HttpProxyAuth
import com.collabnet.svnedge.admin.pkgsupdate.PackageInfo 
import com.collabnet.svnedge.admin.pkgsupdate.PackagesUpdateProgressTracker 
import com.collabnet.svnedge.admin.pkgsupdate.PackagesUpdateSecurityPolicy 
import com.collabnet.svnedge.util.ConfigUtil;
import com.sun.pkg.client.Image
import com.sun.pkg.client.Image.FmriState
import com.sun.pkg.client.Image.ImagePlan
import com.sun.pkg.client.ImagePlanProgressTracker

import java.util.HashSet

import org.springframework.beans.factory.InitializingBean
import org.cometd.Client
import org.mortbay.cometd.ChannelImpl

/**
 * The Packages Update service is responsible for managing the updates of the
 * CSVN application.
 * 
 * @author Marcello de Sales (mdesales@collab.net)
 */
public final class PackagesUpdateService implements InitializingBean {

    /**
     * The command line service reference.
     */
    def commandLineService
    /**
     * The lifecycle service reference.
     */
    def lifecycleService

    /**
     * the quartz administration service
     */
    def jobsAdminService

    /**
     * the networking service
     */
    def networkingService

    /**
     * Non-transactional service 
     */
    boolean transactional = false
    /**
     * Auto-wired Cometd bayeux server
     */
    def bayeux
    /**
     * The bayeux publisher client
     */
    private Client bayeuxPublisherClient
    /**
     * The Bayeux publisher Status message progress channel
     */
    private ChannelImpl statusMessageChannel
    /**
     * The Bayeux publisher Status message progress channel
     */
    private ChannelImpl percentageMessageChannel
    /**
     * The current image PATH
     */
    private String imagePath
    /**
     * This is the image directory path of the CSVN application, where the
     * production installation resides. It must contain at least the metadata 
     * directories "org.opensolares,pkg" and "pkg"
     */
    private Image csvnImage
    /**
     * If the service has been successfully bootstraped. That includes if the
     * directory where the application is running from contains the pkg metadata
     * directory.
     */
    private boolean bootstraped
    /**
     * If there are updates available
     */
    private boolean areThereUpdates
    /**
     * If the system has been updated since last bootstrap due to software 
     * updates
     */
    private boolean hasSystemBeenUpdated
    /**
     * If the Svn Server has been updated since last bootstrap due to software 
     * updates
     */
    private boolean hasSvnServerBeenUpdated
    /**
     * If the system has new package that were installed since the last 
     * bootstrap due to the installation of new packages.
     */
    private boolean hasNewPackagesBeenInstalled
    /**
     * The release number of the installed csvn package.
     */
    private String installedReleaseNumber
    /**
     * The branch number of the installed csvn package.
     */
    private String installedBranchNumber
    /**
     * The release number of the installed Subversion package.
     */
    private String installedSvnReleaseNumber
    /**
     * The branch number of the installed Subversion package.
     */
    private String installedSvnBranchNumber
    /**
     * The packages update progress tracker
     */
    private ImagePlanProgressTracker progressTracker
    /**
     * If the server needs to be restarted after an update has been applied.
     */
    private boolean systemNeedsRestart = false
    /**
     * The origin where the packages are being downloaded from. This is defined
     * in the image metadata.
     */
    private String originURL
    /**
     * The proxyURL is the one used to access the originURL, in case one is 
     * necessary.
     */
    private String proxyToOriginURL
    /**
     * An internal cache of the currently installed packages in case there is
     * no connection with the originURL. No caches available for the packages
     * to be upated or new ones.
     */
    private PackageInfo[] installedPackagesCache
    /**
     * An internal cache of the new packages.
     */
    private PackageInfo[] newPackagesCache
    /**
     * An internal cache of the updatable packages.
     */
    private PackageInfo[] upgradablePackagesCache
    /**
     * The time out to connect to the origin URL
     */
    private final int REPO_TIMEOUT = 3000
    /**
     * Our pkg authority string for our repos
     */
    private final String COLLABNET = "collab.net"

    /**
     * @param appHomePath is the home directory of the installation.
     * @throws FileNotFoundException if the given appHomePath is not a valid
     * Sun Update image directory.
     * @throws NoRouteToHostException if there's no connection to the 
     * origin URL.
     */
    def bootstrap = { config ->

        def appHomePath = config.svnedge.softwareupdates.imagepath
        this.imagePath = appHomePath
        try {
            this.verifyImageDirectoryPath(appHomePath)

            this.csvnImage = new Image(new File(appHomePath))
            try {
                // This should set the UUID in the image if not set
                this.csvnImage.setAuthority(this.COLLABNET, null, null)
            } catch (Exception e) {
                // Ignore exceptions
            }
            this.retrieveImageOriginUrl()
            this.retrieveInstalledPackagesVersionNumber()
            this.initializeCaches()
            this.verifyAndRemoveSoftwareUpdateFlagFile()
            if (this.hasTheSvnServerBeenUpdated()) {
                log.info("Restarting the Svn service after applying update...")
                this.restartSvnServer()
            }
            this.setProxyFromSystemEnvironment()
            this.reloadPackagesAndUpdates()
            this.bootstraped  = true

            log.debug("The Bayeux comet client/server service has been " +
                "setup for the software updates service.")

        } catch (NoRouteToHostException nrthe) {
            log.error("There is no network connection to the image server at " +
                    this.originURL, nrthe)
            if (this.proxyToOriginURL) {
                log.error("Tried connecting to the server using the proxy " + 
                    this.proxyToOriginURL)
            }

        } catch (Exception directoryNotFound) {
            log.error("Packages Update not initialized: " 
                + directoryNotFound.message)
        }
        jobsAdminService.createOrReplaceTrigger(PackagesUpdateJob.createTrigger())
    }

    // just like @PostConstruct
    void afterPropertiesSet() {
        this.bayeux.setSecurityPolicy(new PackagesUpdateSecurityPolicy())
        this.bayeuxPublisherClient = this.bayeux.newClient(this.class.name)
        def channelName = "/csvn-updates/status"
        def percentageChannel = "/csvn-updates/percentages"
        def create = true
        this.statusMessageChannel = this.bayeux.getChannel(channelName, create)
        this.percentageMessageChannel = this.bayeux.getChannel(
                    percentageChannel, create)
    }

    /**
     * Initialize the caches
     */
    private void initializeCaches() {
        this.upgradablePackagesCache = new PackageInfo[0]
        this.newPackagesCache = new PackageInfo[0]
        this.installedPackagesCache = new PackageInfo[0]
    }

    /**
     * Sets the property from the system environment in case it is set. The
     * environment variable is "http_proxy", the same one as the pkg client
     * creates based on the updatetool configuration.
     */
    def setProxyFromSystemEnvironment() throws UnknownHostException, 
            IOException {

        String proxyUrl = networkingService.httpProxy
        log.debug("Loading any proxy configuration... " + proxyUrl ?: "None")
        try {
            this.proxyToOriginURL = proxyUrl?.trim()
            def prx = (this.proxyToOriginURL) ? 
                    HttpProxyAuth.newInstance(new URL(this.proxyToOriginURL)) :
                    null
            log.debug("Setting the Proxy server from the environment...")
            this.csvnImage.setProxy(prx)
            log.debug("Proxy Url: " + (this.proxyToOriginURL) ?: "None")
        } catch (IOException ioe) {
            log.error("Can't connect to proxy: " + ioe.getMessage(), ioe)
        } catch (IllegalArgumentException iae) {
            log.error("The format of the proxy URL is incorrect: " + 
                    this.proxyToOriginURL)
        } catch (Exception e) {
            log.error ("Proxy configuration could not be set: ${e.message}")
        }
    }

    /**
     * @return The release number of the installed packages.
     */
    def getInstalledReleaseNumber() {
        return this.installedReleaseNumber
    }

    /**
     * @return The branch number of the installed packages.
     */
    def getInstalledBranchNumber() {
        return this.installedBranchNumber
    }

    /**
     * @return The current installed version, which is comprised of the
     * release number + "-" + branch number.
     */
    def getInstalledVersionNumber() {
        if (this.installedBranchNumber) {
            return this.installedReleaseNumber + "-" + 
                    this.installedBranchNumber
        } else {
            return null
        }
    }

    /**
     * @return The current installed Subversion version, which is comprised 
     * of the release number + "-" + branch number.
     */
    def getInstalledSvnVersionNumber() {
        //if the version was not cached at startup, give it another chance.
        if (this.installedSvnBranchNumber) {
            return this.installedSvnReleaseNumber + "-" + 
                    this.installedSvnBranchNumber
        } else {
            return null
        }
    }

    /**
     * @return whether or not the service has been bootstrapped
     */
    def hasBeenBootstraped() {
        return this.bootstraped
    }

    /**
     * @param imagePath is the absolute path to the csvn production image 
     * directory.
     * @return verifies if the given imagePath is a pkg directory.
     * @throws FileNotFoundException if the imagePath provided is not a regular
     * pkg image path, containing the directories .org.opensolaris,pkg and pkg
     */
    private void verifyImageDirectoryPath(imagePath) 
            throws FileNotFoundException {

        if (!new File(imagePath).exists()) {
            throw new FileNotFoundException("The directory " +
                    "${imagePath} does not exist!")
        }
        def requiredDirs = [new File(imagePath, ".org.opensolaris,pkg"),
                    new File(imagePath, "pkg")]
        for (reqDir in requiredDirs) {
            if (!reqDir.exists()) {
                throw new FileNotFoundException("The directory " +
                        "${imagePath} does not seem to contain Sun Update" +
                        "Packages. The directory ${reqDir} was not found!")
            }
        }
    }

    /** 
     * @return The current installed version, which is comprised of the
     * release number + "-" + branch number.
     */
    def retrieveInstalledPackagesVersionNumber() {
        def installedFilesPath = this.imagePath + File.separator + 
                ".org.opensolaris,pkg" + File.separator + "state" + 
                File.separator + "installed"
        def installedFilesDir = new File(installedFilesPath)
        if (installedFilesDir.exists()) {
            //Get the version of the packages from the metadata file
            for(fileName in installedFilesDir.list()) {
                //csvn-svn@1.6.11%2C2.6.18.8.5-704.322%3A20100517T115213Z
                if (!this.installedSvnReleaseNumber && 
                        fileName.contains("csvn-svn")) {
                    this.installedSvnReleaseNumber = fileName.split("@")[1].
                            split("%")[0]
                    this.installedSvnBranchNumber = fileName.split("-")[2].
                            split("%")[0]
                } else //csvn@1.0.0%2C2.6.18.8.5-704.322%3A20100517T115346Z
                if (!this.installedReleaseNumber && fileName.contains("csvn") 
                        && !fileName.contains("-svn")) {
                    this.installedReleaseNumber = fileName.split("@")[1].
                            split("%")[0]
                    this.installedBranchNumber = fileName.split("-")[1].
                            split("%")[0]
                }
                if (installedSvnReleaseNumber && installedReleaseNumber) {
                    break
                }
            }
        }
    }

    /**
     * @return The reference to the image directory where the Sun Update Center
     * pkg(5) is installed. This is the production directory of the csvn app,
     * containing at least the metadata directory "org.opensolares,pkg" and
     * the directory pkg.
     */
    def getCSvnImage() {
        if (this.csvnImage == null) {
            throw new IllegalStateException("The packages update service " +
                    "needs to be bootstrapped with the appropriate image " +
                    "path. Current: ${this.imagePath}")
        }
        return this.csvnImage
    }

    /**
     * If the svn server is running, then stop and restart it after a 
     * software update.
     */
    public void restartSvnServer() {
        if (System.getProperties().get("os.name").contains("Win")) {
            //The Windows version is restarted from the wrapper.
            return
        }
        try {
            //restart the Linux svn server
            if (this.lifecycleService.isStarted()) {
                
                def result = this.lifecycleService.stopServer()
                if (result < 0) {
                    log.warn("The Subversion Server did not stop.")
                }

                result = this.lifecycleService.startServer()
                if (result < 0) {
                    log.warn("The Subversion Server was already " +
                            "running while attempting to restart " +
                            "itself.")
                } else
                if (result == 0) {
                    log.info("Subversion Server was restarted " +
                            "successfully.")
                } else {
                    log.error("There was a problem restarting the " +
                            "Subversion Server!")
                }
            }

        } catch (Exception e) {
            log.error("There was a problem restarting the Subversion " +
                    "Server!", e)
        }
    }

    /**
     * Deleting the data/run/csvn.udpated artifact after an update.
     */
    private void verifyAndRemoveSoftwareUpdateFlagFile() {
        def statusUpdateFilePath = this.imagePath + "/data/run"
        def updatedFile = new File(statusUpdateFilePath, "csvn.updated")
        if (updatedFile.exists()) {
            this.hasSystemBeenUpdated = true
            log.info("File ${updatedFile} exists... Deleting it...")
            updatedFile.delete()
            log.debug("File ${updatedFile} removed...")
        }
        updatedFile = new File(statusUpdateFilePath, "csvn-svn.updated")
        if (updatedFile.exists()) {
            this.hasSvnServerBeenUpdated = true
            log.info("File ${updatedFile} exists... Deleting it...")
            updatedFile.delete()
            log.debug("File ${updatedFile} removed...")
        }
        updatedFile = new File(statusUpdateFilePath, "csvn.pckgs.installed")
        if (updatedFile.exists()) {
            this.hasNewPackagesBeenInstalled = true
            log.info("File ${updatedFile} exists... Deleting it...")
            updatedFile.delete()
            log.debug("File ${updatedFile} removed...")
        }
    }

    /**
     * @return the string to show during the system initialization after 
     * software updates were installed.
     */
    public boolean hasTheSystemBeenUpdated() {
        return this.hasSystemBeenUpdated
    }

    /**
     * @return the string to show during the system initialization after 
     * software updates were installed.
     */
    public boolean hasTheSvnServerBeenUpdated() {
        return this.hasSvnServerBeenUpdated
    }

    /**
     * @return the string to show during the system initialization after 
     * software updates were installed.
     */
    public boolean hasNewPackagesBeenInstalled() {
        return this.hasNewPackagesBeenInstalled
    }

    /**
     * Puts the service back in the state of not been updated.
     */
    public void setTheSystemToNotBeenUpdated() {
        this.hasSystemBeenUpdated = false
        this.hasSvnServerBeenUpdated = false
        this.hasNewPackagesBeenInstalled = false
    }

    /**
     * @return an instance of File for the root directory of the csvn image
     */
    def getImageRootDirectoryFile() {
        return this.getCSvnImage().rootDirectory
    }

    /**
     * @return Retrieves the URL of the origin of the image.
     */
    private String retrieveImageOriginUrl() {
        def csvnImage = this.getCSvnImage()
        for (line in csvnImage.lines) {
            if (line.contains("origin")) {
                this.originURL = line.replace(" ", "").split("=")[1]
                break
            }
        }
    }

    /**
     * @return The origin URL from the image.
     */
    def getImageOriginUrl() {
        return this.originURL
    }

    /**
     * @return The origin URL from the image.
     */
    def getImageProxyToOriginUrl() {
        return this.proxyToOriginURL
    }

    /**
     * @return A Collection<Image.FmeriState> of all packages, installed or
     * not, in the image directory from the inventory list. This list includes
     * all the current and previous versions of each of the packages.
     * @throws NoRouteToHostException if there's no connection to the image
     * repository.
     * @throws Exception if any error occurs while retrieving the packages
     * contents.
     */
    def getAllPackages() throws NoRouteToHostException, Exception {
        def allPkgs = null
        def allKnownVersions = true
        //all known versions, that is, including the ones from the past
        try {
            return this.getCSvnImage().getInventory(allPkgs, allKnownVersions)
        } catch (NoRouteToHostException nrthe) {
            throw new NoRouteToHostException("There is no network connection " +
                    "to the image server at " + this.originURL)
        } catch (Exception ioe) {
            throw new Exception("An error occurred while loading the packages" +
                    ":" + ioe.getMessage(), ioe)
        }
    }

    /**
     * @return A Collection<Image.FmeriState> of the most recent version of the
     * packages available in the image directory from the inventory list. The
     * state of the packages in this list can be either installed or not, and
     * updatable or not.
     * @throws NoRouteToHostException if there's no connection to the image
     * repository.
     * @throws Exception if any error occurs while retrieving the packages
     * contents.
     */
    private getMostRecentPackages() throws NoRouteToHostException, Exception {
        def allPkgs = null
        def allKnownVersions = false 
        //most recent version only for all packages
        return this.getCSvnImage().getInventory(allPkgs, allKnownVersions)
    }

    /**
     * Refreshes the packages in the file system
     */
    private void refreshPackagesFileSystem() throws NoRouteToHostException, 
            UnknownHostException {

        try {
            this.getCSvnImage().refreshCatalogs()
        } catch (NoRouteToHostException nrthe) {
            throw new NoRouteToHostException(nrthe.getMessage())
        } catch (UnknownHostException uhe) {
            throw new UnknownHostException(uhe.getMessage())
        }
    }

    /**
     * Retrieves all the package versions by the state of the installed variable
     * @param isInstalled if the status of the package is installed
     * @return a Collection<Image.FmriState> with packages in the image 
     * directory that has its installed status equals to the given value of 
     * the parameter statusIsInstalled.
     * @throws NoRouteToHostException if there's no connection to the image
     * repository.
     * @throws Exception if any error occurs while retrieving the packages
     * contents.
     */
    private getPackagesByInstalledStatus(boolean isInstalled) throws 
            NoRouteToHostException, Exception {

        this.refreshPackagesFileSystem()
        Collection<Image.FmriState> installedPackages = 
                new HashSet<Image.FmriState>()
        //All packages are only related to the ones that are installed
        for (Image.FmriState fs : this.getMostRecentPackages()) {
            if (fs.installed == isInstalled) {
                installedPackages << fs
            }
        }
        return installedPackages
    }

    /**
     * @return a Collection<Image.FmriState> with all the installed packages. 
     * The updatetool GUI refers to this list as the "Installed Components".
     * @throws NoRouteToHostException if there's no connection to the image
     * repository.
     * @throws Exception if any error occurs while retrieving the packages
     * contents.
     */
    def getInstalledPackages() throws NoRouteToHostException, Exception {
        def installed = true
        return this.getPackagesByInstalledStatus(installed)
    }

    /**
     * @return a Collection<Image.FmriState> with all the non-installed 
     * packages. The updatetool GUI refers to this list as the "Available 
     * Add-ons".
     * @throws NoRouteToHostException if there's no connection to the image
     * repository.
     * @throws Exception if any error occurs while retrieving the packages
     * contents.
     */
    def getNonInstalledPackages() throws NoRouteToHostException, Exception {
        def installed = false
        return this.getPackagesByInstalledStatus(installed)
    }

    /**
     * @return A Collection<Image.FmriState> of updatable packages for the 
     * given image directory. The updatetool GUI refers to this list as the 
     * "Available Updates".
     * @throws NoRouteToHostException if there's no connection to the image
     * repository.
     * @throws Exception if any error occurs while retrieving the packages
     * contents.
     */
    def getUpgradablePackages() throws NoRouteToHostException, Exception {
        this.refreshPackagesFileSystem()
        Map<String, Image.FmriState> packagesToInstall = 
                new LinkedHashMap<String, Image.FmriState>()
        for (Image.FmriState fs : this.getAllPackages()) {
            if (!fs.installed && !fs.upgradable) {
                //not installed and not upgradable
                if (packagesToInstall.get(fs.fmri.name)) {
                    def csvnStateVersion = packagesToInstall.get(fs.fmri.name)
                    if (fs.fmri.getVersion().isSuccessor(
                                csvnStateVersion.fmri.getVersion())) {
                        packagesToInstall[fs.fmri.name] = fs
                    }
                } else {
                    packagesToInstall[fs.fmri.name] = fs
                }
            }
        }
        return packagesToInstall.values()
    }

    /**
     * @return PackageInfo[] with basic information about the packages to be
     * updated
     */
    private PackageInfo[] makePackageInfoArray(
            Collection<Image.FmriState> pkgStates) {
        PackageInfo[] newUpdates = new PackageInfo[pkgStates.size()]
        int i = -1
        for(newPkgUpdateState in pkgStates) {
            newUpdates[++i] = PackageInfo.makeNewPackageInfo(this.csvnImage, 
                    newPkgUpdateState)
        }
        return newUpdates
    }

    
    /**
     * @return an instance of PackageInfo[] with the packages info for the
     * new packages that can be added.
     * @throws NoRouteToHostException if there's no connection to the image
     * repository.
     * @throws Exception if any error occurs while retrieving the packages
     * contents.
     */
    private PackageInfo[] retrieveNewPackagesInfo() throws 
            NoRouteToHostException, Exception {

        def installedPackages = this.getInstalledPackages()
        def upgradable = this.getUpgradablePackages()
        def addOns = new HashSet<Image.FmriState>()
        outer:
        for (add in upgradable) {
            for (inst in installedPackages) {
                if (add.fmri.getName().equals(inst.fmri.getName())) {
                    continue outer
                }
            }
            addOns << add
        }
        return this.makePackageInfoArray(addOns)
    }

    /**
     * @return an instance of PackageInfo[] with the packages info for the
     * packages that can be upgradable.
     * @throws NoRouteToHostException if there's no connection to the image
     * repository.
     * @throws Exception if any error occurs while retrieving the packages
     * contents.
     */
    private PackageInfo[] retrieveUpgradablePackagesInfo() throws 
            NoRouteToHostException, Exception {

        def installedPackages = this.getInstalledPackages()
        def upgradable = this.getUpgradablePackages()
        def addOns = new HashSet<Image.FmriState>()
        outer:
        for (add in upgradable) {
            for (inst in installedPackages) {
                if (add.fmri.getName().equals(inst.fmri.getName())) {
                    addOns << add
                    continue outer
                }
            }
        }
        return this.makePackageInfoArray(addOns)
    }

    /**
     * @return an instance of PackageInfo[] with the packages info for 
     * installed packages.
     * @throws NoRouteToHostException if there's no connection to the image
     * repository.
     * @throws Exception if any error occurs while retrieving the packages
     * contents.
     */
    private PackageInfo[] retrieveInstalledPackagesInfo() throws 
            NoRouteToHostException, Exception {

        return this.makePackageInfoArray(this.getInstalledPackages())
    }

    /**
     * Reloads the installed packages
     */
    def reloadInstalledPackages() {
        this.installedPackagesCache = this.retrieveInstalledPackagesInfo()
    }

    /**
     * Reloads the updatable packages. After this call, execute
     * this.areTherePackagesUpdates() to find out if there are updates
     * available.
     */
    def reloadUpdatablePackages() {
        this.upgradablePackagesCache = this.retrieveUpgradablePackagesInfo()
        this.areThereUpdates = this.upgradablePackagesCache.size() > 0
    }

    /**
     * Reloads the new packages.
     */
    def reloadNewPackages() {
        this.newPackagesCache = this.retrieveNewPackagesInfo()
    }

    /**
     * Checks for software Updates. The result can be verified with
     * PackagesUpdateService.areThereUpdatesAvailable()
     */
    def reloadPackagesAndUpdates() throws NoRouteToHostException, Exception {
        this.reloadInstalledPackages()
        this.reloadUpdatablePackages()
        this.reloadNewPackages()
        this.retrieveInstalledPackageVersion()
    }

    /**
     * @return The cache of the upgradable packages
     */
    def getUpgradablePackagesInfo() {
        return this.upgradablePackagesCache
    }

    /**
     * @return The cache of the new packages
     */
    def getNewPackagesInfo() {
        return this.newPackagesCache
    }

    /**
     * @return The cache of the installed packages
     */
    def getInstalledPackagesInfo() {
        return this.installedPackagesCache
    }

    /**
     * @return if there are packages addons to be installed.
     */
    def areThereNewPackagesAddOns() {
        return this.newPackagesCache.size() > 0
    }

    /**
     * @return an instance of com.sun.pkg.client.Version for the currently
     * installed package.
     * @throws NoRouteToHostException if there's no connection to the image
     * repository.
     * @throws Exception if any error occurs while retrieving the packages
     * contents.
     */
    def retrieveInstalledPackageVersion() throws NoRouteToHostException, 
            Exception {
        try {
            this.refreshPackagesFileSystem()
        } catch (Exception e) {
            log.warn("Error while reloading the pkg packages...", e)
        }
        for (Image.FmriState fs : this.getMostRecentPackages()) {
            if (fs.fmri.name.equals("csvn") && fs.installed == true) {
                this.installedReleaseNumber = fs.fmri.getVersion().getRelease().
                        toString()
                this.installedBranchNumber = fs.fmri.getVersion().getBranch().
                        toString()
            } else
            if (fs.fmri.name.equals("csvn-svn") && fs.installed == true) {
                this.installedSvnReleaseNumber = fs.fmri.getVersion().
                        getRelease().toString()
                this.installedSvnBranchNumber = fs.fmri.getVersion().
                        getBranch().toString()
            }
        }
    }

    /**
     * @return if there are updates available for the current image.
     */
    def areThereUpdatesAvailable() {
        return this.areThereUpdates
    }

    /**
     * Performs the software installation
     * @param packagesToInstall is the collection of package names to be 
     * installed. Those packages are on the state of updatable.
     * @param locale the request context locale (used for cometd messages)
     * @throws NoRouteToHostException if there's no connection to the image
     * repository.
     * @throws IOException if any IO problems occur while installing new 
     * packages. This can be related to file permissions, missing files, etc.
     */
    def installPackages(Collection<Image.FmriState> packagesToInstall,
            boolean newPackages, Locale locale)
            throws NoRouteToHostException, IOException {

        if (packagesToInstall.size() > 0) {
            List<Image.FmriState> csvnPackages = 
                    new LinkedList<Image.FmriState>()
            for(pkg in packagesToInstall) {
                csvnPackages << pkg.fmri
            }
            ImagePlan imgPlan = this.csvnImage.makeInstallPlan(csvnPackages)
            this.progressTracker = PackagesUpdateProgressTracker.makeNew(
                    this.imagePath, this.bayeuxPublisherClient, 
                    this.statusMessageChannel, this.percentageMessageChannel,
                    newPackages, locale)
            imgPlan.execute(this.progressTracker)

            this.areThereUpdates = false
            this.systemNeedsRestart = true
            this.reloadPackagesAndUpdates()
        }
    }

    /**
     * Installs all updates for the CSVN packages.
     * @param locale the request context locale (used for cometd messages)
     * @throws NoRouteToHostException if there's no connection to the image
     * repository.
     * @throws IOException if any IO problems occur while installing new 
     * packages. This can be related to file permissions, missing files, etc.
     */
    def installPackagesAddOns(Locale locale) throws NoRouteToHostException, IOException {
        Thread.startDaemon("Csvn Installer Daemon") {
            log.debug("Starting the installation process for addons...")

            def addOns = new HashSet()
            for(a in this.getNewPackagesInfo()) {
                addOns << a.getFmriState()
            }
            log.debug("Installing ${addOns.size()} updates...")
            this.installPackages(addOns, true, locale)
        }
    }

    /**
     * Installs all updates for the CSVN packages.
     * @param locale the request context locale (used for cometd messages)
     * @throws NoRouteToHostException if there's no connection to the image
     * repository.
     * @throws IOException if any IO problems occur while installing new 
     * packages. This can be related to file permissions, missing files, etc.
     */
    def installPackagesUpdates(Locale locale) throws NoRouteToHostException, IOException {
        Thread.startDaemon("Csvn Installer Daemon") {
            log.debug("Starting the update process...")

            def updates = new HashSet()
            for(a in this.getUpgradablePackagesInfo()) {
                updates << a.getFmriState()
            }
            log.debug("Installing ${updates.size()} updates...")
            this.installPackages(updates, false, locale)
        }
    }

    /**
     * @return if the system needs to be restarted.
     */
    def systemNeedsRestart() {
        return this.systemNeedsRestart
    }

    /**
     * @return true if the update did not complete and the updates directory
     * still exists
     */
    def isIncompleteWindowsUpdate() {
        File f = new File(ConfigUtil.appHome(), 'updates')
        return f.exists()
    }
    
    /**
     * Restart the web context by calling System.exit(5). The Wrapper code
     * will restart the server.
     */
    def restartServer() {
        if (this.progressTracker.hasFinished()) {

            //restart the csvn app server
            System.exit(5)

        }
    }
}
