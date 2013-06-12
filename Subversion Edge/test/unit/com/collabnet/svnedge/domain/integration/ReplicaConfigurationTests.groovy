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
package com.collabnet.svnedge.domain.integration

import grails.test.*

class ReplicaConfigurationTests extends GrailsUnitTestCase {
    protected void setUp() {
        super.setUp()
    }
    
    protected void tearDown() {
        super.tearDown()
    }

    void testConstraints() {
        def testConfig = new ReplicaConfiguration(name: "test_replica")
        mockForConstraintsTests(ReplicaConfiguration, [testConfig])

        ReplicaConfiguration config = new ReplicaConfiguration(
                name: "test_replica", 
                commandRetryAttempts: -1, 
                commandRetryWaitSeconds: 0)
        assertFalse config.validate()
        assertEquals "commandRetryAttempts cannot be negative", 
                "min", config.errors["commandRetryAttempts"]
        assertNull "commandRetryWaitSeconds should be valid",
                config.errors["commandRetryWaitSeconds"]

        config = new ReplicaConfiguration(name: "test_replica",
                    commandRetryAttempts: 0,
                    commandRetryWaitSeconds: -1)
        assertFalse config.validate()
        assertEquals "commandRetryWaitSeconds cannot be negative",
                "min", config.errors["commandRetryWaitSeconds"]
        assertNull "commandRetryAttempts should be valid",
                config.errors["commandRetryAttempts"]
                
        config = new ReplicaConfiguration(name: "test_replica",
                commandRetryAttempts: 11,
                commandRetryWaitSeconds: 301)
        assertFalse config.validate()
        assertEquals "commandRetryAttempts max is 10", 
                "max", config.errors["commandRetryAttempts"]
        assertEquals "commandRetryWaitSeconds max is 300",
                "max", config.errors["commandRetryWaitSeconds"]
    }
}