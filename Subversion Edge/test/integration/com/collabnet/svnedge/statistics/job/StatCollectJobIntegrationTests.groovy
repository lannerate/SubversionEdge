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

import com.collabnet.svnedge.statistics.StatCollectJob 
import grails.test.*
import com.collabnet.svnedge.TestJobHelper

class StatCollectJobIntegrationTests extends GrailsUnitTestCase {
    def networkStatisticsService
    def quartzScheduler
    def executorService
    def grailsApplication
    def statCollectJob
    def jobHelper
    
    protected void setUp() {
        super.setUp()
        statCollectJob = new StatCollectJob()
        statCollectJob.appCtx = grailsApplication.mainContext
        jobHelper = new TestJobHelper(job: statCollectJob,
                listenerName: "StatCollectJobIntegration", log: log,
                executorService: executorService, quartzScheduler: quartzScheduler)
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testTriggerNetworkStats() {
        Map params = new HashMap(1)
        params.put("serviceName", "networkStatisticsService")
        jobHelper.executeJob(params)
        // make sure the stat values appear
        def values = networkStatisticsService.getCurrentThroughput()
        assertNotNull("The bytesIn value should not be null.", values[0])
        assertNotNull("The bytesOut value should not be null.", values[1])
    }
}
