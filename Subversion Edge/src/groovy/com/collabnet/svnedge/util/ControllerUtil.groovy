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
package com.collabnet.svnedge.util

import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log

import com.collabnet.svnedge.domain.Wizard

/**
 * Class for common utility methods shared by controllers
 */
public class ControllerUtil {
    
    static Log log = LogFactory.getLog(ControllerUtil.class)

    /**
     * Helper to extract "list view selected ids" from a request parameter collection
     * @param params
     * @return list of id string corresponding to the selected item ids of a list view
     */
    public static List getListViewSelectedIds(Map params) {
        def ids = []
        params.each() {
            def matcher = it.key =~ /listViewItem_(.+)/
            if (matcher && matcher[0][1]) {
                def id = matcher[0][1]
                if (it.value == "on") {
                    ids << id
                }
            }
        }
        return ids
    }

    /**
     * Helper to stream a request body into a temporary file
     * @param request the HttpServletRequest whose body will be written as a file
     * @param targetFile empty file into which to write the file (optional -- temp file created
     * otherwise)
     * @return File handle to the temporary file
     */
    public static File getFileFromRequest(request, targetFile = null) {
        if (!targetFile) {
            targetFile = File.createTempFile("requestBody", ".tmp")
        }
        log.debug("Writing request body to file: ${targetFile.canonicalPath}")
        targetFile.withOutputStream { it << request.inputStream }
        return targetFile
    }

    /**
     * Sets the 'sort' and 'order' properties for the request, only if they have
     * not been sent by the client.
     * 
     * @param params query parameter map injected into the controller
     * @param sortBy default column
     */
    public static void setDefaultSort(params, sortBy, order = "asc") {
        if (!params.sort) {
            params.sort = sortBy
            params.order = order
        }
        if (!params.order) {
            params.order = order
        }
    }

    /**
     * Helper to create "size" and "date" bean-style properties on the File class
     * for use by jstl 
     */
    public static void decorateFileClass() {
        // add virtual bean-style properties to File for "date" and "size" 
        File.metaClass.getDate = {-> delegate.lastModified() }
        File.metaClass.getSize = {-> delegate.length() }
    }
    
    public static void wizardCompleteStep(def controller, Closure defaultLogic) {
        def wizard = Wizard.getLastActiveWizard()
        if (wizard) {
            def helper = wizard.currentStep?.helper()
            if (helper?.targetController == controller.controllerName &&
                    (helper.targetAction == controller.actionName ||
                     helper.alternateActions.contains(controller.actionName))) {
                wizard.completeCurrentStep()
                if (wizard.done) {
                    controller.flash.wizard_message = 
                            controller.message(code: 'wizard.' + 
                            wizard.label + '.done')
                    controller.redirect(controller: 'status', action: 'index')
                } else {
                    helper = wizard.currentStep?.helper()
                    if (helper?.forceTarget) {
                        controller.redirect(controller: helper.targetController,
                                action: helper.targetAction)
                    } else {
                        defaultLogic()
                    }
                }
            } else {
                defaultLogic()
            }
        } else {
            defaultLogic()
        }
    }
}
