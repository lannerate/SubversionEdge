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
package com.collabnet.svnedge.integration.command.impl

import org.apache.log4j.Logger
import com.collabnet.svnedge.domain.Repository 
import com.collabnet.svnedge.domain.Server 
import com.collabnet.svnedge.domain.integration.CtfServer 
import com.collabnet.svnedge.domain.integration.ReplicatedRepository 
import com.collabnet.svnedge.domain.integration.RepoStatus
import com.collabnet.svnedge.event.SyncReplicaRepositoryEvent
import com.collabnet.svnedge.integration.command.AbstractRepositoryCommand 
import com.collabnet.svnedge.integration.command.ShortRunningCommand 

/**
 * This command uses svnsync to update the given repository
 */
public class RepoSyncCommand extends AbstractRepositoryCommand 
        implements ShortRunningCommand {

    private Logger log = Logger.getLogger(getClass())

    def constraints() {
        log.debug("Acquiring the replica commands executor service...")
        if (!this.params.repoName) {
            throw new IllegalArgumentException("The repo path must be provided")
        }
        def repoName = getRepoName()
        def repoRecord = Repository.findByName(repoName)
        if (!repoRecord) {
            throw new IllegalArgumentException("There is no replicated " +
                "repository called '${repoName}'.")
        }
        def repo = ReplicatedRepository.findByRepo(repoRecord)
        if (!repo) {
            throw new IllegalStateException("The replication process for " +
                "the repository '${repoName}' hasn't been finished to " +
                "execute svn sync.")
        }
        if (!(repo.status == RepoStatus.OK ||
                repo.status == RepoStatus.OUT_OF_DATE)) {
            throw new IllegalStateException("The replicated repository " +
                "'${repoName}' is not in the OK or OUT_OF_DATE state.")
        }
    }

    def execute() {
        def repoName = getRepoName()

        log.debug("Synchronizing repo: " + repoName + " for revision " +
            this.params.revision)
        def commandLineService = getService("commandLineService")
        def syncRepoURI = commandLineService.createSvnFileURI(
                new File(Server.getServer().repoParentDir, repoName))
        def ctfServer = CtfServer.getServer()
        def username = ctfServer.ctfUsername
        def securityService = getService("securityService")
        def password = securityService.decrypt(ctfServer.ctfPassword)
        def repo = Repository.findByName(repoName)
        def replRepo = ReplicatedRepository.findByRepo(repo)
        execSvnSync(replRepo, System.currentTimeMillis(), username, password, 
            syncRepoURI)
    }

    @Override
    protected void doHandleExecutionException(t) {
        Repository repo = Repository.findByName(this.repoName)
        def svnRepoService = getService("svnRepoService")
        svnRepoService.publishEvent(new SyncReplicaRepositoryEvent(this,
                repo, SyncReplicaRepositoryEvent.FAILED, t))
    }

    def undo() {
        log.debug("Nothing to undo for an svnsync command")
    }
}
