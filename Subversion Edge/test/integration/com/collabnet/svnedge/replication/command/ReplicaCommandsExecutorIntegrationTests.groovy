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

import com.collabnet.svnedge.TestUtil
import com.collabnet.svnedge.util.ConfigUtil 
import com.collabnet.svnedge.domain.Repository 
import com.collabnet.svnedge.domain.Server 
import com.collabnet.svnedge.domain.integration.CommandResult 
import com.collabnet.svnedge.domain.integration.ReplicaConfiguration;
import com.collabnet.svnedge.domain.integration.ReplicatedRepository 
import com.collabnet.svnedge.domain.integration.RepoStatus 
import com.collabnet.svnedge.domain.integration.ApprovalState;
import com.collabnet.svnedge.domain.integration.CtfServer
import com.collabnet.svnedge.integration.FetchReplicaCommandsJob
import com.collabnet.svnedge.integration.command.AbstractCommand
import com.collabnet.svnedge.integration.command.CommandsExecutionContext
import com.collabnet.svnedge.integration.command.impl.MockShortRunningCommand
import com.collabnet.svnedge.integration.command.impl.ReplicaPropsUpdateCommand
import com.collabnet.svnedge.integration.command.impl.RepoAddCommand
import com.collabnet.svnedge.integration.command.impl.RepoRemoveCommand
import com.collabnet.svnedge.integration.command.impl.RepoSyncCommand
import static com.collabnet.svnedge.admin.JobsAdminService.REPLICA_GROUP

import grails.test.*

class ReplicaCommandsExecutorIntegrationTests extends GrailsUnitTestCase {

    def replicaCommandExecutorService
    def ctfRemoteClientService
    def commandLineService
    def securityService
    def jobsAdminService
    def grailsApplication
    def config

    def REPO_NAME = "testproject2"
    def EXSY_ID = "exsy9876"
    def rConf
    File repoParentDir
    CommandsExecutionContext executionContext

    public ReplicaCommandsExecutorIntegrationTests() {
        this.rConf = ReplicaConfiguration.getCurrentConfig()
        if (!this.rConf) {
            rConf = new ReplicaConfiguration(svnMasterUrl: null, 
                name: "Test Replica", description: "Super replica", 
                message: "Auto-approved", systemId: "replica1001", 
                commandPollRate: 5, approvalState: ApprovalState.APPROVED)
            this.rConf.save()
        }
        // Setup a test repository parent
        repoParentDir = TestUtil.createTestDir("repo")
    }

    protected void tearDown() {
        super.tearDown()
        repoParentDir.deleteDir()

        // delete any log file
        AbstractCommand.getExecutionLogFile(executionContext)?.delete()
    }

    protected void setUp() {
        super.setUp()

        assertNotNull("The replica instance must exist", this.rConf)
        this.config = grailsApplication.config
        def ctfUrl = CommandTestsHelper.makeCtfBaseUrl(config)
        this.rConf.svnMasterUrl = ctfUrl + "/svn/repos"
        this.rConf.save()

        Server server = Server.getServer()
        server.repoParentDir = repoParentDir.getCanonicalPath()
        server.save()

        // delete the repo directory for the repo we are adding.
        def repoFileDir = new File(repoParentDir, REPO_NAME)
        repoFileDir.deleteDir()

        CtfServer ctfServer = CtfServer.getServer()
        ctfServer.baseUrl = ctfUrl
        ctfServer.mySystemId = EXSY_ID
        ctfServer.ctfUsername = "admin"
        ctfServer.ctfPassword = "n3TEQWKEjpY="
        ctfServer.save()

        executionContext = new CommandsExecutionContext()
        executionContext.logsDir = System.getProperty("java.io.tmpdir")
        executionContext.appContext = grailsApplication.mainContext
    }

    /**
     * Test processing a good add command.
     */
    void testProcessAddCommand() {
        def cmdParams = [:]
        cmdParams["repoName"] = REPO_NAME
        cmdParams["masterId"] = EXSY_ID

        def commandMap = [code: 'repoAdd', id: CommandTestsHelper.createCommandId(), params: cmdParams,
            context: executionContext]
        def command = AbstractCommand.makeCommand(getClass().getClassLoader(),
            commandMap)
        assertTrue "The command instance is incorrect", 
            command instanceof RepoAddCommand

        replicaCommandExecutorService.commandLifecycleExecutor(command)

        assertNotNull("Processing a command should not return null.", command)
        if (command.executionException) {
            println command.executionException
        }
        assertNull("Processing a command should not return an exception.\n" + 
                   command.executionException, command.executionException)
        assertTrue("Processing a command should return a true succeeded.",
                   command.succeeded)

        // verify the database records
        def repo = ReplicatedRepository.findByRepo(Repository.findByName(
            REPO_NAME))
        assertNotNull("The repo should exist.", repo)

        // verify the file-system state
        File repoDir = new File(repoParentDir, REPO_NAME)
        assertTrue("Repository directory should exist: " + 
            repoDir.getCanonicalPath(), repoDir.exists())
    }

    /**
     * Test processing a good remove command.
     */
    void testProcessRemoveCommand() {
        def classLoader = getClass().getClassLoader()

        def cmdParams = [:]
        cmdParams["repoName"] = REPO_NAME
        cmdParams["masterId"] = EXSY_ID

        // add first
        def commandMap = [code: 'repoAdd', id: CommandTestsHelper.createCommandId(), params: cmdParams,
            context: executionContext]
        def command = AbstractCommand.makeCommand(classLoader, commandMap)
        assertTrue "The command instance is incorrect",
            command instanceof RepoAddCommand

        replicaCommandExecutorService.commandLifecycleExecutor(command)

        // verify that the database records exist
        def repo = ReplicatedRepository.findByRepo(Repository.findByName(
            REPO_NAME))
        assertNotNull("The repo should exist.", repo)

        // verify the directory exists in the file-system 
        File repoDir = new File(repoParentDir, REPO_NAME)
        assertTrue("Repository directory should exist: " + 
            repoDir.getCanonicalPath(), repoDir.exists())

        // then remove
        commandMap = [code: 'repoRemove', id: CommandTestsHelper.createCommandId(), params: cmdParams,
            context: executionContext]

        command = AbstractCommand.makeCommand(classLoader, commandMap)
        assertTrue "The command instance is incorrect",
            command instanceof RepoRemoveCommand

        replicaCommandExecutorService.commandLifecycleExecutor(command)

        assertNotNull("Processing a command should not return null.", command)
        if (command.executionException) {
            println command.executionException
        }
        assertNull("Processing a command should not return an exception.\n" + 
                   command.executionException, command.executionException)
        assertTrue("Processing a command should return a true succeeded.",
                   command.succeeded)

        // verify that the database record was removed, but on removed state
        repo = ReplicatedRepository.findByRepo(Repository.findByName(REPO_NAME))
        assertNotNull("The repo should exist.", repo)
        assertEquals("The repo's status should be REMOVED.",
            RepoStatus.REMOVED, repo.getStatus())

        // verify that the directory was removed from the file-system.
        assertFalse("Repository directory should not exist: " + 
            repoDir.getCanonicalPath(), repoDir.exists())
    }

    /**
     * Test processing the replica props update command.
     */
    void testProcessReplicaPropsUpdateCommand() {
        def classLoader = getClass().getClassLoader()

        def newName = "Updated Replica Name"
        def newDescription = "Description Updated"
        def cmdParams = [:]
        cmdParams["name"] = newName
        cmdParams["description"] = newDescription

        // add first
        def commandMap = [code: 'replicaPropsUpdate', id: CommandTestsHelper.createCommandId(), params: cmdParams,
            context: executionContext]
        def command = AbstractCommand.makeCommand(classLoader, commandMap)
        assertTrue "The command instance is incorrect",
            command instanceof ReplicaPropsUpdateCommand

        replicaCommandExecutorService.commandLifecycleExecutor(command)

        assertNotNull("Processing a command should not return null.", command)
        if (command.executionException) {
            println command.executionException
        }
        assertNull("Processing a command should not return an exception.\n" + 
                   command.executionException, command.executionException)
        assertTrue("Processing a command should return a true succeeded.",
                   command.succeeded)

        this.rConf = ReplicaConfiguration.getCurrentConfig()
        assertEquals("The name should have been changed with the command " +
            "update", this.rConf.name, newName)
        assertEquals("The description should have been changed with the " +
            "command update", this.rConf.description, newDescription)

        // start the fetch job with the default properties as the poll rate
        // will be updated (commandPollPeriod)
        jobsAdminService.createOrReplaceTrigger(FetchReplicaCommandsJob.makeTrigger(60))
        log.info("Resuming replica jobs")
        jobsAdminService.resumeGroup(REPLICA_GROUP)

        def fetchCommandsJobTrigger = jobsAdminService.getTrigger(
            FetchReplicaCommandsJob.TRIGGER_NAME,
            FetchReplicaCommandsJob.TRIGGER_GROUP)
        def fetchDetails = jobsAdminService.getTriggerDetailsFromInstance(
            fetchCommandsJobTrigger)
        println "The first details of the fetch: ${fetchDetails}"

        cmdParams = [:]
        cmdParams["commandPollPeriod"] = "3"
        cmdParams["commandConcurrencyLong"] = "5"
        cmdParams["commandConcurrencyShort"] = "15"

        // add first
        commandMap = [code: 'replicaPropsUpdate', id: CommandTestsHelper.createCommandId(), params: cmdParams,
            context: executionContext]

        command = AbstractCommand.makeCommand(classLoader, commandMap)
        assertTrue "The command instance is incorrect",
            command instanceof ReplicaPropsUpdateCommand

        replicaCommandExecutorService.commandLifecycleExecutor(command)

        assertNotNull("Processing a command should not return null.", command)
        if (command.executionException) {
            println command.executionException
            fail("The command should be updated with the command poll params")
        }
        assertTrue("Processing a command should return a true succeeded.",
                   command.succeeded)

        this.rConf = ReplicaConfiguration.getCurrentConfig()
        assertTrue("The poll rate should have been updated " +
            "${this.rConf.commandPollRate} <> ${cmdParams.commandPollPeriod}",
            this.rConf.commandPollRate == cmdParams["commandPollPeriod"].toInteger())
        assertTrue("The max number of long-running commands must be changed" +
            "${this.rConf.maxLongRunningCmds} <> ${cmdParams.commandConcurrencyLong}",
            this.rConf.maxLongRunningCmds == cmdParams["commandConcurrencyLong"].toInteger())
        assertTrue("The max number of short-running commands must be changed" +
            "${this.rConf.maxLongRunningCmds} <> ${cmdParams.commandConcurrencyLong}",
            this.rConf.maxShortRunningCmds == cmdParams["commandConcurrencyShort"].toInteger())

        fetchCommandsJobTrigger = jobsAdminService.getTrigger(
            FetchReplicaCommandsJob.TRIGGER_NAME,
            FetchReplicaCommandsJob.TRIGGER_GROUP)
        def updatedFetchDetails = jobsAdminService.getTriggerDetailsFromInstance(
            fetchCommandsJobTrigger)
        assertNotNull "The updated trigger should not be null", 
            updatedFetchDetails

        // return the queue sizes back to the default, as later tests will assume
        // the semaphore permits match the configuration values
        commandMap = [code: 'replicaPropsUpdate', id: CommandTestsHelper.createCommandId(), 
            params: [commandConcurrencyLong: "2", commandConcurrencyShort:"10"],
            context: executionContext]     
        command = AbstractCommand.makeCommand(classLoader, commandMap)
        replicaCommandExecutorService.commandLifecycleExecutor(command)
    }

   /**
    * Test processing the replica props update command.
    */
    void testProcessReplicaPropsUpdateCommandIncorrectParams() {
        def classLoader = getClass().getClassLoader()

        def cmdParams = [:]

        // add first
        def commandMap = [code: 'replicaPropsUpdate', id: CommandTestsHelper.createCommandId(), params: cmdParams,
            context: executionContext]
        def command = AbstractCommand.makeCommand(classLoader, commandMap)
        assertTrue "The command instance is incorrect",
            command instanceof ReplicaPropsUpdateCommand

        replicaCommandExecutorService.commandLifecycleExecutor(command)

        assertFalse("Processing empty params should return succeeded=false.",
            command.succeeded)
        assertNotNull("Processing the props update with emtpy values on name " +
            "and description should throw an exception",
            command.executionException)
        println command.executionException

        cmdParams = [:]
        cmdParams["commandPollPeriod"] = "-1"
        cmdParams["commandConcurrencyLong"] = "-50"
        cmdParams["commandConcurrencyShort"] = "-11"

       // update the properties
       commandMap = [code: 'replicaPropsUpdate', id: CommandTestsHelper.createCommandId(), params: cmdParams,
           context: executionContext]

       command = AbstractCommand.makeCommand(classLoader, commandMap)
        assertTrue "The command instance is incorrect",
            command instanceof ReplicaPropsUpdateCommand

        replicaCommandExecutorService.commandLifecycleExecutor(command)

        assertFalse("Processing wrong command should return succeeded=false.",
            command.succeeded)
        assertNotNull("Processing the props update with negative values " +
            "should throw an exception", command.executionException)
        println command.executionException
    }

    void testAddRemoveReAddRepoOnDatabase() {
        def classLoader = getClass().getClassLoader()

        def cmdParams = [:]
        cmdParams["repoName"] = REPO_NAME
        cmdParams["masterId"] = EXSY_ID

        // add first
        def commandMap = [code: 'repoAdd', id: CommandTestsHelper.createCommandId(), params: cmdParams,
            context: executionContext]
        def command = AbstractCommand.makeCommand(classLoader, commandMap)
        assertTrue "The command instance is incorrect",
            command instanceof RepoAddCommand

        replicaCommandExecutorService.commandLifecycleExecutor(command)

        assertTrue("Processing a command should return a true succeeded.",
            command.succeeded)
        if (command.executionException) {
            println command.executionException
            fail("The command repoAdd should have worked.")
        }

        // verify that the database records exist
        def repo = ReplicatedRepository.findByRepo(Repository.findByName(
            REPO_NAME))
        assertNotNull("The repo should exist.", repo)

        // verify the directory exists in the file-system
        File repoDir = new File(repoParentDir, REPO_NAME)
        assertTrue("Repository directory should exist: " +
            repoDir.getCanonicalPath(), repoDir.exists())

        // then remove
        commandMap = [code: 'repoRemove', id: CommandTestsHelper.createCommandId(), params: cmdParams,
            context: executionContext]
        command = AbstractCommand.makeCommand(classLoader, commandMap)
        assertTrue "The command instance is incorrect",
            command instanceof RepoRemoveCommand

        replicaCommandExecutorService.commandLifecycleExecutor(command)

        assertTrue("Processing a command should return a true succeeded.",
            command.succeeded)
        if (command.executionException) {
            println command.executionException
            fail("The command should be updated with the command poll params")
        }

        // verify that the database record was removed, but on removed state
        repo = ReplicatedRepository.findByRepo(Repository.findByName(REPO_NAME))
        assertNotNull("The repo should exist.", repo)
        assertEquals("The repo's status should be REMOVED.",
            RepoStatus.REMOVED, repo.getStatus())

        // verify that the directory was removed from the file-system.
        assertFalse("Repository directory should not exist: " +
            repoDir.getCanonicalPath(), repoDir.exists())

        // try to re-add the same repository
        commandMap = [code: 'repoAdd', id: CommandTestsHelper.createCommandId(), params: cmdParams,
            context: executionContext]
        command = AbstractCommand.makeCommand(classLoader, commandMap)
        assertTrue "The command instance is incorrect",
            command instanceof RepoAddCommand

        replicaCommandExecutorService.commandLifecycleExecutor(command)

        assertTrue("Processing a command should return a true succeeded.",
            command.succeeded)
        if (command.executionException) {
            println command.executionException
            fail("Should be able to re-add a removed repository.")
        }

        repo = ReplicatedRepository.findByRepo(Repository.findByName(REPO_NAME))
        assertTrue("The repository should be enabled.", repo.getEnabled())
    }

    void testSyncReplicatedRepository() {
        def classLoader = getClass().getClassLoader()

        def repoResult = CommandTestsHelper
            .createTestRepository(config, ctfRemoteClientService)
        def repoName = repoResult.repoName
        def projectName = repoResult.projectName
        def cmdParams = [repoName: repoName]

        // add first
        def commandMap = [code: 'repoAdd', id: CommandTestsHelper.createCommandId(), params: cmdParams,
            context: executionContext]
        def command = AbstractCommand.makeCommand(classLoader, commandMap)
        assertTrue "The command instance is incorrect",
            command instanceof RepoAddCommand

        replicaCommandExecutorService.commandLifecycleExecutor(command)

        assertTrue("Processing a command should return a true succeeded.",
            command.succeeded)
        if (command.executionException) {
            println command.executionException
            fail("Should be able to add a repository for sync.")
        }

        def repo = ReplicatedRepository.findByRepo(Repository.findByName(
            repoName))
        assertNotNull("The repo should exist.", repo)

        File repoDir = new File(repoParentDir, repoName)
        assertTrue("Repository directory should exist: " + 
            repoDir.getCanonicalPath(), repoDir.exists())

        File wcDir = TestUtil.createTestDir("wc")

        def replicaConfig = ReplicaConfiguration.getCurrentConfig()
        def masterRepoUrl = replicaConfig.getSvnMasterUrl() + "/" + repoName

        def ctfServer = CtfServer.getServer()
        def username = ctfServer.ctfUsername
        def password = securityService.decrypt(ctfServer.ctfPassword)
        command = [ConfigUtil.svnPath(), "co", masterRepoUrl, wcDir.canonicalPath,
            "--username", username, "--password", password,
            "--non-interactive", "--no-auth-cache"] // "--config-dir=/tmp"
        commandLineService.execute(command.toArray(new String[0]))

        File dotSvn = new File(wcDir, ".svn")
        assertTrue("Working copy missing .svn folder", dotSvn.exists())

        def testFile = File.createTempFile("sync-test", ".txt", wcDir)
        String filename = testFile.name
        testFile.text = "This is a test file"
        command = [ConfigUtil.svnPath(), "add", testFile.canonicalPath,
            "--non-interactive"]
        commandLineService.execute(command.toArray(new String[0]))

        command = [ConfigUtil.svnPath(), "ci", testFile.canonicalPath,
            "--username", username, "--password", password,
            "--non-interactive", "-m", "Test commit"]
        commandLineService.execute(command.toArray(new String[0]))

        def repoUri = commandLineService.createSvnFileURI(repoDir)
        File wcDir2 = TestUtil.createTestDir("wc2")
        command = [ConfigUtil.svnPath(), "co", repoUri, wcDir2.canonicalPath,
            //"--username", username, "--password", password,
            "--non-interactive", "--no-auth-cache"]
        commandLineService.execute(command.toArray(new String[0]))
        File testFile2 = new File(wcDir2, filename)
        assertFalse("Test file should not exist yet: " + 
            testFile2.canonicalPath, testFile2.exists())

        cmdParams = [:]
        cmdParams["repoName"] = repoName

        // execute svn sync
        def cmdId = CommandTestsHelper.createCommandId()
        commandMap = [code: 'repoSync', id: cmdId, params: cmdParams,
            context: executionContext]
        command = AbstractCommand.makeCommand(classLoader, commandMap)
        assertTrue "The command instance is incorrect",
            command instanceof RepoSyncCommand

        replicaCommandExecutorService.commandLifecycleExecutor(command)

        assertTrue("Processing a command should return a true succeeded.",
            command.succeeded)
        if (command.executionException) {
            println command.executionException
            fail("Should be able to sync a command.")
        }

        boolean fileExists = false
        for (int i = 0; i < 30 && !fileExists; i++) {
            command = [ConfigUtil.svnPath(), "up", wcDir2.canonicalPath,
                //"--username", username, "--password", password,
                "--non-interactive", "--no-auth-cache"]
            commandLineService.execute(command.toArray(new String[0]))
            fileExists = testFile2.exists()
            if (!fileExists) {
                Thread.sleep(1000)
            }
        }
        assertTrue("Test file should exist: " + testFile2.canonicalPath, 
            fileExists)
        println "Waiting until the command has terminated result..."
        def commandResult
        int counter = 0
        while(counter++ < 3) {
            commandResult = CommandResult.findWhere(commandId: cmdId)
            if (commandResult?.transmitted) {
                break
            } else {
                println "Command result not transmitted yet..."
                Thread.sleep(1000)
            }
        }
        wcDir.deleteDir()
        wcDir2.deleteDir()
        CommandTestsHelper
            .deleteTestProject(config, ctfRemoteClientService, projectName)
    }
    
    void testCommandRetryOnFailure() {
        def classLoader = getClass().getClassLoader()
        def cmdParams = [:]
        cmdParams["commandExpectedFailures"] = 3
        cmdParams["commandRunTimeSeconds"] = 1

        // add first
        def commandMap = [code: 'mockShortRunning', id: CommandTestsHelper.createCommandId(), params: cmdParams,
            context: executionContext]
        def command = AbstractCommand.makeCommand(classLoader, commandMap)
        assertTrue "The command instance is incorrect",
            command instanceof MockShortRunningCommand

        ReplicaConfiguration replicaConfig = ReplicaConfiguration.currentConfig
        replicaConfig.commandRetryAttempts = 2
        replicaConfig.commandRetryWaitSeconds = 1
        replicaCommandExecutorService.commandLifecycleExecutor(command)

        assertFalse("Processing a command should return a false succeeded, " +
                "if retry attempts is less than mocked failures",
                command.succeeded)
        assertEquals "First exception should be returned",
                MockShortRunningCommand.FAILED + " 1",
                command.executionException?.message

        command = AbstractCommand.makeCommand(classLoader, commandMap)
        assertTrue "The command instance is incorrect",
                command instanceof MockShortRunningCommand

        replicaConfig.commandRetryAttempts = 3
        replicaCommandExecutorService.commandLifecycleExecutor(command)

        assertTrue("Processing a command should return a true succeeded.",
            command.succeeded)
        if (command.executionException) {
            println command.executionException
            fail("The command should have worked.")
        }
    }
}
