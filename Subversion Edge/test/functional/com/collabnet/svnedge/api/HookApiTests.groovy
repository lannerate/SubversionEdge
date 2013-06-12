/*
 * CollabNet Subversion Edge
 * Copyright (C) 2012, CollabNet Inc. All rights reserved.
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
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import static groovyx.net.http.ContentType.JSON
import com.collabnet.svnedge.console.SvnRepoService
import com.collabnet.svnedge.util.ConfigUtil
import groovyx.net.http.RESTClient
import groovyx.net.http.HttpResponseException
import org.apache.http.entity.FileEntity
import groovyx.net.http.ContentType

class HookApiTests extends AbstractSvnEdgeFunctionalTests {

    def svnRepoService
    def testRepo

    @Override
    protected void setUp() {
        //The web framework must be initialized.
        super.setUp()
        testRepo = ApiTestHelper.createRepo(svnRepoService)
    }

    @Override
    protected void tearDown() {
        svnRepoService.removeRepository(testRepo)
        svnRepoService.deletePhysicalRepository(testRepo)
        //The tear down method terminates all the web-related objects, and
        //therefore, must be performed in the end of the operation.
        super.tearDown()
    }


    void testHookPutText() {
        
        def testFile = new File(ConfigUtil.confDirPath, "httpd.conf.dist")

        def rest = new RESTClient( ApiTestHelper.getSchemeHostPort() + "/csvn/api/1/" )
        rest.headers["Authorization"] = "Basic ${ApiTestHelper.makeAdminAuthorization()}"
        def params = [ 'format': 'json']
        def resp = rest.put( path: "hook/${testRepo.id}/testScript.py",
                query: params,
                body: testFile,
                requestContentType: 'text/plain' )
        assert resp.status == 201
    }

    void testHookPutBinary() {

        def testFile = new File(ConfigUtil.svnPath())
    
        def rest = new RESTClient( ApiTestHelper.getSchemeHostPort() + "/csvn/api/1/" )
        rest.encoder.'application/octet-stream' = {
            def entity = new FileEntity( (File) it, "application/octet-stream" );
            entity.setContentType( "application/octet-stream" );
            return entity
        }
        rest.headers["Authorization"] = "Basic ${ApiTestHelper.makeAdminAuthorization()}"
        def params = [ 'format': 'json']
        def resp = rest.put( path: "hook/${testRepo.id}/testScript.bin",
                query: params,
                body: testFile, 
                requestContentType: 'application/octet-stream' )
        assert resp.status == 201
    }

    void testHookPutWhenLocked() {

        // put to a locked location should result in 403 forbidden
        def testFile = new File(ConfigUtil.confDirPath, "httpd.conf.dist")
        
        // lock the file for editing via web ui
        def http = new RESTClient(ApiTestHelper.getSchemeHostPort() + "/csvn/")
        def httpResp = http.post (path: "j_spring_security_check",
                requestContentType: ContentType.URLENC,
                body: [j_username: 'admin', j_password: 'admin'])
        
        httpResp = http.post (path: "repo/index",
                requestContentType: ContentType.URLENC,
                body: ['_action_editHook': 'Edit', 'id': testRepo.id, 'listViewItem_post-commit.tmpl': 'on'])

        
        // now try REST PUT and expect 403 failure
        def rest = new RESTClient( ApiTestHelper.getSchemeHostPort() + "/csvn/api/1/" )
        rest.headers["Authorization"] = "Basic ${ApiTestHelper.makeAdminAuthorization()}"
        def params = [ 'format': 'json']
        try {
            def restResp = rest.put( path: "hook/${testRepo.id}/post-commit.tmpl",
                query: params,
                body: testFile,
                requestContentType: 'text/plain' )
            
            assert "This line should not be reached", false
        }
        catch (HttpResponseException e) {
            assert e.response.status == 403
        }
     }
    
    void testHookPost() {

        // post to a new file location should be successful
        def testFile = new File(ConfigUtil.confDirPath, "httpd.conf.dist")

        def rest = new RESTClient( ApiTestHelper.getSchemeHostPort() + "/csvn/api/1/" )
        rest.headers["Authorization"] = "Basic ${ApiTestHelper.makeAdminAuthorization()}"
        def params = [ 'format': 'json']
        def resp = rest.post( path: "hook/${testRepo.id}/testScript.py",
                query: params,
                body: testFile,
                requestContentType: 'text/plain' )
        assert resp.status == 201

        // post to an existing file location should be blocked
        try {
            resp = rest.post( path: "hook/${testRepo.id}/post-commit.tmpl",
                    query: params,
                    body: testFile,
                    requestContentType: 'text/plain' )
        }
        catch (HttpResponseException e) {
            assert e.response.status == 500
        }
    }
   
    void testHookDelete() {

        def hookName = "post-commit.tmpl"
        
        def rest = new RESTClient( ApiTestHelper.getSchemeHostPort() + "/csvn/api/1/" )
        rest.headers["Authorization"] = "Basic ${ApiTestHelper.makeAdminAuthorization()}"
        def params = [ 'format': 'json']
        def resp = rest.delete(path: "hook/${testRepo.id}/${hookName}",
                               query: params)
        assert resp.status == 200
        
        try {
            resp = rest.delete(path: "hook/${testRepo.id}/${hookName}",
                               query: params)
        } catch (HttpResponseException e) {
            assert e.response.status == 500
        }
    }
    
    private static final def HOOKS_DIR_CONTENTS = [
        'post-commit.tmpl', 'post-lock.tmpl', 'post-revprop-change.tmpl',
        'post-unlock.tmpl', 'pre-commit.tmpl', 'pre-lock.tmpl',
        'pre-revprop-change.tmpl', 'pre-unlock.tmpl', 'start-commit.tmpl']
    
    
    void testHookList() {
        def url = "/api/1/hook/" + testRepo.id
        
        // without auth, GET is protected
        get("${url}?format=xml")
        assertStatus 401
        
        // without admin auth, GET is protected
        get("${url}?format=json") {
            headers['Authorization'] = "Basic ${ApiTestHelper.makeUserAuthorization()}"
        }
        assertStatus 401

        // authorized request contains hooks directory contents
        get("${url}?format=json") {
            headers['Authorization'] = "Basic ${ApiTestHelper.makeAdminAuthorization()}"
        }
        assertContentContains '{"hooks": ['

        // check contents with a non-default sort
        def rest = new RESTClient( ApiTestHelper.getSchemeHostPort() + "/csvn/api/1/" )
        rest.headers["Authorization"] = "Basic ${ApiTestHelper.makeAdminAuthorization()}"
        def params = [format: 'json', sort: 'name', order: 'desc']
        def resp = rest.get(path: "hook/${testRepo.id}", query: params)
        assert resp.status == 200
        assert resp.contentType == JSON.toString()
        def fileList = resp.data['hooks']
        assertNotNull "Expected a list of file data", fileList
        assertEquals "Incorrect number of hook files", 
                HOOKS_DIR_CONTENTS.size(), fileList.size()        
        assertEquals "Hook scripts should be in descending alphabetical order",
                HOOKS_DIR_CONTENTS.reverse(), fileList.collect { it.name }
    }
    
    void testHookDownload() {
        def url = "/api/1/hook/" + testRepo.id + "/post-commit.tmpl"

        // authorized request contains hooks directory contents
        get(url) {
            headers['Authorization'] = "Basic ${ApiTestHelper.makeAdminAuthorization()}"
        }
        assertContentType "text/plain"
        assertContentStrict(svnRepoService.getHookFile(testRepo, "post-commit.tmpl").text)        
    }
}
