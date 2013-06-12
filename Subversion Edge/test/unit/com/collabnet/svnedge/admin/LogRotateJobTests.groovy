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
package com.collabnet.svnedge.admin

import grails.test.GrailsUnitTestCase
import com.collabnet.svnedge.util.ConfigUtil

/**
 * This unit test exercises the LogRotateJob
 */
class LogRotateJobTests extends GrailsUnitTestCase {

    File logParentDir
    File logDir
    LogRotateJob job

    /**
     * sets up test resources and mocking
     */
    protected void setUp() {
        super.setUp()
        File tempDir = new File(System.getProperty("java.io.tmpdir"))
        int rndm = Math.round(Math.random() * 10000)
        logParentDir = new File("logs${rndm}", tempDir)
        logParentDir.mkdir()
        logDir = new File("logs", logParentDir)
        logDir.mkdir()

        // create 31 files, with date stamps going back that many days
        Date today = new Date()
        for (i in 1..31) {
            def f= new File("logfile_${i}.log", logDir)
            f.createNewFile()
            f << 'LOG DATA'

            f.setLastModified((today - i).time)
        }

        // use expando to mock the ConfigUtil method invoked in the target
        job = new LogRotateJob()
        def cfgUtil = new Expando()
        cfgUtil.dataDirPath = { -> logParentDir.getPath() }
        job.configUtil = cfgUtil
    }

    protected void tearDown() {
        super.tearDown()
        logParentDir.deleteDir()
    }

    public void testPruneLog() {

        assertEquals("Log dir should have 31 files before pruning", 31, logDir.listFiles().length)

        job.pruneLog(30)
        assertEquals("Log dir should have 29 file after prune", 29, logDir.listFiles().length)

        job.pruneLog(5)
        assertEquals("Log dir should have 4 file after prune", 4, logDir.listFiles().length)


    }

}
