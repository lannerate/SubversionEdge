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

import com.collabnet.svnedge.CantBindPortException;
import com.collabnet.svnedge.domain.integration.ApprovalState 
import com.collabnet.svnedge.domain.integration.ReplicaConfiguration 
import com.collabnet.svnedge.integration.command.AbstractReplicaCommand 
import com.collabnet.svnedge.integration.command.ShortRunningCommand 

/**
 * This command updates the state of the replica server with the URL of the
 * Master repository after the Master has approved this server as a replica
 * 
 * @author John Mcnally (jmcnally@collab.net)
 * @author Marcello de Sales (mdesales@collab.net)
 *
 */
public class ReplicaApproveCommand extends AbstractReplicaCommand 
        implements ShortRunningCommand {

    private Logger log = Logger.getLogger(getClass())

    def repoFileDir
    def repoDbTuple

    def constraints() {
        def replica = ReplicaConfiguration.getCurrentConfig()
        if (replica.approvalState == ApprovalState.APPROVED) {
            throw new IllegalStateException("The replica is already approved " +
                "with the id '${replica.systemId}'")
        }

        // Verify if the necessary parameters exists.
        if (!this.params["scmUrl"]) {
            throw new IllegalStateException("The command does not have the " +
                "required parameter 'scmUrl'.")
        }
        if (!this.params["masterId"]) {
            throw new IllegalStateException("The command does not have the " +
                "required parameter 'masterId'.")
        }
        logExecution("CONSTRAINTS-verified")
    }

    def execute() {
        log.debug("Acquiring the replica setup service...")

        def url = this.params["scmUrl"]
        def masterId = this.params["masterId"]
        log.debug("Updating replica with master URL: " + url + 
            " and masterId: " + masterId)

        logExecution("EXECUTE-updateServerAfterApproval")
        def replicaService = getService("setupReplicaService")
        replicaService.updateServerAfterApproval(url, masterId)

        logExecution("EXECUTE-updateProps")
        updateProps()

        logExecution("EXECUTE-updateFetchRate")
        updateFetchRate()

        logExecution("EXECUTE-updateExecutorPoolSizes")
        updateExecutorPoolSizes()
        try {
            def setupTeamForgeService = getService("setupTeamForgeService")
            setupTeamForgeService.restartServer()

        } catch (CantBindPortException cantRestartServer) {
            log.error("Although the registration succeeded, there was an " +
                "error restarting the apache/subversion server",
                cantRestartServer)
            logExecution("EXECUTE-restartserver", cantRestartServer)
        }
   }

   def undo() {
       log.debug("Execute failed... Nothing to undo...")
       logExecution("UNDO-terminated")
    }
}
