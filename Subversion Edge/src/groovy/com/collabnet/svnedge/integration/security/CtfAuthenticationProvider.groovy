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
package com.collabnet.svnedge.integration.security

import java.net.ConnectException;

import com.collabnet.svnedge.domain.Server
import com.collabnet.svnedge.domain.ServerMode
import com.collabnet.svnedge.domain.integration.CtfServer;
import com.collabnet.svnedge.integration.CtfAuthenticationException;
import com.collabnet.svnedge.integration.CtfConnectionException;
import com.collabnet.svnedge.integration.CtfServiceUnavailableException;
import com.collabnet.svnedge.integration.RemoteMasterException;

import org.mortbay.log.Log;
import org.springframework.security.providers.ProviderNotFoundException;
import org.springframework.security.providers.AuthenticationProvider
import org.springframework.security.Authentication
import org.springframework.security.AuthenticationException
import org.springframework.security.BadCredentialsException
import org.codehaus.groovy.grails.plugins.springsecurity.GrailsUser
import com.collabnet.svnedge.admin.RepoDiscoveryJob
import org.apache.commons.logging.LogFactory

class CtfAuthenticationProvider implements AuthenticationProvider {

    boolean transactional = true
    def log = LogFactory.getLog(CtfAuthenticationProvider.class)

    // Because of the way we're defining this as a bean in resources.groovy,
    // any injected beans need to be set explicitly there
    def ctfRemoteClientService

    Authentication authenticate(Authentication authentication) 
            throws AuthenticationException {

        GrailsUser gUser = null
        try {
            gUser = ctfRemoteClientService.authenticateUser(
                authentication.getPrincipal(), authentication.getCredentials())

            // if ROLE_ADMIN is granted and mode is MANAGED, kick off async repo discovery
            // if ROLE_ADMIN is not granted, throw auth exception
            if (Server.server.mode == ServerMode.MANAGED) {
                if (gUser.authorities.find { it.authority == "ROLE_ADMIN" }) {
                    RepoDiscoveryJob.triggerNow([:])
                }
            }

        } catch (CtfServiceUnavailableException connectivityError) {
            log.warn(connectivityError.message)
            throw new ProviderNotFoundException(connectivityError.getMessage())

        } catch (CtfAuthenticationException connectivityError) {
            log.warn(connectivityError.message)
            throw new BadCredentialsException(connectivityError.getMessage())
        
        } catch (Exception otherError) {
            def otherMsg = "Other problem occurred while contacting the " +
                "teamforge manager: " + otherError.getMessage()
            log.warn(otherMsg)
            throw new BadCredentialsException(otherMsg)
        }
        if (!gUser) {
            throw new BadCredentialsException("Authentication failed for " + 
                authentication.getPrincipal())
        }
        new CtfAuthentication(true, gUser)
    }

    boolean supports(Class authentication) {
        return Server.getServer().managedByCtf()
    }
} 
