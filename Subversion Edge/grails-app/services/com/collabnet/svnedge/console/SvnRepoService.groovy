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
package com.collabnet.svnedge.console

import com.collabnet.svnedge.ConcurrentBackupException
import com.collabnet.svnedge.RepoLoadException
import com.collabnet.svnedge.ValidationException
import com.collabnet.svnedge.admin.RepoDumpJob
import com.collabnet.svnedge.admin.RepoLoadJob
import com.collabnet.svnedge.admin.RepoVerifyJob
import com.collabnet.svnedge.console.SchedulerBean.Frequency
import com.collabnet.svnedge.domain.Repository
import com.collabnet.svnedge.domain.Server
import com.collabnet.svnedge.domain.ServerMode
import com.collabnet.svnedge.domain.integration.ReplicatedRepository
import com.collabnet.svnedge.domain.statistics.StatValue
import com.collabnet.svnedge.domain.statistics.Statistic
import com.collabnet.svnedge.util.ConfigUtil
import groovy.io.FileType
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile
import org.quartz.CronTrigger
import org.quartz.JobDataMap
import org.quartz.SimpleTrigger

class SvnRepoService extends AbstractSvnEdgeService {

    private static final String REPO_JOB_TRIGGER_GROUP = "RepoJob"
    private static final boolean ASCENDING = true
    private static final boolean DESCENDING = false

    // dependencies
    def operatingSystemService
    def lifecycleService
    def commandLineService
    def serverConfService
    def statisticsService
    def jobsAdminService
    def fileUtil

    boolean transactional = false

    /**
     * Returns repository feature for give FS format.
     *
     * @param repo is the instance of a repository.
     * @param fsFormat is fsformat number of given repository. 
     * @return String
     *
     */
    def getRepoFeatures(Repository repo, int fsFormat) {
        def list = [
                "",
                "svndiff0",
                "svndiff1",
                "svndiff1, sharding, mergeinfo",
                "svndiff1, sharding, mergeinfo, rep-sharing, packed revs",
                "svndiff1, sharding, mergeinfo, rep-sharing, packed revs, packed revprops (unsupported), not released",
                "svndiff1, sharding, mergeinfo, rep-sharing, packed revs, packed revprops",
        ]

        def feature = ""

        if (fsFormat <= 1) {
            feature = list.get(1)
        } else if (fsFormat >= list.size()) {
            feature = list.get(list.size() - 1)
        } else {
            feature = list.get(fsFormat)
        }

        return feature
    }

    /**
     * Returns repository UUID.
     *
     * @param repo is the instance of a repository.
     * @return uuid string
     *
     */
    def getReposUUID(Repository repo) {
        def repoPath = this.getRepositoryHomePath(repo)

        def uuid = ""

        def f = new File(new File(repoPath, "db/uuid").canonicalPath)
        if (f.exists()) {
            uuid = f.readLines()[0]
        } else {
            log.warn("Missing $repoPath/db/uuid file...")
        }
        return uuid
    }

    /**
     * replaces the UUID of a repo, generating anew or applying the param value
     * @param repo to update
     * @param uuid optional string uuid to set the repo to
     * @return void
     */
    def setReposUUID(Repository repo, String uuid = null) {
        def cmd = [ConfigUtil.svnadminPath(), "setuuid", getRepositoryHomePath(repo)]
        if (uuid) {
            cmd << uuid
        }
        String[] rslt = commandLineService.execute(cmd, null, null)
        log.debug("Result of setting UUID: ${rslt}" )
    }

    /**
     * Returns repository fstype.
     *
     * @param repo is the instance of a repository.
     * @return fstype string
     *
     */
    def getReposFsType(Repository repo) {
        def repoPath = this.getRepositoryHomePath(repo)

        def fsType = "FSFS"

        def f = new File(new File(repoPath, "db/fs-type").canonicalPath)
        if (f.exists()) {
            try {
                fsType = f.readLines()[0].toUpperCase()
            } catch (e) {
                log.debug("Reading from $repoPath/db/fs-type (" +
                        e.getMessage() + "), Assuming FSFS as FS-Type.")
            }
        } else {
            log.warn("Missing $repoPath/db/fs-type file..." +
                    ", Assuming FSFS as FS-Type.")
        }

        return fsType
    }

    /**
     * Returns repository fs format
     *
     * @param repo is the instance of a repository.
     * @return repository fs format integer.
     */
    def getReposFsFormat(Repository repo) {
        def repoPath = this.getRepositoryHomePath(repo)

        // As per the Subversion FS design docs, Svn asssumes fsformat as 1
        // in case db/format file is missing from repository. We follow the
        // same.
        def fsFormat = 1
        def f = new File(new File(repoPath, "db/format").canonicalPath)
        if (f.exists()) {
            try {
                fsFormat = f.readLines()[0].toInteger()
            } catch (e) {
                log.debug("Reading from $repoPath/db/format (" + e.getMessage() +
                        "), Assuming repository fs format to be '1'.")
                fsFormat = 1
            }
        } else {
            log.warn("Missing $repoPath/db/format file..." +
                    ", Assuming repository fs format to be '1'.")
        }
        return fsFormat
    }

    /**
     * Checks rep-sharing is enabled or disabled.
     *
     * @param repo is the instance of a repository.
     * @return Boolean
     */
    def getReposRepSharing(Repository repo) {
        def repoPath = this.getRepositoryHomePath(repo)

        def repSharing = true
        def f = new File(new File(repoPath, "db/fsfs.conf").canonicalPath)
        if (!f.exists()) {
            log.warn("Missing $repoPath/db/fsfs.conf file...")
            return false
        }
        f.withReader {reader ->
            String line
            while ((line = reader.readLine()) != null) {
                line = line.trim()
                if (line.matches("[# ]*enable-rep-sharing[ ]*=.*")) {
                    if (line.startsWith("#")) {
                        repSharing = true
                    } else {
                        String[] strsplit = line.split("=")
                        if (strsplit.length <= 2) {
                            repSharing = Boolean.parseBoolean(strsplit[1].trim())
                        } else {
                            repSharing = true
                        }
                    }
                    break
                }
            }
        }

        return repSharing
    }

    /**
     * Returns repository format
     *
     * @param repo is the instance of a repository.
     * @return repository format integer.
     */
    def getReposFormat(Repository repo) {
        def repoPath = this.getRepositoryHomePath(repo)

        def repoFormat = 0
        def f = new File(new File(repoPath, "format").canonicalPath)
        if (f.exists()) {
            try {
                repoFormat = f.readLines()[0].toInteger()
            } catch (e) {
                repoFormat = 0
                log.debug("Reading from $repoPath/format (" + e.getMessage() +
                        "), Assuming repository format to be '0'.")
            }
        } else {
            log.warn("Missing $repoPath/format file..." +
                    ", Assuming repository format to be '0'.")
        }
        return repoFormat
    }

    /**
     * Returns Sharding information.
     *
     * @param repo is the instance of a repository.
     * @return Sharding revision number in case its enabled else
     *         return -1.
     */
    def getReposSharding(Repository repo) {
        def repoPath = this.getRepositoryHomePath(repo)
        def sharded = -1

        def f = new File(new File(repoPath, "db/format").canonicalPath)
        if (!f.exists()) {
            log.warn("Missing $repoPath/db/format file...")
            return -1
        }

        try {
            f.withReader { reader ->
                String line
                while ((line = reader.readLine()) != null) {
                    line = line.trim()
                    if (line.matches("^layout\\ sharded.*")) {
                        String[] strsplit = line.split("\\ ")
                        if (strsplit.length == 3) {
                            sharded = strsplit[2].toLong()
                        }
                        break
                    }
                }
            }
        } catch (e) {
            sharded = -1
            log.debug("Reading from $repoPath/db/format (" + e.getMessage() +
                    "), Assuming repository sharding disabled.")
        }

        return sharded
    }

    /**
     * Returns the current head revision.
     *
     * @param repo is the instance of a repository.
     * @return revision number.
     */
    def findHeadRev(Repository repo) {
        return findHeadRev(this.getRepositoryHomePath(repo))
    }

    private findHeadRev(String repoPath) {
        def f = new File(repoPath, "db/current")
        if (!f.exists()) {
            log.warn("Missing $repoPath/db/current file...")
            return 0
        }

        try {
            String[] strsplit = f.readLines()[0].split("\\ ")
            return strsplit[0].toInteger()
        } catch (Exception e) {
            log.error("Can't find head revision for repository " + repoPath)
            return 0
        }
    }

    /**
     * Finds least packed revision
     *
     * @param repo is the instance of a repository.
     * @return revision number.
     */
    def findMinPackedRev(Repository repo) {
        def repoPath = this.getRepositoryHomePath(repo)

        def f = new File(new File(repoPath, "db/min-unpacked-rev").canonicalPath)
        if (!f.exists()) {
            log.warn("Missing $repoPath/db/min-unpacked-rev file...")
            return 0
        }

        try {
            return (f.readLines()[0]).toInteger()
        } catch (e) {
            log.debug("Reading from $repoPath/db/min-unpacked-rev (" +
                    e.getMessage() + "), Assuming min-unpacked-rev to be '0'.")
            return 0
        }
    }

    /**
     * Creates a new repository.
     *
     * @param useTemplate If true a basic trunk/branches/tags structure
     * will be generated.
     */
    def createRepository(Repository repo, boolean useTemplate) {
        def repoPath = this.getRepositoryHomePath(repo)
        def exitStatus = commandLineService.executeWithStatus(
                ConfigUtil.svnadminPath(), "create", repoPath)
        if (exitStatus == 0 && useTemplate) {
            log.debug("Created repository " + repoPath +
                    ". Adding default paths...")
            def fileURL = {
                return commandLineService.createSvnFileURI(
                        new File(repoPath, it))
            }
            exitStatus = commandLineService.executeWithStatus(
                    ConfigUtil.svnPath(), "mkdir",
                    fileURL("trunk"), fileURL("branches"), fileURL("tags"),
                    "-m", "Creating_initial_branch_structure",
                    "--no-auth-cache", "--non-interactive") // --quiet"
        }
        return exitStatus
    }

    /**
     * Runs svnadmin verify on a repository.
     *
     * @return true , if no errors
     */
    def verifyRepository(Repository repo, OutputStream progress = null) {
        def repoPath = this.getRepositoryHomePath(repo)
        repo.verifyOk = verifyRepositoryPath(repoPath, progress)
        repo.save()
        return repo.verifyOk
    }

    private def verifyRepositoryPath(String repoPath, OutputStream progress = null) {
        def exitStatus = (progress == null) ?
            commandLineService.executeWithStatus(
                    ConfigUtil.svnadminPath(), "verify", repoPath) :
            commandLineService.execute([ConfigUtil.svnadminPath(),
                    "verify", repoPath], progress, progress)[0] as Integer
        return (exitStatus == 0)
    }

    /**
     * @param repo is the instance of a repository.
     * @return the canonical path to the repository in the file system.
     */
    def getRepositoryHomePath(Repository repo) {
        Server server = lifecycleService.getServer()
        return new File(server.repoParentDir, repo.name).canonicalPath
    }

    /**
     * Moves the repository contents to an inaccessible location to be
     * archived or otherwise further processing
     * @param repo whose folder to move
     * @return String message about the new location
     */
    def archivePhysicalRepository(Repository repo) {
        def server = lifecycleService.getServer()
        File repoToDelete = new File(this.getRepositoryHomePath(repo))
        File f = new File(new File(server.repoParentDir).getParentFile(),
                "deleted-repos")
        if (!f.exists()) {
            f.mkdir()
        }
        def count = 0
        File repoArchiveLocation = new File(f, repo.name)
        while (repoArchiveLocation.exists()) {
            repoArchiveLocation = new File(f, repo.name + "." + (++count))
        }
        repoToDelete.renameTo(repoArchiveLocation)
        return "Moved repository " + repo.name + " new location is " +
                repoArchiveLocation.getAbsolutePath()
    }

    /**
     * Deletes the repository contents from the file system
     * @param repo
     * @return boolean indicating success or failure
     */
    def deletePhysicalRepository(Repository repo) {
        File repoToDelete = new File(this.getRepositoryHomePath(repo))
        return repoToDelete.deleteDir();
    }

    /**
     * Removes a Repository from the DB (no SVN or filesystem action is taken) by
     * properly handling constraints that prevent direct delete
     * @param r
     */
    def removeRepository(Repository repo) {

        // delete FK'd stats first
        StatValue.executeUpdate("delete from StatValue s where s.repo.id = :repoId", [repoId: repo.id])

        // delete FK'd ReplicatedRepository
        def replicatedRepos = ReplicatedRepository.findAllByRepo(repo)
        replicatedRepos.each() {
            it.delete()
        }

        // remove scheduled jobs
        clearScheduledJobs(repo)

        // delete the repo entity
        repo.delete()
    }

    /**
     * Syncs the Repository table with the contents of the svn server 
     * repositories directory
     */
    def syncRepositories() {

        log.info("Syncing SVN repo folders to database")
        Server server = lifecycleService.getServer()

        // fetch the repo directories (skipping hidden)
        def files = Arrays.asList(new File(server.repoParentDir).listFiles(
                {file -> isRepository(file) } as FileFilter))

        def repoFolderNames = files.collect { it.name }
        def repoDbNames = Repository.list().collect() { it.name }
        def isWindows = operatingSystemService.isWindows()

        // create DB rows for repositories IN SVN but not in DB
        repoFolderNames.each { folder ->
            if (!repoDbNames.contains(folder)) {
                log.debug("Adding Respository row to represent folder: " +
                        "${folder}")
                def r = new Repository(name: folder, permissionsOk: true)

                // if Windows, assume permissions are OK; otherwise, validate
                if (!isWindows) {
                    r.permissionsOk = validateRepositoryPermissions(r)
                }
                r.save()
            }
        }

        // remove DB rows for repositories NOT IN SVN
        def reposToDelete = []
        repoDbNames.each { repo ->
            if (!repoFolderNames.contains(repo)) {
                log.debug("Deleting Repository row with no matching folder: " +
                        "${repo}")
                reposToDelete << repo
            }
        }

        reposToDelete.each { it ->
            removeRepository(Repository.findByName(it))
        }

    }

    /**
     * determine if a File resembles an SVN repo. Can be used with a zip file (hotcopy backup) or directory
     * @return boolean assessment
     */
    boolean isRepository(File f) {

        boolean isDirectory = f.exists() && f.isDirectory()
        boolean isZip = f.exists() && f.isFile() && f.name.toLowerCase().endsWith(".zip")
        boolean hasFormat = false
        boolean hasDb = false
        if (isDirectory) {
            f.eachFile {file ->
                hasFormat |= (file.isFile() && file.name == "format")
                hasDb |= (file.isDirectory() && file.name == "db")
            }
        }
        else if (isZip) {
            ZipFile zipFile = new ZipFile(f)
            def firstEntry = null
            zipFile.entries.each {
                if (!firstEntry) {
                    firstEntry = it.name
                }
                hasFormat |= (!it.isDirectory() && 
                        (it.name == "format" || 
                         it.name == firstEntry + "format"))
                hasDb |= (it.isDirectory() && 
                        (it.name == "db/" || it.name == firstEntry + "db/"))
            }
            zipFile.close()
        }
        return hasFormat && hasDb
    }

    /**
     * This action will merely set Repo.permissionsOk = true
     * TODO in a future story it should sudo chown the repo directory
     */
    def updateRepositoryPermissions(Repository repo) {
        repo.permissionsOk = true
        repo.save(flush: true)
    }

    /**
     * @param repoStatus is the current status.
     * @param server is the current server instance.
     * @return the formatted string for the repository.
     */
    def formatRepoStatus(repoStatus, locale) {
        def server = lifecycleService.getServer()
        def buffer = new StringBuilder()
        if (repoStatus.size() == 0) {
            if (server.mode == ServerMode.REPLICA) {
                return getMessage("repository.status.notAdded", locale)
            } else {
                def num = Repository.count()
                if (num == 0) {
                    buffer.append getMessage("repository.status.noRepos", locale)
                } else {
                    buffer.append getMessage("repository.status.totalNumber",
                            [num], locale)
                }
                if (server.mode == ServerMode.STANDALONE) {
                    buffer.append " " +
                            getMessage("repository.status.createNew", locale)
                }
                return buffer.toString()
            }
        }
        repoStatus.eachWithIndex { it, index ->
            buffer.append(it.count + " " + it.status)
            if (index < repoStatus.size() - 1) {
                buffer.append(", ")
            }
        }
        return buffer.toString()
    }

    /**
     * Validates whether the httpd user and group match the ownership of the
     * input Repo dir
     * @param repo
     * @return boolean indicator
     */
    boolean validateRepositoryPermissions(Repository repo) {
        def repoPath = this.getRepositoryHomePath(repo)
        return (commandLineService.getPathOwner(repoPath) == 
                serverConfService.getHttpdUser() &&
                commandLineService.getPathGroup(repoPath) ==
                serverConfService.getHttpdGroup())
    }

    /**
     * Lists all the dump files generated for the given repository
     * @param repo A Repository object
     * @return List of File objects
     */
    List<File> listDumpFiles(repo, sortBy = "date", isAscending = false) {
        def files = []

        Server server = Server.getServer()
        File dumpDir = new File(server.dumpDir, repo.name)
        if (dumpDir.exists()) {
            dumpDir.eachFile(FileType.FILES) { f ->
                def name = f.name
                if (!name.endsWith("-processing") && !name.endsWith("-processing.zip")) {
                    files << f
                }
            }
            files = sortFiles(files, sortBy, isAscending)
        }
        return files
    }

    /**
     * Lists all the dumps on the file system, which can include those of deleted repos
     * @param sortBy sort column (name is default)
     * @param isAscending sort direction (true)
     * @return Map of String (repo name) keying a List of File objects
     */
    Map<String, List<File>> listBackupsOnFilesystem(sortBy = "name", isAscending = true) {
        def fileMap = [:]

        Server server = Server.getServer()
        File dumpDir = new File(server.dumpDir)
        if (dumpDir.exists()) {
            dumpDir.eachFile(FileType.DIRECTORIES) { d ->
                def files = []
                d.eachFile(FileType.FILES) { f ->
                    def name = f.name
                    if (!name.endsWith("-processing") && !name.endsWith("-processing.zip")) {
                        files << f
                    }
                }
                files = sortFiles(files, sortBy, isAscending)
                fileMap.put(d.name, files)
            }
        }
        return fileMap
    }

    /**
     * Hard delete of the specified repository dump file
     *
     * @param filename
     * @param repo Repository object
     * @return true , if the delete was successful; false otherwise
     */
    boolean deleteDumpFile(filename, repo) throws FileNotFoundException {
        return getDumpFile(filename, repo).delete()
    }

    /**
     * Copies the contents of the specified repository dump file to the 
     * given stream
     *
     * @param filename
     * @param repo Repository object
     * @param outputStream
     * @return true , if the file could be read
     */
    boolean copyDumpFile(filename, repo, outputStream)
    throws FileNotFoundException {
        File dumpFile = getDumpFile(filename, repo)
        return copyFile(dumpFile, outputStream)
    }
    
    private boolean copyFile(File f, outputStream) {
        if (f.canRead()) {
            f.withInputStream {
                outputStream << it
            }
            return true
        }
        return false
    }

    private File getDumpFile(filename, repo) throws FileNotFoundException {
        Server server = Server.getServer()
        File dumpDir = new File(server.dumpDir, repo.name)
        File dumpFile = new File(dumpDir, filename)
        if (dumpFile.exists()) {
            return dumpFile
        }
        throw new FileNotFoundException(filename)
    }

    List retrieveScheduledBackups(repo = null) {
        List backups = []
        def triggers = jobsAdminService
                .getTriggers(RepoDumpJob.jobName, RepoDumpJob.group)
        for (trigger in triggers) {
            if ((!repo && trigger.group == REPO_JOB_TRIGGER_GROUP) ||
                    trigger.name ==~ /${repo.name}|${repo.name}-(hotcopy|dump|cloud).*/) {
                backups << trigger.jobDataMap
            }
        }
        return backups
    }

    List retrieveScheduledJobs(repo = null) {
        List jobs = []
        List triggers = jobsAdminService
                .getTriggers(RepoDumpJob.jobName, RepoDumpJob.group)
        triggers.addAll(jobsAdminService
                .getTriggers(RepoVerifyJob.jobName, RepoVerifyJob.group))
        for (trigger in triggers) {
            boolean includeJob = false
            includeJob = (!repo && trigger.group == REPO_JOB_TRIGGER_GROUP)
            includeJob |= (repo && trigger.name ==~ /${repo.name}|${repo.name}-.*/)
            if (includeJob) {
                def jobMap = [:]
                jobMap << trigger.jobDataMap
                jobMap.put("jobName", trigger.jobName)
                jobs << jobMap
            }
        }
        return jobs
    }

    /**
     * method to schedule a RepoDump quartz job
     * @param bean the parameters for the dump job
     * @param repo the repo ni question
     * @return the filename the dumpfile is expected to have
     */
    String scheduleDump(DumpBean bean, repo, Integer userId = null, 
            String tName = null) {

        String cron = BackgroundJobUtil.getCronExpression(bean.schedule)
        log.debug("Scheduling backup dump using cron expression: " + cron)
        if (tName) {
            deleteScheduledJob(tName)
        }
        tName = BackgroundJobUtil.generateTriggerName(repo, bean)
        
        def descKey = "repository.action.createAdhocDumpfile.job.description"
        if (bean.backup) {
            descKey = bean.cloud ? "repository.action.cloudSync.job.description" :
                    bean.hotcopy ? "repository.action.createHotcopy.job.description" :
                    "repository.action.createDumpfile.job.description"
        }
        def tGroup = bean.backup ? REPO_JOB_TRIGGER_GROUP : "AdhocDump"
        def trigger = new CronTrigger(tName, tGroup, cron)
        log.debug("cron expression summary:\n" + trigger.expressionSummary)
        trigger.setJobName(RepoDumpJob.name)
        trigger.setJobGroup(RepoDumpJob.group)

        // data for reporting status to quartz job listeners
        def jobType = bean.cloud ? BackgroundJobUtil.JobType.CLOUD : 
                (bean.hotcopy ? BackgroundJobUtil.JobType.HOTCOPY : 
                BackgroundJobUtil.JobType.DUMP)
        File progressFile = 
                BackgroundJobUtil.prepareProgressLogFile(repo.name, jobType)
        def jobDataMap =
        [id: tName, repoId: repo.id,
                description: getMessage(descKey, [repo.name], bean.userLocale),
                progressLogFile: progressFile.absolutePath,
                urlProgress: "/csvn/log/show?fileName=/temp/${progressFile.name}&view=tail",
                urlResult: "/csvn/repo/dumpFileList/${repo.id}",
                urlConfigure: "/csvn/repo/bkupSchedule/${repo.id}",
                locale: bean.userLocale]
        if (bean.cloud) {
            jobDataMap['urlResult'] = repo.cloudSvnUri
        }
        if (userId) {
            jobDataMap['userId'] = userId
        }
        // data for generating the dump file
        jobDataMap.putAll(bean.toMap())
        trigger.setJobDataMap(new JobDataMap(jobDataMap))

        jobsAdminService.createOrReplaceTrigger(trigger)
        return bean.cloud ? null : dumpFilename(bean, repo)
    }

    /**
     * method to schedule a RepoVerify quartz job
     * @param bean the parameters for the dump job
     * @param repo the repo
     */
    void scheduleVerifyJob(SchedulerBean bean, repo, Integer userId = null,
                        String tName = null, Locale locale = Locale.default) {

        log.debug("Scheduling verify for repo '${repo.name}'")
        File progressFile = BackgroundJobUtil
                .prepareProgressLogFile(repo.name, BackgroundJobUtil.JobType.VERIFY)

        if (tName) {
            jobsAdminService.removeTrigger(tName, REPO_JOB_TRIGGER_GROUP)
        }
        tName = BackgroundJobUtil.generateTriggerName(repo, BackgroundJobUtil.JobType.VERIFY, bean)

        def descKey = "repository.action.verify.job.description"

        String cron = BackgroundJobUtil.getCronExpression(bean)
        log.debug("Scheduling verify for repo '${repo.name}' using cron expression: " + cron)
        def trigger = new CronTrigger(tName, REPO_JOB_TRIGGER_GROUP, cron)
        log.debug("cron expression summary:\n" + trigger.expressionSummary)
        trigger.setJobName(RepoVerifyJob.name)
        trigger.setJobGroup(RepoVerifyJob.group)

        def jobDataMap =
            [id: tName, repoId: repo.id,
                    description: getMessage(descKey, [repo.name], locale),
                    progressLogFile: progressFile.absolutePath,
                    urlProgress: "/csvn/log/show?fileName=/temp/${progressFile.name}&view=tail",
                    urlConfigure: "/csvn/repo/bkupSchedule/${repo.id}",
                    locale: locale]
        if (userId) {
            jobDataMap['userId'] = userId
        }
        // indicate whether this is a scheduled / recurring job or adhoc / one-off in the data map
        jobDataMap['isRecurring'] = 
                !([SchedulerBean.Frequency.NOW, SchedulerBean.Frequency.ONCE].contains(bean.frequency)) 
        
        jobDataMap.putAll(bean.toMap())
        trigger.setJobDataMap(new JobDataMap(jobDataMap))

        jobsAdminService.createOrReplaceTrigger(trigger)
        return
    }

    File getLoadDirectory(Repository repo) {
        File dumpDir = new File(Server.getServer().dumpDir, repo.name)
        File loadDir = new File(dumpDir, "load")
        if (!loadDir.exists()) {
            loadDir.mkdirs()
        }
        return loadDir
    }

    /**
     * File delete utility which will retry 5 times, pausing the thread 1 second between attempts
     * @param f the file to delete
     * @return boolean indicating success or failure
     */
    private boolean deleteWithRetry(File f) {

        int retryLimit = 5
        int retryCount = 0
        boolean deleted = false
        while (!deleted && ++retryCount < retryLimit) {
            deleted = (f?.delete() == true)
            if (!deleted) {
                Thread.sleep(1000)
            }
        }
        return deleted

    }

    /**
     * schedules the repo load operation for 5 seconds out, and returns
     * @param repo the Repository to load
     * @param options any parameters needed by the
     * @return
     */
    def scheduleLoad(Repository repo, Map options, Integer userId = null) {
        long startTime = System.currentTimeMillis() + 5000L;
        def tName = "RepoLoad-${repo.name}"
        def tGroup = "AdhocLoad"
        File tempLogDir = new File(ConfigUtil.logsDirPath(), "temp")
        def progressFile = File.createTempFile("load-progress", ".txt", tempLogDir)
        def trigger = new SimpleTrigger(tName, tGroup, new Date(startTime))

        trigger.setJobName(RepoLoadJob.name)
        trigger.setJobGroup(RepoLoadJob.group)
        Locale locale = (options["locale"] ?: Locale.default) as Locale
        def jobDataMap =
        [id: "repoLoad-${repo.name}",
                repoId: repo.id,
                ignoreUuid: options["ignoreUuid"],
                description: getMessage("repository.action.loadDumpFile.job.description",
                        [repo.name], locale),
                urlProgress: "/csvn/log/show?fileName=/temp/${progressFile.name}&view=tail",
                progressLogFile: progressFile.absolutePath, 
                locale: locale]

        if (userId) {
            jobDataMap['userId'] = userId
        }
        trigger.setJobDataMap(new JobDataMap(jobDataMap))

        jobsAdminService.createOrReplaceTrigger(trigger)
    }

    def loadDumpFile(Repository repo, Map options) throws FileNotFoundException, RepoLoadException {
        File progress = new File(options["progressLogFile"])
        File loadDir = getLoadDirectory(repo)
        File[] files = loadDir.listFiles({ return it.isFile() } as FileFilter)
        if (files.length > 0) {
            File dumpFile = files[0]
            // load the dump file, cleaning up only on success exit code
            if (isRepository(dumpFile)) {
                loadHotcopy(dumpFile, repo, options, progress.newOutputStream())
            }
            else {
                loadDumpFile(dumpFile, repo, options, progress.newOutputStream())
            }
            boolean dumpFileDeleted = deleteWithRetry(dumpFile)
            boolean progressFileDeleted = deleteWithRetry(progress)
            log.debug("Deleted the dump file? ${dumpFileDeleted}; Deleted the progress file? ${progressFileDeleted}")
        }
        else {
            def message = "No dumpfile found in the load location '${loadDir.absolutePath}' for repo '${repo.name}'"
            log.error(message)
            throw new FileNotFoundException(message)
        }
    }

    def loadDumpFile(File dumpFile, Repository repo,
                     Map options, OutputStream progress) throws FileNotFoundException, RepoLoadException {

        log.info("Loading dumpfile '${dumpFile?.absolutePath}' to repo '${repo?.name}'")
        if (!dumpFile?.exists()) {
            def message = "No dumpfile found in the load location '${dumpFile?.absolutePath}' for repo '${repo?.name}'"
            log.error(message)
            throw new FileNotFoundException(message)
        }
        def cmd = [ConfigUtil.svnadminPath(), "load"]
        if (options["ignoreUuid"]) {
            cmd << "--ignore-uuid"
        }
        cmd << getRepositoryHomePath(repo)

        Process p = null
        InputStream is = null
        // if the dumpfile ends in .zip, stream in the first ZipEntry to the load process
        def zipFile = null
        if (dumpFile.name.endsWith(".zip")) {
            zipFile = new ZipFile(dumpFile)
            is = zipFile.getInputStream(zipFile.entries.nextElement())
        }
        else {
            is = dumpFile.newInputStream()
        }
        p = commandLineService.startProcessWithInputStream(cmd, is, progress)
        p.waitFor()
        // close resources
        is.close()
        progress.close()
        if (zipFile) {
            zipFile.close()
        }
        // signal errors
        if (p.exitValue() != 0) {
            log.error("Unable to load the dump file '${dumpFile.absolutePath}'")
            throw new RepoLoadException(p.text)
        }
    }

    /**
     * replaces a repository with the supplied hotcopy
     * @param source the hotcopy (either a zip or a repo directory location)
     * @param target the target Repository to replace
     * @param options
     * @param progress
     * @return
     * @throws RepoLoadException
     */
    def loadHotcopy(File source, Repository target,
                    Map options, OutputStream progress) throws RepoLoadException {

        log.info("Loading dumpfile '${source?.absolutePath}' to repo '${target?.name}'")
        if (!source?.exists() || !isRepository(source)) {
            def message = "No repository hotcopy found in the load location '${source?.absolutePath}' for repo '${repo?.name}'"
            log.error(message)
            throw new RepoLoadException(message)
        }

        // store the original repo uuid if needed -- will set the unzipped hotcopy to this if ignore-uuid is true
        String uuid = getReposUUID(target)

        boolean isDirectory = source.exists() && source.isDirectory()
        boolean isZip = source.exists() && source.isFile() && source.name.toLowerCase().endsWith(".zip")

        File hotcopyLocation = null
        File originalHotcopyLocation = null

        // unzip an archived hotcopy to the temp location
        if (isZip) {
            // create temp location
            hotcopyLocation = new File(getRepositoryHomePath(target) + "_tmp")
            // delete empty file and mkdir instead
            hotcopyLocation.delete()
            hotcopyLocation.mkdir()
            // unzip into this dir
            fileUtil.extractArchive(source, hotcopyLocation)
            // find the repo root
            originalHotcopyLocation = hotcopyLocation
            hotcopyLocation = findRepoRoot(hotcopyLocation)
        }
        // or, if hotcopy is already unzipped
        else if (isDirectory) {
            hotcopyLocation = source
        }

        // now move the hotcopy into place
        File repoLocation = new File(getRepositoryHomePath(target))
        if (!repoLocation.deleteDir()) {
            throw new RepoLoadException("Unable to move original repo out of the way for load")
        }
        if (!hotcopyLocation.renameTo(new File(getRepositoryHomePath(target)))) {
            throw new RepoLoadException("Unable to move hotcopy into place")
        }
        // delete the hotcopy unzip location, if any dirs are left behind
        if (originalHotcopyLocation?.exists()) {
            originalHotcopyLocation.deleteDir()
        }

        // if load option "ignoreUuid" is true, we should restore the UUID of the original repo (ignoring UUID of the
        // hotcopy backup)
        if (options["ignoreUuid"]) {
            setReposUUID(target, uuid)
        }
    }

    /**
     * given a file location, will find the repo root dir at this location or beneath. mainly,
     * this is intended to find the root of an unzipped hotcopy, which might include a repo name parent dir
     * containing the svn artifacts
     * @param file a directory
     * @return the File which is the repository root
     * @throws FileNotFoundException if no repo root can be found in the hierarchy
     */
    File findRepoRoot(File file) throws FileNotFoundException {
        if (!file || !file.exists() || !file.isDirectory()) {
            throw new FileNotFoundException("the root file is not a directory or does not exist")
        }
        // collect candidate directories, starting with root provided
        def repoRootCandidates = [file]
        file.eachDir() { it ->
            repoRootCandidates << it
        }
        // find first with repo characteristics
        def rootToReturn = repoRootCandidates.find({isRepository(it)})
        if (rootToReturn) {
            return rootToReturn
        }
        else {
            throw new FileNotFoundException("the root and its subdirs do not appear to be repo hotcopies")
        }
    }

    /**
     * Method to invoke "svnadmin dump" possibly piped through svndumpfilter
     *
     * @param bean dump options
     * @param repo domain object
     * @return dump filename
     */
    String createDump(DumpBean bean, repo) throws ConcurrentBackupException {
        Server server = Server.getServer()
        def cmd = [ConfigUtil.svnadminPath(), "dump"]
        cmd << new File(server.repoParentDir, repo.name).canonicalPath
        if (!bean.revisionRange) {
            bean.revisionRange = "0:" + findHeadRev(repo)
        }
        cmd << "-r"
        cmd << bean.revisionRange
        if (bean.incremental) {
            cmd << "--incremental"
        }
        if (bean.deltas) {
            cmd << "--deltas"
        }

        File dumpDir = new File(server.dumpDir, repo.name)
        if (!dumpDir.exists()) {
            dumpDir.mkdirs()
        }

        File progressLogFile = BackgroundJobUtil.prepareProgressLogFile(repo.name, BackgroundJobUtil.JobType.DUMP)
        if (progressLogFile.exists()) {
            String msg = getMessage("repository.action.backup.alreadyInProgress",
                    [repo.name, progressLogFile.canonicalPath])
            throw new ConcurrentBackupException(msg)
        }

        String filename = dumpFilename(bean, repo)
        File tempDumpFile = new File(dumpDir, filename + "-processing")
        File finalDumpFile = new File(dumpDir, filename)
        if (tempDumpFile.exists() || finalDumpFile.exists()) {
            throw new ValidationException("dumpBean.filename.exists", "filename")
        }

        log.debug("Dump command: " + cmd)
        Process dumpProcess = commandLineService.startProcess(cmd)
        FileOutputStream progress = new FileOutputStream(progressLogFile)
        FileOutputStream out = new FileOutputStream(tempDumpFile)

        if (!bean.deltas && bean.filter && (bean.includePath || bean.excludePath)) {
            log.debug("Dump: With filter")
            String svndumpfilterPath = new File(new File(
                    ConfigUtil.svnadminPath()).parent, "svndumpfilter").canonicalPath
            def threads = []
            threads << dumpProcess.consumeProcessErrorStream(progress)
            if (bean.includePath) {
                def filterCmd = [svndumpfilterPath, "include"]
                addFilterOptions(bean, filterCmd)
                filterCmd.addAll(bean.includePathPrefixes)
                dumpProcess = dumpProcess.pipeTo(commandLineService.startProcess(filterCmd))
                threads << dumpProcess.consumeProcessErrorStream(progress)
            }
            if (bean.excludePath) {
                def filterCmd = [svndumpfilterPath, "exclude"]
                addFilterOptions(bean, filterCmd)
                filterCmd.addAll(bean.excludePathPrefixes)
                dumpProcess = dumpProcess.pipeTo(commandLineService.startProcess(filterCmd))
                threads << dumpProcess.consumeProcessErrorStream(progress)
            }
            threads << dumpProcess.consumeProcessOutputStream(out)

                try {
                    for (t in threads) {
                        try {
                            t.join()
                        } catch (InterruptedException e) {
                            log.debug("Process consuming thread was interrupted")
                        }
                    }
                } finally {
                    out.close()
                }
                finishDumpFile(finalDumpFile, tempDumpFile, progress,
                        progressLogFile, bean.userLocale)
                cleanupOldBackups(bean, repo)

        } else {
            log.debug("Dump: No filter")
                try {
                    dumpProcess.waitForProcessOutput(out, progress)
                } finally {
                    out.close()
                }
                finishDumpFile(finalDumpFile, tempDumpFile, progress,
                        progressLogFile, bean.userLocale)
                cleanupOldBackups(bean, repo)
        }
        return filename
    }

    private finishDumpFile(finalDumpFile, tempDumpFile, progress,
                           progressLogFile, locale) {
        def dumpFilename = finalDumpFile.name
        if (dumpFilename.endsWith('.zip')) {
            println('repository.service.bkup.progress.compress.dump.file',
                    progress, locale)
            File tempZipFile = new File(tempDumpFile.parentFile,
                    tempDumpFile.name + ".zip")
            def baseDumpFilename =
                    dumpFilename.substring(0, dumpFilename.length() - 4)

            ZipArchiveOutputStream zos = null
            try {
                zos = new ZipArchiveOutputStream(tempZipFile)
                zos.setCreateUnicodeExtraFields(ZipArchiveOutputStream
                        .UnicodeExtraFieldPolicy.NOT_ENCODEABLE)
                ZipArchiveEntry ze = 
                        new ZipArchiveEntry(tempDumpFile, baseDumpFilename)
                zos.putArchiveEntry(ze)
                tempDumpFile.withInputStream { zos << it }
                zos.closeArchiveEntry()
            }
            finally {
                zos?.close()
            }

            println('repository.service.bkup.progress.compress.dump.file.done',
                    progress, locale)
            tempDumpFile.delete()
            tempDumpFile = tempZipFile
        }
        println('repository.service.bkup.progress.rename.dump.file',
                progress, locale)
        if (!tempDumpFile.renameTo(finalDumpFile)) {
            log.warn("Rename of dump file " + tempDumpFile?.name + " to " +
                    finalDumpFile?.name + " failed.")
        }
        println('repository.service.bkup.progress.dump.done',
                [finalDumpFile?.name], progress, locale)
        progress.close()
        progressLogFile.delete()
    }

    private cleanupOldBackups(dumpBean, repo) {
        int numToKeep = dumpBean.numberToKeep
        if (dumpBean.backup && numToKeep > 0) {
            def prefix = repo.name + "-bkup-"
            def hcPrefix
            if (dumpBean.hotcopy) {
                prefix += 'hotcopy'
            } else {
                hcPrefix = prefix + 'hotcopy'
            }
            println "\n\nPruning files containing ${prefix}"
            def dumps = listDumpFiles(repo, "date", DESCENDING)
            int i = 0
            for (dumpFile in dumps) {
                def name = dumpFile.name
                if (name.startsWith(prefix) &&
                    (!hcPrefix || !name.startsWith(hcPrefix)) &&
                        !name.endsWith("-processing") &&
                        !name.endsWith("-processing.zip") &&
                        (++i > numToKeep)) {

                    dumpFile.delete()
                    println "Deleted ${dumpFile}"
                }
                else { println "Leaving ${dumpFile}" }
            }
        }
    }

    private String pad(int value) {
        return (value < 10) ? "0" + value : String.valueOf(value)
    }

    String dumpFilename(DumpBean bean, repo) {
        Calendar cal = Calendar.getInstance()
        SchedulerBean sched = bean.schedule
        String ts = ""
        ts += (sched.year < 1) ? cal.get(Calendar.YEAR) : sched.year
        ts += pad((sched.month < 1) ? cal.get(Calendar.MONTH) + 1 : sched.month)
        ts += pad((sched.dayOfMonth < 1) ?
            cal.get(Calendar.DAY_OF_MONTH) : sched.dayOfMonth)
        ts += pad((sched.hour < 0) ? cal.get(Calendar.HOUR_OF_DAY) : sched.hour)
        ts += pad((sched.minute < 0) ? cal.get(Calendar.MINUTE) : sched.minute)
        ts += pad((sched.second < 0) ? cal.get(Calendar.SECOND) : sched.second)
        def range = bean.revisionRange ?
            "-r" + bean.revisionRange.replace(":", "_") : ""
        def options = ""
        if (bean.incremental) {
            options += "-incremental"
        }
        if (bean.deltas) {
            options += "-deltas"
        }
        if (bean.filter) {
            options += "-filtered"
        }
        def zip = bean.compress ? ".zip" : ""
        def prefix = bean.backup ? repo.name + "-bkup" : repo.name
        if (bean.hotcopy) {
            prefix += "-hotcopy"
        }
        return prefix + range + options + "-" + ts + ".dump" + zip
    }

    /**
     * Will find most-recent up-to-date backup matching the DumpBean criteria for the given repo,
     * or null if there is no existing backup of the criteria
     * @param bean the DumpBean
     * @param repo the repo
     * @return File or null
     */
    File findUpToDateBackup(DumpBean bean, Repository repo) {
        // find the dumpfile name we would generate now
        if (!bean.revisionRange) {
            bean.revisionRange = "0:" + findHeadRev(repo)
        }
        String name = dumpFilename(bean, repo)

        // see if any file already exists, ignoring date
        int dateBegin = name.lastIndexOf("-") + 1
        int dateEnd = name.indexOf(".", dateBegin)
        String namePrefix = name.substring(0, dateBegin)
        String nameSuffix = name.substring(dateEnd)
        File dumpDir = new File(Server.server.dumpDir, repo.name)

        // return last match or null
        def existingBackups = []
        if (dumpDir.exists()) {
            dumpDir.eachFileMatch(~"${namePrefix}\\d{14}${nameSuffix}") {
                log.debug("found existing backup file which is up to date: " +
                          it.name)
                existingBackups << it
            }
        }
        if (existingBackups.size()) {
            return existingBackups.sort{ file -> file.lastModified() }.reverse()[0]
        }
        else {
            return null
        }
    }

    private addFilterOptions(DumpBean bean, filterCmd) {
        if (bean.dropEmptyRevs) {
            filterCmd << "--drop-empty-revs"
            if (bean.renumberRevs) {
                filterCmd << "--renumber-revs"
            }
        } else if (bean.preserveRevprops) {
            filterCmd << "--preserve-revprops"
        }

        if (bean.skipMissingMergeSources) {
            filterCmd << "--skip-missing-merge-sources"
        }
    }

    /**
     * Removes all triggers for backup and verify jobs on the given repository
     *
     * @param repo Repository domain object
     */
    def clearScheduledJobs(repo) {
        def jobs = retrieveScheduledJobs(repo)
        jobs?.each { 
            deleteScheduledJob(it.id)
        }
    }
    
    /**
     * Removes the given trigger
     */
    def deleteScheduledJob(jobId) {
        // Need to convert for pre-3.0 job id's
        if (jobId?.startsWith('repoDump-')) {
            jobId = jobId.substring(9)
        }
        else if (jobId?.startsWith('repoHotcopy-')) {
            jobId = jobId.substring(12)
        }
        jobsAdminService.removeTrigger(jobId, REPO_JOB_TRIGGER_GROUP)
    }    

    /**
     * Method to invoke "svnadmin hotcopy", verify the result, and create
     * an archive suitable for back up.
     *
     * @param bean dump options
     * @param repo domain object
     * @return archive filename
     */
    String createHotcopy(DumpBean bean, repo) throws ConcurrentBackupException {
        bean.hotcopy = true
        bean.compress = true
        Server server = Server.getServer()
        File repoDir = new File(server.repoParentDir, repo.name)
        File dumpDir = new File(server.dumpDir, repo.name)
        if (!dumpDir.exists()) {
            dumpDir.mkdirs()
        }
        File tmpDir = new File(dumpDir, "hotcopy")

        File progressLogFile = BackgroundJobUtil.prepareProgressLogFile(repo.name, BackgroundJobUtil.JobType.HOTCOPY)
        if (progressLogFile.exists()) {
            String msg = getMessage("repository.action.backup.alreadyInProgress",
                    [repo.name, progressLogFile.canonicalPath])
            throw new ConcurrentBackupException(msg)
        }
        Locale locale = bean.userLocale

        def cmd = [ConfigUtil.svnadminPath(), "hotcopy",
                repoDir.canonicalPath, tmpDir.canonicalPath]
        FileOutputStream progress = null
        String filename = null
        try {
            progress = new FileOutputStream(progressLogFile)
            println('repository.service.bkup.progress.hotcopy.start',
                    progress, locale)
            def result = commandLineService.execute(cmd, progress, progress)
            boolean isVerified = false
            if (result[0] == "0") {
                println('repository.service.bkup.progress.verifying.hotcopy',
                        progress, locale)
                isVerified = verifyRepositoryPath(tmpDir.canonicalPath, progress)
                println('repository.service.bkup.progress.hotcopy.verify.done',
                        progress, locale)
            }
            if (isVerified) {
                bean.revisionRange = "0:" + findHeadRev(tmpDir.canonicalPath)
                filename = dumpFilename(bean, repo)
                File finalZipFile = new File(dumpDir, filename)
                File tempZipFile = new File(dumpDir,
                        filename.substring(0, filename.length() - 4) + "-processing.zip")
                println('repository.service.bkup.progress.compressing.hotcopy',
                        progress, locale)
                fileUtil.archiveDirectory(tmpDir, tempZipFile, progress)
                println('repository.service.bkup.progress.compress.done.rename.file',
                        progress, locale)
                if (!tempZipFile.renameTo(finalZipFile)) {
                    log.warn("Rename of file " + tempZipFile?.name + " to " +
                            finalZipFile?.name + " failed.")
                }
            } else {
                println('repository.service.bkup.progress.verify.failed',
                        progress, locale)
                log.warn("Hotcopy of ${repo.name} repository failed to verify")
            }
            progress.close()
            progress = null
            progressLogFile.delete()
        } finally {
            progress?.close()
            tmpDir.deleteDir()
        }
        cleanupOldBackups(bean, repo)
        return filename
    }

    def getCertDetails(String svnUrl, String username, String password) {
        
        def command = [ConfigUtil.svnPath(), "ls", svnUrl,
                "--username", username, "--password", password,
                "--config-dir", ConfigUtil.svnConfigDirPath()]
        String[] commandOutput =
                commandLineService.execute(command.toArray(new String[0]),
                [LANG:"en_US.utf8"], null, true)
        String errorMessage = commandOutput[2]
        
        if (errorMessage) {
            return parseCertificate(errorMessage)

        } else {
            return [hostname: null, validity: null, issuer: null,
                    fingerprint: null]
        }
    }
    
    /**
     * This function parses the raw certificate and returns the certificate
     * credentials.
     */
    private def parseCertificate(String rawCertificate) {
        if (rawCertificate.find("Certificate information:")) {
            def certLines = rawCertificate.split("\n")
            String hostname
            String validity
            String issuer
            String fingerPrint
            for (line in certLines) {
                if(line.find("- Hostname:")) {
                    def hostnameLine = line.split(" - Hostname: ")
                    hostname = hostnameLine[1]
                }
                else if(line.find("- Valid:")) {
                    def validLine = line.split(" - Valid: ")
                    validity = validLine[1]
                }
                else if(line.find("- Issuer:")) {
                    def issuerLine = line.split(" - Issuer: ")
                    issuer = issuerLine[1]
                }
                else if(line.find("- Fingerprint:")) {
                    def fingerPrintLine = line.split(" - Fingerprint: ")
                    fingerPrint = fingerPrintLine[1]
                }
            }
            return [hostname: hostname, validity: validity, issuer: issuer,
                    fingerprint: fingerPrint]
        }
        else {
            return [hostname: null, validity: null, issuer: null,
                    fingerprint: null]
        }
    }
    
    boolean acceptSslCertificate(svnUrl, username, password, 
            viewedFingerprint = null, boolean isTemporary = false) {

        boolean isAccepted = false
        def command = [ConfigUtil.svnPath(), "--config-dir",
            ConfigUtil.svnConfigDirPath(), "ls", svnUrl,
            "--username", username, "--password", password]
        Process p = commandLineService.startProcess(command, [LANG:"en_US.utf8"], true)
        StringBuffer outBuffer = new StringBuffer(512)
        StringBuffer errorBuffer = new StringBuffer(512)
        p.consumeProcessOutput(outBuffer, errorBuffer)

        def currentFingerprint = null
        // give several minutes for response
        def limit = 300 * 1000
        def waitTime = 400
        while (viewedFingerprint && viewedFingerprint != currentFingerprint && 
                waitTime < limit) {
            Thread.sleep(waitTime)
            def cert = parseCertificate(errorBuffer.toString())
            currentFingerprint = cert.fingerprint
            waitTime *= 2
        }

        if (viewedFingerprint == currentFingerprint) {
            def lineEnd = System.getProperty("line.separator")
            def acceptResponse = (isTemporary ? "t" : "p") + lineEnd
            p.withWriter { it << acceptResponse }
            isAccepted = true
            p.waitFor()
        }
        return isAccepted
    }

    /**
     * Lists all the files within 'hooks' directory for the given repository.
     * Subdirectories are not included.
     * 
     * @param repo A Repository object
     * @return List of File objects
     */
    List<File> listHooks(repo, sortBy = "name", isAscending = true) {
        def files = []
        
        Server server = Server.getServer()
        File repoDir = new File(server.repoParentDir, repo.name)
        File hooksDir = new File(repoDir, "hooks")
        if (hooksDir.exists()) {
            hooksDir.eachFile(FileType.FILES) { f ->
                files << f
            }
            files = sortFiles(files, sortBy, isAscending)
        }
        return files
    }

    /**
     * creates a hook script for the repo, but does not allow
     * an existing file at the target name to be overwritten
     * @param repo The repository
     * @param sourceFile a file to be installed as a hook script
     * @param targetFilename the final name (optional)
     */
    boolean createHook(Repository repo, File sourceFile, String targetFilename) {

        if (!repo || !sourceFile) {
            throw new IllegalArgumentException("The repository and source file must be defined")
        }
        def repoHomeDir = getRepositoryHomePath(repo)
        def hooksDir = new File(repoHomeDir, "hooks")
        File destinationFile = new File(hooksDir, targetFilename ?: sourceFile.name)

        destinationFile.exists() ? false : createOrReplaceHook(repo, sourceFile, targetFilename)
    }

    /**
     * Creates or replaces a hook script for the repo. 
     * The <code>sourceFile</code> is presumed to be a temporary file which
     * is moved into the repo <code>hooks</code> folder and made
     * executable. If a name is provided, that will be used instead
     * of the file's original name.
     * @param repo The repository
     * @param sourceFile a file to be installed as a hook script
     * @param targetFilename the final name (optional)
     */
    boolean createOrReplaceHook(Repository repo, File sourceFile, String targetFilename) {

        if (!repo || !sourceFile) {
            throw new IllegalArgumentException("The repository and source file must be defined")
        }
        def repoHomeDir = getRepositoryHomePath(repo)
        def hooksDir = new File(repoHomeDir, "hooks")
        File destinationFile = new File(hooksDir, targetFilename ?: sourceFile.name)
        log.info("Creating hook script: ${destinationFile.canonicalPath}")
        
        boolean existingFileDeleted = false
        boolean renamed = false
        // remove any existing file at the destination
        if (destinationFile.exists()) {
            existingFileDeleted = destinationFile.delete()
        }
        // move the input file to the destination
        renamed = sourceFile.renameTo(destinationFile)
        // may need to copy from one FS to another
        if (!renamed) {
            destinationFile.withOutputStream { out ->
                    sourceFile.withInputStream { input -> out << input } }
            sourceFile.delete()
            renamed = true
        }
        destinationFile.setExecutable(true)
        return renamed
    }
    
    private def sortFiles(files, sortBy, isAscending) {
        int sign = isAscending ? 1 : -1
        switch (sortBy) {
            case "name":
                files = files.sort { a, b -> sign * (a.name <=> b.name) }
                break
            case "size":
                files = files.sort { f -> sign * f.length() }
                break
            case "date":
            case "lastModified":
                files = files.sort { f -> sign * f.lastModified() }
                break
            default:
                files = files.sort { f -> -1 * f.lastModified() }
        }
        return files
    }
    
    /**
    * Copies a file within the hooks directory
    *
    * @param repo A Repository object
    * @param originalName the current hook script
    * @param copyName the name of the copy
    * @return File pointing to the new file
    */
    File copyHookFile(repo, originalName, copyName) 
           throws FileNotFoundException, ValidationException {
        
        return hookFileOperation(repo, originalName, copyName) { 
            sourceFile, targetFile ->
            targetFile.withOutputStream { out -> copyFile(sourceFile, out) }
            targetFile.setExecutable(true)
        }
    }

    private File hookFileOperation(repo, sourceName, targetName, Closure c) 
            throws FileNotFoundException, ValidationException {
        // Assuming subversion/apache should have similar capabilities with
        // respect to hook filenames as with repository directory names
        if (targetName && 
                !targetName.matches(Repository.RECOMMENDED_NAME_PATTERN)) {
            throw new ValidationException("repository.name.matches.invalid")
        }
        
        File sourceFile = getHookFile(repo, sourceName)
        Server server = Server.getServer()
        File repoDir = new File(server.repoParentDir, repo.name)
        File hooksDir = new File(repoDir, "hooks")
        File targetFile = null
        if (targetName) {
            targetFile = new File(hooksDir, targetName)
            if (targetFile.exists()) {
                throw new ValidationException("repository.file.already.exists")
            }
        }
        c(sourceFile, targetFile)
        return targetFile
    }

    /**
    * Renames a file within the hooks directory
    *
    * @param repo A Repository object
    * @param sourceName the current hook script
    * @param targetName the new name of the hook
    * @return File pointing to the newly named file
    */
    File renameHookFile(repo, sourceName, targetName) 
           throws FileNotFoundException, ValidationException {

        return hookFileOperation(repo, sourceName, targetName) { 
            sourceFile, targetFile ->
            if (!sourceFile.renameTo(targetFile)) {
                targetFile.withOutputStream { out -> copyFile(sourceFile, out) }
                sourceFile.delete()
            }
            targetFile.setExecutable(true)
        }
    }    
    
    def File getHookFile(repo, filename) throws FileNotFoundException {
        Server server = Server.getServer()
        File repoDir = new File(server.repoParentDir, repo.name)
        File hooksDir = new File(repoDir, "hooks")
        File hookFile = new File(hooksDir, filename)
        if (hookFile.exists()) {
            return hookFile
        }
        throw new FileNotFoundException(filename)
    }
    
    /**
     * Passes the contents of the hook file to the outputStream
     */
    boolean streamHookFile(repo, filename, outputStream) {
        File hookFile = getHookFile(repo, filename)
        return copyFile(hookFile, outputStream)
    }
    
    /**
     * Removes the hook file/script from the filesystem
     * @return true if the file existed and was deleted
     */
    boolean deleteHookFile(repo, hookName) throws FileNotFoundException {
        boolean result = false
        hookFileOperation(repo, hookName, null) {
            sourceFile, targetFile ->
            result = sourceFile.delete()
        }
        return result
    }

    /**
     * Retrieve a list of repositories for which the given username has access to the
     * root path.
     *
     * @param username
     * @return list of repository names
     */
    def listMatchingRepositories(String query, String username, boolean sortByName = false,
            int maxUnfilteredResults = 100, int maxAuthzResults = 20) {
        // repo names might contain _ and we'll escape % just for completeness
        query = query.replace('_', '[_]').replace('%', '[%]')
        def allRepos = Repository.findAllByNameLike('%' + query + '%',
                sortByName ? [sort: 'name'] : null)
        if (allRepos.size() <= maxUnfilteredResults) {
            def repos = filterAuthorizedRepositories(allRepos, username, maxAuthzResults + 1)
            return (repos?.size() > maxAuthzResults) ? null : repos
        }
        return null
    }

    /**
     * Retrieve a list of repositories for which the given username has access to the
     * root path.
     * 
     * @param username
     * @param params sort and page parameters
     * @return list of Repository
     */
    def listAuthorizedRepositories(String username, boolean sortByName) {
        def repos = sortByName ? 
                Repository.list([sort: "name"]) : Repository.list()
        return filterAuthorizedRepositories(repos, username)
    }
                
    private def filterAuthorizedRepositories(repos, username, maxResults = -1) {
        def authzRepos = []
        File accessFile = new File(ConfigUtil.confDirPath(), "svn_access_file")
        String accessPath = accessFile.absolutePath
        Server server = Server.getServer()
        if (server.forceUsernameCase) {
            username = username.toLowerCase()
        }
        for (repo in repos) {
            boolean isAccess = checkAccessOf(username, repo.name, accessPath)
            if (isAccess) {
                authzRepos << repo
                if (maxResults > 0 && authzRepos.size() >= maxResults) {
                    break
                }
            } 
        }
        return authzRepos
    }
    
    private boolean checkAccessOf(String username, String repoName, String accessRulesPath) {
        boolean b = false
        def svnauthzPath = ConfigUtil.svnauthzPath()
        def output = commandLineService.executeWithOutput(svnauthzPath, 'accessof', 
                '--username', username, '--repository', repoName, '--path', '/',
                accessRulesPath)
        if (output.contains('r')) {
            b = true
        }
        return b
    }
}
