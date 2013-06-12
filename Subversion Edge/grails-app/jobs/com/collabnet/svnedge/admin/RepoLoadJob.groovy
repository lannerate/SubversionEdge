/*
 * CollabNet Subversion Edge
 * Copyright (C) 2011, CollabNet Inc. All rights reserved.
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

import com.collabnet.svnedge.domain.Repository
import com.collabnet.svnedge.event.LoadRepositoryEvent

/**
 * When triggered, the RepoLoadJob will invoke the {@link com.collabnet.svnedge.console.SvnRepoService#loadDumpFile}
 * load dump service, with specifics supplied in the trigger JobDataMap
 */
class RepoLoadJob {

    def jobsInfoService
    def svnRepoService

    static def group = "Maintenance"
    def volatility = false

    // scheduled dynamically -- no static triggers
    static triggers = { }

    // the Job execute method
    def execute(context) {
        log.info("Executing scheduled RepoLoad ...")
        def dataMap = context.getMergedJobDataMap()
        jobsInfoService
                .queueJob(backupRunnable(dataMap), context.scheduledFireTime)
    }
    
    def backupRunnable = { dataMap ->
        return [
            dataMap: dataMap,
            run: {

        Repository repo = Repository.get(dataMap.get("repoId"))
        if (!repo) {
            log.error("Unable to execute the repo load: repoId not found")
        }
        try {
            log.info("Loading dump file for repo: ${repo.name}")
            svnRepoService.loadDumpFile(repo, dataMap)
            svnRepoService.publishEvent(new LoadRepositoryEvent(this,
                    repo, LoadRepositoryEvent.SUCCESS, 
                    dataMap['userId'], dataMap['locale'], 
                    dataMap['progressLogFile'] ? 
                    new File(dataMap['progressLogFile']) : null))
        }
        catch (Exception e) {
            log.error("Unable to load the dump file", e)
            svnRepoService.publishEvent(new LoadRepositoryEvent(this,
                    repo, LoadRepositoryEvent.FAILED, 
                    dataMap['userId'], dataMap['locale'], 
                    dataMap['progressLogFile'] ? 
                    new File(dataMap['progressLogFile']) : null, e))
        }
        }]
    }
}

