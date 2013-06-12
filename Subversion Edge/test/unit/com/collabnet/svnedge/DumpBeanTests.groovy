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
package com.collabnet.svnedge


import grails.test.*
import com.collabnet.svnedge.console.DumpBean

class DumpBeanTests extends GrailsUnitTestCase {

    protected void setUp() {
        super.setUp()
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testIncludePathMultiple() {
        DumpBean params = new DumpBean(includePath: "trunk branches ta\\ gs")
        def paths = params.includePathPrefixes
        if (!paths) {
            fail "Expected 3 paths, not 0"
        }

        assertEquals("Expected 3 paths, found " + paths, 3, paths.size())
        assertEquals("Expected 'trunk'", 'trunk', paths[0])
        assertEquals("Expected 'branches'", 'branches', paths[1])
        assertEquals("Expected 'ta gs'", 'ta gs', paths[2])
    }
}
