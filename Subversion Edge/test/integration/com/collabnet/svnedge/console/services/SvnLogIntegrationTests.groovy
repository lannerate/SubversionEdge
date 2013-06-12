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

import com.collabnet.svnedge.domain.Repository 
import com.collabnet.svnedge.domain.SvnLog 
import grails.test.*

class SvnLogIntegrationTests extends GrailsUnitTestCase {
    def repo

    protected void setUp() {
        super.setUp()
        def repoName = "svnLogRepo"
        repo = new Repository(name: repoName).save()
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testSvnLogCreation() {
        def svnLogUser = "svnLogTestUser"
        // create normal SvnLog
        def log1 = new SvnLog(lineNumber: 1, 
                              timestamp: new Date().getTime(),
                              username: svnLogUser,
                              repo: repo,
                              action: "update",
                              path: "/test/path",
                              revision: "r3")
        if (!log1.validate()) {
            log1.errors.allErrors.each { println it }
            fail("The normal SvnLog creation failed to validate.")
        }
        log1.save()
        // create SvnLog with null Repo
        def log2 = new SvnLog(iineNumber: 2,
                              timestamp: new Date().getTime(),
                              username: svnLogUser,
                              repo: null,
                              action: "update",
                              path: "/test/path",
                              revision: "r3")
        if (!log2.validate()) {
            log2.errors.allErrors.each { println it }
            fail("The SvnLog with null repo creation failed to validate.")
        }
        log2.save()
        def svnLogs = SvnLog.findAllByUsername(svnLogUser)
        assertNotNull("The svnLogs should not be null.", svnLogs)
        assertEquals("There should be two svnLogs found.", 2, svnLogs.size())
    }
}
