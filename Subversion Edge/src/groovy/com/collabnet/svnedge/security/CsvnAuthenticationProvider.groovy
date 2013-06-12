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
package com.collabnet.svnedge.security

import org.springframework.security.*
import org.springframework.security.providers.*
import org.springframework.security.userdetails.*

import org.codehaus.groovy.grails.plugins.springsecurity.GrailsUserImpl

import org.apache.commons.logging.LogFactory
import com.collabnet.svnedge.domain.Role 
import com.collabnet.svnedge.domain.Server 
import com.collabnet.svnedge.domain.User

import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.httpclient.HttpMethod

import org.apache.commons.httpclient.HttpStatus
import org.apache.commons.httpclient.Credentials
import org.apache.commons.httpclient.UsernamePasswordCredentials
import org.apache.commons.httpclient.auth.AuthScope
import org.apache.commons.httpclient.Header
import org.codehaus.groovy.grails.commons.ConfigurationHolder

/**
 * This class implements the SvnEdge Console authentication logic. If the Svn
 * server is configured for LDAP authentication, this provider will
 * test the user credentials against the LDAP server for Console access.
 * <p>
 * LDAP authentication is achieved by using the Apache server itself, so that the
 * authentication behavior of SVN and the console is identical. The
 * Apache server is configured to provide a special HTTP "auth helper" endpoint for
 * use by this provider, which is restricted to localhost access.
 * <p>
 * In addition to implementing the AuthenticationProvider interface, this
 * class can test a URL for the CollabNet auth realm and helps with
 * finding the port on which the auth helper listens. 
 *
 */
class CsvnAuthenticationProvider implements AuthenticationProvider {

    def log = LogFactory.getLog(CsvnAuthenticationProvider.class)

    public static final String AUTH_REALM = "CollabNet Subversion Repository"

    def daoAuthenticationProvider

    Authentication authenticate(Authentication auth) {

        log.debug("Attempting CSVN authentication")

        // if not ldap enabled, proceed to Acegi daoAuthProvider
        def server = Server.getServer()
        if (!server.ldapEnabled && !server.ldapEnabledConsole) {
            return daoAuthenticationProvider.authenticate(auth)
        }

        try {

            def authToken
            User.withTransaction { status ->

                log.debug("testing for user in database")
                def dbUser = User.findByUsername(auth.getName())

                // if dbuser found and is not from prior ldap auth, use standard DaoAuth
                if (dbUser && !dbUser.isLdapUser()) {
                    authToken = daoAuthenticationProvider.authenticate(auth)
                    return  // exit "withTransaction" closure
                }
                else {
                    // else, test the credentials, which throws exception on failure
                    validateCredentials(auth)
                }

                // create dbuser if not found (when this is first LDAP auth)
                if (!dbUser) {

                    log.debug("creating user entity to match ldap user")
                    dbUser = User.newLdapUser(username: auth.getName(),
                            realUserName: auth.getName())
                    dbUser.save()

                    // add new user to ROLE_USER
                    def roleUser = Role.findByAuthority("ROLE_USER")
                    roleUser.addToPeople(dbUser).save()
                    dbUser.authorities = [roleUser]
                }

                // create acegi auth artifacts
                GrantedAuthorityImpl[] authorities = dbUser.authorities.collect {new GrantedAuthorityImpl(it.authority)}
                def userDetails = new GrailsUserImpl(dbUser.username, dbUser.passwd, true, true, true, true, authorities, dbUser)
                authToken = new UsernamePasswordAuthenticationToken(userDetails, auth.credentials, userDetails.authorities)
            }

            return authToken
        }
        catch (BadCredentialsException bce) {
            log.debug("User credentials rejected for '${auth.getName()}': " + bce.getMessage());
            throw bce
        }
    }

    boolean supports(Class authentication) {
        return true
    }

    /**
     * This method attempts to access an apache secure location with the given credentials
     * @param auth
     * @throws BadCredentialsException if the secure location rejects the credentials
     * or some other transport error occurs
     */
    private void validateCredentials(Authentication auth) throws BadCredentialsException {

        def authHelperUrl = getAuthHelperUrl(Server.getServer())

        // test that the configured url can actually be used
        // for credential validation
        if (!testAuthListener(authHelperUrl)) {
            log.error("The auth helper url is not protected: " + authHelperUrl);
            throw new BadCredentialsException("The auth helper url is not usable: " + authHelperUrl)
        }

        HttpClient client = new HttpClient()
        HttpMethod method = new GetMethod(authHelperUrl)
        int statusCode

        try {
            
            // set authentication properties
            client.getParams().setAuthenticationPreemptive(true);
            Credentials creds = new UsernamePasswordCredentials(auth.principal, auth.credentials);
            client.getState().setCredentials(AuthScope.ANY, creds);

            // Execute the http method.
            statusCode = client.executeMethod(method);
        }
        catch (Throwable e) {
            log.error("Could not test credentials, fatal HTTP protocol error: " + e.getMessage(), e);
            throw new BadCredentialsException("The HTTP connection was not successful")
        }
        finally {
            // Release the connection.
            method.releaseConnection();
        }

        // validate that we successful authenticated
        if (statusCode != HttpStatus.SC_OK) {
            log.warn("Authentication against Apache failed")
            throw new BadCredentialsException("The credential was not accepted")
        }
    }

    /**
     * Returns the Apache Auth Helper url for use by this AuthProvider
     * @param server
     * @return String url
     */
    public String getAuthHelperUrl(Server server) {
        return "http://localhost:" + getAuthHelperPort(server, false)
    }

    /**
     * Returns the authHelper port currently configured. If "verify"
     * param is true, this method will check that the port returned 
     * is available or is in use by the running Apache. Set verify for true 
     * if the apache conf is being created, but otherwise false should
     * return the state of the previous call.
     * <p>
     * Server.authHelperPort will be updated by this method.  
     * @param server
     * @param verify check
     * @return
     */
    public String getAuthHelperPort(Server server, boolean verify) {

        // fetch the initial auth helper port (saved or use default)
        Integer authHelperPort = server.authHelperPort ?:
            ConfigurationHolder.config.svnedge.defaultApacheAuthHelperPort

        if (verify) {

            // test availability
            boolean portOk = false
            ServerSocket s = null
            while (!portOk) {
                try {
                    log.debug("attempting to bind auth helper to port: " + authHelperPort)
                    s = new ServerSocket(authHelperPort)
                    portOk = true
                    log.debug("succesful")
                }
                catch (Exception e) {

                    // might be in use already by apache itself
                    // in which case, assume we can continue to use
                    if (testAuthListener("http://localhost:" + authHelperPort)) {
                        portOk = true
                        log.debug("auth helper port '${authHelperPort}' is in use already by Apache (OK)")
                    }
                    else {
                        log.debug("auth helper port '${authHelperPort}' not available: " + e.getMessage())
                        ++authHelperPort
                    }
                }
                finally {
                    s?.close()
                }
            }
        }

        // save the port if it differs from what we know
        if (authHelperPort != server.authHelperPort) {
            server.authHelperPort = authHelperPort
            server.save()
        }

        return authHelperPort.toString()
    }

    /**
     * This method will verify that the given url is protected
     * by the CollabNet auth realm and can be used for testing
     * credentials
     * @param authelperUrl
     * @return
     */
    private boolean testAuthListener(String authHelperUrl) {

        HttpClient client = new HttpClient()
        HttpMethod method = new GetMethod(authHelperUrl)
        client.getParams().setSoTimeout(1000);

        int statusCode

        // if this is our server, expect 401 + CollabNet auth realm
        try {
            // Execute the http method.
            statusCode = client.executeMethod(method);

            Header h = method.getResponseHeader("WWW-Authenticate")
            String authHeader = h?.value

            if (statusCode == 401 && authHeader?.contains(AUTH_REALM)) {
                return true
            }
        }
        catch (Exception e) {
            log.info("Unable to test auth helper endpoint: " + e.getMessage())
        }
        return false
    }
}
