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

import com.collabnet.svnedge.domain.statistics.StatAction 
import com.collabnet.svnedge.domain.statistics.StatValue 
import com.collabnet.svnedge.domain.statistics.Statistic
import com.collabnet.svnedge.domain.Repository

/**
 * The class handles consolidating data from smaller intervals into 
 * larger ones.  For instance, if you had data like:
 *
 *  min      0  1  2  1   
 *  max      0  1  2  1
 *  avg      0  1  2  1
 *  last     0  1  2  1
 *  time     0  1  2  3
 *  interval 1  1  1  1
 * 
 *  You could consolidate it into a single point like:
 *
 *  min      0
 *  max      2
 *  avg      1
 *  last     1
 *  time     0
 *  interval 4
 *
 *  Exactly how data is consolidated is determined by the StatActions
 *  associated with a StatGroup.
 */
class ConsolidateStatisticsService {
    
    def consolidate(statGroup) {
        def sortedActions = statGroup.getActions().sort{ 
            it.getCollect().getSeconds() 
        }
        for (StatAction action: sortedActions) {
            if (action.getConsolidateSource()) {
                consolidateData(statGroup, action.getConsolidateSource(), 
                                action)
            }
        }
    }

    /**
     * Consolidate StatValues from the smallerAction to the largerAction,
     * if possible (smaller here indicates smaller collection intervals in 
     * seconds).
     */
    def consolidateData(statGroup, smallAction, largeAction) {
        for (Statistic stat: statGroup.getStatistics()) {
            def smallInterval = smallAction.getCollect().getSeconds() * 1000
            def largeInterval = largeAction.getCollect().getSeconds() * 1000
            def lastSmallSV = getLastStatValue(stat, smallInterval)
            def lastLargeSV = getLastStatValue(stat, largeInterval)
            if (!lastSmallSV) {
                // we can't consolidate if there is no data in the smaller
                // action
                continue
            }
            def smallLastTime = lastSmallSV.getTimestamp() + smallInterval
            def largeLastTime
            if (!lastLargeSV) {
                // if we haven't consolidated data for this action
                largeLastTime = getFirstStatValue(stat, smallInterval)
                    .getTimestamp()
                largeLastTime = AbstractStatisticsService
                    .idealStartTime(largeInterval, largeLastTime)
            } else {
               largeLastTime = lastLargeSV.getTimestamp() + largeInterval
            }
            while ((largeLastTime + largeInterval) <= smallLastTime) {

                // first, consolidate general stats having no repository id
                def values = getStatValues(stat, largeLastTime,
                                           largeLastTime + largeInterval,
                                           smallInterval, null)
                def consolidatedStatValue = rollupStatValues(values)
                consolidatedStatValue.timestamp = largeLastTime
                consolidatedStatValue.interval = largeInterval
                consolidatedStatValue.save()

                // second, consolidate stats that *do* pertain to repository
                Repository.list().each { it ->
                    values = getStatValues(stat, largeLastTime,
                                           largeLastTime + largeInterval,
                                           smallInterval, it)
                    consolidatedStatValue = rollupStatValues(values)
                    consolidatedStatValue.repo = it
                    consolidatedStatValue.timestamp = largeLastTime
                    consolidatedStatValue.interval = largeInterval
                    consolidatedStatValue.save()
                }
                largeLastTime += largeInterval
            }
        }
    }

    /**
     * Given a list of stat values, this method will average into a new
     * consolidated value
     * @param values the StatValues to average
     * @return a new "consolidated" statvalue
     */
    private StatValue rollupStatValues(List values) {
        def min = null
        def max = null
        def avg = null
        def avgSum = null
        def avgCnt = 0
        def last = null
        // assuming that the Statistic of the entire group is the same, so we can
        // just grab the first 
        def stat = (values) ? values.first().statistic : null

        for (StatValue value: values) {
            if (min == null || value.getMinValue() < min) {
                min = value.getMinValue()
            }
            if (max == null || value.getMaxValue() > max) {
                max = value.getMaxValue()
            }
            if (avgSum == null) {
                avgSum = value.getAverageValue()
                avgCnt++
            } else if (value.getAverageValue() != null) {
                avgSum += value.getAverageValue()
                avgCnt++
            }
        }
        if (avgCnt != 0) {
            avg = avgSum / (new Float(avgCnt))
        }
        // The values should be in ascending order by time, so the last
        // is the last value, if it exists.
        if (values && values.size() > 0 && values[-1]) {
            last = values[-1].getLastValue()
        }
        // create the new StatValue with the data
        def consValue = new StatValue(minValue: min,
                                      maxValue: max,
                                      averageValue: avg,
                                      lastValue: last,
                                      derived: true,
                                      statistic: stat)

        return consValue
    }

    /**
     * Return the last StatValue associated with a given Statistic and
     * interval.  May return null if no StatValues are found.
     */
    def getLastStatValue(stat, interval) {
        def crit = StatValue.createCriteria()
        def values = crit.list {
            and {
                eq('statistic', stat)
                eq('interval', interval)
            }
            maxResults(1)
            order('timestamp', 'desc')
        }
        (values)? values[0] : null
    }

    /**
     * Return the first StatValue associated with a given Statistic and
     * interval.  May return null if no StatValues are found.
     */
    def getFirstStatValue(stat, interval) {
        def crit = StatValue.createCriteria()
        def values = crit.list {
            and {
                eq('statistic', stat)
                eq('interval', interval)
            }
            maxResults(1)
            order('timestamp', 'asc')
        }
        (values)? values[0] : null
    }

    /**
     * Get the StatValues between the startTime and endTime.
     */ 
    def getStatValues(stat, startTime, endTime, interval, repository) {
        def crit = StatValue.createCriteria()
        def values = crit.list {
            and {
                eq('statistic', stat)
                eq('interval', interval)
                between('timestamp', startTime, endTime)
                // we want between exclusive of endTime
                lt('timestamp', endTime)
                
                // add repo restriction or explicit null
                if (repository) {
                    eq('repo', repository)
                }
                else {
                    isNull('repo')
                }
            }
            order('timestamp', 'asc')
        }
        return values
    }
}
