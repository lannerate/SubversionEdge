/*
 * CollabNet Subversion Edge
 * Copyright (C) 2012, CollabNet Inc. All rights reserved.
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
import grails.converters.JSON

import com.collabnet.svnedge.domain.Server
import com.collabnet.svnedge.domain.User
import com.collabnet.svnedge.domain.Wizard
import com.collabnet.svnedge.domain.WizardStep
import com.collabnet.svnedge.util.ControllerUtil;
import com.collabnet.svnedge.wizard.gettingstarted.GettingStartedWizard

/**
 * Getting Started wizard actions.
 */
@Secured(['ROLE_ADMIN', 'ROLE_ADMIN_SYSTEM'])
class GettingStartedController {

    def authenticateService
    def lifecycleService
    def networkingService
    
    def index = {
    }

    def startWizard = {
        def ww = getDomain()
        ww.active = true
        //ww.increment()
        ww.save(flush: true)
        redirectWizard(ww.currentStep)
    }

    private def redirectWizard(WizardStep currentStep) {
        def wizard = getDomain()
        if (wizard.done) {
            flash.wizard_message = message(code: 'wizard.' +
                    wizard.label + '.done')
            redirect(controller: 'status', action: 'index')
        } else {
            def helper = currentStep?.helper()        
            if (helper?.forceTarget) {
                redirect(controller: helper.targetController,
                        action: helper.targetAction)
            } else {
                redirect controller: 'status', action: 'index', params: params
            }
        }
    }

    def suspendWizard = {
        def ww = getDomain()
        ww.active = false
        ww.save(flush: true)
        redirect controller: 'status', action: 'index', params: params
    }
    
    def abortWizard = {
        def ww = getDomain()
        ww.active = false
        ww.done = true
        ww.save(flush: true)
        redirect controller: 'status', action: 'index', params: params
    }
    
    def gotoStep = {
        def stepLabel = params['label']
        def w = getDomain()
        w.steps.each { 
            if (stepLabel == it.label) {
                w.currentStep = it
                w.save()
            }
        }
        log.debug "Current wizard step: " + w.currentStep.dump()
        def helper = w.currentStep.helper()
        if (helper.forceTarget) {
            redirect(controller: helper.targetController, 
                     action: helper.targetAction)
        } else {
            redirect(controller: 'status', action: 'index')
        }
    }
    
    def completeCurrentStep = {
        def w = getDomain()
        def step = w.completeCurrentStep()
        redirectWizard(step)
    }
    
    def skipStep = {
        def wizard = getDomain()
        wizard.increment()
        redirectWizard(wizard.currentStep)
    }

    def reset = {
        def ww = getDomain()
        ww.initialized = false
        ww.active = true
        ww.done = false
        ww.currentStep = null
        ww.save()
        ww.steps.each {
            it.done = false
            it.save()
        }
        Server server = Server.getServer()
        server.adminEmail = "devnull@collab.net"
        server.adminName = "Nobody"
        server.save()
        
        User admin = User.findByUsername('admin')
        admin.passwd = authenticateService
            .encodePassword('admin')
        admin.save()
        
        redirect controller: 'logout', action: 'index'
    }
    
    def availableHostnames = {
        def names = networkingService.availableHostnames();
        /*
        if (names) {
            if (names.size() == 1 && names[0] == networkingService.hostname) {
                names = []
            }
        }
        */
        render names as JSON
    }
    
    def isDefaultPortAllowed = {
        lifecycleService.clearCachedResults()
        render([isDefaultPortAllowed: lifecycleService.isDefaultPortAllowed()] as JSON)
    }

    private def getDomain() {
        return Wizard.findByHelperClassName(GettingStartedWizard.class.name)
    }
}
