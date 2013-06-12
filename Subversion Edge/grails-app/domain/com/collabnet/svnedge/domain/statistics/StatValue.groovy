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

import com.collabnet.svnedge.domain.Repository 

/** 
 * The value for a statistic at a particular point (well, interval actually) 
 * in time.  Basing this mostly off of how rrdtool functions, since we want
 * the same sort of thing: a round-robin database.
 * Depending on the type, we'll either store the values as a rate (for COUNTER)
 * or as the value itself (GAUGE).
 */
class StatValue {
    long timestamp
    // this interval is in msec (StatGroup is in sec)
    long interval
    /* We store min, max, average, and last value for each interval
     * This is useful for when we combine intervals together (i.e. derived 
     * values.
     */                                                          
    Double minValue
    Double maxValue
    Double averageValue
    Double lastValue
    boolean derived = false
    boolean uploaded = false
    static belongsTo = [ statistic: Statistic ]
    // Some statValues are associated with a particular repository
    Repository repo

    static constraints = {
        repo(nullable:true)
    }

    String toString() {
        "{" + super.toString() + ":: " +
        "id=" + id + "; " +
        "timestamp=" + timestamp + "; " +
        "interval=" + interval + "; " +
        "minValue=" + minValue + "; " +
        "maxValue=" + maxValue + "; " +
        "averageValue=" + averageValue + "; " +
        "lastValue=" + lastValue + "; " +
        "derived=" + derived + "; " +
        "uploaded=" + uploaded + "; " +
        "repo=" + (repo ? repo.name : "none") + "}"
    }
}
