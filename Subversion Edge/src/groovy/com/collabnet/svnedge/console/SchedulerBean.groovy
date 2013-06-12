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
package com.collabnet.svnedge.console

import java.util.List;
import java.util.Map;

/**
 * A command bean to hold all the parameters for an svnadmin dump with filtering
 */
public class SchedulerBean {
    static enum Frequency { NOW, ONCE, HOURLY, DAILY, WEEKLY, MONTHLY }
    static enum DAY_OF_WEEK { SUN, MON, TUE, WED, THU, FRI, SAT }
    
    // null value should be handled the same as NOW
    Frequency frequency
    int second = -1
    int hour = -1
    int minute = -1
    // the rest of these are 1-based
    int dayOfMonth = -1
    int month = -1
    int year = -1
    // Sunday = 1, Saturday = 7
    int dayOfWeek = 1

    // list of fieldnames to facilitate conversion to/from a JobDataMap for use
    // in Quartz scheduling
    static List propertyNames = [
        "second",
        "hour",
        "minute",
        "dayOfMonth",
        "month",
        "year",
        "dayOfWeek"
    ]

    /**
    * convenience method to create a SchedulerBean from a Map
    * @param m map
    * @return SchedulerBean instance
    */
   static SchedulerBean fromMap(Map m) {
       SchedulerBean b = new SchedulerBean()
       propertyNames.each { it ->
           def mapValue = m.get("schedule." + it)
           if (mapValue != null) b."${it}" = mapValue
       }
       b.frequency = (m.get("schedule.frequency") != null) ? Frequency.valueOf(m.get("schedule.frequency")) : null
       return b
   }
   
   /**
    * convenience method to create a Map from SchedulerBean
    * @return Map of the bean's properties
    */
   Map toMap() {
       Map m = [:]
       propertyNames.each { it ->
           def beanValue = this."${it}"
           m.put("schedule." + it, beanValue)
       }
       m.put("schedule.frequency", frequency.toString())
       return m
   }
}