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
package com.collabnet.svnedge.controller.admin

import org.codehaus.groovy.grails.plugins.springsecurity.Secured

@Secured(['ROLE_ADMIN','ROLE_ADMIN_SYSTEM'])
class JobsAdminController {

    //injecting the jobs service
    def jobsAdminService

    def index = { 
        redirect(action:"show")
    }

    def show = {
        def summary = jobsAdminService.getHumanReadableSummary()

        //transformation needed since the HTML does not break to the next
        //line if the summary text is on the same line.
        def charactersWide = 0
        StringBuilder newSummary = new StringBuilder()
        def stillNeeds = false
        for(character in summary) {
            if ((++charactersWide % 35) == 0)
                if (character != " ") {
                    stillNeeds = true
                    newSummary << character
                } else {
                    newSummary << "$character<BR>"
                }
            else {
                if (stillNeeds && character == " ") {
                    newSummary << "$character<BR>"
                    stillNeeds = false
                } else {
                    newSummary << character
                }
            }
        }

        return [groupTriggers: jobsAdminService.getJobsAndTriggersInfo(),
                anyJobsRunning: jobsAdminService.AreThereJobsRunning(),
                anyJobsPaused: jobsAdminService.AreThereJobsPaused(),
                summary: newSummary]
    }

    def updateJobs = {
        def chosenJob = params.chosenJob
        def operation = params.operation

        jobsAdminService.updateJobsScheduler(params.chosenJob, params.operation)

        def jobs = ""
        if (chosenJob) {
            jobs = "($chosenJob)"
        }

        if (operation == "pauseAll") {
            flash.message = "Pause all jobs $jobs"
        } else 
        if (operation == "resumeAll") {
            flash.message = "Resumed all jobs $jobs"
        }
        redirect(action:show)
    }
}
