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
package com.collabnet.svnedge.integration.command.handler

import com.collabnet.svnedge.integration.command.AbstractCommand 
import com.collabnet.svnedge.integration.command.CommandState
import com.collabnet.svnedge.integration.command.LongRunningCommand 
import com.collabnet.svnedge.integration.command.ShortRunningCommand 
import com.collabnet.svnedge.integration.command.event.LongRunningCommandQueuedEvent 
import com.collabnet.svnedge.integration.command.event.ShortRunningCommandQueuedEvent 
import com.collabnet.svnedge.util.InterruptibleLoopRunnable;

import java.util.Map
import java.util.concurrent.BlockingQueue

import org.apache.log4j.Logger


/**
 * The Commands Scheduler Handler is responsible for processing the remote 
 * queued commands in an asynchronous fashion using a blocking linked queue.
 * A synchronizer will wait until all retrieved commands are first offered 
 * to the blocking queue for the scheduler to begin to do its job.
 * 
 * An eligible command to run will be selected by verifying if there are
 * no other commands running for the command group (replica server or 
 * repository name). With a selection, the handler will publish either the
 * LongRunningCommandQueuedEvent or ShortRunningCommandQueuedEvent to signal
 * that a command has been queued on the long-running executor queue or on
 * the short-running executor queue.
 * 
 * @author Marcello de Sales (mdesales@collab.net)
 *
 */
class CommandsSchedulerHandler extends InterruptibleLoopRunnable {

    static Logger log = Logger.getLogger(CommandsSchedulerHandler.class)

    /**
     * The scheduler service reference.
     */
    def schedulerService
    /**
     * The queue of received remote commands received from the Replica manager
     * by the Fetch Job and offered by the scheduler's offer method.
     */
    BlockingQueue<Map<String, String>> receivedRemoteQueuedCommands
    /**
     * The synchronizer reference to the binary SynchronousQueue responsible
     * to block the processing of commands until all received commands have
     * been offered (not only one as the blocking method call take).
     */
    BlockingQueue<Boolean> synchronizer

    /**
     * Creates a new scheduler with the given service reference, the
     * queue of commands and the synchronizer.
     * @param service is the scheduler service reference.
     * @param commandsQueue is the blocking queue of received commands.
     * @param syncQueue
     */
    def CommandsSchedulerHandler(service, commandsQueue, syncQueue) {
        schedulerService = service
        receivedRemoteQueuedCommands = commandsQueue
        synchronizer = syncQueue
    }

    @Override
    protected void loop() {
            log.debug "Waiting for queued commands..."
            // block until all remote commands are initially offered
            synchronizer.take()
            log.debug "Initial commands offered... evaluating all of them..."
            boolean isCommandQueued = false
            while(receivedRemoteQueuedCommands.size() > 0) {
                def iterator = receivedRemoteQueuedCommands.iterator()
                while (iterator.hasNext()) {
                    def nextCommand = iterator.next()
                    log.debug "Verifying next command $nextCommand..."
                    def category = schedulerService.getCommandCategory(nextCommand)
                    if (!schedulerService.isCommandNextForCategory(category, nextCommand) ||
                            schedulerService.isThereCommandRunning(category)) {

                        // can't schedule this command
                        continue
                    }
                    log.debug "Command eligible to be scheduled: $nextCommand"
                    def classLoader = getClass().getClassLoader()
                    def commandInstance = AbstractCommand.makeCommand(
                        classLoader, nextCommand)
                    commandInstance.makeTransitionToState(CommandState.SCHEDULED)
                    synchronized(receivedRemoteQueuedCommands) {
                        iterator.remove()
                    }
                    // remove the command map from the scheduled command
                    schedulerService.scheduleCommandForExecution(category, nextCommand)
                    // make command instance from map
                    switch(commandInstance) {
                        case(LongRunningCommand):
                            log.debug "Scheduling long-running command: " +
                                "$commandInstance"
                            schedulerService.publishEvent(
                                new LongRunningCommandQueuedEvent(this, commandInstance))
                            break

                        case(ShortRunningCommand):
                            log.debug "Scheduling short-running command: " +
                                "$commandInstance"
                            schedulerService.publishEvent(
                                new ShortRunningCommandQueuedEvent(this, commandInstance))
                            break
                    }

                    commandInstance.logExecution("QUEUED")

                    // start evaluating the commands from the beginning
                    isCommandQueued = true
                    break
                }
                if (isCommandQueued) {
                    isCommandQueued = false
                } else {
                    Thread.sleep(5000)
                }
            }
    }
}
