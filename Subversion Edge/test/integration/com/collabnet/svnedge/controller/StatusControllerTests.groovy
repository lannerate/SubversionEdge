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
import com.collabnet.svnedge.domain.Server
import com.collabnet.svnedge.domain.ServerMode

class StatusControllerTests extends AbstractSvnEdgeControllerTests {

    def quartzScheduler
    def statisticsService
    def lifecycleService
    def packagesUpdateService
    def operatingSystemService
    def networkingService
    def svnRepoService
    def securityService

    protected void setUp() {
        super.setUp()
        controller.quartzScheduler = quartzScheduler
        controller.statisticsService = statisticsService
        controller.lifecycleService = lifecycleService 
        controller.packagesUpdateService = packagesUpdateService
        controller.operatingSystemService = operatingSystemService
        controller.networkingService = networkingService
        controller.svnRepoService = svnRepoService
        controller.securityService = securityService

    }

    protected void tearDown() {
        super.tearDown()
    }

    void testIndexStandalone() {

        def server = Server.getServer()
        server.mode = ServerMode.STANDALONE
        server.save(flush: true)

        controller.index()
    }

}
