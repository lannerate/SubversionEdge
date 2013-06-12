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


import com.collabnet.svnedge.domain.Server
import com.collabnet.svnedge.domain.ServerMode
import java.text.SimpleDateFormat
import org.quartz.Trigger

/**
 * Manages the jobs in the console
 * @author Marcello de Sales (mdesales@collabn.net)
 */
class JobsAdminService {
    public static def REPLICA_GROUP = "Replica"

    boolean transactional = true

    def quartzScheduler

    def anyJobsRunning
    def anyJobsPaused

    private static final SimpleDateFormat FORMATTER = new SimpleDateFormat(
            "MMM dd, yyyy HH:mm:ss")

    def bootStrap = {
        log.info("Bootstrapping jobs...")
        resumeAll()
    }

    def resumeAll() {
        log.info("Resuming all relevant jobs...")
        quartzScheduler.resumeAll()
        def server = Server.getServer()
        if (server.mode != ServerMode.REPLICA) {
            log.info("Replication jobs are paused in server mode ${server.mode}")
            pauseGroup(REPLICA_GROUP)
        }
        else {
            log.info("Replication jobs are running")
        }
    }

    def pauseAll() {
        log.info("Pausing all existing jobs...")
        quartzScheduler.pauseAll()
    }

    def getHumanReadableSummary() {
        def metaData = quartzScheduler.getMetaData()
        return "Running since: ${metaData.runningSince}<br/>" +
                "Standby mode: ${metaData.inStandbyMode}<br/>" +
                "Number of jobs executed: ${metaData.numberOfJobsExecuted}"
    }

    def getJobSimpleDetails(executingContext) {
        def jobDetail = [name: executingContext.getJobDetail().getName(),
                group: executingContext.getJobDetail().getGroup(),
                description: executingContext.getJobDetail().getDescription()]
        return jobDetail
    }

    def getTriggerDetailsFromInstance(trigger) {
        def nextFireTime = ""
        try {
            nextFireTime = FORMATTER.format(trigger.getNextFireTime())
        } catch (Exception e) {
            nextFireTime = "None"
        }

        def previousFireTime = ""
        try {
            previousFireTime = FORMATTER.format(trigger.getPreviousFireTime())
        } catch (Exception e) {
            previousFireTime = "None"
        }

        def startTime = ""
        try {
            startTime = FORMATTER.format(trigger.getStartTime())
        } catch (Exception e) {
            startTime = "None"
        }

        def description = trigger.getDescription() ?: ""

        return [name: trigger.getName(), group: trigger.getGroup(),
                jobName: trigger.getJobName(), jobGroup: trigger.getJobGroup(),
                description: description, startTime: startTime,
                nextFireTime: nextFireTime, previousFireTime: previousFireTime]
    }

    def getTriggerStateString(stateInt) {
        switch (stateInt) {
            case Trigger.STATE_NORMAL:
                anyJobsRunning = true
                return "Running"
            case Trigger.STATE_PAUSED:
                anyJobsPaused = true
                return "Paused"
            case Trigger.STATE_COMPLETE: return "Just finished"
        }
    }

    def AreThereJobsRunning() {
        return anyJobsRunning
    }

    def AreThereJobsPaused() {
        return anyJobsPaused
    }

    def getTriggerDetailsFromScheduler(trigger) {
        def stateInt = quartzScheduler.getTriggerState(trigger.getName(),
                trigger.getGroup())
        return [state: getTriggerStateString(stateInt)]
    }

    def getJobsAndTriggersInfo() {
        def jobsTriggers = [:]
        //trigger group names (paused or not)
        def triggerGroupNames = quartzScheduler.getTriggerGroupNames()
        for (triggerGroupName in triggerGroupNames) {
            def triggerNames = quartzScheduler.getTriggerNames(triggerGroupName)
            anyJobsRunning = false
            anyJobsPaused = false
            for (triggerName in triggerNames) {
                def trigger = getTrigger(triggerName, triggerGroupName)
                def triggerDetails = getTriggerDetailsFromInstance(trigger)
                triggerDetails.putAll(getTriggerDetailsFromScheduler(trigger))

                def group = jobsTriggers[triggerDetails.jobGroup] ?: [:]
                group["triggerState"] = triggerDetails.state
                def job = group[triggerDetails.jobName] ?: []
                job << triggerDetails
                group[triggerDetails.jobName] = job
                jobsTriggers[triggerDetails.jobGroup] = group
            }
        }


        return jobsTriggers
    }

    def getJobExecutionDetails(context) {
        return [fireTime: FORMATTER.format(context.getFireTime()),
                ranTime: context.getJobRunTime(),
                nextFireTime: FORMATTER.format(context.getNextFireTime()),
                prevFireTime: FORMATTER.format(context.getPreviousFireTime()),
                fireCounter: context.getRefireCount()]
    }

    def getExecutingJobsInfo() {
        def executingCtxs = quartzScheduler?.getCurrentlyExecutingJobs()
        def groups = [:]
        try {
            if (executingCtxs) {
                for (execContext in executingCtxs) {
                    if (!execContext) continue
                    //building the details
                    def simpleDetails = getJobSimpleDetails(execContext)
                    def executionDetails = getJobExecutionDetails(execContext)
                    executionDetails.putAll(simpleDetails)

                    //building the response map
                    def previousList = groups[simpleDetails.group]
                    previousList << executionDetails
                    groups[simpleDetails.group] = previousList
                }
            }
        } catch (Exception e) {
            log.error(e)
        }
        return groups
    }

    def updateJobsScheduler(groupName, operation) {
        if (operation == "pauseAll") {
            if (groupName) {
                pauseGroup(groupName)
            } else {
                pauseAll()
            }
        } else
        if (operation == "resumeAll") {
            if (groupName) {
                resumeGroup(groupName)
            } else {
                resumeAll()
            }
        }
    }

    def pauseGroup(groupName) {
        // Scheduler.getPausedTriggerGroups only returns valid results if
        // triggers are paused using pauseTriggerGroup. If all the triggers
        // in a group are paused individually or by way of pauseJobGroup
        // the group is not flagged as paused.
        //quartzScheduler.pauseJobGroup(groupName)
        log.info("Pausing jobs in group ${groupName}")
        quartzScheduler.pauseTriggerGroup(groupName + "_Triggers")
    }

    def getPausedGroups() {
        def groupsWithTriggers = getJobGroupNames()
        def paused = quartzScheduler.getPausedTriggerGroups()
        def groups = []
        if (paused) {
            for (def triggerGroup: paused) {
                def index = triggerGroup.indexOf("_Triggers")
                if (index > 0) {
                    def g = triggerGroup.substring(0, index)
                    if (groupsWithTriggers.contains(g)) {
                        groups << g
                    }
                }
            }
        }
        return groups
    }

    def resumeGroup(groupName) {
        log.info("Resuming jobs in group ${groupName}")
        quartzScheduler.resumeTriggerGroup(groupName + "_Triggers")
    }

    /**
     * Reschedules the running job, by deleting the existing trigger with the 
     * given name and rescheduling the new one.
     * @param updateTriggerName is the name of an existing trigger.
     * @param triggerGroup the name of the trigger group.
     * @param newInterval is the new trigger interval to be used in the trigger.
     */
    def rescheduleJob(updateTriggerName, triggerGroupName, newInterval) {
        log.debug("Attempting reschedule the trigger ${updateTriggerName}")
        def triggerNames = quartzScheduler.getTriggerNames(triggerGroupName)
        for (triggerName in triggerNames) {
            if (triggerName != updateTriggerName) {
                continue
            }
            def trigger = getTrigger(triggerName, triggerGroupName)
            log.debug("Found trigger object ${trigger}")
            trigger.setRepeatInterval(newInterval)
            log.debug("Updated trigger with new interval of ${newInterval}")
            quartzScheduler.rescheduleJob(triggerName, triggerGroupName,
                    trigger);
            log.debug("Rescheduled the job for the trigger ${newInterval}")
        }
    }

    /**
     * create or update a trigger, depending on whether it exists
     * @param trigger to create or update
     * @return
     */
    def createOrReplaceTrigger(Trigger trigger) {
        def existingTrigger = getTrigger(trigger.name, trigger.group)
        if (existingTrigger) {
            log.info("Updating ${trigger.name}")
            quartzScheduler.rescheduleJob(trigger.name, trigger.group, trigger)
        }
        else {
            log.info("Scheduling ${trigger.name}")
            quartzScheduler.scheduleJob(trigger)
        }
    }

    // reschedule unscheduled trigger (which should be associated with a job)
    def scheduleTrigger(Trigger trigger) {
        quartzScheduler.scheduleJob(trigger)
    }

    def getTrigger(triggerName, triggerGroup) {
        quartzScheduler.getTrigger(triggerName, triggerGroup)
    }

    // return the Triggers for a given job
    def getTriggers(jobName, jobGroup) {
        quartzScheduler.getTriggersOfJob(jobName, jobGroup)
    }

    def getTriggerNamesInGroup(triggerGroup) {
        quartzScheduler.getTriggerNames(triggerGroup)
    }

    def removeTrigger(triggerName, triggerGroup) {
        quartzScheduler.unscheduleJob(triggerName, triggerGroup)
    }

    /**
     * Returns job groups which include at least one job with a trigger.
     */
    def getJobGroupNames() {
        def allGroups = quartzScheduler.getJobGroupNames() as List
        return allGroups ? allGroups.findAll {
            def triggerNames = quartzScheduler.getTriggerNames(it + "_Triggers")
            triggerNames && triggerNames.length > 0
        } : []
    }

    def getJobNames(groupName) {
        return quartzScheduler.getJobNames(groupName)
    }
}
