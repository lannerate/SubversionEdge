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


package com.collabnet.svnedge.admin

import com.collabnet.svnedge.console.BackgroundJobUtil
import com.collabnet.svnedge.domain.Repository
import com.collabnet.svnedge.event.VerifyRepositoryEvent

/**
 * When triggered, this job will verify the repo indicated in the context.
 */
class RepoVerifyJob {

    public static final String EVENT_SOURCE_SCHEDULED = "RepoVerifyJob-Scheduled"
    public static final String EVENT_SOURCE_ADHOC = "RepoVerifyJob-AdHoc"
    def svnRepoService
    def jobsInfoService

    static def jobName = "com.collabnet.svnedge.admin.RepoVerifyJob"
    static def group = "Maintenance"
    def volatility = false

    // scheduled dynamically -- no static triggers
    static triggers = { }

    // the Job execute method
    def execute(context) {
        def dataMap = context.getMergedJobDataMap()
        jobsInfoService
                .queueJob(verifyRunnable(dataMap), context.scheduledFireTime)
       
    }
    
    def verifyRunnable = {dataMap ->
        return [dataMap: dataMap,
                run: {
                    Repository repo = Repository.get(dataMap.get("repoId"))
                    def progressLog = BackgroundJobUtil
                            .prepareProgressLogFile(repo.name, BackgroundJobUtil.JobType.VERIFY)
                    log.info("Verifying repo '${repo.name}'...")
                                
                    boolean result = false
                    Exception e = null
                    try {
                        result = svnRepoService.verifyRepository(repo,
                                new FileOutputStream(progressLog))
                        // delete progress log on success
                        if (result) {
                            log.info("Successfully verified repo '${repo.name}'")
                            progressLog.delete()
                        }
                        else {
                            log.error("Failed verification for repo '${repo.name}'. " + 
                                    "See ${progressLog.absolutePath} for details.")
                        }
                    }
                    catch (Exception ex) {
                        log.error ("Caught exception verifying repo '${repo.name}': ${ex.message}. " + 
                                "See ${progressLog.absolutePath} for details.")
                        e = ex
                    }
                    // publish event
                    def source = Boolean.valueOf(dataMap["isRecurring"]) ? EVENT_SOURCE_SCHEDULED : EVENT_SOURCE_ADHOC
                    svnRepoService.publishEvent(new VerifyRepositoryEvent(source, repo,
                            (result ? VerifyRepositoryEvent.SUCCESS : VerifyRepositoryEvent.FAILED),
                            dataMap['userId'], dataMap['locale'], progressLog, e))
                }
        ]
    } 

    

}


