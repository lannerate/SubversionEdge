/*
 * CollabNet Subversion Edge
 * Copyright (C) 2012, CollabNet Inc. All rights reserved.
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

class TestUtil {

    static File createTestDir(String prefix) {
        def testDir = File.createTempFile(prefix + "-test", null)
        // we want a dir, not a file, so delete and mkdir
        testDir.delete()
        testDir.mkdir()
        // TODO This doesn't seem to work, might need to delete in teardown
        //testDir.deleteOnExit()
        return testDir
    }

    /**
     * creates a repo-like directory
     * @param repoName repo to create
     * @param repoParentDir file in which to create the repo
     * @return
     */
    static File createMockRepo(String repoName, File repoParentDir) {

        def entities = [ [dir:'conf'], [dir:'db'], [dir:'hooks'], [dir:'locks'],
                [file:'format'], [file:'db/current'],
                [file:'db/fsfs.conf'], [file:'db/min-unpacked-rev'],
                [file:'format'], [file:'db/uuid'], [file:'db/fs-type'],
                [file:'db/txn-current'], [file:'db/txn-current-lock']
        ]
        File repoDir
        File repoFile

        def newRepo = new File(repoParentDir.absolutePath, repoName)
        newRepo.mkdir()

        entities.each {
            if (it['dir'] != null) {
                repoDir = new File(newRepo, it['dir'])
                repoDir.mkdir()
            } else if (it['file'] != null) {
                repoFile = new File(newRepo, it['file'])
                repoFile.createNewFile()
            }
        }
        return newRepo
    }
}
