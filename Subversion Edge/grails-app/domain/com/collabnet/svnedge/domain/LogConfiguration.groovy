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

import com.collabnet.svnedge.admin.LogManagementService.ConsoleLogLevel
import com.collabnet.svnedge.admin.LogManagementService.ApacheLogLevel
import com.collabnet.svnedge.util.ConfigUtil

/**
 * Defines logging parameters for svn server and console. 
 * We expect there to be only one LogConfiguration defined.
 */
class LogConfiguration {

    int pruneLogsOlderThan = 0
    ApacheLogLevel apacheLogLevel = ApacheLogLevel.WARN
    ConsoleLogLevel consoleLogLevel = ConsoleLogLevel.WARN
    int maxLogSize = 0
    boolean enableAccessLog = true
    boolean enableSubversionLog = true
    boolean enableLogCompression = false
    boolean minimizeLogging = false
    
    static constraints = {
        maxLogSize(nullable: false, min:0)
        pruneLogsOlderThan(nullable: false, min:0)
    }
    
    static LogConfiguration getConfig() {
        def config = LogConfiguration.get(1)
        if (!config) {
            config = new LogConfiguration()
            config.save()
        }
        return config
    }
}