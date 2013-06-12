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

import com.collabnet.svnedge.domain.Repository 

class ReplicatedRepository {

    // Repository name
    Repository repo

    // The timestamp from the master during the last successful sync
    Long lastSyncTime = -1

    // The latest revision synced from the master
    Long lastSyncRev = -1

    // Whether syncing is enabled for this repo
    Boolean enabled = false

    // The status of the repo
    RepoStatus status = RepoStatus.OK

    // Any message to further explain the status
    String statusMsg
    
    static constraints = {
        repo(unique:true)
        statusMsg (nullable:true)
    }

    static def listSortedByName(params) {
        def polarity = params.order && params.order == "desc" ?
            "desc" : "asc"
        return executeQuery(
            "select rr from ReplicatedRepository as rr inner join " + 
            "rr.repo as r order by r.name " + polarity,
            [max:params.max, offset:params.offset])
    }
}

enum RepoStatus {OK, OUT_OF_DATE, IN_PROGRESS, ERROR, DISABLED, REMOVED, NOT_READY_YET}
