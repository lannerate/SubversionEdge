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
package com.collabnet.svnedge.event


import org.springframework.context.ApplicationEvent

import com.collabnet.svnedge.console.DumpBean
import com.collabnet.svnedge.domain.Repository

/**
 * Spring event related to background repo dump
 */
class LoadRepositoryEvent extends RepositoryEvent {
    
    def LoadRepositoryEvent(source,
            Repository repo, boolean isSuccess, 
            Integer userId = null, Locale locale = null, 
            File processOutput = null, Exception e = null) {
        super(source, repo, isSuccess, userId, locale, processOutput, e)
        messagePrefix = "mail.message.load"
    }
}
