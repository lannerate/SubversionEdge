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
package com.collabnet.svnedge.domain

class RepoTemplate {
    public static final int STANDARD_LAYOUT_ID = 2
    
    String name
    String location
    boolean active
    boolean dumpFile
    int displayOrder
    
    static constraints = {
        name(nullable: false, blank: false, unique: true, minSize: 1, maxSize: 120)
        location(nullable: false, blank: false, unique: true, minSize: 1, maxSize: 255)
        displayOrder(min:0)
    }
}
