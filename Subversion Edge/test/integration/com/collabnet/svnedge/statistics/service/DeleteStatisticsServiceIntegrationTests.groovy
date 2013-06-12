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

import com.collabnet.svnedge.domain.statistics.Category;
import com.collabnet.svnedge.domain.statistics.Interval 
import com.collabnet.svnedge.domain.statistics.StatAction 
import com.collabnet.svnedge.domain.statistics.StatGroup 
import com.collabnet.svnedge.domain.statistics.StatValue 
import com.collabnet.svnedge.domain.statistics.Statistic 
import com.collabnet.svnedge.domain.statistics.StatisticType 
import com.collabnet.svnedge.domain.statistics.Unit 
import grails.test.*

class DeleteStatisticsServiceIntegrationTests extends GrailsUnitTestCase {
    def deleteStatisticsService

    def stat
    def statAction
    def collectInterval
    def deleteInterval

    protected void setUp() {
        super.setUp()
        def categoryName = "Category for Test Delete"
        def category = new Category(name: categoryName)
        category.save(flush:true)
        def unitName = "Test Delete Unit"
        def unit = new Unit(name: unitName,
                        minValue: 0,
                        maxValue: 100,
                        formatter: "unitFormat")
        unit.save(flush:true)
        def statgroupName = "StatGroup for Test Delete"
        def statGroup = new StatGroup(name: statgroupName, 
                                  title: statgroupName,
                                  unit: unit, category: category)
        category.addToGroups(statGroup).save()
        collectInterval = new Interval(name: "Test collect interval",
                                       seconds: 5).save()
        deleteInterval = new Interval(name: "Test delete interval",
                                      seconds: 60).save()
        statAction = new StatAction(collect: collectInterval,
                                    delete: deleteInterval)
        statGroup.addToActions(statAction).save()
        statAction.save()
        stat = new Statistic(name: "Test statistic", 
                             title: "Test statistic", 
                             type: StatisticType.GAUGE)
        statGroup.addToStatistics(stat).save()
        stat.save()
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testDeletion() {
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
        List<StatValue> values = StatValue.findAllByStatistic(stat)
        assertEquals("There should be 3 values initially.", 3, values.size())
        deleteStatisticsService.delete(statAction)
        values = StatValue.findAllByStatistic(stat)
        assertEquals("There should be 2 values after a delete.", 2, 
                     values.size())        
    }
}
