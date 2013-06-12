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

import com.collabnet.svnedge.domain.Repository 
import com.collabnet.svnedge.domain.statistics.StatValue 
import com.collabnet.svnedge.domain.statistics.Statistic 

/**
 * Service for getting most-recent persisted statistics values. By extending
 * RealTimeStatisticsService, the same interface is presented but some methods
 * are overridden for using datastore instead of synchronous calculations.
 */
class LastCollectedStatisticsService extends RealTimeStatisticsService {

    def fileSystemStatisticsService

    /**
     * gets the time of the FileSystem data when using this service. Date of the
     * last statistics run is used here
     * @return Date or null
     */
    @Override
    Date getTimestampFileSystemData() {
        Statistic stat = fileSystemStatisticsService.getRepoUsedStat()
        StatValue val = fileSystemStatisticsService.getLastStatValue(stat)
        Long time = val?.timestamp
        return (time) ? new Date(time) : null
    }


    /**
     * returns the last disk-used data point, or null if the job has not run
     * @return Double bytes used or null
     */
    @Override
    public Double getSystemUsedDiskspace() {

        Statistic stat = fileSystemStatisticsService.getSysUsedStat()
        StatValue val = fileSystemStatisticsService.getLastStatValue(stat)
        return val?.lastValue

    }

    /**
     * returns the last disk-free data point, or null if the job has not run
     * @return Double bytes used or null
     */
    @Override
    public Double getRepoAvailableDiskspace() {

        Statistic stat = fileSystemStatisticsService.getRepoFreeStat()
        StatValue val = fileSystemStatisticsService.getLastStatValue(stat)
        return val?.lastValue

    }

    /**
     * returns the last disk-used data point by repo, or null if the job
     * has not run
     * @param repo
     * @return Double bytes used or null
     */
    @Override
    public Double getRepoUsedDiskspace(Repository repo) {

        Statistic stat = fileSystemStatisticsService.getRepoUsedStat()
        StatValue val = fileSystemStatisticsService.getLastStatValue(stat, repo)
        return val?.lastValue

    }

    /**
     * returns the last disk-usage data point by all repos, or null if the job
     * has not run
     * @return Double bytes used or null
     */
    @Override
    public Double getRepoUsedDiskspace() {
        Statistic stat = fileSystemStatisticsService.getRepoUsedStat()
        StatValue val = fileSystemStatisticsService.getLastStatValue(stat)
        return val?.lastValue
    }
}
