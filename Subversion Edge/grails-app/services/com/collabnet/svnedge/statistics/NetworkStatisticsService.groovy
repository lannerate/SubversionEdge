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

import com.collabnet.svnedge.domain.MonitoringConfiguration
import com.collabnet.svnedge.domain.statistics.Category;
import com.collabnet.svnedge.domain.statistics.StatGroup 
import com.collabnet.svnedge.domain.statistics.StatValue 
import com.collabnet.svnedge.domain.statistics.Statistic 
import com.collabnet.svnedge.domain.statistics.StatisticType 
import com.collabnet.svnedge.domain.statistics.Unit 

import org.quartz.SchedulerException

class NetworkStatisticsService extends AbstractStatisticsService {

    boolean transactional = true

    def operatingSystemService
    def networkingService
    def jobsAdminService

    protected static String RECEIVED_STAT_NAME = "BytesIn"
    protected static String SENT_STAT_NAME = "BytesOut"

    public static String CATEGORY_NAME = "Network"
    public static String STATGROUP_NAME = "NetworkThroughput"
    public static String TRIGGER_NAME = "networkTrigger"
    public static String BPS_UNIT_NAME = "Bytes per Second"

    def bytesInId
    def bytesOutId
    def statGroupId

    def bootStrap = {
        log.info("Bootstrapping network statistics")
        if (!getStatGroup()) {
            this.createNetworkStatistics()
        }
        def interval = getStatGroup().getRawInterval() * 1000
        def params = ["serviceName": "networkStatisticsService"]
        def statCollectJob = new StatCollectJob()
        try {
            def trigger = StatCollectJob
                .createTrigger(TRIGGER_NAME, interval, params, 12800L)
            jobsAdminService.createOrReplaceTrigger(trigger)
            log.info("creating stat collection job at interval (millis): " + interval)
        } catch (SchedulerException ex) {
            log.warn("Did not schedule StatCollectJob due to exception.", ex)
        }
        addDeleteJob(getStatGroup())
        addConsolidateJob(getStatGroup())
    }

    def getStatGroup() {
        def group = StatGroup.get(statGroupId)
        if (!group) {
            group = StatGroup.findByName(STATGROUP_NAME)
        }
        group
    }

    /**
     * @return the statistics object for the bytes in.
     */
    def getBytesIn() {
        def stat = Statistic.get(bytesInId)
        if (!stat) {
            stat = Statistic.findByName(RECEIVED_STAT_NAME)
        }
        stat
    }

    /**
    * @return the statistics object for the bytes out.
    */
    def getBytesOut() {
        def stat = Statistic.get(bytesOutId)
        if (!stat) {
            stat = Statistic.findByName(SENT_STAT_NAME)
        }
        stat
    }

    /**
    * @return the key for the InRate of the getThroughputRates data.
    */
    def getThroughputInRateName() {
        getBytesIn().getName()
    }

    /**
     * @return the key for the OutRate of the getThroughputRates data.
     */
    def getThroughputOutRateName() {
        getBytesOut().getName()
    }

    /**
     * Called from the quartz service
     */
    def collectData() {
        if (MonitoringConfiguration.config?.networkEnabled) {
            collectNetworkStats()
        }
    }
    
    private def collectNetworkStats() {
        log.debug("Collecting network transmitted and received values")
        def now = new Date().getTime()
        def byteValues = parseBytes()
        def interval = getStatGroup().getRawInterval() * 1000
        def receivedValue = new StatValue(timestamp: now,
                                          interval: interval,
                                          minValue: byteValues[0],
                                          maxValue: byteValues[0],
                                          averageValue: byteValues[0],
                                          lastValue: byteValues[0],
                                          statistic: getBytesIn())
        receivedValue.save()
        def transmittedValue = new StatValue(timestamp: now,
                                             interval: interval,
                                             minValue: byteValues[1],
                                             maxValue: byteValues[1],
                                             averageValue: byteValues[1],
                                             lastValue: byteValues[1],
                                             statistic: getBytesOut())
        transmittedValue.save()
    }

    def createNetworkStatistics() {
        def category = Category.findByName(CATEGORY_NAME)
        if (!category) {
            category = new Category(name: CATEGORY_NAME)
            check(category)
            category.save()
        }
        def unit = Unit.findByName(NetworkStatisticsService.BPS_UNIT_NAME)
        if (!unit) {
            unit = new Unit(name: NetworkStatisticsService.BPS_UNIT_NAME,
                            minValue: 0)
            check(unit)
            unit.save()
        }
        def netIf = networkingService.selectedInterface.name
        def statGroup = StatGroup.findByName(STATGROUP_NAME)
        if (!statGroup) {
            statGroup = new StatGroup(name: STATGROUP_NAME, 
                title: getMessage("statistics.graph.leftNav.throughput") +
                    " (${netIf})", unit: unit, category: category)
            check(statGroup)
            category.addToGroups(statGroup).save()
            // save the statGroup so that we can get id info
            statGroup.save()
            addDefaultActions(statGroup)
        }
        statGroupId = statGroup.getId()
        log.debug("StatGroupId: " + statGroupId)
        def bytesIn = Statistic.findByName(RECEIVED_STAT_NAME)
        if (!bytesIn) {
            bytesIn = new Statistic(name: RECEIVED_STAT_NAME, title:
                getMessage("statistics.graph.leftNav.rate.receivedData") +
                    " (${netIf})", type: StatisticType.GAUGE, group: statGroup)
            check(bytesIn)
            statGroup.addToStatistics(bytesIn)
            // save the bytesIn so that we can get id info
            bytesIn.save()
        }
        bytesInId = bytesIn.getId()
        def bytesOut = Statistic.findByName(SENT_STAT_NAME)
        if (!bytesOut) {
            bytesOut = new Statistic(name: SENT_STAT_NAME, title: 
                getMessage("statistics.graph.leftNav.rate.transmittedData") +
                    "(${netIf})", type: StatisticType.GAUGE, group: statGroup)
            check(bytesOut)
            statGroup.addToStatistics(bytesOut).save()
            // save the bytes out so that we can get id info
            bytesOut.save()
        }
        bytesOutId = bytesOut.getId()
        log.info("Bootstrapping network statistics done for interface " +
            "'${netIf}'.")
    }

    /**
    * If we have enough data to do so, return information about our
    * current throughput levels.  Enough information means at least one
    * recent statValue has been recorded.  If we have only one, we can
    * take a current snapshot of the values and use it to calculate.
    * If we have two data points, we can just use those.
    * We will also return the interval between data points we use.
    * The data returned is of the form:
    * [[rateIn, timeDiffIn], [rateOut, timeDiffOut]]
    * But might be:
    * [null, null]
    */
   def getCurrentThroughput() {
       def cmdIn = getBytesIn()
       def cmdOut = getBytesOut()

       def rateTimeIn = calculateCurrentRate(cmdIn)
       def rateTimeOut = calculateCurrentRate(cmdOut)

       return [rateTimeIn, rateTimeOut]
   }

   /**
    * Calculate the current rate for a statistic.  Returns both
    * the rate and the time it's over.
    */
   def calculateCurrentRate(stat) {
       def value1, value2, time1, time2
       def recentValues = getLastTwoStatValues(stat)
       if (recentValues.size() == 0) {
           // not enough data
           return null
       } else if (recentValues.size() == 1) {
           // we have data, but we'll need to grab data for now
           def now = new Date().getTime()
           def currentValue = getCurrentValue(stat)
           value1 = recentValues[0].getAverageValue()
           time1 = recentValues[0].getTimestamp()
           value2 = currentValue
           time2 = now
       } else {
           value1 = recentValues[1].getAverageValue()
           time1 = recentValues[1].getTimestamp()
           value2 = recentValues[0].getAverageValue()
           time2 = recentValues[0].getTimestamp()
       }
       // do a few sanity checks on our values
       if (time2 < time1) {
           msg = "Time values for rate calculation are misordered."
           log.error(msg)
           throw new RuntimeException(msg)
       }
       if (value2 < value1) {
           log.error("Byte values are decreasing instead of increasing. " +
                         "Cannot calculate rate.")
           return null
       }
       def rate = (value2 - value1) / (time2 - time1)
       return [rate, (time2 - time1)]
   }

   /**
    * @return the current value for the given stat.
    */
   def getCurrentValue(stat) {
       def values = parseBytes()
       if (stat.equals(getBytesIn())) {
           return values[0]
       } else if (stat.equals(getBytesOut())) {
           return values[1]
       }
       return null
   }

   /**
    * The normalized bytesIn/bytesOut rates for a given time frame.
    * @param start Date
    * @param end Date
    * @return the rates
    */
   def getThroughputRates(start, end) {
       def startTime = start.getTime()
       def endTime = end.getTime()
       def idealInterval = getBestDisplayInterval(getStatGroup(),
                                                  dateDiffInSec(start,
                                                                end)) * 1000
       def inValues = getStatValues(getBytesIn(), startTime, endTime,
                                    idealInterval)
       def outValues = getStatValues(getBytesOut(), startTime, endTime,
                                     idealInterval)
       def idealStart = idealStartTime(idealInterval, startTime)
       if (startTime > idealStart) {
           // make sure we start after the startTime
           // (the idealStartTime goes backwards)
           idealStart += idealInterval
       }
       log.debug("startTime: " + startTime + ", endTime: " + endTime
                 + ", interval: " + idealInterval + "idealStart: "
                 + idealStart)
       /* we store values, but we're really interested in rates
        * we want to calculate rates at well-defined times for simplicity
        * so we'll look for data near (within interval) of the times we want
        * we'll need one point before and one after.  Calculate the rate
        * and assign it to that idealized time point.
        */
       def rates = new TreeMap()
       def inValuesIndex = 0
       def outValuesIndex = 0
       for (long time = idealStart; time < endTime; time += idealInterval) {
           def inRateIndex =
               getRate(inValuesIndex, inValues, time, idealInterval)
           def inRate = inRateIndex[0]
           inValuesIndex = inRateIndex[1]
           def outRateIndex =
               getRate(outValuesIndex, outValues, time, idealInterval)
           def outRate = outRateIndex[0]
           outValuesIndex = outRateIndex[1]
           rates[time] = [(getThroughputInRateName()): inRate,
               (getThroughputOutRateName()): outRate]
       }
       rates
   }

   /**
    * @param index
    * @param values
    * @param time
    * @param interval
    * @return the normalized rate at the time, if one can be calculated.
    */
   def getRate(index, values, time, interval) {
       def rate = null
       // look for a value with timestamp before time but within interval
       def lowValue = null
       for(; index < values.size() && values[index].timestamp <= time;
           index++) {
           if (values[index].timestamp + interval >= time) {
               lowValue = values[index]
           }
       }
       if (lowValue) {
           /* the high value will be the next value (lowValue is already
            * the closest lower than time), unless that value is too high
            */
           if (index < values.size() &&
                   values[index].timestamp <= (time + interval)) {
               def highValue = values[index]
               rate = (highValue.averageValue - lowValue.averageValue) / 
                   (highValue.timestamp - lowValue.timestamp) * 1000
           }
       }
       [rate, index]
   }

   /**
    * The bytes received/transmitted for the currently selected interface are
    * retrieved from the operating system service using the SIGAR framework.
    * Any compatibility in different OSs must refer to its documentation.
    * @return a list containing bytesIn, bytesOut.
    */
   def parseBytes() {
       def bytesReceived
       def bytesTransmitted
       def ifName = networkingService.selectedInterface.name
       try {
           def ifStats = networkingService.getNetworkInterfaceStatistics(ifName)
           bytesReceived = ifStats.getRxBytes()
           bytesTransmitted = ifStats.getTxBytes()
           def rFormatted = operatingSystemService.formatBytes(bytesReceived)
           def tFormatted = operatingSystemService.formatBytes(bytesTransmitted)
           log.debug("Network Interface: ${ifName}")
           log.debug("Bytes received: ${bytesReceived} (${rFormatted})")
           log.debug("Bytes transmitted: ${bytesTransmitted} (${tFormatted})")

       } catch (Exception problemsNetworkIf) {
           log.error("An error occurred while capturing the Network " +
               "statistics for '${ifName}': " + problemsNetworkIf.message)
           bytesReceived = 0
           bytesTransmitted = 0
       }

       return [bytesReceived, bytesTransmitted]
   }
   
   // reformulate a number to something in the given magnitude
   // i.e. 60,000 with mag 5 -> 6
   public applyMag(number, mag, truncation) {
       if (number != null) {
           operatingSystemService.truncate(number.floatValue() / (1024**mag),
                                     truncation)
       } else {
           null
       }
   }
}
