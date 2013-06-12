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
package com.collabnet.svnedge.controller

import org.springframework.mock.web.MockMultipartFile
import org.springframework.mock.web.MockMultipartHttpServletRequest

import com.collabnet.svnedge.console.RepoTemplateService;
import com.collabnet.svnedge.domain.RepoTemplate;

import grails.test.*

class RepoTemplateControllerTests extends ControllerUnitTestCase {
    File testDir
    
    protected void setUp() {
        super.setUp()
        
        mockDomain(RepoTemplate, [
                new RepoTemplate(name: "T1", location: "dump1.zip", 
                                 displayOrder: 3),
                new RepoTemplate(name: "l10n_T2", location: "dump2", 
                                 displayOrder: 1),
                new RepoTemplate(name: "HC3", 
                                 location: "repoArchive3.zip", 
                                 displayOrder: 2)
        ])

        // mock the i18n "message" map available to controller
        controller.metaClass.message = { Map p -> 
            return p.code
        }
        
        // mock the RepoTemplateService
        def svc = new Expando()
        svc.substituteL10nName = { p1, p2 ->
            if (p1.name.startsWith("l10n_")) {
                p1.name = p1.name.substring(5) 
            }    
        }
        controller.repoTemplateService = svc

        mockLogging(RepoTemplateController, true)
    }

    protected void tearDown() {
        super.tearDown()
        if (testDir && testDir.exists()) {
            testDir.deleteDir()
        }
    }

    void testList() {
        def model = controller.list()
        def templateList = model.repoTemplateInstanceList
        assertNotNull "Model should contain template list.", templateList
        assertEquals "Should be 3 templates.", 3, templateList.size()
        assertEquals "Localized template should be first.", 2, templateList[0].id
        assertEquals "Name should be localized.", "T2", templateList[0].name
        assertEquals "HC3 template should be next.", 3, templateList[1].id
        assertEquals "T1 template should be last.", 1, templateList[2].id
    }

    void testSave() {
        controller.params.name = "test template"
        
        def fieldName = 'templateUpload'
        def originalFileName = 'my-dump-file'
        def contentType = 'application/octet-stream'
        def content = '123'
        controller.metaClass.request = new MockMultipartHttpServletRequest()
        controller.request.addFile(new MockMultipartFile(
                fieldName, originalFileName, contentType, content as byte[]))
        
        
        def serviceControl = mockFor(RepoTemplateService)
        File templateDir = createTestDir("templateDir")
        File uploadDir = new File(templateDir, "temp")
        
        serviceControl.demand.getUploadDirectory() { ->
            if (!uploadDir.exists()) {
                uploadDir.mkdir()
            }
            return uploadDir
        }
        serviceControl.demand.saveTemplate() { rt, t, b ->
            assert !b
            return true
        }

        controller.repoTemplateService = serviceControl.createMock();
        def model = controller.save()
        assertEquals "Save was successful, did not redirect to 'list'", 
                "list", controller.redirectArgs["action"]
        serviceControl.verify()
        
        File uploadedFile = new File(uploadDir, originalFileName)
        assertTrue "Uploaded file not found.", uploadedFile.exists()
        assertEquals "File content was unexpected.", content, uploadedFile.text
    }

    void testUpdate() {
        int id = 2
        assertEquals "Current name is wrong", 'l10n_T2', 
                RepoTemplate.get(id).name
        controller.params.id = id
        controller.params.name = 'T2'
        controller.params.active = 'true'
        controller.update()
        assertEquals "Update was successful, did not redirect to 'list'", 
                "list", controller.redirectArgs["action"]

        assertEquals "Updated name is wrong", 'T2', RepoTemplate.get(id).name
        assertTrue "Template should be flagged as displayable", 
                RepoTemplate.get(id).active

        // this should fail (validation error)
        controller.params.name = 'T1'
        def model = controller.update()
        assertTrue "Expected error when name matches an existing template",
                model.repoTemplateInstance.hasErrors()
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
