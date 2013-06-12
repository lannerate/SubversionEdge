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

import com.collabnet.svnedge.AbstractSvnEdgeFunctionalTests;
import com.collabnet.svnedge.domain.User;
import com.collabnet.svnedge.domain.Server
import com.collabnet.svnedge.domain.ServerMode

import org.codehaus.groovy.grails.commons.ConfigurationHolder

/**
 * This test plan exercises the scenarios of a complete conversion process.
 * 
 * @author Marcello de Sales (mdesales@collab.net)
 *
 */
class FullConversionToTeamForgeFunctionalTests 
        extends AbstractConversionFunctionalTests {

    /**
     * The name of the project created during the complete conversion.
     */
    def String createdProjectName

    @Override
    protected void setUp() {
        super.setUp()

        // create a repository to make a complete conversion. Users are
        // created by default.
        this.createLocalRepository()
    }

    /**
     * Creates a local repository with a random name
     */
    private void createLocalRepository() {
        if (this.isItAFreshConversion()) {
            get('/repo/create')
            assertStatus 200

            def repoName = "csvnrepo" + new Random().nextInt(1000)
            def button = getMessage("repository.page.create.button.create")
            form {
                name = repoName
                click button
            }
            assertStatus 200
            assertContentDoesNotContain(getMessage("default.errors.summary"))
            def title = getMessage("repository.page.show.title", [repoName])
            assertContentContains(title)
        }
    }

    /**
      * <li>Test Case 1: Successful conversion to TeamForge Mode
      * <ul><li>SetUp
      * <ul><li>Login to SvnEdge
    <li>Revert to Standalone Mode in case on TeamForge Mode
    </ul>
    <li>Steps to reproduce
    <ul><li>Go to the Credentials Form
    <li>Enter correct credentials and existing CTF URL and try to convert;
    </ul>
    <li>Expected Results
    <ul><li>Successful conversion message is shown
    <li>Login -> Logout as admin
    <li>Verify that the server is on TeamForge mode;
    <li>Login to CTF server and verify that the system ID from the SvnEdge 
    server is listed on the list of integration servers
    </ul>
    <li>Tear Down
    <ul><li>Revert conversion if necessary
    <li>Logout from the SvnEdge server
    </ul></ul>
     */
    void testCase1_convertCompleteCSVN() {
        // Step 1: Verify the tabs and go to the credentials one.
        this.goToCredentialsTab()

        // Step 2: Verify the credentials with teamforge.
        def username = config.svnedge.ctfMaster.username
        def password = config.svnedge.ctfMaster.password
        def apiKey = config.svnedge.ctfMaster.apiKey
        def continueButton = 
            getMessage("setupTeamForge.page.ctfInfo.button.continue")
        form {
            ctfURL = this.getTestCtfUrl()
            ctfUsername = username
            ctfPassword = password
            serverKey = apiKey
            click continueButton
        }
        assertStatus 200

        def webDavUnreachable = 
            getMessage("ctfRemoteClientService.local.webdav.unreachable")
        def viewVCUnreachable = 
            getMessage("ctfRemoteClientService.local.viewvc.unreachable")
        //TODO: if the viewvc or svn path is not reachable, then re-try once.
        def responseString = this.response.contentAsString
        if (responseString.contains(webDavUnreachable) || 
            responseString.contains(viewVCUnreachable)) {
            form {
                ctfURL = this.getTestCtfUrl()
                ctfUsername = username
                ctfPassword = password
                serverKey = apiKey
                click continueButton
            }
            assertStatus 200
        }
        assertContentDoesNotContain(
            getMessage("setupTeamForge.action.ctfInfo.ctfConnection.error"))
        assertContentContains(
            getMessage("setupTeamForge.page.ctfProject.ctfProject.label"))
        assertContentContains(
            getMessage("setupTeamForge.page.ctfProject.ctfProject.label.tip"))

        this.createdProjectName = "csvnproj" + new Random().nextInt(10000)
        def projButton = 
            getMessage("setupTeamForge.page.ctfProject.button.continue")
        // Step 3: provide the project name.
        form {
            ctfProject = this.createdProjectName
            click projButton
        }
        assertStatus 200

        assertContentDoesNotContain("Error related to project name")
        assertContentContains(encodeAsHTML(getMessage(
            "setupTeamForge.action.updateProject.initialCreation", 
            [this.createdProjectName])))

        // Step 4: choose the users name information.
        def usersButton = 
            getMessage("setupTeamForge.page.ctfUsers.button.continue")
        form {
            click usersButton
        }
        assertStatus 200

        assertContentContains(
            getMessage("setupTeamForge.page.confirm.ready.tip"))
        assertContentContains(getMessage("setupTeamForge.page.confirm.server"))
        assertContentContains(this.getTestCtfUrl())
        assertContentContains(getMessage("setupTeamForge.page.confirm.project"))
        assertContentContains(this.createdProjectName)

        // Step 5: click on the convert button and verify the conversion.
        def convertButton = 
            getMessage("setupTeamForge.page.confirm.button.confirm")
        form {
            click convertButton
        }
        assertStatus 200

        assertContentDoesNotContain(
            getMessage("setupTeamForge.action.ctfInfo.ctfConnection.error"))
        if (this.response.contentAsString.contains("TeamForge server cannot " +
                "access the local ")) {
            fail("Conversion is failing because CTF cannot access the local ViewVC or SVN URLs.")
        }
        //new File("/tmp/full.html") << this.response.contentAsString
                
        assertContentContains(
            getMessage("setupTeamForge.action.convert.success"))
        assertContentContains(getMessage("setupTeamForge.page.convert.project"))
        assertContentContains(this.createdProjectName)
        assertContentContains(
            getMessage("setupTeamForge.page.convert.sourceCode"))
        def projectUrl = this.getTestCtfUrl() + "/sf/scm/do/listRepositories/" +
            "projects.${this.createdProjectName}/scm"
        assertContentContains(projectUrl)

        assertConversionSucceeded()

        // cleans up the projects list from the server, since there is
        // pagination.
        this.removeProjectFromCtfServer()
    }

    /**
     * 1. Verifies that the CTF server lists the current ctf;
     * 2. Verifies that the name of the project created during conversion is 
     * listed in the projects list;
     * 3. Verifies if the users were created on CTF server.
     * 4. Verifies that the repositories from the SvnEdge server are listed
     * in the project page.
     */
    protected void assertConversionSucceededOnCtfServer() {
        super.assertConversionSucceededOnCtfServer()
        this.assertProjectWasCreatedOnCtfServer(this.createdProjectName)
        this.assertUsersWereCreatedOnCtfServer()
        this.assertRepositoriesWereCreatedOnCtfServer(this.createdProjectName)
    }

    /**
    * <li>Test Case 2: Wrong CTF URL during the complete conversion
    * <ul><li>SetUp
    * <ul><li>Login to SvnEdge
        <li>Revert to Standalone Mode in case on TeamForge Mode
     </ul>
     <li>Steps to reproduce
       <ul><li>Go to the Credentials Form
        <li>Enter incorrect CTF URL, but credentials and try to convert;
     </ul>
     <li>Expected Results
        <ul><li>Error message is shown with the "unknown" CTF URL.
        <li>Login -> Logout -> Verify that the server is on Standalone mode;
    </ul>
    <li>Tear Down
        <ul><li>Revert conversion if necessary
        <li>Logout from the SvnEdge server
     */
    void testCase2_unknownTeamForgeHostName() {
        // Step 1: Verify the tabs and go to the credentials one.
        this.goToCredentialsTab()

        // Step 2: verify that incorrect URL does not convert.
        def username = config.svnedge.ctfMaster.username
        def password = config.svnedge.ctfMaster.password
        def apiKey = config.svnedge.ctfMaster.apiKey
        def ctfHost = "unknown.cloud.sp.collab.net"
        def ctfUrlServer = "http://${ctfHost}"
        def continueButton = 
            getMessage("setupTeamForge.page.ctfInfo.button.continue")
        form {
            ctfURL = ctfUrlServer
            ctfUsername = username
            ctfPassword = password
            serverKey = apiKey
            click continueButton
        }
        assertStatus 200
        assertContentContains(getMessage(
            "ctfRemoteClientService.host.unknown.error", [ctfHost]))

        // Step 4: Verify the attempt to convert did not succeed.
        assertConversionDidNotSucceeded()
    }

    /**
     <li>Test Case 3: Wrong credentials during the complete conversion
    * <ul><li>SetUp
    * <ul><li>Login to SvnEdge
        <li>Revert to Standalone Mode in case on TeamForge Mode
     </ul>
     <li>Steps to reproduce
       <ul><li>Go to the Credentials Form
        <li>Enter incorrect credentials, but correct CTF URL and try to convert;
     </ul>
     <li>Expected Results
        <ul><li>Error message is shown with the "incorrect credentials".
        <li>Login -> Logout -> Verify that the server is on Standalone mode;
    </ul>
    <li>Tear Down
        <ul><li>Revert conversion if necessary
        <li>Logout from the SvnEdge server
     */
    void testCase3_incorrectCredentialsToTeamForge() {
        // Step 1: Verify the tabs and go to the credentials one.
        this.goToCredentialsTab()

        // Step 2: verify that incorrect credentials do not convert.
        def continueButton =
            getMessage("setupTeamForge.page.ctfInfo.button.continue")
        def apiKey = config.svnedge.ctfMaster.apiKey
        form {
            ctfURL = this.getTestCtfUrl()
            ctfUsername = "xyc"
            ctfPassword "wrongpass"
            serverKey = apiKey
            click continueButton
        }
        assertStatus 200
        assertContentContains(getMessage("ctfRemoteClientService.auth.error",
            [this.getTestCtfUrl()]))

        // Step 3: Verify the attempt to convert did not succeed.
        assertConversionDidNotSucceeded()
    }

    /**
     <li>Test Case 4: form fields are missing during the complete conversion
    * <ul><li>SetUp
    * <ul><li>Login to SvnEdge
        <li>Revert to Standalone Mode in case on TeamForge Mode
     </ul>
     <li>Steps to reproduce
       <ul><li>Go to the Credentials Form
        <li>DO NOT enter form values (empty) and try to convert;
     </ul>
     <li>Expected Results
        <ul><li>Error message is shown with "An error occurred"
            <li>All the error messages for the form fields are the same as 
            the messages.properties by the keys.
        <li>Login -> Logout -> Verify that the server is on Standalone mode;
    </ul>
    <li>Tear Down
        <ul><li>Revert conversion if necessary
        <li>Logout from the SvnEdge server
     */
    void testCase4_missingParametersToForm() {
        // Step 1: Verify the tabs and go to the credentials one.
        this.goToCredentialsTab()

        // Step 2: verify that incorrect credentials do not convert.
        def continueButton = 
            getMessage("setupTeamForge.page.ctfInfo.button.continue")
        form {
            ctfURL = ""
            ctfUsername = ""
            ctfPassword ""
            click continueButton
        }
        assertStatus 200
        assertContentContains(getMessage("ctfConversionBean.ctfURL.blank"))
        assertContentContains(getMessage("ctfConversionBean.ctfUsername.blank"))
        assertContentContains(getMessage("ctfConversionBean.ctfPassword.blank"))

        // Step 3: Verify the attempt to convert did not succeed.
        assertConversionDidNotSucceeded()
    }

    /**
     <li>Test Case 5: Malformed TeamForge URL during the complete conversion
    * <ul><li>SetUp
    * <ul><li>Login to SvnEdge
        <li>Revert to Standalone Mode in case on TeamForge Mode
     </ul>
     <li>Steps to reproduce
       <ul><li>Go to the Credentials Form
        <li>Enter a malformed URL without protocol nor port number;
     </ul>
     <li>Expected Results
        <ul><li>Error message is shown with "Malformed URL"
            <li>All the error messages for the form fields are the same as 
            the messages.properties by the keys.
        <li>Login -> Logout -> Verify that the server is on Standalone mode;
    </ul>
    <li>Tear Down
        <ul><li>Revert conversion if necessary
        <li>Logout from the SvnEdge server
     */
    void testCase5_providingMalformedTeamForgeURL() {
        // Step 1: Verify the tabs and go to the credentials one.
        this.goToCredentialsTab()

        // Step 2: verify that incorrect credentials do not convert.
        def continueButton = 
            getMessage("setupTeamForge.page.ctfInfo.button.continue")
        def badUrl = "malformed_Url_Without_Protocol_Port"
        form {
            ctfURL = badUrl
            ctfUsername = "usr"
            ctfPassword "pwd"
            click continueButton
        }
        assertStatus 200
        assertContentContains(
            getMessage("ctfRemoteClientService.host.malformedUrl", [badUrl]))

        // Step 3: Verify the attempt to convert did not succeed.
        assertConversionDidNotSucceeded()
    }
}
