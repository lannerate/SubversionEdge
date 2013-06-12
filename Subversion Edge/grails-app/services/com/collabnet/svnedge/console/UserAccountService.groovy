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
package com.collabnet.svnedge.console

import com.collabnet.svnedge.domain.Role 
import com.collabnet.svnedge.domain.User;
import com.collabnet.svnedge.domain.UserProperty

/**
 * This class provides User and Role management services and bootstraps the security context
 */
class UserAccountService extends AbstractSvnEdgeService {

    def lifecycleService
    def authenticateService
    def csvnAuthenticationProvider

    private def tipMessageCounts = [:]
    private def tipMessageAliases = [:]

    // ensures the existence of essential Roles and Users
    def bootStrap = {env->

        // create required security roles if needed
        Role roleAdmin = Role.findByAuthority("ROLE_ADMIN") ?:
            new Role(authority: "ROLE_ADMIN", 
                description: getMessage("role.ROLE_ADMIN"))

        Role roleAdminSystem = Role.findByAuthority("ROLE_ADMIN_SYSTEM") ?:
            new Role(authority: "ROLE_ADMIN_SYSTEM", 
                description: getMessage("role.ROLE_ADMIN_SYSTEM"))

        Role roleAdminRepo = Role.findByAuthority("ROLE_ADMIN_REPO") ?:
            new Role(authority: "ROLE_ADMIN_REPO", 
                description: getMessage("role.ROLE_ADMIN_REPO"))

        Role roleAdminHooks = Role.findByAuthority("ROLE_ADMIN_HOOKS") ?:
            new Role(authority: "ROLE_ADMIN_HOOKS", 
                description: getMessage("role.ROLE_ADMIN_HOOKS"))

        Role roleAdminUsers = Role.findByAuthority("ROLE_ADMIN_USERS") ?:
            new Role(authority: "ROLE_ADMIN_USERS", 
                description: getMessage("role.ROLE_ADMIN_USERS"))

        Role roleUser = Role.findByAuthority("ROLE_USER") ?:
            new Role(authority: "ROLE_USER", 
                description: getMessage("role.ROLE_USER"))

        // passwod "admin" used for all test users
        def password = authenticateService.encodePassword("admin")

        // only create test users for dev and test target configs
        switch (env) {

            case "development":
            case "test":

                // create test users
                log.info("Creating test users for all each role in this " +
                    "environment: ${env}")

                User adminSystem = User.findByUsername("adminSystem") ?: 
                    saveNewSuperUser("adminSystem", password)

                User adminRepo = User.findByUsername("adminRepo") ?:
                    new User(username: "adminRepo",
                            realUserName: "Repo Administrator", passwd: password,
                            description: "repository admin user", enabled: true,
                            email: "adminRepo@example.com").save(flush: true)

                User adminHooks = User.findByUsername("adminHooks") ?:
                    new User(username: "adminHooks",
                            realUserName: "Repo Hooks Administrator", passwd: password,
                            description: "repository hooks admin user", enabled: true,
                            email: "adminHooks@example.com").save(flush: true)

                User adminUsers = User.findByUsername("adminUsers") ?:
                    new User(username: "adminUsers",
                            realUserName: "Users Administrator", passwd: password,
                            description: "security admin user", enabled: true,
                            email: "adminUsers@example.com").save(flush: true)

                User normalUser = User.findByUsername("user") ?:
                    new User(username: "user",
                            realUserName: "Regular User", passwd: password,
                            description: "regular user", enabled: true,
                            email: "user@example.com").save(flush: true)

                User normalDots = User.findByUsername("user.new") ?:
                    new User(username: "user.new",
                            realUserName: "Regular User Dot", passwd: password,
                            description: "regular user with dot", enabled: true,
                            email: "user.new@example.com").save(flush: true)


                roleAdminSystem.addToPeople(adminSystem)
                roleAdminRepo.addToPeople(adminRepo)
                roleAdminHooks.addToPeople(adminHooks)
                roleAdminUsers.addToPeople(adminUsers)


                roleUser.addToPeople(adminSystem)
                roleUser.addToPeople(adminRepo)
                roleUser.addToPeople(adminHooks)
                roleUser.addToPeople(adminUsers)
                roleUser.addToPeople(normalUser)
                roleUser.addToPeople(normalDots)

                // Allow admin account access to svn

                lifecycleService.setSvnAuth(adminSystem, "admin")
                lifecycleService.setSvnAuth(adminRepo, "admin")
                lifecycleService.setSvnAuth(adminHooks, "admin")
                lifecycleService.setSvnAuth(adminUsers, "admin")
                lifecycleService.setSvnAuth(normalUser, "admin")
                lifecycleService.setSvnAuth(normalDots, "admin")

            default:

                User superadmin = User.findByUsername("admin") 
                if (!superadmin) {
                    log.warn("Creating 'admin' super user since not found. " +
                        "Be sure to change password.")
                    superadmin = saveNewSuperUser("admin", password)
                    lifecycleService.setSvnAuth(superadmin, "admin")
                }
            
                if (!superadmin.authorities?.contains(roleUser)) {
                    roleUser.addToPeople(superadmin)
                }
                if (!superadmin.authorities?.contains(roleAdmin)) {
                    roleAdmin.addToPeople(superadmin)
                }

                try {
                    roleAdmin.save(flush: true)
                    roleUser.save(flush: true)
                    roleAdminSystem.save(flush: true)
                    roleAdminRepo.save(flush: true)
                    roleAdminHooks.save(flush: true)
                    roleAdminUsers.save(flush: true)
                }
                catch (Exception e) {
                    log.warn("Could not create roles", e)
                }

                break

        }
        
        initializeTipMessages()
    }

    private void initializeTipMessages() {
        Properties p = new Properties()
        InputStream stream = getTipsResourceAsStream()
        try {
            p.load(stream)
        } finally {
            stream.close()
        }
        for (name in p.stringPropertyNames()) {
            int dot = name.lastIndexOf('.')
            int index = name.substring(dot + 1) as int
            String key = name.substring(0, dot + 1)
            Integer prevCount = tipMessageCounts[key]
            if (!prevCount || prevCount < index) {
                tipMessageCounts[key] = index
            }
            
            def value = p[name]
            int start = value.indexOf('<alias>')
            if (start >= 0) {
                int end = value.indexOf('</alias>')
                tipMessageAliases[name] = value.substring(start + 7, end)
            }
        }
        
        log.info("tip message counts: " + tipMessageCounts)
        log.debug("tip message aliases: " + tipMessageAliases)
    }

    private InputStream getTipsResourceAsStream() {
        def stream
        try {
            stream = grailsApplication.mainContext.getResource("/WEB-INF/grails-app/i18n/tips.properties").file.newInputStream()
        } catch (IOException e) {
            stream = this.class.getResourceAsStream("/grails-app/i18n/tips.properties")
        }
        return stream
    }
        
    private User saveNewSuperUser(userid, password) {
        new User(username: userid, realUserName: "Super Administrator", enabled: true,
                 passwd: password, description: "admin user", email: "admin@example.com")
            .save(flush: true)
    }

    String tipMessageCode(User user, String controller, String action) {
        def prefix = authenticateService
                .ifAnyGranted("ROLE_ADMIN,ROLE_ADMIN_SYSTEM") ?
                'tip.admin.' : 'tip.enduser.'
        if (controller == 'repo' && authenticateService.ifAnyGranted(
                "ROLE_ADMIN,ROLE_ADMIN_REPO,ROLE_ADMIN_HOOKS")) {
            prefix = 'tip.adminRepo.'
        } else if (controller == 'user' && authenticateService.ifAnyGranted(
                "ROLE_ADMIN,ROLE_ADMIN_USERS")) {
            prefix = 'tip.adminUser.'
        }

        String controllerKey = prefix + controller + '.'
        String actionKey = prefix + controller + '.' + action + '.'
        if (tipMessageCounts[controllerKey] || tipMessageCounts[actionKey]) {
            
            int controllerCount = tipMessageCounts[controllerKey] ?: 0
            int actionCount = tipMessageCounts[actionKey] ?: 0
            int generalCount = tipMessageCounts[prefix] ?: 1
            double threshold = weightSpecificToGeneral(
                    controllerCount + actionCount, generalCount)
            double dice = Math.random()
            log.debug("tipMessageCode for c=" + controller + ", a=" + action +
                 " threshold=" + threshold + ", dice=" + dice)
            
            if (dice <= threshold) {
                if (actionCount > 0) {
                    if (controllerCount > 0) {
                        threshold = weightSpecificToGeneral(
                                actionCount, controllerCount)
                        dice = Math.random()
                        prefix = (dice < threshold) ? actionKey : controllerKey
                    } else {
                        prefix = actionKey
                    }
                } else {
                    prefix = controllerKey
                }
            }
        }

        def props = user.propertiesMap
        UserProperty tipCount = props[prefix]
        if (tipCount) {
            int newCount = (tipCount.value as int) + 1
            int maxIndex = tipMessageCounts[prefix]
            if (maxIndex < newCount) {
                newCount = 1
            }
            tipCount.value = newCount as String
            
        } else {
            tipCount = new UserProperty(name: prefix, value: "1")
            user.addToProps(tipCount)
        }
        tipCount.save()
        
        String key = prefix + tipCount.value
        return tipMessageAliases[key] ?: key
    }

    private float weightSpecificToGeneral(int specific, int general) {
        // 0.25  + 0.25 * min( (2 * specific/general, 1 ) + min( specific/general, 1 ) * 0.4
        // At 1/15 ratio weights specific message at 0.34
        // At 1/1 ratio or higher, weights specific message at 0.9
        return 0.25f + 0.25f * Math.min(1.0f, 2.0f * specific / general) +
                0.4f * Math.min(1.0f, 1.0f * specific / general)
    }
}
