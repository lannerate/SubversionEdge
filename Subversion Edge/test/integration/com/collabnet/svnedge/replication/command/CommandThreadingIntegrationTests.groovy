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
package com.collabnet.svnedge.replication.command

import com.collabnet.svnedge.domain.Server
import com.collabnet.svnedge.integration.FetchReplicaCommandsJob
import com.collabnet.svnedge.integration.command.AbstractCommand
import com.collabnet.svnedge.integration.command.CommandsExecutionContext
import com.collabnet.svnedge.integration.command.event.MaxNumberCommandsRunningUpdatedEvent;

import grails.test.GrailsUnitTestCase
import com.collabnet.svnedge.domain.integration.*
import com.collabnet.svnedge.domain.ServerMode
import org.junit.Ignore

/**
 * This test case verifies command threading, blocking, etc in the
 * ReplicaCommand processing components. It uses mock command implementation
 * classes for long and short run times, and scans the command execution log
 * to validate expectations. The command execution log should be configured to
 * log "commandStateTransitions" in Config.groovy (svnedge.replica.logging)
 */
class CommandThreadingIntegrationTests extends GrailsUnitTestCase {

    def replicaCommandExecutorService
    def replicaCommandSchedulerService
    def commandLineService
    def securityService
    def jobsAdminService
    def grailsApplication
    def config

    def ctfRemote
    def commandResultDeliveryService
    def fetchReplicaCommandsJob

    def EXSY_ID = "exsy9876"
    def rConf

    enum ExecutionOrder { SEQUENTIAL, PARALLEL, UNKNOWN }

    public CommandThreadingIntegrationTests() {
        this.rConf = ReplicaConfiguration.getCurrentConfig()
        if (!this.rConf) {
            rConf = new ReplicaConfiguration(svnMasterUrl: null,
                    name: "Test Replica", description: "Super replica",
                    message: "Auto-approved", systemId: "replica1001",
                    approvalState: ApprovalState.APPROVED)

        }

        def server = Server.getServer()
        server.setMode(ServerMode.REPLICA)
        server.save()
    }

    protected void setUp() {
        super.setUp()

        // clear the command queue and history
        replicaCommandSchedulerService.cleanCommands()
        CommandResult.list().each {
            it.delete()
        }

        assertNotNull("The replica instance must exist", this.rConf)
        this.config = grailsApplication.config
        this.rConf.svnMasterUrl = "http://forge.collab.net/svn/repos"
        this.rConf.save()

        CtfServer ctfServer = CtfServer.getServer()
        ctfServer.baseUrl = "http://forge.collab.net"
        ctfServer.mySystemId = EXSY_ID
        ctfServer.ctfUsername = "admin"
        ctfServer.ctfPassword = "n3TEQWKEjpY="
        ctfServer.save()

        // setup mocking of the remote Ctf server
        fetchReplicaCommandsJob = grailsApplication.mainContext.getBean('com.collabnet.svnedge.integration.FetchReplicaCommandsJob')
        ctfRemote = new Expando()
        commandResultDeliveryService = new Expando()
        def cnSoap60 = new Expando()

        // set command queue sizes
        def oldMaxLong = rConf.maxLongRunningCmds
        def oldMaxShort = rConf.maxShortRunningCmds
        rConf.maxLongRunningCmds = 2
        rConf.maxShortRunningCmds = 2
        rConf.save()
        replicaCommandSchedulerService.setSemaphoresUpdatedEvent(
            new MaxNumberCommandsRunningUpdatedEvent(this,
                oldMaxLong, 2, oldMaxShort, 2))
        
        cnSoap60.getUserSessionBySoapId = { p1 -> "userSessionId1001" }
        ctfRemote.login = { p1, p2, p3, p4 -> "soapSessionId1001" }
        ctfRemote.cnSoap = { p1 -> cnSoap60 }
        ctfRemote.logoff = { p1, p2, p3 -> return }
        ctfRemote.getReplicaQueuedCommands = { p1, p2, p3, p4, p5 -> [] }

        commandResultDeliveryService.restartDelivering = { p1 -> true}
        commandResultDeliveryService.stopDelivering = {-> true}
        commandResultDeliveryService.getUnacknowledgedExecutingCommandResults = {-> []}

        fetchReplicaCommandsJob.ctfRemoteClientService = ctfRemote
        fetchReplicaCommandsJob.commandResultDeliveryService = commandResultDeliveryService

        // start the background jobs (otherwise disabled in test mode)
        replicaCommandSchedulerService.startBackgroundHandlers()
        replicaCommandExecutorService.startBackgroundHandlers()

        // run the job as if initiated by quartz (quartz jobs do not run in test mode it seems)
        fetchReplicaCommandsJob.execute()
        Thread.currentThread().sleep(2000)
    }

    protected void tearDown() {
        super.tearDown()

        // delete log file
//        getExecutionLog()?.delete()

        // clear the command queue and history
        replicaCommandSchedulerService.cleanCommands()
        CommandResult.list().each {
            it.delete()
        }
    }

    /**
     * Tests basic blocking and concurrency expectations
     */
    void testBasicBlockingAndConcurrency() {

        // mock commands from CTF
        def remotecmdexecs = []
        remotecmdexecs << [id: 'cmdexec9801', repoName: 'threadTestRepo1', code: 'mockLongRunning']
        remotecmdexecs << [id: 'cmdexec9802', code: 'mockShortRunning']
        remotecmdexecs << [id: 'cmdexec9803', repoName: 'threadTestRepo2', code: 'mockLongRunning']
        remotecmdexecs << [id: 'cmdexec9804', repoName: 'threadTestRepo3', code: 'mockLongRunning']
        remotecmdexecs << [id: 'cmdexec9805', repoName: 'threadTestRepo1', code: 'mockShortRunning']
        ctfRemote.getReplicaQueuedCommands = { p1, p2, p3, p4, p5 -> remotecmdexecs }

        // clear the execution log
        getExecutionLog()?.delete()

        // run the quartz job
        fetchReplicaCommandsJob.execute()

        // wait a bit
        File logFile = getExecutionLog()
        waitForLog(logFile)
        println "Command execution log:"
        println logFile.text
        
        // validate concurrency expectations in the execution log
        assertEquals("commands from same repo should be sequential",
                ExecutionOrder.SEQUENTIAL, getExecutionOrder("cmdexec9801", "cmdexec9805"));

        // one of these command pairs should be parallel
        boolean shortAndLongConcurrency = (ExecutionOrder.PARALLEL == getExecutionOrder("cmdexec9801", "cmdexec9802")) ||
                    (ExecutionOrder.PARALLEL == getExecutionOrder("cmdexec9803", "cmdexec9802")) ||
                    (ExecutionOrder.PARALLEL == getExecutionOrder("cmdexec9804", "cmdexec9802"))
    
        // one of these command pairs should be parallel
        boolean distinctRepoConcurrency = (ExecutionOrder.PARALLEL == getExecutionOrder("cmdexec9801", "cmdexec9803")) ||
                (ExecutionOrder.PARALLEL == getExecutionOrder("cmdexec9801", "cmdexec9804")) ||
                (ExecutionOrder.PARALLEL == getExecutionOrder("cmdexec9803", "cmdexec9804"))

        // one of these command pairs should be serial
        boolean distinctRepoSequentiality = (ExecutionOrder.SEQUENTIAL == getExecutionOrder("cmdexec9801", "cmdexec9803")) ||
                (ExecutionOrder.SEQUENTIAL == getExecutionOrder("cmdexec9801", "cmdexec9804")) ||
                (ExecutionOrder.SEQUENTIAL == getExecutionOrder("cmdexec9803", "cmdexec9804"))

        assertTrue("command for repo and general category should be parallel",
                shortAndLongConcurrency);
        assertTrue("commands for distinct repos should run parallel within the concurrency limit",
                distinctRepoConcurrency);
        assertTrue("commands for distinct repos should run sequential outside the concurrency limit",
                distinctRepoSequentiality);
    }

    /**
     * examines the execution log to determine (if possible) run order of two given commandIds
     * @param cmdId1
     * @param cmdId2
     * @return ExecutionOrder of the two commands (SEQUENTIAL, PARALLEL, UNKNOWN)
     */
    private ExecutionOrder getExecutionOrder(String cmdId1, String cmdId2) {
        String logContent = getExecutionLog().text

        Date cmd1Start
        Date cmd2Start
        Date cmd1End
        Date cmd2End

        logContent.eachLine { it ->
            if (it.contains(cmdId1) && it.contains("entering state: RUNNING")) {
                cmd1Start = parseTime(it)
            }
            if (it.contains(cmdId1) && it.contains("entering state: TERMINATED")) {
                cmd1End = parseTime(it)
            }
            if (it.contains(cmdId2) && it.contains("entering state: RUNNING")) {
                cmd2Start = parseTime(it)
            }
            if (it.contains(cmdId2) && it.contains("entering state: TERMINATED")) {
                cmd2End = parseTime(it)
            }
        }

        try {
            if (cmd1Start < cmd2End && cmd2Start < cmd1End) {
                return ExecutionOrder.PARALLEL
            }
            else if (cmd1End <= cmd2Start || cmd2End <= cmd1Start){
                return ExecutionOrder.SEQUENTIAL
            }
            else {
                return ExecutionOrder.UNKNOWN
            }
        }
        catch (Exception e) {
            return ExecutionOrder.UNKNOWN
        }
    }

    /**
     * fetch the command execution log
     * @return
     */
    private File getExecutionLog() {
        def executionContext = new CommandsExecutionContext()
        executionContext.logsDir = config.svnedge.logsDirPath
        return AbstractCommand.getExecutionLogFile(executionContext)
    }
    
    /**
     * Waits for the file to exist and appear not to be growing
     */
    private File waitForLog(logFile) {
        int count = 0
        while (count++ < 12 && !logFile.exists()) {
            log.info "${count}. Log file does not exist. Waiting..."
            Thread.sleep(5000L)
        }
        if (count == 12) {
            log.severe "Command log ${logFile} does not exist after 1 minute"
        }
        if (logFile.exists()) {
            long prevSize = 0
            long size = logFile.length()
            while (size == 0L || size != prevSize) {
                prevSize = size
                log.info "Waiting for log file to quit growing..."
                Thread.sleep(20000)
                size = logFile.length()
            }
        }
        return logFile 
    }

    /**
     * parse the date of a given row of command execution log
     * @param s log row
     * @return the Date
     */
    private Date parseTime(String s) {
        // assume first token of line is HH:MM:SS,LLL
        Calendar cal = Calendar.getInstance()
        String time = new StringTokenizer(s, " ").nextToken()
        def timeTokenizer = new StringTokenizer(time, ":,")
        String hour = timeTokenizer.nextToken()
        String minutes = timeTokenizer.nextToken()
        String seconds = timeTokenizer.nextToken()
        String millis = timeTokenizer.nextToken()

        cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hour))
        cal.set(Calendar.MINUTE, Integer.parseInt(minutes))
        cal.set(Calendar.SECOND, Integer.parseInt(seconds))
        cal.set(Calendar.MILLISECOND, Integer.parseInt(millis))

        return cal.time
    }

}
