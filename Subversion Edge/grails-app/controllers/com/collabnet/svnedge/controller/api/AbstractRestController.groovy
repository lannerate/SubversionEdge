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

package com.collabnet.svnedge.controller.api

import grails.converters.JSON
import grails.converters.XML
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import grails.converters.deep.XML
import org.json.JSONObject
import groovy.util.slurpersupport.GPathResult
import org.springframework.validation.Errors
import org.springframework.context.ApplicationContextAware
import org.codehaus.groovy.grails.commons.ApplicationHolder

/**
 * Default "not-implemented" endpoints for rest controllers
 */
abstract class AbstractRestController {

    def restRetrieve = {
        response.status = 405
        def result = [errorMessage: message(code: "api.error.405")]
        withFormat {
            json { render result as JSON }
            xml { render result as XML }
        }
    }

    def restUpdate = {
        response.status = 405
        def result = [errorMessage: message(code: "api.error.405")]
        withFormat {
            json { render result as JSON }
            xml { render result as XML }
        }
    }

    def restDelete = {
        response.status = 405
        def result = [errorMessage: message(code: "api.error.405")]
        withFormat {
            json { render result as JSON }
            xml { render result as XML }
        }
    }

    def restSave = {
        response.status = 405
        def result = [errorMessage: message(code: "api.error.405")]
        withFormat {
            json { render result as JSON }
            xml { render result as XML }
        }
    }

    /**
     * This helper will read the JSON or XML request body into a request parameter for use
     * by the <code>getRestParam</code> method
     */
    void parseRestRequest() {
        // return immediately if request has already been parsed
        if (params.requestParsed) {
            return
        }
        params.requestParsed = true
        try {
            if (request.format == "json") {
                params.bodyJson = grails.converters.JSON.parse(request)
            }
            else if (request.format == "xml") {
                params.bodyXml = grails.converters.deep.XML.parse(request)
            }
        } 
        catch (Exception e) {
            log.warn("Unable to parse JSON or XML body in request: ${e.message}")
        }
    }

    /**
     * Convenience method to find a Query param or XML/JSON element in the request body. Ensures that
     * <code>parseRestRequest</code> has been run.
     * @param elementKey the key we seek
     * @return the corresponding value, or null
     */
    String getRestParam(String elementKey) {

        // check for standard GET or POST param first, then look to request body
        if (params.elementKey) {
            return params.elementKey
        }

        parseRestRequest()
        
        def elementValue = null
        if (params.bodyXml)  {
            elementValue = params.bodyXml.entry.find({ it.@key == elementKey })?.text() 
        }
        else if (params.bodyJson) {
            elementValue = params.bodyJson[elementKey] 
        }
        return elementValue
    }

    /**
     * Formatting helper to prepare validation errors for api usage
     * @param errors the BindingResult/Validation errors
     * @return String formatted result
     */
    String formatErrors(Errors errors) {
        if (!errors) {
            return null
        }
        StringBuilder sb = new StringBuilder()
        for(it in errors.allErrors) {
            def args = it.arguments ?: null
            def code = it.code
            def msg = ApplicationHolder.application.mainContext.getMessage(code, args, 
                    it.defaultMessage, request.locale)
            sb.append(msg + "\n")
        }
        return sb.toString()
    }
}
