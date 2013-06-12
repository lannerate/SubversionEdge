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

import com.collabnet.svnedge.console.AbstractSvnEdgeService
import com.collabnet.svnedge.domain.NetworkConfiguration
import com.collabnet.svnedge.domain.User
import com.collabnet.svnedge.domain.integration.ApprovalState
import com.collabnet.svnedge.domain.integration.CtfServer
import com.collabnet.svnedge.domain.integration.ReplicaConfiguration
import com.collabnet.svnedge.net.HttpProxyAuth
import com.collabnet.svnedge.util.SoapClient
import com.collabnet.svnedge.util.SvnEdgeCertHostnameVerifier
import grails.util.GrailsUtil
import java.security.cert.CertificateException
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLHandshakeException
import org.apache.axis.AxisFault
import org.codehaus.groovy.grails.plugins.springsecurity.GrailsUser
import org.codehaus.groovy.grails.plugins.springsecurity.GrailsUserImpl
import org.springframework.security.GrantedAuthority
import org.springframework.security.GrantedAuthorityImpl

/**
 * CTFWsClientService defines the service used by SVNEdge to communicate with 
 * a Master CTF based on the bootstrapped location information (url, port, ssl
 * system ID. Although this class is called a Web Service client, it exposes
 * proxy methods for one HTTP GET request method.
 * 
 * For user authentication, this service uses the CTF SDK, requesting a login
 * with the username and password. On the other hand, user authorization is 
 * requested by using the ScmPermissionsProxyServlet.
 * 
 * For the CTF SDK, 
 * visit http://www.open.collab.net/community/cif/ctf/52/sdk.tar.gz
 *
 * @author Marcello de Sales(mdesales@collab.net)
 */
public class CtfRemoteClientService extends AbstractSvnEdgeService {

    private static String ROLE_USER = "ROLE_USER"
    private static String ROLE_ADMIN = "ROLE_ADMIN"
    private static String ROLE_ADMIN_SYSTEM = "ROLE_ADMIN_SYSTEM"
    /**
     * The prefix of the command ids.
     */
    public static final String COMMAND_ID_PREFIX = "cmdexec"

    def securityService
    def networkingService

    boolean transactional = false

    private static class LRUCache extends LinkedHashMap<String, CtfRemoteStrategy> {
        private final int maxSize;

        private LRUCache(int maxSize) {
            super(maxSize, 0.75f, true)
            this.maxSize = maxSize
        }

        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > maxSize;
        }
    }

    // there will likely only ever be one entry here, but using a cache so that
    // it won't break down during testing
    private static Map<String, CtfRemoteStrategy> mStrategyCache = 
        new LRUCache(5).asSynchronized()

    /**
     * Closure to build the list of parameters for a URL
     */
    def buildParam = { map ->
        def allParams = ""
        map.each{ key, value -> allParams += "$key=$value&" }
        return allParams[0..-2]
    }

    def registerReplica(server, replica) {
        ApprovalState.APPROVED.getName()
    }

    def getReplicaApprovalState() {
        ApprovalState.APPROVED.getName()
    }

    /**
     * @return the URL to the CTF server based on the configuration.
     */
    private makeCtfBaseUrl(useSsl, hostname, port) {
        def ctfProto = useSsl ? "https://" : "http://"
        def ctfPort = port == 80 ? "" : ":" + port
        return ctfProto + hostname + ctfPort
    }

    public def cnSoap(ctfBaseUrl) {
        def url = ctfBaseUrl ?: CtfServer.getServer().baseUrl
        return getStrategy(url).makeCollabNetClient()
    }

    public def makeScmSoap(ctfBaseUrl) {
        def url = ctfBaseUrl ?: CtfServer.getServer().baseUrl
        return getStrategy(url).makeScmAppClient()
    }

    /**
     * clears any client or connection instances that may be cached
     * @return
     */
    public def clearClientCache() {
        mStrategyCache.clear()
    }

    private def getStrategy(ctfBaseUrl) throws UnknownHostException, 
            NoRouteToHostException, MalformedURLException,
            SSLHandshakeException {
        def url = ctfBaseUrl ?: CtfServer.getServer().baseUrl
        def networkConfiguration = networkingService.getNetworkConfiguration();
        def strategy = mStrategyCache.get(url)
        if (!strategy) {
            strategy = new Soap60CtfRemoteStrategy(url, networkConfiguration)
            try {
                // will throw an exception, if ce-soap60 namespace doesn't exist
                strategy.makeCollabNetClient().getApiVersion()
            } catch (AxisFault e) {
                strategy = new Soap50CtfRemoteStrategy(url, networkConfiguration)
            }
            catch (Exception e) {
                strategy = new Soap50CtfRemoteStrategy(url, networkConfiguration)
                log.debug("Defaulting to soap60 CTF API because of exception", e)
            }
            mStrategyCache.put(url, strategy)
        }
        return strategy
    }

    private SoapClient makeScmListenerClient(ctfBaseUrl) {
        NetworkConfiguration nc = networkingService.getNetworkConfiguration()
        return new SoapClient((ctfBaseUrl ?: CtfServer.getServer().baseUrl) + 
                              "/ce-soap/services/ScmListener", nc)
    }

    private String authzBaseUrl() {
        // FIXME! This needs to be invoked on the local appserver, but
        // keeping it this way, so that tests continue to pass
        CtfServer.getServer().baseUrl +
            "/integration/servlet/ScmPermissionsProxyServlet?"
    }

    /**
     * @param ctfUrl is the URL for the CTF server in the format 
     * 'protocol://domainNAme:portNumber'.
     * @param username is the username identification
     * @param password is the password.
     * @return the User Session ID from the given CTF server URL.
     * @throws CtfAuthenticationException in case the login is incorrect
     * @throws RemoteMasterException in case any other exception occurs
     */
    public String login(ctfUrl, username, password, locale) 
            throws CtfAuthenticationException, RemoteMasterException, 
            UnknownHostException, NoRouteToHostException, MalformedURLException,
            SSLHandshakeException, CtfConnectionException {
        try {
            return getStrategy(ctfUrl).login(username, password, locale)

        } catch (AxisFault e) {
            GrailsUtil.deepSanitize(e)
            if (e.faultString.contains("password was set by an admin")) {
                def msg = getMessage("ctfRemoteClientService.auth.needschange",
                    [ctfUrl.encodeAsHTML()], locale)
                log.info(msg)
                throw new CtfAuthenticationException(msg, 
                    "ctfRemoteClientService.auth.needschange")

            } else if (e.faultString.contains("Error logging in.")) {
                def msg = getMessage("ctfRemoteClientService.auth.error", 
                    [ctfUrl.encodeAsHTML()], locale)
                log.info(msg)
                throw new CtfAuthenticationException(msg, 
                    "ctfRemoteClientService.auth.error")

            } else if (e.faultString.contains("SSLHandshakeException")) {
                // catch axis fault of this type and rethrow as underlying exception
                log.debug("SSL problem preventing authentication with CTF.", e)
                throw new SSLHandshakeException(e.getMessage())

            } else if (e.faultString.contains("(301)Moved Permanently")) {
                def msg = getMessage("ctfRemoteClientService.service.redirect.error",
                    [ctfUrl.encodeAsHTML()], locale)
                log.info(msg)
                throw new CtfConnectionException(msg,
                    "ctfRemoteClientService.service.redirect.error")

            } else if (e.faultString.contains("(503)Service Temporarily Unavailable") 
                    || e.faultString.contains("(502)Proxy Error")) {

                def key = "ctfRemoteClientService.cannot.authenticate.user.integration"
                // the ctf service/server is down.
                def msg = getMessage(key, [ctfUrl.encodeAsHTML()], locale)
                log.info(msg)
                throw new RemoteMasterException(msg, key)

            } else if (e.detail instanceof UnknownHostException) {
                def hostname = new URL(ctfUrl).host
                throw new UnknownHostException(getMessage(
                    "ctfRemoteClientService.host.unknown.error", 
                    [hostname.encodeAsHTML()], locale))

            } else if (e.detail instanceof NoRouteToHostException ||
                    e.faultString.contains("Connection refused")) {
                def hostname = new URL(ctfUrl).host
                throw new NoRouteToHostException(getMessage(
                    "ctfRemoteClientService.host.unreachable.error", 
                    [hostname.encodeAsHTML()], locale))

            } else {
                def msg = getMessage("ctfRemoteClientService.general.error",
                    [ctfUrl.encodeAsHTML()], locale)
                log.error(msg, e)
                throw new RemoteMasterException(ctfUrl, msg, e)
            }
        } catch (CtfAuthenticationException e) {
            throw e
        } catch (Exception otherErrors) {
            throw new MalformedURLException(getMessage(
                "ctfRemoteClientService.host.malformedUrl",
                [ctfUrl.encodeAsHTML()], locale))
        }
    }

    /**
     * @param username is the username identification
     * @param password is the password.
     * @return the User Session ID from the CTF server. 
     */
    public String login(username, password, locale) 
            throws CtfAuthenticationException, RemoteMasterException,
            UnknownHostException, NoRouteToHostException, MalformedURLException,
            SSLHandshakeException {
        return login(CtfServer.server.baseUrl, username, password, locale)
    }

    public void logoff(ctfUrl, username, sessionId, boolean quieter = false) {
        try {
            cnSoap(ctfUrl).logoff(username, sessionId)
        } catch (Exception e) {
            if (quieter) {
                log.debug("Logging off from session " + sessionId + 
                          " failed.", e)
            } else {
                log.warn("Logging off from session " + sessionId + 
                         " failed.", e)
            }
        }
    }
    
    /**
     * Authenticate against the CTF server.
     * WARNING: if the CTF master's admin pw is default, this may
     * return true for empty usernames.
     * @param username is an existing username on the Master CTF. 
     * @param password is the associated password for the given username. 
     * @return GrailsUser, if auth succeeds, null otherwise
     */
    GrailsUser authenticateUser(username, password) throws RemoteMasterException,
            NoRouteToHostException, UnknownHostException {
        GrailsUser gUser = null
        String sessionId = null
        def locale = Locale.getDefault()
        def ctfUrl = CtfServer.server.baseUrl
        try {
            sessionId = getStrategy().login(username, password, null)

        } catch (AxisFault e) {
             String faultMsg = e.faultString
             def hostname = new URL(ctfUrl).host

           if (e.detail instanceof NoRouteToHostException) {
                // server started without connection, recovered, and lost again
                // Network is unreachable
                throw new NoRouteToHostException(getMessage(
                    "ctfRemoteClientService.host.unreachable.error", 
                    [hostname.encodeAsHTML()], locale))

           } else if (e.detail instanceof UnknownHostException) {
                 throw new UnknownHostException(getMessage(
                     "ctfRemoteClientService.host.unknown.error",
                     [hostname.encodeAsHTML()], locale))

            } else if (faultMsg.contains("(503)Service Temporarily Unavailable") 
                    || faultMsg.contains("(502)Proxy Error")) {

                // the ctf service/server is down.
                def msg = getMessage(
                    "ctfRemoteClientService.cannot.authenticate.user.unreachable",
                    [hostname.encodeAsHTML()], locale)
                throw new CtfServiceUnavailableException(msg, e.detail)

            } else if (faultMsg.contains("verify the username and password")) {
                def key = "ctfRemoteClientService.auth.error"
                def msg = getMessage(key, [ctfUrl.encodeAsHTML()], locale)
                throw new CtfAuthenticationException(msg, key)
            }

        } catch (Exception e) {
             GrailsUtil.deepSanitize(e)
             // also no session, but log this one as it indicates a problem
             def generalMsg = getMessage(
                 "ctfRemoteClientService.general.error", [e.getMessage()],
                 locale)
             log.error(generalMsg, e)
             throw new RemoteMasterException(ctfUrl, generalMsg, e)
        }

        if (null != sessionId && sessionId.length() > 0) {

            // if the provided credentials match those stored in the CtfServer instance,
            // grant ROLE_ADMIN to the session
            def ctfServer = CtfServer.getServer()
            boolean loginMatchesStoredCredentials = (username == ctfServer.ctfUsername)            
            
            gUser = getUserDetails(sessionId, username, loginMatchesStoredCredentials)

            try {
                cnSoap().logoff(username, sessionId)
            } catch (Exception e) {
                GrailsUtil.deepSanitize(e)
                log.warn("Unable to logoff from session for " + username + 
                    " due to exception", e)
            }
        }
        gUser
    }

    /**
     * @param sessionId the ctf soap session key
     * @param username for the user.
     * @param isLocalAdmin grant ROLE_ADMIN to the user session, irrespective of Ctf permissions
     * @return information about the user
     */
   private GrailsUser getUserDetails(sessionId, username, boolean isLocalAdmin = false) {
      
        def ctfUser = cnSoap().getUserData(sessionId, username)
        
        // if we're not already granting local admin permissions, check back with CTF to 
        // see if the user has the Integration.edit.edit_scm permission
        boolean hasScmEditPermission = false
        if (!ctfUser.superUser && !isLocalAdmin) {
            try {
                String[] adapterNames = makeScmSoap().getScmAdapterNames(sessionId)
                if (adapterNames?.length) {
                    hasScmEditPermission = true
                }
            }
            catch (Exception e)  {
                log.info("ScmAppSoap.getScmAdapterNamers() is not accessible; not granting local admin role ")
            }
        }
       
        // TODO CTF REPLICA
        // Using a domain object here to avoid introducing another user
        // object until it is decided whether we should maybe use 
        // ReplicaUser here.
        User u = new User(username: username, 
            realUserName: ctfUser.fullName,
            email: ctfUser.email)
        // create a pseudo integer id for the local pseudo user
        // which is needed for file edit locks
        u.id = (Math.random() * 1000).toInteger()
        // not sure if this is needed on a new object, but we don't want
        // the data saved to the db, so adding it for safety
        u.discard()

        // trues =>  enabled, accountNonExpired, credentialsNonExpired,
        //           accountNonLocked,
        new GrailsUserImpl(username, "password", true, true, true, true, 
                           getGrantedAuthorities(ctfUser, isLocalAdmin, hasScmEditPermission), u)
    }

    private GrantedAuthority[] getGrantedAuthorities(ctfUser, boolean isLocalAdmin = false, boolean isScmAdmin = false) {
        
        def auth = []
        if (!ctfUser.restrictedUser) {
            auth << new GrantedAuthorityImpl(ROLE_USER)
            if (ctfUser.superUser || isLocalAdmin) {
                auth << new GrantedAuthorityImpl(ROLE_ADMIN)
            }
            else if (isScmAdmin)  {
                auth << new GrantedAuthorityImpl(ROLE_ADMIN_SYSTEM)
            }
        }
        auth as GrantedAuthority[]
    }

    private static String PERM_USER = "view"
    private static String PERM_ADMIN = "admin"

    def getReplicaPermissions(username) {
        // TODO CTF REPLICA
        def perms = []
        if (username == "root") {
            perms = [PERM_USER, PERM_ADMIN]
        } else if (username == "marcello") {
            perms = [PERM_USER]
        }
        log.info("CTF Perms=" + perms + " for username=" + username)
        perms
    }


    /**
     * This method defines the user authorization against a Master CTF. The
     * target Master url is constructed during the bootstrap, so that it can
     * process the given username, repository Path, and the optional access 
     * Type. The access to the Master CTF is performed through an HTTP GET 
     * request to a Servlet.
     * 
     * def getScmPermissionForPath(username, system_id, repo_path, access_type)
     * https://forge.collab.net/integration/viewvc/viewvc.cgi/trunk/core/
     * saturn/src/sourceforge_home/integration/SourceForge.py?revision=
     * 29863&root=ce&system=exsy1017&view=markup
     * 
     * @param username is the username of a given user on the Master CTF.
     * @param repoPath is the repository path. It can be reponame/path/to/smth
     * @param accessType is how the access must be granted. 
     * @throws IOException if any communication problem occurs with the Master
     * (Not reachable or unknown host). The server can return a 401 HTTP 
     * Response in case a Master CEE is used instead. (which is strange since
     * it's a bad request that results in Unauthorized)
     * 
     */
    String getRolePaths(username, repoPath, accessType) {
        def params = [:]
        String systemId = CtfServer.getServer().mySystemId
        if (accessType)
            params = ["username":username, "systemId":systemId, 
                      "repoPath":repoPath, "accessType":accessType]
        else
            params = ["username":username, "systemId":systemId, 
                      "repoPath":repoPath]
        def rolesPathRestUrl = authzBaseUrl() + buildParam(params)
        def result = rolesPathRestUrl.toURL().text
        log.debug("getScmPermissionForPath(${rolesPathRestUrl.toURL()}) " +
            "RESPONSE = " + result)
        return result
    }

    /**
     * Clears the cache at the SCM Permissions servlet. This requests clean
     * cache for the authentication servlet.
     * @return if the operation to clear the remote cache was successfully 
     * executed.
     * @throws IOException if any communication problem occurs with the Master
     * (Not reachable or unknown host). The server can return a 401 HTTP 
     * Response in case a Master CEE is used instead. (which is strange since
     * it's a bad request that results in Unauthorized)
     */
    boolean clearCacheOnMasterCTF() {
        def params = [clearCache:true]
        def rolesPathRestUrl = authzBaseUrl() + buildParam(params)
        def result = rolesPathRestUrl.toURL().text.trim()
        log.debug("clearCTFCache(${rolesPathRestUrl.toURL()}) " +
            "RESPONSE = " + result)
        return result == "permissions cache cleared"
    }

    def uploadStatistics(valuesByStats) {
        def results = [:]
        /* TODO CTF REPLICA
        def proxy = getReplicaProxy()
        def statRequests = valuesByStats.collect{ valuesByStat ->
            buildStatisticsRequestType(valuesByStat, proxy)
        }
        def uploadResults = proxy.uploadStatistics(getHostname(), 
                                                   statRequests)
        uploadResults.result.each {
            results[it.statistic] = it.succeeded
            if (!it.succeeded && it.failureMsg) {
                log.error("Uploading " + it.statistic + " failed due to " 
                          + it.failureMsg)
            } else if (!it.succeeded) {
                log.error("Uploading " + it.statistic + " failed")
            }
        }
        */
        results
    }

    private def buildStatisticsRequestType(valuesByStat, proxy) {
        def statRequest = proxy
            .create("com.collabnet.helm.ws.svnedge.StatisticsRequestType")
        statRequest.statistic = valuesByStat["statistic"].getName()
        statRequest.values = valuesByStat["values"].collect { value ->
            buildStatValueType(value, proxy)
        }
        return statRequest
    }

    private def buildStatValueType(value, proxy) {
        def statValue = proxy
            .create("com.collabnet.helm.ws.svnedge.StatValueType")
        statValue.timestamp = value.getTimestamp()
        statValue.interval = value.getInterval()
        statValue.minValue = value.getMinValue()
        statValue.maxValue = value.getMaxValue()
        statValue.averageValue = value.getAverageValue()
        statValue.lastValue = value.getLastValue()
        statValue.derived = value.getDerived()
        return statValue
    }

    def getSVNNotifications(timestamp) {
    }

    def uploadReplicaErrors(errors) {

    }

    /**
     * Adds this instance of SVN Edge as a new external system at a given
     * CTF located at the given ctfUrl.
     * 
     * @param ctfUrl is the url of the CTF system. 
     * @param userSessionId is sessionID of the default user to call the soap
     * client.
     * @param title is the title of the new external system
     * @param description is the description of the new external system
     * @param csvnProps is a map of the parameters to the service.
     * 
     * @return the String value of the system ID
     * 
     * @throws CtfAuthenticationException if the authentication fails with 
     * TeamForge.
     * @throws RemoteMasterException if any error occurs during the method 
     * call.
     */
    def String addExternalSystem(ctfUrl, userSessionId, adapterType, title, 
            description, csvnProps, locale) {

        try {
           def props = getStrategy(ctfUrl).makeSoapNamedValues(csvnProps)
           def scmSoap = this.makeScmSoap(ctfUrl)
           return scmSoap.addExternalSystem(userSessionId, adapterType,
               title, description, props)

        } catch (AxisFault e) {
            String faultMsg = e.faultString
            if (faultMsg.contains("The parameter type/value") && 
                faultMsg.contains("is invalid for the adapter type")) {
                // No such object: The parameter type/value 
                // 'RepositoryBaseUrl=http://cu064.cloud.sp.collab.net:18080/svn' 
                // is invalid for the adapter type 'Subversion'
                def typeValue = faultMsg.split("'")[1].split("=")
                def paramType = typeValue[0]
                def paramValue = typeValue[1]
                GrailsUtil.deepSanitize(e)
                if (paramType.equals("RepositoryBaseUrl")) {
                    throw new RemoteAndLocalConversationException(ctfUrl,
                        getMessage(
                            "ctfRemoteClientService.local.webdav.unreachable",
                            [paramValue], locale))

                } else if (paramType.equals("ScmViewerUrl")) {
                    throw new RemoteAndLocalConversationException(ctfUrl, 
                        getMessage(
                            "ctfRemoteClientService.local.viewvc.unreachable",
                            [paramValue], locale))

                } else {
                    def msg = getMessage(
                            "ctfRemoteClientService.local.remote.general.error",
                            [paramValue], locale)
                    throw new RemoteAndLocalConversationException(ctfUrl, msg +
                        " ${faultMsg}")
                }

            } else if (faultMsg.contains("Session is invalid or timed out")) {
                throw new CtfSessionExpiredException(ctfUrl, userSessionId,
                    getMessage("ctfRemoteClientService.remote.sessionExpired", 
                        locale), e)

            } else if (faultMsg.contains("Security exception")) {
                throw new InvalidSecurityKeyException(ctfUrl, faultMsg, e)
            } else {
                def msg = getMessage(
                    "ctfRemoteClientService.createExternalSystem.error", locale)
                throw new RemoteMasterException(ctfUrl, msg + " " + faultMsg, e)
            }

        } catch (Exception e) {
            GrailsUtil.deepSanitize(e)
            // also no session, but log this one as it indicates a problem
            def generalMsg = getMessage(
                "ctfRemoteClientService.createExternalSystem.error", locale)
            log.error(generalMsg, e)
            throw new RemoteMasterException(ctfUrl, generalMsg, e)
        }
    }
            
    /**
    * The list of replicable external systems element of the list is a
    * map of the properties of the integration server. Each elements has the
    * properties of the external system.
    *
    * @param ctfUrl is the ctf server complete URL, including protocol,
    * hostname and port.
    * @param sessionId is the user sessionId retrieved after logging in.
    * @param locale is the request locale for messaging
    * @return the list of replicable external systems in the TeamForge server
    * reached by the given ctfUrl, using the given sessionId.
    * @throws CtfSessionExpiredException if the given sessionId is expired.
    * @throws RemoteMasterException if any other error occurs during the
    * method execution.
    */
   def getReplicableScmExternalSystemList(ctfUrl, sessionId, locale) throws
          RemoteMasterException {
       try {
           def lt = makeScmSoap(ctfUrl).getReplicableScmExternalSystemList(
               sessionId).dataRows
           def scmList = []
           if (lt && lt.length > 0) {
               lt.each { extSystem ->
                   def scmSys = [:]
                   scmSys.id = extSystem.id
                   scmSys.title = extSystem.title
                   scmSys.description = extSystem.description
                   scmSys.isSvnEdge = extSystem.isSvnEdge
                   scmList << scmSys
               }
           }
           return scmList

       } catch (AxisFault e) {
           String faultMsg = e.faultString
           GrailsUtil.deepSanitize(e)
           if (faultMsg.contains("Session is invalid or timed out")) {
               throw new CtfSessionExpiredException(ctfUrl, sessionId,
                   getMessage("ctfRemoteClientService.remote.sessionExpired",
                       locale), e)
           } 
           else if (faultMsg.contains("No such operation")) {
               def errorMessage = getMessage(
                  "ctfRemoteClientService.host.noReplicaSupport.error", 
                  [ctfUrl], locale) 
               log.error(errorMessage, e)
               throw new RemoteMasterException(ctfUrl, errorMessage, e)
           }
           else {
               def errorMessage = getMessage(
                  "ctfRemoteClientService.listProjects.error", locale) + " " +
                      faultMsg
              log.error(errorMessage, e)
              throw new RemoteMasterException(ctfUrl, errorMessage, e)
          }
       }
    }

    /**
    * Adds this instance of SVN Edge as a new external system at a given
    * CTF located at the given ctfUrl.
    *
    * @param ctfUrl is the url of the CTF system.
    * @param userSessionId is sessionID of the default user to call the soap
    * client.
    * @param title is the title of the new external system
    * @param description is the description of the new external system
    * @param replicaProps is a map of the parameters to the service.
    *
    * @return the String value of the system ID
    *
    * @throws CtfAuthenticationException if the authentication fails with
    * TeamForge.
    * @throws RemoteMasterException if any error occurs during the method
    * call.
    */
    def String addExternalSystemReplica(ctfUrl, userSessionId, masterSystemId, 
            name, description, comment, replicaProps, locale)
            throws RemoteMasterException { 

        try {
            def soap = this.makeScmListenerClient(ctfUrl)
            def replicaId = (String) soap.invoke("addExternalSystemReplica", [userSessionId,
                masterSystemId, name, description, comment, replicaProps.HostName, 
                replicaProps.ConsolePort as int, replicaProps.HostPort as int, 
                replicaProps.HostSSL as boolean, replicaProps.ViewVCContextPath])
            return replicaId

        } catch (AxisFault e) {
             String faultMsg = e.faultString
            
            // this code is silently swallowing exceptions -- disabled
//             if (faultMsg.contains("The parameter type/value") &&
//                faultMsg.contains("is invalid for the adapter type")) {
//                // No such object: The parameter type/value
//                //'RepositoryBaseUrl=http://cu064.cloud.sp.collab.net:18080/svn'
//                // is invalid for the adapter type 'Subversion'
//                def typeValue = faultMsg.split("'")[1].split("=")
//                def paramType = typeValue[0]
//                def paramValue = typeValue[1]
//                GrailsUtil.deepSanitize(e)

            if (faultMsg.contains("Session is invalid or timed out")) {
               throw new CtfSessionExpiredException(ctfUrl, userSessionId,
                  getMessage("ctfRemoteClientService.remote.sessionExpired",
                       locale), e)
            }
             else {
                throw new RemoteMasterException(ctfUrl, e.faultString, e)
             }
            
         } catch (Exception e) {
           
             GrailsUtil.deepSanitize(e)
             def generalMsg = getMessage(
                 "ctfRemoteClientService.createExternalSystem.error", 
                 locale)
             log.error(generalMsg, e)
             throw new RemoteMasterException(ctfUrl, generalMsg, e)
         }
   }

    /**
     * Deletes this replica from the CTF system with the supplied credentials
     * (requires superuser)
     * @param ctfUsername
     * @param ctfPassword
     * @param errors
     * @param locale
     * @throws RemoteMasterException
     * @throws CtfAuthenticationException
     */
    def deleteReplica(ctfUsername, ctfPassword, errors, locale) throws CtfAuthenticationException, RemoteMasterException {

        def ctfUrl = CtfServer.getServer().baseUrl
        def replicaConfig = ReplicaConfiguration.getCurrentConfig()
        def replicaId = replicaConfig.systemId
        def soapId
        try {
            soapId = login(ctfUrl, ctfUsername, ctfPassword, locale)
            def sessionId = cnSoap(ctfUrl).getUserSessionBySoapId(soapId)
            def soap = this.makeScmListenerClient(ctfUrl)
            soap.invoke("deleteExternalSystemReplica", [sessionId, replicaId])
        }
        catch (CtfAuthenticationException e) {
            def msg = getMessage("ctfRemoteClientService.auth.error",
                    [ctfUrl.encodeAsHTML()], locale)
            log.debug(msg)
            errors << msg
            throw new CtfAuthenticationException(msg,
                    "ctfRemoteClientService.auth.error")
        }
        catch (AxisFault e) {
            if (e.faultCode.toString().contains("PermissionDeniedFault") ||
                    e.faultString.contains(
                    "You must be a site administrator to delete an external system")) {
                def msg = getMessage("ctfRemoteClientService.permission.error", 
                    [ctfUrl.encodeAsHTML()], locale)
                log.debug(msg)
                errors << msg
                throw new CtfAuthenticationException(msg, 
                    "ctfRemoteClientService.permission.error")
            }
            else if (e.faultString.contains("No such object: ${replicaId}")) {
                // this fault suggests that the replica server was already removed on the CTF side but not yet reverted
                // to standalone mode here
                log.warn("Attempting to delete, but this replica does not exist on the CTF master -- already deleted?")
            }
            else { 
                def errorMsg = getMessage(
                         "ctfRemoteClientService.general.error", [e.getMessage()],
                         locale)
                log.warn(errorMsg, e)
                errors << errorMsg
                throw new RemoteMasterException(ctfUrl, errorMsg, e)
            }
        } finally {
            if (soapId) {
                logoff(ctfUrl, ctfUsername, soapId)
            }
        }
    }

    /**
     * @param ctfUrl is the ctf server complete URL, including protocol, 
     * hostname and port.
     * @param sessionId is the user sessionId retrieved after logging in. This
     * can be a user session ID or a SOAP session ID.
     * @return the list of users in the TeamForge server reached by the
     * given ctfUrl, using the given sessionId.
     * @throws CtfSessionExpiredException if the given sessionId is expired.
     * @throws RemoteMasterException if any other error occurs during the
     * method execution.
     */
    def getUserList(ctfUrl, sessionId, locale) throws RemoteMasterException {
        try {
            def filter = null
            return this.cnSoap(ctfUrl).getUserList(sessionId, filter).dataRows

        } catch (AxisFault e) {
            String faultMsg = e.faultString
            GrailsUtil.deepSanitize(e)
            if (faultMsg.contains("Session is invalid or timed out")) {
                throw new CtfSessionExpiredException(ctfUrl, sessionId,
                    getMessage("ctfRemoteClientService.remote.sessionExpired",
                        locale), e)
            } else {
                def errorMessage = getMessage(
                    "ctfRemoteClientService.listUsers.error", locale) + " " +
                    faultMsg
                log.error(errorMessage, e)
                throw new RemoteMasterException(ctfUrl, errorMessage, e)
            }
        }
    }

    /**
     * @param ctfUrl is the ctf server complete URL, including protocol, 
     * hostname and port.
     * @param sessionId is the user sessionId retrieved after logging in.
     * @return the list of projects in the TeamForge server reached by the
     * given ctfUrl, using the given sessionId.
     * @throws CtfSessionExpiredException if the given sessionId is expired.
     * @throws RemoteMasterException if any other error occurs during the
     * method execution.
     */
    def getProjectList(ctfUrl, sessionId, locale) throws RemoteMasterException {
        try {
            return getStrategy(ctfUrl).getProjectList(sessionId)

        } catch (AxisFault e) {
            String faultMsg = e.faultString
            GrailsUtil.deepSanitize(e)
            if (faultMsg.contains("Session is invalid or timed out")) {
                throw new CtfSessionExpiredException(ctfUrl, sessionId,
                    getMessage("ctfRemoteClientService.remote.sessionExpired", 
                        locale), e)
            } else {
                def errorMessage = getMessage(
                    "ctfRemoteClientService.listProjects.error", locale) + " " +
                    faultMsg
                log.error(errorMessage, e)
                throw new RemoteMasterException(ctfUrl, errorMessage, e)
            }
        }
    }

    /**
     * Creates a new User at the given TeamForge site identified by the given
     * URL. The session ID of the user with permissions to create user must
     * also be provided. The new user will be created with the given username,
     * password, email and real name. Other properties of the user is if he/she
     * will be granted Super User credentials, or it will be restricted. 
     * @param ctfUrl is the URL of the CTF server, with the protocol, hostname
     * and port number.
     * @param soapId is the sesion ID of an authenticated user with
     * permissions to create a new user.
     * @param username is the username of the new user.
     * @param password is the password for the new user.
     * @param email is the email address of the new user.
     * @param realName is the real name associated with the new user.
     * @param isSuperUser if the user must be granted the super user.
     * @param isRestrictedUser if the user is restricted.
     * @return a UserDO from the created user.
     * @throws CtfSessionExpiredException in case the given session Id is 
     * expired.
     * @throws RemoteMasterException in case the user exists, if illegal 
     * properties are provided, if the creation is denied for the given 
     * sessionId, or the site exceeded the limit to create new users or other
     * general errors.
     */
    public def createUser(ctfUrl, soapId, username, password, email, 
        realName, boolean isSuperUser, boolean isRestrictedUser, locale) 
            throws RemoteMasterException {

        return getStrategy(ctfUrl).createUser(soapId, username, password, email,
            realName, isSuperUser, isRestrictedUser, locale)
    }            
            
    /**
     * Checks for a project in CTF which matches the projectName or
     * projectPath parameters.  If found, returns the name of the project
     * as it exists in CTF, otherwise returns null
     */
    String projectExists(ctfUrl, sessionId, projectName, projectPath, locale)
        throws RemoteMasterException {

        def projects = this.getProjectList(ctfUrl, sessionId, locale)
        String realName
        for (p in projects) {
            if (projectName.toLowerCase() == p.title.toLowerCase() || 
                projectPath == p.path.substring(9)) {
                realName = p.title
                break
            }
        }
        realName
    }
        
    /**
     * Deletes a project in CTF. This is used to clean up after tests.
     * @param ctfUrl
     * @param username
     * @param password
     * @param projectName
     */
    def deleteProject(ctfUrl, soapId, projectName) {
        def cnSoap = cnSoap(ctfUrl)
        def projects = getProjectList(ctfUrl, soapId, Locale.getDefault())
        String projectId = null
        for (p in projects) {
            if (projectName == p.title.toLowerCase()) {
                projectId = p.id
                break
            }
        }
        getStrategy(ctfUrl).deleteProject(soapId, projectId)
    }

    /**
     * create a CTF project
     *
     * @param soapId the session id
     * @param ctfUrl base URL to CTF server
     * @param projectPath projectName as used in the URL a lower case version with no spaces
     * or other special characters
     * @param projectName the title for the project
     * @param desc description of the project's purpose
     * @return the object id of the new project
     */
    String createProject(ctfUrl, soapId, projectPath, projectName, desc) {
        def p = cnSoap(ctfUrl).createProject(soapId, projectPath, projectName, desc)
        return p.id
    }

    def createRepository(ctfUrl, soapSessionId, projectId, systemId, repoName,
            desc, idRequiredOnCommit, hideMonitoringDetails) {
    
        getStrategy(ctfUrl).createRepository(soapSessionId, projectId, systemId,
            repoName, desc, idRequiredOnCommit, hideMonitoringDetails)
    }

    /**
     * Retrieves the queued commands from the CTF server identified by the
     * ctfUrl for the given replicaServerId.
     * @param ctfUrl is the url of the CTF system.
     * @param userSessionId is sessionID of the default user to call the soap.
     * @param replicaServerId is ID of the replica server.
     * @param runningCommands is the list of command results that haven't beent
     * transmitted yet. Those commands will avoid the replica manager send the
     * already executing commands and also to NOT count as a retransmission.
     * @param locale is the locale defined for error messages.
     * @return List of replica command execution with Id, command and repository
     * name, if any.
     * @throws RemoteMasterException if any error occurs during the method
     * call.
     * @throws NoRouteToHostException if the server started without connection,
     * acquired connection, and then lost it in the middle of execution.
     * @throws UnknownHostException if the server started with connectivity but
     * is unable to the host due to proxy, etc.
     */
    def getReplicaQueuedCommands(ctfUrl, userSessionId, replicaServerId, 
            runningCommands, locale) throws RemoteMasterException, 
            NoRouteToHostException, UnknownHostException {

        try {
            def runningCommandIds = []
            for (cmd in runningCommands) {
                if (ReplicationService.isCtfCommand(cmd.commandId)) {
                    runningCommandIds << cmd.commandId
                }
            }
            def soap = this.makeScmListenerClient(ctfUrl)
            def queuedCommands = (String[]) soap.invoke("getReplicaQueuedCommands", 
                [userSessionId, replicaServerId, runningCommandIds as String[]])

            def cmdsList = []
            if (queuedCommands && queuedCommands.length > 1) {
                log.debug("Mapping queued commands: " + queuedCommands.toArrayString())
                def keyMap = [id: 'id', command: 'code', repository_name: 'repoName']
                def queuedCmd, cmdParams
                for (int i = 0; i < queuedCommands.length - 1; i++) {
                    def key = queuedCommands[i]
                    if (key == ';') { 
                        queuedCmd = [:]
                        cmdParams = [:]
                        queuedCmd.params = cmdParams
                        cmdsList << queuedCmd
                    } else {
                        def value = queuedCommands[++i]
                        if (key.startsWith('parameter.')) {
                            def paramName = key.substring(10)
                            cmdParams[paramName] = value
                        } else {
                            def mappedKey = keyMap.containsKey(key) ? keyMap[key] : key
                            queuedCmd[mappedKey] = value
                            // TODO: the repository name should be defined in the params
                            // TODO: Remove the repositoryName property when removed
                            if (mappedKey == 'repoName') {
                                cmdParams[mappedKey] = value
                            }
                        }
                    }
                }
            }
            def idComparator = [
                   compare: {a,b->
                       (a.id.replace(COMMAND_ID_PREFIX,"") as Integer) -
                           (b.id.replace(COMMAND_ID_PREFIX,"") as Integer)
                   }
                 ] as Comparator
            // sort the received commands by ID before returning.
            return cmdsList.sort(idComparator)

        } catch (AxisFault e) {
             String faultMsg = e.faultString

           if (e.detail instanceof NoRouteToHostException) {
                // server started without connection, recovered, and lost again
                // Network is unreachable
                def hostname = new URL(ctfUrl).host
                throw new NoRouteToHostException(getMessage(
                    "ctfRemoteClientService.host.unreachable.error", 
                    [hostname.encodeAsHTML()], locale))

           } else if (e.detail instanceof UnknownHostException) {
                 def hostname = new URL(ctfUrl).host
                 throw new UnknownHostException(getMessage(
                     "ctfRemoteClientService.host.unknown.error",
                     [hostname.encodeAsHTML()], locale))

           } else if (faultMsg.contains("Session is invalid or timed out")) {
                throw new CtfSessionExpiredException(ctfUrl, userSessionId,
                    getMessage("ctfRemoteClientService.remote.sessionExpired",
                    locale), e)

            } else if (faultMsg.contains("No such object: ${replicaServerId}") ||
                    faultMsg.contains(
                    "Invalid replica server object id: ${replicaServerId}")) {
                // this fault indicates that the replica server no longer exists
                // on the ctf instance (deleted) -- will respond by creating 
                // and executing the unregister command
                log.warn "This replica is no longer supported by the CTF " +
                    "master; reverting to standalone mode"
                // create a virtual command to unregister the replica
                def id = Math.round(Math.random() * 10000)
                return [[id: COMMAND_ID_PREFIX + id, code: "replicaUnregister"]]

            } else {
                 def generalMsg = getMessage(
                     "ctfRemoteClientService.general.error", [e.getMessage()],
                     locale)
                log.error(generalMsg, e)
                throw new RemoteMasterException(ctfUrl, generalMsg, e)
            }

        } catch (Exception e) {
             GrailsUtil.deepSanitize(e)
             // also no session, but log this one as it indicates a problem
             def generalMsg = getMessage(
                 "ctfRemoteClientService.general.error", [e.getMessage()],
                 locale)
             log.error(generalMsg, e)
             throw new RemoteMasterException(ctfUrl, generalMsg, e)
        }
    }

    /**
     * Uploads the result of a given command to the master.
     * @param ctfUrl is the url of the CTF system.
     * @param userSessionId is sessionID of the default user to call the soap
     * @param replicaServerId is ID of the replica server
     * @param commandId is the ID of the command execution
     * @param succeeded defines if the command succeeded or not.
     * @param locale is the locale defined for error messages.
     * @throws RemoteMasterException if any error occurs during the method
     * call.
     */
    def uploadCommandResult(ctfUrl, userSessionId, replicaServerId, commandId,
            succeeded, locale) throws RemoteMasterException {

        try {
            def soap = this.makeScmListenerClient(ctfUrl)
            soap.invoke("uploadCommandResult", [userSessionId, replicaServerId, commandId, succeeded])

        } catch (AxisFault e) {
            String faultMsg = e.faultString
            if (faultMsg.contains("Session is invalid or timed out") ||
                faultMsg.contains("Invalid user session")) {
                throw new CtfSessionExpiredException(ctfUrl, userSessionId,
                   getMessage("ctfRemoteClientService.remote.sessionExpired",
                        locale), e)
            } else {
                 throw new RemoteMasterException(ctfUrl, e.faultString, e)
            }

         } catch (Exception e) {
            GrailsUtil.deepSanitize(e)
            // also no session, but log this one as it indicates a problem
            def generalMsg = getMessage(
                "ctfRemoteClientService.uploadCommandResult.error", [commandId,
                    succeeded, replicaServerId], locale) + e.getMessage()
            log.error(generalMsg)
            throw new RemoteMasterException(ctfUrl, generalMsg, e)
        }
    }

    def updateReplicaUser(userSessionId, newCtfUsername) {

        def ctfServer = CtfServer.getServer()
        def ctfUrl = ctfServer.baseUrl

        def replicaServerId = ReplicaConfiguration.currentConfig.systemId
        def soap = this.makeScmListenerClient(ctfUrl)
        soap.invoke("updateReplicaUser", [userSessionId, replicaServerId, newCtfUsername])
    }

    private String encodeParameters(paramMap) {
        StringBuilder buf = new StringBuilder();
        for (def entry : paramMap.entrySet()) {
            buf.append('&').append(URLEncoder.encode(entry.key))
                .append('=').append(URLEncoder.encode(entry.value))
        }
        buf.substring(1)
    }        
    
    private void writeParameters(paramMap, conn) {
        conn.outputStream.withWriter "UTF-8", {
            it << encodeParameters(paramMap)
        }
    }
    
    private HttpURLConnection openPostUrl(String url, def paramMap, boolean followRedirect) 
            throws SSLHandshakeException {
        HttpURLConnection conn = setupConnection(url, paramMap, followRedirect)
        try {
            writeParameters(paramMap, conn)
        } catch (CertificateException ce) {
            log.debug("Trying again with default cert because of exception", ce)
            // try again in case CTF is using the default svnedge cert
            try {
                conn.disconnect()
            } catch (Exception e) {
                log.debug("Unable to close http connection.", e)
            }
            conn = setupConnection(url, paramMap, followRedirect)
            HttpsURLConnection httpsConn = (HttpsURLConnection) conn
            httpsConn.hostnameVerifier = new SvnEdgeCertHostnameVerifier()
            writeParameters(paramMap, conn)
        }
        return conn
    }

    private setupConnection(String url, def paramMap, boolean followRedirect) {
        NetworkConfiguration nc = networkingService.getNetworkConfiguration()
        HttpURLConnection conn = (nc?.httpProxyHost) ?
            (HttpURLConnection) new URL(url).openConnection(HttpProxyAuth.newInstance(new URL(nc.proxyUrl))) :
            (HttpURLConnection) new URL(url).openConnection()
        conn.setRequestMethod("POST")
        conn.setRequestProperty("Accept-Language", 'en');
        conn.setRequestProperty("Content-Type", 
            "application/x-www-form-urlencoded; charset=UTF-8");
        conn.setInstanceFollowRedirects(followRedirect)
        conn.setDoOutput(true)
        conn.setUseCaches(false);
        return conn
    }

    private void debugHeaders(conn) {
        String headerfields = conn.getHeaderField(0);
        log.debug headerfields
        log.debug "---Start of headers---"
        int i = 1;
        while ((headerfields = conn.getHeaderField(i)) != null) {
            String key = conn.getHeaderFieldKey(i);
            log.debug(((key == null) ? "" : key + ": ") + headerfields)
            i++;    
        }
    }

    /**
     * @param ctfUrl the main ctf url.
     * @return a login URL for a given CTF URL
     */
    private String buildCtfLoginUrl(ctfUrl) {
        return ctfUrl + "/sf/sfmain/do/login"
    }

    /**
     * @param ctfUrl is a ctf url, containing the protocol + hostname + port
     * @param ctfUsername is an existing username in the given ctfUrl
     * @param ctfPassword is a password associated with the given username.
     * @return Map with the keys [jsessionid, usessionid] after logging in
     * into the given ctfUrl with the given ctfUsername and ctfPassword
     */
    public def getCtfSessionIds(ctfUrl, ctfUsername, ctfPassword) 
            throws SSLHandshakeException {
        def requestParams = [username: ctfUsername, password: ctfPassword,
             sfsubmit: "submit"]
        def conn = openPostUrl(this.buildCtfLoginUrl(ctfUrl), requestParams, 
                false)

        this.debugHeaders(conn)

        def headers = conn.headerFields
        def cookieHeaders = headers.get("Set-Cookie")
        if (!cookieHeaders) {
            cookieHeaders = headers.get("set-cookie")
        }
        def sessionid = [jsessionid: null, usessionid: null]
        if (cookieHeaders) {
            cookieHeaders.each {
                def cookies = HttpCookie.parse(it)
                cookies.each { cookie ->
                    if (cookie.name == 'sf_auth') {
                        def v = cookie.value
                        def amp = v.indexOf('&')
                        sessionid.jsessionid = v.substring(amp + 1)
                        sessionid.usessionid = v.substring(0, amp)
                    }
                }
            }
        } else {
            log.debug "No cookies set in the response."
        }
        
        conn.disconnect()
        return sessionid
    }

    /**
     * Deletes an external system in CTF using the Web UI
     * 
     * @param ctfUrl is the ctf url
     * @param jsessionid is the jsessionid of a current opened session
     * @param externalSystemId is the external system id to be removed
     * @param errors is the collection of errors to be collected.
     * 
     * @return boolean if the system id has been deleted
     */
    private undoExternalSystemOnRemoteCtfServer(ctfUrl, jsessionid, 
            externalSystemId, errors, locale) {

        def delUrl = ctfUrl + "/sf/sfmain/do/selectSystems;jsessionid=" + 
            jsessionid
        def formParams = [sfsubmit: "delete", _listItem: externalSystemId]
        def conn = openPostUrl(delUrl, formParams, true) // follow redirects

        log.debug "response from deleting integration server"
        debugHeaders(conn)

        String page =  conn.inputStream.text
        def systemId = page.find('value="(exsy' + externalSystemId + ')"', 
                { match, id -> id })
        def exceptionIndex = page.indexOf("A TeamForge system error has occurred")
        if (systemId || exceptionIndex >= 0) {
            log.warn "Delete from CTF did not succeed\n\n" + page
            def msg = getMessage(
                "setupTeamForge.integration.recovery.incomplete", locale)
            errors << msg
            return false
        } else {
            return true
        }
    }
}

