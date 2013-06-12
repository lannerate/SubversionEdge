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
package com.collabnet.svnedge.console.services

import com.collabnet.svnedge.admin.LogRotateJob 
import grails.test.*

class JobsAdminServiceIntegrationTests extends GrailsUnitTestCase {
    
    def jobsAdminService
    
    void testPauseAll() {
        def allGroups = jobsAdminService.getJobGroupNames()
        def origPaused = jobsAdminService.getPausedGroups()
        allGroups.each { jobsAdminService.resumeGroup(it) }
        jobsAdminService.pauseAll()
        def paused = jobsAdminService.getPausedGroups()
        assertEquals("All jobs should be paused. paused=" + paused + "\nAll groups=" + allGroups, 
            allGroups.size(), paused.size())
        
        // restore previous state
        for (group in paused) {
            if (!origPaused.contains(group)) {
                jobsAdminService.resumeGroup(group)
            }
        }
        assertEquals "Failed to restore state after test", 
            origPaused.size(), jobsAdminService.getPausedGroups().size()
    }

    /**
     * Test pausing a job's trigger and then resuming it.
     */
    void testJobPauseAndResume() {
        // make sure that the job is present
        def jobGroupNames = jobsAdminService.getJobGroupNames() as List
        assertTrue("The job should be present before removing it.", 
                   jobGroupNames.contains(LogRotateJob.group))

        try {
            jobsAdminService.pauseGroup(LogRotateJob.group)
        } catch (Exception e) {
            fail("An exception occurred while attempting to remove the trigger"
                 + e.getMessage())
        }

        def paused = jobsAdminService.getPausedGroups()
        assertTrue("LogRotate/Maintenance triggers should be paused.",
                   paused.contains(LogRotateJob.group))

        // attempt to resume it
        try {
            jobsAdminService.resumeGroup(LogRotateJob.group)
        } catch (Exception e) {
            fail("An exception occurred while attempting to add the trigger"
                 + e.getMessage())
        }

        paused = jobsAdminService.getPausedGroups()
        assertTrue("Replica_registration triggers should not be paused.",
                   !paused.contains("Replica_registration"))
    }

}
