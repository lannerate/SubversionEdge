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
package com.collabnet.svnedge.domain

import grails.test.*

class RepoTemplateTests extends GrailsUnitTestCase {
    protected void setUp() {
        super.setUp()
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testConstraints() {
        def testTemplate = new RepoTemplate(name: "existing_template", 
                                            location: "dump1.zip")
        mockForConstraintsTests(RepoTemplate, [testTemplate])

        RepoTemplate template = new RepoTemplate()
        assertFalse template.validate()
        assertEquals "name cannot be null", "nullable", template.errors["name"]
        assertEquals "location cannot be null", 
                "nullable", template.errors["location"]
        
        template = new RepoTemplate(name: "valid", location: " ")
        assertFalse template.validate()
        assertNull "'valid' is a valid name",  template.errors["name"]
        assertEquals "location cannot be blank", "blank", 
                template.errors["location"]

        template = new RepoTemplate(name: " ", location: "valid.zip")
        assertFalse template.validate()
        assertEquals "name cannot be blank", "blank", template.errors["name"]
        assertNull "'valid.zip' is a valid location",  
                template.errors["location"]
                
        String longName = "testName90123456789012345678901234567890" + 
                "1234567890123456789012345678901234567890" + 
                "1234567890123456789012345678901234567890" 
                
        template = new RepoTemplate(name: longName, location: "valid.zip")
        assertTrue "120 character name is valid", template.validate()
        
        template = new RepoTemplate(name: longName + "1", location: "valid.zip")
        assertFalse template.validate()
        assertEquals "name cannot exceed 120 characters", 
                "maxSize", template.errors["name"]
        
        template = new RepoTemplate(name: "valid", 
                location: longName + longName + 1234567890123456)
        assertFalse template.validate()
        assertEquals "location cannot exceed 255 characters", 
                "maxSize", template.errors["location"]

        template = new RepoTemplate(name: "existing_template",
                                    location: "dump1.zip")
        assertFalse template.validate()
        assertEquals "name must be unique", "unique", template.errors["name"]
        assertEquals "location must be unique",
                "unique", template.errors["location"]
    }
}