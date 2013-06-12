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
package com.collabnet.svnedge.controller

import java.io.FileInputStream;
import java.text.MessageFormat;
import java.util.Properties;

import grails.test.ControllerUnitTestCase;

/**
 * The abstract SvnEdge controller tests contains basic methods used
 * by all the controller tests that needs to access the i18n messages and 
 * make use of the mock infrastructure. The Messages are accessed through a
 * metaclass property message, populated directly from the messages.properties.
 * 
 * This was a suggestion proposed by users in the grails tracker
 * http://jira.codehaus.org/browse/GRAILS-5926.
 * 
 * @author Marcello de Sales (mdesales@collab.net)
 *
 */
public abstract class AbstractSvnEdgeControllerTests extends ControllerUnitTestCase {

    def props

    protected void setUp() {
        super.setUp()

        props = new Properties()
        // TODO: support different languages.
        def stream = new FileInputStream("grails-app/i18n/messages.properties")
        props.load stream
        stream.close()

        mockI18N(controller)
    }

    /**
     * Loads the messages.properties instance.
     */
    def mockI18N = { controller ->
        controller.metaClass.message = { Map map ->
        if (!map.code)
            return ""
        if (map.args) {
            def formatter = new MessageFormat("")
            formatter.applyPattern props.getProperty(map.code)
            return formatter.format(map.args.toArray())
        } else {
            return props.getProperty(map.code)
            }
        }
    }
}
