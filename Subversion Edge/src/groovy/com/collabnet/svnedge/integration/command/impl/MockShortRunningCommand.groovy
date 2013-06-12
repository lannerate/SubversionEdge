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
package com.collabnet.svnedge.integration.command.impl

import com.collabnet.svnedge.integration.command.AbstractCommand
import org.apache.log4j.Logger

/**
 * This command simulates a short running Replica command for testing purposes
 */
public class MockShortRunningCommand extends AbstractCommand 
        implements com.collabnet.svnedge.integration.command.ShortRunningCommand {

    private Logger log = Logger.getLogger(getClass())
    private int runCount = 0
    public static String FAILED = "Simulating command failure"
    
    def constraints() {
        log.debug("Constraints...")
    }

    def execute() {
        int expectedFailures = this.params?.commandExpectedFailures ?: 0
        if (runCount++ < expectedFailures) {
            log.debug FAILED + " " + runCount
            throw new RuntimeException(FAILED + " " + runCount)
        }    
        long runTime = this.params?.commandRunTimeSeconds ?: 5
        log.debug("Executing short-running command with " + runTime +
                  " second run time...")
        Thread.sleep(runTime * 1000L)
    }

    def undo() {
        log.debug("Undoing...")
    }
}
