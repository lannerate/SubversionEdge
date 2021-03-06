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

import org.apache.log4j.Logger
import com.collabnet.svnedge.integration.command.AbstractCommand

/**
 * This command simulates a long running Replica Command for testing purposes
 */
public class MockLongRunningCommand extends AbstractCommand 
        implements com.collabnet.svnedge.integration.command.LongRunningCommand {

    private Logger log = Logger.getLogger(getClass())

    def constraints() {
        log.debug("Constraints...")
    }

    def execute() {
        log.debug("Executing long-running command with 15 second wait...")
        Thread.sleep 15000
    }

    def undo() {
        log.debug("Undoing...")
    }
}
