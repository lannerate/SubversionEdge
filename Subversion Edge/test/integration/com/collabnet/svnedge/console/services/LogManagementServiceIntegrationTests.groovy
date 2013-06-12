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
package com.collabnet.svnedge.console.services

import grails.test.*
import com.collabnet.svnedge.admin.LogManagementService 
import com.collabnet.svnedge.domain.LogConfiguration
import com.collabnet.svnedge.util.ConfigUtil;

/**
 * this test class validates the persistence of LogLevel elections
 */
class LogManagementServiceIntegrationTests extends GrailsUnitTestCase {

    def logManagementService

    protected void setUp() {
        super.setUp()
        logManagementService.setConsoleLevel(LogManagementService.ConsoleLogLevel.DEBUG)
    }

    protected void tearDown() {
        super.tearDown()
        logManagementService.setConsoleLevel(LogManagementService.ConsoleLogLevel.DEBUG)
    }

    void testConsoleLogLevelSet() {

        def c = logManagementService.getConsoleLevel()
        assertEquals ("Default level for console should be DEBUG for test", LogManagementService.ConsoleLogLevel.DEBUG, c )

        logManagementService.setConsoleLevel(LogManagementService.ConsoleLogLevel.INFO)
        c = logManagementService.getConsoleLevel()

        assertEquals ("Console should be INFO after setting", LogManagementService.ConsoleLogLevel.INFO, c )

    }

    void testUpdateLogConfiguration() {


        logManagementService.updateLogConfiguration(LogManagementService.ConsoleLogLevel.WARN,
                LogManagementService.ApacheLogLevel.ERROR, 5, true, true, false, 10, false)

        // verify console update
        def c = logManagementService.getConsoleLevel()
        assertEquals ("Console should be WARN after setting", LogManagementService.ConsoleLogLevel.WARN, c )

        // verify the apache conf update
        def confFile = new File(ConfigUtil.confDirPath(), "csvn_logging.conf")

        boolean foundLogLevel = false
        confFile.eachLine {
            it -> if (it.startsWith("LogLevel")) {
                foundLogLevel = true
                assertTrue ("Apache conf should contain 'LogLevel error'", it.equals("LogLevel error"))
            }
        }

        if (!foundLogLevel) {
            fail ("Did not find Apache LogLevel config")
        }

        def s = LogConfiguration.getConfig()
        assertEquals ("Expected persistence of pruneLogsOlderThan field", 5, s.pruneLogsOlderThan)
        assertEquals ("Expected persistence of maxLogSize field", 10, s.maxLogSize)
        assertEquals ("Expected persistence of consoleLevel", LogManagementService.ConsoleLogLevel.WARN,
                s.consoleLogLevel)

        assertEquals ("Expected persistence of apacheLevel", LogManagementService.ApacheLogLevel.ERROR,
                s.apacheLogLevel)


    }

    void testSuppressAccessLog() {
        logManagementService.updateLogConfiguration(LogManagementService.ConsoleLogLevel.WARN,
                        LogManagementService.ApacheLogLevel.ERROR, 5, false, true, false, 0, false)

        // verify the apache conf update
        def confFile = new File(ConfigUtil.confDirPath(), "csvn_logging.conf")

        boolean foundAccessLog = false
        boolean foundSubversionLog = false
        confFile.eachLine { it -> 
            if (it.startsWith("CustomLog")) {
                if (it.contains('access_')) {
                    foundAccessLog = true
                }
                if (it.contains('subversion_')) {
                    foundSubversionLog = true
                }
            }
        }
        assertFalse("Apache conf should NOT contain access log config", foundAccessLog)
        assertTrue("Apache conf should contain subversion log config", foundSubversionLog)
    }
        
    void testSuppressSubversionLog() {
        logManagementService.updateLogConfiguration(LogManagementService.ConsoleLogLevel.WARN,
                        LogManagementService.ApacheLogLevel.ERROR, 5, true, false, false, 0, false)

        // verify the apache conf update
        def confFile = new File(ConfigUtil.confDirPath(), "csvn_logging.conf")

        boolean foundAccessLog = false
        boolean foundSubversionLog = false
        confFile.eachLine { it -> 
            if (it.startsWith("CustomLog")) {
                if (it.contains('access_')) {
                    foundAccessLog = true
                }
                if (it.contains('subversion_')) {
                    foundSubversionLog = true
                }
            }
        }
        assertTrue("Apache conf should contain access log config", foundAccessLog)
        assertFalse("Apache conf should NOT contain subversion log config", foundSubversionLog)
    }

    void testBootstrap() {

        File tempLogs = new File(ConfigUtil.logsDirPath(), "temp")
        if (tempLogs.exists()) {
            tempLogs.eachFile { it.delete() }
            tempLogs.deleteDir()
        }

        assertFalse("The temp log folder should not exist", tempLogs.exists())
        logManagementService.bootstrap()
        assertTrue("The temp log dir should now exist", tempLogs.exists())

    }

}

