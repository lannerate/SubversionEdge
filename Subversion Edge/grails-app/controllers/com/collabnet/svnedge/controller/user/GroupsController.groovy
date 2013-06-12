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
package com.collabnet.svnedge.controller.user

import java.util.List;

import com.collabnet.svnedge.domain.Role 
import com.collabnet.svnedge.domain.Groups
import com.collabnet.svnedge.domain.User 
import org.codehaus.groovy.grails.plugins.springsecurity.Secured


@Secured(['ROLE_ADMIN', 'ROLE_ADMIN_USERS'])
class GroupsController {

    def authenticateService

    def index = {
        redirect action: list, params: params
    }

    def list = {
        if (!params.max) {
            params.max = 10
        }
        [groupList: Groups.list(params), groupTotal: Groups.count(), activeButton: "user"]
    }

    def show = {
        def group = Groups.get(params.id)
        if (!group) {
            flash.message = "group not found with id $params.id"
            redirect action: list
            return
        }

        [groupInstance: group, activeButton: "user"]
    }

    def edit = {
        def group = Groups.get(params.id)
        if (!group) {
            flash.message = "group not found with id $params.id"
            redirect action: list
            return
        }

        [groupInstance: group, activeButton: "user", userList: getUserList()]
    }
	/**
	 * create group
	 */
	def create = {
		def group = new Groups()
		
		
		return [groupInstance: group, activeButton: "user", userList: getUserList(),roleList : getRoleList(),
				authorizedRoleList : getAuthorizedRoleList()]
	}

	
    /**
     * group update action.
     */
    def update = {

        def role = Role.get(params.id)
        if (!role) {
            flash.message = "Role not found with id $params.id"
            redirect action: edit, id: params.id
            return
        }

        if (authenticateService.ifNotGranted("${role.authority},ROLE_ADMIN")) {

            flash.error = "You do not have the authority to edit this role"
            redirect action: edit, id: params.id
            return
        }

        // add self to Role.people if already there (removed by Grails params mapping)
        def u = getActiveUser()
        boolean addSelf = role.people.contains(u)



        long version = params.version.toLong()
        if (role.version > version) {
            role.errors.rejectValue 'version', "role.optimistic.locking.failure",
                    "Another user has updated this Role while you were editing."
            render view: 'edit', model: [role: role, activeButton: "user"]
            return
        }

        role.properties = params
        // update members when all deleted
        if (!params.people) {
            role.people = []
        }
        // restore self to collection if needed
        if (addSelf) {
            role.addToPeople (u)
        }

        // do not remove the ROLE_USER role if other roles are granted to the user
        Role roleUser = Role.findByAuthority("ROLE_USER")
        def users = roleUser.people
        if (role == roleUser) {
            def admins = []
            admins.addAll(Role.findByAuthority("ROLE_ADMIN").people)
            admins.addAll(Role.findByAuthority("ROLE_ADMIN_REPO").people)
            admins.addAll(Role.findByAuthority("ROLE_ADMIN_USERS").people)
            admins.addAll(Role.findByAuthority("ROLE_ADMIN_SYSTEM").people)
            getUserList().each {
                if (admins.contains(it) && !users.contains(it)) {
                    roleUser.addToPeople(it)
                }
            }
        } else {            
            role.people.each {
                if (!users.contains(it)) {
                    roleUser.addToPeople(it)
                }
            }
        }

        if (role.save()) {
            flash.message = "The Role has been updated"
            redirect action: show, id: role.id
        }
        else {
            render view: 'edit', model: [role: role, activeButton: "user", userList: getUserList()]
        }
    }

    private User getActiveUser() {
        String principal = authenticateService.principal().getUsername()
        return User.findByUsername(principal)

    }

    private List<User> getUserList() {

        def users = User.list().sort({it.username})
        String principal = authenticateService.principal().getUsername()

        // remove active session user from this list (cannot modify own privileges)
        users = users.findAll {it -> it.username != principal}

        return users
    }
	
	private List<Role> getRoleList() {
		
				// fetch list of Role
		def roles = Role.list().sort({it.authority})
		
		return roles
		
	}
		
	private List<Role> getAuthorizedRoleList() {
		
			def roles = getRoleList()
				 // filter to match Princpal's roles if not Super User
			if (authenticateService.ifNotGranted("ROLE_ADMIN")) {
		
				roles = roles.findAll {it ->
					authenticateService.ifAllGranted("${it.authority}")
				}
			}
		    return roles
		
	}
}
