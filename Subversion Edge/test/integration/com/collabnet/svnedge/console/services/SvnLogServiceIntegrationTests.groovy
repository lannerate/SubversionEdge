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

import com.collabnet.svnedge.domain.SvnLog 
import grails.test.*

class SvnLogServiceIntegrationTests extends GrailsUnitTestCase {
    def svnLogService

    def repoName = "svnLogRepo"

    def logFile

    // it may be appropriate to add more lines (or change them) to test
    // additional patterns
    def logLines = [
        "[11/Nov/2009:15:45:54 -0800] root ${repoName} checkout-or-export " +
            "/trunk r3 depth=infinity 0",
        "[11/Nov/2009:16:51:01 -0800] root ${repoName} checkout-or-export " +
            "/trunk r5 depth=infinity 0",
        "[11/Nov/2009:16:53:55 -0800] root ${repoName} update /trunk r6 " +
            "send-copyfrom-args 0", 
        "[11/Nov/2009:16:53:55 -0800] root ${repoName} update /trunk r6 " +
            "send-copyfrom-args 0",
        "[11/Nov/2009:16:53:55 -0800] root ${repoName} update /trunk r6 " +
            "send-copyfrom-args 0",
        "[11/Nov/2009:16:56:54 -0800] root ${repoName} update /trunk r6 " +
            "send-copyfrom-args 0",
        "[11/Nov/2009:16:58:55 -0800] root ${repoName} update /trunk/data " +
            "r7 send-copyfrom-args 0",
        "[11/Nov/2009:17:16:46 -0800] root ${repoName} update /trunk/data " +
            "r7 send-copyfrom-args 0"
    ]

    protected void setUp() {
        super.setUp()
        logFile = File.createTempFile("/tmp", "svnlog")
        println "created ${logFile.getCanonicalPath()}"
        svnLogService.svnLogFile = logFile.getCanonicalPath()
    }

    protected void tearDown() {
        super.tearDown()
        logFile.delete()
    }

    void testParseLine() {
        def svnLog
        logLines.eachWithIndex { line, i ->
            svnLog = svnLogService.parseLine(line, i)
            if (!svnLog) {
                fail("Failed to parse line (${line})")
            }
            if (!svnLog.validate()) {
                fail("Failed to validate parsed SvnLog ($svnLog)")
            }
        }
    }

    void testParseFile() {
        PrintWriter out = 
            new PrintWriter(new BufferedWriter(new FileWriter(logFile)))

        // first test the empty file
        svnLogService.parseFile()
        assertEquals("The number of svnLogs should be zero before adding " +
                         "lines.", 0, SvnLog.count())
        // now add a few lines and parse
        int stopAdd = 4
        logLines[0..<stopAdd].each { line ->
            out.write(line + "\n", 0, line.size() + 1)
        }
        out.flush()
        svnLogService.parseFile()
        assertEquals("The number of svnLogs should be equal to the number " +
                         "of lines added.", stopAdd, SvnLog.count())
        // now add the rest
        logLines[stopAdd..<logLines.size()].each { line ->
            out.write(line + "\n", 0, line.size() + 1)
        }
        out.flush()
        svnLogService.parseFile()
        assertEquals("The number of svnLogs should be equal to the number " +
                         "of lines added.", logLines.size(), SvnLog.count())
    }

    void testGetFirstLog() {
        loadData()
        SvnLog firstLog = svnLogService.getFirstLog()
        def firstTime = svnLogService.dateFormat
            .parse("11/Nov/2009:15:45:54 -0800").getTime()
        assertEquals("The first log should have the first time.", firstTime,
                     firstLog.getTimestamp())        
    }

    void testGetLastLog() {
        loadData()
        SvnLog lastLog = svnLogService.getLastLog()
        def lastTime = svnLogService.dateFormat
            .parse("11/Nov/2009:17:16:46 -0800").getTime()
        assertEquals("The last log should have the last time.", lastTime,
                     lastLog.getTimestamp())        
    }

    private void loadData() {
        PrintWriter out = 
            new PrintWriter(new BufferedWriter(new FileWriter(logFile)))
        logLines.each { line ->
            out.write(line + "\n", 0, line.size() + 1)
        }
        out.flush()
        svnLogService.parseFile()
    }

}
