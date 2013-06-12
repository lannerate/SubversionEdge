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
package com.collabnet.svnedge.integration.security

import org.codehaus.groovy.grails.plugins.springsecurity.GrailsUser
import org.springframework.security.Authentication
import org.springframework.security.GrantedAuthority

class CtfAuthentication implements Authentication {

    boolean isAuthed = false
    GrailsUser principal

    CtfAuthentication(isAuthed, principal) {
        this.isAuthed = isAuthed
        this.principal = principal
    }

    Object getCredentials() {
        return this.principal.getCredentials()
    }

    Object getDetails() {
        return this.principal.getDetails()
    }

    Object getPrincipal() {
        return this.principal
    }

    GrantedAuthority[] getAuthorities() {
        return this.principal.getAuthorities()
    }

    String getName() {
        return this.principal.getUsername()
    }

    boolean isAuthenticated() {
        return this.isAuthed
    }

    void setAuthenticated(boolean isAuthenticated) {
        this.isAuthed = isAuthenticated
    }
}
