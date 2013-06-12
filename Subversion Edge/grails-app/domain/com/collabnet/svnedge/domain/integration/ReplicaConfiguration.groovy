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
package com.collabnet.svnedge.domain.integration


/**
 * This stores the replication configuration.
 */
public class ReplicaConfiguration {

    static final Integer DEFAULT_MAX_LONG_RUNNING_COMMANDS = 2
    static final Integer DEFAULT_MAX_SHORT_RUNNING_COMMANDS = 10
    
    /**
     * The URL of the Master SVN
     */
    String svnMasterUrl
    /**
     * The name of the replica.
     */
    String name
    /**
     * The description of the replica: location, purpose, etc.
     */
    String description
    /**
     * the id assigned by CTF to this replica server
     */
    String systemId
    /**
     * state is relative to the current master.
     */
    ApprovalState approvalState
    /**
     * The pool rate in seconds.
     */
    Integer commandPollRate = 5
    /**
     * The max number of long-running commands such as svnsync.
     */
    Integer maxLongRunningCmds = DEFAULT_MAX_LONG_RUNNING_COMMANDS
    /**
     * The max number of short-running commands such as the props updates
     */
    Integer maxShortRunningCmds = DEFAULT_MAX_SHORT_RUNNING_COMMANDS

    /**
     * Fingerprint details
     */
    String acceptedCertFingerPrint
    
    int commandRetryAttempts = 0
    int commandRetryWaitSeconds = 5

    String contextPath() {
        String path = null
        if (svnMasterUrl) {
            path = new URL(svnMasterUrl).path
            if (path.endsWith("/")) {
                path = contextPath.substring(0, contextPath.length() - 1)
            }
        }
        return path
    }
    
    static constraints = {
        svnMasterUrl(nullable:true)
        acceptedCertFingerPrint(nullable:true)
        systemId(nullable:false)
        description(nullable:false)
        commandPollRate(nullable:false)
        maxLongRunningCmds(nullable:false)
        maxShortRunningCmds(nullable:false)
        commandRetryAttempts(min: 0, max: 10)
        commandRetryWaitSeconds(min: 0, max: 300)
    }

    /**
     * @return pseudo singleton provider
     */
    static ReplicaConfiguration getCurrentConfig() {
        def replicaConfigRows = ReplicaConfiguration.list()
        if (replicaConfigRows) {
            return replicaConfigRows.last()
        }
        else {
            return null
        }
    }
}
