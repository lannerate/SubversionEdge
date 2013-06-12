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

import grails.util.GrailsUtil;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.cometd.Client;
import org.mortbay.cometd.ChannelImpl;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationListener;

import com.collabnet.svnedge.console.AbstractSvnEdgeService;
import com.collabnet.svnedge.integration.command.AbstractCommand;
import com.collabnet.svnedge.integration.command.CommandState;
import com.collabnet.svnedge.integration.command.LongRunningCommand;
import com.collabnet.svnedge.integration.command.ShortRunningCommand;
import com.collabnet.svnedge.integration.command.event.CommandAboutToRunEvent;
import com.collabnet.svnedge.integration.command.event.CommandTerminatedEvent;
import com.collabnet.svnedge.integration.command.event.CommandResultReportedEvent;
import com.collabnet.svnedge.integration.command.event.LongRunningCommandQueuedEvent;
import com.collabnet.svnedge.integration.command.event.ReplicaCommandsExecutionEvent;
import com.collabnet.svnedge.integration.command.event.ShortRunningCommandQueuedEvent;

/**
 * This service is responsible for maintaining the current status of the 
 * commands being executed. It will use Cometd to push the information on 
 * each command status change.
 * 
 * @author Marcello de Sales (mdesales@collab.net)
 *
 */
public final class ReplicaServerStatusService extends AbstractSvnEdgeService
        implements ApplicationListener<ReplicaCommandsExecutionEvent> {

    boolean transactional = false

    def executorService

    /**
     * The current set of commands by the state.
     */
    private ConcurrentMap<CommandState, Set<AbstractCommand>> commandsByStateIndex
    /**
     * All the commands running with their associated state.
     */
    private ConcurrentMap<AbstractCommand, CommandState> allCommands
    /**
     * The index of long-running commands
     */
    private Set<LongRunningCommand> allLongRunningIndexSet
    /**
     * The index of short-running commands
     */
    private Set<ShortRunningCommand> allShortRunningIndexSet

    public ReplicaServerStatusService() {
        commandsByStateIndex = new ConcurrentHashMap<CommandState, 
            Set<AbstractCommand>>()
        allCommands = new ConcurrentHashMap<AbstractCommand, 
            CommandState>()
        allLongRunningIndexSet = Collections.synchronizedSet(
            new HashSet<LongRunningCommand>())
        allShortRunningIndexSet = Collections.synchronizedSet(
            new HashSet<ShortRunningCommand>())
    }

    /**
     * @return whether there are commands in either state (
     * CommandState.SCHEDULED, RUNNING, TERMINATED, REPORTED...)
     */
    public boolean areThereAnyCommands() {
        return this.allCommands.size() > 0
    }

    /**
     * @param state is the state of a command.
     * @return whether there are commands in the given state.
     */
    public boolean areThereAnyCommands(CommandState state) {
        def cmdsInState = this.commandsByStateIndex.get(state)
        return cmdsInState && cmdsInState.size() > 0
    }

    /**
     * @param commandId is the command Id.
     * @return a given command with the given ID.
     */
    public AbstractCommand getCommand(String commandId) {
        for(command in allCommands.keySet()) {
            if (command.id.equals(commandId)) {
                return command
            }
        }
        return null
    }

    /**
     * @param state the current state.
     * @return the set of commands on the given state.
     */
    public Set<AbstractCommand> getCommands(CommandState state) {
        Set<AbstractCommand> commandsInState = this.commandsByStateIndex.get(state)
        Set<AbstractCommand> result = new LinkedHashSet<AbstractCommand>()
        if (!commandsInState || commandsInState.size() == 0) {
            return result
        }
        // as a concurrent hash set.
        synchronized (commandsByStateIndex) {
            Iterator<AbstractCommand> iter = commandsInState.iterator()
            while(iter.hasNext()) {
                result.add(iter.next())
            }
        }
        return result
    }

    /**
     * The getCommands for the state
     * @param state is the set of commands.
     * @return the commands for the given set of state.
     */
    public Set<AbstractCommand> getCommands(CommandState ... state) {
        if (state != null && state.length > 0) {
            Set<AbstractCommand> cmds = new LinkedHashSet<AbstractCommand>()
            for (CommandState commandState : state) {
                cmds.addAll(getCommands(commandState))
            }
            return cmds

        } else {
            return new LinkedHashSet<AbstractCommand>()
        }
    }

    /**
     * @return The total size of commands.
     */
    public int getAllCommandsSize() {
        return this.allCommands.size()
    }

    /**
     * @param commandType Is one of the marker interfaces 
     * {@link LongRunningCommand} or {@link ShortRunningCommand}.
     * @return All the current commands for the given type.
     */
    public Set<AbstractCommand> getCommandsByType(commandType) {
        Set<AbstractCommand> all = new LinkedHashSet<AbstractCommand>()
        if (commandType == LongRunningCommand.class) {
            synchronized (allLongRunningIndexSet) {
                for (cmd in allLongRunningIndexSet) {
                    all << cmd
                }
            }
        } else if (commandType == ShortRunningCommand.class) {
            synchronized (allShortRunningIndexSet) {
                for (cmd in allShortRunningIndexSet) {
                    all << cmd
                }
            }
        }
        return all
    }

    /**
     * @return All the current commands running in all states.
     */
    public Set<AbstractCommand> getAllCommands() {
        Set<AbstractCommand> all = new LinkedHashSet<AbstractCommand>()
        for (state in CommandState.values()) {
            all.addAll(getCommands(state))
        }
        return all
    }

     /**
      * Updates the command Maps for the commands.
      * @param command is the command to be executed.
      * @param state is the state of the command.
      */
    private void updateOrRemoveCommandState(AbstractCommand command, 
            CommandState state) {

        if (!command) {
            throw new IllegalArgumentException("The command must be provided.")

        } else if (!state) {
            throw new IllegalArgumentException("The state must be provided.")
        }
        if (state == CommandState.REPORTED) {
            def commandState = allCommands.remove(command)

            if (command instanceof LongRunningCommand) {
                allLongRunningIndexSet.remove(command)

            } else if (command instanceof ShortRunningCommand) {
                allShortRunningIndexSet.remove(command)
            }

            if (commandState) {
                Set<AbstractCommand> commands = 
                        commandsByStateIndex.get(commandState)
                if (commands && commands.size() > 0) {
                    synchronized (commands) {
                        Iterator<AbstractCommand> iter = commands.iterator()
                        while (iter.hasNext()) {
                            def cmd = iter.next()
                            if (cmd.id == command.id) {
                                iter.remove()
                                break
                            }
                        }
                    }
                }
            } else {
                log.warn("Processing CommandResultReportedEvent for command, " +
                    command.id + ", which is not found in a prior state.")
            }

        } else {
            // override the state getting the previous value
            def previousState = allCommands.put(command, state)

            if (command instanceof LongRunningCommand) {
                allLongRunningIndexSet << command

            } else if (command instanceof ShortRunningCommand) {
                allShortRunningIndexSet << command
            }

            // command had not been registered before
            if (!previousState) {
                Set<AbstractCommand> commands = commandsByStateIndex.get(state)
                if (!commands) {
                    Set<AbstractCommand> newSet = Collections.synchronizedSet(
                        new LinkedHashSet<AbstractCommand>())
                    commands = commandsByStateIndex.putIfAbsent(state, newSet)
                    if (!commands) {
                        commands = newSet
                    }
                }
                commands.add(command)

            } else {
                // remove from the previous state
                Set<AbstractCommand> commands = commandsByStateIndex.get(
                    previousState)
                if (commands && commands.size() > 0) {
                    synchronized (commands) {
                        Iterator<AbstractCommand> iter = commands.iterator()
                        while (iter.hasNext()) {
                            def cmd = iter.next()
                            if (cmd == command) {
                                iter.remove()
                                break
                            }
                        }
                    }
                }
                Set<AbstractCommand> newStateCmds = commandsByStateIndex.get(state)
                if (!newStateCmds) {
                    Set<AbstractCommand> newSet = Collections.synchronizedSet(
                        new LinkedHashSet<AbstractCommand>())
                    newStateCmds = commandsByStateIndex.putIfAbsent(state, newSet)
                    if (!newStateCmds) {
                        newStateCmds = newSet
                    }
                }
                newStateCmds.add(command)
            }
        }
    }

    /**
     * The event handler of all {@link ReplicaCommandsExecutionEvent} to 
     * process the different events.
     * @param executionEvent is the instance of an execution event.
     */
    void onApplicationEvent(ReplicaCommandsExecutionEvent executionEvent) {
        switch(executionEvent) {
            case LongRunningCommandQueuedEvent:
            case ShortRunningCommandQueuedEvent:
                def scheduledCommand = executionEvent.queuedCommand
                log.debug "Command scheduled: $scheduledCommand"
                def state = CommandState.SCHEDULED
                updateOrRemoveCommandState(scheduledCommand, state)
                break;

            case CommandAboutToRunEvent:
                def commandToExecute = executionEvent.commandToExecute
                log.debug "Command executing: $commandToExecute"
                def state = CommandState.RUNNING
                updateOrRemoveCommandState(commandToExecute, state)
                break

            case CommandTerminatedEvent:
                def terminatedCommand = executionEvent.terminatedCommand
                log.debug "Command terminated: ${terminatedCommand}"
                def state = CommandState.TERMINATED
                updateOrRemoveCommandState(terminatedCommand, state)
                break

            case CommandResultReportedEvent:
                def cmdResult = executionEvent.commandResult
                def state = CommandState.REPORTED
                def reportedCommand = AbstractCommand.makeCommand(cmdResult,
                    state)
                log.debug "Command reported: ${reportedCommand}"
                reportedCommand.state = state
                updateOrRemoveCommandState(reportedCommand, state)
                break
        }
    }

}
