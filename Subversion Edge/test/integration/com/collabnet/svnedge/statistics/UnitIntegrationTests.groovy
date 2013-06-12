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

import com.collabnet.svnedge.domain.statistics.Unit 
import grails.test.*

class UnitIntegrationTests extends GrailsUnitTestCase {

    protected void setUp() {
        super.setUp()
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testCreateUnit() {
        def unitName = "Test Unit"
        def unit = new Unit(name: unitName,
                            minValue: 0,
                            maxValue: 100,
                            formatter: "unitFormat")
        assertTrue("The unit should validate.", unit.validate())
        if (!unit.save(flush:true)) {
            unit.errors.each {
                println it
            }
        }
        def savedUnit = Unit.findByName(unitName)
        assertNotNull("The saved unit was not found!", savedUnit)
    }

}
