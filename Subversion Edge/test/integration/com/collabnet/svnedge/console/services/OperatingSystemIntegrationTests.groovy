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
import grails.test.GrailsUnitTestCase;

/**
 * Tests the Operating System service class.
 * 
 * @author Marcello de Sales(mdesales@collab.net)
 *
 */
class OperatingSystemIntegrationTests extends GrailsUnitTestCase {

    def operatingSystemService
    def svnRepoService

    def void testGetProperties() {
        assertNotNull("The properties of the OS must be available", 
            operatingSystemService.properties)
        operatingSystemService.properties.each {key, value ->
            println("# ${key}: ${value}")
            assertNotNull("The OS property key must not be null", key)
            assertNotNull("The OS property value must not be null", value)
        }
    }

    def void testGetEnvironmentVariables() {
        assertNotNull("The properties of the OS must be available",
            operatingSystemService.environmentVariables)
        operatingSystemService.environmentVariables.each {key, value ->
            println("# ${key}: ${value}")
            assertNotNull("The env key must not be null", key)
            assertNotNull("The env value must not be null", value)
        }
    }

    def void testGetSystemProperties() {
        assertNotNull("The properties of the OS must be available",
            operatingSystemService.systemProperties)
        operatingSystemService.systemProperties.each {key, value ->
            println("# ${key}: ${value}")
            assertNotNull("The env key must not be null", key)
            assertNotNull("The env value must not be null", value)
        }
    }

    def void testRootPath() {
        println("The root path of where CSVN is installed: " + 
            operatingSystemService.appRootVolumePath)
        assertNotNull(operatingSystemService.appRootVolumePath)
        if (operatingSystemService.isWindows()) {
            assertTrue("The root path on Windows where CSVN is running " +
                "must be in the format 'DRIVER:\\'", 
                operatingSystemService.appRootVolumePath.length() == 3)
        } else {
            assertTrue("The root path on *nix where CSVN is running " +
                "must be in the format '/'",
                operatingSystemService.appRootVolumePath.startsWith ("/"))
        }
    }

    def void testGetTotalDiskSpaceRegularRepository() {
        Repository newRepo = new Repository(name: "integration")
        svnRepoService.createRepository(newRepo, true)

        def repoDirPath = svnRepoService.getRepositoryHomePath(newRepo)
        println("SvnRepo: " + repoDirPath)

        def dirUsage = operatingSystemService.sigar.getDirUsage(repoDirPath)
        def dirByteSize = dirUsage.diskUsage
        assertTrue("The total value of the disk space is incorrect", 
            dirByteSize > 0)
        println("Repo size bytes: " + dirByteSize)
        println("Repo size KB: " + Math.round(dirByteSize / 1024))

        def numberDirs = dirUsage.subdirs
        assertEquals("The number of default directories for SVN is incorrect",
            10, numberDirs)

        svnRepoService.archivePhysicalRepository(newRepo)

        /*
           $ /usr/lib/jvm/java-6-openjdk/bin/java 
           -Djava.library.path=/u1/svnedge/replica_admin/SvnEdge/ext/sigar 
           -classpath /u1/svnedge/replica_admin/SvnEdge/lib/sigar.jar 
           org.hyperic.sigar.cmd.Du /u1/svnedge/production-tests/1.1/csvn/data/repositories/ddd

           73110   /u1/svnedge/production-tests/1.1/csvn/data/repositories/ddd

           $ du -sb /u1/svnedge/production-tests/1.1/csvn/data/repositories/ddd

           77206   /u1/svnedge/production-tests/1.1/csvn/data/repositories/ddd
           
           Discussion on these results on 
           http://forums.hyperic.com/jiveforums/thread.jspa?threadID=10528
         */
    }
}
