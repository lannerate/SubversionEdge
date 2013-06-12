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
package com.collabnet.svnedge.integration

import grails.test.GrailsUnitTestCase

import com.collabnet.svnedge.domain.integration.*
import com.collabnet.svnedge.integration.command.AbstractCommand
import com.collabnet.svnedge.integration.command.CommandState
import com.collabnet.svnedge.integration.command.CommandsExecutionContext;
import com.collabnet.svnedge.integration.command.ReplicaServerStatusService;
import com.collabnet.svnedge.integration.command.event.CommandAboutToRunEvent;
import com.collabnet.svnedge.integration.command.event.CommandResultReportedEvent;
import com.collabnet.svnedge.integration.command.event.LongRunningCommandQueuedEvent
import com.collabnet.svnedge.integration.command.event.CommandTerminatedEvent
import com.collabnet.svnedge.integration.command.impl.RepoAddCommand;
import static com.collabnet.svnedge.integration.CtfRemoteClientService.COMMAND_ID_PREFIX

import org.codehaus.groovy.grails.commons.ConfigurationHolder;


/**
 * This test case verifies command threading, blocking, etc in the
 * ReplicaCommand processing components. It uses mock command implementation
 * classes for long and short run times, and scans the command execution log
 * to validate expectations
 */
class ReplicaServerStatusServiceTests extends GrailsUnitTestCase {

    def replicaServerStatusService
    def config = ConfigurationHolder.config

    protected void setUp() {
        super.setUp()

        // mock the service and its dependencies
        mockLogging(ReplicaServerStatusService, true)
        replicaServerStatusService = new ReplicaServerStatusService()
    }

    def remotecmdexecs = Collections.synchronizedList(new LinkedList<Map<String, String>>())

    def ReplicaServerStatusServiceTests() {
        remotecmdexecs << [id:'cmdexec1001', repoName:'repo1', code:'repoSync']
        remotecmdexecs << [id:'cmdexec1009', repoName:null, code:'replicaPropsUpdate',
                    params:[until:'2011-01-22']]
        remotecmdexecs << [id:'cmdexec1002', repoName:'repo1', code:'repoSync']
        remotecmdexecs << [id:'cmdexec1006', repoName:'repo2', code:'repoSync']
        remotecmdexecs << [id:'cmdexec1004', repoName:null, code:'replicaPropsUpdate'
                    , params:[name:'Replica Brisbane']]
        remotecmdexecs << [id:'cmdexec1000', repoName:null, code:'replicaApprove',
                    params:[name:'replica title', desc:'super replica']]
        remotecmdexecs << [id:'cmdexec1007', repoName:'repo3', code:'repoSync']
        remotecmdexecs << [id:'cmdexec1008', repoName:null, code:'replicaPropsUpdate'
                    , params:[maxReplicacmdexecs:3, maxRepositorycmdexecs: 10]]
        remotecmdexecs << [id:'cmdexec1005', repoName:'repo2', code:'repoSync']
        remotecmdexecs << [id:'cmdexec1003', repoName:'repo3', code:'repoSync']
    }

    def sortRemoteCommands(commands) {
        def idComparator = [
                    compare: {a,b->
                        (a.id.replace(COMMAND_ID_PREFIX,"") as Integer) -
                                (b.id.replace(COMMAND_ID_PREFIX,"") as Integer)
                    }
                ] as Comparator
        commands.sort(idComparator)
    }

    void testStatusChangeForCommands() {
        def longRunningCommand = new RepoAddCommand()
        longRunningCommand.id = "cmdexec10001" 
        longRunningCommand.repoName = "/tmp/repo1"
        longRunningCommand.state = CommandState.SCHEDULED
        longRunningCommand.context = new CommandsExecutionContext()
        longRunningCommand.context.logsDir = System.getProperty("java.io.tmpdir")

        // calling the event directly
        replicaServerStatusService.onApplicationEvent(new LongRunningCommandQueuedEvent(this, 
                longRunningCommand))

        println "Before getting all scheduled commands: " + longRunningCommand

        // verifying all comments of different states as empty
        for (cmdState in CommandState.values()) {
            if (cmdState == CommandState.SCHEDULED) {
                def scheduledCmds = replicaServerStatusService.getCommands(CommandState.SCHEDULED)
                println "After getting all scheduled commands: " + longRunningCommand
                println "All commands: " + scheduledCmds
                assertNotNull "There should be scheduled commands", scheduledCmds
                assertTrue "There should be 1 scheduled commands", scheduledCmds.size() == 1
                println "MUST be $cmdState at this moment: " + longRunningCommand
                assertTrue ("The test command should be in the state ${cmdState}", 
                    scheduledCmds?.contains(longRunningCommand))
            } else {
                println "MUST NOT be $cmdState at this moment: " + longRunningCommand
                def cmds = replicaServerStatusService.getCommands(cmdState)
                assertFalse ("The test command should not be in the state ${cmdState}", 
                    cmds?.contains(longRunningCommand))
            }
        }

        longRunningCommand.state = CommandState.RUNNING
        // Calling the command directly
        replicaServerStatusService.onApplicationEvent(
            new CommandAboutToRunEvent(this, longRunningCommand))

        println "Before getting all running commands: " + longRunningCommand

        // verifying all comments of different states as empty
        for (cmdState in CommandState.values()) {
            if (cmdState == CommandState.RUNNING) {
                def runningCmds = replicaServerStatusService.getCommands(CommandState.RUNNING)
                println "All commands: " + runningCmds
                println "MUST be $cmdState at this moment: " + longRunningCommand
                assertNotNull "There should be running commands", runningCmds
                assertTrue "There should be 1 running commands", runningCmds.size() == 1
                assertTrue ("The test command should be in the state ${cmdState}", 
                    runningCmds?.contains(longRunningCommand))
            } else {
                println "MUST NOT be $cmdState at this moment: " + longRunningCommand
                def cmds = replicaServerStatusService.getCommands(cmdState)
                assertFalse ("The test command should not be in the state ${cmdState}", 
                    cmds?.contains(longRunningCommand))
            }
        }

        longRunningCommand.state = CommandState.TERMINATED
        longRunningCommand.succeeded = true
        // Calling the command directly
        replicaServerStatusService.onApplicationEvent(
            new CommandTerminatedEvent(this, longRunningCommand))

        println "Before getting all running commands: " + longRunningCommand

        // verifying all comments of different states as empty
        for (cmdState in CommandState.values()) {
            if (cmdState == CommandState.TERMINATED) {
                def terminatedCmds = replicaServerStatusService.getCommands(CommandState.TERMINATED)
                println "All commands: " + terminatedCmds
                println "MUST be $cmdState at this moment: " + longRunningCommand
                assertNotNull "There should be terminated commands", terminatedCmds
                assertTrue "There should be 1 terminated commands", terminatedCmds.size() == 1
                assertTrue ("The test command should be in the state ${cmdState}", 
                    terminatedCmds?.contains(longRunningCommand))
            } else {
                println "MUST NOT be $cmdState at this moment: " + longRunningCommand
                def cmds = replicaServerStatusService.getCommands(cmdState)
                assertFalse ("The test command should not be in the state ${cmdState}", 
                    cmds?.contains(longRunningCommand))
            }
        }

        def cmdResult = new CommandResult()
        cmdResult.commandId = longRunningCommand.id
        cmdResult.commandCode = AbstractCommand.makeCodeName(longRunningCommand)

        // Calling the command directly
        replicaServerStatusService.onApplicationEvent(
            new CommandResultReportedEvent(this, cmdResult))

        // verifying all comments of different states as empty
        for (cmdState in CommandState.values()) {
            println "MUST NOT be $cmdState at this moment: " + longRunningCommand
            def cmds = replicaServerStatusService.getCommands(cmdState)
            assertFalse ("The test command should not be in the state ${cmdState}", 
            cmds?.contains(longRunningCommand))
            assertTrue("The local cache should not have any element.", cmds.size() == 0)
        }
    }
}
