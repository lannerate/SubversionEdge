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
package com.collabnet.svnedge.console.pkgsupdate;

import java.io.FileNotFoundException;
import java.net.NoRouteToHostException;

import com.collabnet.svnedge.admin.PackagesUpdateService 
import com.collabnet.svnedge.admin.pkgsupdate.PackageInfo 
import com.collabnet.svnedge.util.FileDownloaderCategory 
import com.collabnet.svnedge.util.UntarCategory 
import com.sun.pkg.client.Image;

import grails.test.*
import javax.net.ssl.HttpsURLConnection
import com.collabnet.svnedge.util.SSLUtil

class PackagesUpdateServiceIntegrationTests extends GrailsUnitTestCase {

    def grailsApplication
    def config
    def packagesUpdateService
    def jobsAdminService
    def validImageFileDir
    def invalidImagePath
    def pkgInternalDirectory
    def currentVersion

    protected void setUp() {
        super.setUp()
        this.config = grailsApplication.config
        String imageDir = config.svnedge.softwareupdates.imagepath
        this.currentVersion = config.svnedge.softwareupdates.currentVersion
        this.validImageFileDir = new File(imageDir)
        this.pkgInternalDirectory = new File(
            this.validImageFileDir,".org.opensolaris,pkg")
        this.setupTestCsvnImage()

        invalidImagePath = new File(this.validImageFileDir, 
            "/non-existing").absolutePath
        this.packagesUpdateService = new PackagesUpdateService()
        this.packagesUpdateService.jobsAdminService = jobsAdminService

        try {
            this.packagesUpdateService.bootstrap(this.config)

        } catch (Exception otherError) {
            otherError.printStackTrace()
            fail(otherError.getMessage())
        }
    }

    /**
     * Sets up the image directory: Verifies if the directory exists. 
     * If not, create one and download the image artifact.
     */
    private void setupTestCsvnImage() {
        if (!this.pkgInternalDirectory.exists()) {
            this.validImageFileDir.mkdirs()
            def csvnBinUrl = "https://mgr.cloud.sp.collab.net/pbl/svnedge/" +
                "pub/Installers/linux/CollabNetSubversionEdge-" +
                "${currentVersion}-dev_linux-x86.tar.gz"
            csvnBinUrl = csvnBinUrl.toURL()

            def fileName = FileDownloaderCategory.extractFileName(csvnBinUrl)
            def imageFile = new File(this.validImageFileDir, fileName)

            if (!imageFile.exists()) {
//                def svnEdgeDir = config.svnedge.appHome
//                svnEdgeDir = new File(svnEdgeDir).getParentFile().canonicalPath
//                FileDownloaderCategory.setTruststore(svnEdgeDir +
//                    "/scripts/cubit.keystore", "together")
                HttpsURLConnection.setDefaultSSLSocketFactory(SSLUtil.createTrustingSocketFactory());
                FileDownloaderCategory.progressPrintStream = System.out
                try {
                    use(FileDownloaderCategory) {
                        imageFile << csvnBinUrl
                    }
                } catch (IllegalStateException fileAlreadyExists) {
                    // no need to download it then...
                    println(fileAlreadyExists.getMessage())
                }
            }

            try {
                UntarCategory.progressPrintStream = System.out
                UntarCategory.removeRootDir = true
                use(UntarCategory) {
                    this.validImageFileDir << imageFile
                }
                assertTrue("The internal pkg image directory should exist.",
                    this.pkgInternalDirectory.exists())

            } catch (FileNotFoundException tarFileDoesNotExist) {
                // the download might have failed because the file is not reachable
                fail(tarFileDoesNotExist.getMessage())
            }
        }
    }

    void testValidImagePathBootstrap() {
        println("The software version number: " + 
            this.packagesUpdateService.getInstalledVersionNumber())
        println("The subversion version: " + 
            this.packagesUpdateService.getInstalledSvnVersionNumber())
        println("The Release number: " + 
            this.packagesUpdateService.getInstalledReleaseNumber())

        assertNotNull "The version of the software must not be null",
                this.packagesUpdateService.getInstalledVersionNumber()
        assertNotNull "The svn version must not be null",
            this.packagesUpdateService.getInstalledSvnVersionNumber()
        assertNotNull "The installed package release # must not be null",
                this.packagesUpdateService.getInstalledReleaseNumber()
        assertNotNull "The installed package branch # must not be null",
                this.packagesUpdateService.getInstalledBranchNumber()
        assertNotNull "The image origin URL must not be null",
                this.packagesUpdateService.getImageOriginUrl()
    }

    void testGetRootImageDirectoryFile() {
        def rootF = this.packagesUpdateService.getImageRootDirectoryFile()
        assertEquals "The root path for the image is incorrect" ,
                this.validImageFileDir.canonicalPath, rootF.canonicalPath
    }

    void testGetCsvnImageReference() {
        def csvnImage = this.packagesUpdateService.getCSvnImage()
        assertNotNull "The list of updates must not be null", csvnImage
    }

    private void printPackagesInfo(Collection<Image.FmriState> packageStates) {
        for (packageState in packageStates) {
            System.out.println("--------------------------------------------")
            System.out.println("FS Name: ${packageState.fmri.getName()}")
            System.out.println("FS URL: ${packageState.fmri.getURLPath()}")
            System.out.println("FS Version: ${packageState.fmri.getVersion()}")
            System.out.println("FS IS upgradable: ${packageState.upgradable}")
            System.out.println("FS IS installed: ${packageState.installed}")
        }
    }

    void testGetAllPackages() {
        try {
            def allPackages = this.packagesUpdateService.getAllPackages()
            assertNotNull "The list of packages must not be null", allPackages
            assertTrue "The size of the list of packages must include packages",
                    allPackages.size() > 0

        } catch (NoRouteToHostException nrthe) {
            fail(nrthe.getMessage())
        } catch (Exception e) {
            fail(e.getMessage())
        }
    }

    void testGetInstalledPackages() {
        try {
            def installedPkgs = this.packagesUpdateService.
                    getInstalledPackages()
            assertNotNull "The list of installed packages must not be null", 
                    installedPkgs
            assertTrue "The size of the list of installed packages must " +
                    "include packages", installedPkgs.size() > 0
            for(pkgState in installedPkgs) {
                assertTrue "The package ${pkgState} must be on the state " +
                    "'installed'", pkgState.installed
            }
            System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% Installed")
            this.printPackagesInfo(installedPkgs)

        } catch (NoRouteToHostException nrthe) {
            fail(nrthe.getMessage())
        } catch (Exception e) {
            fail(e.getMessage())
        }
    }

    void testGetNonInstalledPackages() {
        try {
            def nonInstPkgs = this.packagesUpdateService.getNonInstalledPackages()
            assertNotNull "The list of non-installed packages must not be null", 
                    nonInstPkgs
            for(pkgState in nonInstPkgs) {
                assertTrue "The package ${pkgState} must be on the state " +
                        "'installed'", !pkgState.installed
            }
            System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% NOT Installed")
            this.printPackagesInfo(nonInstPkgs)

        } catch (NoRouteToHostException nrthe) {
            fail(nrthe.getMessage())
        } catch (Exception e) {
            fail(e.getMessage())
        }
    }

    void testAreThereUpdatesAvailable() {
        try {
            def updts = this.packagesUpdateService.areThereUpdatesAvailable()
            assertNotNull "The available updates return must be not null", updts
            assertTrue "The available updates call must be a boolean result", 
                    updts instanceof Boolean

        } catch (NoRouteToHostException nrthe) {
            fail(nrthe.getMessage())
        } catch (Exception e) {
            fail(e.getMessage())
        }
    }

    void testGetUpgradablePackages() {
        try {
            def upgradPkgs = this.packagesUpdateService.getUpgradablePackages()
            if (this.packagesUpdateService.areThereUpdatesAvailable()) {
                assertNotNull "The list of updates must not be null", upgradPkgs
                for(pkgState in upgradPkgs) {
                    assertTrue "The package must not be upgradable since it is " +
                        "already the newest one", !pkgState.upgradable
                }
            }
            System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% upgradable")
            this.printPackagesInfo(upgradPkgs)


        } catch (NoRouteToHostException nrthe) {
            fail(nrthe.getMessage())
        } catch (Exception e) {
            fail(e.getMessage())
        }
    }

    void testGetAddOnsPackages() {
        try {
            def upgradPkgs = this.packagesUpdateService.getUpgradablePackages()
            assertNotNull "The list of updates must not be null", upgradPkgs
            for(pkgState in upgradPkgs) {
                assertTrue "The package must not be installed", 
                        !pkgState.installed
                assertTrue "The package must not be upgradable since it is " +
                        "already the newest one", !pkgState.upgradable
            }
            System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% addons")
            this.printPackagesInfo(upgradPkgs)

        } catch (NoRouteToHostException nrthe) {
            fail(nrthe.getMessage())
        } catch (Exception e) {
            fail(e.getMessage())
        }
    }

    private void printPackagesInfoPropeties(PackageInfo[] packagesInfo) {
        for(packageInfo in packagesInfo) {
            assertNotNull "The package release must be the release version", 
                    packageInfo.getName()
            assertNotNull "The package release must be the release version", 
                    packageInfo.getSummary()
            assertNotNull "The package release must be the release version", 
                    packageInfo.getDescription()
            assertNotNull "The package release must be the release version", 
                    packageInfo.getRelease()
            assertNotNull "The package release must be the release version", 
                    packageInfo.getBranch()
            assertNotNull "The package release must be the release version", 
                    packageInfo.getPublishedDate()
            assertNotNull "The package release must be the release version", 
                    packageInfo.getVersion()
            assertNotNull "The package release must be the release version", 
                    packageInfo.getSize()
            assertNotNull "The package release must be the release version", 
                    packageInfo.getSizeInMB()
            System.out.println("---------------------------------------")
            System.out.println("Name: ${packageInfo.name}")
            System.out.println("summary: ${packageInfo.summary}")
            System.out.println("Description: ${packageInfo.description}")
            System.out.println("release: ${packageInfo.release}")
            System.out.println("branch: ${packageInfo.branch}")
            System.out.println("version: ${packageInfo.version}")
            System.out.println("pdate: ${packageInfo.publishedDate}")
            System.out.println("size: ${packageInfo.sizeInMB} MB")
        }
    }

    void testGetInstalledPackagesInfo() {
        try {
            def pkgsInf = this.packagesUpdateService.getInstalledPackagesInfo()
            System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%")
            System.out.println("Installed")
            this.printPackagesInfoPropeties(pkgsInf)

        } catch (NoRouteToHostException nrthe) {
            fail(nrthe.getMessage())
        } catch (Exception e) {
            fail(e.getMessage())
        }
    }

    void testGetUpgradablePackagesInfo() {
        try {
            def pkgsInf = this.packagesUpdateService.getUpgradablePackagesInfo()
            System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%")
            System.out.println("Upgradable")
            this.printPackagesInfoPropeties(pkgsInf)

        } catch (NoRouteToHostException nrthe) {
            fail(nrthe.getMessage())
        } catch (Exception e) {
            fail(e.getMessage())
        }
    }

    void testGetNewAddOnPackagesInfo() {
        try {
            def pkgssInfo = this.packagesUpdateService.getNewPackagesInfo()
            System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%")
            System.out.println("New Add-ons")
            this.printPackagesInfoPropeties(pkgssInfo)

        } catch (NoRouteToHostException nrthe) {
            fail(nrthe.getMessage())
        } catch (Exception e) {
            fail(e.getMessage())
        }
    }

//    void testInstallPackagesUpdates() {
//        try {
//            this.packagesUpdateService.setProgressTracker(
//                    new PackagesUpdateProgressTracker(
//                            new PrintWriter(System.out)))
//            this.packagesUpdateService.installPackagesUpdates()
//            if (this.packagesUpdateService.systemNeedsRestart()) {
//                def msg =  this.packagesUpdateService.
//                        getSystemNeedsRestartMessage()
//                assertNotNull "The system must need restart message can't be " +
//                        "null' if there were packages installed", msg
//            }
//
//        } catch (NoRouteToHostException nrthe) {
//            fail(nrthe.getMessage())
//        } catch (Exception e) {
//            fail(e.getMessage())
//        }
//    }

//    void testInstallPackagesAddOns() {
//        try {
//            this.packagesUpdateService.setProgressTracker(
//                    new PackagesUpdateProgressTracker(
//                    new PrintWriter(System.out)))
//            this.packagesUpdateService.installPackagesAddOns()
//            if (this.packagesUpdateService.systemNeedsRestart()) {
//                def msg =  this.packagesUpdateService.
//                        getSystemNeedsRestartMessage()
//                assertNotNull "The system must need restart message can't be " +
//                    "null' if there were packages installed", msg
//            }
//
//        } catch (NoRouteToHostException nrthe) {
//            fail(nrthe.getMessage())
//        } catch (Exception e) {
//            fail(e.getMessage())
//        }
//    }
}
