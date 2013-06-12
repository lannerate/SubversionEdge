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
 * This is a collection of statistic groups.  It's for high-level grouping.
 * For example, you might have a category called "System" that contains groups
 * for CPU, memory, disk space, etc.  or a category called "Svn Requests" that 
 * keeps track of number of users making svn requests, number of svn request
 * made, read/write requests, etc. 
 */
class Category {
    String name
    static hasMany = [ groups: StatGroup ]

    static constraints = {
        name(blank:false)
    }
}
