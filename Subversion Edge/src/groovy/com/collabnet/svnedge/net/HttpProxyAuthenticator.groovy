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

import java.net.Authenticator;
import java.net.PasswordAuthentication;

/**
 * This class handles the proxy authentication with the provided username
 * and password from the 
 */
private class HttpProxyAuthenticator extends Authenticator {
    
    /**
     * The default password authenticator from the outter class.
     */
    private PasswordAuthentication pa
    
    public HttpProxyAuthenticator(PasswordAuthentication pwdAuthentication) {
        super()
        this.pa = pwdAuthentication
    }
    
    @Override
    public PasswordAuthentication getPasswordAuthentication() {
        if (this.getRequestorType() != Authenticator.RequestorType.PROXY) {
            return null;
        }
        if (!this.getRequestingProtocol().equalsIgnoreCase("http")) {
            return null;
        }
        if (!this.pa) {
            return null;
        }
        return this.pa
    }
}