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
package com.collabnet.svnedge.statistics.job

import grails.test.GrailsUnitTestCase;

import com.collabnet.svnedge.TestJobHelper
import com.collabnet.svnedge.domain.statistics.Category;
import com.collabnet.svnedge.domain.statistics.Interval 
import com.collabnet.svnedge.domain.statistics.StatAction 
import com.collabnet.svnedge.domain.statistics.StatGroup 
import com.collabnet.svnedge.domain.statistics.Statistic 
import com.collabnet.svnedge.domain.statistics.StatisticType 
import com.collabnet.svnedge.domain.statistics.Unit 
import com.collabnet.svnedge.statistics.AbstractStatisticsService 
import com.collabnet.svnedge.statistics.StatCountJob 

class StatCountJobIntegrationTests extends GrailsUnitTestCase {
    def quartzScheduler
    def executorService

    def statGroup
    def stat
    def statCountJob
    def jobHelper
    
    protected void setUp() {
        super.setUp()
        def testName = "test"
        createTestStats(testName)
        statCountJob = new StatCountJob()
        jobHelper = new TestJobHelper(job: statCountJob,
                listenerName: "StatCountJobIntegration", log: log,
                executorService: executorService, quartzScheduler: quartzScheduler)
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testStatCountExecution() {
        Map params = new HashMap(1)
        params.put("statGroupName", statGroup.getName())
        def now = new Date().getTime()
        def interval = statGroup.getRawInterval() * 1000
        jobHelper.executeJob(params)
        // check that we now have a zero StatValue
        stat.refresh()
        def statValue = AbstractStatisticsService.getStatValue(stat, 
                                                               now, interval)
        assertNotNull("The StatValue should have been created.", statValue)
        assertEquals("The StatValue should be zero.", 
                     statValue.getAverageValue(), 0)
    }

    void createTestStats(name) {
        def category = new Category(name: name)
        category.save()
        def unit = new Unit(name: name, minValue: 0)
        unit.save()
        statGroup = new StatGroup(name: name, title: name,
                                  unit: unit)
        category.addToGroups(statGroup).save()
        statGroup.save()
        Interval interval = new Interval(name:"TestInterval",
                                         seconds:500).save()
        StatAction statAction = new StatAction(group: statGroup,
                                               collect: interval, 
                                               isRaw: true).save()
        statGroup.addToActions(statAction).save()
        stat = new Statistic(name: name, 
                             title: name,
                             type: StatisticType.COUNTER)
        statGroup.addToStatistics(stat).save()
        stat.save()
    }
}
