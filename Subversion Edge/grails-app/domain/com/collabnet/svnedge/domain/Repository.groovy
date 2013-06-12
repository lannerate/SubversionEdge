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

import com.collabnet.svnedge.domain.statistics.StatValue 

/**
 * Repository domain class.
 */
class Repository {
    static final def RECOMMENDED_NAME_PATTERN = ~/[^<>\/\?\\;:'"`!@#%&\$\*\+)(\|\s]+/
    static final int NAME_MAX_LENGTH = 32

    /** Name */
    String name
    /**
     * PermissionsOk -- flag to indicate need for permissions fix-up
     */
    Boolean permissionsOk = true
    /**
     * Flag that will be set to false, if svnadmin verify fails
     */
    boolean verifyOk = true
    
    String cloudName
    String cloudSvnUri

    /**
     * Repo statistics are FK'd, so this is used for cascade delete 
     */
    static hasMany = [ statValues: StatValue ]

    /**
     * In the web UI we try to guide users to create URL and command-line
     * compatible repo names, but we don't want to eliminate the ability to 
     * discover repos which are created with non-matching names, so this can't be
     * a constraint on the object
     */
    boolean validateName(regex=RECOMMENDED_NAME_PATTERN) {
        def b = name.length() <= NAME_MAX_LENGTH
        if (b) {
            b = name.matches(regex)
            if (!b) {
                errors.rejectValue("name", "repository.name.matches.invalid")
            }
        } else {
            errors.rejectValue("name", "repository.name.size.toobig",
                [NAME_MAX_LENGTH].toArray(), "Name exceeds maximum length.")
        }
        b
    }

    static constraints = {
        name(blank: false, unique: true)
        cloudName(blank: true, nullable: true)
        cloudSvnUri(blank: true, nullable: true)
    }
}
