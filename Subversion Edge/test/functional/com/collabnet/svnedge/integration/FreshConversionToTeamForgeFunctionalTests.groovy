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
 * This test plan exercises the scenarios of the fresh conversion process.
 * 
 * @author Marcello de Sales (mdesales@collab.net)
 *
 */
class FreshConversionToTeamForgeFunctionalTests 
    extends AbstractConversionFunctionalTests {

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
    void testCase1_convertFreshCSVN() {
        // Step 1: Verify the tabs and go to the credentials one.
        this.goToCredentialsTab()

        // Step 2: Convert to teamforge mode and verify it worked.
        def username = config.svnedge.ctfMaster.username
        def password = config.svnedge.ctfMaster.password
        def apiKey = config.svnedge.ctfMaster.apiKey
        def button = getMessage("setupTeamForge.page.ctfInfo.button.convert")
        form {
            ctfURL = this.getTestCtfUrl()
            ctfUsername = username
            ctfPassword = password
            serverKey = apiKey
            click button
        }
        assertStatus 200

        //TODO: if the viewvc or svn path is not reachable, then re-try once.
        if (this.response.contentAsString.contains("TeamForge server cannot " +
            "access the local ")) {
            form {
                ctfURL = this.getTestCtfUrl()
                ctfUsername = username
                ctfPassword = password
                serverKey = apiKey
                click button
            }
            assertStatus 200
        }
        if (this.response.contentAsString.contains("TeamForge server cannot " +
                "access the local ")) {
            fail("Conversion is failing because CTF cannot access the local ViewVC or SVN URLs.")
        }
        assertContentDoesNotContain(getMessage(
            "setupTeamForge.action.ctfInfo.ctfConnection.error"))
        //new File("/tmp/fresh.html") << this.response.contentAsString
        assertContentContains(getMessage(
            "setupTeamForge.action.convert.success.managed"))

        // Step 3: Verify the conversion is persistent.
        assertConversionSucceeded()
    }

    /**
    * <li>Test Case 2: Wrong CTF URL during the fresh conversion
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
        def button = getMessage("setupTeamForge.page.ctfInfo.button.convert")
        def ctfHost = "unknown.cloud.sp.collab.net"
        def ctfUrlServer = "http://${ctfHost}"
        form {
            ctfURL = ctfUrlServer
            ctfUsername = username
            ctfPassword = password
            click button
        }
        assertStatus 200
        assertContentContains(encodeAsHTML(
            getMessage("ctfRemoteClientService.host.unknown.error", [ctfHost])))

        // Step 4: Verify the attempt to convert did not succeed.
        assertConversionDidNotSucceeded()
    }

    /**
     <li>Test Case 3: Wrong credentials during the fresh conversion
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
        def button = getMessage("setupTeamForge.page.ctfInfo.button.convert")
        form {
            ctfURL = this.getTestCtfUrl()
            ctfUsername = "xyc"
            ctfPassword "wrongpass"
            click button
        }
        assertStatus 200
        assertContentContains(encodeAsHTML(getMessage("ctfRemoteClientService.auth.error",
            [this.getTestCtfUrl()])))

        // Step 3: Verify the attempt to convert did not succeed.
        assertConversionDidNotSucceeded()
    }

    /**
     <li>Test Case 4: form fields are missing during the fresh conversion
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
        def button = getMessage("setupTeamForge.page.ctfInfo.button.convert")
        form {
            ctfURL = ""
            ctfUsername = ""
            ctfPassword ""
            click button
        }
        assertStatus 200
        assertContentContains(getMessage("setupTeamForge.action.general.error"))
        assertContentContains(getMessage("ctfConversionBean.ctfURL.blank"))
        assertContentContains(getMessage("ctfConversionBean.ctfUsername.blank"))
        assertContentContains(getMessage("ctfConversionBean.ctfPassword.blank"))

        // Step 3: Verify the attempt to convert did not succeed.
        assertConversionDidNotSucceeded()
    }

    /**
     <li>Test Case 5: Malformed TeamForge URL during the fresh conversion
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
            getMessage("setupTeamForge.page.ctfInfo.button.convert")
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
