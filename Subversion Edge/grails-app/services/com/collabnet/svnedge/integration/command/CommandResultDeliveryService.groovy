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

import java.util.concurrent.atomic.AtomicInteger;

import com.collabnet.svnedge.console.AbstractSvnEdgeService
import com.collabnet.svnedge.domain.integration.CommandResult 
import com.collabnet.svnedge.domain.integration.CtfServer
import com.collabnet.svnedge.integration.command.CommandsExecutionContext
import com.collabnet.svnedge.integration.command.event.CommandResultReportedEvent;
import com.collabnet.svnedge.integration.command.event.CommandTerminatedEvent
import com.collabnet.svnedge.integration.command.event.ConnectivityWithReplicaManagerRestoredEvent
import com.collabnet.svnedge.integration.command.event.ReplicaCommandsExecutionEvent
import com.collabnet.svnedge.integration.RemoteMasterException
import com.collabnet.svnedge.integration.ReplicationService

import org.springframework.context.ApplicationListener

/**
 * The ReplicaCommandResultDeliveryService is responsible delivering the
 * executed commands to the Replica manager (CTF server).
 * 
 * @author Marcello de Sales (mdesales@collab.net)
 */
public class CommandResultDeliveryService extends AbstractSvnEdgeService 
        implements ApplicationListener<ReplicaCommandsExecutionEvent> {

    static transactional = false

    def ctfRemoteClientService
    def securityService

    /**
     * The delivery synchronizer, only to be open when there is connection to
     * the replica manager.
     */
    def connectivityWithRemoteManagerOpen = true

    /**
     * Bootstraps the service
     */
    def bootStrap = {
        log.info("Bootstrapping the command result delivery service")
        // remove commands which did not finish, but will not be running now,
        // so that they can be resent from TF.
        def commands = CommandResult.findAllWhere(succeeded: null)
        for (def cmd : commands) {
            cmd.delete()
        }
    }

    /**
     * Signals the service that the communication with the Replica manager has
     * been restored in case.
     * @param context is the execution context for the commands.
     */
    def synchronized restartDelivering(CommandsExecutionContext context) {
        if (!connectivityWithRemoteManagerOpen) {
            connectivityWithRemoteManagerOpen = true
            publishEvent(new ConnectivityWithReplicaManagerRestoredEvent(this,
                context))
        }
    }

    /**
     * Signals the service that there is no communication with the replica
     * manager. It is used to simply simply lock the delivery of results.
     */
    def synchronized stopDelivering() {
        connectivityWithRemoteManagerOpen = false
    }

    /**
     * Persist the command result before trying submitting the results.
     * @param terminatedCommand the instance of {@link AbstractCommand}
     * that has terminated.
     * @return The {@link CommandResult} instance for the command.
     */
    def makePersistedCommandResult(command) {
        if (!command || !command instanceof AbstractCommand) {
            throw new IllegalArgumentException("The command must be provided")
        }
        def cmdResult = CommandResult.findByCommandId(command.id)
        if (!cmdResult) {
            cmdResult = new CommandResult()
            cmdResult.commandId = command.id
            cmdResult.commandCode = AbstractCommand.makeCodeName(command)
            // As Hibernate typically batches up SQL statements and executes
            // them at the end of the session, we need to flush in place.
            cmdResult.save(flush:true)
        }
        return cmdResult
    }

    /**
     * Updates the reference to the queuedCommands and removes the existing
     * entries. That is, if an entry has been already queued to be processed,
     * the command result is already persisted and, for this reason, the command
     * is removed from the received list.
     * @param remoteQueuedCommands is the received commands
     */
    def synchronized registerClearExistingCommands(remoteQueuedCommands) {
        def iterator = remoteQueuedCommands.iterator()
        while (iterator.hasNext()) {
            def remoteCommand = iterator.next()
            if (!CommandResult.findByCommandId(remoteCommand.id)) {
                // create the command result if it does not exist.
               makePersistedCommandResult(remoteCommand)

            } else {
                // Discard the command as it already exists in the report
                // as tne method is synchronized, changes can be made.
                iterator.remove()
            }
        }
        // removed from reference.
    }

    /**
     * @return the current command results that haven't been transmitted. That
     * means, all of the results that are still either scheduled or executing.
     */
    def synchronized getUnacknowledgedExecutingCommandResults() {
        return CommandResult.findAllWhere(transmitted: false)
    }

    /**
     * The event handler of all {@link ReplicaCommandsExecutionEvent} to 
     * process the different events.
     * @param executionEvent is the instance of an execution event.
     */
    void onApplicationEvent(ReplicaCommandsExecutionEvent executionEvent) {
        switch(executionEvent) {
            case CommandTerminatedEvent:
                def terminatedCommand = executionEvent.terminatedCommand
                log.debug "CommandTerminatedEvent: ${terminatedCommand}"
                // save the result before attempting to deliver to remote manager
                def commandResult = makePersistedCommandResult(terminatedCommand)
                saveCommandResult(commandResult, terminatedCommand.succeeded)
                synchronized(this) {
                    if (connectivityWithRemoteManagerOpen) {
                        // Report the results in parallel
                        runAsync {
                            log.debug("Executing Report ${terminatedCommand}")
                            reportTerminatedCommandResult(
                                terminatedCommand.context, commandResult)
                            log.debug("Done executing Report ${terminatedCommand}")
                        }
                    } else {
                        log.debug "Since there is no connectivity, wait " +
                            "until it is restored to continue."
                    }
                }
                break

            case ConnectivityWithReplicaManagerRestoredEvent:
                log.debug("ConnectivityWithReplicaManagerRestoredEvent: " +
                    "restored connectivity")
                connectivityWithRemoteManagerOpen = true
                // Report the results in parallel
                def context = executionEvent.executionContext
                runAsync {
                    log.debug("Executing Reporting Pending Command Results")
                    reportPendingResultsAfterConnectivityRestored(context)
                    log.debug("Done executing Reporting Pending Command Results")
                }
                break
        }
    }

    /**
     * Reports all the pending command results in parallel, if any exists, 
     * during their delivery.
     * @param executionContext is the execution context of the commands.
     */
    private def reportPendingResultsAfterConnectivityRestored(executionContext) {
        def commandResults = CommandResult.findAll(
            "from CommandResult as r where r.transmitted=false")
        if (commandResults && commandResults.size() > 0) {
            executionContext.activeCommands = new AtomicInteger(commandResults.size())
            // Report all the command results in parallel
            log.debug("There are ${commandResults.size()} command " +
                "result(s) to be delivered...")
            commandResults.each { cmdResult ->
                reportTerminatedCommandResult(executionContext, cmdResult)
            }
        } else {
            log.debug("There are no command results to be delivered...")
        }
    }

    /**
     * Reports the termination of a command back to the CTF Replica Manager
     * @param commandContext is the context of the command execution.
     * @param commandResult is the instance a Command Result.
     * @param result is the result that determines whether the command succeeded
     * or failed.
     */
    def reportTerminatedCommandResult(commandContext, commandResult) {
        try {
            def succeeded = commandResult.succeeded ? "SUCEEDED" : "FAILED"
            if (ReplicationService.isCtfCommand(commandResult.commandId)) {
                log.debug("Attempting to deliver the command result " +
                        "${commandResult.commandId} -> ${succeeded}.")
                // attempt to deliver the result to the Replica manager
                transmitCommandResult(commandContext, commandResult)
                log.debug("Result successfully acknowledged the command " +
                    "${commandResult.commandId} -> ${succeeded}.")
            }

            publishEvent(new CommandResultReportedEvent(this, commandResult))

            // remove the command result as it has been transmitted.
            deleteTransmittedResults(commandResult)

        } catch (Exception remoteError) {
            log.error("Error while acknowledging the command " +
                "${commandResult.commandId}: " + remoteError.getMessage())
            stopDelivering()
        }
    }

    /**
     * Saves the result of the command.
     * @param commandId is the ID of the command.
     * @param result is the succeeded result
     */
    def saveCommandResult(CommandResult commandResult, succeededResult) {
        if (!commandResult) {
            throw new IllegalArgumentException("The command result instance " +
                "must be provided must be saved")
        }
        commandResult.succeeded = succeededResult
        commandResult = commandResult.merge()
        commandResult.save(flush:true)
    }

    /**
     * Transmits the given succeeded result related the given commandId using
     * the given commands execution context.
     * In case there is any exception while trying to connect with the manager
     * server, the event UnableToDeliverCommandResultEvent is published.
     * @param execContext is the commands execution context with the connection
     * with the Replica manager (CTF server).
     * @param cmdResult is the instance of the command result.
     * @throws RemoteMasterException if any problem while communicating with the
     * remote replica manager occurs.
     */
    def transmitCommandResult(execContext, cmdResult) 
            throws RemoteMasterException {

        def ctfServer = CtfServer.getServer()
        def soapId = execContext.soapSessionId
        if (execContext.userSessionId) {
            try { 
                ctfRemoteClientService.uploadCommandResult(ctfServer.baseUrl,
                    execContext.userSessionId, execContext.replicaSystemId,
                    cmdResult.commandId, cmdResult.succeeded, execContext.locale)
            } catch (Exception cantConnectCtfMaster) {
                execContext.userSessionId = null
                log.info "Could not deliver command result using existing session " +
                    " will try logging in to ${ctfServer.baseUrl}: " + 
                    cantConnectCtfMaster.getMessage()
                try {
                    ctfRemoteClientService.logoff(ctfServer.baseUrl,
                        ctfServer.ctfUsername, soapId, true)
                    soapId = null
                } catch (Exception ignore) {
                    log.debug("Exception trying to logoff soap session", ignore)
                }
                loginAndTransmitCommandResult(execContext, cmdResult, ctfServer)
            } finally {
                int activeCommands = execContext.activeCommands.decrementAndGet()
                if (activeCommands == 0) {
                    log.debug("Last command finished execution, logging off soap session")
                    execContext.userSessionId = null
                    if (soapId) {
                        ctfRemoteClientService.logoff(ctfServer.baseUrl,
                            ctfServer.ctfUsername, soapId, true)
                    }
                } else {
                    log.debug(activeCommands + 
                        " commands left to complete using the same context.")
                }
            }
        } else {
            loginAndTransmitCommandResult(execContext, cmdResult, ctfServer)
        }
    }

    private void loginAndTransmitCommandResult(execContext, cmdResult, ctfServer) {
        def ctfPassword = securityService.decrypt(ctfServer.ctfPassword)        
        def soapId
        try {
            soapId = ctfRemoteClientService.login(ctfServer.baseUrl,
                ctfServer.ctfUsername, ctfPassword, execContext.locale)
            def sessionId = ctfRemoteClientService.cnSoap(ctfServer.baseUrl)
                .getUserSessionBySoapId(soapId)
            //upload the commands results back to ctf
            ctfRemoteClientService.uploadCommandResult(ctfServer.baseUrl,
                sessionId, execContext.replicaSystemId, 
                cmdResult.commandId, cmdResult.succeeded, execContext.locale)

        } catch (Exception cantConnectCtfMaster) {
            log.error("Can't deliver command results from the CTF replica " +
                    "manager ${ctfServer.baseUrl}", cantConnectCtfMaster)
            stopDelivering()
            return

        } finally {
            if (soapId) {
                ctfRemoteClientService.logoff(ctfServer.baseUrl,
                    ctfServer.ctfUsername, soapId)
            }
        }
    }

    /**
     * Sets the transmitted value to true.
     * @param cmdResult is the command result object.
     */
    def deleteTransmittedResults(CommandResult cmdResult) {
        if (!cmdResult) {
            throw new IllegalArgumentException("The command result instance " +
                "must be provided must be saved")
        }
        cmdResult = cmdResult.merge()
        cmdResult.delete()
    }
}
