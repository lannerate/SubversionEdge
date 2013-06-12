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

import java.util.Map;

/**
 * A command bean to hold all the parameters for an svnadmin dump with filtering
 */
public class DumpBean {
    boolean compress = true
    String revisionRange
    boolean incremental
    boolean deltas
    boolean filter
    String includePath
    String excludePath
    boolean dropEmptyRevs
    boolean renumberRevs
    boolean preserveRevprops
    boolean skipMissingMergeSources
    Locale userLocale
    int numberToKeep = 0
    SchedulerBean schedule = new SchedulerBean()
    boolean backup
    Boolean cloud
    Boolean hotcopy

    // list of fieldnames to facilitate conversion to/from a JobDataMap for use
    // in Quartz scheduling
    private static List propertyNames = [
        "compress",
        "revisionRange",
        "incremental",
        "deltas",
        "filter",
        "includePath",
        "excludePath",
        "dropEmptyRevs",
        "renumberRevs",
        "preserveRevprops",
        "skipMissingMergeSources",
        "numberToKeep",
        "backup",
        "cloud",
        "hotcopy",
        "userLocale"
    ]

    Integer getLowerRevision() {
        Integer result = null
        if (revisionRange?.trim()) {
            String range = revisionRange.trim()
            int colonPos = range.indexOf(':')
            if (colonPos > 0) {
                result = range.substring(0, colonPos) as Integer
            } else if (colonPos < 0) {
                result = range as Integer
            } else {
                result = 0
            }
        }
        return result
    }    

    Integer getUpperRevision() {
        Integer result = null
        if (revisionRange?.trim()) {
            String range = revisionRange.trim()
            int colonPos = range.indexOf(':')
            if (colonPos >= 0) {
                result = range.substring(colonPos + 1) as Integer
            }
        }
        return result
    }    

    List<String> getIncludePathPrefixes() {
        return parsePaths(includePath)
    }
    
    List<String> getExcludePathPrefixes() {
        return parsePaths(excludePath)
    }
    
    private List<String> parsePaths(String paths) {
        List<String> result = null
        if (paths?.trim()) {
            String prefixes = paths.trim()
            List matches = prefixes.findAll(~/\b((?:\S|\\ )+)\b/, { match, path -> 
                return path.replace("\\", "").trim() })
            result = matches
        }
        return result
    }
    
    static constraints = {   
        
        revisionRange(blank: true, nullable: true, matches: "\\d+(?::\\d+)?",
                      validator: { val, obj ->
                          Integer upperRev = obj.getUpperRevision()
                          if (upperRev && upperRev < obj.getLowerRevision()) {
                              return ['lowerLimitExceedsUpperLimit']
                          }
                      })
    }
    
    /**
     * convenience method to create a DumpBean from a Map
     * @param m map
     * @return DumpBean instance
     */
    static DumpBean fromMap(Map m) {
        DumpBean b = new DumpBean()
        propertyNames.each { it ->
            def mapValue = m.get(it)
            if (mapValue || mapValue instanceof Boolean) {
                b."${it}" = mapValue
            }
        }
        b.schedule = SchedulerBean.fromMap(m)
        return b
    }
    
    /**
     * convenience method to create a Map from Scheduler/DumpBean
     * @return Map of the bean's properties
     */
    Map toMap() {
        Map m = [:]
        propertyNames.each { it ->
            def beanValue = this."${it}"
            m.put(it, beanValue)
        }
        m.putAll(this.schedule.toMap())
        return m
    }
}

