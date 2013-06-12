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
package com.collabnet.svnedge.integration.command

import java.util.concurrent.atomic.AtomicInteger;


/**
 * The Commands Execution Context wraps up the details about the command 
 * execution in a given session. It maintains the references of the connection
 * with the Replica manager, application context, etc. Those references are
 * needed for the initial communication with TeamForge, creation of command
 * instances, reporting command results, logging, etc.
 * 
 * @author Marcello de Sales (mdesales@collab.net)
 */
final class CommandsExecutionContext {

    /**
     * The user's locale during the command execution, retrieved from the
     * browser or the system.
     */
    def locale
    /**
     * The base URL of the CTF server.
     */
    def ctfBaseUrl
    
    /**
     * The user's session ID retrieved during the initial connection.
     */
    private String userSessionId
    
    public synchronized String getUserSessionId() {
        return userSessionId
    }

    public synchronized void setUserSessionId(String sessionId) {
        userSessionId = sessionId
    }

    /**
     * The soap session ID retrieved during the initial connection.
     */
    private String soapSessionId
    
    public synchronized String getSoapSessionId() {
        return soapSessionId
    }

    public synchronized void setSoapSessionId(String sessionId) {
        soapSessionId = sessionId
    }
    
    /**
     * The replica server ID.
     */
    def replicaSystemId
    /**
     * The Grails' app context, which is needed to get instances of services
     * in the command classes, among others.
     */
    def appContext
    /**
     * The directory where the commands execution logs must be saved.
     */
    def logsDir

    AtomicInteger activeCommands
    
    @Override
    public String toString() {
        return "CommandsExecutionContext [locale=" + locale + ", ctfBaseUrl=" +
               ctfBaseUrl + ", userSessionId=" + userSessionId +
               ", soapSessionId=" + soapSessionId +
               ", replicaSystemId=" + replicaSystemId + ", appContext=" +
               appContext + ", logsDir=" + logsDir + ", activeCommands=" + 
               activeCommands?.get().toString() ?: "null" + "]";
    }
}
