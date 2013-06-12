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
package com.collabnet.svnedge.admin


import java.io.FileNotFoundException;

import org.apache.log4j.Level
import org.apache.log4j.Logger
import com.collabnet.svnedge.admin.LogRotateJob 
import com.collabnet.svnedge.domain.LogConfiguration
import com.collabnet.svnedge.util.ConfigUtil;

/**
 * This service offers methods for configuring Apache and Console
 * logging, and provides access to the List of log files
 */
class LogManagementService {

    def operatingSystemService
    def serverConfService
    def lifecycleService
    def jobsAdminService

    def dataDir

    static final List GrailsLogs = [
        [name: 'Grails Application', logger: 'grails.app'],
        [name: 'Controllers', logger: 'grails.app.controller'],
        [name: 'Services', logger: 'grails.app.service'],
        [name: 'Domains', logger: 'grails.app.domain'],
        [name: 'Filters', logger: 'grails.app.filters'],
        [name: 'TagLibs', logger: 'grails.app.taglib'],
        [name: 'Grails Web Requests', logger: 'org.codehaus.groovy.grails.web'],
        [name: 'URLMappings', logger: 'org.codehaus.groovy.grails.web.mapping'],
        [name: 'Application Classes', logger: 'com.collabnet']
    ]

    public static enum ConsoleLogLevel { DEBUG, INFO, WARN, ERROR}
    public static enum ApacheLogLevel { DEBUG, INFO, WARN, ERROR }
    public static String LOG_SIZE_SUFFIX = 'M'
    
    public def bootstrap = {
        ConsoleLogLevel consoleLogLevel = LogConfiguration.getConfig()?.consoleLogLevel
        if (consoleLogLevel) {
            setConsoleLevel(consoleLogLevel)
        }
        jobsAdminService.createOrReplaceTrigger(LogRotateJob.createTrigger())

        // ensure the existence of the temp logs directory
        File tempLogDir = new File(ConfigUtil.logsDirPath(), "temp")
        if (!tempLogDir.exists()) {
            tempLogDir.mkdir()
        }
    }

    /**
     * This method configures the logging of both Apache and Jetty/Csvn.
     * @param consoleLevel the ConsoleLogLevel for csvn logging
     * @param apacheLevel the ApacheLogLevel for ErrorLog
     * @param daysToKeep days of log to keep before clearing (0 to keep all)
     */
    void updateLogConfiguration(ConsoleLogLevel consoleLevel,
            ApacheLogLevel apacheLevel, Integer daysToKeep, 
            boolean enableAccessLog, boolean enableSubversionLog,
            boolean minimizeLogging, Integer maxLogSize, 
            boolean enableLogCompression) {

        log.info("Setting apache log level: " + apacheLevel)
        log.info("Setting console log level: " + consoleLevel)
        log.info("Setting log days to keep: " + daysToKeep)
        log.info("Access log enabled: " + enableAccessLog)
        log.info("Subversion log enabled: " + enableSubversionLog)
        log.info("Minimize logging: " + minimizeLogging)
        log.info("Log compression enabled: " + enableLogCompression)
        log.info("Max log size (MB): " + maxLogSize)
        
        // adjust console logging to requested level
        setConsoleLevel(consoleLevel)

        // update config instance and re-write files if needed
        LogConfiguration lc = LogConfiguration.getConfig()
        def apacheRestartNeeded = (apacheLevel != lc.apacheLogLevel ||
                enableAccessLog != lc.enableAccessLog || 
                enableSubversionLog != lc.enableSubversionLog ||
                minimizeLogging != lc.minimizeLogging ||
                maxLogSize != lc.maxLogSize)

        lc.apacheLogLevel = apacheLevel
        lc.consoleLogLevel = consoleLevel
        lc.pruneLogsOlderThan = daysToKeep
        lc.enableAccessLog = enableAccessLog
        lc.enableSubversionLog = enableSubversionLog
        lc.enableLogCompression = enableLogCompression
        lc.minimizeLogging = minimizeLogging
        lc.maxLogSize = maxLogSize
        lc.save()

        if (apacheRestartNeeded) {

            serverConfService.writeLogConf()
            lifecycleService.gracefulRestartServer()
        }

    }

    /**
     * Fetches the log files as a List
     * @return List of File
     */
    List<File> getLogFiles() {
        def files = Arrays.asList (new File(
                ConfigUtil.dataDirPath() + "/logs").listFiles(
                {file -> !file.isDirectory() && !file.name.endsWith('.lck') } as FileFilter))

        return files
    }

    /**
     * Provides the current logging level for the console app
     * @return ConsoleLogLevel of the runtime environment
     */
    ConsoleLogLevel getConsoleLevel() {
      
        def consoleLevel = Logger.getLogger("grails.app").getLevel() ?: Level.INFO
        return Enum.valueOf(ConsoleLogLevel, consoleLevel.toString().toUpperCase())
        
    }

    /**
     * Sets the current logging level of the console app
     * @param consoleLevel new ConsoleLogLevel
     */
    void setConsoleLevel(ConsoleLogLevel consoleLevel) {

        // take no action if we are setting the same level
        if (consoleLevel == getConsoleLevel()) {
            return
        }

        // convert local enum to Log4j
        def level = Level.toLevel(consoleLevel.toString())

        // iterate the Map of logs that we want to configure
        GrailsLogs.each {
            def lggr = it.logger
            Logger l = (lggr) ? Logger.getLogger(lggr) : Logger.getRootLogger()
            l.setLevel(level)
        }

        // print the main properties from the operating system
        operatingSystemService.printProperties()
    }

    /**
     * Provides the current logging level for Apache
     * @return ApacheLogLevel
     */
    ApacheLogLevel getApacheLevel() {      
        return LogConfiguration.getConfig().apacheLogLevel ?: ApacheLogLevel.WARN
    }

    /**
     * Provides the current days of log to keep
     * @return Integer
     */
    Integer getLogDaysToKeep() {
        
        def pruneDays = LogConfiguration.getConfig().pruneLogsOlderThan ?: 0
        return pruneDays
    }

    /**
     * Gets the File instance of the log given the name.
     * @param logFileName is the name of a file that must exist in the directory
     * 'CSVN/data/logs'.
     * @return the File object with the reference to the log file.
     */
    def getLogFile(logFileName) throws FileNotFoundException {
        def file = new File(ConfigUtil.dataDirPath() + 
            "/logs/" + logFileName.trim())
        if (!file.exists()) {
            throw new FileNotFoundException("The log file " +
                "'${file.canonicalPath}' does not exist")
        }
        return file
    }
}
