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

import grails.test.*

import com.collabnet.svnedge.domain.Server
import com.collabnet.svnedge.domain.statistics.StatValue

class FileSystemStatisticsServiceIntegrationTests extends GrailsUnitTestCase {
    def fileSystemStatisticsService

    protected void setUp() {
        super.setUp()
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testGetSpace() {
        def repoUsedSpace = fileSystemStatisticsService.getRepoUsedDiskspace()
        assertNotNull("The space used by the repos should be returned.", 
                      repoUsedSpace)
        if (new File(Server.getServer().repoParentDir).list().size() > 0) {
            assertTrue("The repoUsedSpace value should be greater than 0.", 
                       repoUsedSpace > 0)
        } else {
            assertEquals("The repoUsedSpace value should be 0.", 0, repoUsedSpace)
        }
        def sysUsedSpace = fileSystemStatisticsService.getSystemUsedDiskspace()
        assertNotNull("The space used by the system should be returned.", 
                      sysUsedSpace)
        assertTrue("The sysUsedSpace value should be greater than 0.", 
                   sysUsedSpace > 0)
    }

    void testCollectData() {
        fileSystemStatisticsService.collectData()
        def now = new Date().getTime()
        def sysUsedStat = fileSystemStatisticsService.getSysUsedStat()
        def repoUsedStat = fileSystemStatisticsService.getRepoUsedStat()
        def interval = fileSystemStatisticsService.getStatGroup()
            .getRawInterval() * 1000
        def repoValue = fileSystemStatisticsService.getStatValue(repoUsedStat,
                                                                 now, interval)
        assertNotNull("The repo used statvalue should not be null.", repoValue)
        def sysValue = fileSystemStatisticsService.getStatValue(sysUsedStat,
                                                                 now, interval)
        assertNotNull("The sys used statvalue should not be null.", sysValue)
    }

    void testGetChartValues() {
        // make sure there is data
        fileSystemStatisticsService.collectData()
        def start = (new Date() - 1).getTime()
        def end = (new Date() + 1).getTime()
        def values = fileSystemStatisticsService.getChartValues(start, end)
        assertNotNull("The chart values should not be null.", values)
        assertTrue("There should be at least one chart value.", 
                   values.size() > 0)
    }

}
