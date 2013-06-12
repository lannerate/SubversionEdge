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

import java.net.NoRouteToHostException
import java.net.UnknownHostException
import java.net.MalformedURLException
import javax.net.ssl.SSLHandshakeException

import grails.util.GrailsUtil
import org.apache.axis.AxisFault
import com.collabnet.ce.soap60.webservices.ClientSoapStubFactory
import com.collabnet.ce.soap60.webservices.cemain.ICollabNetSoap
import com.collabnet.ce.soap60.webservices.cemain.UserSoapDO
import com.collabnet.ce.soap60.webservices.scm.IScmAppSoap;
import com.collabnet.ce.soap60.types.SoapNamedValues;
import com.collabnet.ce.soap60.fault.IllegalArgumentFault;
import com.collabnet.ce.soap60.fault.InvalidSessionFault;
import com.collabnet.ce.soap60.fault.LoginFault
import com.collabnet.ce.soap60.fault.ObjectAlreadyExistsFault;
import com.collabnet.ce.soap60.fault.PermissionDeniedFault;
import com.collabnet.ce.soap60.fault.SystemFault;
import com.collabnet.ce.soap60.fault.UserLimitExceededFault;

import com.collabnet.svnedge.domain.integration.CtfServer 
import com.collabnet.svnedge.domain.integration.ReplicaConfiguration
import com.collabnet.svnedge.domain.NetworkConfiguration
import com.collabnet.svnedge.util.SoapClient

/**
 * Implements communication with CTF using the soap60 namespace
 */
public class Soap60CtfRemoteStrategy extends CtfRemoteStrategy {
    private final String url
    
    public Soap60CtfRemoteStrategy(String url, NetworkConfiguration networkConfiguration) {
        this.url = url ?: CtfServer.getServer().baseUrl
        this.networkConfiguration = networkConfiguration
    }
        
    public def makeCollabNetClient() {
        ClientSoapStubFactory.setConfig(SoapClient.getEngineConfiguration(this.networkConfiguration))
        return ClientSoapStubFactory.getSoapStub(ICollabNetSoap.class, url, DEFAULT_TIMEOUT)
    }

    public def makeScmAppClient() {
        ClientSoapStubFactory.setConfig(SoapClient.getEngineConfiguration(this.networkConfiguration))
        return ClientSoapStubFactory.getSoapStub(IScmAppSoap.class, url, DEFAULT_TIMEOUT)
    }

    public def login(username, password, locale)
            throws CtfAuthenticationException, UnknownHostException, 
            NoRouteToHostException, MalformedURLException,
            SSLHandshakeException {
        def soapId = null
        try {
            soapId = makeCollabNetClient().login(username, password)
        } catch(LoginFault e) {
            loginFaultHandler(e, url, locale)
        }
        return soapId
    }

    /**
     * Creates a new instance of SoapNamedValues based on the array lists
     * @param a name/value map
     * @return a new instance of SoapNamedValues
     */
    def makeSoapNamedValues(props) {
        def (names, values) = separateMap(props)
        def soapNamedValues = new SoapNamedValues()
        soapNamedValues.setNames(names);
        soapNamedValues.setValues(values);
        return soapNamedValues
    }
     
    /**
     * @see CtfRemoteStrategy#createUser
     */
    public def createUser(soapId, username, password, email,
            realName, boolean isSuperUser, boolean isRestrictedUser, locale)
            throws RemoteMasterException {
        // Ignores the password and uses 'null' instead, since this will 
        // trigger an email to the user to set their password.
        // Also not setting an organization
        try {
            def ctfSoap = makeCollabNetClient()
            return ctfSoap.createUser(soapId, username, email, realName,
                null, 'en', 'GMT', 'ANY', isSuperUser, isRestrictedUser, null)
            
        } catch (ObjectAlreadyExistsFault userExists) {
            GrailsUtil.deepSanitize(userExists)
            throw new RemoteMasterException(url, getMessage(
                "ctfRemoteClientService.createUser.alreadyExists",
                [url, username], locale), userExists)
            
        } catch (IllegalArgumentFault invalidProperties) {
            GrailsUtil.deepSanitize(invalidProperties)
            throw new RemoteMasterException(url, getMessage(
                "ctfRemoteClientService.createUser.invalidProps", [username],
                locale), invalidProperties)
            
        } catch (InvalidSessionFault sessionExpired) {
            GrailsUtil.deepSanitize(sessionExpired)
            throw new CtfSessionExpiredException(url, soapId,
                getMessage("ctfRemoteClientService.createUser.invalidProps",
                [username, soapId], locale), sessionExpired)
            
        } catch (PermissionDeniedFault permissionDenied) {
            GrailsUtil.deepSanitize(permissionDenied)
            throw new RemoteMasterException(url, soapId, getMessage(
                "ctfRemoteClientService.createUser.sessionIdHasNoPermission",
                [username, soapId], locale), permissionDenied)
            
        } catch (UserLimitExceededFault noMoreUsers) {
            GrailsUtil.deepSanitize(noMoreUsers)
            throw new RemoteMasterException(url, soapId, getMessage(
                "ctfRemoteClientService.createUser.limitExceeded",
                [username, soapId], locale), noMoreUsers)
            
        } catch (SystemFault generalProblem) {
            GrailsUtil.deepSanitize(generalProblem)
            def msg = getMessage(
                "ctfRemoteClientService.createUser.generalError", [username],
                locale)
            throw new RemoteMasterException(url, soapId, msg + " " +
                "${generalProblem.message}", generalProblem)
        }
    }

    def getProjectList(sessionId) throws AxisFault {
        makeCollabNetClient().getProjectList(sessionId, false).dataRows
    }
    
    def createRepository(soapSessionId, projectId, systemId, repoName, desc,
        idRequiredOnCommit, hideMonitoringDetails) {

    makeScmAppClient().createRepository(soapSessionId, projectId, systemId,
        repoName, repoName, desc, idRequiredOnCommit,
        hideMonitoringDetails, desc)
    }

    def deleteProject(soapId, projectId) {
        makeCollabNetClient().deleteProject(soapId, projectId, true, true)
    }
}

