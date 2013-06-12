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
package com.collabnet.svnedge.console.pkgsupdate

import grails.test.*

/**
 * Tests the execution of the packages update job.
 * 
 * @author Marcello de Sales (mdesales@collab.net)
 */
class PackagesUpdateJobTests extends GrailsUnitTestCase {

    def packagesUpdateJob
    def grailsApplication

    protected void setUp() {
        this.packagesUpdateJob = grailsApplication.mainContext.getBean(
                "com.collabnet.svnedge.admin.PackagesUpdateJob")
    }

    void testExecution() {
        try {
            packagesUpdateJob.execute()
        } catch (Exception e) {
            e.printStackTrace()
            fail("The software updates job should not fail: " + e.getMessage())
        }
    }
}
