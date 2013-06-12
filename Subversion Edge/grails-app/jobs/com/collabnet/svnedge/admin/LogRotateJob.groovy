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
package com.collabnet.svnedge.admin


import java.util.Date

import com.collabnet.svnedge.domain.LogConfiguration
import com.collabnet.svnedge.util.ConfigUtil

import org.quartz.CronTrigger
import org.quartz.SchedulerException
import org.quartz.Trigger

class LogRotateJob {

    def configUtil
    static def name = "com.collabnet.svnedge.admin.LogRotateJob"
    static def group = "Maintenance"
    def volatility = false

    static triggers = { 
    // artf4934, some OS's don't compile the quartz plugin correctly, so
    // this method of scheduling doesn't work.  Using a service bootstrap
    // method as workaround
    //  
    //    cron name: "LogRotateTrigger", group: group + "_Triggers", \
    //    startDelay: 60000, \
    //    cronExpression: "0 5 0 * * ?"
    }

    /** 
     * Schedule daily 12:05 am repeating trigger
     */
    static Trigger createTrigger() {
        def trigger = new CronTrigger("LogRotateTrigger",
            group + "_Triggers", name, group, "0 5 0 * * ?")
        return trigger
    }

    def pruneLog(daysToKeep) {
        def dataDir = configUtil.dataDirPath()
        def dataDirObj = new File(dataDir, "logs")
        Date today = new Date()
        Date lastDayToKeep = today - ((int) daysToKeep)
        long pruningTimestamp = lastDayToKeep.getTime()
        String[] entries = dataDirObj.listFiles()
        for (int i = 0; i < entries.length; i++) {
            String entry = entries[i]
            def entryObj = new File(entry)
            // do not remove the /logs/temp folder
            if (entryObj.isDirectory() && entryObj.name == "temp") {
                continue
            }
            long lastModified = entryObj.lastModified()
            if (lastModified < pruningTimestamp) {
                if (entryObj.delete() == false) {
                    log.info("Pruning: ${entry} failed.")
                }
            }
        }
    }

    def execute() {
        def lc = LogConfiguration.getConfig()
        if (lc.pruneLogsOlderThan != 0) {
            pruneLog(lc.pruneLogsOlderThan)
        }
    }
}
