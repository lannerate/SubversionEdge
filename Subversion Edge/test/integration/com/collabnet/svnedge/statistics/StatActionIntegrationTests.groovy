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

class StatActionIntegrationTests extends GrailsUnitTestCase {
    def statGroup1
    def statGroup2
    def intervalRaw
    def deleteRaw
    def intervalDerived
    def deleteDerived
    def intervalBadMultiple
    def intervalAfterDelete

    protected void setUp() {
        super.setUp()
        def categoryName = "Category for StatActionTest"
        def category = new Category(name: categoryName)
        category.save()
        def unitName = "Test Unit"
        def unit = new Unit(name: unitName,
                            minValue: 0,
                            maxValue: 100,
                            formatter: "unitFormat")
        unit.save()
        def statgroupName1 = "StatGroup1 for StatActionTest"
        statGroup1 = new StatGroup(name: statgroupName1, 
                                   title: statgroupName1,
                                   unit: unit, category: category).save()
        def statgroupName2 = "StatGroup2 for StatActionTest"
        statGroup2 = new StatGroup(name: statgroupName2, 
                                   title: statgroupName2,
                                   unit: unit, category: category).save()
        intervalRaw = new Interval(name: "2-sec", seconds: 2).save()
        deleteRaw = new Interval(name: "200-sec", seconds: 200).save()
        intervalDerived = new Interval(name: "4-sec", seconds: 4).save()
        deleteDerived = new Interval(name: "400-sec", seconds: 400).save()
        intervalBadMultiple = new Interval(name: "3-sec", seconds: 3).save()
        intervalAfterDelete = new Interval(name: "500-sec", 
                                           seconds: 500).save() 
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testCreate() {
        // create the raw StatAction (should succeed)
        def rawStatAction = new StatAction(group: statGroup1, 
                                           collect: intervalRaw, 
                                           delete: deleteRaw, 
                                           consolidateSource: null)
        if (!rawStatAction.validate()) {
            rawStatAction.errors.each {
                println it
            }
            fail("The statAction should validate.")
        }
        rawStatAction.save()
        // try to create the dervied StatAction w/ the rawStatAction as
        // the source, but w/ another group (should fail)
        def derivedStatActionFail = 
            new StatAction(group: statGroup2, 
                           collect: intervalDerived, 
                           delete: deleteDerived, 
                           consolidateSource: rawStatAction)
        if (derivedStatActionFail.validate()) {
            fail("The StatAction who's source and group do not match should "
                 + "not validate")
        }
        // try to create a reasonable derived StatAction (should succeed)
        def derivedStatAction = 
            new StatAction(group: statGroup1, 
                           collect: intervalDerived, 
                           delete: deleteDerived, 
                           consolidateSource: rawStatAction)
        if (!derivedStatAction.validate()) {
            derivedStatAction.errors.each {
                println it
            }
            fail("The derived StatAction should validate")
        }
        derivedStatAction.save()
        // try to create a derived StatAction w/ a bad multiple
        def badMultStatAction = 
            new StatAction(group: statGroup1, 
                           collect: intervalBadMultiple, 
                           delete: deleteDerived, 
                           consolidateSource: rawStatAction)
        if (badMultStatAction.validate()) {
            fail("The StatAction who's interval is not an even multiple " 
                 + "should not validate.")
        }
        // try to create a derived StatAction who's interval is beyond
        // the delete time of it's source.
        def afterDeleteStatAction = 
            new StatAction(group: statGroup1, 
                           collect: intervalAfterDelete, 
                           delete: deleteDerived, 
                           consolidateSource: rawStatAction)
        if (afterDeleteStatAction.validate()) {
            fail("The StatAction who's interval is beyond the delete time" 
                 + " of its source should not validate.")
        }
    }
}
