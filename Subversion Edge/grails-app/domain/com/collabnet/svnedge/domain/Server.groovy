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
package com.collabnet.svnedge.domain

import com.collabnet.svnedge.domain.integration.CtfServer 
import com.collabnet.svnedge.util.ConfigUtil

/**
 * Defines the svn server and console. 
 * We expect there to be only one Server defined.
 */
class Server {

    static final String DEFAULT_SVN_BASE_PATH = '/svn'
    
    // this property represents the apache SSL state
    boolean useSsl = false;
    // this property represents the console SSL state
    Boolean useSslConsole = false;
    /**
     * When server is used as a replica, hostname uniquely identifies the 
     * Replica to the Master.  Care should be take when changing this value.
     */
    String hostname
    
    int port
    Integer authHelperPort
    String repoParentDir
    boolean defaultStart
    boolean allowAnonymousReadAccess
    boolean forceUsernameCase
    boolean ldapEnabled
    Boolean ldapEnabledConsole
    boolean fileLoginEnabled 
    String ldapServerHost
    int ldapServerPort
    String ldapAuthBasedn
    String ldapAuthBinddn
    String ldapAuthBindPassword
    String ldapLoginAttribute
    String ldapSearchScope
    String ldapFilter
    String ldapSecurityLevel
    boolean ldapServerCertVerificationNeeded
    boolean replica
    String adminName
    String adminEmail
    String adminAltContact
    String svnBasePath
    ServerMode mode = ServerMode.STANDALONE
    String dumpDir = ConfigUtil.dumpDirPath()

    static String getViewvcBasePath() {
        return "/viewvc"
    }

    String svnURL() {
        return urlPrefix() + getSvnBasePath() + "/"
    }
    
    String secureURL() {
        return urlPrefix() + "/secure/"
    }
    
    String viewvcURL(String repoName) {
        String url = null
        if (ServerMode.MANAGED == mode || ServerMode.REPLICA == mode) {
            def systemId = CtfServer.server.mySystemId
            url = urlPrefix() + '/viewvc/?root=' + repoName + '&system=' + systemId
        } else {
            url =  urlPrefix() + "/viewvc" + (repoName ? "/" + repoName : "") + "/"   
        }
        url
    }
    
    String urlPrefix() {
        def scheme = useSsl ? "https" : "http"
        String port = useSsl ? 
            (port == 443) ? "" : ":" + port : (port == 80) ? "" : ":" + port
        return scheme + "://" + hostname + port   
    }

    String ldapURL() {
        String ldapUrl = (server.ldapSecurityLevel != "NONE") ?
                'ldaps://' : 'ldap://'
        ldapUrl += ldapServerHost
        if (ldapServerPort != 389) {
            ldapUrl += ':' + ldapServerPort
        }
        ldapUrl += '/'
        return ldapUrl
    }

    static constraints = {
        hostname(nullable: false, blank: false, unique: true)
        port(min:80, max: 65535)
        repoParentDir(nullable: false, blank: false, 
                validator: { val, obj ->
                    def dirFile = new File(val)
                    def result = null
                    if (dirFile.exists()) {
                        if (!dirFile.isDirectory()) {
                            result = ['notADirectory']
                        }
                    } else if (val.startsWith('\\\\') || val.startsWith('//')) {
                        result = System.getProperty('user.name').toLowerCase()
                                .contains('system') ? 
                                ['systemUser'] : ['sharePermissions']           
                    } else {
                        result = ['doesNotExist']
                    }
                    return result
        })
        adminName(nullable: true)
        adminEmail(nullable: false, blank: false, email: true)
        adminAltContact(nullable: true)
        ldapServerHost(nullable: true, validator: { val, obj ->
            if (obj.ldapEnabled) {
                if (!val || val.equals("")) {
                    return ['blank']
                }
            }
        })
        ldapServerPort(min:1, max: 65535, validator: { val, obj ->
            if (obj.ldapEnabled) {
                if (!val || val.equals("")) {
                    return ['blank']
                }
            }
        })
        authHelperPort(nullable: true, min:1, max: 65535, validator: { val, obj ->
            if (obj.ldapEnabledConsole) {
                if (!val || val.equals("")) {
                    return ['blank']
                }
            }
        })
        ldapAuthBasedn(nullable: true)
        ldapAuthBinddn(nullable: true)
        ldapAuthBindPassword(nullable: true)
        ldapLoginAttribute(nullable: true)
        ldapSearchScope(nullable: true)
        ldapFilter(nullable: true)
        ldapSecurityLevel(nullable: true)
        mode(nullable:false)
        ldapEnabled(validator: { val, obj ->
            // Ensure that some authentication is chosen
            if (!val && !obj.fileLoginEnabled ) {
                return ['chooseAuth']
            }
        })
        dumpDir(nullable: false, blank: false, validator: { val, obj ->
            def dirFile = new File(val)
            return dirFile.exists() && dirFile.isDirectory()
        })
    }
    
    static Server getServer() {
        return Server.get(1)
    }
    
    boolean managedByCtf() {
        return (this.mode == ServerMode.MANAGED || 
                this.mode == ServerMode.REPLICA )       
    }
    
    boolean convertingToManagedByCtf() {
        return this.mode == ServerMode.CONVERTING_TO_MANAGED
    }
    
    AdvancedConfiguration advancedConfig() {
        return AdvancedConfiguration.getConfig()
    }
    
    static CtfServer getManagedServer() {
        return CtfServer.getServer()
    }

    /**
     * @return the port number of the console web application. That is, the
     * Jetty Server port number.
     */
    public static String getConsolePort() {
        return getConsolePort(false);
    }

    /**
     * @param ssl if true returns the ssl port if available, else the standard port
     * @return the port number of the console web application. That is, the
     * Jetty Server port number.
     */
    public static String getConsolePort(boolean ssl) {
        String portNumber = null;
        def propertyName = ssl ? "jetty.ssl.port" : "jetty.port"
        if (System.getProperty(propertyName)) {
            portNumber = System.getProperty(propertyName).trim()
        }
        // if the property is not found, and ssl was not requested,
        // provide the development port number
        if (!portNumber && !ssl) {
            portNumber = "8080"
        }
        return portNumber
    }

    String consoleUrlPrefix() {
        def scheme = useSslConsole ? "https" : "http"
        String port = ':' + getConsolePort(useSslConsole)
        return scheme + "://" + hostname + port + '/csvn'
    }
}

enum ServerMode { STANDALONE, CONVERTING_TO_MANAGED, MANAGED, REPLICA }

