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
package com.collabnet.svnedge.controller





import grails.test.*

class StatisticsControllerTests extends AbstractSvnEdgeControllerTests {
    
    def operatingSystemService
    def networkStatisticsService
    def fileSystemStatisticsService

    protected void setUp() {
        super.setUp()
        controller.operatingSystemService = operatingSystemService
        controller.networkStatisticsService = networkStatisticsService
        controller.fileSystemStatisticsService = fileSystemStatisticsService
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testIndex() {
        controller.index()
    }

    void testGetTimespan() {
        def timespan = controller.getTimespan()
        assertNotNull("The timespan should not be null.", timespan)
    }

    void testByteRateChart() {
        def rateChart = controller.getByteRateChart()
        assertNotNull("The byte rate line chart should not be null.", 
                      rateChart)
    }

    void testDiskpaceChart() {
        def diskspaceChart = controller.getDiskspaceChart()
        assertNotNull("The diskspace chart should not be null.",
                      diskspaceChart)
    }

}
