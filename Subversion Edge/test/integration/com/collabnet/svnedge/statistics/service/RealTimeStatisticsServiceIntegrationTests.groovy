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
package com.collabnet.svnedge.statistics.service

import com.collabnet.svnedge.domain.Repository 
import com.collabnet.svnedge.domain.integration.ReplicatedRepository 
import com.collabnet.svnedge.domain.integration.RepoStatus 
import grails.test.*


class RealTimeStatisticsServiceIntegrationTests extends GrailsUnitTestCase {
    def realTimeStatisticsService

    protected void setUp() {
        super.setUp()
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testReposStatus() {
        // load up a couple of repos with various status
        new ReplicatedRepository(repo: new Repository(name: "testrepo1").save(), 
            lastSyncTime: -1,  lastSyncRev:-1, enabled: true, 
            status: RepoStatus.NOT_READY_YET).save()
        new ReplicatedRepository(repo: new Repository(name: "testrepo2").save(), 
            lastSyncTime: -1, lastSyncRev:-1, enabled: true, 
            status: RepoStatus.OK).save()
        new ReplicatedRepository(repo: new Repository(name: "testrepo3").save(), 
            lastSyncTime: -1, lastSyncRev:-1, enabled: true, 
            status: RepoStatus.OK).save()
        new ReplicatedRepository(repo: new Repository(name: "testrepo4").save(), 
            lastSyncTime: -1, lastSyncRev:-1, enabled: true, 
            status: RepoStatus.ERROR).save()
        def repoStatus = realTimeStatisticsService.getReposStatus()
        assertNotNull("The repository status should not be null.", repoStatus)
        assertTrue("The repository status should be a list.", 
                   repoStatus instanceof java.util.List)
        assertEquals("There should be 3 items in the status list.", 3, 
                     repoStatus.size())
        repoStatus.each {
            if (it.status.equals(RepoStatus.NOT_READY_YET)) {
                assertEquals("There should be one NOT_READY_YET.", 1, it.count)
            } else if (it.status.equals(RepoStatus.OK)) {
                assertEquals("There should be 2 OK.", 2, it.count)
            } else if (it.status.equals(RepoStatus.ERROR)) {
                assertEquals("There should be one ERROR.", 1, it.count)
            }
        }
    }

    void testRepoUsedDiskspace() {
        def repoUsed = realTimeStatisticsService.getRepoUsedDiskspace()
        assertNotNull("The space used by the repositories should not be null.",
                      repoUsed)
        assertTrue("The space used by the repositories should not be " +
                   "negative.", repoUsed >= 0)
        def systemUsed = realTimeStatisticsService.getSystemUsedDiskspace()
        assertNotNull("The system disk space should not be null.",
            systemUsed)
    }

    void testSystemUsedDiskspace() {
        def systemUsed = realTimeStatisticsService.getSystemUsedDiskspace()
        assertNotNull("The space used by the system should not be null.",
                      systemUsed)
        assertTrue("The space used by the system should not be negative.",
                   systemUsed >= 0)
        def totalSpace = realTimeStatisticsService.getSystemTotalDiskspace()
        if (totalSpace != null) {
            assertTrue("The space used by the system should be less than " +
                           "the total diskspace", systemUsed <= totalSpace) 
        }
    }

    void testSystemTotalDiskspace() {
        def systemTotal = realTimeStatisticsService.getSystemTotalDiskspace()
        assertNotNull("The total diskspace should not be null.", systemTotal)
        assertTrue("The total diskspace should be positive.", systemTotal >= 0)
    }

    void testThroughput() {
        def throughput = realTimeStatisticsService.getThroughput()
        assertNotNull("The throughput should not be null.", throughput)
        assertEquals("The throughput should contain 4 values.", 4, 
                     throughput.size())
    }
}
