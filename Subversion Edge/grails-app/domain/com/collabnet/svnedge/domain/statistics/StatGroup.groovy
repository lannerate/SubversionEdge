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
package com.collabnet.svnedge.domain.statistics


/**
 *  The class represents a group of statistics.  
 *  This group represents statistics that could be shown on the same
 *  graph.  For example, you might have a group called "Disk Space" which
 *  contains statistics for "free disk space", "used disk space", "total disk
 *  space", etc.
 *  In some instances, a statistic might be the lone member of a group.
 */
class StatGroup {
    // short name
    String name
    // title for a graph
    String title
    Unit unit
    boolean isReplica = false
    static hasMany = [ actions: StatAction, statistics: Statistic ]
    static belongsTo = [ category : Category ]

    static constraints = {
        name(blank:false, unique:true)
        title(blank:false)
        unit(nullable:false)
        category(nullable:false)
    }

    static mappings = {
        columns {
            actions lazy:false
        }
    }

    /**
     * Return the number of seconds associated with the raw collection
     * interval.
     */
    def getRawInterval() {
        def rawAction
        for (StatAction action: getActions()) {
            if (action.getConsolidateSource() == null) {
                rawAction = action
                break
            }
        }
        return rawAction?.getCollect()?.getSeconds()
    }

    /**
     * Return the shortest non-null delete interval in seconds.
     * May return null if no delete intervals are specified.
     */
    def getMinDeleteInterval() {
        def minDelete
        for (StatAction action: getActions()) {
            def deleteSec = action.getDelete()?.getSeconds()
            if (deleteSec && (!minDelete || deleteSec < minDelete)) {
                minDelete = deleteSec
            }
        }
        return minDelete
    }

    /**
     * Return the shortest non-raw collect interval in seconds.
     * May return null if no non-raw intervals exist.
     */
    def getMinConsolidateInterval() {
        def minConsolidate
        for (StatAction action: getActions()) {
            if (action.getConsolidateSource()) {
                def consSec = action.getCollect()?.getSeconds()
                if (consSec && (!minConsolidate || consSec < minConsolidate)) {
                    minConsolidate = consSec
                }
            }
        }
        return minConsolidate
    }

}
