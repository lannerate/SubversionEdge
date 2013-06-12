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

import com.collabnet.svnedge.domain.Role 
import com.collabnet.svnedge.domain.Server;
import com.collabnet.svnedge.domain.User 
import com.collabnet.svnedge.domain.Wizard;

import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil
import org.codehaus.groovy.grails.plugins.springsecurity.Secured
import com.collabnet.svnedge.util.ControllerUtil

/**
 * User controller.
 */
@Secured(['ROLE_USER'])
class UserController {

    static allowedMethods = [save: "POST", update: "POST", delete: "POST"]
    def authenticateService
    def userCache
    def ldapService
    def lifecycleService
    def userAccountService
    org.hibernate.SessionFactory sessionFactory

    def index = {
        // redirect to full user management if has authority
        // else direct to Show view of principal
        if(authenticateService.ifAnyGranted("ROLE_ADMIN,ROLE_ADMIN_USERS")) {
            redirect(action: "list", params: params)
        }
        else {
            redirect(action: "showSelf", params: params)
        }
    }

    def showSelf = {
        def username = authenticateService.principal().getUsername()
        def userInstance = User.findByUsername(username)
        def email = email(userInstance)
        def editable = editable(userInstance, email)
        def view = 'show'
        
        render(view: view, model: [userInstance: userInstance, 
                editable: editable, email: email])
    }
    
    private def editable(User user, email) {
        return !user.isLdapUser() || !email || user.email != User.LDAP_PSEUDO_EMAIL
    }
    
    private def email(User user) {
        return user.isLdapUser() ? 
                ldapService.getEmailForLdapUser(user) : user.email 
    }

    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_USERS'])
    def list = {
        ControllerUtil.setDefaultSort(params, "username")
        [userInstanceList: User.list(params), userInstanceTotal: User.count()]
    }

    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_USERS'])
    def create = {
        def userInstance = new User()
        userInstance.authorities= [Role.findByAuthority("ROLE_USER")]
        userInstance.properties = params
        return [userInstance: userInstance, roleList : getRoleList(), 
                authorizedRoleList : getAuthorizedRoleList()]
    }

    def save = {
        // clean the string parameters
        params.each{ key, value -> 
            if (value instanceof String) {
                params[key] = value.trim()
            }
        }
        def userInstance = new User(params)

        // user is enabled (required by acegi)
        userInstance.enabled = true
        userInstance.validate()

        // check Principal has authority to add new user to the Role
        params.authorities.each {

           def role = Role.get(it)
           if(authenticateService.ifNotGranted("ROLE_ADMIN,${role.authority}")){
               userInstance.errors.rejectValue('authorities', 
                       'user.authorities.auth')
               role.discard()
           }
        }

        // encode the passwd
        if (params.passwd) {
            if (params.passwd == params.passwordConfirm) {
                userInstance.passwd = authenticateService
                        .encodePassword(params.passwd)
            } else {
                userInstance.errors.rejectValue('passwd',
                        'user.passwd.mismatch')
            }
        }

        if (!userInstance.hasErrors() && userInstance.save()) {

            // add to default security group if not already
            userInstance.addToAuthorities (Role.findByAuthority("ROLE_USER"))

            log.info("User created: " + userInstance.username)
            lifecycleService.setSvnAuth(userInstance, params.passwd)
            flash.message = message(code: "default.created.message", 
                    args: [message(code: "user.label"),
                           userInstance.username])
            redirect(action: "show", id: userInstance.id)

        } else {
            // discard session changes to User and Role
            userInstance.discard()

            flash.error = "${message(code: 'default.errors.summary')}"

            Role.list().each {
                it.discard()
            }
            
            render(view: "create", model: [userInstance: userInstance, 
                    roleList : getRoleList(), 
                    authorizedRoleList : getAuthorizedRoleList()])
        }
    }

    def show = {
        def userInstance = User.get(params.id)
        if (!userInstance) {
            flash.error = message(code: "default.not.found.message", 
                    args: [message(code: "user.label"),
                    params.id])
            redirect(action: "list")
        }
        else if (!canEdit(params.id)) {
            flash.error= message(code: "default.not.authorized.message", 
                    args: [message(code: "user.label"),
                           params.id])
            redirect(action: "list")
        }
        else {
            def email = email(userInstance)
            def editable = editable(userInstance, email)
            [userInstance: userInstance, editable: editable, email: email]
        }
    }

    def edit = {
        def userInstance = User.get(params.id)
        if (!userInstance) {
            flash.message = message(code: "default.not.found.message", 
                    args: [message(code: "user.label"),
                           params.id])
            redirect(action: "list")
        }
        else if (!canEdit(params.id)) {
            flash.error= message(code: "default.not.authorized.message", 
                    args: [message(code: "user.label"),
                           params.id])
            redirect(action: "list")
        }
        else {
            def email = email(userInstance)
            def editable = editable(userInstance, email)
            if (userInstance.isLdapUser() && !editable) {
                flash.warn = message(code: "user.page.edit.ldap.message")
                redirect(action: "show", params: [id : params.id])

            } else {
                boolean editingSelf = isRequestingSelf(params.id)
                return [userInstance: userInstance, email: email,
                        roleList : getRoleList(),
                        authorizedRoleList : getAuthorizedRoleList(),
                        allowEditingRoles : !editingSelf
                ]
            }
        }
    }

    def update = {
        def userInstance = User.get(params.id)

        if (userInstance) {

            // stop changes to out-of-date userInstance
            if (params.version) {
                def version = params.version.toLong()
                if (userInstance.version > version) {

                    userInstance.errors.rejectValue("version", 
                            "default.optimistic.locking.failure", 
                            [message(code: 'user.label')] as
                                Object[], message(code: 'default.optimistic.locking.failure'))
                    render(view: "edit", model: [userInstance: userInstance])
                    return
                }
            }


            // only allow admins to adjust other users; anyone can edit self
            if(!canEdit(params.id)) {

                userInstance.errors.rejectValue("version", 
                        "user.authorities.auth", message(code: 'default.not.authorized.message'))
                render(view: "edit", model: [userInstance: userInstance])
                return
            }
            def previousEmail = userInstance.email
            boolean invalidateSecurityCache = false
            def originalRoles = userInstance.authorities
            // update userInstance per form except passwd and authorities fields
            bindData(userInstance, params, ['passwd', 'authorities'])
            // user is enabled (required by acegi)
            userInstance.enabled = true
            // validate for field settings
            userInstance.validate()

            def passwordsMatch = true
            def newpassword = null
            if (params.passwd_change_active && params.passwd_change_active == 'true') {
                if (params.containsKey('passwd') && !params.passwd) {
                    userInstance.errors.rejectValue('passwd', 
                            'user.passwd.blank')
                }
                
                newpassword = params.passwd
                if (newpassword) {
                    passwordsMatch = newpassword == params.confirmPasswd
                    if (passwordsMatch) {
                        userInstance.passwd = newpassword
                        userInstance.validate()
                        if (!userInstance.hasErrors()) {
                            invalidateSecurityCache = true
                        }
                    } else {
                        userInstance.errors.rejectValue('passwd', 
                                'user.passwd.mismatch')
                    }
                }
            }

            // determine if this is a self-edit
            boolean editingSelf = isRequestingSelf(params.id)


            // update Role memberships if this is an ADMIN user (except self)
            if(authenticateService.ifAnyGranted("ROLE_ADMIN,ROLE_ADMIN_USERS") 
                    && !editingSelf) {

                // find added and subtracted roles
                def addedRoles = Role.list().findAll {
                    addedRole -> params.authorities?.toList()?.contains(
                            addedRole.id.toString())
                }

                // this includes both explicitly unchecked AND
                // "disabled" roles that current user can't edit
                // (the latter have to be restored to the user)
                def subtractedRoles = Role.list().findAll {
                    subtractedRole -> !params.authorities?.toList()?.contains(
                            subtractedRole.id.toString())
                }

                // verify authority to add / subtract
                addedRoles.each {

                   if(authenticateService.ifAnyGranted("ROLE_ADMIN," +
                           "${it.authority}")) {
                       // add the ROLE_USER role if other roles are granted to the user
                       Role roleUser = Role.findByAuthority("ROLE_USER")
                       if (!addedRoles.contains(roleUser)) {
                           roleUser.addToPeople(userInstance)
                       }
                       it.addToPeople userInstance
                       invalidateSecurityCache = true
                   }
                   else {
                       userInstance.errors.rejectValue('authorities', 
                               'user.authorities.auth')
                   }
                }

                subtractedRoles.each {

                   if(authenticateService.ifAnyGranted("ROLE_ADMIN,"+
                           "${it.authority}")) {
                       // do not remove the ROLE_USER role if other roles are granted to the user
                       if (it.authority != 'ROLE_USER' || subtractedRoles.size == Role.list().size()) {
                           it.removeFromPeople(userInstance)
                           invalidateSecurityCache = true
                       }
                   }

                   else {

                       // restore role membership if no authority to revoke 
                       // it but is missing from the submit
                       if (originalRoles.contains(it)) {
                           it.addToPeople userInstance
                       }
                   }
                }

            }
            else {
                userInstance.authorities = originalRoles
            }
            // if role grants are changed, we need to remove user
            // from Acege UserCache
            if (invalidateSecurityCache)  {
                userCache.removeUserFromCache(userInstance.username)  
            }

            boolean success = !userInstance.hasErrors() && passwordsMatch
            if (success) {
                if (newpassword) {
                    userInstance.passwd = authenticateService
                        .encodePassword(newpassword)
                }
                success = userInstance.save(flush: true)
            }
            if (success && newpassword) {
                log.info("User password updated: " + userInstance.username)
                // update SVN auth for this user
                lifecycleService.setSvnAuth(userInstance, newpassword)
            }
            if (success) {
                log.info("User updated: " + userInstance.username)
                flash.message = message(code: "default.updated.message", 
                        args: [message(code: "user.label"),
                               userInstance.username])
                Server server = Server.getServer()
                if (server.adminEmail == 'devnull@collab.net' ||
                        server.adminEmail == previousEmail) {
                    server.adminEmail = userInstance.email
                    server.adminName = userInstance.realUserName
                    server.save()
                }
                ControllerUtil.wizardCompleteStep(this) {
                    redirect(action: 'show', id: userInstance.id)
                }            
            }
            else {
                // This is needed when using Errors.rejectValue, after
                // validate() method has not found any errors.  Otherwise
                // hibernate may choose to revalidate the object even 
                // though we are discarding it.
                GrailsHibernateUtil.setObjectToReadyOnly(userInstance,
                    sessionFactory)
                // undo changes
                // this suddenly started throwing away the password errors when
                // rewriting the template, commenting out until further
                // investigation
                //userInstance.discard()
                request['error'] = message(code: 'default.errors.summary')
                render(view: "edit", model: [userInstance: userInstance,
                       roleList: getRoleList(), 
                       authorizedRoleList: getAuthorizedRoleList(), 
                       allowEditingRoles : !editingSelf])
            }
        }
        else {
            flash.error = message(code: "default.not.found.message", 
                    args: [message(code: "user.label"),
                           params.id])
            redirect(action: "list")
        }
    }

    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_USERS'])
    def delete = {
        def userInstance = User.get(params.id)
        
        if (userInstance) {
            try {

                def authPrincipal = authenticateService.principal()
                //avoid self-delete if the logged-in user is an admin
                if (!(authPrincipal instanceof String) && 
                        authPrincipal.username == userInstance.username) {
                    flash.error = message(code: "user.not.deleted.auth.message")
                    redirect(action: "show", id: params.id)
                    return
                }
                else {
                    //first, delete this person from People_Authorities table.
                    Role.findAll().each { it.removeFromPeople(userInstance) }
                    lifecycleService.removeSvnAuth(userInstance)
                    userInstance.delete()

                }

                flash.message = message(code: "default.deleted.message", 
                        args: [message(code: "user.label"),
                               userInstance.username])
                redirect(action: "list")
            }
            catch (org.springframework.dao.DataIntegrityViolationException e) {
                flash.error = message(code: "default.not.deleted.message", 
                        args: [message(code: "user.label"),
                               userInstance.username])
                redirect(action: "show", id: params.id)
            }
        }
        else {
            flash.error = message(code: "default.not.found.message", 
                    args: [message(code: "user.label"), 
                           params.id])
            redirect(action: "list")
        }
    }


    private boolean isRequestingSelf(String id) {
        def uname = authenticateService.principal().getUsername();
        def uid = User.findByUsername(uname)?.id?.toString()
        return uid == id
    }

    private boolean canEdit(String id) {
        // this is the ruleset for editing:

        // 1) anyone can edit self
        // 2) ROLE_ADMIN can edit anyone
        def uname = authenticateService.principal().getUsername();
        def sessionUser = User.findByUsername(uname)
        if (sessionUser?.id?.toString() == id ||
                authenticateService.ifAllGranted("ROLE_ADMIN")) return true

        // 3) ROLE_ADMIN_USERS can edit anyone having same or lower auth grants
        def targetUser = User.findById(id)
        if (authenticateService.ifAllGranted("ROLE_ADMIN_USERS")) {

            boolean targetRolesExceedMine = false

            // check that target user's roles are also owned by editor
            targetUser.authorities.each { it -> 
                if (!sessionUser.authorities.contains(it)) 
                    targetRolesExceedMine = true
            }

            return (!targetRolesExceedMine)

        }

        return false
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
