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
package com.collabnet.svnedge.services

import com.collabnet.svnedge.console.CommandLineService;
import com.collabnet.svnedge.console.OperatingSystemService;

import grails.test.GrailsUnitTestCase;

class CommandLineServiceTests extends GrailsUnitTestCase {

    def commandLineService
    def operatingSystemService

    protected void setUp() {
        super.setUp()

        commandLineService = new CommandLineService()
        operatingSystemService = new OperatingSystemService()
    }

    void testExecuteSuccessfulCommand() {
        def cmd = "ping www.facebook.com -c 3"
        if (operatingSystemService.isSolaris()) {
            cmd = "/usr/sbin/ping -s www.facebook.com 56 3"
        }
        if (operatingSystemService.isWindows()) {
            cmd = "ping www.facebook.com -n 3"
        }
        println "Executing command $cmd"
        def outputListener = commandLineService.executeAsync(cmd)
        assertNotNull "The output listener must exist", outputListener

        def allLines = []
        def line
        while ((line = outputListener.getNextOutputLine()) != null) {
            assertNotNull "Each line from the execution must be not null", line
             println line
             allLines << line
        }
        assertNotNull "The command must have produced lines", allLines
    }

    void testExecuteMultipleCommandsInParallel() {
        def cmd = "ping www.google.com -c 3"
        def cmd2 = "ping www.collab.net -c 2"

        if (operatingSystemService.isSolaris()) {
            cmd = "/usr/sbin/ping -s www.google.com 56 3"
            cmd2 = "/usr/sbin/ping -s www.collab.net 56 2"
        }
        if (operatingSystemService.isWindows()) {
            cmd = "ping www.google.com -n 3"
            cmd2 = "ping www.collab.net -n 2"
        }
        // execute the command in parallel
        println "Executing command $cmd"
        def outputListener = commandLineService.executeAsync(cmd)
        println "Executing command $cmd2"
        def outputListener2 = commandLineService.executeAsync(cmd2)

        assertNotNull "The output listener 1 must exist", outputListener
        assertNotNull "The output listener 2 must exist", outputListener2

        def allLines = []
        def line
        while ((line = outputListener.getNextOutputLine()) != null) {
            assertNotNull "Each line from the execution must be not null", line
             println line
             allLines << line
        }
        def allLines2 = []
        def line2
        while ((line2 = outputListener2.getNextOutputLine()) != null) {
            assertNotNull "Each line from the execution must be not null", line2
             println line2
             allLines2 << line2
        }

        assertNotNull "The command 1 must have produced lines", allLines
        assertNotNull "The command 2 must have produced lines", allLines2

        def cmd1Domain = allLines.toString().contains("google.com") && 
            !allLines.toString().contains("collab.net")
        assertTrue "The command 1 must contain the string 'google.com'", 
            cmd1Domain

        def cmd1Domain2 = allLines2.toString().contains("collab.net") && 
            !allLines2.toString().contains("google.com")
        assertTrue "The command 2 must contain the string 'collab.net'", 
            cmd1Domain2
    }

    void testExecuteAsyncWithErrors() {
        def cmd = "wrongping www.google.com -c 3"
        try {
            def outputListener = commandLineService.executeAsync(cmd)
            fail "The command execution with a non-existing OS command must " +
                "throw an exception"

        } catch (Exception e) {
            println e.getMessage()
        }
    }
}
