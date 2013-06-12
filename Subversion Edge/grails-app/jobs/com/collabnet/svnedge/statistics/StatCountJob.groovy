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


import com.collabnet.svnedge.domain.statistics.StatGroup 
import com.collabnet.svnedge.domain.statistics.StatValue 
import com.collabnet.svnedge.domain.statistics.StatisticType 
import com.collabnet.svnedge.statistics.AbstractStatisticsService
import org.quartz.JobDataMap
import org.quartz.SimpleTrigger
import org.quartz.Trigger

/**
 * This job handles inserting zero values into the StatValue table for
 * counter statistics.  Counter stats are event-driven, but there is no
 * guarantee that any event will happen during a given time-slice: adding
 * zeros with this job helps distinguish between system-down times and times
 * when the system was running, but no events occurred. 
 */
class StatCountJob {
    static def name = "com.collabnet.svnedge.statistics.StatCountJob"
    static def group = "Statistics"
    static def triggerGroup = "Statistics_Triggers"

    static triggers = {}

    def execute(context) {
        log.debug("Executing the StatCountJob...")
        def dataMap = context.getMergedJobDataMap()
        def statGroupName = dataMap.get("statGroupName")
        log.debug("StatGroupName :" + statGroupName)
        def statGroup = StatGroup.findByName(statGroupName)
        def currentTime = new Date().getTime()
        def interval = statGroup.getRawInterval() * 1000
        def statistics = statGroup.getStatistics()
        for (stat in statistics) {
            log.debug("StatCountJob for :" + stat)
            if (!stat.getType().equals(StatisticType.COUNTER)) {
                continue
            }
            // we sync on the stat to ensure that we don't collide with
            // the services which are adding to the counters.
            synchronized(stat) {
                try {
                    // check if a statValue already exists
                    def statValue = AbstractStatisticsService
                        .getStatValue(stat, currentTime, interval)
                    if (!statValue) {
                        statValue = new StatValue(timestamp: 
                                                  AbstractStatisticsService
                                                  .idealStartTime(interval, 
                                                                  currentTime),
                                                  interval: interval,
                                                  minValue: 0,
                                                  maxValue: 0,
                                                  averageValue: 0,
                                                  lastValue: 0,
                                                  statistic: stat)
                        statValue.save()
                    }
                } catch (Exception e) {
                    log.error("Exception occurred: " + e)
                }
            }
        }
    }

    /** 
     * Create an infinitely repeating simple trigger with the given name
     * and interval.
     */
    static Trigger createTrigger(triggerName, interval, params, startDelay) {
        def trigger = new SimpleTrigger(triggerName, triggerGroup, 
                                        SimpleTrigger.REPEAT_INDEFINITELY, 
                                        interval)
        trigger.setJobName(name)
        trigger.setJobGroup(group)
        trigger.setJobDataMap(new JobDataMap(params))
        trigger.setStartTime(new Date(System.currentTimeMillis() + startDelay))
        trigger
    }
}
