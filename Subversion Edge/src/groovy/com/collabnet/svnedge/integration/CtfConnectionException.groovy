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

import com.collabnet.svnedge.integration.RemoteMasterException

/**
 * This exception represents any general problem making a connection to the
 * the CTF Server
 */
class CtfConnectionException extends RemoteMasterException {

    /**
     * The key to the messages.properties used.
     */
    def messageKey

    /**
     * Creates a new exception with the message and the key to the originating
     * messages.property key.
     * @param message the message retrieved from the messages.properties.
     * @param key the key of the originating message if needed.
     */
    public CtfConnectionException(String message, String key) {
        super(message, null)
        this.messageKey = key
    }

    /**
     * Creates a new exception with the given sessionId during the
     * communication with the CTF server identified by the given hostname.
     * 
     * @param message is the error message.
     */
    public CtfConnectionException(String message) {
        super(message, null)
    }
}
