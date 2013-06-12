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
package com.collabnet.svnedge.replication.command

import java.io.File;

import org.codehaus.groovy.grails.commons.ConfigurationHolder;

import com.collabnet.svnedge.domain.integration.CommandResult 
import com.collabnet.svnedge.integration.command.AbstractCommand;
import com.collabnet.svnedge.integration.command.CommandsExecutionContext;
import com.collabnet.svnedge.integration.command.impl.ReplicaApproveCommand;
import com.collabnet.svnedge.integration.command.impl.ReplicaPropsUpdateCommand;
import com.collabnet.svnedge.integration.command.impl.RepoSyncCommand;

import grails.test.GrailsUnitTestCase;

import static com.collabnet.svnedge.integration.CtfRemoteClientService.COMMAND_ID_PREFIX

class AbstractCommandIntegrationTests extends GrailsUnitTestCase {

    def grailsApplication
    def config

    protected void setUp() {
        this.config = grailsApplication.config
    }

    void testGetExecutionLogFile() {
        def logName = "replica_cmds_" + String.format('%tY_%<tm_%<td', 
            new Date()) + ".log"
        def logsDir = config.svnedge.logsDirPath + ""

        CommandsExecutionContext ctxt = new CommandsExecutionContext()
        ctxt.logsDir = new File(logsDir)

        def executionLogFile = AbstractCommand.getExecutionLogFile(ctxt)
        assertNotNull "The log file must not be null", executionLogFile

        def logFile = new File(logsDir, logName)
        println "Log file name: " + logFile
        assertEquals "The log file name must be in the logs dir file", 
            executionLogFile.toString(), logFile.toString()

        ctxt.logsDir = null
        assertNull "The log file name must be null with null dir", 
            AbstractCommand.getExecutionLogFile(ctxt)
    }

    void testMakeRepoCommand() {
        def cmdMap = [id:CommandTestsHelper.createCommandId(), code:'repoSync',
            params:[repoName:'/repos/repo1']]
        def ctxt = new CommandsExecutionContext()
        cmdMap.context = ctxt

        def cmdInstance = AbstractCommand.makeCommand(
            this.getClass().getClassLoader(), cmdMap)
        assertNotNull cmdInstance

        def sufix = "Command"
        def commandClassName = cmdMap.code.capitalize() + sufix
        assertEquals "The class name must be the capitalized code plus " +
            "'Command'", commandClassName, cmdInstance.class.getSimpleName()

        assertTrue "AbstractCommand factory must transform the command map " +
            "$cmdMap into an instance of RepoSyncCommand", 
            cmdInstance instanceof RepoSyncCommand

        assertNotNull cmdInstance.id
        assertEquals cmdMap.id, cmdInstance.id

        assertNotNull cmdInstance.repoName
        assertEquals cmdMap.params.repoName, cmdInstance.params.repoName
        assertEquals "The single repo name must be available in a method",
            "repo1", cmdInstance.getRepoName()

        assertNotNull cmdInstance.context
        assertSame "The context object must be assigned to the command", 
            ctxt, cmdInstance.context
    }

    void testMakeReplicaCommand() {
        def cmdMap = [id:CommandTestsHelper.createCommandId(), code:'replicaPropsUpdate',
            params:[name:'Replica Brisbane', commandPollPeriod:4,
                commandConcurrencyLong: 10, commandConcurrencyShort: 15]]
        def ctxt = new CommandsExecutionContext()
        cmdMap.context = ctxt

        def cmdInstance = AbstractCommand.makeCommand(
            this.getClass().getClassLoader(), cmdMap)
        assertNotNull cmdInstance

        def sufix = "Command"
        def commandClassName = cmdMap.code.capitalize() + sufix
        assertEquals "The class name must be the capitalized code plus " +
            "'Command'", commandClassName, cmdInstance.class.getSimpleName()

        assertTrue "AbstractCommand factory must transform the command map " +
            "$cmdMap into an instance of ReplicaPropsUpdateCommand", 
            cmdInstance instanceof ReplicaPropsUpdateCommand

        assertNotNull cmdInstance.id
        assertEquals cmdMap.id, cmdInstance.id

        assertNull "Replica commands must not contain repoName", 
            cmdInstance.repoName

        for (entry in cmdInstance.params) {
            assertEquals "Parameter entry $entry is missing", 
                cmdMap.params[entry.key], cmdInstance.params[entry.key]
        }

        assertNotNull cmdInstance.context
        assertSame "The context object must be assigned to the command", 
            ctxt, cmdInstance.context
    }

    /**
     * Test processing a good add command.
     */
    void testLogExecution() {
        def cmdMap = [id:CommandTestsHelper.createCommandId(), code:'repoSync',
            params:[repoName:'/repos/repo1']]

        def logsDir = config.svnedge.logsDirPath + ""

        CommandsExecutionContext ctxt = new CommandsExecutionContext()
        ctxt.logsDir = new File(logsDir)
        cmdMap.context = ctxt

        def cmdInstance = AbstractCommand.makeCommand(
            this.getClass().getClassLoader(), cmdMap)

        def logName = "replica_cmds_" + String.format('%tY_%<tm_%<td', 
            new Date()) + ".log"
        def logFile = new File(logsDir, logName)
        println "Log file name: " + logFile

        if (logFile.exists()) {
            logFile.delete()
        }

        def execStepToken = "TEST"
        AbstractCommand.logExecution(execStepToken, cmdInstance)

        assertTrue "The log file $logFile must exist after logging " +
            "execution steps", logFile.exists()

        assertTrue "The log file $logFile must have loggin contents " +
            "after logging execution steps", logFile.size() > 0

        def logFileContents = new File(logsDir, logName).text
        println "Log file contents: $logFileContents" 

        assertTrue "The file must contain the token '$execStepToken'", 
            logFileContents.contains(execStepToken)
        assertTrue "The file must contain the id token '${cmdInstance.id}",
            logFileContents.contains(cmdInstance.id)
        def commandClassName = cmdInstance.getClass().getSimpleName()
        assertTrue "The file must contain the class name token " +
            "'$commandClassName'", logFileContents.contains(commandClassName)
        assertTrue "The file must contain the repository name/path" +
            cmdInstance.repoName,logFileContents.contains(cmdInstance.repoName)

        def cmdMap2 = [id:CommandTestsHelper.createCommandId(), code:'replicaPropsUpdate',
            params:[name:'Replica Brisbane', commandPollPeriod:4,
                commandConcurrencyLong: 10, commandConcurrencyShort: 15]]
        cmdMap2.context = ctxt
        def cmdInstance2 = AbstractCommand.makeCommand(
            this.getClass().getClassLoader(), cmdMap2)

        def execStepToken2 = "RUN"
        AbstractCommand.logExecution(execStepToken2, cmdInstance2)

        assertTrue "The log file $logFile must exist after logging " +
            "execution steps", logFile.exists()

        assertTrue "The log file $logFile must have loggin contents " +
            "after logging execution steps", logFile.size() > 0

        logFileContents = new File(logsDir, logName).text
        println "Log file contents after new execution: $logFileContents" 

        assertTrue "The file must contain the token '$execStepToken2'", 
            logFileContents.contains(execStepToken2)
        assertTrue "The file must STILL contain the PREVIOUS token " +
            "'$execStepToken'", logFileContents.contains(execStepToken)

        assertTrue "The file must contain the id token '${cmdInstance2.id}",
            logFileContents.contains(cmdInstance2.id)
        assertTrue "The file must contain the PREVIOUS id token " +
            "'${cmdInstance.id}", logFileContents.contains(cmdInstance2.id)

        assertTrue "The file must contain the PREVIOUS '$commandClassName'",
            logFileContents.contains(commandClassName)
        commandClassName = cmdInstance2.getClass().getSimpleName()
        assertTrue "The file must contain the token '$commandClassName'",
            logFileContents.contains(commandClassName)

        def execStepToken3 = "ERROR-RUN"
        def exceptionMessage = "An exception that can happen at runtime."
        try {
            AbstractCommand.logExecution(execStepToken3, cmdInstance2,
                new RuntimeException(exceptionMessage))

        } catch (Exception e) {
            fail("Exceptions in commands must be logged, but they can't: " + 
                e.getMessage())
        }

        assertTrue "The log file $logFile must exist after logging " +
            "execution steps with exceptions", logFile.exists()

        assertTrue "The log file $logFile must have loggin contents " +
            "after logging execution steps", logFile.size() > 0

        logFileContents = new File(logsDir, logName).text
        println "Log file contents after new execution: $logFileContents" 

        assertTrue "The file must contain the token '$execStepToken2'", 
            logFileContents.contains(execStepToken2)
        assertTrue "The file must STILL contain the PREVIOUS token " +
            "'$execStepToken'", logFileContents.contains(execStepToken)

        assertTrue "The file must contain the token '$execStepToken3'", 
            logFileContents.contains(execStepToken3)
        assertTrue "The file must STILL contain the PREVIOUS token " +
            "'$execStepToken'", logFileContents.contains(execStepToken)

        assertTrue "The file must contain the id token '${cmdInstance2.id}",
            logFileContents.contains(cmdInstance2.id)
        assertTrue "The file must contain the PREVIOUS id token " +
            "'${cmdInstance.id}", logFileContents.contains(cmdInstance2.id)

        assertTrue "The file must contain the PREVIOUS '$commandClassName'",
            logFileContents.contains(commandClassName)
        commandClassName = cmdInstance2.getClass().getSimpleName()
        assertTrue "The file must contain the token '$commandClassName'",
            logFileContents.contains(commandClassName)

        assertTrue "The file must contain the exception message " +
            "'$exceptionMessage'", logFileContents.contains(exceptionMessage)

        if (logFile.exists()) {
            logFile.delete()
        }
    }

    public void testLogExecutionWithWrongParametersException() {
        try {
            AbstractCommand.logExecution(null, null)
            fail("There should be no log with null parameters")

        } catch (Exception e) {
            assertNotNull(e)
        }
        try {
            AbstractCommand.logExecution("TOKEN", null)
            fail("There should be no log with null parameters")

        } catch (Exception e) {
            assertNotNull(e)
        }
        try {
            def command = new ReplicaApproveCommand()
            command.context = [:]
            AbstractCommand.logExecution(null, command)
            fail("There should be no log with null parameters")

        } catch (Exception e) {
            assertNotNull(e)
        }
    }
}
