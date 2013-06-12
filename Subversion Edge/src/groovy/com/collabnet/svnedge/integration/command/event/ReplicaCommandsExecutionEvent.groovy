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

import org.springframework.context.ApplicationEvent;

import com.collabnet.svnedge.integration.command.CommandsExecutionContext;

/**
 * This is the abstract replica commands execution event used during the 
 * scheduling and execution of commands.
 * 
 * Events that requires remote communication with the Replica Manager (TeamForge)
 * server need to provide an instance of Commands Execution Context.
 *
 * @author Marcello de Sales (mdesales@collab.net)
 *
 */
abstract class ReplicaCommandsExecutionEvent extends ApplicationEvent {

    /**
     * The execution context used by the executor to communicate with 
     * the remote Replica Manager server.
     */
    def final executionContext

    def ReplicaCommandsExecutionEvent(source, CommandsExecutionContext context) {
        super(source)
        this.executionContext = context
    }
}
