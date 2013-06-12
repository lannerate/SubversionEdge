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
import com.collabnet.svnedge.domain.statistics.StatValue 
import com.collabnet.svnedge.domain.statistics.Statistic 
import com.collabnet.svnedge.domain.statistics.StatisticType 
import com.collabnet.svnedge.domain.statistics.Unit 
import com.collabnet.svnedge.statistics.DeleteStatJob 

class DeleteStatJobIntegrationTests extends GrailsUnitTestCase {
    def quartzScheduler
    def executorService
    def deleteStatisticsService

    def statGroup
    def stat
    def deleteStatJob
    def jobHelper

    protected void setUp() {
        super.setUp()
        def testName = "test"
        createTestStats(testName)
        deleteStatJob = new DeleteStatJob()
        deleteStatJob.deleteStatisticsService = deleteStatisticsService
        jobHelper = new TestJobHelper(job: deleteStatJob,
                listenerName: "DeleteStatJobIntegration", log: log,
                executorService: executorService, quartzScheduler: quartzScheduler)
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testDeleteStatExecution() {
        // Check that we have 3 StatValues initially
        List<StatValue> values = StatValue.findAllByStatistic(stat)
        assertEquals("There should be 3 values initially.", 3, values.size())
        Map params = new HashMap(1)
        params.put("statGroupName", statGroup.getName())
        jobHelper.executeJob(params)
        // check that we now have 2 StatValues
        values = StatValue.findAllByStatistic(stat)
        assertEquals("There should be 2 values after a delete.", 2, 
                     values.size())   
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
        def collectInterval = new Interval(name: "Test collect interval",
                                           seconds: 5).save()
        def deleteInterval = new Interval(name: "Test delete interval",
            seconds: 60).save()
        def statAction = new StatAction(collect: collectInterval,
                                        delete: deleteInterval, isRaw: true)
        statGroup.addToActions(statAction).save()
        statAction.save()
        stat = new Statistic(name: name, 
                             title: name,
                             type: StatisticType.COUNTER)
        statGroup.addToStatistics(stat).save()
        stat.save()
        def statValueTime = System.currentTimeMillis()
        def statValueInt = collectInterval.getSeconds() * 1000
        def statValueDeleteInt = deleteInterval.getSeconds() * 1000
        // recent stat value
        new StatValue(timestamp: statValueTime, interval: statValueInt,
                      minValue: 0, maxValue: 0, averageValue: 0, lastValue: 0,
                      derived: false, uploaded: false, statistic: stat).save()
        // old enough to delete
        new StatValue(timestamp: statValueTime - statValueDeleteInt, 
                      interval: statValueInt,
                      minValue: 0, maxValue: 0, averageValue: 0, lastValue: 0,
                      derived: false, uploaded: false, statistic: stat).save()
        // old enough to delete, but wrong interval 
        new StatValue(timestamp: statValueTime - statValueDeleteInt, 
                      interval: statValueInt * 2,
                      minValue: 0, maxValue: 0, averageValue: 0, lastValue: 0,
                      derived: false, uploaded: false, statistic: stat).save()
    }
}
