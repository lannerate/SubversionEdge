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

package com.collabnet.svnedge.controller.api

import org.codehaus.groovy.grails.plugins.springsecurity.Secured
import grails.converters.JSON
import grails.converters.XML

import com.collabnet.svnedge.domain.Repository
import com.collabnet.svnedge.util.ControllerUtil
import com.collabnet.svnedge.util.ServletContextSessionLock

import com.collabnet.svnedge.ResourceLockedException

/**
 * REST API controller for managing repository hook scripts
 * <p><bold>URL:</bold></p>
 * <code>
 *   /csvn/api/1/hook
 * </code>
 */
@Secured(['ROLE_ADMIN', 'ROLE_ADMIN_HOOKS'])
class HookRestController extends AbstractRestController {
    
    def fileUtil
    def svnRepoService

    /**
     * <p>API to retrieve the directory listing for hooks. Subdirectories are
     * not included. File details include name, size, and last modified date.
     * Optional sort and order query parameters are available.</p>
     *
     * <p><bold>HTTP Method:</bold></p>
     * <code>
     *     GET
     * </code>
     *
     * <p><bold>XML-formatted response example:</bold></p>
     * <pre>
     * &lt;map&gt;
     *   &lt;entry key="hooks"&gt;
     *     &lt;map&gt;
     *       &lt;entry key="name"&gt;pre-commit&lt;/entry&gt;
     *       &lt;entry key="size"&gt;1024&lt;/entry&gt;
     *       &lt;entry key="lastModified"&gt;14314314103483721&lt;/entry&gt;
     *     &lt;/map&gt;
     *   &lt;/entry&gt;
     * &lt;/map&gt;
     * </pre>
     */
    def restRetrieve = {
        def result = [:]
        String filename = params.cgiPathInfo
        def repo = Repository.get(params.id)
        if (repo) {
            if (filename) {
                downloadHook(repo, filename)
                return null
            } else {
                result['hooks'] = listHooks(repo)
            }
        } else {
            response.status = 400
            result['errorMessage'] = message(code: "api.error.400.repository")
            log.debug("Invalid repository parameter: " + params.id)
        }
        
        withFormat {
            json { render result as JSON }
            xml { render result as XML }
        }
    }

    private def listHooks(repo) {
        def fileMapList = []
        ControllerUtil.setDefaultSort(params, "name")
        boolean isAscending = params.order == "asc"
        def fileList = svnRepoService.listHooks(repo, params.sort, isAscending)
        fileList.each { File f ->
            fileMapList << [name: f.name, 
                        size: f.length(), lastModifiedMillis: f.lastModified()]
        }
        return fileMapList
    }

    private void downloadHook(repo, filename) {
        // Not sure how sophisticated we need to be here, so just
        // treating any non-ascii file as binary
        File hookFile = svnRepoService.getHookFile(repo, filename)
        def contentType = fileUtil.isAsciiText(hookFile) ? "text/plain" :
                "application/octet-stream"
        response.setContentType(contentType)
        response.setHeader("Content-disposition",
                'attachment;filename="' + filename + '"')
        if (!svnRepoService.streamHookFile(repo, filename, response.outputStream)) {
            throw new FileNotFoundException()
        }
    }


    /**
     * <p>Rest method to create or replace a given repo hook script with the file contents
     * of the request. The request body is streamed in its entirety to a temporary file
     * and transferred to the repo hooks directory, and can be of any content type.</p>
     *
     * <p><bold>HTTP Method:</bold></p>
     * <code>
     *   PUT
     * </code>
     *
     * <p><bold>URL:</bold></p>
     * <code>
     *   /csvn/api/1/hook/{repoId}/{filename}
     * </code>
     */
    def restUpdate = {
        def result = [:]
        ServletContextSessionLock lock
        try {
            def repo = Repository.get(params.id)
            String destinationFileName = params.cgiPathInfo
            
            // validate that lock is available
            def lockToken = "repo:${repo.id};hookName:${destinationFileName}"
            lock = ServletContextSessionLock.obtain(session, lockToken)
            if (!lock) {
                throw new ResourceLockedException(message(code: "api.error.403.objectLocked"))
            }
            
            File uploadedFile = ControllerUtil.getFileFromRequest(request)
            
            if (!uploadedFile?.length()) {
                log.warn("File upload request contained no file data")
                throw new IllegalArgumentException(message(code: "api.error.400.missingFile"))
            }
            else {
                // "overwrite" param set to false will block replacement of existing file
                def success = (params["overwrite"] == "false") ? 
                        svnRepoService.createHook(repo, uploadedFile, destinationFileName) :
                        svnRepoService.createOrReplaceHook(repo, uploadedFile, destinationFileName)
                if (!success) {
                    response.status = 500
                    result['errorMessage'] = message(code: "api.error.500")
                    result['errorDetail'] = message(code: "api.error.500.filesystem")
                }
                else {
                    response.status = 201
                    result['message'] = message(code: "api.message.201")
                }
            }
        }
        catch (ResourceLockedException e) {
            response.status = 403
            result['errorMessage'] = message(code: "api.error.403")
            result['errorDetail'] = e.toString()
            log.warn("Exception handling a REST PUT/POST request", e)
        }
        catch (IllegalArgumentException e) {
            response.status = 400
            result['errorMessage'] = message(code: "api.error.400")
            result['errorDetail'] = e.toString()
            log.warn("Exception handling a REST PUT/POST request", e)
        }
        catch (Exception e) {
            response.status = 500
            result['errorMessage'] = message(code: "api.error.500")
            result['errorDetail'] = e.toString()
            log.warn("Exception handling a REST PUT/POST request", e)
        }
        
        lock?.release(session)
        withFormat {
            json { render result as JSON }
            xml { render result as XML }
        }
    }

    /**
     * <p>Rest method to create a given repo hook script with the file contents
     * of the request. The request body is streamed in its entirety to a temporary file
     * and transferred to the repo hooks directory, and can be of any content type.</p>
     * <p>If there is an existing file by the same name in the hooks directory, 
     * the method will return an error.</p>
     *
     * <p><bold>HTTP Method:</bold></p>
     * <code>
     *   POST
     * </code>
     *
     * <p><bold>URL:</bold></p>
     * <code>
     *   /csvn/api/1/hook/{repoId}/{filename}
     * </code>
     */
    def restSave = {
        params["overwrite"] = "false"
        restUpdate()
    }

    /**
    * <p>Rest method to delete a given repo hook script.</p>
    *
    * <p><bold>HTTP Method:</bold></p>
    * <code>
    *   DELETE
    * </code>
    *
    * <p><bold>URL:</bold></p>
    * <code>
    *   /csvn/api/1/hook/{repoId}/{filename}
    * </code>
    */
   def restDelete = {
       def result = [:]
       try {
           def repo = Repository.get(params.id)
           String filename = params.cgiPathInfo
           
           def success = svnRepoService.deleteHookFile(repo, filename)
           if (success) {
               response.status = 200
               result['message'] = message(code: "api.message.200")
           } else {
               response.status = 500
               result['errorMessage'] = message(code: "api.error.500")
               result['errorDetail'] = message(code: "api.error.500.filesystem")
           }
       }
       catch (FileNotFoundException e) {
           response.status = 500
           result['errorMessage'] = message(code: "api.error.500.filesystem")
           result['errorDetail'] = e.toString()
           log.warn("Exception handling a REST DELETE request", e)
       }
       catch (Exception e) {
           response.status = 500
           result['errorMessage'] = message(code: "api.error.500")
           result['errorDetail'] = e.toString()
           log.warn("Exception handling a REST DELETE request", e)
       }

       withFormat {
           json { render result as JSON }
           xml { render result as XML }
       }
   }

}
