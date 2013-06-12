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
import com.collabnet.svnedge.domain.User
import com.collabnet.svnedge.domain.Role
import org.springframework.validation.Errors

/**
 * REST API controller for creating and updating User accounts
 * <p><bold>URL:</bold></p>
 * <code>
 *   /csvn/api/1/user
 * </code>
 */
@Secured(['ROLE_ADMIN', 'ROLE_ADMIN_USERS'])
class UserRestController extends AbstractRestController {
    
    def authenticateService
    def lifecycleService

    /**
     * <p>API to create a Subversion Edge user with the given properties. 
     * On success, the new <code>userId</code> is returned.</p>
     *
     * <p><bold>HTTP Method:</bold></p>
     * <code>
     *     POST
     * </code>
     *
     * <p><bold>XML-formatted request body example:</bold></p>
     * <pre>
     * &lt;map&gt;
     *   &lt;entry key="username"&gt;new-repo&lt;/entry&gt;
     *   &lt;entry key="password"&gt;false&lt;/entry&gt;
     *   &lt;entry key="fullName"&gt;2&lt;/entry&gt;
     *   &lt;entry key="emailAddress"&gt;2&lt;/entry&gt;
     * &lt;/map&gt;
     * </pre>    
     *
     * <p><bold>XML-formatted return example:</bold></p>
     * <pre>
     * &lt;map&gt;
     *   &lt;entry key="message"&gt;Entity created or updated successfully&lt;/entry&gt;
     *   &lt;entry key="userId"&gt;42&lt;/entry&gt;
     * &lt;/map&gt;
     * </pre>    
     */
    def restSave = {
        def result = [:]

        def passwordClear = getRestParam("password")
        def passwordMd5 = (passwordClear) ? 
                authenticateService.encodePassword(passwordClear) :
                null

        def userInstance = new User(username: getRestParam("username"),
                passwd: passwordMd5,
                realUserName: getRestParam("fullName"),
                email: getRestParam("emailAddress"))
        userInstance.authorities= [Role.findByAuthority("ROLE_USER")]
        userInstance.validate()
        
        if (!userInstance.hasErrors()) {
            userInstance.save()
            lifecycleService.setSvnAuth(userInstance, passwordClear)
            response.status = 201
            result['message'] = message(code: "api.message.201")
            result['userId'] = userInstance.id
            log.info("User created via API: ${userInstance.username}")
        }
        else {
            userInstance.discard()
            response.status = 400
            result['errorMessage'] = message(code: "api.error.400")
            StringBuilder sb = new StringBuilder("Failed to create user '${getRestParam("username")}'")
            if (userInstance.errors) {
                def errorsFormatted = formatErrors(userInstance.errors)
                result['errorDetail'] = errorsFormatted
                sb.append(": ${errorsFormatted}")
            }
            log.warn(sb.toString())
        }
        
        withFormat {
            json { render result as JSON }
            xml { render result as XML }
        }
    }


}
