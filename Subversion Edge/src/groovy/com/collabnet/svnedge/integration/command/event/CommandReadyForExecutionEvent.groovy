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

import com.collabnet.svnedge.integration.command.AbstractCommand 

/**
 * This event signals the executor service that the given command is ready 
 * to be executed by its executor (instance of a long-running or short-running)
 * . However, the command will only execute if there is a permit available to
 * it. In this case, the Event CommandAboutToRunEvent.
 *
 *  @author Marcello de Sales (mdesales@collab.net)
 *
 */
final class CommandReadyForExecutionEvent extends ReplicaCommandsExecutionEvent {

    /**
     * The instance of the command to be executed.
     */
    def final commandToExecute

    def CommandReadyForExecutionEvent(source, AbstractCommand command) {
        super(source, command.context)
        commandToExecute = command
    }
}
