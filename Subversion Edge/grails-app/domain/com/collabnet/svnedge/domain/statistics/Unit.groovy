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
 * Represents a datatype's unit.  For instance, a datatype like "bytes" might
 * have min:0, max: null (unbounded), formatter: method that can turn large 
 * numbers of bytes in K, M, G, etc.
 */
class Unit {
    String name
    /* min and max are Integers, with null implying unbounded.
     * Assuming we won't need Floats here, but that may need to be changed 
     * later.
     */
    Integer minValue
    Integer maxValue
    /* This will be a String that references a method somehwere.
     * If it's null, than no formatting will be done.
     */ 
    String formatter
    
    static constraints = {
        name(blank:false, unique:true)
        minValue(nullable:true)
        maxValue(nullable:true)
        formatter(nullable:true)
    }
}
