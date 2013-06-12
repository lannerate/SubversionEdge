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


import com.collabnet.svnedge.domain.Server 
import com.collabnet.svnedge.domain.integration.CtfServer 
import com.collabnet.svnedge.util.FileDownloaderCategory 
import com.collabnet.svnedge.util.UntarCategory 
import functionaltestplugin.FunctionalTestCase;
import org.codehaus.groovy.grails.commons.ApplicationHolder 
import org.codehaus.groovy.grails.commons.ConfigurationHolder 
import org.codehaus.groovy.grails.plugins.codecs.HTMLCodec
import javax.net.ssl.HttpsURLConnection
import com.collabnet.svnedge.util.SSLUtil

/**
 * This is the basic implementation of functional tests for the SvnEdge.
 * It contains supporting methods for the execution of common functional tests, 
 * exposing the references to internal objects and services.
 * 
 * References about the instance the FunctionaltestCase, see 
 * http://plugins.grails.org/grails-functional-test/tags/RELEASE_1_2_4/src/groovy/functionaltestplugin/FunctionalTestCase.groovy
 * 
 * @author Marcello de Sales (mdesales@collab.net)
 *
 */
public abstract class AbstractSvnEdgeFunctionalTests extends FunctionalTestCase {

    /**
     * The default configuration holder.
     */
    private config = ConfigurationHolder.config
    /**
     * The default grails application instance.
     * The same as adding the autowire of the grails application bean.
     * def grailsApplication
     */
    private app = ApplicationHolder.application

    private boolean javaScriptPriorState = javaScriptEnabled
    
    def lifecycleService

    @Override
    protected void setUp() {
        //The web framework must be initialized.
        super.setUp()
        javaScriptPriorState = javaScriptEnabled
        javaScriptEnabled = false
    }

    @Override
    protected void tearDown() {
        //The tear down method terminates all the web-related objects, and
        //therefore, must be performed in the end of the operation.
        super.tearDown()
        javaScriptEnabled = javaScriptPriorState
    }

    protected void login(username, password) {
        get('/login/auth')
        assertStatus(200)

        if (this.response.contentAsString.contains(
                getMessage("layout.page.login"))) {
            this.logout()
        }
        def login = getMessage("layout.page.login")
        form('loginForm') {
            j_username = username
            j_password = password
            click "Sign In"
        }
        assertStatus(200)
    }

    /**
     * Performs the logout by clicking on the link.
     */
    protected void logout() {
        def logout = getMessage("layout.page.logout")
        if (this.response.contentAsString.contains(logout)) {
            click logout
        }
        assertStatus(200)
        assertContentContains(getMessage("login.page.auth.header"))
    }

    protected void loginAdmin() {
        this.login ("admin", "admin")
        assertContentContains("loggedInUser")
    }

    protected void loginUser() {
        this.login ("user", "admin")
        assertContentContains("loggedInUser")
    }

    protected void loginUserDot() {
        this.login ("user.new", "admin")
        assertContentContains("loggedInUser")
    }

    /**
     * Gets an i18n message from the messages.properties file without providing
     * parameters using the default locale.
     * @param key is the key in the messages.properties file.
     * @return the message related to the key in the messages.properties file
     * using the default locale.
     */
    protected def getMessage(String key) {
        return this.getMessage(key, null)
    }

    /**
     * Gets an i18n message from the messages.properties file without providing
     * parameters using the default locale.
     * @param key is the key in the messages.properties file.
     * @param params is the list of parameters to provide the i18n.
     * @return the message related to the key in the messages.properties file
     * using the default locale.
     */
    protected def getMessage(String key, params) {
        def appCtx = app.getMainContext()
        return appCtx.getMessage(key, params as String[], Locale.getDefault())
    }

    /**
     * Gets the configuration holder.
     */
    protected def getConfig() {
        assertNotNull("The configuration must never be null", this.config)
        return this.config
    }

    /**
     * @return the server instance.
     */
    protected def getServer() {
        return Server.getServer()
    }

    /**
    * @return the CTF server instance.
    */
    protected def getCtfServer() {
        return CtfServer.getServer()
    }

    /**
    * @return the lifecycle service.
    */
    protected def getLifecycleService() {
        return this.lifecycleService
    }

    /**
     * Stops the SVN server running on the machine.
     */
    protected void stopSvnServer() {
        if (this.lifecycleService.isStarted()) {
            this.lifecycleService.stopServer()
        }
    }

    /**
     * Sets up the image directory: Verifies if the directory exists.
     * If not, create one and download the image artifact.
     */
    protected void setupTestCsvnImage() {
        def imageDir = config.svnedge.softwareupdates.imagepath
        def currentVersion = config.svnedge.softwareupdates.currentVersion
        def validImageFileDir = new File(imageDir)
        def pkgInternalDirectory = new File(validImageFileDir,
            ".org.opensolaris,pkg")

        if (!pkgInternalDirectory.exists()) {
            validImageFileDir.mkdirs()
            def csvnBinUrl = "https://mgr.cloud.sp.collab.net/pbl/svnedge/" +
                   "pub/Installers/linux/CollabNetSubversionEdge-" +
                   "${currentVersion}-dev_linux-x86.tar.gz"
            csvnBinUrl = csvnBinUrl.toURL()

            def fileName = FileDownloaderCategory.extractFileName(csvnBinUrl)
            def imageFile = new File(validImageFileDir, fileName)

            if (!imageFile.exists()) {
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
                    validImageFileDir << imageFile
                }
                assertTrue("The internal pkg image directory should exist.",
                    pkgInternalDirectory.exists())

            } catch (FileNotFoundException tarFileDoesNotExist) {
                // the download might have failed because the file is not 
                //reachable
               fail(tarFileDoesNotExist.getMessage())
            }
        }
    }
    
    protected String encodeAsHTML(String s) {
        return HTMLCodec.encode(s)
    }
}
