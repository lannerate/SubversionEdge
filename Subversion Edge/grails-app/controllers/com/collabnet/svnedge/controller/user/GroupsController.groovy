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

import org.codehaus.groovy.grails.plugins.springsecurity.Secured

import com.collabnet.svnedge.controller.RepoController
import com.collabnet.svnedge.domain.Groups
import com.collabnet.svnedge.domain.Role
import com.collabnet.svnedge.domain.User

@Secured(['ROLE_ADMIN', 'ROLE_ADMIN_USERS'])
class GroupsController {

    def authenticateService
	def uuid
	def repoController = new RepoController();
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
		def gid = params.id
        def group = Groups.get(params.id)
        if (!group) {
            flash.message = "group not found with id $params.id"
            redirect action: list
            return
        }

        [groupInstance: group, activeButton: "user", userList: getUserList()]
    }
	
	def precreate = {
		redirect action: create, params: params
	}
	/**
	 * create group
	 */
	@Secured(['ROLE_ADMIN', 'ROLE_ADMIN_USERS'])
	def create = {
	
		uuid = "GROUP_" +UUID.randomUUID().toString()
		
		Groups group = Groups.findByAuthority(uuid) ?:
		new Groups(authority:uuid,name:uuid,
				description: message(code:"group.GROUP_USER"))
		
		def password = authenticateService.encodePassword("admin")
		
		User normalUser = User.findByUsername("user") ?:
		new User(username: "user",
				realUserName: "Regular User", passwd: password,
				description: "regular user", enabled: true,
				email: "user@example.com").save(flush: true)
				
		group.addToPeople(normalUser)
		 try {
				group.save(flush: true)
	        }catch (Exception e) {
	            log.warn("Could not create roles", e)
	     }
		return [groupInstance: group, activeButton: "user", userList: getUserList()]
	}
	
	
	/**
	 * group save action.
	 */
	@Secured(['ROLE_ADMIN', 'ROLE_ADMIN_USERS'])
	def save = {
		
        def group = Groups.get(params.id)
        if (!group) {
            flash.message = "group not found with id $params.id"
            redirect action: create
            return
        }
		group.name=params.name
		group.authority = params.authority
		group.description = params.description
		

        if (authenticateService.ifNotGranted("${group.authority},ROLE_ADMIN")) {

            flash.error = "You do not have the authority to edit this group"
            redirect action: create
            return
        }

        // add self to Role.people if already there (removed by Grails params mapping)
        def u = getActiveUser()
        boolean addSelf = group.people.contains(u)



        long version = params.version.toLong()
        if (group.version > version) {
            group.errors.rejectValue 'version', "role.optimistic.locking.failure",
                    "Another user has updated this Role while you were editing."
            render view: 'create', model: [group: group, activeButton: "user"]
            return
        }

        group.properties = params
        // update members when all deleted
        if (!params.people) {
            group.people = []
        }
        // restore self to collection if needed
        if (addSelf) {
            group.addToPeople (u)
        }

        // do not remove the GROUP_USER role if other roles are granted to the user
        Groups groupUser = Groups.findByAuthority("GROUP_USER")
        def users = groupUser.people
        if (group == groupUser) {
            def admins = []
/*            admins.addAll(Roles.findByAuthority("GROUP_ADMIN").people)
            admins.addAll(Roles.findByAuthority("GROUP_ADMIN_REPO").people)
            admins.addAll(Roles.findByAuthority("GROUP_ADMIN_USERS").people)
            admins.addAll(Roles.findByAuthority("GROUP_ADMIN_SYSTEM").people)*/
            getUserList().each {
                if (admins.contains(it) && !users.contains(it)) {
                    groupUser.addToPeople(it)
                }
            }
        } else {            
            group.people.each {
                if (!users.contains(it)) {
                    groupUser.addToPeople(it)
                }
            }
        }

        if (group.save()) {
            flash.message = "The group has been updated"
			try{
				if(repoController.SaveAccessRules(repoController.getAccessrules())){
					 flash.message = "The access rules has been updated sucessfully!"
				 }else{
				 flash.message = "The access rules has been updated failly!"
				 }
        	}catch(Exception e){
			    flash.message = "find  Exceptions, when the access rules has been updated!"
				log.info(e.getMessage())
        	}	
            //redirect controller: "groups",  action:show, id: group.id 
        }
		
		redirect action:list 
        /*else {
            render view: 'list', model: [group: group, activeButton: "user", userList: getUserList()]
        }
		*/
  }
	
	
	
    /**
     * group update action.
     */
	@Secured(['ROLE_ADMIN', 'ROLE_ADMIN_USERS'])
    def update = {
//		int ids = params.id as int
        def group = Groups.get(params.id)
        if (!group) {
            flash.message = "group not found with id $params.id"
            redirect action: edit, id: params.id
            return
        }

        if (authenticateService.ifNotGranted("${group.authority},ROLE_ADMIN")) {

            flash.error = "You do not have the authority to edit this group"
            redirect action: edit, id: params.id
            return
        }

        // add self to Role.people if already there (removed by Grails params mapping)
        def u = getActiveUser()
        boolean addSelf = group.people.contains(u)



        long version = params.version.toLong()
        if (group.version > version) {
            group.errors.rejectValue 'version', "role.optimistic.locking.failure",
                    "Another user has updated this Role while you were editing."
            render view: 'edit', model: [group: group, activeButton: "user"]
            return
        }

        group.properties = params
        // update members when all deleted
        if (!params.people) {
            group.people = []
        }
        // restore self to collection if needed
        if (addSelf) {
            group.addToPeople (u)
        }

        // do not remove the GROUP_USER role if other roles are granted to the user
        Groups groupUser = Groups.findByAuthority("GROUP_USER")
        def users = groupUser.people
        if (group == groupUser) {
            def admins = []
/*            admins.addAll(Roles.findByAuthority("GROUP_ADMIN").people)
            admins.addAll(Roles.findByAuthority("GROUP_ADMIN_REPO").people)
            admins.addAll(Roles.findByAuthority("GROUP_ADMIN_USERS").people)
            admins.addAll(Roles.findByAuthority("GROUP_ADMIN_SYSTEM").people)*/
            getUserList().each {
                if (admins.contains(it) && !users.contains(it)) {
                    roleUser.addToPeople(it)
                }
            }
        } else {            
            group.people.each {
                if (!users.contains(it)) {
                    groupUser.addToPeople(it)
                }
            }
        }

        if (group.save()) {
            flash.message = "The group has been updated"
			try{
				if(repoController.SaveAccessRules(repoController.getAccessrules())){
					 flash.message = "The access rules has been updated sucessfully!"
				 }else{
				 flash.message = "The access rules has been updated failly!"
				 }
			}catch(Exception e){
				flash.message = "find  Exceptions, when the access rules has been updated!"
				log.info(e.getMessage())
			}
          //  redirect action: show, id: group.id
        }
        else {
            render view: 'edit', model: [group: group, activeButton: "user", userList: getUserList()]
        }
    }

	/**
	 * group update action.
	 */
	@Secured(['ROLE_ADMIN', 'ROLE_ADMIN_USERS'])
	def delete = {
		def did = params.did
		def group = Groups.get(params.did)
		if (!group) {
			flash.message = "group not found with id $params.id"
			redirect action: show, id: params.id
			return
		}

		if (authenticateService.ifNotGranted("${group.authority},ROLE_ADMIN")) {

			flash.error = "You do not have the authority to edit this group"
			redirect action: show, id: params.id
			return
		}

/*		long version = params.version.toLong()
		if (group.version > version) {
			group.errors.rejectValue 'version', "role.optimistic.locking.failure",
					"Another user has updated this Role while you were editing."
			redirect action: show, id: group.id
			return
		}*/
		try{
			if (group.delete()) {
				flash.message = "The group has been delete"
				
				//redirect action: list
			}
			
			try{
				if(repoController.SaveAccessRules(repoController.getAccessrules())){
					 flash.message = "The access rules has been updated sucessfully!"
				 }else{
				 flash.message = "The access rules has been updated failly!"
				 }
			}catch(Exception e){
				flash.message = "find  Exceptions, when the access rules has been updated!"
				log.info(e.getMessage())
			}
			
			
		}catch(Exception e){
			//flash.message = "this group:  $params.id cannot be deleted !"
			
		}
	//	redirect action: list
		
		
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