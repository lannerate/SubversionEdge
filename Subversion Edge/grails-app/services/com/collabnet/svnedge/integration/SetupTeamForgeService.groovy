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
package com.collabnet.svnedge.integration

import java.net.URL;
import java.net.URLEncoder;
import java.net.HttpURLConnection;
import java.net.UnknownHostException
import java.net.NoRouteToHostException
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import javax.net.ssl.SSLHandshakeException

import grails.util.GrailsUtil

import com.collabnet.svnedge.CantBindPortException 
import com.collabnet.svnedge.util.ConfigUtil;
import com.collabnet.svnedge.console.AbstractSvnEdgeService
import com.collabnet.svnedge.domain.Repository 
import com.collabnet.svnedge.domain.Server 
import com.collabnet.svnedge.domain.ServerMode 
import com.collabnet.svnedge.domain.User 
import com.collabnet.svnedge.domain.integration.CtfServer
import com.collabnet.svnedge.domain.integration.ReplicaConfiguration

import javax.net.ssl.SSLHandshakeException

class SetupTeamForgeService extends AbstractSvnEdgeService {

    static final int CTF_REPO_PATH_LIMIT = 128

    def lifecycleService
    def serverConfService
    def commandLineService
    def operatingSystemService
    def authenticationManager
    def csvnAuthenticationProvider
    def ctfAuthenticationProvider
    def ctfRemoteClientService
    def securityService
    def svnRepoService
    def discoveryService

    /**
     * The integration properties file.
     */
    private static def TF_PROPERTIES_FILE = "data/conf/teamforge.properties"
    /**
     * The team forge integration path.
     */
    private static def TF_PATH_VAR = "sf.sourceForgePropertiesPath"

    /**
     * Path to parent directory of viewvc.cgi
     */
    private static final String CGI_PATH_VAR = "viewvc.cgi.path"

    /**
     * Path to application home, used by csvn's viewvc.cgi
     */
    private static final String APP_HOME_VAR = "csvn.appHome"

    boolean transactional = true

    /**
    * Sets system properties used to configure the integration webapp
    * and scripts
    *
    * @param appHome is the application home directory
    */
   def bootStrap = { appHome ->
       def tfPropertiesFile = new File(appHome, TF_PROPERTIES_FILE);
       System.properties[TF_PATH_VAR] = tfPropertiesFile.absolutePath
       log.debug("Updating system property '${TF_PATH_VAR}'='" +
                 "${System.properties[TF_PATH_VAR]}'")
       System.properties[CGI_PATH_VAR] =
           new File(appHome, "bin/cgi-bin").absolutePath
       log.debug("Updating system property '${CGI_PATH_VAR}'='" +
                 "${System.properties[CGI_PATH_VAR]}'")
       System.properties[APP_HOME_VAR] = appHome
       log.debug("Setting system property '${APP_HOME_VAR}'='" +
                 "${System.properties[APP_HOME_VAR]}'")

       def server = Server.getServer()
       if (server && server.mode != ServerMode.MANAGED) {
            convertIfPropertiesExist(appHome, server)
       }
        updateIntegrationScripts(appHome, ServerMode.MANAGED)
        updateTeamForgeProperties(appHome)
   }
   
   private void convertIfPropertiesExist(appHome, server) {
       File ctfInitializer = new File(ConfigUtil.confDirPath(), 
               'init_as_integration_server.properties')
       if (ctfInitializer.exists()) {
           log.info "Initializing as a CTF integration server"
           Properties p = new Properties()
           ctfInitializer.withReader { p.load(it) }
           CtfConversionBean ctf = new CtfConversionBean()
           ctf.ctfURL = p.getProperty('ctf_url')
           ctf.serverKey = p.getProperty('shared_secret_key')
           ctf.consolePort = p.getProperty('edge_console_port', '8080') as int
           String s = p.getProperty('external_system_id')
           if (s) {
               ctf.exSystemId = s
           }
           s = p.getProperty('ctf_username')
           if (s) {
               ctf.ctfUsername = s
               ctf.ctfPassword = p.getProperty('ctf_password')
           }
           boolean isServerChanges = false
           p.keySet().each { key ->
               log.debug "init_as_int_server key=${key}"
               if (key.startsWith('server_')) {
                   String fieldName = key.substring(7)
                   Class type = server.getClass()
                           .getDeclaredField(fieldName).getType()
                   String val = p.getProperty(key)
                   server."${fieldName}" = 
                           (Boolean.class == type || Boolean.TYPE == type) ? 
                           val.toBoolean() : val.asType(type)
                   def val2 = server."${fieldName}"
                   log.debug "    set server.${fieldName}=${val2}"
                   isServerChanges = true
               }
           }
           if (isServerChanges) {
               server.save()
           }
           
           if (!ctf.exSystemId) {
               File repoParent = new File(server.repoParentDir)
               File idFile = new File(repoParent, ".scm.properties")
               if (idFile.exists()) {
                   Properties pp = new Properties()
                   idFile.withReader { pp.load(it) }
                   ctf.exSystemId = pp.getProperty('external_system_id')
               }
           }
           
           if (ctf.ctfUsername) {
               runAsync {
                   URL appUrl = new URL(server.useSslConsole ? 'https' : 'http',
                            'localhost', ctf.consolePort, '/csvn')
                   def isContinue = 0
                   while (isContinue++ < 60) {
                       try {
                           def con = appUrl.openConnection()
                           if (con.responseCode == con.HTTP_MOVED_TEMP ||
                                   con.responseCode == con.HTTP_OK) {
                               break
                           }
                       } catch (Exception e) {
                           // keep trying
                       }
                       Thread.sleep(5000)
                   }
                   if (isContinue < 60) {
                       try {
                           doConvertIfPropertiesExist(ctf, server)
                       } catch (Exception e) {
                           log.error "Unable to convert to TF mode", e
                       }
                   } else {
                       log.error "Timed out after 5 minutes waiting to be " +
                               "ready for CTF integration server conversion"
                   }
               }
           } else {
               doConvertIfPropertiesExist(ctf, server)
           }
       }
   }
   
   private void doConvertIfPropertiesExist(ctf, server) {
        def result = convert(ctf)
        if (result.exception) {
            throw result.exception
        }
        result.errors.each { log.error(it) }
        result.warnings.each { log.warn(it) }

        if (ctf.exSystemId) {
            File repoParent = new File(server.repoParentDir)
            File idFile = new File(repoParent, ".scm.properties")
            if (!idFile.exists()) {
                Properties pout = new Properties()
                pout['external_system_id'] = ctf.exSystemId
                idFile.withWriter { pout.store(it, 'Integration server config') }
            }
        }
    }
   
   public void updateIntegrationScripts(String appHome, ServerMode mode) {
       def server = Server.getServer()
       // In ctf mode, confirm that the integration scripts are up-to-date
       if (server && server.mode == mode) {
           File libDir = new File(appHome, "lib")
           File zipFile = new File(libDir, INTEGRATION_SCRIPTS_ZIP)
           File integrationDir = new File(libDir, "integration")
           if (zipFile.exists() && (!integrationDir.exists() ||
               zipFile.lastModified() > integrationDir.lastModified())) {
               log.info("Updating CTF integration scripts.")
               unpackIntegrationScripts()
           }
       }
   }

   private void updateTeamForgeProperties(appHome) {
       File confDir = new File(appHome, "data/conf")
       File tfProps = new File(confDir, "teamforge.properties.dist")
       if (tfProps.exists()) {
           boolean isChanged = false
           String text = tfProps.text
           String prop = "sfmain.integration.executables.python"
           if (text.indexOf(prop) < 0) {
               int pos = text.indexOf("# Integration listener keys")
               if (pos > 0) {
                   text = text.substring(0, pos) + 
                       "\n" + prop + "=python\n" +
                       text.substring(pos)
               } else {
                   text += "\n" + prop + "=python\n"
               }
               isChanged = true
           }

           prop = "scm.python.path"
           String propAndNewValue = prop + "=\${python.path}"
           if (text.indexOf(prop) < 0) {
               int pos = text.indexOf("# Integration server constants")
               if (pos >= 0) {
                   text = text.substring(0, pos) + 
                       "\n" + propAndNewValue + "\n" +
                       text.substring(pos)
               } else {
                   text += "\n" + propAndNewValue + "\n"
               }
               isChanged = true

           } else {
               String propAndOldValue = prop + 
                   "=\${lib.dir}:\${lib.dir}/svn-python" 
               int pos = text.indexOf(propAndOldValue)
               if (pos >= 0) {
                   text = text.substring(0, pos) + propAndNewValue +
                       text.substring(pos + propAndOldValue.length())
                   isChanged = true
               }
           }

           if (isChanged) {
               tfProps.write(text)
               serverConfService.writeConfigFiles()
           }
       }
   }
   
   private String getWebSessionId(conversionData) {
        if (!conversionData.webSessionId) {
            def ids = loginCtfWebapp(conversionData)
            conversionData.webSessionId = ids.jsessionid
            conversionData.userSessionId = ids.usessionid
        }
        conversionData.webSessionId
    }
    
    private String getUserSessionId(conversionData) {
        if (!conversionData.userSessionId) {
            def ids = loginCtfWebapp(conversionData)
            conversionData.webSessionId = ids.jsessionid
            conversionData.userSessionId = ids.usessionid
        }
        conversionData.userSessionId
    }

    /**
     * @param conversionData contains the conversion data from wizard.
     * @param projectName is the name of the project to be verified.
     * @return if the given project name exists in the TeamForge server given
     * in the conversionData.
     * @throws CtfSessionExpiredException in case the session expires.
     */
    def String projectExists(conversionData, projectName) 
        throws RemoteMasterException {

        return ctfRemoteClientService.projectExists(conversionData.ctfURL,
            conversionData.soapSessionId, projectName, projectUrl(projectName),
            conversionData.userLocale)
    }

    /**
     * @param conversionData is the wizard bean.
     * @return The list of remote TeamForge projects that are named with
     * the same name as the local repositories on CSVN.
     * @throws RemoteMasterException in case the call to the remote TeamForge
     * SOAP API to return the list of projects fail.
     */
    def List<String> getProjectsWhichMatchRepoNames(conversionData) 
        throws RemoteMasterException {

        def ctfProjectsList = ctfRemoteClientService.getProjectList(
            conversionData.ctfURL, conversionData.soapSessionId, 
            conversionData.userLocale)
        def projects = new HashSet(ctfProjectsList.dataRows.toList().collect {
            it.title })
        def repos = Repository.list(sort:"name")
        List<String> repoNames = new ArrayList<String>(repos.size())
        for (repo in repos) {
            if (projects.contains(repo.name)) {
                repoNames << repo.name
            }
        }
        repoNames
    }

    private static final def CTF_VALID_REPO_NAME = ~/[a-z][_a-z0-9\-]*/
    private static final def FIXABLE_REPO_NAME = ~/[_A-Za-z0-9]*/
    private static final def UPPER_CHAR = ~/[A-Z]/

    /**
     * CTF only allows repo directories which start with a letter and use
     * letters, numbers, and underscore in the name.  This method sets
     * several flags or lists in the wizard bean related to invalid repos
     */
    def validateRepos() {
        def repoParent = Server.getServer().repoParentDir
        def repos = Repository.list(sort:'name')
        def repoNames = new HashSet(repos.collect({ it.name }))
        def unfixableRepoNames = []
        def duplicatedReposIgnoringCase = []
        def containsUpperCaseRepos = false
        def containsReposWithInvalidFirstChar = false
        def longRepoPath = null
        def permissionsNotOk = []
        for (repo in repos) {
            def repoName = repo.name
            if (!repo.validateName(CTF_VALID_REPO_NAME)) {
                if (repoName.matches(FIXABLE_REPO_NAME)) {
                    if (repoName.find(UPPER_CHAR)) {
                        containsUpperCaseRepos = true
                        if (repoNames.contains(repoName.toLowerCase())) {
                            duplicatedReposIgnoringCase << repoName
                        }
                    }     
                    char firstChar = repoName.charAt(0)
                    // uppercase characters are handled above, so don't treat
                    // them as an invalid first character
                    if (firstChar < ('A' as char) || 
                        firstChar > ('z' as char)) {

                        containsReposWithInvalidFirstChar = true
                    }
                } else {
                    unfixableRepoNames << repoName
                }
            }
            if (!longRepoPath) { 
                def repoPath = new File(repoParent, repoName).absolutePath
                if (repoPath.length() > CTF_REPO_PATH_LIMIT) {
                    longRepoPath = repoPath                    
                }                
            }
            if (!repo.permissionsOk) {
                permissionsNotOk << repoName
            }
        }
        return ['unfixableRepoNames':unfixableRepoNames, 
                'duplicatedReposIgnoringCase':duplicatedReposIgnoringCase,
                'containsUpperCaseRepos':containsUpperCaseRepos, 
                'containsReposWithInvalidFirstChar': 
                containsReposWithInvalidFirstChar,
                'longRepoPath':longRepoPath,
                'permissionsNotOk':permissionsNotOk]
    }

    String validateRepoPrefix(prefix) {
        String conflict = null
        def repoNames = Repository.list().collect { it.name.toLowerCase() }
        HashSet nameSet = new HashSet(repoNames)
        for (repo in repoNames) {
            String tmp = prefix + repo.toLowerCase()
            if (nameSet.contains(tmp)) {
                conflict = tmp
                break
            }
        }
        conflict
    }

    /**
     * @param conversionData is the wizard bean.
     * @return the list of users from CSVN compared to the remote TeamForge
     * server's. 
     * @throws RemoteMasterException in case the call to the TeamForge server
     * to retrieve users fail.
     */
    List<List<String>> getCsvnUsersComparedToCtfUsers(conversionData) {
        def ctfRemoteUsers = ctfRemoteClientService.getUserList(
            conversionData.ctfURL, conversionData.soapSessionId, 
            conversionData.userLocale)
        def ctfUsers = new HashSet(ctfRemoteUsers.toList().collect { 
            it.userName })
        // get non-ldap users for CTF import
        def csvnUsers = User.list(sort:"username").findAll{ u -> 
            !u.isLdapUser()
        }
        List<String> ctfUsernames = new ArrayList<String>(csvnUsers.size())
        List<String> csvnOnlyUsernames = new ArrayList<String>(csvnUsers.size())
        for (user in csvnUsers) {
            if (ctfUsers.contains(user.username)) {
                ctfUsernames << user.username
            } else {
                csvnOnlyUsernames << user.username
            }
        }
        [ctfUsernames, csvnOnlyUsernames]
    }
    
    /**
     * Will try to connect to the CTF instance and if successful will fill in the version information
     * for the conversionData object.  If unable to connect, conversionData.errorMessage may contain
     * useful information
     */
    boolean confirmConnection(conversionData) throws CtfAuthenticationException, RemoteMasterException,
           UnknownHostException, NoRouteToHostException, MalformedURLException,
           SSLHandshakeException, CtfConnectionException {
        def ctfSoap = ctfRemoteClientService.cnSoap(conversionData.ctfURL)
        conversionData.soapSessionId = ctfRemoteClientService.login(
            conversionData.ctfURL, conversionData.ctfUsername,
            conversionData.ctfPassword, conversionData.userLocale)
        conversionData.apiVersion = ctfSoap.getApiVersion()
        conversionData.appVersion = ctfSoap.getVersion(conversionData.soapSessionId)
        // Calling this here because we may not keep the user's credentials
        // after this point and we'll need the web session id to revert
        getWebSessionId(conversionData)
    }
    
    private loginCtfWebapp(conversionData) {
        return ctfRemoteClientService.getCtfSessionIds(conversionData.ctfURL,
            conversionData.ctfUsername, conversionData.ctfPassword)
    }

    /**
     * Register this svnedge instance as an integration server on TeamForge
     * @param conversionData is the data to be used for the conversion.
     * @return the systemId of the registration.
     * @throws CtfAuthenticationException if the authentication fails with 
     * TeamForge.
     * @throws RemoteMasterException if any error occurs during the method 
     * call.
     */
    protected def registerIntegrationServer(conversionData) {

        def server = Server.getServer()

        def adapterType = "Subversion"
        def appServerPort = conversionData.consolePort
        def title = "CollabNet Subversion Edge (${server.hostname}:" +
                "${appServerPort})"
        def description = "This is a CollabNet Subversion Edge " +
                "server in managed mode from ${server.hostname}:" +
                "${appServerPort}."

        // SSL for integration and viewvc apps is based on Jetty's
        // configuration, not Apache/SVN (aka "server")
        def useSSL = conversionData.consoleSsl ? "true" : "false"
        def repositoryPath = operatingSystemService.isWindows() ? 
            "/windows-scmroot" : server.repoParentDir

        // CTF assumes RepositoryBaseUrl does not include trailing slash --
        // removing from submitted value if present
        String svnUrl = server.svnURL();
        if (svnUrl.endsWith("/")) {
            svnUrl =  svnUrl.substring(0, svnUrl.length() - 1) 
        }
        def props = [ RequireApproval: "false", HostName: server.hostname, 
            HostPort: "${appServerPort}", HostSSL: useSSL,
            isCSVN: "true", ScmViewerUrl: getViewVcUrl(server, server.useSsl), 
            RepositoryBaseUrl: svnUrl,  RepositoryRootPath: repositoryPath] 

        def systemId = ctfRemoteClientService.addExternalSystem(
            conversionData.ctfURL, conversionData.soapSessionId, adapterType, title, 
            description, props, conversionData.userLocale)

        return systemId
    }

    protected def getViewVcUrlPrefix(server, useSsl) {
        def port = useSsl ? 
            ((server.port == 443) ? "" : ":" + server.port) :
            ((server.port == 80) ? "" : ":" + server.port)
        (useSsl ? "https" : "http") +
                "://" + server.hostname + port
    }
    
    protected def getViewVcUrl(server, useSsl) {
        getViewVcUrlPrefix(server, useSsl) + "/viewvc"
    }

    protected void fixInvalidRepos(conversionData) {
        def repos = Repository.list()
        for (repoDomain in repos) {
            def repo = repoDomain.name
            if (!repoDomain.validateName(CTF_VALID_REPO_NAME)) {
                if (repo.matches(FIXABLE_REPO_NAME)) {
                    def newRepo = repo
                    if (conversionData.lowercaseRepos) {
                        newRepo = newRepo.toLowerCase()
                    }     
                    char firstChar = newRepo.charAt(0)
                    if (conversionData.repoPrefix && 
                        (firstChar < ('a' as char) || 
                        firstChar > ('z' as char))) {
                        newRepo = conversionData.repoPrefix + newRepo
                    }
                    def newRepoDomain = new Repository(name:newRepo)
                    newRepoDomain.discard()
                    if (newRepo == repo || 
                        !newRepoDomain.validateName(CTF_VALID_REPO_NAME)) {
                        def msg = getMessage(
                            "setupTeamForge.integration.ctfRepoName.invalid",
                            conversionData.userLocale)
                        throw new Exception(msg + ": " + newRepo)
                    } else {
                        def parentDir = Server.getServer().repoParentDir
                        File f1 = new File(parentDir, repo)
                        File f2 = new File(parentDir, newRepo)
                        if (f2.exists()) {
                            throw new Exception(getMessage(
                                "setupTeamForge.integration.ctfRepoName" +
                                ".alreadyExists", [repo, newRepo], 
                                conversionData.userLocale))
                        } else if (!f1.renameTo(f2)) {
                            throw new Exception(getMessage("setupTeamForge." +
                                "integration.ctfRepoName.unexpectedError", 
                                [repo, newRepo], conversionData.userLocale))
                        } else {
                            repoDomain.name = newRepo
                            repoDomain.save()
                        }
                    }
                } else {
                    throw new Exception(getMessage(
                        "setupTeamForge.integration.ctfRepoName.cantBeFixed",
                        [repo], conversionData.userLocale))
                }
            } 
        }
    }

    protected void addReposToProjects(conversionData) {
        if (conversionData.isProjectPerRepo) {
            throw new UnsupportedOperationException("Not implemented")
        } else {
            addReposToSingleProject(conversionData)
        }
    }

    String projectUrl(String projectName) {
        return projectName.toLowerCase().replace(' ', '_').replace('.', '_')
    }

    private void addReposToSingleProject(conversionData) 
        throws RemoteMasterException{

        def projectName = conversionData.ctfProject
        def projectPath = projectUrl(projectName)
        def projects = ctfRemoteClientService.getProjectList(
            conversionData.ctfURL, conversionData.soapSessionId, 
            conversionData.userLocale)
        String projectId = null
        for (p in projects) {
            if (projectName.toLowerCase() == p.title.toLowerCase() || 
                projectPath == p.path.substring(9)) {
                projectId = p.id
                conversionData.ctfProjectPath = p.path
                break
            }
        }
        
        if (!projectId) {
            log.info("Creating new project '" + projectName + 
                "' in CTF to hold existing repositories")
            projectId = ctfRemoteClientService.createProject(
                conversionData.ctfURL, conversionData.soapSessionId, 
                projectPath, projectName, getMessage(
                    "setupTeamForge.integration.container.existed", 
                    conversionData.userLocale))
        }
        
        def systemId = conversionData.exSystemId
        def scmSoap = ctfRemoteClientService.makeScmSoap(conversionData.ctfURL)
        boolean idRequiredOnCommit = false
        boolean hideMonitoringDetails = false
        def desc = getMessage("setupTeamForge.integration.addedFromExistingRepo"
            , ["CollabNet Subversion Edge"], conversionData.userLocale)
        def repos = Repository.list()
        if (log.isDebugEnabled()) {
            log.debug "repos=" + repos
        }
        String repoParentDir = Server.getServer().repoParentDir
        for (repo in repos) {
            File repoDir = new File(repoParentDir, repo.name)
            File hooksDir = new File(repoDir, "hooks")
            archiveCurrentHooks(hooksDir)            

            ctfRemoteClientService.createRepository(conversionData.ctfURL, 
                conversionData.soapSessionId, projectId, systemId, 
                repo.name, desc, idRequiredOnCommit, hideMonitoringDetails) 
        }
        conversionData.ctfProjectId = projectId
    }
        
    def boolean isFreshInstall() {
        Repository.count() == 0
    }

    def convert(conversionData) {
        def errors = []
        def warnings = []

        def ctfServer = CtfServer.getServer() 
        if (!ctfServer) {
            ctfServer = new CtfServer()
        }
        ctfServer.baseUrl = conversionData.ctfURL 
        ctfServer.internalApiKey = conversionData.serverKey
        ctfServer.save(flush:true)

        // TODO shutdown access to svn, put up a static conversion
        // page until done.
        def server = Server.getServer()
        server.mode = ServerMode.CONVERTING_TO_MANAGED
        server.save(flush:true)

        def exception = null
        try {
            if (this.isFreshInstall() && conversionData.ctfUsername) {
                conversionData.soapSessionId = ctfRemoteClientService.login(
                    conversionData.ctfURL, conversionData.ctfUsername, 
                    conversionData.ctfPassword, conversionData.userLocale)
            }
            doConvert(conversionData, ctfServer, server, errors, warnings)

        } catch (UnknownHostException unknownHost) {
            exception = unknownHost

        } catch (NoRouteToHostException unreachableServer) {
            exception = unreachableServer

        } catch (MalformedURLException malformedUrl) {
            exception = malformedUrl

        } catch (SSLHandshakeException sslException) {
            exception = sslException

        } catch (InvalidSecurityKeyException keyException) {
            exception = keyException

        } catch (CantBindPortException cantRestartServer) {
            if (!this.isFreshInstall()) {
                this.undoLocalServerConfiguration(server)
            }
            exception = cantRestartServer

        } catch (CtfAuthenticationException wrongCredentials) {
            if (!this.isFreshInstall()) {
                this.undoLocalServerConfiguration(server)
            }
            exception = wrongCredentials

        } catch (RemoteMasterException scmMightNotBeReachable) {
            // this might happen because ViewVC or Subversion URLs are not
            // reachable for some reason. Throw as general error. Also
            // login problems, or parameters that are wrong.
            // At this point, only the configuration and the server state has
            // been changed. So, change it back instead of a full revert.
            if (!this.isFreshInstall()) {
                this.undoLocalServerConfiguration(server)
            }
            exception = scmMightNotBeReachable

        } catch (Exception e) {
            log.error("CTF mode conversion failed: " + e.message, e)
            if (e.message) {
                errors << "An exception occured: " + e.message
            } else {
                errors << "An exception occured: " + e.getClass().name
            }
            if (!conversionData.exSystemId) {
                exception = e
            }
        }

        // exceptions occurred before server configuration changes.
        if (exception) {
            undoServerMode(server)
            restartServer()
        }

        if (errors && !exception) {
            try {
                doRevertFromCtfMode(conversionData.ctfURL,
                    conversionData.exSystemId,
                    getWebSessionId(conversionData), 
                    server, ctfServer, errors, conversionData.userLocale)
            } catch (Exception e) {
                def msg = getMessage(
                    "setupTeamForge.integration.recovery.failed", 
                    conversionData.userLocale)
                log.error(msg, e)
                errors << msg
            }
        }
        return ['errors':errors, 'warnings':warnings, 'exception': exception]
    }

    private def doConvert(conversionData, ctfServer, server, errors, warnings) {
        installIntegrationServer(conversionData)
        unpackIntegrationScripts(conversionData.userLocale)

        serverConfService.backupAndOverwriteHttpdConf()
        serverConfService.writeConfigFiles()
        restartServer()

        // Noticing a tendency for the viewvc URL validation during 
        // conversion to fail once and then work the second try.
        // See artf6841 as a possible example.
        Thread.sleep(3000)

        if (!conversionData.exSystemId && (conversionData.ctfUsername || conversionData.soapSessionId)) {
            conversionData.exSystemId = registerIntegrationServer(conversionData)
        }
        ctfServer.mySystemId = conversionData.exSystemId
        ctfServer.save(flush:true)

        // if no errors are encountered during the initial conversion, then
        // proceed with the conversion.

        if (conversionData.ctfProject) {
            fixInvalidRepos(conversionData)
            addReposToProjects(conversionData)
        }

        server.mode = ServerMode.MANAGED
        server.save(flush:true)
        restartServer()

        authenticationManager.providers = [ctfAuthenticationProvider]

        if (conversionData.ctfProject && conversionData.importUsers
            && !errors) {
            addUsers(conversionData, warnings)
        }

        log.info("Registered external system ID '${conversionData.exSystemId}'")
    }

    private logMsg(def msg, def errors, Exception e) {
            GrailsUtil.deepSanitize(e)
            log.warn(msg, e)
            errors << msg
    }
    
    private undoServerMode(Server server) {
        server.mode = ServerMode.STANDALONE
        server.save(flush:true)
    }

    private void undoLocalServerConfiguration(Server server) {
        undoServerMode(server)
        serverConfService.restoreHttpdConfFromBackup() 
        serverConfService.writeConfigFiles()
        try {
            restartServer()
        } catch (CantBindPortException cantBind) {

        }
    }

    /**
     * Sets the server to be in stand-alone mode, as well as removes the 
     * ctfServer instance.
     * 
     * @param server is the current server
     * @param ctfServer is the current ctf server
     * @param errors is the errors to be collected.
     */
    private void undoServers(Server server, CtfServer ctfServer, errors, locale) {
        try {
            if (ctfServer && ctfServer.mySystemId || server) {
                this.undoLocalServerConfiguration(server)
            }

        } catch (Exception e) {
            def msg = getMessage(
                "setupTeamForge.integration.recovery.serverFailed", locale)
            logMsg(msg, errors, e)
            errors << msg
        }

        if (ctfServer) {
            ctfServer.delete()
        }

        authenticationManager.providers = [csvnAuthenticationProvider]
    }

    private void undoLocalRepositoriesDependencies(Server server, errors, 
            locale) {
        File repoParent = new File(server.repoParentDir)
        try {
            File idFile = new File(repoParent, ".scm.properties")
            if (idFile.exists() && !idFile.delete()) {
                def msg = getMessage(
                    "setupTeamForge.integration.recovery.externalProperty", 
                    locale)
                logMsg(msg, errors, null)
            }
        } catch (Exception e) {
            def msg = getMessage(
                    "setupTeamForge.integration.recovery.externalProperty", 
                    locale)
            logMsg(msg, errors, e)
        }

        try {
            repoParent.eachFile {
                File hooksDir = new File(it, "hooks")
                if (hooksDir.exists()) {
                    restoreNonCtfHooks(hooksDir)
                }
            }
        } catch (Exception e) {
            def msg = getMessage(
                    "setupTeamForge.integration.recovery.hookscripts.failed", 
                    locale)
            logMsg(msg, errors, e)
        }
    }

    /**
     * Converts the current instance back to SvnEdge stand-alone. If the current
     * server is already converted, then the given admin username/password is
     * used to login to the ctfUrl currently installed.
     * 
     * @param ctfUsername is the username for the ctf admin.
     * @param ctfPassword is the password for the ctf admin.
     * @param errors the errors to be collected.
     * @throws CtfAuthenticationException if the authentication fails with the
     * CTF server.
     */
    public void revertFromCtfMode(String ctfUsername, String ctfPassword,
            errors, locale) throws CtfAuthenticationException {

        Server server = Server.getServer()
        CtfServer ctfServer = CtfServer.getServer()

        if (ctfServer?.baseUrl) {
            def sessionIds = ctfRemoteClientService.getCtfSessionIds(ctfServer.baseUrl, ctfUsername,
                ctfPassword)
            doRevertFromCtfMode(ctfServer.baseUrl, ctfServer.mySystemId,
                sessionIds?.jsessionid, server, ctfServer, errors, locale)
        }
    }

    private void doRevertFromCtfMode(ctfUrl, exSystemId, jsessionid, 
                                     server, ctfServer, errors, locale) {
        if (jsessionid) {
            this.undoServers(server, ctfServer, errors, locale)
            if (ctfUrl && exSystemId) {
                ctfRemoteClientService.undoExternalSystemOnRemoteCtfServer(
                    ctfUrl, jsessionid, exSystemId, errors, locale)
            }
            this.undoLocalRepositoriesDependencies(server, errors, locale)
        } else {
            def msg = getMessage("setupTeamForge.integration.ctf.auth.failed",
                locale)
            throw new CtfAuthenticationException(msg, 
                "setupTeamForge.integration.ctf.auth.failed")
        }
    }
                              
    /**
     * Updates credentials needed for an integration server, namely
     * the API key.
     * @param ctfConn A bean with the serverKey property
     */
    void updateCtfConnection(CtfConnectionBean ctfConn) 
            throws InvalidSecurityKeyException {
        CtfServer ctfServer = CtfServer.getServer()
        if (ctfServer.internalApiKey != ctfConn.serverKey) {
            ctfServer.internalApiKey = ctfConn.serverKey
            ctfServer.save()
            if (lifecycleService.isStarted()) {
                lifecycleService.restartServer()
                if (!confirmApiSecurityKey()) {
                    throw new InvalidSecurityKeyException(
                            ctfConn.ctfURL, "API security key is invalid.", null)
                }
            }
        }
    }
    
    /**
     * Tests that svn can contact TF for auth/z
     */
    boolean confirmApiSecurityKey() {
        if (lifecycleService.isStarted()) {
            def replicaConfiguration = ReplicaConfiguration.getCurrentConfig()
            Server server = Server.getServer()
            def contextPath = server.getSvnBasePath()
            if (replicaConfiguration) {
                contextPath = replicaConfiguration.contextPath()
            }
            String dateStamp = new Date().format("yyyy_MM_dd")
            File errorLog = new File(ConfigUtil.logsDirPath(),
                    "error_" + dateStamp + "_00_00_00.log")
            int initialSize = (int) errorLog.length()
            
            String username = "nonexistentUser"
            String password = "myPassword"
            def svnUrl = server.urlPrefix() +
                    contextPath + "/_junkrepos"
            def command = [ConfigUtil.svnPath(), "ls", svnUrl,
                "--non-interactive", "--username", username,
                "--password", password,
                "--config-dir", ConfigUtil.svnConfigDirPath()]
            if (server.useSsl) {
                // we can trust whatever server cert is used here, especially
                // since we executing a nonsense command, just to look at the
                // local error log
                //command << "--trust-server-cert"
            }
            String[] commandOutput = commandLineService
                    .execute(command.toArray(new String[0]), null, null, false)
            if (commandOutput[2].contains("issuer is not trusted")) {
                // Could check for the svnedge default cert fingerprint,
                // but since we are not sending any important data here, it
                // is okay to just accept whatever cert is being used.
                svnRepoService.acceptSslCertificate(
                        svnUrl, username, password, null, true)
            }
                    
            if (errorLog.exists()) {
                String errorText = errorLog.text.substring(initialSize)
                return !errorText.contains("Security exception")
            } else {
                log.warn "confirmApiSecurityKey returning true because " +
                        errorLog.canonicalPath + " does not exist."
            }
        }
        log.debug "confirmApiSecurityKey returning true because server is not running."
        return true
    }

    private static final String sfPrefix = 
        "# BEGIN SOURCEFORGE SECTION - Do not remove these lines\n"
    private static final String sfSuffix = "# END SOURCEFORGE SECTION\n"

    private static final String NON_CTF_HOOKS_ARCHIVE = "pre-ctf-hooks.zip"

    protected void archiveCurrentHooks(File hooksDir) {
        archiveFiles(NON_CTF_HOOKS_ARCHIVE, hooksDir, 
                     [CTF_HOOKS_ARCHIVE, OLD_CTF_HOOKS_ARCHIVE])
    }

    protected void archiveFiles(String zipFileName, File directory, 
        List<String> excludedFiles=null) {
        log.debug("Archiving files in " + directory.name + 
                  " into " + zipFileName)
        File zipFile = new File(directory, zipFileName)
        boolean filesExist = false
        directory.eachFile { f ->
            if (isFileToBeArchived(f, zipFile, excludedFiles)) {
                filesExist = true
            }
        }
        if (!filesExist) {
            log.debug("No files to archive")
            return
        }
        
        ZipOutputStream zos = null
        try {
            zos = new ZipOutputStream(zipFile.newOutputStream())
            directory.eachFile { f ->
                if (isFileToBeArchived(f, zipFile, excludedFiles)) {
                    ZipEntry ze = new ZipEntry(f.name)
                    if (f.canExecute()) {
                        ze.extra = [1] as byte[]
                    }
                    zos.putNextEntry(ze)
                    f.withInputStream { zos << it }
                    zos.closeEntry()
                }
            }
        } finally {
            if (zos) {
                zos.close()
            }
        }

        directory.eachFile { f ->
            if (isFileToBeArchived(f, zipFile, excludedFiles)) {
                log.debug("Deleting file: " + f.name)
                f.delete()
            }
        }
    }

    private boolean isFileToBeArchived(File f, File zipFile, List<String> excludedFiles) {
        return f.isFile() && !f.equals(zipFile) && 
            (!excludedFiles || !excludedFiles.contains(f.name))
    }
    
    private static final String INTEGRATION_SCRIPTS_ZIP =
        "integration-scripts.zip"

    public unpackIntegrationScripts(Locale locale = null) {
        def libDir = new File(ConfigUtil.appHome(), "lib")
        def archiveFile = new File(libDir, INTEGRATION_SCRIPTS_ZIP)
        def integrationDir = new File(libDir, "integration")
        if (integrationDir.exists() && !integrationDir.deleteDir()) {
            log.warn("Unable to remove integration directory before " + 
                "unpacking up-to-date integration scripts.")
        }
        unpackZipFile(archiveFile, libDir, {a, b -> })
        File oldViewvcFile = new File(libDir, 
            "integration/viewvc/lib/vcauth/teamforge")
        File newViewvcFile = 
            new File(libDir, "viewvc/vcauth/teamforge")
        if (newViewvcFile.exists() && !newViewvcFile.deleteDir()) {
            log.warn("Unable to delete existing ViewVC teamforge " + 
                "authorizer: " + newViewvcFile.absolutePath)
        }
        if (!oldViewvcFile.renameTo(newViewvcFile)) {
            throw new Exception(
                getMessage("setupTeamForge.integration.vcauth.failed", locale))
        } else {
            oldViewvcFile.parentFile.parentFile.deleteDir()
        }

        oldViewvcFile = new File(libDir, 
            "integration/viewvc/bin/cgi/ctf_viewvc.cgi")
        if (!oldViewvcFile.exists()) {
            throw new FileNotFoundException("missing file " + oldViewvcFile)
        }
        newViewvcFile = new File(ConfigUtil.appHome(), 
                                 "bin/cgi-bin/ctf_viewvc.cgi")
        if (newViewvcFile.exists() && !newViewvcFile.delete()) {
            log.warn("Unable to delete existing ViewVC teamforge " + 
                "handler: " + newViewvcFile.absolutePath)
        }
        if (!oldViewvcFile.renameTo(newViewvcFile)) {
            throw new Exception(getMessage(
                "setupTeamForge.integration.viewvc.failed", 
                [newViewvcFile], locale))
        } else {
            oldViewvcFile.parentFile.parentFile.parentFile.deleteDir()
        }
        
        newViewvcFile.executable = true
    }

    protected unpackZipFile(File archiveFile, File destDir, 
                            Closure fileEntryClosure) {
        ZipFile zipFile = null
        try {
            // Open Zip file for reading
            zipFile = new ZipFile(archiveFile)
            Enumeration<ZipEntry> zipFileEntries = zipFile.entries()
            while (zipFileEntries.hasMoreElements())
            {
                ZipEntry entry = zipFileEntries.nextElement()
                if (entry.isDirectory()) {
                    new File(destDir, entry.name).mkdirs()
                } else {
                    File destFile = new File(destDir, entry.getName())
                    BufferedInputStream is = null
                    try {
                        is = new BufferedInputStream(
                            zipFile.getInputStream(entry));
                        destFile.withOutputStream { it << is }
                    } finally {
                        if (is) {
                            is.close()
                        }
                    }
                    fileEntryClosure(entry, destFile)
                }
            }
        } finally {
            if (zipFile) {
                zipFile.close()
            }
        }
    }

    private static final String CTF_HOOKS_ARCHIVE = "ctf-hook-scripts.zip"
    private static final String OLD_CTF_HOOKS_ARCHIVE = 
        "old-ctf-hook-scripts.zip"
    
    protected void restoreNonCtfHooks(File hooksDir) {
        log.debug("Archiving CTF hook scripts and restoring pre-ctf " + 
                  "scripts for " + hooksDir.name)
        File ctfHooksZip = new File(hooksDir, CTF_HOOKS_ARCHIVE)
        if (ctfHooksZip.exists()) {
            File oldCtfHooksZip = new File(hooksDir, OLD_CTF_HOOKS_ARCHIVE)
            if (!oldCtfHooksZip.exists() || oldCtfHooksZip.delete()) {
                log.debug("Previous CTF archive is being moved.")
                ctfHooksZip.renameTo oldCtfHooksZip
            }
        }

        log.debug("Archiving CTF hook scripts")
        archiveFiles(CTF_HOOKS_ARCHIVE, hooksDir, 
                     [NON_CTF_HOOKS_ARCHIVE, OLD_CTF_HOOKS_ARCHIVE])

        File archiveFile = new File(hooksDir, NON_CTF_HOOKS_ARCHIVE)
        if (archiveFile.exists()) {
            log.debug("Restoring pre-ctf hook scripts")
            unpackZipFile(archiveFile, hooksDir, {entry, destFile -> 
                byte[] extra = entry.extra
                if (extra && extra.length == 1 && extra[0] == 1) {
                    destFile.setExecutable(true)
                }
            })
            archiveFile.delete()
        }
    }

    private String getContentPrefix(String script) {
        String sourceforgeHome = "./FIXME" // FIXME!
        File pythonScriptDir = new File(sourceforgeHome, "integration")
        sfPrefix + "python " + 
            new File(pythonScriptDir, script).canonicalPath
    }

    def installIntegrationServer(conversionData) {
        // TODO
        // will need to copy conversionData.serverKey to the integration
        // server configuration.  For example in a sourceforge.properties
        // file:
        // sfmain.integration.security.shared_secret=${conversionData.serverKey}

    }

    protected def addUsers(conversionData, warnings) {
        try {
            def csvnUsernames = 
                getCsvnUsersComparedToCtfUsers(conversionData)[1]

            def csvnUsers = User.list()
            def dotUsersAlreadyFound = false
            for (User user in csvnUsers) {
                if (csvnUsernames.contains(user.username)) {
                    if (user.username.contains(".")) {
                        if (!dotUsersAlreadyFound) {
                            def msg = getMessage("setupTeamForge.integration." +
                                "users.usernamesWithDotsNotImported", 
                                conversionData.userLocale)
                            warnings << msg
                            dotUsersAlreadyFound = true
                        }
                        continue
                    }
                    addUser(user, conversionData, warnings)
                }
            }
        } catch (Exception e) {
            def msg = getMessage(
                "setupTeamForge.integration.users.someNotImported",
                conversionData.userLocale)
            logMsg(msg, warnings, e)
        }
    }

    protected def addUser(User u, conversionData, warnings) {
        boolean isSuperUser = false
        boolean isRestrictedUser = false
        String password = securityService.generatePassword(8, 12)
        def userDO = null
        try {
            userDO = ctfRemoteClientService.createUser(conversionData.ctfURL,
                conversionData.soapSessionId, u.username, password, u.email,
                u.realUserName, isSuperUser, isRestrictedUser, conversionData.userLocale)

            if (userDO && conversionData.assignMembership) {
                //TODO: expose the method call addProjectMemeber to the service
                def ctfSoap = ctfRemoteClientService.cnSoap(
                    conversionData.ctfURL)
                ctfSoap.addProjectMember(
                    conversionData.soapSessionId, 
                    conversionData.ctfProjectId,
                    userDO.username)
            }
        } catch (com.collabnet.ce.soap50.fault.NoSuchObjectFault e) {
            log.warn("Error while assigning project membership to " + 
                     u.username, e)
            def msg = getMessage(
                "setupTeamForge.integration.users.notAddedAsMember", 
                [u.username], conversionData.userLocale)
            warnings << msg

        } catch (RemoteMasterException remoteProblems) {
            def msg = getMessage(
                "setupTeamForge.integration.users.errorAssigningMembership", 
                [u.username], conversionData.userLocale)
            log.error(msg, remoteProblems)
            warnings << msg + ": " + remoteProblems.getMessage()
        }
    }

    public def restartServer() throws CantBindPortException {
        def result = -1;
        if (lifecycleService.isStarted()) {
            result = lifecycleService.gracefulRestartServer()
        } else {
            result = lifecycleService.startServer()
        }

        discoveryService.serverUpdated()
        return result
    }

    private void copyFiles(fromDir, toDir) {
        fromDir.eachFile {
            if (it.isFile()) {
                copyFile(it, new File(toDir, it.name))
            }
        }
    }       

    private void copyFile(f1, f2) {
        InputStream ins = new FileInputStream(f1);
        OutputStream outs = new FileOutputStream(f2);
      try {

        byte[] buf = new byte[1024];
        int len;
        while ((len = ins.read(buf)) > 0){
            outs.write(buf, 0, len);
        }
      }
      finally {
        ins.close();
        outs.close();
      }
    }
}
