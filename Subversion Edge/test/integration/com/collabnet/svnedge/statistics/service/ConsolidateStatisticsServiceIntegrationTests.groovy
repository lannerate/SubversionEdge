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
import com.collabnet.svnedge.domain.Repository

class ConsolidateStatisticsServiceIntegrationTests extends GrailsUnitTestCase {
    def statGroup
    def stat

    def consolidateStatisticsService

    protected void setUp() {
        super.setUp()
        def categoryName = "Category for Test Consolidate"
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
        def collectInterval1 = new Interval(name: "Test consolidate interval1",
                                            seconds: 1).save()
        def collectInterval2 = new Interval(name: "Test consolidate interval2",
                                            seconds: 2).save()
        def collectInterval3 = new Interval(name: "Test consolidate interval3",
                                            seconds: 4).save()
        def statAction1 = new StatAction(group: statGroup,
                                         collect: collectInterval1, 
                                         consolidateSource: null).save()
        statGroup.addToActions(statAction1).save()
        def statAction2 = new StatAction(group: statGroup,
                                         collect: collectInterval2,
                                         consolidateSource: statAction1).save()
        statGroup.addToActions(statAction2).save()
        def statAction3 = new StatAction(group: statGroup,
                                         collect: collectInterval3,
                                         consolidateSource: statAction2)
            .save()
        statGroup.addToActions(statAction3).save()
        stat = new Statistic(name: "Test consolidate statistic", 
                             title: "Test statistic", 
                             type: StatisticType.GAUGE)
        statGroup.addToStatistics(stat).save()
        statGroup.save()
        stat.save()
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testConsolidation() {
        // test consolidation w/ no data
        try {
           consolidateStatisticsService.consolidate(statGroup)
        } catch (Exception e) {
            fail("Got exception while doing empty consolidation:" + e)
        }
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
        def values1sec = StatValue.findAllByStatisticAndInterval(stat, 1000)
        assertEquals("The size of the 1-sec values should be 5.", 5,
                     values1sec.size())
        consolidateStatisticsService.consolidate(statGroup)
        // after consolidation, there should be 2 new StatValues for 2-sec
        // and 1 new StatValue for 4-sec.
        def values2sec = StatValue.findAllByStatisticAndInterval(stat, 2000)
        def values4sec = StatValue.findAllByStatisticAndInterval(stat, 4000)
        assertEquals("The size of the 2-sec values should be 2.", 2,
                     values2sec.size())
        assertEquals("The size of the 4-sec values should be 1.", 1,
                     values4sec.size())
        def value4sec = values4sec[0]
        assertEquals("The min value should be 0.", 0, value4sec.getMinValue())
        assertEquals("The max value should be 8.", 8, value4sec.getMaxValue())
        assertEquals("The avg value should be 3.", 3,
                     value4sec.getAverageValue())
        assertEquals("The last value should be 3.", 3,
                     value4sec.getLastValue())
        assertEquals("The interval should be 4000.", 4000,
                     value4sec.getInterval())
        assertTrue("Derived should be true.", value4sec.getDerived())
        assertEquals("The timestamp should be 0.", 0, value4sec.getTimestamp())
    }

    /**
     * tests consolidation by repository
     */
    void testConsolidationForRepository() {

        Repository r1 = new Repository(name: 'testRepo1')
        Repository r2 = new Repository(name: 'testRepo2')
        r1.save()
        r2.save()

        // add data points for repo1
        new StatValue(timestamp: 0, interval: 1000, minValue: 0, maxValue: 0,
                      averageValue: 0, lastValue: 0, derived: false,
                      uploaded: false, statistic: stat, repo: r1).save()
        new StatValue(timestamp: 1000, interval: 1000, minValue: 8,
                      maxValue: 8,
                      averageValue: 8, lastValue: 8, derived: false,
                      uploaded: false, statistic: stat, repo: r1).save()

        // add data points for repo2
        new StatValue(timestamp: 0, interval: 1000, minValue: 4, 
                      maxValue: 4,
                      averageValue: 4, lastValue: 4, derived: false,
                      uploaded: false, statistic: stat, repo: r2).save()
        new StatValue(timestamp: 1000, interval: 1000, minValue: 6,
                      maxValue: 6,
                      averageValue: 6, lastValue: 6, derived: false,
                      uploaded: false, statistic: stat, repo: r2).save()

        def values1sec = StatValue.findAllByStatisticAndInterval(stat, 1000)
        assertEquals("The size of the 1-sec values should be 4.", 4,
                     values1sec.size())
        // before consolidation, there should be no StatValues for 2-sec
        // interval
        def values2sec = StatValue.findAllByStatisticAndInterval(stat, 2000)
        assertEquals("The size of the 2-sec values should be 0.", 0,
                     values2sec.size())

        consolidateStatisticsService.consolidate(statGroup)

        // after consolidation, there should be StatValues for 2-sec
        // interval
        values2sec = StatValue.findAllByStatisticAndInterval(stat, 2000)
        assertEquals("The size of the 2-sec values should be 2.", 2,
                     values2sec.size())

        def consolidatedRepo1 = StatValue.withCriteria {
           and {
              eq('statistic', stat)
              eq('interval', 2000L)
              eq('repo', r1)
              eq('derived', true)
           }
        }
        assertEquals("The size of the 2-sec interval values for repo 1 should be 1.", 1,
                     consolidatedRepo1.size())
        def consolidatedRepo2 = StatValue.withCriteria {
           and {
              eq('statistic', stat)
              eq('interval', 2000L)
              eq('repo', r2)
              eq('derived', true)
           }
        }
        assertEquals("The size of the 2-sec interval values for repo 2 should be 1.", 1,
                     consolidatedRepo2.size())

        assertEquals("The min value for repo 1 should be 0.", 0, consolidatedRepo1[0].getMinValue())
        assertEquals("The max value for repo 1 should be 8.", 8, consolidatedRepo1[0].getMaxValue())
        assertEquals("The avg value for repo 1 should be 4.", 4,
                     consolidatedRepo1[0].getAverageValue())

        assertEquals("The min value for repo 2 should be 4.", 4, consolidatedRepo2[0].getMinValue())
        assertEquals("The max value for repo 2 should be 5.", 6, consolidatedRepo2[0].getMaxValue())
        assertEquals("The avg value for repo 2 should be 5.", 5,
                     consolidatedRepo2[0].getAverageValue())
    }
}
