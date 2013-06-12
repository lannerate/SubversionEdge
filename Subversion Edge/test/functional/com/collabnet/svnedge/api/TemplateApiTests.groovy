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

package com.collabnet.svnedge.api

import com.collabnet.svnedge.AbstractSvnEdgeFunctionalTests
import groovyx.net.http.RESTClient

class TemplateApiTests extends AbstractSvnEdgeFunctionalTests {
    
    def url = "/api/1/template"
    def svnRepoService
    def repoTemplateService

    void testTemplateGet() {
        
        // without auth, GET is protected
        get("${url}?format=xml")
        assertStatus 401
        
        // without admin auth, GET is protected
        get("${url}?format=json") {
            headers['Authorization'] = "Basic ${ApiTestHelper.makeUserAuthorization()}"
        }
        assertStatus 401

        // authorized request contains repo information
        get("${url}?format=json") {
            headers['Authorization'] = "Basic ${ApiTestHelper.makeAdminAuthorization()}"
        }
        assertContentContains '{"templates": ['
        assertContentContains "\"id\": 1"        
        assertContentContains "\"name\": \"Empty repository\""        
    }

    void testTemplatePut() {

        // create a template to update
        def testTemplate = ApiTestHelper.createTemplate(repoTemplateService, svnRepoService)

        // the new dump file
        def testDump = ApiTestHelper.createDumpFile(svnRepoService)


        def rest = new RESTClient( ApiTestHelper.getSchemeHostPort() + "/csvn/api/1/" )
        rest.headers["Authorization"] = "Basic ${ApiTestHelper.makeAdminAuthorization()}"
        def params = [ 'format': 'json']
        def resp = rest.put( path: "template/${testTemplate.id}",
                query: params,
                body: testDump,
                requestContentType: 'text/plain' )
        assert resp.status == 201
    }

    void testTemplatePost() {

        // the new dump file
        def testDump = ApiTestHelper.createDumpFile(svnRepoService)
        // the new repo template
        def testTemplateName = "FunctionalTestingTemplate " + Math.floor(Math.random() * 10000).toInteger()

        def rest = new RESTClient( ApiTestHelper.getSchemeHostPort() + "/csvn/api/1/" )
        rest.headers["Authorization"] = "Basic ${ApiTestHelper.makeAdminAuthorization()}"
        def params = [ 'format': 'json', 'name': testTemplateName, 'active': true]
        def resp = rest.post( path: "template",
                query: params,
                body: testDump,
                requestContentType: 'text/plain' )
        assert resp.status == 201

        // test that new repo template exists
        get("${url}?format=json") {
            headers['Authorization'] = "Basic ${ApiTestHelper.makeAdminAuthorization()}"
        }
        assertContentContains "\"name\": \"${testTemplateName}\""
    }
    
     void testTemplateUnsupportedMethods() {
        // unauthorized calls receive 401
        delete(url)
        assertStatus 401

        // authorized calls receive 405 (not implemented)
        delete(url) {
            headers['Authorization'] = "Basic ${ApiTestHelper.makeAdminAuthorization()}"
        }
        assertStatus 405

        // a template detail view is not yet supported
        get("${url}/1") {
            headers['Authorization'] = "Basic ${ApiTestHelper.makeAdminAuthorization()}"
        }
        assertStatus 405
    }
}
