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
package com.collabnet.svnedge.integration.command.event

/**
 * This event signals the change of the maximum number of long-running and
 * short-running commands parameter to the executor's semaphores. It carries the
 * references to the old and new values for each of the semaphore's.
 *
 * @author Marcello de Sales (mdesales@collab.net)
 *
 */
final class MaxNumberCommandsRunningUpdatedEvent extends ReplicaCommandsExecutionEvent {

    /**
     * The old max number of long-running commands for the long-running 
     * semaphore.
     */
    def final oldMaxLongRunningCmds
    /**
     * The new max number of long-running commands for the long-running 
     * semaphore.
     */
    def final newMaxLongRunningCmds
    /**
     * The old max number of short-running commands for the short-running 
     * semaphore.
     */
    def final oldMaxShortRunningCmds
    /**
     * The new max number of short-running commands for the short-running 
     * semaphore.
     */
    def final newMaxShortRunningCmds

    def MaxNumberCommandsRunningUpdatedEvent(source, int oldMaxLongRunningCmds,
            int newMaxLongRunningCmds, int oldMaxShortRunningCmds,
            int newMaxShortRunningCmds) {

        super(source, null)
        this.oldMaxLongRunningCmds = oldMaxLongRunningCmds
        this.newMaxLongRunningCmds = newMaxLongRunningCmds
        this.oldMaxShortRunningCmds = oldMaxShortRunningCmds
        this.newMaxShortRunningCmds = newMaxShortRunningCmds
    }
}
