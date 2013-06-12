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
package com.collabnet.svnedge.statistics

import com.collabnet.svnedge.domain.statistics.StatAction 
import com.collabnet.svnedge.domain.statistics.StatValue 
import com.collabnet.svnedge.domain.statistics.Statistic 

/**
 * This class handles deleteing old StatValue data.  How soon data is 
 * deleted depends on the StatAction associated with it's StatGroup. 
 */
class DeleteStatisticsService {

    boolean transactional = true

    def delete(StatAction action) {
        if (!action.getDelete()) {
            log.info("No delete associated with " + action)
            return
        }
        def deleteOlderThan = new Date().getTime() - 
            (1000 * action.getDelete().getSeconds())
        // only delete data who's interval matches this
        def intervalGrade = action.getCollect().getSeconds() * 1000
        List<Statistic> applicableStats = Statistic.findAllByGroup(action
                                                                   .getGroup())
        /* delete with an executeUpdate so that we don't have to pull
           objects into memory just to delete them */
        for (Statistic stat: applicableStats) {
            StatValue.executeUpdate("delete StatValue sv where " 
                                    + "sv.timestamp < ? and sv.statistic = ? " 
                                    + "and sv.interval = ?", 
                                    [deleteOlderThan, stat, intervalGrade] as Collection)
        }
    }
}
