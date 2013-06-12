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
package com.collabnet.svnedge.domain



/**
 * This class represents lines from the subversion log file.
 */
class SvnLog {
    int lineNumber
    long timestamp
    // someday User might be a real object, but it's not currently.
    String username
    Repository repo
    String action
    String path
    // depending on the action, the revision might actually be a range  
    String revision

    static constraints = {
        repo(nullable:true)
    }

    /**
     * Returns true if the other is an SvnLog with the same values
     * for all the non-db (i.e. not id, version) values.
     */
    def valuesEqual(other) {
        if (other && other instanceof SvnLog) {
            if (other.lineNumber == this.lineNumber 
                && other.timestamp == this.timestamp 
                && other.username == this.username
                && other.repo == this.repo
                && other.action == this.action
                && other.path == this.path
                && other.revision == this.revision) {
                return true
            }
        }
        return false
    }
}
