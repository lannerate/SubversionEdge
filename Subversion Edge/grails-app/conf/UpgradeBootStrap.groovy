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

import com.collabnet.svnedge.console.DumpBean
import com.collabnet.svnedge.admin.LogManagementService.ApacheLogLevel
import com.collabnet.svnedge.admin.LogManagementService.ConsoleLogLevel
import com.collabnet.svnedge.admin.RepoDumpJob
import com.collabnet.svnedge.console.BackgroundJobUtil
import com.collabnet.svnedge.domain.Repository
import com.collabnet.svnedge.domain.SchemaVersion
import com.collabnet.svnedge.domain.Server
import com.collabnet.svnedge.domain.ServerMode
import com.collabnet.svnedge.domain.integration.CtfServer
import com.collabnet.svnedge.domain.statistics.Statistic
import com.collabnet.svnedge.util.ConfigUtil

/**
 * Bootstrap script for handling any special conditions associated with upgrades
 */
class UpgradeBootStrap {

    def operatingSystemService
    def fileSystemStatisticsService
    def dataSource
    def jobsAdminService
    def lifecycleService
    def serverConfService
    def svnRepoService

    def init = { servletContext ->
        log.info("Applying updates")
        release1_1_0()
        release1_2_0()
        release1_3_1()
        release2_1_0()
        release3_0_2()
        release3_4_0()
    }

    private boolean isSchemaCurrent(int major, int minor, int revision) {

        def v = SchemaVersion.createCriteria()
        def resultCount = v.get {
            and {
                eq("major", major)
                eq("minor", minor)
                eq("revision", revision)
            }
            projections {
                rowCount()
            }
        }
        return (resultCount > 0)
    }

    private def release1_1_0() {

        if (isSchemaCurrent(1, 1, 0)) {
            // result found at version, assume this is applied
            return
        }

        log.info("Applying 1.1.0 updates")

        def server = Server.getServer()
        if (server) {
            log.info("Initializing new fields on Server instance")
            server.mode = ServerMode.STANDALONE
            server.save()
        }

        SchemaVersion v = new SchemaVersion(major: 1, minor: 1, revision: 0,
                description: "1.1.0 added Server field: mode")
        v.save()
    }

    def void release1_2_0() {

        if (isSchemaCurrent(1, 2, 0)) {
            return
        }

        // the current changes necessary are only for windows.
        if (!operatingSystemService.isWindows()) {
            return
        }

        log.info("Applying 1.2.0 updates")

        Statistic.executeUpdate("UPDATE Statistic s SET s.name='BytesIn' " +
                "WHERE s.name='WinBytesIn'")
        Statistic.executeUpdate("UPDATE Statistic s SET " +
                "s.name='BytesOut' WHERE s.name='WinBytesOut'")

        SchemaVersion v = new SchemaVersion(major: 1, minor: 2, revision: 0,
                description: "1.2.0 updated Statistic values: name. " +
                        "(WinBytesIn -> BytesIn), (WinBytesOut -> BytesOut).")
        v.save()
    }

    def void release1_3_1() {

        if (isSchemaCurrent(1, 3, 1)) {
            return
        }

        log.info("Applying 1.3.1 updates")

        Server.executeUpdate("UPDATE Server s SET s.ldapEnabledConsole = " +
                "s.ldapEnabled")

        SchemaVersion v = new SchemaVersion(major: 1, minor: 3, revision: 1,
                description: "1.3.1 updated Server adding field " +
                        "'ldapEnabledConsole'.")
        v.save()
    }

    def void release2_1_0() {
        if (isSchemaCurrent(2, 1, 0)) {
            return
        }
        log.info("Applying 2.1.0 updates")

        Server s = Server.getServer()
        if (s) {
            File dumpDir = new File(ConfigUtil.dumpDirPath())
            if (!dumpDir.exists()) {
                dumpDir.mkdir()
            }

            s.dumpDir = ConfigUtil.dumpDirPath()
            s.save(flush: true)
        }

        SchemaVersion v = new SchemaVersion(major: 2, minor: 1, revision: 0,
                description: "2.1.0 added Quartz tables and data; initialized Server.dumpDir field")

        v.save()

    }

    def void release3_0_2() {
        if (isSchemaCurrent(3, 0, 2)) {
            return
        }
        log.info("Applying 3.0.2 updates")

        def triggers = jobsAdminService
                .getTriggers(RepoDumpJob.jobName, RepoDumpJob.group)
        for (trigger in triggers) {
            if (trigger.group == "Backup") {
                String oldName = trigger.name
                try {
                    def dataMap = trigger.jobDataMap
                    Repository repo = Repository.get(dataMap.get("repoId"))
                    DumpBean bean = DumpBean.fromMap(dataMap)
                    if (repo && bean) {
                        String newName = 
                                BackgroundJobUtil.generateTriggerName(repo, bean)
                        if (oldName != newName) {
                            jobsAdminService
                                    .removeTrigger(oldName, trigger.group)
                            trigger.name = newName
                            dataMap.id = newName
                            jobsAdminService.scheduleTrigger(trigger)
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to update trigger name for '" + oldName +
                            "', so it may show up in Schedule Backup existing" +
                            "jobs, but not in the Scheduled Jobs of the " +
                            "individual repository.  To correct this, delete" +
                            " the backup job from Schedule Backup screen.", e)
                }
            }
        }

        SchemaVersion v = new SchemaVersion(major: 3, minor: 0, revision: 2,
                description: "3.0.2 Renamed quartz triggers for backup jobs")
        v.save()
    }
    
    private static String BEGIN_SOURCEFORGE_SECTION = "BEGIN SOURCEFORGE SECTION - Do not remove these lines";
    private static String END_SOURCEFORGE_SECTION = "END SOURCEFORGE SECTION";

    def void release3_4_0() {
        if (isSchemaCurrent(3, 4, 0)) {
            return
        }

        Server server = Server.getServer()
        if (!server || server.mode == ServerMode.STANDALONE) {
            log.info("4.0.0 update is not needed in standalone mode.")
        } else {
            log.info("Applying 4.0.0 updates")
            
            String pythonExecutable = "python"
            def sfIntegrationsRoot = new File(ConfigUtil.appHome(), 'lib/integration').absolutePath
            def systemId = CtfServer.getServer().mySystemId
        
            StringBuilder postRevpropChangeContent = new StringBuilder()
            postRevpropChangeContent.append(pythonExecutable)
                .append(' "')
                .append(sfIntegrationsRoot)
                .append('/post-revprop-change.py" ')
                .append('"$1" "$2" "$3" "$4" "$5" ')
                .append(systemId)
                .append('\n');
    
            String hook = 'post-revprop-change'
            def os = operatingSystemService.isWindows() ? windowsAdapter : unixAdapter
            addHookScript(postRevpropChangeContent.toString(), hook, server.repoParentDir, os)
        }
    
        SchemaVersion v = new SchemaVersion(major: 3, minor: 4, revision: 0,
                description: "4.0.0 For CTF integration server, added post-revprop-change")
        v.save()
    }

	private void addHookScript(String content, hook, repoParentDir, os) {
		String script = buildHookScript(os, content)
		def files = Arrays.asList(new File(repoParentDir).listFiles(
				{repoDir -> new File(repoDir, "hooks").exists() } as FileFilter))
		files.each { repoDir ->
            File hookScriptFile = os.getHookScriptFile(repoDir, hook);
            
			// if file exists, replace it
			if (hookScriptFile.exists()) {
                File hookScriptFileBkup = os.getHookScriptFile(repoDir, hookScriptFile.name + '.bkup');
                hookScriptFileBkup.text = hookScriptFile.text
			} 
            hookScriptFile.text = script
            hookScriptFile.executable = true
		}
	}

    // the following is modified from AbstractCommandExecutor.createHookScript in integration 
    private String buildHookScript(def os, String scriptContent) {
            
        File tfProps = new File(ConfigUtil.confDirPath(), "teamforge.properties")
        def sfPropertiesPath = tfProps.absolutePath
        def libDir = new File(ConfigUtil.appHome(), "lib").absolutePath
        def pythonPath = "${libDir}${File.pathSeparator}${libDir}${File.separator}svn-python"
        
        def normalizePath = { String fileSystemPath ->
            return fileSystemPath.replaceAll("\\\\", "/").replace("//", "/")
        }

        StringBuilder script = new StringBuilder();
        script.append(os.commentStr + " " + BEGIN_SOURCEFORGE_SECTION);
        script.append("\n\n");

        script.append(os.getEnvironmentVariableString("SOURCEFORGE_PROPERTIES_PATH",
                        os.quoteStr + normalizePath(sfPropertiesPath) + os.quoteStr));
        script.append("\n");

        if (null != pythonPath) {
            // update path string for platform, then add to script
            script.append(os.getEnvironmentVariableString("PYTHONPATH",
                            os.quoteStr + normalizePath(pythonPath) + os.quoteStr));
            script.append("\n\n");
        } else {
            // For pretty scripts
            script.append("\n");
        }

        // update script command for platform
        // assume "python" is on path
        scriptContent = scriptContent.replaceAll("/", os.fileSeparator);
        scriptContent = os.replaceArguments(scriptContent);

        script.append(scriptContent);
        script.append("\n");
        script.append(os.commentStr + " " + END_SOURCEFORGE_SECTION);
        script.append("\n");
        os.doScriptModifications(script);
        return script.toString()
    }
    
    def unixAdapter = [
        quoteStr: "'",
        commentStr: "#",
        fileSeparator: "/",
        getEnvironmentVariableString: { String name, String value ->
            return name + '=' + value + '\nexport ' + name
        },
        getHookScriptFile: { File repository, String hook ->
            return new File(new File(repository, "hooks"), hook)
        },
        doScriptModifications: { StringBuilder sb ->
            sb.insert(0, "#!/bin/sh\n\n");
        },
        replaceArguments: { String scriptContent -> 
            return scriptContent;
        }
    ]

    def windowsAdapter = [
        // In Windows batch scripts, surrounding environment variables with quotes of any kind break
        // their values in Python because the surrounding quotes become part of the variable value.
        quoteStr: "",
        commentStr: "::",
        fileSeparator: "\\\\",
        getEnvironmentVariableString: { String name, String value ->
            return 'SET ' + name + '=' + value
        },
        getHookScriptFile: { File repository, String hook ->
            return new File(new File(repository, "hooks"), hook + ".bat")
        },
        doScriptModifications: { StringBuilder sb ->
            sb.insert(0, "@ECHO OFF\n\n");
        },
        replaceArguments: { String scriptContent -> 
            return scriptContent.replaceAll('"\\$(\\d+)"', '%$1')
        }
    ]
}
