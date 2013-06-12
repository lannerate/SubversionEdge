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
package com.collabnet.svnedge.integration

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.ApplicationContext;

import com.collabnet.svnedge.console.AbstractSvnEdgeService;
import com.collabnet.svnedge.domain.Repository;
import com.collabnet.svnedge.domain.Server;
import com.collabnet.svnedge.domain.integration.CtfServer;
import com.collabnet.svnedge.domain.integration.ReplicaConfiguration;
import com.collabnet.svnedge.domain.integration.ReplicatedRepository
import com.collabnet.svnedge.integration.command.CommandExecutionException
import com.collabnet.svnedge.integration.command.CommandsExecutionContext;
import com.collabnet.svnedge.util.ConfigUtil;

class ReplicationService extends AbstractSvnEdgeService {
    
    private static final String SYNC_ID_PREFIX = 'mansync'
    private static final String REVPROPS_ID_PREFIX = 'manrevprops'
    public static final def MANUAL_ID_PREFICES = 
            [SYNC_ID_PREFIX, REVPROPS_ID_PREFIX]
    
    def commandLineService
    def securityService
    def replicaCommandSchedulerService
    
    AtomicInteger commandIndex
    
    ReplicationService() {
        initializeCommandIndex()
    }
    
    void synchronizeRepositories(repoIds, locale) {
        queueCommands(repoIds, locale, 'repoSync', SYNC_ID_PREFIX)
    }
        
    private void queueCommands(repoIds, locale, cmdCode, cmdIdPrefix, params = null) {
        def executionContext = new CommandsExecutionContext()
        executionContext.appContext = grailsApplication.mainContext
        CtfServer ctfServer = CtfServer.server
        executionContext.ctfBaseUrl = ctfServer.baseUrl
        executionContext.locale = locale
        executionContext.logsDir = ConfigUtil.logsDirPath()
        def replica = ReplicaConfiguration.getCurrentConfig()
        executionContext.replicaSystemId = replica.systemId
        
        def cmds = []
        repoIds?.each {
            Repository repo = Repository.get(it)
            def cmdParams = [repoName: repo.name]
            if (params) {
                cmdParams.putAll(params)
            }
            cmds << [id: cmdIdPrefix + commandIndex.incrementAndGet(), 
                    params: cmdParams, code: cmdCode, repoName: repo.name]
        }

        if (cmds) {
            executionContext.activeCommands = new AtomicInteger(cmds.size())
            log.debug("There are ${cmds.size()} commands queued.")
            // execute the command using the background service.
            replicaCommandSchedulerService.offer(cmds, executionContext)
        }
    }
    
    private void initializeCommandIndex() {
        int startId = 0
        File logTempFile = new File(ConfigUtil.logsDirPath(), 'temp')
        logTempFile.eachFile { f ->
            def filename = f.name
            MANUAL_ID_PREFICES.each { prefix ->
                if (filename.startsWith(prefix)) {
                    int id = filename
                            .substring(prefix.length(), filename.length() - 4)
                            .toInteger()
                    if (id > startId) {
                        startId = id
                    }
                }
            }
        }
        commandIndex = new AtomicInteger(startId)
    }

    void synchronizeRevprops(Repository repo, revision, locale) {
        def params = revision ? [revision: revision] : null
        queueCommands([repo.id], locale, 'copyRevprops', REVPROPS_ID_PREFIX, params)
    }
    
    static boolean isCtfCommand(String commandId) {
        boolean isCtfCommand = true
        MANUAL_ID_PREFICES.each {
            if (commandId.startsWith(it)) {
                isCtfCommand = false
            }
        }
        return isCtfCommand
    }
}
