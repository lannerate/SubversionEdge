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
import com.collabnet.svnedge.domain.statistics.Interval 
import com.collabnet.svnedge.domain.statistics.StatAction 
import com.collabnet.svnedge.domain.statistics.StatValue 
import com.collabnet.svnedge.domain.statistics.Statistic 
import com.collabnet.svnedge.util.StatisticsTime 

import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.quartz.SchedulerException

abstract class AbstractStatisticsService {

    public static int MAX_DISPLAY_POINTS = 500

    private app = ApplicationHolder.application

    // data collection job intervals
    protected Interval five_min
    protected Interval hour
    protected Interval day
    protected Interval week
    protected Interval thirty_days

    /**
     * Gets an i18n message from the messages.properties file without providing
     * parameters using the default locale.
     * @param key is the key in the messages.properties file.
     * @return the message related to the key in the messages.properties file
     * using the default locale.
     */
    protected def getMessage(String key) {
        def appCtx = app.getMainContext()
        return appCtx.getMessage(key, null, Locale.getDefault())
    }

    /**
     * Checks if a domain object can be created successfully.  Useful
     * for bootstrapping.
     */
    def check = { domain ->
        if (!domain.validate()) {
            domain.errors.allErrors.each {
                log.info("Creation failed: " + it)
                }
        }   
    }

    /**
     * Add default collect/delete intervals to the StatGroup.
     */
    def addDefaultActions = { statGroup ->

        createIntervals()
        StatAction raw = new StatAction(group: statGroup,
                                        collect:five_min, 
                                        delete:day, 
                                        consolidateSource:null)
        check(raw)
        raw.save()
        statGroup.addToActions(raw).save()
        StatAction derive1 = new StatAction(group: statGroup,
                                            collect:hour, 
                                            delete:week, 
                                            consolidateSource:raw)
        check(derive1)
        derive1.save()
        statGroup.addToActions(derive1).save()
        StatAction derive2 = new StatAction(group: statGroup,
                                            collect:day, 
                                            delete:thirty_days, 
                                            consolidateSource:derive1)
        check(derive2)
        derive2.save()
        statGroup.addToActions(derive2).save()
        if (!statGroup.getActions()) {
            log.error("No actions found!")
        } else {
            for (StatAction action: statGroup.getActions()) {
                log.info("Action = " + action)
            }
        }
    }

    /**
     * Creates the Interval rows if needed
     */
    protected void createIntervals() {
        five_min = Interval.findByName(
            StatisticsTime.FIVE_MINUTES.toString())
        if (!five_min) {
            five_min = new Interval(
                name: StatisticsTime.FIVE_MINUTES.toString(),
                seconds: StatisticsTime.FIVE_MINUTES.getSeconds())
            check(five_min)
            five_min.save()
        }
        hour = Interval.findByName(StatisticsTime.HOUR.toString())
        if (!hour) {
            hour = new Interval(name:StatisticsTime.HOUR.toString(),
                               seconds:StatisticsTime.HOUR.getSeconds())
            check(hour)
            hour.save()
        }
        day = Interval.findByName(StatisticsTime.DAY.toString())
        if (!day) {
            day = new Interval(name: StatisticsTime.DAY.toString(),
                               seconds: StatisticsTime.DAY.getSeconds())
            check(day)
            day.save()
        }
        week = Interval.findByName(StatisticsTime.WEEK.toString())
        if (!week) {
            week = new Interval(name: StatisticsTime.WEEK.toString(),
                                seconds: StatisticsTime.WEEK.getSeconds())
            check(week)
            week.save()
        }
        thirty_days = Interval.findByName(
            StatisticsTime.THIRTY_DAYS.toString())
        if (!thirty_days) {
            thirty_days = new Interval(
                name: StatisticsTime.THIRTY_DAYS.toString(),
                seconds: StatisticsTime.THIRTY_DAYS.getSeconds())
            check(thirty_days)
            thirty_days.save()
        }
    }

    /**
     *  Given a StatGroup and a span of time in seconds, choose the best
     *  avaiable collection interval to use for displaying a graph of this
     *  data.  Ideally, we want an interval that gives close to 
     *  MAX_DISPLAY_POINTS points, w/o going over.  If there is no such 
     *  interval, then we may return the smallest that is more than 
     *  MAX_DISPLAY_POINTS.  
     *  If no intervals are found, return null.
     *  Return the number of seconds in the best interval.
     */
    def getBestDisplayInterval(statGroup, span) {
        // the interval w/ the smallest number of points > MAX_DISPLAY_POINTS
        def smallestOver = null
        // the interval w/ the largest number of points < MAX_DISPLAY_POINTS
        def largestUnder = null
        def actions = statGroup.getActions()
        actions.each { action ->
            long actionInt = action.getCollect().getSeconds()
            if (span / actionInt > MAX_DISPLAY_POINTS) {
                if (smallestOver == null || smallestOver < actionInt) {
                    smallestOver = actionInt
                }
            } else {
                if (largestUnder == null || largestUnder > actionInt) {
                    largestUnder = actionInt
                }
            }
        }
        if (largestUnder != null) {
            return largestUnder
        } else {
            return smallestOver
        }
    }

    /**
     * Returns the difference in two dates in seconds.
     */
    def dateDiffInSec = { Date start, Date end ->
        return (end.getTime() - start.getTime()) / 1000
    }

    /**
     * Add a new DeleteStatJob for the StatGroup.  Statistics services
     * should call this during bootstrap.
     * We're assuming that the smallest delete interval is an appropriate
     * interval to run the job with.  This means that we don't guarantee
     * that data will be deleted exactly when it expires, but it should
     * be good for most reasonable settings of delete intervals.  If we
     * ever feel the need to guarantee this, we should use the greatest
     * common divisor instead.
     */
    def addDeleteJob(statGroup) {
        def minInterval = statGroup.getMinDeleteInterval()
        if (minInterval) {
            def params = ["statGroupName": statGroup.getName()]
            def deleteStatJob = new DeleteStatJob()
            try {
                def trigger = DeleteStatJob
                              .createTrigger(statGroup.getName()
                                             + "StatDeleteTrigger",
                                             minInterval * 1000, params)
                jobsAdminService.createOrReplaceTrigger(trigger)
                log.info("Created DeleteStatJob for " + statGroup.getName())
            } catch (SchedulerException ex) {
                log.warn("Failed to schedule DeleteStatJob for "
                          + statGroup.getName() + " due to exception.", ex)
            }
        } else {
            log.info("Not starting delete job for " + statGroup.getName() 
                     + " because there are no delete intervals.")
        }
    }

    def addConsolidateJob(statGroup) {
        def minInterval = statGroup.getMinConsolidateInterval()
        if (minInterval) {
            def params = ["statGroupName": statGroup.getName()]
            def consolidateStatJob = new ConsolidateStatJob()
            try {
                def trigger = ConsolidateStatJob
                              .createTrigger(statGroup.getName()
                                             + "StatConsolidateTrigger", 
                                             minInterval * 1000, params)
                jobsAdminService.createOrReplaceTrigger(trigger)
                log.info("Created ConsolidateStatJob for " 
                         + statGroup.getName())
            } catch (SchedulerException ex) {
                log.warn("Failed to start ConsolidateStatJob for "
                          + statGroup.getName() + " due to exception.", ex)
            }
        } else {
            log.info("Not starting consolidate job for " + statGroup.getName() 
                     + " because there are no consolidate intervals.")
        }
    }


    static idealStartTime(interval, containedTime) {
        return containedTime - (containedTime % interval)
    }

    static nextIdealTime(interval, startTime) {
        return startTime + (startTime % interval)
    }

    static getStatValue(stat, eventTime, interval) {
        def crit = StatValue.createCriteria()
        def values = crit.list {
            and {
                eq('statistic', stat)
                between('timestamp', eventTime - interval, eventTime)
                eq('interval', interval)
            }
            maxResults(1)
            order('timestamp', 'desc')
        }
        values? values[0] : null
    }

    /**
     *  Return the stat values for the given stat, in the time frame, 
     *  with the given interval, ordered by the timestamp.
     *  @param stat statistic
     *  @param startTime beginning of time window (in ms since 1970)
     *  @param endTime end of time window (in ms since 1970)
     *  @param interval only get statValues with this interval.  Optional.
     *  @param repository only get statValues with this repository. If null will
     *  only fetch rows having null repo value
     */
    static getStatValues(Statistic stat, Long startTime, Long endTime,
                         Long interval, Repository repo) {
        def crit = StatValue.createCriteria()
        def values = crit.list {
            and {
                eq('statistic.id', stat.id)
                if (interval) {
                    eq('interval', interval)
                }
                if (repo) {
                    eq('repo.id', repo.id)
                }
                else {
                    isNull('repo')
                }

                between('timestamp', startTime, endTime)
            }
            order('timestamp', 'asc')
        }
        return values
    }

    static getStatValues(stat, startTime, endTime) {
        getStatValues(stat, startTime, endTime, null, null)
    }

    static getStatValues(stat, startTime, endTime, interval) {
        getStatValues(stat, startTime, endTime, interval, null)
    }

    /**
     * Return a TreeMap of the average values of each stat in the statList,
     * where the timestamp is used as the key.  Then value of the Map is a 
     * Map with the key being the stat name, and the value being the
     * average value.  For instance,
     * [[1256163084988: ['statName1': 50, 'statName2': 25]],  
     *  [1256163085008: ['statName1': 67, 'statName2': 33]],
     *  [1256163084932: ['statName1: 88]]
     * If there is no data for a stat, it will be missing from the map.
     * If there is no data for any of the stats, there will still be a 
     * datapoint.  (The data returned is used for graphing, so empty data
     * points will still leave space on the graph).
     * This method should only be used with stats that are stored by 
     * idealStartTime.
     */
    def getValuesKeyedByIdealTime(Collection<Statistic> statList,
                                  Long startTime, Long endTime, 
                                  Long interval, Repository repo) {
        def map = new TreeMap()
        def idealStart = idealStartTime(interval, startTime)
        log.debug("startTime: " + startTime + ", endTime: " + endTime 
                 + ", interval: " + interval + "idealStart: " + idealStart)
        def points = ((endTime - idealStart) / interval)
        log.debug("num points: " + points)
        // seed the map with emptiness
        for (long time = idealStart; time < endTime; time += interval) {
            map[time] = [:]
        }
        
        statList.each { stat ->
            def statValues = getStatValues(stat, idealStart, endTime, interval, repo)
                     
            statValues.each { statValue ->
                def idealTime = idealStartTime(interval, statValue.getTimestamp())
                def hash = map[idealTime]
                if (hash == null) {
                    // very unexpected.  We have a data point that's not
                    // where we expect it to be (i.e. not evenly spaced and 
                    // ideal)
                    log.error("Non-ideal time found for " + stat.getName()
                        + " : " + statValue.getTimestamp())
                } else {
                    hash[stat.getName()] = statValue.getAverageValue()
                } 
            }
        }
        map
    }

    /**
     * Return a TreeMap of the average values of each repo for the stat,
     * where the timestamp is used as the key.  Then value of the Map is a 
     * Map with the key being the repo name, and the value being the
     * average value.  For instance,
     * [[1256163084988: ['repo1': 50, 'repo2': 25]],  
     *  [1256163085008: ['repo1': 67, 'repo2': 33]],
     *  [1256163084932: ['repo1': 88]]
     * If there is no data for a repo, it will be missing from the map.
     * If there is no data for any of the repos, there will still be a 
     * datapoint.  (The data returned is used for graphing, so empty data
     * points will still leave space on the graph).
     * This method should only be used with stats that are stored by 
     * idealStartTime.
     */
    def getRepoValuesKeyedByIdealTime(Statistic stat,
                                      Long startTime, Long endTime, 
                                      Long interval) {
        def map = new TreeMap()
        def idealStart = idealStartTime(interval, startTime)
        // seed the map with emptiness
        for (long time = idealStart; time < endTime; time += interval) {
            map[time] = [:]
        }
        
        Repository.list().each { repo ->
            def statValues = getStatValues(stat, idealStart, endTime, 
                                           interval, repo)
            statValues.each { statValue ->
                def hash = map[statValue.getTimestamp()]
                if (hash == null) {
                    // very unexpected.  We have a data point that's not
                    // where we expect it to be (i.e. not evenly spaced and 
                    // ideal)
                    log.error("Non-ideal time found for " + stat.getName()
                        + " : " + statValue.getTimestamp())
                } else {
                    hash[repo.getName()] = statValue.getAverageValue()
                } 
            }
        }
        map
    }

    /**
     *  Return the sum of the average values for all non-derived stat values
     *  for the given stat, in the time frame.
     *  @param stat statistic
     *  @param startTime beginning of time window (in ms since 1970)
     *  @param endTime end of time window (in ms since 1970)
     */
    static sumStatValue(stat, startTime, endTime) {
        def crit = StatValue.createCriteria()
        def value = crit.get {
            and {
                eq('statistic', stat)
                between('timestamp', startTime, endTime)
                eq('derived', false)
            }
            projections {
                sum('averageValue')
            }
        }
        // the value will be null if no rows are found (we could get fancy
        // and use coalesce, but that would require using a more complex
        // query), so we will check and return zero if that is the case
        if (!value) {
            value = 0
        }
        return value
    }

    /**
     * Return the last statValue for a given statistic.
     */
    def getLastStatValue(stat) {
         def crit = StatValue.createCriteria()
        def values = crit.list {
            and {
                eq('statistic', stat)
                eq('derived', false)
            }
            maxResults(1)
            order('timestamp', 'desc')
        }
        (values)? values[0] : null
    }

    /**
     * Return the last statValue for a given statistic by Repo
     * @param repo the Repository
     */
    def getLastStatValue(stat, repo) {
        def crit = StatValue.createCriteria()
        def values = crit.list {
            and {
                eq('statistic', stat)
                eq('derived', false)
                eq('repo', repo)
            }
            maxResults(1)
            order('timestamp', 'desc')
        }
        (values)? values[0] : null
    }

    /**
     * Return the last two raw statValues for a given statistic.
     */
    def getLastTwoStatValues(stat) {
        def crit = StatValue.createCriteria()
        def values = crit.list {
            and {
                eq('statistic', stat)
                eq('derived', false)
            }
            maxResults(2)
            order('timestamp', 'desc')
        }
        return values
    }
}
