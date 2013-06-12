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

import com.collabnet.svnedge.domain.statistics.Category;

import grails.test.*

class CategoryIntegrationTests extends GrailsUnitTestCase {

    protected void setUp() {
        super.setUp()
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testCreateCategory() {
        def categoryName = "Category for Test"
        def cat = new Category(name: categoryName)
        assertTrue("This category should validate.", cat.validate())
        if (!cat.save(flush:true)) {
            cat.errors.each {
                println it
            }
        }
        def savedCat = Category.findByName(categoryName)
        assertNotNull("The test category was not found!", savedCat)
    }

    void testCreateNoNameCategoryFail() {
        def categoryName = ""
        def cat = new Category(name: categoryName)
        assertFalse("This category should not validate.", cat.validate())
        cat.save(flush:true)
        def savedCat = Category.findByName(categoryName)
        assertNull("A category with no name should not be saved.", savedCat)
    }
}
