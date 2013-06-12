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

import grails.test.*

import com.collabnet.svnedge.TestJobHelper
import com.collabnet.svnedge.domain.statistics.Category;
import com.collabnet.svnedge.domain.statistics.Interval 
import com.collabnet.svnedge.domain.statistics.StatAction 
import com.collabnet.svnedge.domain.statistics.StatGroup 
import com.collabnet.svnedge.domain.statistics.StatValue 
import com.collabnet.svnedge.domain.statistics.Statistic 
import com.collabnet.svnedge.domain.statistics.StatisticType 
import com.collabnet.svnedge.domain.statistics.Unit 
import com.collabnet.svnedge.statistics.ConsolidateStatJob 

class ConsolidateStatJobIntegrationTests extends GrailsUnitTestCase {
    def quartzScheduler
    def executorService
    def consolidateStatisticsService

    def statGroup
    def stat
    def consolidateStatJob
    def jobHelper

    protected void setUp() {
        super.setUp()
        createTestStats()
        consolidateStatJob = new ConsolidateStatJob()
        consolidateStatJob.consolidateStatisticsService = consolidateStatisticsService
        jobHelper = new TestJobHelper(job: consolidateStatJob,
                listenerName: "ConsolidateStatJobIntegration", log: log,
                executorService: executorService, quartzScheduler: quartzScheduler)
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testConsolidateStatExecution() {
        // make sure the actions are present
        assertEquals("There should be 3 statActions for the group", 3, 
                     statGroup.getActions().size())
        // make sure the statistic is present
        assertEquals("There should be 1 statistic for the group", 1, 
                     statGroup.getStatistics().size())
        def values1sec = StatValue.findAllByStatisticAndInterval(stat, 1000)
        assertEquals("The size of the 1-sec values should be 5.", 5, 
                     values1sec.size())
        Map params = new HashMap(1)
        params.put("statGroupName", statGroup.getName())
        jobHelper.executeJob(params)
        stat.refresh()
        values1sec = StatValue.findAllByStatisticAndInterval(stat, 1000)
        assertEquals("The size of the 1-sec values should be 5.", 5, 
                     values1sec.size())
        // check that we now have 2 2-sec values and 1 4-sec value.
        def values2sec = StatValue.findAllByStatisticAndInterval(stat, 
                                                                 2000)
        def values4sec = StatValue.findAllByStatisticAndInterval(stat, 4000)
        assertEquals("The size of the 2-sec values should be 2.", 2, 
                     values2sec.size())
        assertEquals("The size of the 4-sec values should be 1.", 1, 
                     values4sec.size())
    }

    void createTestStats() {
                def categoryName = "Category for Test ConsolidateJob"
        def category = new Category(name: categoryName)
        category.save(flush:true)
        def unitName = "Test Consolidate Unit"
        def unit = new Unit(name: unitName,
                            minValue: 0,
                            maxValue: 100,
                            formatter: "unitFormat")
        unit.save(flush:true)
        def statgroupName = "StatGroup for Test Consolidate"
        statGroup = new StatGroup(name: statgroupName, 
                                  title: statgroupName,
                                  unit: unit, category: category)
        category.addToGroups(statGroup).save()
        statGroup.save()
        def collectInterval1 = new Interval(name: "Test consolidateJob interval1",
                                            seconds: 1).save()
        def collectInterval2 = new Interval(name: "Test consolidateJob interval2",
                                            seconds: 2).save()
        def collectInterval3 = new Interval(name: "Test consolidateJob interval3",
                                            seconds: 4).save()
        def statAction1 = new StatAction(group: statGroup,
                                         collect: collectInterval1, 
                                         consolidateInterval:null).save()
        statGroup.addToActions(statAction1).save()
        def statAction2 = 
            new StatAction(group: statGroup,
                           collect: collectInterval2,
                           consolidateSource:statAction1).save()
        statGroup.addToActions(statAction2).save()
        def statAction3 = 
            new StatAction(group: statGroup,
                           collect: collectInterval3,
                           consolidateSource:statAction2).save()
        statGroup.addToActions(statAction3).save()
        stat = new Statistic(name: "Test consolidateJob statistic", 
                             title: "Test statistic", 
                             type: StatisticType.GAUGE)
        statGroup.addToStatistics(stat).save()
        statGroup.save()
        stat.save()
        // add data points at the lowest interval
        new StatValue(timestamp: 0, interval: 1000, minValue: 0, maxValue: 0, 
                      averageValue: 0, lastValue: 0, derived: false, 
                      uploaded: false, statistic: stat).save()
        new StatValue(timestamp: 1000, interval: 1000, minValue: 1, 
                      maxValue: 1, 
                      averageValue: 1, lastValue: 1, derived: false, 
                      uploaded: false, statistic: stat).save()
        new StatValue(timestamp: 2000, interval: 1000, minValue: 8, 
                      maxValue: 8, 
                      averageValue: 8, lastValue: 8, derived: false, 
                      uploaded: false, statistic: stat).save()
        new StatValue(timestamp: 3000, interval: 1000, minValue: 3, 
                      maxValue: 3, 
                      averageValue: 3, lastValue: 3, derived: false, 
                      uploaded: false, statistic: stat).save()
        new StatValue(timestamp: 4000, interval: 1000, minValue: 6, 
                      maxValue: 6, 
                      averageValue: 6, lastValue: 6, derived: false, 
                      uploaded: false, statistic: stat).save()
        stat.save()
    }
}
