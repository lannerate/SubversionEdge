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
package com.collabnet.svnedge.integration.command

import com.collabnet.svnedge.integration.command.event.MaxNumberCommandsRunningUpdatedEvent
import com.collabnet.svnedge.integration.FetchReplicaCommandsJob
import com.collabnet.svnedge.domain.integration.ReplicaConfiguration 

/**
 * Defines the Abstract Replica Server Command. That is, a command that is used
 * to manage the replica server properties.
 * 
 * @author Marcello de Sales (mdesales@collab.net)
 */
public abstract class AbstractReplicaCommand extends AbstractCommand {

    /**
     * Updates replica configuration with non-null values of command parameters.
     * Used by ReplicaApprove and ReplicaPropsUpdate commands
     */
    protected def updateProps() {
        log.debug("Updating the properties of the server...")
        def replica = ReplicaConfiguration.getCurrentConfig()

        boolean changed = false
        // update the name property
        if (this.params.name) {
            replica.name = this.params.name
            changed = true
        }

        // update the description property
        if (this.params.description) {
            replica.description = this.params.description
            changed = true
        }

        if (changed) {
            log.debug("Flushing the command name and properties...")
            replica.save(flush:true)
        }
    }

    /**
     * Updates the fetch command rate of the replica server.
     */
    def updateFetchRate() {
        log.debug("Updating the fetch rate/interval for the Fetch job...")
        def replica = ReplicaConfiguration.getCurrentConfig()

        boolean changed = false
        // update the command pool rate
        def poolRate = this.params.commandPollPeriod
        if (poolRate && poolRate.toInteger() != replica.commandPollRate) {

            changed = true
            replica.commandPollRate = poolRate.toInteger()

            // reschedule the job with the updated rate
            def jobsAdminService = getService("jobsAdminService")
            try {
                def interval = poolRate.toInteger() * 1000L
                jobsAdminService.rescheduleJob(
                    FetchReplicaCommandsJob.TRIGGER_NAME,
                    FetchReplicaCommandsJob.TRIGGER_GROUP, interval)

            } catch (Exception e) {
                log.error("Tried to reschedule the trigger and nothing happened", e)
                throw new IllegalStateException(e)
            }
        }
        if (changed) {
            log.debug("Flushing the fetch rate/interval...")
            replica.save(flush:true)
        }
    }

    /**
     * Updates the executor pool size. That's actually the number of permits
     * of the long-running and short-running commands semaphores. It creates
     * and instance of the MaxNumberCommandsRunningUpdatedEvent and, without
     * publishing this Spring event, sets it on the scheduler service to be
     * used when no commands are running and the semaphores can be updated.
     */
    def updateExecutorPoolSizes() {
        log.debug("Updating the Executor pool max number of long/short cmds...")
        def replica = ReplicaConfiguration.getCurrentConfig()
        def oldMaxLong = replica.maxLongRunningCmds
        def oldMaxShort = replica.maxShortRunningCmds
        def newMaxLong = oldMaxLong
        def newMaxShort = oldMaxShort

        // update the max number of long-running commands property
        def maxLongRunningCmds = this.params.commandConcurrencyLong
        if (maxLongRunningCmds) {
            newMaxLong = maxLongRunningCmds.toInteger()
            if (newMaxLong != oldMaxLong) {
                replica.maxLongRunningCmds = maxLongRunningCmds.toInteger()
            }
        }
        
        // update the max number of short-running commands property
        def maxShortRunningCmds = this.params.commandConcurrencyShort
        if (maxShortRunningCmds) {
            newMaxShort = maxShortRunningCmds.toInteger()
            if (newMaxShort != oldMaxShort) {
                replica.maxShortRunningCmds = maxShortRunningCmds.toInteger()
            }
        }

        if (oldMaxLong != newMaxLong || oldMaxShort != newMaxShort) {
            log.debug("Flushing the executor pool sizes...")
            replica.save(flush:true)

            def maxChangesEvent = new MaxNumberCommandsRunningUpdatedEvent(this,
                oldMaxLong, newMaxLong, oldMaxShort, newMaxShort)
            def schedulerService = getService("replicaCommandSchedulerService")
            schedulerService.setSemaphoresUpdatedEvent(maxChangesEvent)
        } else {
            log.debug("ReplicaUpdateProps command did not alter either queue size.")
        }
    }
}
