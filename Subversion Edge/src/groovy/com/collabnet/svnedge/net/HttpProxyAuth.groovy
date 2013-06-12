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
package com.collabnet.svnedge.net;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.Authenticator;
import java.net.PasswordAuthentication;

/**
 * The Proxy Auth class is used for authentication purposes during the
 * packages update. This code was inspired on the pkg version 
 * AuthProxy, not available in the current version of our pkg deployment.
 * 
 * @author Marcello de Sales (mdesales@collab.net)
 *
 */
class HttpProxyAuth extends Proxy {

    /**
     * The password authenticator holder, with the user's username
     * and password based on the url.
     */
    def pwdAuth

    /**
     * Constructs a new ProxyAuth instance with the socket address and default
     * type of HTTP.Type.HTTP.
     * 
     * @param socketAddress is the socketAddress from the URL.
     */
    private HttpProxyAuth(SocketAddress socketAddress) {
        super(Proxy.Type.HTTP, socketAddress)
    }

    /**
     * @return The username of the authentication on a String. It can be
     * also null if the username is not defined.
     */
    def getUsername() {
        return (this.pwdAuth?.getUserName() ?: null)
    }

    /**
     * @return The password provided on a String. It can be also null if the
     * password is not defined.
     */
    def getPassword() {
        if (this.pwdAuth?.getPassword()) {
            def sb = new StringBuilder("")
            this.pwdAuth.getPassword().each { 
                sb.append(it)
            }
            return sb.toString()

        } else return null
    }

    private HttpProxyAuth(url) {
        this(new InetSocketAddress(url.host, url.port))

        def userAndPassword = url.getUserInfo()
        //username:password
        if (userAndPassword && userAndPassword.length() > 0) {
            def username = userAndPassword.split(":", 2)[0]
            def password = userAndPassword.split(":", 2)[1]

            def pwdArray = password ? password.toCharArray() : null
            this.pwdAuth = new PasswordAuthentication(username, pwdArray)
        }
    }

    /**
     * Constructs a new ProxyAuth instance with a given URL.
     * @param url is the URL of the proxy server. It can contain any of the
     * valid formats for a URL, including HTTP or HTTPS for protocol and the
     * authentication information as the username and password. The following
     * is an example of the URL format.
     * 
     * <BR>
     * <b>protocol://username:password@hostname:port</b>
     * 
     * If the username and password are used, the default system's authenticator
     * is created with the cridential.
     */
    public static HttpProxyAuth newInstance(url) {
        if (!url) {
            throw new IllegalArgumentException("The URL must be provided")
        }
        def pa = new HttpProxyAuth(url)
        if (pa.username && pa.password) {
            Authenticator.setDefault(new HttpProxyAuthenticator(pa.pwdAuth));
        }
        return pa
    }

    @Override
    public String toString() {
        def ia = (InetSocketAddress)this.address();
        if (this.username == null) {
            return "http://" + ia.hostName + ":" + ia.port;
        } else {
            def sb = new StringBuilder("")
            this.password.toCharArray().each { 
                sb.append("*")
            }
            return "http://" + this.username + ":" + sb.toString() + "@" +
                ia.hostName + ":" + ia.port;
        }
    }
}
