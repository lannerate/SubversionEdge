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

import com.collabnet.svnedge.integration.command.ShortRunningCommand 

/**
 * This event signals the command executor that a new short-running command
 * has been queued.
 *
 * @author Marcello de Sales (mdesales@collab.net)
 *
 */
class ShortRunningCommandQueuedEvent extends ReplicaCommandsExecutionEvent {

    /**
     * The instance of a short-running command queued.
     */
    def final queuedCommand

    def ShortRunningCommandQueuedEvent(source, ShortRunningCommand command) {
        super(source, command.context)
        this.queuedCommand = command
    }
}
