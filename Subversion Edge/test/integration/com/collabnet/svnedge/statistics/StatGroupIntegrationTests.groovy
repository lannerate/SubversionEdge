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
import com.collabnet.svnedge.domain.statistics.Interval 
import com.collabnet.svnedge.domain.statistics.StatAction 
import com.collabnet.svnedge.domain.statistics.StatGroup 
import com.collabnet.svnedge.domain.statistics.Unit 
import grails.test.*

class StatGroupIntegrationTests extends GrailsUnitTestCase {
    def category
    def unit

    protected void setUp() {
        super.setUp()
        def categoryName = "Category for Test"
        category = new Category(name: categoryName)
        category.save(flush:true)
        def unitName = "Test Unit"
        unit = new Unit(name: unitName,
                        minValue: 0,
                        maxValue: 100,
                        formatter: "unitFormat")
        unit.save(flush:true)
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testCreateStatGroup() {
        def statgroupName = "StatGroup for Test"
        def statGroup = new StatGroup(name: statgroupName, 
                                      title: statgroupName,
                                      unit: unit, category: category)
        Interval rawInterval = new Interval(name: "500 sec test", 
                                            seconds: 500).save()
        Interval derive1 = new Interval(name: "2500 sec test",
                                        seconds: 2500).save()
        Interval derive2 = new Interval(name: "5000 sec test",
                                        seconds: 5000).save()
        StatAction rawAction = new StatAction(group: statGroup,
                                              collect: rawInterval, 
                                              consolidateSource: null).save()
        statGroup.addToActions(rawAction).save()
        StatAction deriveAction1 = 
            new StatAction(group: statGroup, collect: derive1,
                           consolidateSource:rawAction).save()
        statGroup.addToActions(deriveAction1).save()
        StatAction deriveAction2 = 
            new StatAction(group: statGroup, collect: derive2,
                           consolidateSource:deriveAction1).save()
        statGroup.addToActions(deriveAction2).save()
        assertTrue("The statGroup should validate.", statGroup.validate())
        if (!statGroup.save(flush:true)) {
            statGroup.errors.each {
                println it
            }
        }
        def savedSG = StatGroup.findByName(statgroupName)
        assertNotNull("The StatGroup was not found!", savedSG)
        def rawIntervalSec = savedSG.getRawInterval()
        assertEquals("The raw interval should be 500 seconds.", 500, 
                     rawIntervalSec)
    }

    void testNullMinDeleteInterval() {
        def statgroupName = "StatGroup2 for Test"
        def statGroup = new StatGroup(name: statgroupName, 
                                      title: statgroupName,
                                      unit: unit, category: category)
        Interval rawInterval = new Interval(name: "500 sec test", 
                                            seconds: 500).save()
        Interval derive1 = new Interval(name: "2500 sec test",
                                        seconds: 2500).save()
        Interval derive2 = new Interval(name: "5000 sec test",
                                        seconds: 5000).save()
        StatAction rawAction = new StatAction(collect: rawInterval, 
                                              isRaw: true).save()
        statGroup.addToActions(rawAction)
        StatAction deriveAction1 = new StatAction(collect: derive1).save()
        statGroup.addToActions(deriveAction1)
        StatAction deriveAction2 = new StatAction(collect: derive2).save()
        statGroup.addToActions(deriveAction2).save()
        statGroup.save()
        def minInterval = statGroup.getMinDeleteInterval()
        assertNull("The min delete interval should be null if there are no " 
                   + "deletes specified.", minInterval)
    }

    void testMinDeleteInterval() {
        def statgroupName = "StatGroup2 for Test"
        def statGroup = new StatGroup(name: statgroupName, 
                                      title: statgroupName,
                                      unit: unit, category: category)
        Interval rawInterval = new Interval(name: "500 sec test", 
                                            seconds: 500).save()
        Interval derive1 = new Interval(name: "2500 sec test",
                                        seconds: 2500).save()
        Interval derive2 = new Interval(name: "5000 sec test",
                                        seconds: 5000).save()
        StatAction rawAction = new StatAction(group: statGroup,
                                              collect: rawInterval, 
                                              consolidateSource: null).save()
        statGroup.addToActions(rawAction)
        StatAction deriveAction1 = 
            new StatAction(group: statGroup,
                           collect: derive1, 
                           delete: derive2, 
                           consolidateSource: rawAction).save()
        statGroup.addToActions(deriveAction1)
        StatAction deriveAction2 = 
            new StatAction(group: statGroup,
                           collect: derive2, consolidateSource: deriveAction1,
                           delete: rawInterval).save()
        statGroup.addToActions(deriveAction2).save()
        statGroup.save()
        def minInterval = statGroup.getMinDeleteInterval()
        assertNotNull("The min delete interval should not be null if there is" 
                   + " a delete specified.", minInterval)
        assertEquals("The min delete interval should be 500.", 500, 
                     minInterval)
    }

    void testMinConsolidateInterval() {
        def statgroupName = "StatGroup3 for Test"
        def statGroup = new StatGroup(name: statgroupName, 
                                      title: statgroupName,
                                      unit: unit, category: category)
        Interval rawInterval = new Interval(name: "500 sec test", 
                                            seconds: 500).save()
        Interval derive1 = new Interval(name: "2500 sec test",
                                        seconds: 2500).save()
        Interval derive2 = new Interval(name: "5000 sec test",
                                        seconds: 5000).save()
        StatAction rawAction = new StatAction(group: statGroup,
                                              collect: rawInterval, 
                                              consolidateSource:null).save()
        statGroup.addToActions(rawAction)
        StatAction deriveAction1 = 
            new StatAction(group: statGroup,
                           collect: derive1,
                           consolidateSource: rawAction,
                           delete: derive2).save()
        statGroup.addToActions(deriveAction1)
        StatAction deriveAction2 = 
            new StatAction(group: statGroup,
                           collect: derive2,
                           consolidateSource: deriveAction1,
                           delete: rawInterval).save()
        statGroup.addToActions(deriveAction2).save()
        statGroup.save()
        def minInterval = statGroup.getMinConsolidateInterval()
        assertNotNull("The min consolidate interval should not be null if " 
                      + "there is a non-raw collect interval specified.", 
                      minInterval)
        assertEquals("The min consolidate interval should be 2500.", 2500, 
                     minInterval)
    }

}
