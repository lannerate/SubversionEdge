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

import com.collabnet.svnedge.domain.statistics.Category;
import com.collabnet.svnedge.domain.statistics.StatGroup 
import com.collabnet.svnedge.domain.statistics.Statistic 
import com.collabnet.svnedge.domain.statistics.StatisticType 
import com.collabnet.svnedge.domain.statistics.Unit 
import grails.test.*

class StatisticIntegrationTests extends GrailsUnitTestCase {
    def statGroup

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
        statGroup = new StatGroup(name: statgroupName, 
                                  title: statgroupName,
                                  unit: unit, category: category,
                                  rawInterval: rawInterval,
                                  derivedIntervals: derivedIntervals)
        statGroup.save(flush:true)
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testCreateStatistic() {
        def statName = "Test statistic"
        def stat = new Statistic(name: statName, title: statName,
                                 type: StatisticType.GAUGE, group: statGroup)
        assertTrue("The statistic should validate.", stat.validate())
        if (!stat.save(flush:true)) {
            stat.errors.each {
                println it
            }
        }
        def savedStat = Statistic.findByName(statName)
        assertNotNull("The Statistic was not found!", savedStat)
    }
}
