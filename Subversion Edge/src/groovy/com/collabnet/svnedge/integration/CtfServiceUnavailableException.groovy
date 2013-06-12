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
 * The communication with the TeamForge server can't be completed because
 * the server is down due to 504 or 503 HTTP errors.
 * 
 * @author Marcello de Sales (mdesales@collab.net)
 *
 */
class CtfServiceUnavailableException extends RemoteMasterException {

    /**
     * The key to the messages.properties used.
     */
    def messageKey

    /**
     * Creates a new exception with the given sessionId during the
     * communication with the CTF server identified by the given hostname.
     * 
     * @param sessionId is the sessionId based on the login to CTF.
     * @param hostname is the hostname of the CTF.
     * @param message is the error message.
     * @param cause is the original error message.
     */
    public CtfServiceUnavailableException(String hostname, String message, 
            Throwable cause) {

        super(hostname, message, cause)
    }

    /**
     * Creates a new exception with the message and the key to the originating
     * messages.property key.
     * @param message the message retrieved from the messages.properties.
     * @param key the key of the originating message if needed.
     */
    public CtfServiceUnavailableException(String message, String key) {
        super(message, null)
        this.messageKey = key
    }

    /**
     * Creates a new exception with the given sessionId during the
     * communication with the CTF server identified by the given hostname.
     * 
     * @param message is the error message.
     */
    public CtfServiceUnavailableException(String message) {
        super(message, null)
    }
}
