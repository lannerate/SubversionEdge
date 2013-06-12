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
package com.collabnet.svnedge


import com.collabnet.svnedge.domain.Repository 
import grails.test.GrailsUnitTestCase;

/**
 * Unit tests for Repository domain object
 */
class RepositoryTests extends GrailsUnitTestCase {

    protected void setUp() {
        super.setUp()
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testConstraints() {

        // mock the grails domain class Repository
        def repoTest = new Repository(name: "existing_repo")
        mockForConstraintsTests(Repository, [repoTest])

        // additional mock validation rule (mockForConstraints() doesn't handle this)
        Repository.metaClass.'static'.findByNameIlike = { null }

        // exercise constraints
        def repo = new Repository(name: "repo1")
        assertTrue "the Repository domain class should ALLOW 'repo1' name", repo.validateName()

        repo = new Repository(name: "Repo_1")
        assertTrue "the Repository domain class should not ALLOW 'Repo_1' name", repo.validateName()

        repo = new Repository(name: "repo.1")
        assertTrue "the Repository domain class should not ALLOW 'repo.1' name", repo.validateName()

        repo = new Repository(name: "Repo_!")
        assertFalse "the Repository domain class should not ALLOW 'Repo_!' name", repo.validateName()

        repo = new Repository(name: "repo.*")
        assertFalse "the Repository domain class should not ALLOW 'repo.1' name", repo.validateName()

        repo = new Repository()
        repo.validate()
        assertEquals "the Repository domain class should NOT ALLOW 'null' name", "nullable", repo.errors["name"]

        // Removed this constraint from the class so not testing here (moved to controller logic)
//      repo = new Repository(name : "existingRepo")
//      // additional mock validation rule (mockForConstraints() doesn't handle this) -- this time it should fail
//      Repository.metaClass.'static'.findByNameIlike = { "unique" }
//      repo.validate()
//      assertEquals  "the Repository domain class should NOT ALLOW repeat 'existingRepo' name", "unique",
//              repo.errors["name"]

        repo = new Repository(name: "repo 1")
        assertFalse "the Repository domain class should NOT ALLOW 'repo 1' name", repo.validateName()
    }

}
