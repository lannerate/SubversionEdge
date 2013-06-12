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
package com.collabnet.svnedge.integration

import grails.test.GrailsUnitTestCase
import org.codehaus.groovy.grails.commons.ConfigurationHolder

import com.collabnet.svnedge.integration.SetupTeamForgeService;

class SetupTeamForgeServiceTests extends GrailsUnitTestCase {
    def setupTeamForgeService
    File testDir

    def config = ConfigurationHolder.config

    protected void setUp() {
        super.setUp()

        // mock the service and its dependencies
        mockLogging (SetupTeamForgeService, true)
        setupTeamForgeService = new SetupTeamForgeService()
    }

    protected void tearDown() {
        super.tearDown()

        if (testDir) {
            testDir.eachFile { it.delete() }
            testDir.delete()
        }
        testDir = null
    }

    private File createTestDir() {
        File dir = File.createTempFile("repo-hooks", null)
        String path = dir.absolutePath
        dir.delete()
        dir = new File(path)
        dir.mkdir()
        testDir = dir
    }
    
    private def setupFiles(String f1Name, String f2Name) {
        File f1 = new File(testDir, f1Name)
        def f1Contents = "Some random text to have compressed: " + f1Name
        f1.write(f1Contents)
        File f2 = new File(testDir, f2Name)
        def f2Contents = f2Name +
"""Actually, trunk can now be used for 1.1 development,
so new features can go there.   In fact, if John/Jeremy feel like merging
CTF_MODE to trunk, that'd be fine."""
        f2.write(f2Contents)
        assertTrue "${f1.name} wasn't created", f1.exists()
        assertTrue "${f2.name} wasn't created", f2.exists()
        f2.setExecutable(true)
        return [f1, f1Contents, f2, f2Contents]
    }

    void testArchiveCurrentHooks() {
        createTestDir()
        def (f1, f1Contents, f2, f2Contents) = setupFiles("test1.txt", "test2.txt")
        assertEquals "Only the created files should be present", 2,
            testDir.listFiles().length
        setupTeamForgeService.archiveCurrentHooks(testDir)
        archiveAssertions()

        // check that contents can be restored
        setupTeamForgeService.restoreNonCtfHooks(testDir)
        assertEquals "Only the original files should be present", 2, 
            testDir.listFiles().length
        assertTrue "${f1.name} doesn't exist", f1.exists()
        assertTrue "${f2.name} doesn't exist", f2.exists()
        assertEquals "${f1.name} content is corrupted", f1Contents, f1.text
        assertEquals "${f2.name} content is corrupted", f2Contents, f2.text
        if (System.getProperty("os.name").substring(0,3).toLowerCase() != "win") {
            assertTrue "${f1.name} should not be executable", !f1.canExecute()
        }
        assertTrue "${f2.name} should be executable", f2.canExecute()
    }

    private void archiveAssertions() {
        File archive = new File(testDir, "pre-ctf-hooks.zip")
        assertTrue "Hookscript archive ${archive.absolutePath} is missing",
            archive.exists()
        assertEquals "Only archive file should be present", 1,
            testDir.listFiles().length
    }

    void testRestoreHooksWithAddedFiles() {
        createTestDir()
        def (f1, f1Contents, f2, f2Contents) = setupFiles("test1.txt", "test2.txt")
        assertEquals "Only the created files should be present", 2,
            testDir.listFiles().length
        setupTeamForgeService.archiveCurrentHooks(testDir)
        archiveAssertions()

        def (f1New, f1NewContent, f2New, f2NewContent) = 
            setupFiles("new-test1.txt", "new-test2.txt")
        assertEquals "Only the created files plus archive should be present", 3,
        testDir.listFiles().length

        // check that contents can be restored
        setupTeamForgeService.restoreNonCtfHooks(testDir)
        assertEquals "Only the original files plus archive should be present", 3,
            testDir.listFiles().length
        assertTrue "${f1.name} doesn't exist", f1.exists()
        assertTrue "${f2.name} doesn't exist", f2.exists()
        File ctfArchive = new File(testDir, "ctf-hook-scripts.zip")
        assertTrue "${ctfArchive.name} doesn't exist", ctfArchive.exists()
        
        // A second time through the cycle, one extra archive is expected
        setupTeamForgeService.archiveCurrentHooks(testDir)
        (f1New, f1NewContent, f2New, f2NewContent) = 
            setupFiles("new2-test1.txt", "new2-test2.txt")
        assertEquals "Only the created files plus archives should be present", 4,
            testDir.listFiles().length
        File archive = new File(testDir, "pre-ctf-hooks.zip")
        assertTrue "Hookscript archive ${archive.absolutePath} is missing",
            archive.exists()
        assertTrue "${f1New.name} doesn't exist", f1New.exists()
        assertTrue "${f2New.name} doesn't exist", f2New.exists()
        assertTrue "${ctfArchive.name} doesn't exist", ctfArchive.exists()

        setupTeamForgeService.restoreNonCtfHooks(testDir)
        assertEquals "Only the original files plus archives should be present", 4,
            testDir.listFiles().length
        assertTrue "${f1.name} doesn't exist", f1.exists()
        assertTrue "${f2.name} doesn't exist", f2.exists()
        assertTrue "${ctfArchive.name} doesn't exist", ctfArchive.exists()
        File oldCtfArchive = new File(testDir, "old-ctf-hook-scripts.zip")
        assertTrue "${oldCtfArchive.name} doesn't exist", oldCtfArchive.exists()
    }

    
}
