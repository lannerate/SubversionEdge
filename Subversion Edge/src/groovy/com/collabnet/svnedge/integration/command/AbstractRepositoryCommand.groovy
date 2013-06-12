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
package com.collabnet.svnedge.integration.command

import com.collabnet.svnedge.domain.Repository 
import com.collabnet.svnedge.domain.Server 
import com.collabnet.svnedge.domain.integration.CtfServer 
import com.collabnet.svnedge.domain.integration.ReplicaConfiguration 
import com.collabnet.svnedge.domain.integration.ReplicatedRepository 
import com.collabnet.svnedge.domain.integration.RepoStatus 
import com.collabnet.svnedge.util.ConfigUtil;

/**
 * Defines the Abstract Repository Command used to manage the replicated
 * repositories.
 * 
 * @author John Mcnally (jmcnally@collab.net)
 * @author Marcello de Sales (mdesales@collab.net)
 *
 */
abstract class AbstractRepositoryCommand extends AbstractCommand {

    /**
     * Takes the "repoName" parameter and strips any parent paths
     * @return just the path name after the final /
     */
    protected String getRepoName() {
        String repoName = this.params["repoName"]
        if (!repoName) {
            return null
        }
        int pos = repoName.lastIndexOf('/');
        if (pos >= 0 && repoName.length() > pos + 1) {
            repoName = repoName.substring(pos + 1)
        }
        return repoName
    }

    /**
     * Adds the repository on the database.  If the repository has no db
     * record, it will be added.  If it's been previously removed, it's
     * status will be changed back to NOT_READY_YET and enabled.
     */
    def addRepositoryOnDatabase(repoName) {
        def repoRecord = Repository.findByName(repoName)
        if (repoRecord) {
            def repoReplica = ReplicatedRepository.findByRepo(repoRecord)
            if (repoReplica) {
                repoReplica.enabled = true;
                repoReplica.status = RepoStatus.NOT_READY_YET
                repoReplica.statusMsg = null
                repoReplica.save()

            } else {
                repoReplica = new ReplicatedRepository(repo: repoRecord,
                    lastSyncTime: -1, lastSyncRev:-1, enabled: true, 
                    status: RepoStatus.NOT_READY_YET)
                repoReplica.save(flush:true)
            }

        } else {
            Repository repository = new Repository(name:repoName)
            repository.save()
            ReplicatedRepository repoReplica = new ReplicatedRepository(
                repo: repository, lastSyncTime: -1, lastSyncRev:-1,
                enabled: true, status: RepoStatus.NOT_READY_YET)
            repoReplica.save(flush:true)
        }
    }

    /*
     * Creates local replica repositories
     */
    def createRepositoryOnFileSystem(repoName) {
        def repo = Repository.findByName(repoName)
        def replRepo = ReplicatedRepository.findByRepo(repo)
        def svnRepoService = getService("svnRepoService")
        def repoPath = svnRepoService.getRepositoryHomePath(repo)
        if (new File(repoPath).exists()) {
            if (svnRepoService.verifyRepository(repo)) {
                log.info("createRepositoryOnFileSystem found an existing repo: "
                         + repoName)
                replRepo.status = RepoStatus.IN_PROGRESS
                replRepo.statusMsg = null
                replRepo.save()
                
                syncRepo(repoPath, replRepo, repoName)
    
            }  else {
                def msg = "createRepositoryOnFileSystem found existing directory " +
                    repoPath + ", but it would not verify as a valid repository."
                log.error(msg)
                replRepo.status = RepoStatus.ERROR
                replRepo.statusMsg = msg
                replRepo.save()
                throw new IllegalStateException(msg)
            }
        } else {
            if (svnRepoService.createRepository(repo, false) == 0) {
                log.info("Created the repo with svnadmin.")
                replRepo.status = RepoStatus.IN_PROGRESS
                replRepo.statusMsg = null
                replRepo.save()
                
                syncRepo(repoPath, replRepo, repoName)
            } else {
                def msg = "Svnadmin failed to create repository: " + repoName
                log.error(msg)
                replRepo.status = RepoStatus.ERROR
                replRepo.statusMsg = msg
                replRepo.save()
                throw new IllegalStateException(msg)
            }
        }
        replRepo.status = RepoStatus.OK
        replRepo.statusMsg = null
        replRepo.save()
    }

    private def getSyncRepoURI(repoName) {
        def commandLineService = getService("commandLineService")
        return commandLineService.createSvnFileURI(
                new File(Server.getServer().repoParentDir, repoName))
    }
    
    /**
     * Restarts the server to sync the master svn version in the httpd config
     */
    def syncConfigurationWithMasterIfFirstRepo() {

        if (ReplicatedRepository.count() == 1) {
            log.debug("Restarting server to set SVNMasterVersion directive.")
            def lifecycleService = getService("lifecycleService")
            lifecycleService.gracefulRestartServer()
        }
    }

    /**
     * @return if the current server running is a windows box.
     */
    private boolean isWindows() {
        def operatingSystemService = getService("operatingSystemService")
        return operatingSystemService.isWindows()
    }

    private static final String PRE_REV_PROP_SCRIPT = "#!/bin/bash\nexit 0;\n"
    /**
     * Prepares the hook scripts for the given repository path.
     * @param repoPath
     * @param repo
     */
    private def prepareHookScripts(repoPath) {
        def preRevPropChangeScript = repoPath + '/hooks/pre-revprop-change'
        if (isWindows()) {
            preRevPropChangeScript += ".bat"
        }
        File f = new File(preRevPropChangeScript)
        if (f.exists()) {
            String currentScript = f.text
            if (currentScript != PRE_REV_PROP_SCRIPT) {
                File bkup = new File(preRevPropChangeScript + '.bkup')
                bkup.text = currentScript
                writePreRevPropScript(f, preRevPropChangeScript)
            }
        }
        else {
            writePreRevPropScript(f, preRevPropChangeScript)
        }
    }
        
    private writePreRevPropScript(File f, String preRevPropChangeScript) {
        log.info("Changing the preRevPropChange hook.")
        f.text = PRE_REV_PROP_SCRIPT
        if (!isWindows()) {
            executeShellCommand(["chmod", "755",
                preRevPropChangeScript])
        }
    }
    

    private def syncRepo(repoPath, repo, repoName) {
        prepareHookScripts(repoPath)
        log.info("Initing the repo...: " + repoName)
        def replicaConfig = ReplicaConfiguration.getCurrentConfig()
        def masterRepoUrl = replicaConfig.getSvnMasterUrl() + "/" + repoName
        def commandLineService = getService("commandLineService")
        def syncRepoURI = getSyncRepoURI(repoName)
        def ctfServer = CtfServer.getServer()
        def username = ctfServer.ctfUsername
        def securityService = getService("securityService")
        def password = securityService.decrypt(ctfServer.ctfPassword)
        def command = [ConfigUtil.svnsyncPath(), "init", syncRepoURI, 
            masterRepoUrl, "--allow-non-empty",
            "--source-username", username, "--source-password", password,
            "--non-interactive", "--no-auth-cache", "--config-dir",
            ConfigUtil.svnConfigDirPath()]

        executeShellCommand(command, repo)
        log.info("Done initing the repo.")
        def svnRepoService = getService("svnRepoService")
        repo.lastSyncRev = svnRepoService.findHeadRev(repo.repo)

        def masterUUID = getMasterUUID(masterRepoUrl, username, password, 
            repoName)
        if (masterUUID) {
            command = [ConfigUtil.svnadminPath(), "setuuid", repoPath, 
                masterUUID]
            executeShellCommand(command, repo)
            log.info("Done setting uuid ${masterUUID} of the repo as that " +
                "of master.")
            execSvnSync(repo, System.currentTimeMillis(), username, password, 
                syncRepoURI)
        }
    }

    /**
     * Returns Master Repository's UUID.
     */
    private def getMasterUUID(masterRepoUrl, username, password, repoName) {
        def uuid = null
        def command = [ConfigUtil.svnPath(), "info", masterRepoUrl,
            "--username", username,"--password", password,
            "--non-interactive", "--no-auth-cache", "--config-dir",
            ConfigUtil.svnConfigDirPath()]
        def output = executeShellCommand(command, null)
        def matcher = output =~ /Repository UUID: ([^\s]+)/
        if (matcher && matcher[0][1]) {
            uuid = matcher[0][1]
        } else {
            String msg = "Unable to get master UUID for repo: " + repoName
            log.warn(msg)
            throw new IllegalStateException(msg)
        }
        return uuid
    }

    /**
     * Returns revision number of last successful sync.
     * If there is *no* commit since the last sync this function itself
     * should not be called. But it would be called in situations when the
     * initial setup of '0' revision repositories(Not possible in CEE but
     * possible in CTF.).
     * In such situations it would return 0 indicating
     * do *not* update the lastSyncRev in DB.
     * If sync fails return -1 and updates the Repo record
     * in the db indicating failure.
     */
    def execSvnSync(repo, masterTimestamp, username, password, syncRepoURI) {
        log.info("Syncing repo '${repo.repo.name}' at " +
                " master timestamp: ${masterTimestamp}...")
        def command = [ConfigUtil.svnsyncPath(), "sync", syncRepoURI,
            "--source-username", username, "--source-password", password,
            "--non-interactive", "--no-auth-cache", "--disable-locking",
            "--config-dir", ConfigUtil.svnConfigDirPath()]
        
        def msg = "${command} failed. "
        try {
            def output = executeShellCommand(command, repo)
            // if there is output, scan for last "revision N" pattern
            // if not, there are no new revisions to sync
            if (output) {
                def matcher = output =~ /revision (\d+)/
                repo.lastSyncRev = Long.parseLong(matcher[matcher.count - 1 ][1])
            }
            repo.status = RepoStatus.OK
            repo.statusMsg = null
            repo.lastSyncTime = masterTimestamp
            repo.save()
            log.info("Done syncing repo '${repo.repo.name}'.")
        }
        catch (Exception e) {
            log.warn(msg, e)
            msg += e.getMessage()
            repo.status = RepoStatus.OUT_OF_DATE
            repo.statusMsg = msg
            repo.save()
            throw new CommandExecutionException(this, e, null)
        }
    }

    def removeReplicatedRepository(repoName) {
        def repoDir = new File(Server.getServer().repoParentDir, repoName)
        if (repoDir && repoDir.exists()) {
            repoDir.deleteDir()
        }

        removeRepositoryOnDatabase(repoName)
    }

    /**
     * Removes the repository on the database.  This involves changing the
     * status, sync time/revs and disabling the repo.
     */
    private def removeRepositoryOnDatabase(repoName) {
        def repoRecord = Repository.findByName(repoName)
        if (!repoRecord) {
            log.error("removeRepositoryOnDatabase: No repo found for name " 
                            + "${repoName}")
        } else {
            def repo = ReplicatedRepository.findByRepo(repoRecord)
            if (repo) {
                repo.enabled = false;
                repo.status = RepoStatus.REMOVED
                repo.lastSyncTime = -1
                repo.lastSyncRev = -1
                repo.statusMsg = "Repository removed at " + new Date()
                repo.save()
            } else {
                log.error("removeRepositoryOnDatabase: No repo found for name " 
                          + "${repoName}")
            }
        }
    }
}
