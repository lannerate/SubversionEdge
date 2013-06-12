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

import java.net.NoRouteToHostException;
import org.quartz.CronTrigger
import org.quartz.Trigger

/**
 * The Packages Update Job job will run and update the service status regarding
 * the software updates.
 * 
 * @author Marcello de Sales (mdesales@collab.net)
 */
class PackagesUpdateJob {

    /**
     * The packages update service
     */
    def packagesUpdateService
    def jobsAdminService

    static def jobName = "com.collabnet.svnedge.admin.PackagesUpdateJob"
    static def group = "Maintenance"
    def volatility = false

    static triggers = { 
    // artf4934, some OS's don't compile the quartz plugin correctly, so
    // this method of scheduling doesn't work.  Using a service bootstrap
    // method as workaround
    //  
    //    cron name: jobName + "Trigger", group: group + "_Triggers", \
    //    startDelay: 0, \
    //    cronExpression: "0 15 12 ? * *"
    }

    /** 
     * Schedule a daily 12:15 pm repeating trigger
     */
    static Trigger createTrigger() {
        return new CronTrigger(jobName + "Trigger",
            group + "_Triggers", jobName, group, "0 15 12 ? * *")

    }

    def execute() {
        if (this.packagesUpdateService.hasBeenBootstraped()) {
            log.info("Checking for Software Updates...")
            try {
                this.packagesUpdateService.reloadPackagesAndUpdates()
                if (this.packagesUpdateService.areThereUpdatesAvailable()) {
                    log.info("There are updates available...")
                } else {
                    log.info("There are NO updates available")
                }
            } catch (NoRouteToHostException nrthe) {
                log.error("Can't access the software updates server: " + 
                        nrthe.getMessage())
            } catch (Exception e) {
                log.error("Error while verifying for software updates: " + 
                        e.getMessage())
            }
        } else {
            log.error("The Packages Update Service has not been bootstraped.")
        }
    }
}
