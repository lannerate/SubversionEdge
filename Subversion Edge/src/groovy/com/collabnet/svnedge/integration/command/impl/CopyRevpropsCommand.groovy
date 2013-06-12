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
package com.collabnet.svnedge.integration.command.impl


import com.collabnet.svnedge.domain.Repository
import com.collabnet.svnedge.domain.Server
import com.collabnet.svnedge.domain.integration.CtfServer
import com.collabnet.svnedge.domain.integration.ReplicatedRepository
import com.collabnet.svnedge.domain.integration.RepoStatus
import com.collabnet.svnedge.integration.command.AbstractRepositoryCommand
import com.collabnet.svnedge.integration.command.ShortRunningCommand
import org.apache.log4j.Logger
import com.collabnet.svnedge.util.ConfigUtil

/**
 * This command uses svnsync to copy the revprops at a given revision to the replica
 */
public class CopyRevpropsCommand extends AbstractRepositoryCommand
        implements ShortRunningCommand {

    private Logger log = Logger.getLogger(getClass())

    def constraints() {
        log.debug("Applying contraints...")

        // todo -- will this need to support props at a given url only?
        // if so, more than repoName param is needed, perhaps "repoPath"
        if (!this.params.repoName) {
            throw new IllegalArgumentException("The repoPath path must be provided")
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
        if (repo.status != RepoStatus.OK) {
            throw new IllegalStateException("The replicated repository " +
                "'${repoName}' is not in the OK state.")
        }
    }

    def execute() {
        def repoName = getRepoName()

        log.debug("Synchronizing revprops: " + repoName + " for revision " +
            this.params.revision)
        def commandLineService = getService("commandLineService")
        def securityService = getService("securityService")

        def syncRepoURI = commandLineService.createSvnFileURI(
                new File(Server.getServer().repoParentDir, repoName))
        def ctfServer = CtfServer.getServer()
        def username = ctfServer.ctfUsername
        def password = securityService.decrypt(ctfServer.ctfPassword)
        execCopyRevprops(repoName, this.params.revision, username, password,
            syncRepoURI)
    }

    def execCopyRevprops(repoPath, revision, username, password, syncRepoURI) {
        log.info("Copying revprops on '${repoPath}' for " +
                "revision '${revision}")
        def command = [ConfigUtil.svnsyncPath(), "copy-revprops", syncRepoURI]
        if (revision) {
            command << revision
        }
        command.addAll([
                "--source-username", username, "--source-password", password,
                "--non-interactive", "--no-auth-cache", "--config-dir",
                ConfigUtil.svnConfigDirPath()])

        executeShellCommand(command)
        log.info("Done syncing repoPath '${repoPath}'.")
    }

    def undo() {
        log.debug("Nothing to undo for an svnsync command")
    }
}
