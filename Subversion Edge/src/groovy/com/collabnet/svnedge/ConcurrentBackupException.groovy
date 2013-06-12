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
package com.collabnet.svnedge

/**
 * An exception which may be thrown within a service to indicate an invalid input.
 */
class ConcurrentBackupException extends SvnEdgeRuntimeException {
    
    String field
    
    /**
     * Creates a ConcurrentBackupException with the given message.
     * @param message a String message
     */
    public ConcurrentBackupException(String msg) {
        super(msg)
    }
}
