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
package com.collabnet.svnedge.controller

import org.codehaus.groovy.grails.plugins.springsecurity.RedirectUtils

import org.springframework.security.DisabledException
import org.springframework.security.context.SecurityContextHolder as SCH
import org.springframework.security.providers.ProviderNotFoundException;
import org.springframework.security.ui.AbstractProcessingFilter
import org.springframework.security.ui.webapp.AuthenticationProcessingFilter
import com.collabnet.svnedge.domain.Server

/**
 * Login Controller (Example).
 */
class LoginController {

    /**
     * Dependency injection for the authentication service.
     */
    def authenticateService
    def lifecycleService

    def index = {
        if (isLoggedIn()) {
            redirect uri: '/'
        } else {
            redirect action: auth, params: params
        }
    }

    /**
     * Show the login page.
     */
    def auth = {

        nocache response

        if (isLoggedIn()) {
            redirect uri: '/'
            return
        }
        def config = authenticateService.securityConfig.security
        String postUrl = "${request.contextPath}${config.filterProcessesUrl}"

        render view: 'auth', model: [postUrl: postUrl]
    }
    
    // Login page (function|json) for Ajax access. This method is just an example
    // but it is used in the upgrade process to determine when the server has
    // restarted.
    def authAjax = {
        nocache(response)
        //this is example:
        render """
                <script type='text/javascript'>
                (function() {
                        loginForm();
                })();
                </script>
                """
    }

    /**
     * Show denied page.
     */
    def denied = {
    }

    /**
     * login failed
     */
    def authfail = {

        def username = session[AuthenticationProcessingFilter.SPRING_SECURITY_LAST_USERNAME_KEY]
        def msg = ''
        def exception = session[AbstractProcessingFilter.SPRING_SECURITY_LAST_EXCEPTION_KEY]
        if (exception) {
            if (exception instanceof DisabledException) {
                msg = message(code: 'user.disabled', args: [username])

            } else if (exception instanceof ProviderNotFoundException) {
                msg = exception.getMessage()

            } else if (Server.getServer().ldapEnabledConsole && 
                    !lifecycleService.isStarted()) {

                log.warn("LDAP authentication is enabled, but apache server " +
                    "is not running.")
                msg = message(code: 'login.page.ldap.auth.server.not.started')

            } else {
                msg = message(code: 'user.credential.incorrect', 
                    args: [username])
            }
        }

        if (isAjax()) {
            render "{error: '${msg}'}"
        }
        else {
            flash.error = msg
            redirect action: auth, params: params
        }
    }

    /**
     * Check if logged in.
     */
    private boolean isLoggedIn() {
        return authenticateService.isLoggedIn()
    }

    private boolean isAjax() {
        return authenticateService.isAjax(request)
    }

    /** cache controls */
    private void nocache(response) {
        response.setHeader('Cache-Control', 'no-cache') // HTTP 1.1
        response.addDateHeader('Expires', 0)
        response.setDateHeader('max-age', 0)
        response.setIntHeader ('Expires', -1) //prevents caching at the proxy server
        response.addHeader('cache-Control', 'private') //IE5.x only
    }
}
