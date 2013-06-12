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

import org.apache.axis.AxisFault
import com.collabnet.svnedge.console.AbstractSvnEdgeService;
import com.collabnet.svnedge.domain.User
import com.collabnet.svnedge.domain.integration.ApprovalState
import com.collabnet.svnedge.domain.integration.CtfServer
import com.collabnet.svnedge.domain.integration.ReplicaConfiguration
import com.collabnet.svnedge.util.SoapClient

import java.net.NoRouteToHostException
import java.net.UnknownHostException
import java.net.MalformedURLException
import javax.net.ssl.SSLHandshakeException

import com.collabnet.svnedge.domain.NetworkConfiguration

/**
 * Interface for using different soap versions
 */
public abstract class CtfRemoteStrategy {

    public static final int DEFAULT_TIMEOUT = 120 * 1000
    protected NetworkConfiguration networkConfiguration

    public abstract def makeCollabNetClient()
    public abstract def makeScmAppClient()
    public abstract def login(username, password, locale) throws CtfAuthenticationException, UnknownHostException, 
            NoRouteToHostException, MalformedURLException,
            SSLHandshakeException
    
    /**
    * Creates a new instance of SoapNamedValues based on the array lists
    * @param a name/value map
    * @return a new instance of SoapNamedValues
    */
   public abstract def makeSoapNamedValues(props)

    /**
    * Creates a new User at the given TeamForge site identified by the given
    * URL. The session ID of the user with permissions to create user must
    * also be provided. The new user will be created with the given username,
    * password, email and real name. Other properties of the user is if he/she
    * will be granted Super User credentials, or it will be restricted.
    * @param ctfUrl is the URL of the CTF server, with the protocol, hostname
    * and port number.
    * @param userSessionId is the sesion ID of an authenticated user with
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
   public abstract def createUser(userSessionId, username, password, email,
       realName, boolean isSuperUser, boolean isRestrictedUser, locale)
       throws RemoteMasterException

    protected def loginFaultHandler(e, ctfUrl, locale) 
            throws CtfAuthenticationException {
        def messageKey = "ctfRemoteClientService.auth.error"
        if (e.faultString.contains("password was set by an admin")) {
            messageKey = "ctfRemoteClientService.auth.needschange"
        }
        def msg = getMessage(messageKey, [ctfUrl.encodeAsHTML()], locale)
        log.debug(msg)
        throw new CtfAuthenticationException(msg, messageKey)
    }
            
    protected def separateMap(map) {
        String[] names, values
        if (!map) {
            names = values = new String[0]
        } else {
            names = new String[map.size()]
            values = new String[map.size()]
            int i = 0;
            for (def entry: map.entrySet()) {
                names[i] = entry.key
                values[i++] = String.valueOf(entry.value)
            }
        }
        return [names, values]
    }
    
    public abstract def getProjectList(sessionId) throws AxisFault 
    
    public abstract def createRepository(soapSessionId, projectId, systemId, repoName, desc,
        idRequiredOnCommit, hideMonitoringDetails)

    public abstract def deleteProject(soapId, projectId)
}

