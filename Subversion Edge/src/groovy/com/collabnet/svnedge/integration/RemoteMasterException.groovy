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

import com.collabnet.svnedge.SvnEdgeException


/**
 * Any problem related to a Remote Master server such as an integration
 * or a future Master server.
 * 
 * @author Marcello de Sales (mdesales@collab.net).
 *
 */
class RemoteMasterException extends SvnEdgeException {

    /**
     * The hostname of the remote master that had the communication problem.
     */
    def hostname

    /**
     * The key to the messages.properties used.
     */
    def messageKey

    /**
     * Creates a new exception with the given hostname, error message and
     * the cause.
     * @param hostname is the hostname of the Master remote commuication.
     * @param message is the error message.
     * @param cause is the original cause.
     */
    public RemoteMasterException(String hostname, String message, 
            Throwable cause) {
        super(message, cause)
        this.hostname = hostname
    }

    /**
     * Creates a new exception with the given error message.
     * @param message is the error message.
     * @param key is the messages key in the i18n messages.
     */
    public RemoteMasterException(String message, String key) {
        super(message)
        this.messageKey = key
    }
}
