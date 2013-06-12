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

import com.collabnet.svnedge.domain.Repository 
import com.collabnet.svnedge.domain.SvnLog 
import com.collabnet.svnedge.domain.statistics.StatValue

class SvnStatisticsServiceIntegrationTests extends GrailsUnitTestCase {
    def svnStatisticsService

    protected void setUp() {
        super.setUp()
    }

    protected void tearDown() {
        super.tearDown()
    }


    void testGetHitsForRepo() {
        /* SVN statistics feature is removed for 1.0 release. */
        return
        // create a repo
        def repoName = "svnStatTestRepo"
        def repo = new Repository(name: repoName).save()
        // add some lines to SvnLog
        loadSvnLogs(repo, 0)
        def hits = svnStatisticsService.getHitsForRepo(repo, 
                                                       (new Date() - 1)
                                                       .getTime(), 
                                                       (new Date()).getTime())
        assertEquals("The number of hits returned is 4.", 4, hits)   
    }


    void testCollectData() {
        /* SVN statistics feature is removed for 1.0 release. */
        return
        // try with no data
        svnStatisticsService.collectData()
        def repoName = "svnStatTestCollectRepo"
        def repo = new Repository(name: repoName).save()
        def hitValues = StatValue
            .findAllByStatisticAndRepo(svnStatisticsService.getStat(), repo)
        assertEquals("There should be no values yet.", 0, hitValues.size())
        // load data
        loadSvnLogs(repo, svnStatisticsService.getStatGroup()
                    .getRawInterval() * 1000)
        svnStatisticsService.collectData()
        hitValues = StatValue
            .findAllByStatisticAndRepo(svnStatisticsService.getStat(), repo)
        // now there should be StatValue w/ 3 data points
        assertEquals("There should be one value.", 1, hitValues.size())
        assertEquals("The stat value should have a value of 3.", 3, 
            (hitValues[0]).getAverageValue())
        svnStatisticsService.collectData()
    }

    void testGetChartValues() {
        /* SVN statistics feature is removed for 1.0 release. */
        return
        // make sure there is data
        def repoName = "svnStatTestChartRepo"
        def repo = new Repository(name: repoName).save()
        loadSvnLogs(repo, 0)
        svnStatisticsService.collectData()
        def start = (new Date() - 1).getTime()
        def end = (new Date() + 1).getTime()
        def values = svnStatisticsService.getChartValues(start, end)
        assertNotNull("The chart values should not be null.", values)
        assertTrue("There should be at least one chart value.", 
                   values.size() > 0)
    }

    private void loadSvnLogs(repo, timeOffset) {
        def user = "svnStatUser"
        new SvnLog(lineNumber: 1, 
                   timestamp: new Date().getTime() - timeOffset,
                   username: user,
                   repo: repo,
                   action: "update",
                   path: "/test/path",
                   revision: "r3").save()
        new SvnLog(lineNumber: 2, 
                   timestamp: new Date().getTime() - timeOffset,
                   username: user,
                   repo: repo,
                   action: "update",
                   path: "/test/path",
                   revision: "r3").save()
        new SvnLog(lineNumber: 3, 
                   timestamp: new Date().getTime() - timeOffset,
                   username: user,
                   repo: repo,
                   action: "update",
                   path: "/test/path",
                   revision: "r3").save()
        // make sure the interval is closed
        new SvnLog(lineNumber: 4, 
                   timestamp: new Date().getTime(),
                   username: user,
                   repo: repo,
                   action: "update",
                   path: "/test/path",
                   revision: "r3").save()
    }
}
