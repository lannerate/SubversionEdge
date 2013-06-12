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

import com.collabnet.svnedge.domain.LogConfiguration
import grails.converters.JSON
import grails.converters.XML
import org.codehaus.groovy.grails.plugins.springsecurity.Secured

import com.collabnet.svnedge.admin.LogManagementService.ConsoleLogLevel
import com.collabnet.svnedge.admin.LogManagementService.ApacheLogLevel

/**
 * REST API controller for retrieving and updating the logging settings
 * <p><bold>URL:</bold></p>
 * <code>
 *   /csvn/api/1/logging
 * </code>
 */
@Secured(['ROLE_ADMIN', 'ROLE_ADMIN_SYSTEM'])
class LoggingRestController extends AbstractRestController {

    /**
     * <p>Rest method to view the logging configuration for the Subversion console and server. The log levels are
     * one of the following: DEBUG, INFO, WARN, ERROR. The DaysToKeep field indicates how many days of
     * log to keep (0 means keep all).</p>
     * 
     * <p><bold>HTTP Method:</bold></p>
     * <code>
     *     GET
     * </code>
     * 
     * <p><bold>JSON-formatted request body example:</bold></p>
     * <pre>
     * {
     *   "consoleLogLevel": "WARN",
     *   "serverLogLevel": "WARN",
     *   "daysToKeep": 3
     * }
     * </pre>
     */
    def restRetrieve = {
        def result = [:]
        LogConfiguration lc = LogConfiguration.getConfig()

        result.put "consoleLogLevel", lc.consoleLogLevel.toString()
        result.put "serverLogLevel", lc.apacheLogLevel.toString()
        result.put "daysToKeep", lc.pruneLogsOlderThan 
        result.put "enableAccessLog", lc.enableAccessLog
        result.put "enableSubversionLog", lc.enableSubversionLog
        result.put "enableLogCompression", lc.enableLogCompression
        result.put "maxLogSize", lc.maxLogSize
        
        withFormat {
            json { render result as JSON }
            xml { render result as XML }
        }
    }

    /**
     * <p>Rest method to update the logging configuration for the Subversion server and console. The log levels can be
     * one of the following: DEBUG, INFO, WARN, ERROR. The DaysToKeep field indicates how many days of
     * log to keep (use 0 to keep all). Returns Status Code 201 on success.</p>
     * 
     * <p><bold>HTTP Method:</bold></p>
     * <code>
     *   PUT
     * </code>    
     * 
     * <p><bold>JSON-formatted request body example:</bold></p>
     * <pre>
     * {
     *   "consoleLogLevel": "WARN",
     *   "serverLogLevel": "WARN",
     *   "daysToKeep": 3
     * }
     * </pre>
     */
    def restUpdate = {
        def consoleLogLevel = getRestParam("consoleLogLevel")
        def apacheLogLevel = getRestParam("serverLogLevel")
        def daysToKeep = getRestParam("daysToKeep")
        def enableAccessLog = getRestParam("enableAccessLog")
        def enableSubversionLog = getRestParam("enableSubversionLog")
        def enableLogCompression = getRestParam("enableLogCompression")
        def maxLogSize = getRestParam("maxLogSize")
        
        def result = [:]
        try {
            LogConfiguration lc = LogConfiguration.getConfig()
            if (consoleLogLevel) {
                lc.consoleLogLevel = ConsoleLogLevel.valueOf(consoleLogLevel)
            }
            if (apacheLogLevel) {
                lc.apacheLogLevel = ApacheLogLevel.valueOf(apacheLogLevel)
            }
            if (daysToKeep) {
                lc.pruneLogsOlderThan = Integer.parseInt(daysToKeep)
            }
            if (enableAccessLog) {
                lc.enableAccessLog = Boolean.valueOf(enableAccessLog)
            }
            if (enableSubversionLog) {
                lc.enableSubversionLog = Boolean.valueOf(enableSubversionLog)
            }
            if (enableLogCompresion) {
                lc.enableLogCompression = Boolean.valueOf(enableLogCompression)
            }
            if (maxLogSize) {
                lc.pruneLogsOlderThan = Integer.parseInt(maxLogSize)
            }
            lc.save()
            response.status = 201
            result['message'] = message(code: "api.message.201")
        }
        catch (Exception e) {
            response.status = 400
            result['errorMessage'] = message(code: "api.error.400")
            log.warn("Exception handling a REST PUT request", e)
        }
        
        withFormat {
            json { render result as JSON }
            xml { render result as XML }
        }
    }
}

