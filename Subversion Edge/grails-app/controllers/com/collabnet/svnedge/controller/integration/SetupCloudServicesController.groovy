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
package com.collabnet.svnedge.controller.integration

import org.codehaus.groovy.grails.plugins.springsecurity.Secured
import com.collabnet.svnedge.domain.integration.CloudServicesConfiguration
import com.collabnet.svnedge.domain.User
import com.collabnet.svnedge.integration.AuthenticationCloudServicesException;
import com.collabnet.svnedge.util.ControllerUtil

@Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO', 'ROLE_ADMIN_SYSTEM'])
class SetupCloudServicesController {

    def securityService
    def cloudServicesRemoteClientService
    def messageSource

    def index = {
        def cloudConfig = CloudServicesConfiguration.getCurrentConfig()
        if (cloudConfig && cloudConfig.domain) {
            redirect(action: 'credentials')
        }
    }

    def getStarted = {
        CloudServicesAccountCommand cmd = new CloudServicesAccountCommand()
        def realName = loggedInUserInfo(field: 'realUserName')
        def match = (realName =~ /(\S+)\s+(.+)/)
        if (match) {
            cmd.firstName = match[0][1]
            cmd.lastName = match[0][2]
        } else {
            cmd.lastName = realName
        }
        def email = loggedInUserInfo(field: 'email')
        cmd.emailAddress = email
        cmd.emailAddressConfirm = email
        match = (email =~ /@(.+)\./)
        if (match) {
            cmd.domain = match[0][1].replace('.', '_')
            cmd.organization = cmd.domain.replace('_', ' ').capitalize()
        }
        render(view: "signup", model: [cmd: cmd])
    }

    def checkLoginAvailability = { CloudServicesAccountCommand cmd ->

        // first validate according to local rules to save roundtrip to api
        String response = message(code: "setupCloudServices.login.available.yes")
        boolean tokenOk = true
        cmd.username = params.token
        cmd.validate()
        if (cmd.hasErrors() && (cmd.errors.hasFieldErrors("username"))) {
            tokenOk = false
            response = messageSource.getMessage(cmd.errors.getFieldError("username"), request.locale)
        }
        // else, check remote availability
        else {
            CloudServicesConfiguration cloudConf = CloudServicesConfiguration.getCurrentConfig()
            String domain = (cloudConf?.domain) ?: null
            if (!cloudServicesRemoteClientService.isLoginNameAvailable(cmd.username, domain)) {
                tokenOk = false
                response = message(code: "setupCloudServices.login.available.no")
            }
        }

        render(contentType: "text/json") {
            result(tokenStatus: tokenOk ? "ok" : "error", message: response)
        }
    }

    def checkDomainAvailability = { CloudServicesAccountCommand cmd ->

        // first validate according to local rules to save roundtrip to api
        String response = message(code: "setupCloudServices.domain.available.yes")
        boolean tokenOk = true
        cmd.domain = params.token
        cmd.validate()
        if (cmd.hasErrors() && (cmd.errors.hasFieldErrors("domain"))) {
            tokenOk = false
            response = messageSource.getMessage(cmd.errors.getFieldError("domain"), request.locale)
        }
        // else, check remote availability
        else {
            if (!cloudServicesRemoteClientService.isDomainAvailable(cmd.domain)) {
                tokenOk = false
                response = message(code: "setupCloudServices.domain.available.no")
            }
        }

        render(contentType: "text/json") {
            result(tokenStatus: tokenOk ? "ok" : "error", message: response)
        }
    }

    def createAccount = { CloudServicesAccountCommand cmd ->
        cmd.requestLocale = request.locale
        cmd.validate()
        if (cmd.hasErrors()) {
            render(view: "signup", model: [cmd: cmd])
        }
        else if (cloudServicesRemoteClientService.createAccount(cmd)) {
            // remove error message if it persists from previous submit (occasional grails bug)
            flash.error = null
            request.unfiltered_message = message(code: "setupCloudServices.page.signup.accountCreation.mustValidate")
            ControllerUtil.wizardCompleteStep(this) { 
                render(view: "confirm", model: [cmd: cmd]) 
            }
        }
        else {
            flash.error = message(code: "setupCloudServices.page.signup.accountCreation.error")
            render(view: "signup", model: [cmd: cmd])
        }
    }

    def credentials = {
        def cloudConfig = CloudServicesConfiguration.getCurrentConfig()
        def cmd = new CloudServicesAccountCommand()
        def existingCredentials = cloudConfig && cloudConfig.domain
        if (existingCredentials) {
            cmd.password = securityService.decrypt(cloudConfig.password)
            cmd.username = cloudConfig.username
            cmd.domain = cloudConfig.domain
        }
        [cmd: cmd, existingCredentials: existingCredentials]
    }

    def updateCredentials = { CloudServicesAccountCommand cmd ->
        // with existing config, only password can be updated
        def cloudConfig = CloudServicesConfiguration.getCurrentConfig()
        def existingCredentials = (cloudConfig && cloudConfig.domain)
        if (existingCredentials) {
            cmd.username = cloudConfig.username
            cmd.domain = cloudConfig.domain
            if (params['password_changed'] != 'true') {
                cmd.password = securityService.decrypt(cloudConfig.password)
            }
        }
        // otherwise, all cmd fields are updateable and a new config is created
        else {
            cloudConfig = new CloudServicesConfiguration();
        }

        // first, field validation
        cmd.requestLocale = request.locale
        cmd.validate()
        if (cmd.hasErrors() && (cmd.errors.hasFieldErrors("username") || cmd.errors.hasFieldErrors("password") ||
                cmd.errors.hasFieldErrors("domain"))) {
            render(view: "credentials", model: [cmd: cmd, existingCredentials: existingCredentials])
        }
        // then validate cloudservices credential against api
        // persist the credentials when successful
        else if (cloudServicesRemoteClientService.validateCredentials(cmd.username, cmd.password, cmd.domain)) {
            cloudConfig.username = cmd.username
            cloudConfig.password = securityService.encrypt(cmd.password)
            cloudConfig.domain = cmd.domain
            cloudConfig.enabled = true
            cloudConfig.save()
            ControllerUtil.wizardCompleteStep(this) { 
                render(view: "confirm", model: [cmd: cmd]) 
            }
        }
        else {
            flash.error = message(code: "setupCloudServices.page.credentials.validation.error")
            render(view: "credentials", model: [cmd: cmd, existingCredentials: existingCredentials])
        }
    }

    def removeCredentials = {
        def cloudConfig = CloudServicesConfiguration.currentConfig
        if (cloudConfig) {
            cloudConfig.domain = ''
            cloudConfig.username = ''
            cloudConfig.password = ''
            cloudConfig.save()
            request.message = message(code: "setupCloudServices.page.credentials.remove.success")
        }
        else {
            request.warning = message(code: "setupCloudServices.page.credentials.remove.failure")
        }
        render(view: "index", model: [cmd: new CloudServicesAccountCommand()])
    }

    def selectUsers = {
        def listParams = new HashMap(params)
        if (!params.sort || params.sort == "matchingRemoteUser") {
            listParams.sort = "username"
        }

        def localUsers = User.list(listParams)
        def userList = []
        try {
            def remoteUsers = cloudServicesRemoteClientService.listUsers()
            localUsers.each() {
                def user = [:]
                user.userId = it.id
                user.username = it.username
                user.realUserName = it.realUserName
                user.email = it.email
                def remoteUserInfo = null
                def remoteData = remoteUsers.find { remoteItem -> remoteItem.login == it.username || remoteItem.email == it.email}
                if (remoteData) {
                    remoteUserInfo = "${remoteData.login} | ${remoteData.firstName} ${remoteData.lastName} | ${remoteData.email}"
                }
                user.matchingRemoteUser = remoteUserInfo
                user.selectForMigration = (remoteUserInfo == null && it.username != 'admin')
                userList << user
            }

            // sort the user list if needed
            if (params.sort == "matchingRemoteUser") {
                userList = userList.sort { it."${params.sort}"}
                if (params.order == "desc") {
                    userList = userList.reverse()
                }
            }
        } catch (AuthenticationCloudServicesException auth) {
            flash.unfiltered_error = message(code: auth.message, args: [0])
            redirect(action: 'credentials')
        }
        [userList: userList]
    }

    def createUserLogins = {
        def userList = []
        ControllerUtil.getListViewSelectedIds(params).each {
            User u = User.get(it)
            userList << u
        }
        [userList: userList]
    }

    def migrateUsers = {
        boolean success = true
        ControllerUtil.getListViewSelectedIds(params).each {
            User u = User.get(it)
            def loginName = params."username_${it}"
            success &= cloudServicesRemoteClientService.createUser(u, loginName)
        }
        if (success) {
            flash.message = message(code: 'setupCloudServices.page.migrateUsers.success')
        }
        else {
            flash.error = message(code: 'setupCloudServices.page.migrateUsers.failure')
        }
        forward(action: 'selectUsers')
    }

    def enable = {
        flash.message = message(code: 'setupCloudServices.action.enable.success')
        def cloudConfig = CloudServicesConfiguration.currentConfig
        if (cloudConfig) {
            if (cloudConfig.enabled) {
                flash.message = message(
                        code: 'setupCloudServices.action.enable.formerly')
            }
        } else {
            cloudConfig = new CloudServicesConfiguration(
                    username: '', password: '', domain: '')
        }
        cloudConfig.enabled = true
        cloudConfig.save()
        redirect(action: 'index')
    }
    
    def disable = {
        def cloudConfig = CloudServicesConfiguration.currentConfig
        if (cloudConfig) {
            cloudConfig.enabled = false
            cloudConfig.domain = ''
            cloudConfig.username = ''
            cloudConfig.password = ''
            cloudConfig.save()
            flash.message = 
                    message(code: 'setupCloudServices.action.disable.success')
        } else {
            flash.message =
                message(code: 'setupCloudServices.action.disable.formerly')

        }
        redirect(controller: 'server', action: 'edit')
    }
}

class CloudServicesAccountCommand {
    String username
    String password
    String passwordConfirm
    String domain

    String firstName
    String lastName
    String emailAddress
    String emailAddressConfirm
    String phoneNumber
    String organization
    Boolean acceptTerms

    Locale requestLocale = Locale.default

    static constraints = {
        username(blank: false, matches: "[a-zA-Z0-9_]+", minSize: 3, maxSize: 16)
        password(blank: false, minSize: 6, maxSize: 64)
        passwordConfirm(blank: false,
                validator: { String val, CloudServicesAccountCommand cmd ->
                    if (val != cmd.password) {
                        return "cloudServicesAccountCommand.passwordConfirm.mismatch"
                    }
                }
        )
        domain(blank: false, matches: "[a-zA-Z0-9]+", minSize: 3, maxSize: 32)
        firstName(blank: false)
        lastName(blank: false)
        emailAddress(blank: false, email: true)
        emailAddressConfirm(blank: false,
                validator: { String val, CloudServicesAccountCommand cmd ->
                    if (val != cmd.emailAddress) {
                        return "cloudServicesAccountCommand.emailAddressConfirm.mismatch"
                    }
                }
        )
        phoneNumber(matches: "[0-9 #()\\+-]+")
        organization(blank: false, minSize: 1, maxSize: 256)
        acceptTerms(
                validator: { Boolean val ->
                    if (val != new Boolean(true)) {
                        return "cloudServicesAccountCommand.acceptTerms.notAccepted"
                    }
                }
        )
    }
}