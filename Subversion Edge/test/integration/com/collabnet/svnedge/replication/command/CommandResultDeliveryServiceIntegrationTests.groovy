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

import com.collabnet.svnedge.domain.integration.CommandResult 
import grails.test.GrailsUnitTestCase;

import static com.collabnet.svnedge.integration.CtfRemoteClientService.COMMAND_ID_PREFIX

class CommandResultDeliveryServiceIntegrationTests extends GrailsUnitTestCase {

    def commandResultDeliveryService

    def remotecmdexecs = Collections.synchronizedList(
        new LinkedList<Map<String, String>>())

    def CommandResultDeliveryServiceIntegrationTests() {
        def ids = []
        remotecmdexecs << [id:CommandTestsHelper.createCommandId(ids), repoName:'repo1', code:'repoSync']
        remotecmdexecs << [id:CommandTestsHelper.createCommandId(ids), repoName:null, code:'replicaPropsUpdate',
            params:[until:'2011-01-22']]
        remotecmdexecs << [id:CommandTestsHelper.createCommandId(ids), repoName:'repo1', code:'repoSync']
        remotecmdexecs << [id:CommandTestsHelper.createCommandId(ids), repoName:'repo2', code:'repoSync']
        remotecmdexecs << [id:CommandTestsHelper.createCommandId(ids), repoName:null, code:'replicaPropsUpdate'
            , params:[name:'Replica Brisbane']]
        remotecmdexecs << [id:CommandTestsHelper.createCommandId(ids), repoName:null, code:'replicaApprove',
            params:[name:'replica title', desc:'super replica']]
        remotecmdexecs << [id:CommandTestsHelper.createCommandId(ids), repoName:'repo3', code:'repoSync']
        remotecmdexecs << [id:CommandTestsHelper.createCommandId(ids), repoName:null, code:'replicaPropsUpdate'
            , params:[maxReplicacmdexecs:3, maxRepositorycmdexecs: 10]]
        remotecmdexecs << [id:CommandTestsHelper.createCommandId(ids), repoName:'repo2', code:'repoSync']
        remotecmdexecs << [id:CommandTestsHelper.createCommandId(ids), repoName:'repo3', code:'repoSync']
        sortRemoteCommands(remotecmdexecs)
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

    protected void setUp() {
        CommandResult.executeUpdate("delete CommandResult")
        assertEquals "No commands must exist", 0, CommandResult.count()
    }

    protected void tearDown() {
        CommandResult.executeUpdate("delete CommandResult")
    }

    /**
     * Test processing a good add command.
     */
    void testCommandResultProcess() {
        // commands are received to be processed, and first scheduled.
        // the command result objects are created
        remotecmdexecs.each{ command ->
            commandResultDeliveryService.makePersistedCommandResult(command)
        }
        assertEquals "The number of persisted results must be the same as " +
            "the number of partial commands", remotecmdexecs.size(),
            CommandResult.count()

        // verifying the command results are received to be processed
        def persistedResults = CommandResult.list()
        remotecmdexecs.each{ command ->
            def persistedResult = CommandResult.findWhere(commandId:command.id)
            assertNotNull "The command result must exist by Id", persistedResult
            assertEquals "The command ID must be the same", command.id,
                persistedResult.commandId
            assertNull "The command result must be null until processing",
                persistedResult.succeeded
            assertFalse "The transmitted value should be false",
                persistedResult.transmitted
            assertNotNull "The command result must have a Date created",
                persistedResult.dateCreated
            println "C: " + persistedResult.dateCreated
            assertNotNull "The command result must not be have a last Updated",
                persistedResult.lastUpdated
            println "U: " + persistedResult.lastUpdated
        }

        // results processed. Give results for each of them
        persistedResults.eachWithIndex{ cmdResult, i ->
            def result = cmdResult.commandId.hashCode()
            commandResultDeliveryService.saveCommandResult(cmdResult,
                (result + i) % 2 == 0)
        }

        Thread.sleep(1000)

        println "Results ready for transmission..."
        // verify the changed values, specially the transmitted and succeeded
        remotecmdexecs.eachWithIndex{ command, i ->
            def persistedResult = CommandResult.findByCommandId(command.id)
            def result = persistedResult.commandId.hashCode()
            assertNotNull "The command result must exist by Id", persistedResult
            assertEquals "The command ID must be the same", command.id,
                persistedResult.commandId
            assertEquals "The command result must not be null after processing",
                (result + i) % 2 == 0, persistedResult.succeeded
            assertFalse "The transmitted value should still be false",
                persistedResult.transmitted
            assertNotNull "The command result must have a Date created",
                persistedResult.dateCreated
            println "C: " + persistedResult.dateCreated
            assertNotNull "The command result must have a last Updated after " +
                "the update", persistedResult.lastUpdated
            println "U: " + persistedResult.lastUpdated
        }

        // connection with replica manager (TF) open, remove them
        persistedResults.each{ cmdResult ->
            commandResultDeliveryService.deleteTransmittedResults(cmdResult)
        }

        // verify the changed values, specially the transmitted and succeeded
        println "Results transmitted... verify no commands are available"
        assertEquals "All commands should have been removed", 0, CommandResult.count()
    }

    /**
     * Test processing a good add command.
     */
    void testNoCommandresultDuplicationNorChanges() {
        def command = remotecmdexecs[0]

        commandResultDeliveryService.makePersistedCommandResult(command)
        assertEquals "There must have a command result added", 1, 
            CommandResult.count()

        commandResultDeliveryService.makePersistedCommandResult(command)
        assertEquals "Two comands can't be created with the same ID", 1, 
            CommandResult.count()
    }

    void testAttemptToRegisterExistingCommandResultsRegisterClearExisting() {
        def partialCommands = remotecmdexecs[1..5]
        commandResultDeliveryService.registerClearExistingCommands(
            partialCommands)
        assertEquals "The number of persisted results must be the same as " +
            "the number of partial commands", 5, CommandResult.count()

        def previousSize = CommandResult.count()
        partialCommands = remotecmdexecs[3..7]
        // cleaning the partial commands
        commandResultDeliveryService.registerClearExistingCommands(
            partialCommands)

        assertEquals "The number of partial commands must be 2", 2,
            partialCommands.size()
        assertEquals "The number of persisted results must be only the new " +
            "commands, excluding the existing ones", previousSize + 2,
            CommandResult.count()
    }

    void testIncorrectValues() {
        try {
            commandResultDeliveryService.makePersistedCommandResult(null)
            fail("No result should exist with null command ID")
        } catch (Exception e) {
            
        }
        try {
            commandResultDeliveryService.makePersistedCommandResult("")
            fail("No result should exist with empty command ID")
        } catch (Exception e) {
            
        }

        try {
            commandResultDeliveryService.saveCommandResult(null)
            fail("Should not be able to save command result with null values")
        } catch (Exception e) {
            
        }
        try {
            commandResultDeliveryService.saveCommandResult("")
            fail("Should not be able to save command result with empty values")
        } catch (Exception e) {
            
        }

        try {
            commandResultDeliveryService.deleteTransmittedResults(null)
            fail("Should not be able to save command result with transmitted " +
                "value with null object")
        } catch (Exception e) {
            
        }
        try {
            commandResultDeliveryService.deleteTransmittedResults("")
            fail("Should not be able to save command result with transmitted " +
                "value with empty object")
        } catch (Exception e) {
            
        }
    }
}
