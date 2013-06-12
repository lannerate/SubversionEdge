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
package com.collabnet.svnedge.controller.admin

import java.io.FileNotFoundException;
import java.text.SimpleDateFormat

import org.codehaus.groovy.grails.plugins.springsecurity.Secured
import com.collabnet.svnedge.admin.LogManagementService.ConsoleLogLevel
import com.collabnet.svnedge.admin.LogManagementService.ApacheLogLevel
import com.collabnet.svnedge.domain.LogConfiguration;
import com.collabnet.svnedge.domain.Server;
import com.collabnet.svnedge.domain.ServerMode

import org.springframework.web.servlet.support.RequestContextUtils as RCU
import com.collabnet.svnedge.util.ControllerUtil

/**
 * This Controller manages views and actions related to application logging.
 * This includes listing and viewing log files and configuring the apache and
 * console log levels.
 */
@Secured(['ROLE_ADMIN', 'ROLE_ADMIN_SYSTEM'])
class LogController {

    def operatingSystemService
    def logManagementService
    def setupTeamForgeService

    static allowedMethods = [saveConfiguration : 'POST']

    def list = {

        // fetch the log file list
        def files = logManagementService.getLogFiles()

        ControllerUtil.decorateFileClass()

        // sort log files based File property matching the sort param
        if (params.sort) {
          files = files.sort { f -> f."${params.sort}"}
        }

        if (params.order == "desc") {
          files = files.reverse()
        }
        def dtFormat = message(code: "default.dateTime.format.withZone")
        return [files: files, logDateFormat: dtFormat]
    }

    def saveConfiguration = { LogConfigurationCommand cmd ->

        if (!cmd.hasErrors()) {

            logManagementService.updateLogConfiguration(cmd.consoleLevel,
                    cmd.apacheLevel, cmd.pruneLogsOlderThan, 
                    cmd.enableAccessLog, cmd.enableSubversionLog, 
                    cmd.minimizeLogging, cmd.maxLogSize,
                    cmd.enableLogCompression)
            flash.message = message(code: 
                'logs.action.saveConfiguration.success')
            redirect(action: 'configure')
        }
        else {

            flash.error = message(code: 'default.errors.summary')
            render(view: 'configure', model : [ logConfigurationCommand : cmd,
                    consoleLevels : ConsoleLogLevel.values(),
                    apacheLevels : ApacheLogLevel.values()
                    ])
        }
    }

    def configure = {

        LogConfiguration logConfig = LogConfiguration.getConfig()
        def cmd = new LogConfigurationCommand(
            consoleLevel : logManagementService.consoleLevel,
            apacheLevel : logManagementService.apacheLevel,
            pruneLogsOlderThan : logManagementService.logDaysToKeep,
            enableAccessLog: logConfig.enableAccessLog,
            enableSubversionLog: logConfig.enableSubversionLog,
            minimizeLogging: logConfig.minimizeLogging,
            enableLogCompression: logConfig.enableLogCompression,
            maxLogSize: logConfig.maxLogSize)

        render(view: "configure", model: [ logConfigurationCommand : cmd,
                consoleLevels : ConsoleLogLevel.values(),
                apacheLevels : ApacheLogLevel.values()
                ])
    }

    def show = {
        def logName = params.fileName
        if (!logName || logName.trim().equals("")) {
            flash.error = message(code: 'logs.action.show.fileName.empty')
            redirect(action: "list")
            return
        }
        try {
            def logFile = logManagementService.getLogFile(logName)

            def view = (params.view == 'raw') ? "showRaw" : "show"
            def contentType = (params.view == 'raw') ? "text/plain" : "text/html"

            def logSize = operatingSystemService.formatBytes(logFile.length())
            def modifiedTime = new Date(logFile.lastModified())
            def currentLocale = RCU.getLocale(request)
            def dtFormat = message(code: "default.dateTime.format.withZone")
            def requestFormatter = new SimpleDateFormat(dtFormat,
                currentLocale)
            def logModifiedTime = requestFormatter.format(modifiedTime);
            
            // Help user in the event they are looking at a replica error
            // due to invalid api key
            if (logName.startsWith('replica') && params.highlight &&
                    Server.getServer().mode == ServerMode.REPLICA &&
                    !setupTeamForgeService.confirmApiSecurityKey()) {
                    
                request.unfiltered_warn = message(code: "replica.error.apiSecurityKey",
                        args: [createLink(controller: 'setupReplica',
                                          action: 'editCredentials')])
            }

            render(view: view, contentType: contentType,
                model: [ file: logFile, fileSize: logSize, fileSizeBytes: logFile.length(),
                    fileModification: logModifiedTime, dateTimeFormat:dtFormat])

        } catch (FileNotFoundException logDoesNotExist) {
            flash.error = message(code: 'logs.page.show.header.fileNotFound',
                args:[logName])
            redirect(action: "list")
            return
        }
    }

    /**
     * returns a tail of the log file starting at the given index (param.startIndex). when used by polling updater,
     * can be used for "live" log tail view
     */
    def tail = {
        def logName = params.fileName
        def startIndex = params.startIndex ? Long.parseLong(params.startIndex) : 0
        if (!logName || logName.trim().equals("")) {
            flash.error = message(code: 'logs.action.show.fileName.empty')
            redirect(action: "list")
            return
        }
        try {
            def logFile = logManagementService.getLogFile(logName)

            def modifiedTime = new Date(logFile.lastModified())
            def currentLocale = RCU.getLocale(request)
            def dtFormat = message(code: "default.dateTime.format.withZone")
            def requestFormatter = new SimpleDateFormat(dtFormat,
                currentLocale)
            def logModifiedTime = requestFormatter.format(modifiedTime);

            // fetch any new content after the startIndex
            StringBuffer buffer = new StringBuffer();
            FileInputStream fis = null
            InputStreamReader isr = null
            try {
                fis = new FileInputStream(logFile)
                isr = new InputStreamReader(fis, "UTF8")
                isr.skip(startIndex)
               
                int ch;
                while ((ch = isr.read()) > -1) {
                    buffer.append((char)ch)
                }
                isr.close();
                fis.close();

            } catch (IOException e) {
                log.error("unable to tail the logfile ${logName}", e)
                if (isr != null) {
                    isr.close()
                }
                if (fis != null) {
                    fis.close()
                }
            }

            render(contentType:"text/json") {
                log(fileName: logName,
                        lastModifiedTime: logModifiedTime,
                        startIndex: startIndex,
                        endIndex: startIndex + buffer.size(),
                        content: buffer.toString())
            }

        } catch (FileNotFoundException logDoesNotExist) {
            render(contentType:"text/json") {
                log(fileName: logName,
                        lastModifiedTime: 0,
                        startIndex: 0,
                        endIndex: 0,
                        content: "",
                        error: message(code: 'logs.page.show.header.fileNotFound',
                                args:[logName])
                )
            }
        }
    }
}

/**
 * Command class for 'saveConfiguration' action. Provides validation rules.
 */
class LogConfigurationCommand {

    def operatingSystemService
    ConsoleLogLevel consoleLevel
    ApacheLogLevel apacheLevel
    Integer pruneLogsOlderThan
    boolean enableAccessLog
    boolean enableSubversionLog
    boolean minimizeLogging
    boolean enableLogCompression
    Integer maxLogSize
    
    static constraints = {
        consoleLevel(nullable : false)
        apacheLevel(nullable : false)
        pruneLogsOlderThan(nullable : false, min : 0)
    }
}
