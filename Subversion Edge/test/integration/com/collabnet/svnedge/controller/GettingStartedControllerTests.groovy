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
package com.collabnet.svnedge.controller

import com.collabnet.svnedge.domain.Wizard;
import com.collabnet.svnedge.domain.WizardStep;

import grails.converters.JSON;
import grails.test.*

class GettingStartedControllerTests extends AbstractSvnEdgeControllerTests {

    def lifecycleService
    def networkingService
    def authenticateService
    def operatingSystemService
    
    protected void setUp() {
        super.setUp()
        controller.authenticateService = authenticateService
        controller.lifecycleService = lifecycleService 
        controller.networkingService = networkingService
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testIndex() {
        controller.index()
    }

    void testSuspendAndStartWizard() {
        controller.suspendWizard()
        assertNull "Only existing wizard should be inactive", Wizard.lastActiveWizard
        controller.startWizard()
        assertNotNull "Wizard should be active", Wizard.lastActiveWizard
    }
    
    void testGotoStep() {
        Wizard w = Wizard.lastActiveWizard
        w.increment()
        assertEquals "Expected step 1", 'ChangePassword', w.currentStep.label
        def newStep = 'CloudBackup'
        controller.params['label'] = newStep
        controller.gotoStep()
        w = Wizard.lastActiveWizard
        assertEquals "Should be on new step ", newStep, w.currentStep.label
    }

    void testIsDefaultPortAllowed() {
        controller.isDefaultPortAllowed()
        def b = JSON.parse(mockResponse.contentAsString)['isDefaultPortAllowed']
        if (operatingSystemService.isSolaris() || operatingSystemService.isWindows()) {
            assertTrue "Solaris and Windows test servers should support default port", b 
        } else {
            assertFalse "Linux test servers do not support default port", b 
        }
    }
    
    void testAvailableHostnames() {
        controller.availableHostnames()
        def hostnames = JSON.parse(mockResponse.contentAsString)
        assertNotNull "Available hostnames should not be null", hostnames
        assertTrue "Should be at least one hostname", hostnames.size() > 0
    }
    
    void testCompleteCurrentStep() {
        Wizard w = Wizard.lastActiveWizard
        w.increment()
        WizardStep step = w.currentStep
        assertEquals "Expected step 1", 'ChangePassword', step.label
        def nextStep = 'ServerSettings'
        controller.completeCurrentStep()
        assertTrue "foobar", step.done
        w = Wizard.lastActiveWizard
        assertEquals "Should be on next step ", nextStep, w.currentStep.label
        assertTrue "Expected redirect to Server Settings",
                controller.redirectArgs['controller'] == 'server' &&
                controller.redirectArgs['action'] == 'edit'
    }
}
