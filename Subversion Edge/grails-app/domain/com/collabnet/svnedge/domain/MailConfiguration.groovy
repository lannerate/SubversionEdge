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

import javax.mail.internet.AddressException
import javax.mail.internet.InternetAddress

/**
 * Data on how to send mail. 
 * We expect there to be only one config object defined.
 */
class MailConfiguration {

    boolean enabled = false
    String serverName = "localhost"
    int port = 25
    String authUser
    String authPass
    MailSecurityMethod securityMethod = MailSecurityMethod.NONE
    MailAuthMethod authMethod = MailAuthMethod.NONE
    String fromAddress
    String repoSyncToAddress
    
    String createFromAddress() {
        String addr = fromAddress
        if (!addr && authUser?.contains('@')) {
            try {
                new InternetAddress(authUser)
                addr = authUser
            } catch (AddressException e) {
                // ignore and use default
            }
        }
        if (!addr) {
            String host = serverName
            if (host.startsWith('smtp.')) {
                host = host.substring(5)
            } else if (host.startsWith('exchange.')) {
                host = host.substring(9)
            }
            def username = authUser
            int atIndex = username?.indexOf('@') 
            if (username && atIndex > 0) {
                username = username.substring(0, atIndex)
            }
            if (!username) {
                username = 'SubversionEdge' 
            }   
            addr = username + '@' + host
        }
        return addr
    }
    
    static constraints = {
        serverName(validator: isBlank, unique: true, matches: "\\S+", nullable: true)
        port(validator: isBlank, range: 1..65535)
        authUser(nullable:true)
        authPass(nullable: true)
        authMethod(nullable:true)
        securityMethod(nullable:false)
        fromAddress(nullable:true, email: true)
        repoSyncToAddress(nullable:true, email: true)
    }
    
    private static def isBlank = { val, obj -> 
            return (val || val == 0 || !obj.enabled) ? null : ['blank']
    }
    
    static MailConfiguration getConfiguration() {
        def rows = MailConfiguration.list()
        return rows ? rows.last() : new MailConfiguration()
    }
}

enum MailAuthMethod { NONE, PLAINTEXT, ENCRYPTED, NTLM, KERBEROS }
enum MailSecurityMethod { NONE, STARTTLS, SSL }

