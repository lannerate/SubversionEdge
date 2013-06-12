/*
* CollabNet Subversion Edge
* Copyright (C) 2011, CollabNet Inc. All rights reserved.
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
package com.collabnet.svnedge.integration

import com.collabnet.svnedge.AbstractSvnEdgeFunctionalTests
import com.collabnet.svnedge.LoginFunctionalTests

/**
 * This class tests various aspects of the discovery service feature
 */
class DiscoveryServiceFunctionalTests extends AbstractSvnEdgeFunctionalTests  {

    def discoveryService
    def networkingService


    /**
     * Go to the URLs advertised by the discovery service
     */
    void testGotoTeamforgeUrl() {

        loginAdmin()

        // Step 1: verify the setup page is correct
        get(getSchemeHostPort() + discoveryService.getCsvnContextPath() + discoveryService.getCsvnTeamforgeSetupPath())
        assertStatus 200

        assertContentContains(getMessage("setupTeamForge.page.index.almTitle"))

        // Step 2: verify that the setup page is correct after clicking on
        // the continue button.
        byId("btnCtfMode").click()
        assertStatus 200

        assertContentContains(getMessage("setupTeamForge.page.ctfInfo.p1"))
    }

    /**
     * Go to the URLs advertised by the discovery service
     */
    void testGotoCsvnUrl() {

        loginAdmin()

        // verify the status page
        get(getSchemeHostPort() + discoveryService.getCsvnContextPath())
        assertStatus 200

        assertContentContains(getMessage("status.page.header.title"))

    }

    def getSchemeHostPort() {
        String schemeHostPort = "http://"
        schemeHostPort += "localhost"
        schemeHostPort += ":" + discoveryService.getCsvnPort()
        schemeHostPort
    }


}
