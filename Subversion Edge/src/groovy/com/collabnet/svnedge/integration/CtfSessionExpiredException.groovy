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


/**
* When the sessionId from the user expires during the execution of SOAP calls
* to the TeamForge server.
*
* @author Marcello de Sales (mdesales@collab.net)
*
*/
class CtfSessionExpiredException extends RemoteMasterException {

    /**
     * The sessionId that expired during the call to the SOAP interface.
     */
    def sessionId

    /**
     * Creates a new exception when the given sessionId timed out in the given
     * TeamForge server identified by the hostname.
     * 
     * @param hostname is the hostname of the CTF server.
     * @param sessionId is the expired sessionId.
     * @param message is the error message.
     * @param cause contains the cause of the session expiration.
     */
    public CtfSessionExpiredException(String hostname, String sessionId,
        String message, Throwable cause) {
 
       super(hostname, message, cause)
       this.sessionId = sessionId
    }
}
