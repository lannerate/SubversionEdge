/*
 * CollabNet Subversion Edge
 * Copyright (C) 2013, CollabNet Inc. All rights reserved.
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

import javax.naming.Context
import javax.naming.NamingEnumeration
import javax.naming.NamingException
import javax.naming.directory.SearchControls
import javax.naming.directory.SearchResult
import javax.naming.ldap.InitialLdapContext
import javax.naming.ldap.LdapContext

import com.collabnet.svnedge.domain.Server
import com.collabnet.svnedge.domain.User

class LdapService {

    static transactional = true

    String ldapContextFactoryClassName = System.getProperty(
            "ldap.context.factory.classname", "com.sun.jndi.ldap.LdapCtxFactory")
    
    private Properties createEnvironment() {
        Server server = Server.getServer()
        Properties env = new Properties()
        env.setProperty(Context.INITIAL_CONTEXT_FACTORY, ldapContextFactoryClassName)
        env.setProperty(Context.PROVIDER_URL, server.ldapURL())
        env.setProperty(Context.OBJECT_FACTORIES, 
                "com.collabnet.svnedge.util.DirContextToMapObjectFactory")
        if (server.ldapAuthBindPassword) {
            env.setProperty(Context.SECURITY_PRINCIPAL, server.ldapAuthBinddn)
            env.setProperty(Context.SECURITY_CREDENTIALS, server.ldapAuthBindPassword)
        }
        return env
    }

    /**
     * Get the email for an ldap user
     * 
     * @return the user's email, may be null if not overridden and ldap cannot determine one
     */
    String getEmailForLdapUser(User user) {
        String email = user.email
        if (email == User.LDAP_PSEUDO_EMAIL) {
            email = getLdapEmailForUsername(user.username)
        }
        return email
    }
    
    private boolean isLdapSearchEnabled() {
        Server server = Server.getServer()
        boolean b = server.ldapEnabled && server.ldapEnabledConsole
        if (b) {
            try {
                Class.forName(ldapContextFactoryClassName)
            } catch (Exception e) {
                b = false
                log.info("LdapService is disabled, except for auth, as Jndi ldap factory cannot be loaded.")
                log.debug("Exception loading ldap jndi context factory", e)
            }
        }
        return b
    }

    private def emailPattern = ~/[A-Za-z0-9._%+-]+@(?:[A-Za-z0-9-]+\.)+[A-Za-z]{2,6}/

    /**
     * Searches LDAP for a mail record for the given username
     */
    String getLdapEmailForUsername(String username) {
        String email = null
        if (isLdapSearchEnabled()) {
            try {
                def user = searchUser(username)
                if (user) {
                    email = user['mail']
                    if (!email) {
                        user.values().each {
                            if (it instanceof String && emailPattern.matcher(it).matches()) {
                                if (!email) {
                                    email = it
                                } else {
                                   log.debug("found multiple email possibilities for " + username + 
                                             ": " + email + " and " + it + ", returning null ")
                                    // multiple values which might be email addresses, give up
                                    email = null
                                    return // from iteration closure
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn "Unable to retrieve email from LDAP for username=" + username + ". Set log level to DEBUG for detail."
                log.debug "Unable to retrieve email from LDAP for username=" + username, e
            }
        }
        return email
    }
    
    private def searchUser(String username) throws NamingException {
        Server server = Server.getServer()
        String filter1 = server.ldapFilter ?: '(objectClass=*)'
        if (!filter1.startsWith('(')) {
            filter1 = '(' + filter1 + ')'
        }
        String filter = '(&' + filter1 + '(' + (server.ldapLoginAttribute ?: 'uid') + '=' + username + '))'
        def userList = search(filter, server.ldapAuthBasedn, 
                (server.ldapSearchScope == 'one') ? SearchControls.ONELEVEL_SCOPE : SearchControls.SUBTREE_SCOPE)
        if (userList.isEmpty()) {
            return null
        } else if (userList.size() > 1) {
            throw new NamingException('LDAP returned multiple users with username=' + username)
        }
        
        return userList.get(0)
    }

    private List<Object> search(String filter, String base, 
            int searchScope = SearchControls.ONELEVEL_SCOPE) throws Exception
    {
        LdapContext ctx = null;
        List<Object> result = new ArrayList<Object>();
        try {
            ctx = new InitialLdapContext(createEnvironment(), null);

            SearchControls ctls = new SearchControls()
            ctls.setSearchScope(searchScope)
            ctls.setReturningObjFlag(true)
            
            NamingEnumeration<SearchResult> enm = ctx.search(base, filter, ctls)
            while (enm.hasMoreElements()) {
                SearchResult sr = enm.nextElement()
                result.add(sr.getObject())
            }
        } catch (Exception e) {
            log.warn "Unable to search ldap using filter: " + filter + " and base: " + base
            throw e
            
        } finally {
            try {
                ctx?.close();
            } catch (Exception e) {
                log.warn "Unable to close ldap context"
                log.debug "Unable to close ldap context (stacktrace)", e
            }
        }
        return result
    }

}
