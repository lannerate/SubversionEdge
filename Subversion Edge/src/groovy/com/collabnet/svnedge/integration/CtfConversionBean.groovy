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
package com.collabnet.svnedge.integration


class CtfConversionBean extends CtfConnectionBean {

    String exSystemId
    String ctfProject
    String ctfProjectId
    String ctfProjectPath
    boolean isProjectPerRepo
    String errorMessage
    boolean importUsers = true
    boolean assignMembership = true
    boolean requiresServerKey = false
    boolean consoleSsl = false
    int consolePort

    boolean lowercaseRepos
    String repoPrefix


    boolean validateProjectName() {
        isProjectPerRepo || (null != ctfProject && ctfProject.trim().length() > 0)
    }

    static constraints = {
        ctfURL (blank:false)
        ctfUsername (blank:false)
        ctfPassword (blank:false)
        repoPrefix (blank:true, matches: "[a-z]*")
    }

    def void clearCredentials() {
        this.ctfUsername = null
        this.ctfPassword = null
    }
}
