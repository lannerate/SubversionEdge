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



import com.collabnet.svnedge.domain.Server 

import org.quartz.CronTrigger
import org.quartz.JobDataMap
import org.quartz.SimpleTrigger
import org.quartz.Trigger
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

/**
 * This job calls the collectData method on a statistics service on a regular
 * interval.
 */
class StatCollectJob implements ApplicationContextAware {
    static String name = "com.collabnet.svnedge.statistics.StatCollectJob"
    static String group = "Statistics"
    static String triggerGroup = "Statistics_Triggers"
    def volatility = false

    ApplicationContext appCtx

    static triggers = {}

    def execute(context) {
        if (Server.getServer()) {
            doExecute(context)
        }
    }

    private def doExecute(context) {
        log.debug("Executing the StatCollectJob...")
        def dataMap = context.getMergedJobDataMap()
        def serviceName = dataMap.get("serviceName")
        def service = appCtx?.getBean(serviceName)
        if (!service) {
            log.error("service " + serviceName + " not found.")
        }
        service?.collectData()
    }

    void setApplicationContext(ApplicationContext appContext) {
        appCtx = appContext
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

    /** 
     * Create an infinitely repeating simple trigger with the given name
     * and interval.
     */
    static Trigger createCronTrigger(triggerName, cron, params) {
        def trigger = new CronTrigger(triggerName, triggerGroup,
                name, group, cron)
        trigger.setJobDataMap(new JobDataMap(params))
        return trigger
    }
}
