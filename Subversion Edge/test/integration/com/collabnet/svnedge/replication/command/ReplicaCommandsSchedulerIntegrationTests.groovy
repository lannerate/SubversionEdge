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


import grails.test.*
import static com.collabnet.svnedge.integration.CtfRemoteClientService.COMMAND_ID_PREFIX

class ReplicaCommandsSchedulerIntegrationTests extends GrailsUnitTestCase {

    def replicaCommandSchedulerService
    def executorService

    def remotecmdexecs = Collections.synchronizedList(new LinkedList<Map<String, String>>())

    def ReplicaCommandsSchedulerIntegrationTests() {
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
        sortRemoteCommands(remotecmdexecs)
    }

    protected void setUp() {
        super.setUp()
        executorService.execute {
            replicaCommandSchedulerService.schedulerSynchronizer.take()
        }
    }

    protected void tearDown() {
        super.tearDown()
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

    void testInitialState() {
        assertEquals "The initial queued commands size is incorrect", 0,
            replicaCommandSchedulerService.getQueuedCommandsSize()
        assertEquals "The initial executing number of commnads is incorrect", 0,
            replicaCommandSchedulerService.getExecutingCommandsSize()
    }

    void testOffer() {
        replicaCommandSchedulerService.offer(remotecmdexecs, null)
        assertEquals "The size is incorrect", 10,
            replicaCommandSchedulerService.getQueuedCommandsSize()
        assertEquals "The size is incorrect", 0,
            replicaCommandSchedulerService.getExecutingCommandsSize()

        def categories = replicaCommandSchedulerService.getCategorizedCommandQueues().keySet()
        assertTrue categories.containsAll(["replicaServer", "repo1", "repo2", 
            "repo3"])

        def cat = "replicaServer"
        def cmd = replicaCommandSchedulerService.getNextCommandFromCategory(cat)
        def nextId = "cmdexec1000"
        assertEquals "The next command is incorrect for $cat", nextId, cmd.id
        assertFalse "No command should be running after offer for $cat",
            replicaCommandSchedulerService.isThereCommandRunning(cat)

        cat = "repo1"
        cmd = replicaCommandSchedulerService.getNextCommandFromCategory(cat)
        nextId = "cmdexec1001"
        assertEquals "The next command is incorrect for $cat", nextId, cmd.id
        assertFalse "No command should be running after offer for $cat",
            replicaCommandSchedulerService.isThereCommandRunning(cat)

        cat = "repo2"
        cmd = replicaCommandSchedulerService.getNextCommandFromCategory(cat)
        nextId = "cmdexec1005"
        assertEquals "The next command is incorrect for $cat", nextId, cmd.id
        assertFalse "No command should be running after offer for $cat",
            replicaCommandSchedulerService.isThereCommandRunning(cat)

        cat = "repo3"
        cmd = replicaCommandSchedulerService.getNextCommandFromCategory(cat)
        nextId = "cmdexec1003"
        assertEquals "The next command is incorrect for $cat", nextId, cmd.id
        assertFalse "No command should be running after offer for $cat",
            replicaCommandSchedulerService.isThereCommandRunning(cat)
    }

    def scheduleNextCommand() {
        if (remotecmdexecs.size() == 0) {
            return null
        }
        def iterator = remotecmdexecs.iterator()
        def nextCommand = iterator.next()
        assertNotNull "The next command must be not null", nextCommand

        def category = replicaCommandSchedulerService.getCommandCategory(
            nextCommand)
        assertNotNull "The category should not be null", category

        replicaCommandSchedulerService.scheduleCommandForExecution(category, 
            nextCommand)
        // remove the command (as the scheduler handler)
        iterator.remove()
        return nextCommand
    }

    void testOfferBadValues() {
        replicaCommandSchedulerService.cleanCommands()
        replicaCommandSchedulerService.offer([], null)
        assertEquals "Offering no commands should not change the size of " +
            "queued commands", 0,
            replicaCommandSchedulerService.getQueuedCommandsSize()
        assertEquals "Offering no commands should not change the size of " +
            "executing commands", 0, 
            replicaCommandSchedulerService.getExecutingCommandsSize()

        replicaCommandSchedulerService.offer(null, null)
        assertEquals "Offering null should not change the size of " +
            "queued commands", 0,
            replicaCommandSchedulerService.getQueuedCommandsSize()
        assertEquals "Offering null should not change the size of " +
            "executing commands", 0, 
            replicaCommandSchedulerService.getExecutingCommandsSize()
    }

    void testAddBadValuesToMutex() {
        replicaCommandSchedulerService.cleanCommands()
        try {
            replicaCommandSchedulerService.addCommandToCategoryMutex("r1", null)
            fail("Offering null command to the mutex should throw an exception")

        } catch (Exception e) {
            assertEquals "Offering to the mutex should not change the " +
                "size of executing commands", 0,
            replicaCommandSchedulerService.getExecutingCommandsSize()
        }

        try {
            replicaCommandSchedulerService.addCommandToCategoryMutex(null,"cmd")
            fail("Offering null category to the mutex should throw exception")

        } catch (Exception e) {
            assertEquals "Offering null to the mutex should not change the " +
                "size of executing commands", 0,
            replicaCommandSchedulerService.getExecutingCommandsSize()
        }
    }

    void testIsThereCommandRunningWithBadValues() {
        try {
            replicaCommandSchedulerService.isThereCommandRunning("")
            fail("Verifying empty values should throw an exception")

        } catch (Exception e) {
            println e
        }
        try {
            replicaCommandSchedulerService.isThereCommandRunning(null)
            fail("Verifying empty values should throw an exception")

        } catch (Exception e) {
            println e
        }
    }

    void testGetCategoriesWithBadValues() {
        replicaCommandSchedulerService.cleanCommands()
        replicaCommandSchedulerService.offer(remotecmdexecs[2..4], null)
        assertEquals "Offering 3 commands should have changed the size of " +
            "queued commands", 3,
            replicaCommandSchedulerService.getQueuedCommandsSize()

        try {
            replicaCommandSchedulerService.getNextCommandFromCategory("")
            fail("Getting the next command from an empty category should " +
                "throw an exception.")

        } catch (Exception e) {
            println e
        }

        try {
            replicaCommandSchedulerService.getNextCommandFromCategory(null)
            fail("Getting the next command from a null category should " +
                "throw an exception.")

        } catch (Exception e) {
            println e
        }

        try {
            replicaCommandSchedulerService.getNextCommandFromCategory("repoxxx")
            fail("Getting the next command from a non-existing category " +
                "should throw an exception.")

        } catch (Exception e) {
            println e
        }
    }

}
