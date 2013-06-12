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

import java.io.File;

import grails.util.GrailsUtil;

import com.collabnet.svnedge.CantBindPortException 
import com.collabnet.svnedge.domain.Server 
import com.collabnet.svnedge.domain.User 
import com.collabnet.svnedge.domain.integration.CtfServer 
import com.collabnet.svnedge.util.ConfigUtil;

enum Command {START, STOP, GRACEFUL}

class LifecycleService {

    boolean transactional = true
    
    def grailsApplication
    def commandLineService    
    def serverConfService
    def networkingService
    def operatingSystemService

    int MAX_SERVER_WAIT_TIME = 60

        // these variables cache the result of the corresponding system checks
    // call "clearCachedResults" to enable re-checking
    private Boolean isSudoCache
    private Boolean isHttpdBindSuidCache
    private Boolean isSolarisDefaultPortCache

    /**
     * If the server has failed to restart since the last attempt.
     */
    def restartFailed

    /**
     * @return if the server has failed to restart
     */
    def hasFailedToRestart() {
        return restartFailed
    }

    /**
     * @return if the server has failed restarting since last attempt.
     */
    def serverHasFailedToRestart() {
        restartFailed = true
    }

    /**
     * @return if the server has successfully restarted since last attempt.
     */
    def serverHasSuccessfullyRestarted() {
        restartFailed = false
    }

    private boolean isWindows() {
        return operatingSystemService.isWindows()
    }

    private boolean isSolaris() {
        return operatingSystemService.isSolaris()
    }

    def bootstrapServer(config) {

        def server = Server.getServer()
        if (!server) {
            def bootstrapParam = this.getServerLifecycleBootstrapParams()
            int port = config.svnedge.defaultHighPort
            int authHelperPort = config.svnedge.defaultApacheAuthHelperPort

            File repoParentFile = new File(
                config.svnedge.svn.repositoriesParentPath)
            def repoParentDir = repoParentFile.absolutePath
            server = new Server(
                hostname: bootstrapParam.hostname,
                port: port,
                authHelperPort: authHelperPort,
                repoParentDir: repoParentDir,
                adminName: "Nobody",
                adminEmail: "devnull@collab.net",
                allowAnonymousReadAccess: false,
                forceUsernameCase: false,
                ldapServerHost: "",
                ldapServerPort: 389,
                ldapAuthBasedn: "",
                ldapAuthBinddn: "",
                ldapAuthBindPassword: "",
                fileLoginEnabled: true,
                ldapEnabled: false,
                ldapEnabledConsole: false,
                ldapLoginAttribute: "",
                ldapSearchScope: "sub",
                ldapFilter: "",
                ldapSecurityLevel: "NONE",
                pruneLogsOlderThan: 0,
                ldapServerCertVerificationNeeded: true,
                hasSoftwareUpdates: false,
                svnBasePath: "/svn")
            
            File dumpDir = new File(server.dumpDir)
            if (!dumpDir.exists()) {
                dumpDir.mkdir()
            }

            if (!server.validate()) {
                server.errors.allErrors.each { log.error(it) }
            }

            if (bootstrapParam.isDefaultPortAllowed && 
                    !isPortBoundByAnotherService(80)) {
                server.port = 80
            }

            server.save()
        }

        if (GrailsUtil.environment == "test") {
            new CtfServer(
                baseUrl: config.svnedge.ctfMaster.ssl ? "https://" : 
                    "http://" + config.svnedge.ctfMaster.domainName,
                mySystemId: config.svnedge.ctfMaster.systemId).save(flush: true)
        }

        def mkdirs = {
            if (!it.exists()) {
                it.mkdirs()
            }
        }
    
        def dataDirPath = ConfigUtil.dataDirPath()
        // make run and logs directories if not existing
        mkdirs(new File(dataDirPath, "logs"))
        File runFile = new File(dataDirPath, "run")
        mkdirs(runFile)    
        return server
    }

    /**
     * Confirms whether the svn server is running.
     */
    boolean isStarted() {
        File f =  new File(new File(ConfigUtil.dataDirPath(), "run"), 
            "httpd.pid")

        log.debug("Checking isStarted  Path=" + f.getPath() + 
                  " exists? " + f.exists())
        if (f.exists()) {
            if (f.canRead()) {
                if (operatingSystemService.getProcessExists(f.text.trim())) {
                    return true
                }
                else {
                    f.delete()
                    return false
                }
            } else {
                return true
            }
        }
        else {
            return false
        }
    }

     /**
      * gracefully restart the svn server
      */
    def gracefulRestartServer() throws CantBindPortException {
        if (!isStarted()) {
            return -1
        }
        return startOrStopServer(Command.GRACEFUL)
    }

    /**
     * Starts the svn server
     */
    def startServer() throws CantBindPortException {
        if (isStarted()) {
            return -1
        }
        Server server = getServer()
        if (server.useSsl) 
            createSSLServerCert()
        return startOrStopServer(Command.START)
    }

    /**
     * Starts or gracefully restarts the server
     * @return
     */
    def restartServer() throws CantBindPortException {
        def result = -1;
        if (isStarted()) {
            result = gracefulRestartServer()
        } else {
            result = startServer()
        }
        return result
    }
    
    private def createSSLServerCert() {
        Server server = getServer()
        File f1 = new File(ConfigUtil.confDirPath(), "server.key")
        File f2 = new File(ConfigUtil.confDirPath(), "server.crt")
        def keyFilePath = f1.getAbsolutePath()
        def exitStatus
        boolean newkey = false

        // if no cert/key yet exist, copy the shipped cert and
        // key to the apache location
        if (!f2.exists()) {
            copyShippedCerts(f2, f1)
        }

        // if they still don't exist, generate
        if (!f1.exists()) {
            def cmd1 = [ConfigUtil.opensslPath(), 
                        "genrsa", "-out", keyFilePath, "1024"]
            exitStatus = commandLineService.executeWithStatus(
                cmd1.toArray(new String[0]), null)
            if (exitStatus != 0) {
                log.warn("openssl genrsa key creation failed with code=" + exitStatus)
            }
            newkey = true 
        }

        if (!f2.exists() || newkey) {
            def certreqinput = """--
SomeState
SomeCity
SomeOrganization
SomeOrganizationalUnit
${server.hostname}
root@${server.hostname}
"""
            File certFile = new File(ConfigUtil.confDirPath(), "server.crt")
            def certFilePath = certFile.getAbsolutePath()
            def opensslcnfPath = new File(ConfigUtil.appHome(), 
                "data/certs/openssl.cnf").absolutePath

            def cmd2 = [ConfigUtil.opensslPath(), "req", "-new", "-key", 
                        keyFilePath, "-config", opensslcnfPath, "-x509", 
                        "-days", "365", "-out", certFilePath]
            exitStatus = commandLineService.executeWithStatus(
                              cmd2.toArray(new String[0]), null, certreqinput)
            if (exitStatus != 0)
                log.warn("openssl req failed with code=" + exitStatus)
        }
    }


    /**
     * Copies shipped cert and key from data/certs to the given target Files
     * @param certTarget the File to which we are copying "svnedge.crt"
     * @param keyTarget the File to which we are copying "svnedge.key"
     */
    private void copyShippedCerts(File certTarget, File keyTarget) {
        def certDirPath = new File(ConfigUtil.dataDirPath(), 
                                   "certs").canonicalPath
        File cert = new File(certDirPath, "svnedge.crt")
        File key  = new File(certDirPath, "svnedge.key")

        // we need both source files
        if (!cert.exists() || !key.exists()) {
            return
        }

        // only copy if neither target is in the way
        if (!certTarget.exists() && !keyTarget.exists()) {
            certTarget << cert.text
            keyTarget << key.text
        }
    }

    private Map<String, String> createHttpdEnv() {
        Map<String, String> env = new HashMap<String, String>(3)
        if (!isWindows()) {
    	    //Windows installation drops the mod_python.viewvc inside
	        //$PYTHONHOME/lib/site-packages so locating the
	        //mod_python.apache is easy as long as $PYTHONHOME is set.
    	    //In linux we do *not* ship python and do not install
	        //anything persistent in host's PYTHONHOME, so we set
	        //PYTHONPATH to locate mod_python.apache
            String modPythonLibDir = new File(ConfigUtil.appHome(), "lib")
                .absolutePath
            env.put("PYTHONPATH", modPythonLibDir)
        }

        if (!isWindows() && isHttpdBindSuid()) {
            env.put("LD_PRELOAD", ConfigUtil.libHttpdBindPath())
            String path = System.getenv("PATH")
            if (null != path && path.length() > 0) {
                env.put("PATH", ConfigUtil.httpdBindPath() + ':' + path)
            } else {
                env.put("PATH", ConfigUtil.httpdBindPath())
            }
        }
        env
    }

    private def createHttpdCmd() {
        def cmd = [ConfigUtil.httpdPath(),
            "-f", ConfigUtil.confDirPath() + File.separator + "httpd.conf"]
        if (isWindows()) {
            cmd.addAll(["-n", ConfigUtil.serviceName()])
        }
        cmd
    }

    private def startOrStopServer(Command command) throws CantBindPortException{
        boolean isStart = false
        String startOrStop = "stop"
        if (command == Command.START) {
            startOrStop = "start"
            isStart = true
            serverConfService.writeConfigFiles()
        } else if (command == Command.GRACEFUL) {
            if (isWindows()) {
                startOrStop = "restart"
            } else {
                startOrStop = "graceful"
            }
            serverConfService.writeConfigFiles()
            isStart = true
        }

        def cmd = createHttpdCmd()
        cmd.addAll([ "-k", startOrStop])
        if (getServer().port < 1024) {
            if (!isWindows() && !isHttpdBindSuid() && isSudo()) {
                cmd.add(0, "sudo")
                cmd.add(1, "-S")

            } else if (isSolarisDefaultPortAllowed() && !isHttpdBindSuid()) {
                def httpdCmd = cmd.collect({it.replace(" ", "\\ ")}).join(" ")
                cmd = ["ppriv", "-s", "EIP+net_privaddr", "-e", 
                       "sh", "-c", httpdCmd]
            }
        }

        def commandResponse = commandLineService.execute(cmd.toArray(
                new String[0]), createHttpdEnv())
        def exitStatus = Integer.parseInt(commandResponse[0])
        def output = commandResponse[1]
        def error = commandResponse[2]
        
        // PID is old and needs to be removed.
        if (output?.startsWith("httpd") && output?.contains("pid") && 
                output?.contains("not running")) {

            ConfigUtil.httpdPidFile().delete()

            commandResponse = commandLineService.execute(cmd.toArray(
                new String[0]), createHttpdEnv())
            exitStatus = Integer.parseInt(commandResponse[0])
            output = commandResponse[1]
            error = commandResponse[2]
        }

        // Look for port-bind issue
        if (exitStatus == 1 && error?.contains("could not bind")) {
                clearCachedResults()
                serverHasFailedToRestart()
                throw new CantBindPortException(server.port)

        } else if (exitStatus == 0) {
            int count = 0
            while (count < MAX_SERVER_WAIT_TIME && (isStart != isStarted())) {
                Thread.sleep(1000)
                count++
            }
            if (count >= MAX_SERVER_WAIT_TIME) {
                log.warn("Server " + startOrStop + " attempt failed. " +
                         "Process returned " +
                         "but httpd.pid file did not appear within " +
                         MAX_SERVER_WAIT_TIME + " seconds.")
                serverHasFailedToRestart()

            } else {
                log.debug("Server " + startOrStop + " attempt successful" +
                          " using " + ConfigUtil.httpdPath())
                serverHasSuccessfullyRestarted()
            }
        } else {
            clearCachedResults()
            log.warn("Server " + startOrStop + 
                     " attempt failed with code=" + exitStatus)
            log.warn("Output: " + commandResponse[1])
            serverHasFailedToRestart()
        }
        return exitStatus
    }
    
    /**
     * Stops the svn server
     */
    def stopServer() {
        if (!isStarted()) {
            return -1
        }
        startOrStopServer(Command.STOP)
    }

    
    /**
     * Adds user to htpasswd with given password;
     */
    def setSvnAuth(User user, password) {
        def exitStatus
        exitStatus = writeHtpasswd(user.username, password)
        return exitStatus
    }

    /**
     * Removes user from htpasswd
     */
    def removeSvnAuth(User user) {
        def exitStatus
        File authFile = new File(ConfigUtil.confDirPath(), "svn_auth_file")
        exitStatus = commandLineService
            .executeWithStatus(ConfigUtil.htpasswdPath(), '-D',
            authFile.absolutePath, user.username)
        return exitStatus
    }
    
    private def writeHtpasswd(String username, String password) {
        File authFile = new File(ConfigUtil.confDirPath(), "svn_auth_file")
        def options = authFile.exists() ? "-b" : "-cb"
        def exitStatus = commandLineService
            .executeWithStatusQuietly(ConfigUtil.htpasswdPath(),
            options, authFile.absolutePath, username, password)
        return exitStatus
    }
    
    /**
     * Retrieves the single instance of Server
     */
    Server getServer() {
        Server server = Server.getServer()
        if (!server)
            return server
        File repoParentFile = new File(server.repoParentDir)
        def mkdirs = {
            if (!it.exists()) {
                it.mkdirs()
            }
        }
        mkdirs(repoParentFile)
        return server
    }

    /**
     * Checks whether the management console will be able to start
     * the httpd server on a privileged port.
     */
    boolean isDefaultPortAllowed() {
        return isWindows() || isHttpdBindSuid() ||
            isSolarisDefaultPortAllowed() || isSudo()
    }
    
    private boolean isSudo() {
        
        if (isSudoCache == null) {
            isSudoCache = checkSudo()
        }
        return isSudoCache.booleanValue()
    }
    
        
    private boolean isHttpdBindSuid() {
        
        if (isHttpdBindSuidCache == null) {
            isHttpdBindSuidCache = checkHttpdBindSuid()
        }
        return isHttpdBindSuidCache.booleanValue() 
    }
    
    
    public void clearCachedResults() {
        
        isSudoCache = null
        isHttpdBindSuidCache = null
        isSolarisDefaultPortCache = null        
    }

    private boolean checkSudo() {
        if (isWindows()) {
            return false;
        }

        // validate sudo NOPASSWD is available for executing httpd
        try {
            // clear any earlier sudo timestamp
            def cmd = ["sudo", "-K"]
            commandLineService.execute(cmd.toArray(new String[2]))

            cmd = createHttpdCmd()
            // Checks config file, which if corrupt could lead
            // to false negative, but httpd won't start anyway, so
            // there will be worse problems
            cmd.add("-t")
            cmd.add(0, "sudo")
            cmd.add(1, "-S")
            return !commandLineService.testForPassword(
                    cmd.toArray(new String[0]), createHttpdEnv())
        }
        catch (Exception e) {
            log.warn "Unable to execute sudo (not installed?): ${e.getMessage()}"
            return false
        }
    }
    
    private boolean checkHttpdBindSuid() {
        if (isWindows()) {
            return false;
        }
        boolean result = false
        if (new File(ConfigUtil.httpdBindPath()).exists()) {
            String[] fileProps = commandLineService.executeWithOutput(
                "ls", "-l", ConfigUtil.exeHttpdBindPath())
                .replaceAll(" +", " ").split(" ")
            if (fileProps && fileProps.length > 2) {
                String owner = fileProps[2]
                result = (owner == "root")
                if (result) {
                    String perms = fileProps[0]
                    result = (perms.charAt(3) == 's')
                }
            }
        } else {
            log.debug(ConfigUtil.httpdBindPath() + " does not exist") 
        }
        result
    }

    private boolean isSolarisDefaultPortAllowed() {
        
        if (isSolarisDefaultPortCache == null) {
            isSolarisDefaultPortCache = checkSolarisNetPrivAddr()
        }
        return isSolarisDefaultPortCache
    }


    /**
     * Checks that the svnedge user is allowed to start httpd 
     * on privileged ports.  
     */
    private boolean checkSolarisNetPrivAddr() {
        if (!isSolaris()) {
            return false
        }

        String[] cmd = ["ppriv", "-s", "EIP+net_privaddr", "-e", 
                        "sh", "-c", "echo foo"]
        def commandResponse = commandLineService.execute(cmd)
        def exitStatus = Integer.parseInt(commandResponse[0])
        def output = commandResponse[1]
        boolean result = (exitStatus == 0 && output == "foo\n")
        log.debug("checkSolarisNetPrivAddr()=" + result + "; exitStatus=" + 
                  exitStatus + " output='" + output + "'")
        return result
    }
    
    /**
     * @return the map with the values for the bootstrap.
     */
    def getServerLifecycleBootstrapParams() {
        return [hostname: networkingService.hostname,
            isDefaultPortAllowed: this.isDefaultPortAllowed()]
    }

    private def httpdPid() {
        def pid = 0
        File f = ConfigUtil.httpdPidFile()
        if (f.exists()) {
            f.eachLine { line -> pid = line }
        }
        return pid
    }
    
    boolean isPortBoundByAnotherService(int port) {
        Server server = Server.getServer()
        boolean b = (port != server?.port || !isStarted())
        if (b) {
            try {
                Socket s = new Socket("localhost", port)
                s.close()
            } catch (ConnectException e) {
                b = false
            } catch (Exception e) {
                log.debug "Unexpected exception when checking port availability", e
                b = false
            }
        }
        return b
    }
}
