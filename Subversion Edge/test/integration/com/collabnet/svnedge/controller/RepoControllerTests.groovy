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
package com.collabnet.svnedge.controller

import java.io.File;

import org.codehaus.groovy.grails.commons.ConfigurationHolder

import grails.test.*

import com.collabnet.svnedge.TestUtil;
import com.collabnet.svnedge.console.DumpBean;
import com.collabnet.svnedge.controller.AuthzRulesCommand 
import com.collabnet.svnedge.domain.MailConfiguration;
import com.collabnet.svnedge.domain.Repository 
import com.collabnet.svnedge.domain.Server
import com.collabnet.svnedge.domain.User
import com.collabnet.svnedge.domain.integration.ApprovalState
import com.collabnet.svnedge.domain.integration.ReplicaConfiguration
import com.icegreen.greenmail.util.GreenMailUtil
import com.icegreen.greenmail.util.ServerSetupTest
import org.springframework.mock.web.MockMultipartHttpServletRequest
import org.springframework.mock.web.MockMultipartFile

class RepoControllerTests extends AbstractSvnEdgeControllerTests {

    def authenticateService
    def mailConfigurationService
    def svnRepoService
    def serverConfService
    def grailsApplication
    def greenMail
    def operatingSystemService
    def quartzScheduler
    def jobsAdminService
    def executorService    
    def replicaCommandSchedulerService

    def repoNameNew = "integration_test_new_repo"
    def repoNameExisting = "integration_test_existing_repo"
    
    def repoNew 
    def repoExisting 

    protected void setUp() {

        super.setUp()
        
        repoNew = new Repository(name: repoNameNew)
        repoExisting = new Repository(name: repoNameExisting)
        repoExisting.save()
       
        // make sure the supposedly new repo is not in the way
        svnRepoService.archivePhysicalRepository(repoNew)

        // make sure the supposedly existing repo is in the way
        svnRepoService.createRepository(repoExisting, true)

    }

    protected void tearDown() {
        super.tearDown()
        greenMail.deleteAllMessages()
        // cleanup repo
        svnRepoService.archivePhysicalRepository(repoNew)
        svnRepoService.archivePhysicalRepository(repoExisting)
    }

    void testIndex() {
        controller.index()
    }


    void testSave() {

        controller.svnRepoService = svnRepoService

        // this repo create should succeed
        controller.params.name = repoNameNew
        def model = controller.save()
        def redirArg = controller.redirectArgs["action"]
        
        def roles = authenticateService.principal()?.authorities?.authority
        def expectedAction = roles?.contains('ROLE_ADMIN') || 
                roles?.contains('ROLE_ADMIN_HOOKS') ? 'hooksList' : 'dumpFileList'
        assertEquals "Expected redirect to '" + expectedAction + "' view on successful repo " +
            "create", expectedAction, redirArg

        // this should fail (validation error)
        controller.save()
        assertTrue "Expected error for creating a known existing repo", 
            controller.request.repo.hasErrors()

        // this should fail (validation error)
        controller.params.name = repoNameExisting
        controller.save()
        assertTrue "Expected error for creating an unknown existing repo", 
            controller.request.repo.hasErrors()
    }

    void testEditAuthorization() {

        controller.serverConfService = serverConfService
        controller.metaClass.loggedInUserInfo = { return 1 }

        // should fetch an svn_access_file from services
        def model = controller.editAuthorization()
        assertNotNull "Expected 'authRulesCommand' model object", 
            model.authRulesCommand
    }

    void testSaveAuthorization() {

        controller.serverConfService = serverConfService
        controller.metaClass.loggedInUserInfo = { return 1 }

        // obtain lock
        controller.editAuthorization()

        // save the original file to restore after test
        String original = serverConfService.readSvnAccessFile()

        // content we will submit to controller
        String testFile = NEW_ACCESS_RULES
        def cmd = new AuthzRulesCommand(fileContent: testFile)
        controller.saveAuthorization(cmd)
        assertNotNull "Controller should provide a success message", 
            controller.flash.message
        assertNull "Controller should not return errors", controller.flash.error

        String modified = serverConfService.readSvnAccessFile()
        assertEquals "Access file should now equal the parameter given to " +
            "controller", testFile.trim(), modified

        // restore original
        serverConfService.writeSvnAccessFile(original)
    }
    
    private static final String NEW_ACCESS_RULES = """
[/]
* =
[/testrepo]
* = rw
"""
    
    void testSaveAuthorizationWithoutLock() {
        
        controller.serverConfService = serverConfService
        controller.metaClass.loggedInUserInfo = { return 1 }
        
        def cmd = new AuthzRulesCommand(fileContent: NEW_ACCESS_RULES)
        controller.saveAuthorization(cmd)
        assertNull "Controller should provide a success message",
                controller.flash.message
        assertNotNull "Controller should return an error", controller.flash.warn        
    }    

    private void changeEmailAddresses() {
        User user = User.get(1)
        user.email = "test@example.com"
        user.save(flush: true)
        
        Server server = Server.getServer()
        server.adminEmail = "testAdminMail@example.com"
        server.save(flush: true)
    }

    void testCreateDumpFileSuccessMail() {
        controller.metaClass.loggedInUserInfo = { return 1 }
        changeEmailAddresses()
        // make sure the quartz scheduler is running, is put in standby by other tests
        quartzScheduler.start()
        // but pause jobs likely to start running to ensure a thread is available
        jobsAdminService.pauseGroup("Statistics")

        ConfigurationHolder.config = grailsApplication.config
        MailConfiguration mailConfig = MailConfiguration.configuration
        mailConfig.port = ServerSetupTest.SMTP.port
        mailConfig.enabled = true
        mailConfigurationService.saveMailConfiguration(mailConfig)
        
        def dumpBean = new DumpBean()
        controller.params.id = repoExisting.id
        println "repoId=" + controller.params.id
        controller.createDumpFile(dumpBean)
        
        long startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() < startTime + 10000 &&
                greenMail.receivedMessages.length == 0) {
            Thread.sleep(1000)
            println "Waiting 1 s"
        }
        assertEquals("Expected one mail message ", 1,
            greenMail.receivedMessages.length)
        def message = greenMail.receivedMessages[0]
        assertEquals("Message Subject did not match",
                "[Success][Adhoc dump]Repository: " + repoExisting.name, 
                message.subject)
        assertTrue("Message Body did not match ", GreenMailUtil.getBody(message)
                .startsWith("The dump of repository '" + repoExisting.name + 
                "' completed."))        
    }    

    void testCreateDumpFileFailMail() {
        // this test relies on setting file system permissions
        if (operatingSystemService.isWindows()) {
            return
        }
        controller.metaClass.loggedInUserInfo = { return 1 }
        changeEmailAddresses()
        // make sure the quartz scheduler is running, is put in standby by other tests
        quartzScheduler.start()
        // but pause jobs likely to start running to ensure a thread is available
        jobsAdminService.pauseGroup("Statistics")

        ConfigurationHolder.config = grailsApplication.config
        MailConfiguration mailConfig = MailConfiguration.configuration
        mailConfig.port = ServerSetupTest.SMTP.port
        mailConfig.enabled = true
        boolean b = mailConfigurationService.saveMailConfiguration(mailConfig)
        if (!b) {
            fail "Should not be validation errors: " + mailConfig.errors.dump()
        }
        
        // It would be good to use a test directory here, but svnRepoService is
        // non-transactional, so that won't work
        Server server = Server.getServer()
        File dumpDir = new File(server.dumpDir, repoExisting.name)
        if (!dumpDir.exists()) {
            dumpDir.mkdirs()
        }
        dumpDir.setWritable(false)
        
        def dumpBean = new DumpBean()
        controller.params.id = repoExisting.id
        controller.createDumpFile(dumpBean)
        
        long startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() < startTime + 10000 &&
                greenMail.receivedMessages.length == 0) {
            Thread.sleep(1000)
        }
        dumpDir.setWritable(true)        
        
        assertEquals("Expected one mail message with two recipients", 2,
            greenMail.receivedMessages.length)
        def message = greenMail.receivedMessages[0]
        assertEquals("Message Subject did not match",
                "[Error][Adhoc dump]Repository: " + repoExisting.name, 
                message.subject)
        assertTrue("Message Body did not match ", GreenMailUtil.getBody(message)
                .startsWith("The dump of repository '" + repoExisting.name + 
                "' failed."))        
    }    

    private static final def HOOKS_DIR_CONTENTS = [
        'post-commit.tmpl', 'post-lock.tmpl', 'post-revprop-change.tmpl', 
        'post-unlock.tmpl', 'pre-commit.tmpl', 'pre-lock.tmpl', 
        'pre-revprop-change.tmpl', 'pre-unlock.tmpl', 'start-commit.tmpl']
    
    void testHooksList() {
        def params = controller.params
        params.id = repoExisting.id
        // default order
        def hooksList = controller.hooksList()['hooksList']
        assertEquals "Hook scripts should be in alphabetical order", 
                HOOKS_DIR_CONTENTS, hooksList.collect { it.name }
                
        // reverse it
        params.sortBy= 'name'
        params.order = 'desc'
        hooksList = controller.hooksList()['hooksList']
        assertEquals "Hook scripts should be in descending alphabetical order", 
                HOOKS_DIR_CONTENTS.reverse(), hooksList.collect { it.name }
    }
    
    void testCopyHook() {
        def params = controller.params
        params.id = repoExisting.id
        params['listViewItem_post-commit.tmpl'] = "on"

        def invalidHookName = "invalid/hook/name"        
        params['_confirmDialogText_copyHook'] = invalidHookName
        controller.copyHook()
        assertNull "Did not expect a success message", controller.flash.message
        assertNotNull "There should be an error message", controller.flash.error
        assertEquals "Validation failed on hook copy file name", 
                controller.message(code: 'repository.name.matches.invalid'),
                controller.flash.error.replace("'", "''")
        controller.flash.error = null
            
        def validHookName = "valid_hook_name"
        params['_confirmDialogText_copyHook'] = validHookName
        controller.copyHook()
        assertNotNull "Expected a success message", controller.flash.message
        assertNull "There should not be an error message: " + 
                controller.flash.error, controller.flash.error
        def model = controller.hooksList()
        def files = model["hooksList"]
        File copyFile = null
        files.each { 
            if (validHookName == it.name) {
                copyFile = it
            }
        }
        assertNotNull "Did not find copied file", copyFile
        assertTrue "File should be executable", copyFile.canExecute()
        controller.flash.message = null
        
        // try again, now it should fail as the file already exists
        controller.copyHook()
        assertNull "Did not expect a success message", controller.flash.message
        assertNotNull "There should be an error message", controller.flash.error
        assertTrue "Error message should indicate file exists", 
                controller.flash.error.contains("already exists")
    }

    void testRenameHook() {
        def params = controller.params
        params.id = repoExisting.id
        def existingFilename = 'post-commit.tmpl'
        params['listViewItem_' + existingFilename] = "on"

        def invalidHookName = "invalid/hook/name"        
        params['_confirmDialogText_renameHook'] = invalidHookName
        controller.renameHook()
        assertNull "Did not expect a success message", controller.flash.message
        assertNotNull "There should be an error message", controller.flash.error
        assertEquals "Validation failed on new file name", 
                controller.message(code: 'repository.name.matches.invalid'),
                controller.flash.error.replace("'", "''")
        controller.flash.error = null
            
        def validHookName = "valid_hook_name"
        params['_confirmDialogText_renameHook'] = validHookName
        controller.renameHook()
        assertNotNull "Expected a success message", controller.flash.message
        assertNull "There should not be an error message: " + 
                controller.flash.error, controller.flash.error
        def model = controller.hooksList()
        def files = model["hooksList"]
        File oldFile = null
        File newFile = null
        files.each { 
            if (validHookName == it.name) {
                newFile = it
            }
            if (existingFilename == it.name) {
                oldFile = it
            }
        }
        assertNull "Original file still exists", oldFile
        assertNotNull "Did not find renamed file", newFile
        assertTrue "File should be executable", newFile.canExecute()
        controller.flash.message = null
        
        // try again, now it should fail as the file already exists
        params['listViewItem_' + existingFilename] = "off"
        existingFilename = 'pre-commit.tmpl'
        params['listViewItem_' + existingFilename] = "on"
        controller.renameHook()
        assertNull "Did not expect a success message", controller.flash.message
        assertNotNull "There should be an error message", controller.flash.error
        assertTrue "Error message should indicate file exists: " + 
                controller.flash.error, 
                controller.flash.error.contains("already exists")
    }
    
    void testDeleteHook() {
        def params = controller.params
        params.id = repoExisting.id
        def existingFilename = 'post-commit.tmpl'
        params['listViewItem_' + existingFilename] = "on"

        controller.deleteHook()
        assertNull "There should not be an error message", controller.flash.error
        assertNotNull "Expected a success message", controller.flash.message
        assertEquals "Expected deleted success message",
                controller.message(code: 'repository.action.deleteHook.success',
                                   args: [existingFilename]),
                controller.flash.message            
        def model = controller.hooksList()
        def files = model["hooksList"]
        File oldFile = null
        files.each {
            if (existingFilename == it.name) {
                oldFile = it
            }
        }
        assertNull "File still exists", oldFile
        controller.flash.message = null
        
        // try again, now it should fail as the file was already removed
        params['listViewItem_' + existingFilename] = "on"
        controller.deleteHook()
        assertNull "Did not expect a success message", controller.flash.message
        assertNotNull "There should be an error message", controller.flash.error
        assertTrue "Error message should indicate file does not exist: " +
                controller.flash.error,
                controller.flash.error.contains("could not be found")
    }

    void testEditHook() {
        controller.metaClass.loggedInUserInfo = { return 1 }
        def params = controller.params
        params.id = repoExisting.id
        params['listViewItem_post-commit.tmpl'] = "on"

        def model = controller.editHook()
        def originalFileText = svnRepoService.getHookFile(repoExisting, "post-commit.tmpl").text
        
        assertNotNull "Model should contain a fileId", model.fileId
        assertNotNull "Model should contain hook content", model.fileContent
        assertEquals "File content in model should match disk", originalFileText, model.fileContent

        def editedFileContent = "new content"
        params.fileId = model.fileId
        params.fileContent = editedFileContent
        controller.saveHook()

        def newFileText = svnRepoService.getHookFile(repoExisting, "post-commit.tmpl").text
        assertEquals "File content on disk should be updated", editedFileContent, newFileText

        // restore script file to original text
        svnRepoService.getHookFile(repoExisting, "post-commit.tmpl").text = originalFileText
    }

    void testUploadHook() {
        def fieldName = 'fileUpload'
        def originalFileName = 'my-hook-script-file'
        def contentType = 'text/plain'
        def content = '!#/bin/sh etc etc'
        controller.metaClass.request = new MockMultipartHttpServletRequest()
        controller.request.addFile(new MockMultipartFile(
                fieldName, originalFileName, contentType, content as byte[]))
        controller.params.id = repoExisting.id
        controller.svnRepoService = new Expando()
        controller.svnRepoService.createHook = { p1, p2, p3 -> true }
       
        def model = controller.uploadHook()
        assertEquals "Expected upload success message",
                controller.message(code: 'repository.page.hookCreate.upload.success', args: [originalFileName, repoExisting.name]),
                controller.flash.message
        
        controller.svnRepoService = svnRepoService
    }
    
    private void setupReplica() {
        def rConf = ReplicaConfiguration.currentConfig
        if (!rConf) {
            rConf = new ReplicaConfiguration(svnMasterUrl: null,
                name: "Test Replica", description: "Super replica",
                message: "Auto-approved", systemId: "replica1001",
                commandPollRate: 5, approvalState: ApprovalState.APPROVED)
            rConf.save()
        }
        executorService.execute {
            replicaCommandSchedulerService.schedulerSynchronizer.take()
        }
    }
    
    void testReplicaSyncRepo() {
        setupReplica()
        controller.params.id = repoExisting.id
        def model = controller.replicaSyncRepo()
        assertEquals "Expected sync scheduled success message",
                controller.message(code: 'repository.page.action.replicaSyncRepo.success'),
                controller.flash.message
    }

    void testReplicaSyncRevprops() {
        setupReplica()
        controller.params.id = repoExisting.id
        def model = controller.replicaSyncRevprops()
        assertEquals "Expected sync scheduled success message",
                controller.message(code: 'repository.page.action.replicaSyncRevprops.success'),
                controller.flash.message
    }

}
