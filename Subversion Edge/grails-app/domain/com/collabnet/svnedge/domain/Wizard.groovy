/*
 * CollabNet Subversion Edge
 * Copyright (C) 2011, CollabNet Inc. All rights reserved.
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
package com.collabnet.svnedge.domain

import java.util.List;

import com.collabnet.svnedge.wizard.WizardHelper


class Wizard {
    //static hasMany = [steps: WizardStep]
    static transients = ['helper', 'controller', 'label', 'roles', 'rolesAsString']
    
    WizardStep currentStep
    boolean done
    boolean active
    boolean upgrade
    boolean initialized
    boolean ordered
    String helperClassName
    
    private WizardHelper helper
    
    private WizardHelper helper() {
        if (!helper) {
            helper = Class.forName(helperClassName, true, this.class.classLoader).newInstance()
        }
        return helper
    }
    
    List<WizardStep> getSteps() {
        def rows = WizardStep.findAllByWizardId(this.id)
        List<WizardStep> lws = []
        lws.addAll(rows)
        lws.sort { s1, s2 -> (s1.rank < s2.rank) ? -1 : 
                ((s1.rank > s2.rank) ? 1 : 0) }
    }

    String getLabel() {
        return helper().label
    }
    
    String getController() {
        return helper().controller
    }
    
    List<String> getRoles() {
        return helper().roles
    }
        
    String getRolesAsString() {
        return getRoles().join(',')
    }
    
    int index() {
        int n = 0
        if (currentStep) {
            n = currentStep.rank
        }
        return n
    }
    
    int maxIndex() {
        return steps.size()
    }
    
    WizardStep completeCurrentStep() {
        if (currentStep) {
            currentStep.done = true
            currentStep.save()
            
            boolean finished = true
            steps.each {
                if (!it.done) {
                    finished = false
                }
            }
            if (finished) {
                done = true
                save()
            }
        }
        return ordered ? increment() : currentStep
    }
    
    WizardStep increment() {
        if (done) {
            currentStep = null
        } else {
            int n = iterate(index())
            if (n >= steps.size()) {
                n = iterate(0)
            }
            if (n >= steps.size()) {
                currentStep = steps[0]
            }
        }
        save()
        return currentStep
    }

    private int iterate(int n) {
        while (n < steps.size()) {
            currentStep = steps[n]
            if (!currentStep.done) {
                break
            }
            n++
        }
        return n
    }
    
    static constraints = {
        //substep(nullable: true)
        currentStep(nullable: true)
    }

    static Wizard getActiveWizards() {
        def rows = Wizard.list()
        return rows?.findAll { it.active }
    }

    static Wizard getLastActiveWizard() {
        Wizard w = null
        def rows = Wizard.list().reverse()
        rows.each { 
            if (!w && it.active) {
                w = it
            }
        }
        return w
    }
    
    static Wizard getLastWizard() {
        def rows = Wizard.list()
        return rows ? rows.last() : null
    }
}
