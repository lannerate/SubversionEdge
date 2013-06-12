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
package com.collabnet.svnedge.domain.integration




/**
 * This class contains all the possible states that can exist between
 * a replica and its master.
 */
public enum ApprovalState {
    // the master has accepted this replica
    APPROVED("APPROVED"),

    // the master has not yet accepted this replica
    PENDING("PENDING"),

    // the master refused this replica
    DENIED("DENIED"),

    // this replica does not exist on the master
    NOT_FOUND("NOT_FOUND"),

    // an attempt to register the replica with the master failed
    REGISTRATION_FAILED("REGISTRATION_FAILED"),

    // this replica has been removed by the master
    REMOVED("REMOVED")

    String name

    public ApprovalState(String name) {
        this.name = name
    }

    static list() {
        [APPROVED, PENDING, DENIED, NOT_FOUND, REGISTRATION_FAILED]
    }
}
