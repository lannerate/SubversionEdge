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
package com.collabnet.svnedge.statistics

import com.collabnet.svnedge.domain.Repository 
import com.collabnet.svnedge.domain.statistics.Category;
import com.collabnet.svnedge.domain.statistics.StatGroup 
import com.collabnet.svnedge.domain.statistics.StatValue 
import com.collabnet.svnedge.domain.statistics.Statistic 
import com.collabnet.svnedge.domain.statistics.StatisticType 
import com.collabnet.svnedge.domain.statistics.Unit 
import grails.test.GrailsUnitTestCase;

class StatValueIntegrationTests extends GrailsUnitTestCase {
    def stat

    def repo

    protected void setUp() {
        super.setUp()
        def categoryName = "Category for Test"
        def category = new Category(name: categoryName)
        category.save(flush:true)
        def unitName = "Test Unit"
        def unit = new Unit(name: unitName,
                            minValue: 0,
                            maxValue: 100,
                            formatter: "unitFormat")
        unit.save(flush:true)
        def statgroupName = "StatGroup for Test"
        def rawInterval = 500
        def derivedIntervals = [rawInterval*5, rawInterval*10]
        def statGroup = new StatGroup(name: statgroupName, 
                                      title: statgroupName,
                                      unit: unit, category: category,
                                      rawInterval: rawInterval,
                                      derivedIntervals: derivedIntervals)
        statGroup.save(flush:true)
        def statName = "Test statistic for StatValueIntegration"
        stat = new Statistic(name: statName, title: statName,
                             type: StatisticType.GAUGE, group: statGroup)
        stat.save(flush:true)   
        def repoName = "statValueRepo"
        repo = new Repository(name: repoName).save()
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testCreateStatValue() {
        def statValueTime = System.currentTimeMillis()
        def statValue = new StatValue(timestamp: statValueTime, interval: 500,
                                      minValue: 0, maxValue: 0, 
                                      averageValue: 0, lastValue: 0,
                                      derived: false, uploaded: false,
                                      statistic: stat)
        assertTrue("The statValue should validate.", statValue.validate())
        if (!statValue.save(flush:true)) {
            statValue.errors.each {
                println it
            }
        }
        def savedStatValue = StatValue.findByStatistic(stat)
        assertNotNull("The StatValue was not found!", savedStatValue)
        assertEquals("The time of the StatValue is wrong.", statValueTime,
                     savedStatValue.getTimestamp())
    }

    void testCreateStatValueWithRepo() {
        def statValueTime = System.currentTimeMillis()
        def statValue = new StatValue(timestamp: statValueTime, interval: 500,
                                      minValue: 0, maxValue: 0, 
                                      averageValue: 0, lastValue: 0,
                                      derived: false, uploaded: false,
                                      statistic: stat, repo: repo)
        assertTrue("The statValue should validate.", statValue.validate())
        if (!statValue.save(flush:true)) {
            statValue.errors.each {
                println it
            }
        }
        def savedStatValue = StatValue.findByStatistic(stat)
        assertNotNull("The StatValue was not found!", savedStatValue)
        assertEquals("The time of the StatValue is wrong.", statValueTime,
                     savedStatValue.getTimestamp())
        assertEquals("The statValue's repo is wrong.", statValue.getRepo(),
                     repo)
    }
}
