/*
 * CollabNet Subversion Edge
 * Copyright (C) 2011, CollabNet Inc. All rights reserved.
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

/**
 * This class stores extra network configuration items, in particular the proxy settings.
 * Use NetworkingService.(get|save|remove)NetworkConfiguration() to handle persistence so 
 * the proxy auth password will be transparently encrypted/decrypted
 */
public class NetworkConfiguration {

    String httpProxyHost
    Integer httpProxyPort
    String httpProxyUsername
    String httpProxyPassword
    
    /**
     * convenience method to create proxy url from the persistent pieces
     * @return the complete proxy server url, or null if no host element is defined
     */
    String getProxyUrl() {
        if (httpProxyHost) {
            def hostPort = "${httpProxyHost}:${httpProxyPort}"
            if (httpProxyUsername) {
                return "http://${httpProxyUsername}:${httpProxyPassword}@${hostPort}"
            }
            else {
                return "http://${hostPort}"
            }
        }
        else {
            return null
        }
    }

    /**
     * convenience method to set the constituent fields from a single url
     * @param url
     */
    void setProxyUrl(String url) {
        URI uri =  new URI(url)
        httpProxyHost = uri.host
        httpProxyPort = uri.port > 0 ? uri.port : 80
        if (uri.userInfo) {
            def userInfoParts = uri.userInfo.split(":")
            httpProxyUsername = userInfoParts[0]
            httpProxyPassword = userInfoParts[1]
        }
        else {
            httpProxyUsername = null
            httpProxyPassword = null
        }
    }
    
    
     static constraints = {
        httpProxyHost(nullable: false, blank: false)
        httpProxyPort(nullable: false, min:1, max: 65535 )
        httpProxyUsername(nullable: true, blank: true)
        httpProxyPassword(nullable: true, blank: true)
     }

     static transients = [ "proxyUrl" ]

}

