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



package com.collabnet.svnedge.console

import com.collabnet.svnedge.domain.Repository
import com.collabnet.svnedge.util.ConfigUtil
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * Utility methods related to console background jobs
 */
class BackgroundJobUtil {

    static Log log = LogFactory.getLog(BackgroundJobUtil.class)
    
    public static enum JobType { HOTCOPY, DUMP, CLOUD, VERIFY }

    /**
     * creates a File handle for capturing job output
     * @param repoName
     * @param jobType the JobType
     * @return a File
     */
    public static File prepareProgressLogFile(repoName, jobType) {
        File tempLogDir = new File(ConfigUtil.logsDirPath(), "temp")
        if (!tempLogDir.exists()) {
            tempLogDir.mkdirs()
        }
        return new File(tempLogDir, getProgressLogFileName(
                repoName, jobType))
    }

    /**
     * creates a file name for capturing job output
     * @param repoName
     * @param jobType
     * @return
     */
    public static String getProgressLogFileName(repoName, jobType) {
        return "job-progress-" + repoName + "-" +
                jobType.toString().toLowerCase() +
                ".log"
    }

    /**
     * Creates a trigger name based on repo and scheduling details. This
     * method is exposed, so that it could be used in migrating existing
     * backups when upgrading from 2.3 to 3.0.  It might be restricted in
     * later releases.
     * @param repo Repo
     * @param bean a DumpBean instance
     * @return String trigger name
     */
    public static String generateTriggerName(Repository repo, DumpBean bean) {
        def jobType = (bean.cloud ? BackgroundJobUtil.JobType.CLOUD :
            (bean.hotcopy ? BackgroundJobUtil.JobType.HOTCOPY : BackgroundJobUtil.JobType.DUMP))
        def schedule = (bean.backup) ? bean.schedule : null
        return generateTriggerName(repo, jobType, schedule)
    }

    /**
     * creates a quartz trigger name based on repo and job type. If a ScheduleBean instance is
     * provided, it will be appeneded to the trigger name
     * @param repo
     * @param type
     * @param schedule
     * @return String trigger name
     */
    public static String generateTriggerName(Repository repo, JobType type, SchedulerBean schedule) {
        def tName = "${repo.name}-" + type.toString().toLowerCase()
        if (schedule) {
            tName += "-" + (schedule.frequency == SchedulerBean.Frequency.WEEKLY ?
                schedule.dayOfWeek : 'X') + 'T'
            tName += (schedule.frequency == SchedulerBean.Frequency.HOURLY) ?
                'HH' : pad(schedule.hour)
            tName += pad(schedule.minute)
            tName += (schedule.second < 0) ? "00" : pad(schedule.second)
        }
        return tName
    }

    /**
     * converts a SchedulerBean instance into a cron expression
     * @param schedule SchedulerBean instance
     * @return the equivalent cron expression
     */
    public static String getCronExpression(SchedulerBean schedule) {
        if (!schedule.frequency || schedule.frequency == SchedulerBean.Frequency.NOW) {
            schedule.frequency = SchedulerBean.Frequency.ONCE
            Calendar cal = Calendar.getInstance()
            cal.setTimeInMillis(System.currentTimeMillis() + 2000)
            schedule.second = cal.get(Calendar.SECOND)
            schedule.minute = cal.get(Calendar.MINUTE)
            schedule.hour = cal.get(Calendar.HOUR_OF_DAY)
            schedule.dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
            schedule.month = cal.get(Calendar.MONTH) + 1 // Calendar uses 0 for first month
            schedule.year = cal.get(Calendar.YEAR)
        }
        String seconds = (schedule.second < 0) ? "0" : "${schedule.second}"
        String minute = " ${schedule.minute}"
        String hour = " ${schedule.hour}"
        String dayOfMonth = " *"
        String month = " *"
        String dayOfWeek = " ?"
        String year = ""
        switch (schedule.frequency) {
            case SchedulerBean.Frequency.WEEKLY:
                dayOfWeek = " ${schedule.dayOfWeek}"
                dayOfMonth = " ?"
                break
            case SchedulerBean.Frequency.HOURLY:
                hour = " *"
                break
            case SchedulerBean.Frequency.DAILY:
                break
            case SchedulerBean.Frequency.ONCE:
                dayOfMonth = " ${schedule.dayOfMonth}"
                month = " ${schedule.month}"
                year = " ${schedule.year}"
        }
        
        return seconds + minute + hour + dayOfMonth +
                month + dayOfWeek + year
    }

    private static String pad(int value) {
        return (value < 10) ? "0" + value : String.valueOf(value)
    }


}
