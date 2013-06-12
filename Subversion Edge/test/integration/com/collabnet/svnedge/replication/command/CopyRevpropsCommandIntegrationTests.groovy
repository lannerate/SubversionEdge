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

import com.collabnet.svnedge.TestUtil
import com.collabnet.svnedge.domain.Server

import com.collabnet.svnedge.integration.command.AbstractCommand
import com.collabnet.svnedge.integration.command.CommandsExecutionContext

import com.collabnet.svnedge.util.ConfigUtil
import grails.test.GrailsUnitTestCase

import com.collabnet.svnedge.domain.integration.*

class CopyRevpropsCommandIntegrationTests extends GrailsUnitTestCase {

    def replicaCommandExecutorService
    def commandLineService
    def ctfRemoteClientService
    def securityService
    def jobsAdminService
    def grailsApplication
    def config

    def EXSY_ID = "exsy9876"
    def rConf
    File repoParentDir
    CommandsExecutionContext executionContext
    def repoName
    def projectName

    public CopyRevpropsCommandIntegrationTests() {
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
        CommandTestsHelper
            .deleteTestProject(config, ctfRemoteClientService, projectName)
    }

    protected void setUp() {
        super.setUp()

        assertNotNull("The replica instance must exist", this.rConf)
        this.config = grailsApplication.config
        def ctfUrl = CommandTestsHelper.makeCtfBaseUrl(config)
        this.rConf.svnMasterUrl = ctfUrl + "/svn/repos"
        this.rConf.save(flush:true)

        Server server = Server.getServer()
        server.repoParentDir = repoParentDir.getCanonicalPath()
        server.save(flush:true)

        def repoResult = CommandTestsHelper
            .createTestRepository(config, ctfRemoteClientService)
        repoName = repoResult.repoName
        projectName = repoResult.projectName
        // delete the repo directory for the repo we are adding.
        def repoFileDir = new File(repoParentDir, repoName)
        repoFileDir.deleteDir()

        CtfServer ctfServer = CtfServer.getServer()
        ctfServer.baseUrl = ctfUrl
        ctfServer.mySystemId = EXSY_ID
        ctfServer.ctfUsername = "admin"
        ctfServer.ctfPassword = "n3TEQWKEjpY="
        ctfServer.save(flush:true)

        executionContext = new CommandsExecutionContext()
        executionContext.logsDir = System.getProperty("java.io.tmpdir")
        executionContext.appContext = grailsApplication.mainContext
    }

    public void testSyncRevprops() {
        def classLoader = getClass().getClassLoader()

        def revNumberToAlter
        def originalCommitMsg = "ORIGINAL commit msg"
        def updatedCommitMsg = "UPDATED commit msg"

        def cmdParams = [:]
        cmdParams["repoName"] = repoName

        // add and sync the repo
        assertNotNull("ReplicaConfig must have svnMasterUrl from which to sync",
                ReplicaConfiguration.getCurrentConfig().svnMasterUrl)
        def commandMap = [code: 'repoAdd', id: CommandTestsHelper.createCommandId(), params: cmdParams,
            context: executionContext]
        def command = AbstractCommand.makeCommand(classLoader, commandMap)
        replicaCommandExecutorService.commandLifecycleExecutor(command)

        if (command.executionException) {
            println command.executionException
            fail("Should be able to add a repository for sync.")
        }

        File wcMaster = TestUtil.createTestDir("wcMaster")

        def replicaConfig = ReplicaConfiguration.getCurrentConfig()
        def masterRepoUrl = replicaConfig.getSvnMasterUrl() + "/" + repoName

        def ctfServer = CtfServer.getServer()
        def username = ctfServer.ctfUsername
        def password = securityService.decrypt(ctfServer.ctfPassword)
        command = [ConfigUtil.svnPath(), "co", masterRepoUrl, wcMaster.canonicalPath,
            "--username", username, "--password", password,
            "--non-interactive", "--no-auth-cache"] as String[]// "--config-dir=/tmp"
        commandLineService.execute(command)

        // create / update the reusable test file
        def testFileMaster = File.createTempFile("copy-revprops-test", ".txt", wcMaster)
        testFileMaster.text = "This is a test file"
        log.info("testFile = " + testFileMaster.canonicalPath)
        command = [ConfigUtil.svnPath(), "add", testFileMaster.canonicalPath,
            "--non-interactive"] as String[]
        commandLineService.execute(command)

        // set a custom property
        def propVal = "property initial value"
        command = [ConfigUtil.svnPath(), "propset", "propKey", propVal, testFileMaster.canonicalPath,
            "--username", username, "--password", password,
            "--non-interactive"] as String[]
        commandLineService.execute(command)

        // commit the test file & capture the revision number to alter later
        command = [ConfigUtil.svnPath(), "ci", testFileMaster.canonicalPath,
            "--username", username, "--password", password,
            "--non-interactive", "-m", originalCommitMsg] as String[]
        def result = commandLineService.execute(command)
        log.info ("Commit result in master WC: \n" + result)
        def matcher = result =~ /Committed revision (\d+)/
        revNumberToAlter = matcher[0][1]

        def repoUri = commandLineService.createSvnFileURI(new File(repoParentDir, repoName))
        File wcReplica = TestUtil.createTestDir("wcReplica")
        command = [ConfigUtil.svnPath(), "co", repoUri, wcReplica.canonicalPath,
            //"--username", username, "--password", password,
            "--non-interactive", "--no-auth-cache"] as String[]
        commandLineService.execute(command)
        File testFileReplica = new File(wcReplica, testFileMaster.name)

        cmdParams = [:]
        cmdParams["repoName"] = repoName

        // execute svn sync
        commandMap = [code: 'repoSync', id: CommandTestsHelper.createCommandId(), params: cmdParams,
            context: executionContext]
        command = AbstractCommand.makeCommand(classLoader, commandMap)
        replicaCommandExecutorService.commandLifecycleExecutor(command)

        if (command.executionException) {
            println command.executionException
            fail("Should be able to sync a command.")
        }

        // validate the file in the replica working copy
        boolean fileExists = false
        def fileRevNumber = -1
        for (int i = 0; i < 30 && !fileExists; i++) {
            command = [ConfigUtil.svnPath(), "up", wcReplica.canonicalPath,
                //"--username", username, "--password", password,
                "--non-interactive", "--no-auth-cache"] as String[]
            commandLineService.execute(command)
            fileExists = testFileReplica.exists()
            if (!fileExists) {
                Thread.sleep(1000)
            }
        }
        command = [ConfigUtil.svnPath(), "info", testFileReplica.canonicalPath,
            //"--username", username, "--password", password,
            "--non-interactive", "--no-auth-cache"] as String[]
        result = commandLineService.execute(command)
        log.info ("Info result in replica WC: \n" + result)
        matcher = result =~ /Last Changed Rev: (\d+)/
        fileRevNumber = matcher[0][1]

        assertTrue("Replicated test file should exist: " + testFileReplica.canonicalPath, fileExists)
        assertEquals("Replicated test file should have expected rev number: ${revNumberToAlter}", revNumberToAlter, fileRevNumber)

        // validate inital revprops on replica
        command = [ConfigUtil.svnPath(), "propget", "svn:log", "--revprop", "-r", revNumberToAlter, testFileReplica.canonicalPath,
               //"--username", username, "--password", password,
                "--non-interactive", "--no-auth-cache"] as String[]
        def output = commandLineService.execute(command)
        assertTrue("Test file should have the ORIGINAL commit message", output[1].contains(originalCommitMsg))

        // update revprop on master
        command = [ConfigUtil.svnPath(), "propset", "-r", revNumberToAlter, "--revprop", "svn:log", updatedCommitMsg, 
                testFileMaster.canonicalPath,
                "--username", username, "--password", password,
                "--non-interactive"] as String[]
        commandLineService.execute(command)

        testFileMaster.text = "updating test file"
        command = [ConfigUtil.svnPath(), "ci", testFileMaster.canonicalPath,
            "--username", username, "--password", password,
            "--non-interactive", "-m", originalCommitMsg] as String[]
        result = commandLineService.execute(command)
        log.info ("Commit result in master WC: \n" + result)
        matcher = result =~ /Committed revision (\d+)/
        def nextRevNumber = matcher[0][1]

        assertNotSame("The file revision number should be updated", revNumberToAlter, nextRevNumber)

        // execute svn sync
        cmdParams = [:]
        cmdParams["repoName"] = repoName

        commandMap = [code: 'repoSync', id:  CommandTestsHelper.createCommandId(), params: cmdParams,
            context: executionContext]
        command = AbstractCommand.makeCommand(classLoader, commandMap)
        replicaCommandExecutorService.commandLifecycleExecutor(command)

        if (command.executionException) {
            println command.executionException
            fail("Should be able to sync a command.")
        }

        // validate the file is synced in the replica working copy
        boolean fileRevUpdated = false
        fileRevNumber = "-1"
        for (int i = 0; i < 30 && !fileRevUpdated; i++) {
            command = [ConfigUtil.svnPath(), "up", wcReplica.canonicalPath,
               //"--username", username, "--password", password,
               "--non-interactive", "--no-auth-cache"] as String[]
            commandLineService.execute(command)

            command = [ConfigUtil.svnPath(), "info", testFileReplica.canonicalPath,
           //"--username", username, "--password", password,
           "--non-interactive", "--no-auth-cache"] as String[]
            result = commandLineService.execute(command)
            matcher = result =~ /Last Changed Rev: (\d+)/
            fileRevNumber = matcher[0][1]

            fileRevUpdated = fileRevNumber == nextRevNumber
            if (!fileRevUpdated) {
               Thread.sleep(2000)
            }
        }

        assertTrue ("The replica working copy should receive the new revision", fileRevUpdated)

        // validate revprops not updated
        command = [ConfigUtil.svnPath(), "propget", "svn:log", "--revprop", "-r", revNumberToAlter, testFileReplica.canonicalPath,
               //"--username", username, "--password", password,
                "--non-interactive", "--no-auth-cache"] as String[]
        output = commandLineService.execute(command)
        assertTrue("Test file should still have the ORIGINAL commit message", output[1].contains(originalCommitMsg))

        // execute revprop sync command
        cmdParams = [:]
        cmdParams["repoName"] = repoName
        cmdParams["revision"] = revNumberToAlter
        commandMap = [code: 'copyRevprops', id: CommandTestsHelper.createCommandId(), params: cmdParams,
            context: executionContext]
        command = AbstractCommand.makeCommand(classLoader, commandMap)
        replicaCommandExecutorService.commandLifecycleExecutor(command)
        Thread.sleep(5000)

        // validate updated revprops
        command = [ConfigUtil.svnPath(), "up", wcReplica.canonicalPath,
                //"--username", username, "--password", password,
                "--non-interactive", "--no-auth-cache"] as String[]
        commandLineService.execute(command)
        
        command = [ConfigUtil.svnPath(), "propget", "svn:log", "--revprop", "-r", revNumberToAlter, testFileReplica.canonicalPath,
               //"--username", username, "--password", password,
                "--non-interactive", "--no-auth-cache"] as String[]
        output = commandLineService.execute(command)

        assertTrue("Test file at previous rev should now have the UPDATED commit message", output[1].contains(updatedCommitMsg))

        // clean up test file
        command = [ConfigUtil.svnPath(), "delete", testFileMaster.canonicalPath,
            "--username", username, "--password", password,
            "--non-interactive"] as String[]
        output = commandLineService.execute(command)

        command = [ConfigUtil.svnPath(), "ci", wcMaster,
            "--username", username, "--password", password,
            "--non-interactive", "-m", originalCommitMsg] as String[]
        output = commandLineService.execute(command)

        wcMaster.deleteDir()
        wcReplica.deleteDir()
    }
}
