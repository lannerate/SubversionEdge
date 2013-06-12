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

import com.collabnet.svnedge.domain.Role
import org.codehaus.groovy.grails.plugins.springsecurity.Secured
import grails.converters.JSON
import grails.converters.XML
import com.collabnet.svnedge.domain.User
/**
 * REST API controller for listing user ROLEs and adding/removing users
 * <p><bold>URL:</bold></p>
 * <code>
 *   /csvn/api/1/role
 * </code>
 */
@Secured(['ROLE_ADMIN', 'ROLE_ADMIN_USERS'])
class RoleRestController extends AbstractRestController {
    
    def authenticateService

    /**
     * <p>API to retrieve the list of user roles.</p>
     *
     * <p><bold>HTTP Method:</bold></p>
     * <code>
     *     GET
     * </code>
     *
     * <p><bold>XML-formatted return example:</bold></p>
     * <pre>
     * &lt;map&gt;
     *   &lt;entry key="roles"&gt;
     *     &lt;map&gt;
     *       &lt;entry key="id"&gt;1&lt;/entry&gt;
     *       &lt;entry key="authority"&gt;ROLE_USER&lt;/entry&gt;
     *       &lt;entry key="description"&gt;Grants Subversion and ViewVC access&lt;/entry&gt;
     *     &lt;/map&gt;
     *   &lt;/entry&gt;
     * &lt;/map&gt;  
     * </pre>    
     */
    def restRetrieve = {
        def result
        def roles = []
        def params = [sort: "authority"]
        Role.list(params)?.each {
            def role = [id: it.id,
                    authority: it.authority,
                    description: it.description]
            roles.add(role)
        }
        result = [roles: roles]
        withFormat {
            json { render result as JSON }
            xml { render result as XML }
        }
    }

    /**
     * <p>Rest method to <code>add</code> or <code>remove</code> a given userId to a role. Use the 
     * <code>action</code> parameter to indicate whether to add or remove the userId.</p>
     *
     * <p><bold>URL:</bold></p>
     * <code>
     *   /csvn/api/1/role/${roleId}
     * </code>  
     *  
     * <p><bold>HTTP Method:</bold></p>
     * <code>
     *   PUT
     * </code>    
     * 
     * <p><bold>JSON-formatted request body example:</bold></p>
     * <pre>
     * {
     *   "action": "add",
     *   "userId": 2
     * }
     * </pre>
     */
    def restUpdate = {

        def result
        Role role
        User user
        String action
        
        // validate paramaters
        try {
            role = Role.get(params.id)
            user = User.get(Integer.parseInt(getRestParam("userId")))
            action = getRestParam("action")
            if (!role || !user || !action) {
                throw new Exception("bad parameters")
            }
            if (!action.equalsIgnoreCase("add") && !action.equalsIgnoreCase("remove")) {
                throw new Exception("bad parameters")
            }
        }
        catch (Exception e) {
            log.warn ("Missing or incorrect parameters for Role Update")
            response.status = 400
            result = [errorMessage: message(code: "api.error.400")]
            
        }
        // validate session credential
        if (!result && authenticateService.ifNotGranted("${role.authority},ROLE_ADMIN")) {
            response.status = 401
            result = [errorMessage: message(code: "api.error.401")]
        }

        if (!result) {
            switch(action.toLowerCase()) {
                case "add":
                    user.addToAuthorities(role)
                    break
                case "remove":
                    user.removeFromAuthorities(role)
                    break
            }
            
            user.save()
            response.status = 201
            result = [message: message(code: "api.message.201")]
        }
            
        withFormat {
            json { render result as JSON }
            xml { render result as XML }
        }
    }

}

