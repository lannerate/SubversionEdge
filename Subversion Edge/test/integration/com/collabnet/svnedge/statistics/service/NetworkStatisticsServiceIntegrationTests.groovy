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

import grails.test.*


class NetworkStatisticsServiceIntegrationTests extends GrailsUnitTestCase {
    def networkStatisticsService

    protected void setUp() {
        super.setUp()
    }

    protected void tearDown() {
        super.tearDown()
    }

    // Simple test; more in-depth testing would be done in platform-specific
    // networkStatisticsServices
    void testCollectData() {
        try {
            networkStatisticsService.collectData()
        } catch (Exception e) {
            e.printStackTrace()
            fail("An exception occurred: " + e.getMessage());
        }
    }

    void testThroughputRates() {
        // test to make sure there are no exceptions.  Data will be
        // tested by wrapped platform-specific classes.
        def now = new Date()
        def interval = 
            networkStatisticsService.getStatGroup().getRawInterval() * 1000
        def startTime = new Date(now.getTime() - interval)
        try {
            def rates = networkStatisticsService.getThroughputRates(startTime, 
                                                                    now)
            assertNotNull("Rates should not be null.", rates)
        } catch (Exception e) {
            e.printStackTrace()
            fail("An exception occurred while trying to get throughput rates.")
        }
    }

    void testCurrentThroughput() {
        def throughput = networkStatisticsService.getCurrentThroughput()
        assertNotNull("The value of throughput should not be null.", 
                      throughput)
        assertEquals("The size of throughput should be 2.", 2, 
                     throughput.size())
        // make sure we have data
        networkStatisticsService.collectData()
        throughput = networkStatisticsService.getCurrentThroughput()
        checkThroughputData(throughput)
        // now try it with 2 stat values.
        networkStatisticsService.collectData()
        throughput = networkStatisticsService.getCurrentThroughput()
        checkThroughputData(throughput)
    }

    void checkThroughputData(throughput) {
        assertNotNull("The value of throughput should not be null.", 
                      throughput)
        assertEquals("The size of throughput should be 2.", 2, 
                     throughput.size())
        def rateTimeIn = throughput[0]
        def rateTimeOut = throughput[1]
        assertNotNull("Throughput should have a value for rateTimeIn now.", 
                      rateTimeIn)
        assertNotNull("Throughput should have a value for rateTimeOut now.", 
                      rateTimeOut)
        assertEquals("The size of the rateTimeIn should be 2.", 2, 
                     rateTimeIn.size())
        assertEquals("The size of the rateTimeOut should be 2.", 2, 
                     rateTimeOut.size())
        def rateIn = rateTimeIn[0]
        def timeIn = rateTimeIn[1]
        def rateOut = rateTimeIn[0]
        def timeOut = rateTimeIn[1]
        assertTrue("The rateIn should be greater than or equal to zero.", 
                   rateIn >= 0)
        assertTrue("The rateOut should be greater than or equal to zero.", 
                   rateOut >= 0)
        assertTrue("The timeIn should be greater than or equal to zero.", 
                   timeIn >= 0)
        assertTrue("The timeOut should be greater than or equal to zero.", 
                   timeOut >= 0)
    }
}
