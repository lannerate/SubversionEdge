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
package com.collabnet.svnedge.console.services

import javax.mail.internet.MimeMultipart

import grails.test.GrailsUnitTestCase

import com.collabnet.svnedge.TestUtil
import com.collabnet.svnedge.console.CommandLineService
import com.collabnet.svnedge.console.LifecycleService
import com.collabnet.svnedge.console.OperatingSystemService
import com.collabnet.svnedge.console.SvnRepoService
import com.collabnet.svnedge.console.DumpBean
import com.collabnet.svnedge.domain.MailConfiguration
import com.collabnet.svnedge.domain.Repository
import com.collabnet.svnedge.domain.Server
import com.collabnet.svnedge.util.ConfigUtil
import com.icegreen.greenmail.util.GreenMailUtil
import com.icegreen.greenmail.util.ServerSetupTest

import org.apache.commons.io.FileUtils;
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import com.collabnet.svnedge.console.SchedulerBean

class SvnRepoServiceIntegrationTests extends GrailsUnitTestCase {

    def grailsApplication
    def greenMail
    def mailConfigurationService
    SvnRepoService svnRepoService
    CommandLineService commandLineService
    OperatingSystemService operatingSystemService
    LifecycleService lifecycleService
    def jobsAdminService
    def quartzScheduler
    def repoParentDir
    boolean initialStarted

    protected void setUp() {
        super.setUp()
        initialStarted = lifecycleService.isStarted()
        // start tests with the server off
        if (initialStarted) {
            lifecycleService.stopServer()
        }

        // Setup a test repository parent
        repoParentDir = TestUtil.createTestDir("repo")
        Server server = lifecycleService.getServer()
        server.repoParentDir = repoParentDir.getCanonicalPath()
        server.adminEmail = "testAdminMail@example.com"
        server.save()


    }

    protected void tearDown() {
        super.tearDown()
        repoParentDir.deleteDir()
        greenMail.deleteAllMessages()
    }


    void testUpdateRepositoryPermissions() {

        // first create a repository
        def testRepository = new Repository(name: "testrepo")

        int exitStatus = svnRepoService.createRepository(testRepository, false)
        // assertEquals("Create Repo should succeed", 0, exitStatus)

        svnRepoService.updateRepositoryPermissions(testRepository)

    }

    void testCreateRepository() {
        def testRepoName = "lifecycle-test"
        Repository repo = createRepository(testRepoName)

        // checkout the repo
        def wcDir = TestUtil.createTestDir("wc")
        def testRepoFile = new File(wcDir, testRepoName)
        def status = commandLineService.executeWithStatus(
                ConfigUtil.svnPath(), "checkout",
                "--no-auth-cache", "--non-interactive", commandLineService.createSvnFileURI(new File(repoParentDir, testRepoName)),
                testRepoFile.canonicalPath)
        assertEquals "Failed to checkout repository.", 0, status
        def topDirs = testRepoFile.listFiles()
        def expectedDirs = ['.svn', 'branches', 'tags', 'trunk']
        assertEquals "Wrong number of files." + topDirs, 4, topDirs.length
        for (d in expectedDirs) {
            boolean b = false
            for (td in topDirs) {
                if (d == td.name) {
                    b = true
                    break
                }
            }
            assertTrue "Expected '" + d + "' directory not found", b
        }
        wcDir.deleteDir()
    }

    private Repository createRepository(repoName) {
        Repository repo = new Repository(name: repoName)
        assertEquals "Failed to create repository.", 0,
                svnRepoService.createRepository(repo, true)
        repo.save()
        return repo
    }
    
    public void testDump() {
        Repository repo = createRepository("test-dump")
        deleteProgressFile(repo)

        DumpBean params = new DumpBean()
        params.compress = false
        File dumpFile = createDump(params, repo)
        String contents = dumpFile.text
        assertTrue "Missing trunk in dump", contents.contains("Node-path: trunk")
        assertTrue "Missing branches in dump", contents.contains("Node-path: branches")
        assertTrue "Missing tags in dump", contents.contains("Node-path: tags")

        // test exclusion filter
        params = new DumpBean()
        params.compress = false
        params.filter = true
        params.excludePath = "branches"
        File dumpFile2 = createDump(params, repo)
        contents = dumpFile2.text
        assertTrue "Missing trunk in dump", contents.contains("Node-path: trunk")
        assertFalse "branches exists in dump", contents.contains("Node-path: branches")
        assertTrue "Missing tags in dump", contents.contains("Node-path: tags")
        Thread.sleep(1000)

        // test inclusion filter
        params = new DumpBean()
        params.compress = false
        params.filter = true
        params.includePath = "trunk tags"
        File dumpFile3 = createDump(params, repo)
        contents = dumpFile3.text
        assertTrue "Missing trunk in dump", contents.contains("Node-path: trunk")
        assertFalse "branches exists in dump", contents.contains("Node-path: branches")
        assertTrue "Missing tags in dump", contents.contains("Node-path: tags")

        dumpFile.delete()
        dumpFile2.delete()
        dumpFile3.delete()
    }

    private File newDumpFile(filename, repo) {
        return new File(new File(Server.getServer().dumpDir, repo.name), filename)
    }

    private File createDump(params, repo) {
        def filename = svnRepoService.createDump(params, repo)
        File dumpFile = newDumpFile(filename, repo)
        assertTrue "Dump file does not exist: " + dumpFile.name, dumpFile.exists()
        return dumpFile
    }

    private void deleteProgressFile(repo) {
        // Earlier checkin was not deleting this file, so make sure it is
        // gone before testing
        File tempLogDir = new File(ConfigUtil.logsDirPath(), "temp")
        File progressLogFile =
        new File(tempLogDir, "dump-progress-" + repo.name + ".log")
        if (progressLogFile.exists()) {
            progressLogFile.delete()
        }
    }

    
    public void testHotcopy() {
        Repository repo = createRepository("test-hotcopy")
        if (!operatingSystemService.isWindows()) {
            File hookScript = new File(svnRepoService.getRepositoryHomePath(repo),
                    "hooks/pre-commit.tmpl")
            hookScript.setExecutable(true)
        }
        deleteProgressFile(repo)
        
        DumpBean params = new DumpBean()
        File dumpFile = createHotcopy(params, repo)

        if (!operatingSystemService.isWindows()) {
            // not sure how to test a native zip extraction on windows

            File extractDir = TestUtil.createTestDir("hotcopy")
            def exitStatus = commandLineService.executeWithStatus(
                    "unzip", dumpFile.canonicalPath, "-d", extractDir.canonicalPath)
            assertEquals "Unable to extract archive", 0, exitStatus

            File copiedHookScript = new File(extractDir, "hooks/pre-commit.tmpl")
            assertTrue "pre-commit.tmpl does not exist", copiedHookScript.exists()
            if (!operatingSystemService.isWindows()) {
                assertTrue "pre-commit.tmpl should be executable",
                        copiedHookScript.canExecute()
            }
            def contents = ['conf', 'db', 'format', 'hooks', 'locks', 'README.txt']
            int i = 0
            extractDir.eachFile { f ->
                assertTrue "Found unexpected file/dir: " + f.name,
                        contents.contains(f.name)
                i++
            }
            assertEquals "Number of files/directories did not match",
                    contents.size(), i

            extractDir.deleteDir()
        }

        dumpFile.delete()
    }

    private File createHotcopy(params, repo) {
        def filename = svnRepoService.createHotcopy(params, repo)
        File dumpFile = newDumpFile(filename, repo)
        assertTrue "Dump file does not exist: " + dumpFile.name, dumpFile.exists()
        return dumpFile
    }

    public void testBackupPruning() {
        Repository repo = createRepository("test-backup-pruning")
        deleteProgressFile(repo)
        svnRepoService.listDumpFiles(repo).each  { it.delete() }

        def dumpFiles = []
        DumpBean params = new DumpBean()
        params.backup = true
        params.compress = true
        params.numberToKeep = 2
        dumpFiles << createDump(params, repo)
        Thread.sleep(1000)
        dumpFiles << createDump(params, repo)
        assertFilesExist(dumpFiles)
        
        def hotcopyFiles = []
        params = new DumpBean()
        params.backup = true
        params.numberToKeep = 3
        hotcopyFiles << createHotcopy(params, repo)
        Thread.sleep(1000)
        hotcopyFiles << createHotcopy(params, repo)
        Thread.sleep(1000)
        hotcopyFiles << createHotcopy(params, repo)
        assertFilesExist(hotcopyFiles)
        assertFilesExist(dumpFiles)
        
        File oldDumpFile = dumpFiles.remove(0)
        params = new DumpBean()
        params.backup = true
        params.compress = true
        params.numberToKeep = 2
        dumpFiles << createDump(params, repo)
        assertFalse "Dump file ${oldDumpFile} expected to be pruned", 
                oldDumpFile.exists()
        assertFilesExist(dumpFiles)
        assertFilesExist(hotcopyFiles)
        
        Thread.sleep(1000)
        File oldHotcopyFile = hotcopyFiles.remove(0)
        params = new DumpBean()
        params.backup = true
        params.numberToKeep = 3
        hotcopyFiles << createHotcopy(params, repo)
        assertFalse "Hotcopy file ${oldHotcopyFile} expected to be pruned", 
                oldHotcopyFile.exists()
        assertFilesExist(dumpFiles)
        assertFilesExist(hotcopyFiles)
        
        dumpFiles.each { it.delete() }
        hotcopyFiles.each { it.delete() }
    }

    private void assertFilesExist(files) {
        files.each {
            assertTrue "Backup file ${it} still expected to exist", it.exists()
        }
    }

    public void testLoad() {

        // upper limit on time to run async code
        long timeLimit = System.currentTimeMillis() + 30000
        // make sure the quartz scheduler is running, is put in standby by other tests
        quartzScheduler.start()
        // but pause jobs likely to start running to ensure a thread is available
        jobsAdminService.pauseGroup("Statistics")

        // create a dump file of a src repo with branches/tags/trunk nodes
        def testRepoNameSrc = "load-test-src"
        Repository repoSrc = new Repository(name: testRepoNameSrc)
        assertEquals "Failed to create src repository.", 0,
                svnRepoService.createRepository(repoSrc, true)
        repoSrc.save()

        DumpBean params = new DumpBean()
        params.compress = false
        def filename = svnRepoService.createDump(params, repoSrc)
        File dumpFile = newDumpFile(filename, repoSrc)
        // Async so wait for it
        while (!dumpFile.exists() && System.currentTimeMillis() < timeLimit) {
            Thread.sleep(250)
        }
        assertTrue "Dump file does not exist: " + dumpFile.name, dumpFile.exists()
        log.info "The dumpfile original location: '${dumpFile.absolutePath}'"
        String contents = dumpFile.text
        assertTrue "Missing trunk in dump", contents.contains("Node-path: trunk")
        assertTrue "Missing branches in dump", contents.contains("Node-path: branches")
        assertTrue "Missing tags in dump", contents.contains("Node-path: tags")

        // create a target repo WITHOUT branches/tags/trunk nodes
        def testRepoNameTarget = "load-test-target"

        Repository repoTarget = new Repository(name: testRepoNameTarget)
        assertEquals "Failed to create target repository.", 0,
                svnRepoService.createRepository(repoTarget, false)
        repoTarget.save(flush: true)

        def output = commandLineService.executeWithOutput(
                ConfigUtil.svnPath(), "info",
                "--no-auth-cache", "--non-interactive",
                commandLineService.createSvnFileURI(new File(repoParentDir, testRepoNameTarget)) + "trunk")

        // verify no 'trunk' folder
        assertFalse "svn info output should not contain node info for 'trunk'", output.contains("Node Kind: directory")

        // move src dump file to the expected load location for target
        File loadDir = svnRepoService.getLoadDirectory(repoTarget)
        // delete any residual load files
        loadDir.eachFile {
            it.delete()
        }
        File loadFile = new File(loadDir, dumpFile.name)
        boolean loadFileCreated = dumpFile.renameTo(loadFile)
        assertTrue "should be able to move dumpfile to load directory", loadFileCreated
        assertTrue "loadFile should exist", loadFile.exists()
        log.info "The loadfile to be imported to the target repo: '${loadFile.absolutePath}'"

        // load it
        def tempLogDir = new File(ConfigUtil.logsDirPath(), "temp")
        def progressFile = File.createTempFile("load-progress", ".txt", tempLogDir)
        def options = ["progressLogFile": progressFile.absolutePath]
        options << ["ignoreUuid": false]
        svnRepoService.scheduleLoad(repoTarget, options)

        // verify that repo load has run (trunk/tags/branches which should have been imported)
        boolean loadSuccess = false
        timeLimit = System.currentTimeMillis() + 60000
        while (!loadSuccess && System.currentTimeMillis() < timeLimit) {
            Thread.sleep(5000)
            output = commandLineService.executeWithOutput(
                    ConfigUtil.svnPath(), "info",
                    "--no-auth-cache", "--non-interactive",
                    commandLineService.createSvnFileURI(new File(repoParentDir, testRepoNameTarget)) + "trunk")

            loadSuccess = output.contains("Node Kind: directory")
        }

        assertFalse "load file should be deleted after loading", loadFile.exists()
        assertTrue "the target repo should now have nodes from the src repo after loading", loadSuccess
    }

    public void testLoadZip() {

        // upper limit on time to run async code
        long timeLimit = System.currentTimeMillis() + 30000
        // make sure the quartz scheduler is running, is put in standby by other tests
        quartzScheduler.start()
        // but pause jobs likely to start running to ensure a thread is available
        jobsAdminService.pauseGroup("Statistics")

        // create a dump file of a src repo with branches/tags/trunk nodes
        def testRepoNameSrc = "load-test-src"
        Repository repoSrc = new Repository(name: testRepoNameSrc)
        assertEquals "Failed to create src repository.", 0,
                svnRepoService.createRepository(repoSrc, true)
        repoSrc.save()

        DumpBean params = new DumpBean()
        params.compress = true
        def filename = svnRepoService.createDump(params, repoSrc)
        File dumpFile = newDumpFile(filename, repoSrc)
        // Async so wait for it
        while (!dumpFile.exists() && System.currentTimeMillis() < timeLimit) {
            Thread.sleep(250)
        }
        assertTrue "Dump file does not exist: " + dumpFile.name, dumpFile.exists()
        assertTrue "The Dump file should be a zip file", dumpFile.name.endsWith(".zip")
        log.info "The dumpfile original location: '${dumpFile.absolutePath}'"

        // create a target repo WITHOUT branches/tags/trunk nodes
        def testRepoNameTarget = "load-test-target"

        Repository repoTarget = new Repository(name: testRepoNameTarget)
        assertEquals "Failed to create target repository.", 0,
                svnRepoService.createRepository(repoTarget, false)
        repoTarget.save(flush: true)

        def output = commandLineService.executeWithOutput(
                ConfigUtil.svnPath(), "info",
                "--no-auth-cache", "--non-interactive",
                commandLineService.createSvnFileURI(new File(repoParentDir, testRepoNameTarget)) + "trunk")

        // verify no 'trunk' folder
        assertFalse "svn info output should not contain node info for 'trunk'", output.contains("Node Kind: directory")

        // move src dump file to the expected load location for target
        File loadDir = svnRepoService.getLoadDirectory(repoTarget)
        // delete any residual load files
        loadDir.eachFile {
            it.delete()
        }
        File loadFile = new File(loadDir, dumpFile.name)
        boolean loadFileCreated = dumpFile.renameTo(loadFile)
        assertTrue "should be able to move dumpfile to load directory", loadFileCreated
        assertTrue "loadFile should exist", loadFile.exists()
        log.info "The loadfile to be imported to the target repo: '${loadFile.absolutePath}'"

        // load it
        def tempLogDir = new File(ConfigUtil.logsDirPath(), "temp")
        def progressFile = File.createTempFile("load-progress", ".txt", tempLogDir)
        def options = ["progressLogFile": progressFile.absolutePath]
        options << ["ignoreUuid": false]
        svnRepoService.scheduleLoad(repoTarget, options)

        // verify that repo load has run (trunk/tags/branches which should have been imported)
        boolean loadSuccess = false
        timeLimit = System.currentTimeMillis() + 60000
        while (!loadSuccess && System.currentTimeMillis() < timeLimit) {
            Thread.sleep(5000)
            output = commandLineService.executeWithOutput(
                    ConfigUtil.svnPath(), "info",
                    "--no-auth-cache", "--non-interactive",
                    commandLineService.createSvnFileURI(new File(repoParentDir, testRepoNameTarget)) + "trunk")

            loadSuccess = output.contains("Node Kind: directory")
        }

        assertFalse "load file should be deleted after loading", loadFile.exists()
        assertTrue "the target repo should now have nodes from the src repo after loading", loadSuccess
    }

    public void testLoadHotcopy() {

        // upper limit on time to run async code
        long timeLimit = System.currentTimeMillis() + 30000
        // make sure the quartz scheduler is running, is put in standby by other tests
        quartzScheduler.start()
        // but pause jobs likely to start running to ensure a thread is available
        jobsAdminService.pauseGroup("Statistics")

        // create a hotcopy backup  file of a src repo with branches/tags/trunk nodes

        def testRepoNameSrc = "load-test-src"
        Repository repoSrc = new Repository(name: testRepoNameSrc)
        assertEquals "Failed to create src repository.", 0,
                svnRepoService.createRepository(repoSrc, true)
        repoSrc.save()
        String sourceUuid = svnRepoService.getReposUUID(repoSrc)

        // setup to test permissions retention on *nix
        if (!operatingSystemService.isWindows()) {
            File repoHome = new File(svnRepoService.getRepositoryHomePath(repoSrc))
            File hooks = new File(repoHome, "hooks")
            File startCommit = new File(hooks, "start-commit.tmpl")
            def chmodCmd = ["chmod", "a+x", "${startCommit.absolutePath}"]
            String[] result = commandLineService.execute(chmodCmd, null, null)
            boolean setModeSuccess = result[0] == "0"
            assertTrue "the start-commit.tmpl should be executable", setModeSuccess
            assertTrue "the start-commit.tmpl should be executable", startCommit.canExecute()
        }

        DumpBean params = new DumpBean()
        params.compress = true
        def filename = svnRepoService.createHotcopy(params, repoSrc)
        File dumpFile = newDumpFile(filename, repoSrc)
        // Async so wait for it
        while (!dumpFile.exists() && System.currentTimeMillis() < timeLimit) {
            Thread.sleep(250)
        }
        assertTrue "Dump file does not exist: " + dumpFile.name, dumpFile.exists()
        assertTrue "The Dump file should be a zip file", dumpFile.name.endsWith(".zip")
        log.info "The dumpfile original location: '${dumpFile.absolutePath}'"

        // test loading hotcopy as backup (keep UUID from the file)

        // create a target repo WITHOUT branches/tags/trunk nodes
        def testRepoNameTarget = "load-test-target"

        Repository repoTarget = new Repository(name: testRepoNameTarget)
        assertEquals "Failed to create target repository.", 0,
                svnRepoService.createRepository(repoTarget, false)
        repoTarget.save(flush: true)
        String targetUuid = svnRepoService.getReposUUID(repoTarget)

        def output = commandLineService.executeWithOutput(
                ConfigUtil.svnPath(), "info",
                "--no-auth-cache", "--non-interactive",
                commandLineService.createSvnFileURI(new File(repoParentDir, testRepoNameTarget)) + "trunk")

        // verify no 'trunk' folder
        assertFalse "svn info output should not contain node info for 'trunk'", output.contains("Node Kind: directory")

        // move src dump file to the expected load location for target
        File loadDir = svnRepoService.getLoadDirectory(repoTarget)
        // delete any residual load files
        loadDir.eachFile {
            it.delete()
        }
        File loadFile = new File(loadDir, dumpFile.name)
        FileUtils.copyFile(dumpFile, loadFile)
        assertTrue "loadFile should exist", loadFile.exists()
        log.info "The loadfile to be imported to the target repo: '${loadFile.absolutePath}'"

        // load it
        def tempLogDir = new File(ConfigUtil.logsDirPath(), "temp")
        def progressFile = File.createTempFile("load-progress", ".txt", tempLogDir)
        def options = ["progressLogFile": progressFile.absolutePath]
        options << ["ignoreUuid": false]   // our target repo should get it's UUID from the hotcopy
        svnRepoService.scheduleLoad(repoTarget, options)

        // verify that repo load has run (trunk/tags/branches which should have been imported)
        boolean loadSuccess = false
        timeLimit = System.currentTimeMillis() + 60000
        while (!loadSuccess && System.currentTimeMillis() < timeLimit) {
            Thread.sleep(5000)
            output = commandLineService.executeWithOutput(
                    ConfigUtil.svnPath(), "info",
                    "--no-auth-cache", "--non-interactive",
                    commandLineService.createSvnFileURI(new File(repoParentDir, testRepoNameTarget)) + "trunk")

            loadSuccess = output.contains("Node Kind: directory")
        }


        assertFalse "load file should be deleted after loading", loadFile.exists()
        assertTrue "the target repo should now have nodes from the src repo after loading", loadSuccess
        assertEquals "the target repo should now have the original source UUID", sourceUuid, svnRepoService.getReposUUID(repoTarget)

        // test permissions retention on *nix
        if (!operatingSystemService.isWindows()) {
            File repoHome = new File(svnRepoService.getRepositoryHomePath(repoTarget))
            File hooks = new File(repoHome, "hooks")
            File startCommit = new File(hooks, "start-commit.tmpl")
            boolean setModeSuccess = startCommit.canExecute()
            assertTrue "the start-commit.tmpl of the new repo should be executable", setModeSuccess
        }

        // test loading hotcopy as template (with UUID of target repo preserved)

        // create a target repo WITHOUT branches/tags/trunk nodes
        testRepoNameTarget = "load-test-target2"

        repoTarget = new Repository(name: testRepoNameTarget)
        assertEquals "Failed to create target repository.", 0,
                svnRepoService.createRepository(repoTarget, false)
        repoTarget.save(flush: true)
        targetUuid = svnRepoService.getReposUUID(repoTarget)

        output = commandLineService.executeWithOutput(
                ConfigUtil.svnPath(), "info",
                "--no-auth-cache", "--non-interactive",
                commandLineService.createSvnFileURI(new File(repoParentDir, testRepoNameTarget)) + "trunk")

        // verify no 'trunk' folder
        assertFalse "svn info output should not contain node info for 'trunk'", output.contains("Node Kind: directory")

        // move src dump file to the expected load location for target
        loadDir = svnRepoService.getLoadDirectory(repoTarget)
        // delete any residual load files
        loadDir.eachFile {
            it.delete()
        }
        loadFile = new File(loadDir, dumpFile.name)
        FileUtils.copyFile(dumpFile, loadFile)
        assertTrue "loadFile should exist", loadFile.exists()
        log.info "The loadfile to be imported to the target repo: '${loadFile.absolutePath}'"

        // load it
        tempLogDir = new File(ConfigUtil.logsDirPath(), "temp")
        progressFile = File.createTempFile("load-progress", ".txt", tempLogDir)
        options = ["progressLogFile": progressFile.absolutePath]
        options << ["ignoreUuid": true]   // our target repo should keep its original uuid
        svnRepoService.scheduleLoad(repoTarget, options)

        // verify that repo load has run (trunk/tags/branches which should have been imported)
        Thread.sleep(5000)
        loadSuccess = false
        timeLimit = System.currentTimeMillis() + 120000
        while (!loadSuccess && System.currentTimeMillis() < timeLimit) {
            output = commandLineService.executeWithOutput(
                    ConfigUtil.svnPath(), "info",
                    "--no-auth-cache", "--non-interactive",
                    commandLineService.createSvnFileURI(new File(repoParentDir, testRepoNameTarget)) + "trunk")

            loadSuccess = output.contains("Node Kind: directory")
            Thread.sleep(10000)
        }

        assertTrue "the target repo should now have nodes from the src repo after loading", loadSuccess
        assertFalse "load file should be deleted after loading", loadFile.exists()
        assertEquals "the target repo should still have its own UUID", targetUuid, svnRepoService.getReposUUID(repoTarget)

    }

    public void testHotcopyWithSymlinks() {

        // only test *nix
        if (operatingSystemService.isWindows()) {
            return
        }

        // upper limit on time to run async code
        long timeLimit = System.currentTimeMillis() + 30000
        // make sure the quartz scheduler is running, is put in standby by other tests
        quartzScheduler.start()
        // but pause jobs likely to start running to ensure a thread is available
        jobsAdminService.pauseGroup("Statistics")

        // create a hotcopy backup  file of a src repo with branches/tags/trunk nodes
        def testRepoNameSrc = "hotcopy-src"
        Repository repoSrc = new Repository(name: testRepoNameSrc)
        assertEquals "Failed to create src repository.", 0,
                svnRepoService.createRepository(repoSrc, true)
        repoSrc.save()

        // setup symlink
        File repoHome = new File(svnRepoService.getRepositoryHomePath(repoSrc))
        File hooks = new File(repoHome, "hooks")
        File startCommitTmpl = new File(hooks, "start-commit.tmpl")
        File startCommitLink = new File(hooks, "start-commit")
        assertFalse "the link should not yet exist", startCommitLink.exists()
        def linkCmd = ["ln", "-s", "${startCommitTmpl.absolutePath}", "${startCommitLink.absolutePath}"]
        String[] result = commandLineService.execute(linkCmd, null, null)
        assertTrue "the link should be created", result[0] == "0"

        DumpBean params = new DumpBean()
        params.compress = true
        def filename = svnRepoService.createHotcopy(params, repoSrc)
        File dumpFile = newDumpFile(filename, repoSrc)
        // Async so wait for it
        while (!dumpFile.exists() && System.currentTimeMillis() < timeLimit) {
            Thread.sleep(250)
        }
        assertTrue "Dump file does not exist: " + dumpFile.name, dumpFile.exists()
        assertTrue "The Dump file should be a zip file", dumpFile.name.endsWith(".zip")
        log.info "The dumpfile original location: '${dumpFile.absolutePath}'"

        // test loading hotcopy

        // create a target repo WITHOUT branches/tags/trunk nodes
        def testRepoNameTarget = "hotcopy-target"

        Repository repoTarget = new Repository(name: testRepoNameTarget)
        assertEquals "Failed to create target repository.", 0,
                svnRepoService.createRepository(repoTarget, false)
        repoTarget.save(flush: true)

        def output = commandLineService.executeWithOutput(
                ConfigUtil.svnPath(), "info",
                "--no-auth-cache", "--non-interactive",
                commandLineService.createSvnFileURI(new File(repoParentDir, testRepoNameTarget)) + "trunk")

        // verify no 'trunk' folder
        assertFalse "svn info output should not contain node info for 'trunk'", output.contains("Node Kind: directory")

        // move src dump file to the expected load location for target
        File loadDir = svnRepoService.getLoadDirectory(repoTarget)
        // delete any residual load files
        loadDir.eachFile {
            it.delete()
        }
        File loadFile = new File(loadDir, dumpFile.name)
        FileUtils.copyFile(dumpFile, loadFile)
        assertTrue "loadFile should exist", loadFile.exists()
        log.info "The loadfile to be imported to the target repo: '${loadFile.absolutePath}'"

        // load it
        def tempLogDir = new File(ConfigUtil.logsDirPath(), "temp")
        def progressFile = File.createTempFile("load-progress", ".txt", tempLogDir)
        log.info "Load progress output: ${progressFile.absolutePath}"
        def options = ["progressLogFile": progressFile.absolutePath]
        options << ["ignoreUuid": false]   // our target repo should get it's UUID from the hotcopy
        svnRepoService.scheduleLoad(repoTarget, options)

        // Allow 60 sec max for load after which the load file should be deleted
        timeLimit = System.currentTimeMillis() + 60000
        while (loadFile.exists() && System.currentTimeMillis() < timeLimit) {
            Thread.sleep(5000)
        }
        assertFalse "load file should be deleted after loading", loadFile.exists()
        
        // verify that repo load has run (trunk/tags/branches which should have been imported)
        output = commandLineService.executeWithOutput(
                ConfigUtil.svnPath(), "info",
                "--no-auth-cache", "--non-interactive",
                commandLineService.createSvnFileURI(new File(repoParentDir, testRepoNameTarget)) + "trunk")

        boolean loadSuccess = output.contains("Node Kind: directory")
        assertTrue "the target repo should now have nodes from the src repo after loading", loadSuccess

        // test symlink
        repoHome = new File(svnRepoService.getRepositoryHomePath(repoTarget))
        hooks = new File(repoHome, "hooks")
        startCommitTmpl = new File(hooks, "start-commit.tmpl")
        File startCommit = new File(hooks, "start-commit")
        assertEquals "the start-commit.tmpl and the former link should have same content",
                startCommitTmpl.text, startCommit.text
    }

    public void testLoadFailMail() {
        
        // upper limit on time to run async code
        long timeLimit = System.currentTimeMillis() + 30000
        // make sure the quartz scheduler is running, is put in standby by other tests
        quartzScheduler.start()
        // but pause jobs likely to start running to ensure a thread is available
        jobsAdminService.pauseGroup("Statistics")
                
        // create a target repo WITHOUT branches/tags/trunk nodes
        def testRepoNameTarget = "load-test-target"
        
        Repository repoTarget = new Repository(name: testRepoNameTarget)
        assertEquals "Failed to create target repository.", 0,
                svnRepoService.createRepository(repoTarget, false)
        repoTarget.save(flush: true)
        
        def resource = this.class.getResource("corrupted-dump-file.dump")
        println resource
        File dumpFile = new File(resource.toURI())
        
        // move src dump file to the expected load location for target
        File loadDir = svnRepoService.getLoadDirectory(repoTarget)
        // delete any residual load files
        loadDir.eachFile {
            it.delete()
        }
        File loadFile = new File(loadDir, dumpFile.name)
        loadFile.text = dumpFile.text
        //boolean loadFileCreated = dumpFile.renameTo(loadFile)
        //assertTrue "should be able to move dumpfile to load directory", loadFile.exists()
        assertTrue "loadFile should exist", loadFile.exists()
        log.info "The loadfile to be imported to the target repo: '${loadFile.absolutePath}'"

        // almost all setup, but still need mail configured
        ConfigurationHolder.config = grailsApplication.config
        MailConfiguration mailConfig = MailConfiguration.configuration
        mailConfig.port = ServerSetupTest.SMTP.port
        mailConfig.enabled = true
        mailConfigurationService.saveMailConfiguration(mailConfig)
                
        // load it
        def tempLogDir = new File(ConfigUtil.logsDirPath(), "temp")
        def progressFile = File.createTempFile("load-progress", ".txt", tempLogDir)
        def options = ["progressLogFile": progressFile.absolutePath]
        options << ["ignoreUuid": false]
        svnRepoService.scheduleLoad(repoTarget, options)
        
        long startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() < timeLimit &&
                greenMail.receivedMessages.length == 0) {
            Thread.sleep(1000)
        }
        assertEquals("Expected one mail message", 1,
                greenMail.receivedMessages.length)
        def message = greenMail.receivedMessages[0]
        assertEquals("Message Subject did not match",
                "[Error][Load]Repository: " + testRepoNameTarget,
                message.subject)
        assertTrue(message.content instanceof MimeMultipart)
        MimeMultipart mp = message.content
        assertEquals("Expected an attachment", 2, mp.count)
        assertTrue("Message Body did not match", 
                GreenMailUtil.getBody(mp.getBodyPart(0).content.getBodyPart(0))
                .startsWith("The loading of the dump file or archive into repository '" + 
                testRepoNameTarget + "' failed."))
    }

    void testSyncRepositoriesPerformance() {

        // get the baseline repo count
        svnRepoService.syncRepositories()
        def beginningRepoCount = Repository.count()

        // create large set of repos out of band
        log.info("Creating 1000 repos")
        (1..1000).each {
            def testRepoNameSrc = "sync-test-${it}"
            TestUtil.createMockRepo(testRepoNameSrc, repoParentDir )
        }

        // run the sync method
        def startTime = new Date()
        log.info("Starting repo sync with 1000 to import at: " + startTime)
        svnRepoService.syncRepositories()
        def endTime = new Date()
        log.info("Finished repo sync at: " + endTime)
        log.info("Sync took " + (new Date().time - startTime.time) + "ms")

        def expectedRepos = beginningRepoCount + 1000
        assertEquals ("${expectedRepos} repositories expected after sync", expectedRepos, Repository.count())

        // delete the repos out of band
        (1..1000).each {
            def testRepoNameSrc = "sync-test-${it}"
            def repo = new File(repoParentDir.absolutePath, testRepoNameSrc)
            assertTrue("Should be able do delete repo", repo.deleteDir())
        }

        // run the sync method
        startTime = new Date()
        log.info("Starting repo sync with 1000 to delete at: " + startTime)
        svnRepoService.syncRepositories()
        endTime = new Date()
        log.info("Finished repo sync at: " + endTime)
        log.info("Sync took " + (new Date().time - startTime.time) + "ms")

        assertEquals ("${beginningRepoCount} repositories expected after sync", beginningRepoCount, Repository.count())
    }

    public void testVerify() {

        // make sure the quartz scheduler is running, is put in standby by other tests
        quartzScheduler.start()
        // but pause jobs likely to start running to ensure a thread is available
        jobsAdminService.pauseGroup("Statistics")

        // create a target repo WITHOUT branches/tags/trunk nodes
        def testRepoNameTarget = "verify-test-target"

        Repository repoTarget = new Repository(name: testRepoNameTarget)
        assertEquals "Failed to create target repository.", 0,
                svnRepoService.createRepository(repoTarget, true)
        repoTarget.save(flush: true)

        // configure mail
        ConfigurationHolder.config = grailsApplication.config
        MailConfiguration mailConfig = MailConfiguration.configuration
        mailConfig.port = ServerSetupTest.SMTP.port
        mailConfig.enabled = true
        mailConfigurationService.saveMailConfiguration(mailConfig)

        // configure scheduler bean
        SchedulerBean sched = new SchedulerBean()
        sched.frequency = SchedulerBean.Frequency.NOW

        // schedule verify job
        svnRepoService.scheduleVerifyJob(sched, repoTarget)

        // upper limit on time to run async code
        long timeLimit = System.currentTimeMillis() + 5000
        while (System.currentTimeMillis() < timeLimit &&
                greenMail.receivedMessages.length == 0) {
            Thread.sleep(1000)
        }
        assertEquals("Expected zero messages (success)", 0,
                greenMail.receivedMessages.length)


        // now, break the repo
        File repoDir = new File(svnRepoService.getRepositoryHomePath(repoTarget))
        File repoDbDir = new File(repoDir, "db")
        File repoDbDirBak = new File(repoDir, "db.bak")
        assertTrue ("Expect that we can rename the repo db", repoDbDir.renameTo(repoDbDirBak))

        // schedule verify job
        sched = new SchedulerBean()
        sched.frequency = SchedulerBean.Frequency.NOW
        svnRepoService.scheduleVerifyJob(sched, repoTarget)

        // upper limit on time to run async code
        timeLimit = System.currentTimeMillis() + 5000
        while (System.currentTimeMillis() < timeLimit &&
                greenMail.receivedMessages.length == 0) {
            Thread.sleep(1000)
        }
        assertEquals("Expected one message (fail)", 1,
                greenMail.receivedMessages.length)

        def message = greenMail.receivedMessages[0]
        assertEquals("Message Subject did not match",
                "[Error][Verification]Repository: " + testRepoNameTarget,
                message.subject)
        assertTrue(message.content instanceof MimeMultipart)
        MimeMultipart mp = message.content
        assertEquals("Expected an attachment", 2, mp.count)
        assertTrue("Message Body did not match",
                GreenMailUtil.getBody(mp.getBodyPart(0).content.getBodyPart(0))
                        .startsWith("Verification of repository '" +
                        repoTarget.name + "' failed."))
    }
}
