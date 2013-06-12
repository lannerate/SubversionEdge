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
package com.collabnet.svnedge.admin.pkgsupdate;

import org.cometd.SecurityPolicy
import org.cometd.Message
import org.cometd.Client

/**
 * The security policy for the Packages Update Service that uses the Bayuex 
 * message bus to publish updates. It sets the security level to only accept
 * messages on the channel '/csvn-updates'.
 * 
 * @author Marcello de Sales (mdesales@collab.net)
 *
 */
class PackagesUpdateSecurityPolicy implements SecurityPolicy {

    /* (non-Javadoc)
     * @see org.cometd.SecurityPolicy#canHandshake(org.cometd.Message)
     */
    public boolean canHandshake(Message message) {
        return true
    }

    /* (non-Javadoc)
     * @see org.cometd.SecurityPolicy#canCreate(org.cometd.Client, java.lang.String, org.cometd.Message)
     */
    public boolean canCreate(Client client, String channel, Message message) {
        return true
    }

    /* (non-Javadoc)
     * @see org.cometd.SecurityPolicy#canSubscribe(org.cometd.Client, java.lang.String, org.cometd.Message)
     */
    public boolean canSubscribe(Client client, String channel, 
            Message message) {
        return true
    }

    /* (non-Javadoc)
     * @see org.cometd.SecurityPolicy#canPublish(org.cometd.Client, java.lang.String, org.cometd.Message)
     */
    public boolean canPublish(Client client, String channel, Message message) {
        // because channel can come in many shape, including control event 
        // (handshake)
        if (client && channel?.startsWith('/csvn-updates')) {
            return client.local
        }
        return true
    }
}
