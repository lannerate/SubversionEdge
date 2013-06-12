/*
 * CollabNet Subversion Edge
 * Copyright 2012, CollabNet Inc. All rights reserved.
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

import org.springframework.security.context.SecurityContextHolder as SCH
import org.springframework.security.providers.UsernamePasswordAuthenticationToken

import com.collabnet.svnedge.domain.Wizard;

import grails.test.GrailsUnitTestCase
import grails.util.GrailsWebUtil

class ApplicationFiltersIntegrationTests extends GrailsUnitTestCase {
    
    def filterInterceptor
    def grailsApplication
    def grailsWebRequest
    def authenticationManager

    private def request(Map params = null, controllerName, actionName) {
        grailsWebRequest = GrailsWebUtil
                .bindMockWebRequest(grailsApplication.mainContext)
        if (params) {
            grailsWebRequest.params.putAll(params)
        }
        grailsWebRequest.controllerName = controllerName
        grailsWebRequest.actionName = actionName
        return filterInterceptor
                .preHandle(grailsWebRequest.request, grailsWebRequest.response, null)
    }

    private def getResponse() {
        grailsWebRequest.currentResponse
    }

    def testGettingStartedWizardRedirects() {
        def username = "admin"
        def password = "admin"
        def authToken = new UsernamePasswordAuthenticationToken(username, password)
        SCH.context.authentication = authenticationManager.authenticate(authToken)
        def wizard = Wizard.lastActiveWizard

        def result = request("repo", "list")
        assertEquals "Should be on step 1", 1, wizard.currentStep.rank
        assertTrue "Don't expect a redirect, processing should continue", result
        assertNull "Don't expect a redirect", response.redirectedUrl

        wizard.increment()
        assertEquals "Should be on step 2", 2, wizard.currentStep.rank
        result = request("repo", "list")
        assertFalse "On step 2, request processing should be cancelled " + wizard.currentStep, result
        assertTrue "Expected redirect to Server Settings, not " + response.redirectedUrl,
                response.redirectedUrl.endsWith('/server/edit')

        // confirm that wizard will not redirect, if it is inactive
        wizard.active = false
        wizard.save()
        result = request("repo", "list")
        assertEquals "Should be on step 2", 2, wizard.currentStep.rank
        assertTrue "Don't expect a redirect, processing should continue", result
        assertNull "Don't expect a redirect", response.redirectedUrl
        // restore
        wizard.active = true
        wizard.save()

        wizard.increment()
        result = request("repo", "list")
        assertFalse "On step 3, request processing should be cancelled", result
        assertTrue "Expected redirect to CloudForge signup, not " + response.redirectedUrl,
                response.redirectedUrl.endsWith('/setupCloudServices/index')
    }
    
    def testGettingStartedWizardWithRegularUser() {
        def username = "user"
        def password = "admin"
        def authToken = new UsernamePasswordAuthenticationToken(username, password)
        SCH.context.authentication = authenticationManager.authenticate(authToken)
        def wizard = Wizard.lastActiveWizard

        wizard.increment()
        def result = request("repo", "list")
        assertEquals "Should be on step 1", 1, wizard.currentStep.rank
        assertTrue "Don't expect a redirect, processing should continue", result
        assertNull "Don't expect a redirect", response.redirectedUrl

        wizard.increment()
        assertEquals "Should be on step 2", 2, wizard.currentStep.rank
        result = request("repo", "list")
        assertTrue "On step 2, request processing should not be cancelled for normal user", result
        assertNull "Expected no redirect", response.redirectedUrl
    }
}
