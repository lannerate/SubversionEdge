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
 * This class contains information about when a statistic group's data
 * should be initially collected (raw collection interval), consolidated
 * (non-raw collection interval), and deleted.  Each StatGroup may have
 * many of these StatActions.
 */
class StatAction {
    static belongsTo = [ group: StatGroup ]
    Interval collect
    Interval delete
    // If this is a raw collection interval, the consolidateSource will
    // be null.  Otherwise, this will point to another StatAction that
    // should belong to the same StatGroup.  This StatActions collection
    // interval should be a multiple of it's sources.  For example, if
    // this StatAction is collected every 7 days, the source might be
    // collected every day (*7) or every hour (*7*24).
    // We also need to make sure that the source isn't deleted before we
    // can consolidate from it.
    StatAction consolidateSource

    static constraints = {
        collect(nullable:false)
        delete(nullable:true)
        group(nullable:true)
        consolidateSource(validator: { val, obj ->
            if (val == null) {
                return true
            }
            if (val.getGroup() != obj.getGroup()) {
                return 'group'
            }
            if (obj.getCollect().getSeconds() % \
                val.getCollect().getSeconds() != 0) {
                return 'multiple'
            }
            if (val.getDelete() != null && 
                val.getDelete().getSeconds() < obj.getCollect().getSeconds()) {
                return 'deleteTooSoon'
            }
            return true
        })
    }
}
