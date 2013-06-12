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

import com.collabnet.svnedge.domain.Repository 
import com.collabnet.svnedge.domain.integration.ReplicatedRepository 
import com.collabnet.svnedge.domain.integration.RepoStatus 
import grails.test.*

class RepositoryIntegrationTests extends GrailsUnitTestCase {
    protected void setUp() {
        super.setUp()
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testRepoCreation() {
        def repoName = "testRepo"
        new ReplicatedRepository(repo: new Repository(name: repoName).save(), 
            lastSyncTime: -1, lastSyncRev:-1, enabled: true, 
            status: RepoStatus.OK, statusMsg: null).save()
        def repo = ReplicatedRepository.findByRepo(Repository.findByName(repoName))
        assertNotNull("The repository should not be null.", repo)
    }

    void testRepoList() {
        def repoName = "testRepo"
        new ReplicatedRepository(repo: new Repository(name: repoName).save(), 
            lastSyncTime: -1, lastSyncRev:-1, enabled: true, 
            status: RepoStatus.OK, statusMsg: null).save()
        def repos = ReplicatedRepository.list()
        repos.each { println it.repo.name }
        assertEquals("The size of repos should be 1.", 1, repos.size())
    }
}
