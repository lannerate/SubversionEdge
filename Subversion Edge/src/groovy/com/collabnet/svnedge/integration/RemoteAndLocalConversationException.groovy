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

import com.collabnet.svnedge.integration.RemoteMasterException;

/**
 * Some scenarios of communications between the Master server and this local
 * one requires the exchange of information through SOAP calls or HTTP requests
 * from both parties. This exception must be thrown when there is an error
 * during this communication. The details can be found on the error message.
 * 
 * @author Marcello de Sales (mdesales@collab.net)
 *
 */
class RemoteAndLocalConversationException extends RemoteMasterException {

    /**
     * Creates a new exception with the given hostname.
     * @param msg is the error message.
     */
    public RemoteAndLocalConversationException(String hostname, String msg) {
        super(hostname, msg, new Exception())
    }
}
