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
package com.collabnet.svnedge.statistics

import com.collabnet.svnedge.domain.integration.ReplicatedRepository 
import java.util.Date;

/**
 * Service for getting up-to-date statistics values.
 */
class RealTimeStatisticsService {

    def networkStatisticsService
    def fileSystemStatisticsService

    boolean transactional = true


    /**
     * gets the time of FileSystem data when using this service. Real time
     * data is timestamped "now"
     * @return Date or null
     */
    Date getTimestampFileSystemData() {
        return new Date()
    }

    /**
     * Returns the list of repository status and the number of repos in
     * that state.
     * @return a list of repoStatus and number of repos.
     */
    def getReposStatus() {
        def crit = ReplicatedRepository.createCriteria()
        def results = crit.list {
            projections {
                groupProperty("status")
                rowCount()
            }
        }
        def mapResults = []
        results.each { 
            mapResults.add([status: it[0], count: it[1]])
        }
        return mapResults
    }

    /**
     * Returns the amount of disk space used by the repositories.
     * @return the space for repositories (in bytes)
     */
    def getRepoUsedDiskspace() {
        fileSystemStatisticsService.getRepoUsedDiskspace()
    }

    /**
     * Returns the amount of disk space available for the repositories.
     * @return the space for repositories (in bytes)
     */
    def getRepoAvailableDiskspace() {
        fileSystemStatisticsService.getRepoAvailableDiskspace()
    }

    /**
     * Returns the total space in the partition containing the repo storage.
     * @return the space for repo storage (in bytes)
     */
    def getRepoTotalDiskspace() {
        fileSystemStatisticsService.getRepoTotalDiskspace()
    }

    /**
     * Returns the total amount of diskspace in use.
     * @return the total diskspace in use (in bytes)
     */
    def getSystemUsedDiskspace() {
        fileSystemStatisticsService.getSystemUsedDiskspace()
    }

    /**
     * Returns the total amount of diskspace total.
     * @return the total diskspace (in bytes)
     */
    def getSystemTotalDiskspace() {
        fileSystemStatisticsService.getSystemTotalDiskspace()
    }

    /**
     * Returns the throughputs (in, out) on the primary interface, if 
     * available.
     * @return an array containing throughputIn, timeIntervalIn, throughputOut,
     *         timeIntervalOut.  The throughputs are in b/s, the timeIntervals
     *         are in s.
     */
    def getThroughput() {
        def throughputIn
        def timeIntervalIn
        def throughputOut
        def timeIntervalOut
        // values from the statistics service are in b/ms and ms
        def throughput = networkStatisticsService.getCurrentThroughput()
        if (throughput == null || throughput[0] == null) {
            throughputIn = null
            timeIntervalIn = null
        } else {
            throughputIn = throughput[0][0] * 1000
            timeIntervalIn = throughput[0][1] / 1000
        }
        if (throughput == null || throughput[1] == null) {
            throughputOut = null
            timeIntervalOut = null
        } else {
            throughputOut = throughput[1][0] * 1000
            timeIntervalOut = throughput[1][1] / 1000
        }
        return [throughputIn, timeIntervalIn, throughputOut, timeIntervalOut]
    }
}
