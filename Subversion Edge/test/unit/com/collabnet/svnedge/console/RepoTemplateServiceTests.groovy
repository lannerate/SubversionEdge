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
package com.collabnet.svnedge.console

import java.io.File;

import com.collabnet.svnedge.domain.RepoTemplate
import com.collabnet.svnedge.util.ConfigUtil
import grails.test.*

class RepoTemplateServiceTests extends GrailsUnitTestCase {
    
    File testDir
    RepoTemplateService repoTemplateService
    
    protected void setUp() {
        super.setUp()
        
        mockDomain(RepoTemplate, [
                new RepoTemplate(name: "T1", location: "dump1.zip",
                                 displayOrder: 3, active: true), // id = 1
                new RepoTemplate(name: "l10n_T2", location: "dump2",
                                 displayOrder: 1, active: false), // id = 2
                new RepoTemplate(name: "HC3",
                                 location: "repoArchive3.zip",
                                 displayOrder: 2, active: true) // id =3
        ])

        mockLogging(RepoTemplateService, true)
        
        repoTemplateService = new RepoTemplateService()
    }

    protected void tearDown() {
        super.tearDown()
        if (testDir && testDir.exists()) {
            testDir.deleteDir()
        }
    }

    void testGetTemplateDirectory() {
        File dataDir = createTestDir('dataDir')        
        def configControl = mockFor(ConfigUtil)
        configControl.demand.static.dataDirPath(2) { -> dataDir.canonicalPath }

        File templateDir = repoTemplateService.templateDirectory
        assertTrue "Template directory doesn't exist.", templateDir.exists()
    }

    void testGetUploadDirectory() {
        File dataDir = createTestDir('dataDir')
        def configControl = mockFor(ConfigUtil)
        configControl.demand.static.dataDirPath(2) { -> dataDir.canonicalPath }

        File uploadDir = repoTemplateService.uploadDirectory
        assertTrue "Upload directory doesn't exist.", uploadDir.exists()
    }
    

    void testSaveTemplate() {
        File dataDir = createTestDir('dataDir')
        
        def configControl = mockFor(ConfigUtil)
        configControl.demand.static.dataDirPath(2) { -> dataDir.canonicalPath }

        String dumpFileContents = '123'
        RepoTemplate template = new RepoTemplate(name: "New Template")
        File dumpFile = new File(repoTemplateService.uploadDirectory, "dumpFile")
        dumpFile.text = dumpFileContents
        boolean isInsert = false

        assertEquals "Starting with 3 templates", 3, RepoTemplate.list().size()
        int templateId = repoTemplateService
                .saveTemplate(template, dumpFile, isInsert)
        assertEquals "Expected successful save attempt. ", 4, templateId
        configControl.verify()
        
        configControl.demand.static.dataDirPath(1) { -> dataDir.canonicalPath }
        File templateFile = new File(repoTemplateService.templateDirectory,
                                     'dump' + templateId)
        assertTrue "The dump file was not found at " + 
                templateFile.canonicalPath, templateFile.exists()
        assertEquals "Dump file contents. ",  
                dumpFileContents, templateFile.text
        
        template = RepoTemplate.get(templateId)
        assertTrue "Template should be active by default", template.active
        assertEquals "Template is added to end of list", 
                4, template.displayOrder
    }

    void testSaveTemplateWithInsert() {
        boolean isInsert = true
        File dataDir = createTestDir('dataDir')
        
        def configControl = mockFor(ConfigUtil)
        configControl.demand.static.dataDirPath(2) { -> dataDir.canonicalPath }

        String dumpFileContents = '123'
        RepoTemplate template = new RepoTemplate(name: "New Template")
        File dumpFile = new File(repoTemplateService.uploadDirectory, "dumpFile")
        dumpFile.text = dumpFileContents

        int templateId = repoTemplateService
                .saveTemplate(template, dumpFile, isInsert)
        assertEquals "Expected successful save attempt", 4, templateId
        configControl.verify()
        
        configControl.demand.static.dataDirPath(1) { -> dataDir.canonicalPath }
        File templateFile = new File(repoTemplateService.templateDirectory,
                                     'dump' + templateId)
        assertTrue "The dump file was not found at " + 
                templateFile.canonicalPath, templateFile.exists()
        assertEquals "Dump file contents. ",  
                dumpFileContents, templateFile.text
        
        template = RepoTemplate.get(templateId)
        assertTrue "Template should be active by default", template.active
        assertEquals "Template is added beginning of list", 
                1, template.displayOrder
        assertEquals "Templates should be reordered." + RepoTemplate.list(sort: 'displayOrder'), 
                2, RepoTemplate.get(2).displayOrder
        assertEquals "Templates should be reordered.", 
                3, RepoTemplate.get(3).displayOrder
        assertEquals "Templates should be reordered.", 
                4, RepoTemplate.get(1).displayOrder
    }

    void testSaveTemplateWithDuplicateName() {
        File dataDir = createTestDir('dataDir')
        
        def configControl = mockFor(ConfigUtil)
        configControl.demand.static.dataDirPath() { -> dataDir.canonicalPath }

        String dumpFileContents = '123'
        RepoTemplate template = new RepoTemplate(name: "T1")
        File dumpFile = new File(repoTemplateService.uploadDirectory, "dumpFile")
        dumpFile.text = dumpFileContents
        boolean isInsert = false
        
        int templateId = repoTemplateService
                .saveTemplate(template, dumpFile, isInsert)
        assertEquals "Expected unsuccessful save attempt", 0, templateId
        configControl.verify()
        
        configControl.demand.static.dataDirPath() { -> dataDir.canonicalPath }
        File templateDir = repoTemplateService.templateDirectory
        assertTrue "The template dir should be empty. ", templateDir.exists()
        assertTrue "Uploaded file cleanup is expected in caller. " + 
                "File should exist. ", dumpFile.exists()

        assertTrue "Validation error expected", template.hasErrors()
        assertEquals "Name should have been marked as non-unique",
                "unique", template.errors.name    
    }

    private File createTestDir(String prefix) {
        testDir = File.createTempFile(prefix + "-test", null)
        log.info("testDir = " + testDir.getCanonicalPath())
        // we want a dir, not a file, so delete and mkdir
        testDir.delete()
        testDir.mkdir()
        // TODO This doesn't seem to work, might need to delete in teardown
        testDir.deleteOnExit()
        return testDir
    }
}