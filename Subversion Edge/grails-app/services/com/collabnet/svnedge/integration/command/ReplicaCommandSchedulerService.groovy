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
package com.collabnet.svnedge.integration.command


import grails.util.GrailsUtil

import java.util.Collections

import java.util.concurrent.BlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.SynchronousQueue

import org.springframework.context.ApplicationContextAware
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationListener

import com.collabnet.svnedge.console.AbstractSvnEdgeService
import com.collabnet.svnedge.integration.command.event.AppliedExecutorSemaphoresUpdateEvent 
import com.collabnet.svnedge.integration.command.event.CommandTerminatedEvent 
import com.collabnet.svnedge.integration.command.event.MaxNumberCommandsRunningUpdatedEvent 
import com.collabnet.svnedge.integration.command.event.UpdateSemaphoresEvent 
import com.collabnet.svnedge.integration.command.event.ReplicaCommandsExecutionEvent 
import com.collabnet.svnedge.integration.command.handler.CommandsSchedulerHandler 
import static com.collabnet.svnedge.integration.CtfRemoteClientService.COMMAND_ID_PREFIX

/**
 * The ReplicaCommandSchedulerService provides a scheduler responsible for:
 * <ul><li>Maintaining a blocking queue of received tasks (replica commands) 
 * to be executed;
 * <li>Generating a Priority Queue for each of the group of commands: 
 * commands for each repository or for the replica server.
 * <li>Selecting which command from the queue can be executed based on the type
 * of the command and the capacity of the executor.</li><ul>
 *
 * Each group can only have one command being executed at the same time and,
 * therefore, a mutex map maintains such "flag" for each category.
 *
 * @author Marcello de Sales (mdesales@collab.net)
 */
class ReplicaCommandSchedulerService extends AbstractSvnEdgeService 
        implements ApplicationContextAware, ApplicationListener<ReplicaCommandsExecutionEvent> {

    def commandResultDeliveryService

    ApplicationContext applicationContext
    /**
     * The command category for the replica server.
     */
    private static final String REPLICA_COMMAND_CATEGORY = "replicaServer"
    /**
     * To avoid spring events to be fired more than once.
     */
    static transactional = false
    /**
     * This is the list of commands received from the master.
     * [cmd[id:"cmd1001", code:"addRepo"], ...]
     */
    BlockingQueue<Map<String, String>> queuedCommands
    /**
     * The execution mutex is a map [category, commandID]
     */
    Map<String, String> executionMutex
    /**
     * The command ID index to speed up the offer method.
     */
    Set<String> commandIdIndex
    /**
     * The scheduler executor synchronizer
     */
    BlockingQueue<Boolean> schedulerSynchronizer
    /**
     * Binary latch used to update the instance semaphore once the number of
     * permits have been updated.
     */
    CountDownLatch updatedSemaphoresGate
    /**
     * The event triggered with the new instances of the semaphores.
     */
    MaxNumberCommandsRunningUpdatedEvent semaphoreUpdatedEvent

    /**
     * Bootstraps the service
     */
    def bootStrap = {
        log.info("Bootstrapping the replica command scheduler")
        cleanCommands()
        schedulerSynchronizer = new SynchronousQueue<Boolean>()
        if (GrailsUtil.environment != "test") {
            startBackgroundHandlers()
        }
    }

    def startBackgroundHandlers() {
        // execute the command using the background service.
        runAsync(new CommandsSchedulerHandler(this,
            queuedCommands, schedulerSynchronizer))
    }

    /**
     * Initializes all the data structures for the queued commands.
     */
    void cleanCommands() {
        if (queuedCommands == null) {
            queuedCommands = new LinkedBlockingQueue<Map<String, String>>()
        } else {
            while (queuedCommands.poll()) {
                // emptying queue
            }
        }
        executionMutex = Collections.synchronizedMap(
            new LinkedHashMap<String, String>())
        commandIdIndex = Collections.synchronizedSet(
            new LinkedHashSet<String>())
    }

    /**
     * Offer new commands to the current queue.
     * @param remoteCommandsMaps the set of commands received from the remote
     * master.
     * @param executionContext is the execution context with all the information
     * related to the communication with the Replica manager (TeamForge server).
     */
    def offer(remoteCommandsMaps, executionContext) {
        if (!remoteCommandsMaps || remoteCommandsMaps == []) {
            return
        }
        log.debug "New commands offered: $remoteCommandsMaps"
        log.debug "Commands Execution Context: $executionContext"

        // register new commands and remove the existing ones.
        commandResultDeliveryService.registerClearExistingCommands(
            remoteCommandsMaps)

        for (commandMap in remoteCommandsMaps) {
            if (!commandIdIndex.contains(commandMap.id)) {
                try {
                    commandMap["context"] = executionContext
                    log.debug "Queueing new command $commandMap"
                    queuedCommands.offer(commandMap)
                    commandIdIndex << commandMap.id

                } catch (Exception invalidCommand) {
                    log.error("The remote command $commandMap is invalid.",
                        invalidCommand)
                }
            } else {
                executionContext.activeCommands.decrementAndGet()
            }
        }
        // semaphores updating in the executor service from previous offer...
        // waiting until the semaphores are completely changed...
        // See the AppliedExecutorSemaphoresUpdateEvent handler
        if (updatedSemaphoresGate) {
            log.debug("Permits updated... Waiting for the updated " +
                "semaphore...")
            updatedSemaphoresGate.await()
            semaphoresWereUpdated()
        }
        // synchronize with the scheduler as new commands were offered.
        schedulerSynchronizer.offer(new Boolean(true))
    }

    /**
     * Setting up the event that captures the number of permits for the
     * semaphores.
     * @param semaphoresUpdated is an instance of the event
     * MaxNumberCommandsRunningUpdatedEvent with the changed values.
     */
    def setSemaphoresUpdatedEvent(semaphoresUpdated) {
        if (semaphoresUpdated.newMaxLongRunningCmds != semaphoresUpdated.oldMaxLongRunningCmds ||
                semaphoresUpdated.newMaxShortRunningCmds != semaphoresUpdated.oldMaxShortRunningCmds) {
            log.debug("MaxNumberCommandsRunningUpdatedEvent: Closing " +
                "the gate (count-down latch) until semaphores are updated.")
            semaphoreUpdatedEvent = semaphoresUpdated
            updatedSemaphoresGate = new CountDownLatch(1)
            log.debug "The updated semaphore gate is active. Publish UpdateSemaphoresEvent"
            publishEvent(new UpdateSemaphoresEvent(this, semaphoreUpdatedEvent))

        } else {
            log.debug("semaphoresUpdated event did not alter the queue sizes, so leaving current" +
                      " semaphores in place")
        }
    }

    /**
     * @return if the semaphore's permits have been changed through the command
     * that changed the max number of long-running and short-running commands.
     */
    def hasSemaphoresUpdated() {
        return semaphoreUpdatedEvent != null
    }

    /**
     * @return the instance of the MaxNumberCommandsRunningUpdatedEvent holding
     * the old and new values of the permits for the semaphores.
     */
    def getSemaphoresUpdatedEvent() {
        return semaphoreUpdatedEvent
    }

    /**
     * Removing the references to the updated semaphore gate and the
     * semaphore updated event.
     */
    def semaphoresWereUpdated() {
        updatedSemaphoresGate = null
        semaphoreUpdatedEvent = null
    }

    /**
     * @return the number of commands queued, not yet executing.
     */
    def getQueuedCommandsSize() {
        return queuedCommands.size()
    }

    /**
     * @return the number of categories of commands executing.
     */
    def getExecutingCommandsSize() {
        return executionMutex.size()
    }

    /**
     * Schedules the given command for execution.
     * @param category the name of the category or "replicaServer"
     * @param command is the command map with id, code, etc.
     */
    def synchronized void scheduleCommandForExecution(category, command) {
        addCommandToCategoryMutex(category, command.id)
    }

    /**
     * Adds a mutex for the commandID in the given category.
     * @param category is the name of the repo or "replicaServer"
     * @param commandId is the ID of the command.
     */
    def synchronized void addCommandToCategoryMutex(category, commandId) {
        if (!category) {
            throw new IllegalArgumentException("The category must be provided")
        }
        if (!commandId) {
            throw new IllegalArgumentException("The category must be provided")
        }
        synchronized(executionMutex) {
            executionMutex[category] = commandId
        }
    }

    /**
     * @param category is the name of the category or 
     * @return if there is a command running in the given category.
     */
    def synchronized boolean isThereCommandRunning(category) {
        if (!category) {
            throw new IllegalArgumentException("The category must be provided")
        }
        return executionMutex[category] != null
    }

    /**
     * Remove terminated command from the mutex map.
     * @param finishedCommandId the ID of the command.
     */
    def synchronized void removeTerminatedCommand(finishedCommandId) {
        if (!finishedCommandId) {
            throw new IllegalArgumentException("The finished command ID must " +
                "be provided.")
        }
        // removes from the category mutex
        synchronized (executionMutex) {
            def categoryKey = null
            // execution mutex = [category: commandId]
            for (index in executionMutex){
                if (index.value.equals(finishedCommandId)) {
                    categoryKey = index.key
                    break
                }
            }
            if (categoryKey) {
                executionMutex.remove(categoryKey)
            }
        }
        // removes the command ID from the index.
        synchronized(commandIdIndex) {
            commandIdIndex.remove(finishedCommandId)
        }
    }

    /**
     * @param cmd a given command object.
     * @return the name of the repository name or the constant 
     * REPLICA_COMMAND_CATEGORY.
     */
    def getCommandCategory(cmd) {
        return !cmd.repoName ? REPLICA_COMMAND_CATEGORY : cmd.repoName
    }

    /**
     * @param category the repository name.
     * @param command the command object.
     * @return if the given command is the next to be executed for the given
     * category. That is, if no other command in the given category is being
     * executed.
     */
    def synchronized isCommandNextForCategory(category, command) {
        return getNextCommandFromCategory(category).id == command.id
    }

    /**
     * @return the groups of commands by the repository name or by the constant 
     * REPLICA_COMMAND_CATEGORY.
     */
    def synchronized getCategorizedCommandQueues() {
        // map grouping the tasks by granularity (repoName or replica server)
        return queuedCommands.groupBy{ cmd -> getCommandCategory(cmd) }
    }

    /**
     * @return an instance of Comparator that compares attribute "id" from 
     * the elements of a list/queue of maps.
     */
    def makeQueuedCommandsIdComparator() {
        return [
            compare: {a,b-> 
                (a.id.replace(COMMAND_ID_PREFIX,"") as Integer) -
                    (b.id.replace(COMMAND_ID_PREFIX,"") as Integer)
            }
          ] as Comparator
    }

    /**
     * @param category is the name of a repository or "replicaServer".
     * @return the next command to be executed from queue of commands in the
     * given category. The queue of commands is sorted by the id.
     */
    def synchronized getNextCommandFromCategory(category) {
        if (!category) {
            throw new IllegalArgumentException("The category must be provided.")
        }
        // map grouping the tasks by granularity (repoName or replica server)
        def categorizedQueuedCommands = getCategorizedCommandQueues()
        // compares the value of the keys "id" from the list of maps
        // removing the prefix.
        def idComparator = makeQueuedCommandsIdComparator()
        if (!categorizedQueuedCommands[category]) {
            throw new IllegalArgumentException("The category $category " +
                "does not exist")
        }
        // returns the head of the category queue
        return categorizedQueuedCommands[category].sort(idComparator)[0]
    }

    /**
     * Handles the spring events related to the ApplicationListener.
     * @param executionEvent is the instance of an execution event.
     */
    void onApplicationEvent(ReplicaCommandsExecutionEvent executionEvent) {
        switch(executionEvent) {
            case CommandTerminatedEvent:
                def terminatedCommand = executionEvent.terminatedCommand
                log.debug("CommandTerminatedEvent: $terminatedCommand")
                removeTerminatedCommand(terminatedCommand.id)
                break

            case AppliedExecutorSemaphoresUpdateEvent:
                log.debug("AppliedExecutorSemaphoresUpdateEvent: semaphores " +
                    "were updated... Open the gate...")
                if (updatedSemaphoresGate) {
                    updatedSemaphoresGate.countDown()
                    semaphoresWereUpdated()
                }
                break
        }
    }
    
    List<Map<String, String>> listUnprocessedCommands() {
        List<Map<String, String>> cmds = new ArrayList<Map<String, String>>()
        if (queuedCommands) {
            synchronized (queuedCommands) {
                cmds.addAll(queuedCommands)
            }
        }
        return cmds
    }
}
