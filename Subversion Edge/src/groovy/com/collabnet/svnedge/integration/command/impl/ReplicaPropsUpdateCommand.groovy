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

import com.collabnet.svnedge.domain.integration.ApprovalState 
import com.collabnet.svnedge.domain.integration.ReplicaConfiguration 
import com.collabnet.svnedge.integration.command.AbstractReplicaCommand 
import com.collabnet.svnedge.integration.command.ShortRunningCommand 

/**
 * This command updates the state of the replica server, changing the name and
 * description of the replica server.
 * 
 * @author John Mcnally (jmcnally@collab.net)
 * @author Marcello de Sales (mdesales@collab.net)
 *
 */
public class ReplicaPropsUpdateCommand extends AbstractReplicaCommand 
        implements ShortRunningCommand {

    private Logger log = Logger.getLogger(getClass())

    def constraints() {
        def replica = ReplicaConfiguration.getCurrentConfig()
        if (!replica.approvalState.equals(ApprovalState.APPROVED)) {
            throw new IllegalStateException("The replica needs to be " +
                "approved before updating its properties.")
        }

        // Verify if the parameter "scmUrl" exists.
        if (!this.params["name"] && !this.params["description"] && 
                !this.params.commandPollPeriod && 
                !this.params.commandConcurrencyLong && 
                !this.params.commandConcurrencyShort) {
            throw new IllegalStateException("The command does not have any " +
                "of the required parameters.")
        }

        if (this.params.commandPollPeriod && 
                this.params.commandPollPeriod.toInteger() < 1) {
            throw new IllegalArgumentException("The fetch rate must be a " +
                "positive integer")
        }
        if (this.params.commandConcurrencyLong && 
                this.params.commandConcurrencyLong.toInteger() < 1) {
            throw new IllegalArgumentException("The max number of " +
                "long-running commands must be a positive integer")
        }
        if (this.params.commandConcurrencyShort && 
                this.params.commandConcurrencyShort.toInteger() < 1) {
            throw new IllegalArgumentException("The max number of " +
                "short-running commands must be a positive integer")
        }
    }

    def execute() {
        log.debug("Acquiring the replica configuration instance...")

        logExecution("EXECUTE-updateProps")
        updateProps()

        logExecution("EXECUTE-updateFetchRate")
        updateFetchRate()

        logExecution("EXECUTE-updateExecutorPoolSizes")
        updateExecutorPoolSizes()
    }

    def undo() {
       log.debug("Execute failed... Nothing to undo...")
       logExecution("UNDO-terminated")
    }
}
