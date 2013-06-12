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

import com.collabnet.svnedge.ConcurrentBackupException
import com.collabnet.svnedge.console.DumpBean
import com.collabnet.svnedge.domain.Repository
import com.collabnet.svnedge.event.DumpRepositoryEvent

/**
 * When triggered, the RepoDumpJob will invoke the {@link com.collabnet.svnedge.console.SvnRepoService#createDump}
 * create dump service, with specifics supplied in the trigger JobDataMap
 */
class RepoDumpJob {

    def svnRepoService
    def cloudServicesRemoteClientService
    def jobsInfoService

    static def jobName = "com.collabnet.svnedge.admin.RepoDumpJob"
    static def group = "Maintenance"
    def volatility = false

    // scheduled dynamically -- no static triggers
    static triggers = { }

    // the Job execute method
    def execute(context) {
        dumpOrBackup(context)
    }
    
    private void dumpOrBackup(context) {
        log.info("Executing scheduled RepoDump ...")
        def dataMap = context.getMergedJobDataMap()
        jobsInfoService
                .queueJob(backupRunnable(dataMap), context.scheduledFireTime)
    }
    
    def backupRunnable = { dataMap ->
        return [
            dataMap: dataMap,
            run: {
                
        Repository repo = Repository.get(dataMap.get("repoId"))
        DumpBean dumpBean = DumpBean.fromMap(dataMap)
        if (repo && dumpBean) {
            try {
                if (dumpBean.cloud) {
                    cloudServicesRemoteClientService
                        .synchronizeRepository(repo, dumpBean.userLocale)
                    log.info("Synchronized cloud backup for " + repo.name)
                }
                else {
                    // if this is a backup job, we should skip when there are no changes since last run
                    String existingFile = (dumpBean.backup) ?
                            svnRepoService.findUpToDateBackup(dumpBean, repo)?.name :
                            null
                    if (existingFile && dumpBean.backup) {
                        log.info("Backup skipped, previous file '${existingFile}' is up to date")
                    } else if (dumpBean.hotcopy) {
                        String file = svnRepoService.createHotcopy(dumpBean, repo)
                        log.info("Creating repo hotcopy file: " + file)
                    } else {
                        String file = svnRepoService.createDump(dumpBean, repo)
                        log.info("Creating repo dump file: " + file)
                    }
                }
                svnRepoService.publishEvent(new DumpRepositoryEvent(this, 
                        dumpBean, repo, DumpRepositoryEvent.SUCCESS, 
                        dataMap['userId'], dataMap['locale'],
                        dataMap['progressLogFile'] ? 
                        new File(dataMap['progressLogFile']) : null))
            } catch (ConcurrentBackupException e) {
                log.warn("Backup skipped: " + e.message)
            } catch (Exception e) {
                log.warn("Repository dump/hotcopy/svnsync failed", e)
                def path = dataMap['progressLogFile']
                File progressFile = path ? new File(path) : null
                if (progressFile.exists()) {
                    // rename the file, so future dumps can proceed
                    int dot = path.lastIndexOf('.')
                    String ts = new Date().format('yyyyMMddHHmmss')
                    String newPath = path.substring(0, dot) + '-' + 
                            ts + path.substring(dot)
                    File tsFile = new File(newPath)
                    if (progressFile.renameTo(tsFile)) {
                        progressFile = tsFile
                    } else {
                        log.warn "Attempt to rename " + 
                                progressFile.canonicalPath + " failed."
                    }
                }
                svnRepoService.publishEvent(new DumpRepositoryEvent(this,
                        dumpBean, repo, DumpRepositoryEvent.FAILED, 
                        dataMap['userId'], dataMap['locale'], progressFile, e))
            }
        }
        else {
            log.warn("Unable to execute the repo backup")
        }
        }]
    }
}

