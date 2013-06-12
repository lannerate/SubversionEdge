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
package com.collabnet.svnedge


import java.util.Locale;

import org.codehaus.groovy.grails.commons.ApplicationHolder;

/**
 * This exception is used to capture the error exceptions of starting the
 * Subversion server while another OS process is running on a given
 * port number.
 * 
 * @author Marcello de Sales (mdesales@collab.net)
 *
 */
class CantBindPortException extends SvnEdgeException {
    
    def appHolder = ApplicationHolder.application

    /**
     * The port number that was attempted to be used. 
     */
    def portNumber
    /**
     * The command used to create an OS process.
     */
    def command

    /**
     * Creates a new Can'tBindPortException with the given port number, message 
     * and the cause.
     * @param portNumber is the port number that was attempted to be used.
     * @param message is the exception message.
     * @param cause is the throwable cause.
     */
    public CantBindPortException(String command, String errorMessage) {
        super(errorMessage)
        this.command = command
        def regex = getRegExForErrorMessage(errorMessage)
        def matcher = (errorMessage =~ regex)
        this.portNumber = matcher[0][1]
    }

    /**
     * Different message handling.
     * @param errorMsg is the error message.
     * @return the pattern instance for the matcher.
     */
    def getRegExForErrorMessage(errorMsg) {
        if (errorMsg.contains("[::]")) {
            /*
             * (98)Address already in use: make_sock: 
             * could not bind to address [::]:18080 (98)
             */
            def pattern = /.....([0-9]+)/
            return pattern
        } else if (errorMsg.contains("open logs")  && 
                errorMsg.contains("no listening sockets")) {
            /*
             * (98)Address already in use: make_sock: could not bind
             * to address 0.0.0.0:18080\nno listening sockets available,
             * shutting down Unable to open logs 
             */
            def pattern = /\d+.\d+.\d+.\d+.([0-9]+)/
            return pattern
       } 
    }

    /**
     * Creates a new exception with the port number and the cause
     * 
     * @param portNumber is the port number that was attempted.
     * @param cause is a throwable that caused the error.
     */
    public CantBindPortException(int portNumber, Throwable cause) {
        super(cause)
        this.portNumber = portNumber
    }

    /**
     * Creates a new exception with the port number.
     * @param portNumber is the port number in use in the OS.
     */
    public CantBindPortException(int portNumber) {
        this.portNumber = portNumber
    }

    @Override
    public String getMessage() {
        def key = 'server.error.cantBindPort'
        def args = [this.portNumber] as String[]
        def appCtx = appHolder.getMainContext()
        return appCtx.getMessage(key, args, Locale.getDefault())
    }

    @Override
    public String getMessage(Locale locale) {
        def key = 'server.error.cantBindPort'
        def args = [this.portNumber] as String[]
        def appCtx = appHolder.getMainContext()
        return appCtx.getMessage(key, args, locale)
    }
}
