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

import com.collabnet.svnedge.domain.SchemaVersion
import com.collabnet.svnedge.domain.Server
import com.collabnet.svnedge.domain.ServerMode
import com.collabnet.svnedge.domain.User
import com.collabnet.svnedge.domain.Wizard
import grails.util.GrailsUtil
import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.codehaus.groovy.grails.commons.ConfigurationHolder

class ApplicationFilters {

    def operatingSystemService
    def authenticateService
    def lifecycleService
    def config = ConfigurationHolder.config
    def app = ApplicationHolder.application

    def filters = {

        /**
         * Filtering when requesting via http scheme and console is configured
         * to require ssl. This applies to all controllers/actions *except* the
         * rest api endpoint to fetch the securePort. This is because the mDNS service only
         * advertises the plain http port, and rest clients may not handle the redirect
         * which gets us from there to https
         */
        requireSsl(controller: '*', action: '*') {
            before = {
                // api to get securePort is always allowed 
                if (request.method == "GET" && request.forwardURI.contains("/api") &&
                        request.forwardURI.contains("/securePort")) {
                    return true
                }
                else if (request.scheme == 'http' && Server.getServer().useSslConsole) {
                    def port = System.getProperty("jetty.ssl.port", "4434")
                    def sslUrl = "https://${request.serverName}${port != "443" ? ":" + port : ""}${request.forwardURI}"
                    redirect(url: sslUrl)
                    return false
                }
                return true
            }
        }

        /**
         * Filtering when the server has not loaded the libraries correctly.
         */
        verifyOperatingSystemLibraries(controller: '*', action: '*') {
            after = {
                if (!operatingSystemService.isReady()) {
                    switch (controllerName) {
                        case "status":
                        case "statistics":
                        case "server":
                            flash.error = app.getMainContext().getMessage(
                                    "server.failed.loading.libraries", [] as String[],
                                    Locale.getDefault())
                            break;
                    }
                }
            }
        }

        /**
         * this filter defines the "features" available to the user 
         * (represented by buttons on the main toolbar) based on user roles and server mode
         */
        featureAvailability(controller: '*', action: '*') {

            before = {

                boolean isIntegrationServer = ServerMode.MANAGED == Server.getServer().mode
                boolean isReplicaServer = ServerMode.REPLICA == Server.getServer().mode
                boolean isManagedMode = (isIntegrationServer || isReplicaServer)

                // prohibited actions in Standalone mode
                if (!isManagedMode &&
                        "server" == controllerName && (
                "editIntegration" == actionName || "revert" == actionName)) {

                    flash.error = app.getMainContext().getMessage(
                            "filter.probihited.mode.standalone", null,
                            request.locale)
                    redirect(controller: "status")
                    return false
                }
                // prohibited actions in TF or Replica mode
                if (isManagedMode &&
                        (["user", "role", "setupTeamForge", "repoTemplate"].contains(controllerName) ||
                         ("server" == controllerName && "editAuthentication" == actionName))) {
                    flash.error = app.getMainContext().getMessage(
                            "filter.probihited.mode.managed", null,
                            request.locale)
                    redirect(controller: "status")
                    return false
                }
            }

            // after running the action, add "featureList" to the page model
            after = {model ->

                boolean isIntegrationServer = ServerMode.MANAGED == Server.getServer().mode
                boolean isReplicaServer = ServerMode.REPLICA == Server.getServer().mode
                boolean isManagedMode = (isIntegrationServer || isReplicaServer)

                boolean isSuperUser = authenticateService.ifAnyGranted("ROLE_ADMIN")
                boolean isUserAdmin = authenticateService.ifAnyGranted("ROLE_ADMIN,ROLE_ADMIN_USERS")

                // default list of features for all users
                def featureList = []
                if (!isManagedMode || isSuperUser) {
                    featureList << "repo"
                }

                if (!isManagedMode && isUserAdmin) {
                    featureList << "user"
                }

                // role-based additions
                if (authenticateService.ifAnyGranted("ROLE_ADMIN,ROLE_ADMIN_SYSTEM")) {
                    if (lifecycleService.getServer().replica) {
                        featureList << "admin"
                    }
                    else {
                        featureList << "server"
                    }
                }

                // add featurelist to the request model
                if (!model) {
                    model = new HashMap()
                }
                model.put("featureList", featureList)
                model.put("isManagedMode", isManagedMode)
                model.put("isIntegrationServer", isIntegrationServer)
                model.put("isReplicaServer", isReplicaServer)
            }
        }

        redirectStatusToRepositoryList(controller: 'status', action: '*') {
            before = {
                boolean isIntegrationServer = ServerMode.MANAGED == Server.getServer().mode
                boolean isReplicaServer = ServerMode.REPLICA == Server.getServer().mode
                boolean isManagedMode = (isIntegrationServer || isReplicaServer)
                boolean isAdmin = authenticateService.ifAnyGranted("ROLE_ADMIN,ROLE_ADMIN_SYSTEM")

                if (isManagedMode && !isAdmin) {
                    redirect(controller: 'ocn', action: 'index')
                }
                else if (!isAdmin) {
                    redirect(controller: 'repo', action: 'index')
                }
            }
        }
        
        /**
         * This filter restricts the access to the plug-in dbUtil under the
         * production environment for users with the roles "ROLE_ADMIN".
         */
        dbUtilPluginRestriction(controller: 'dbUtil', action: '*') {
            before = {
                if (GrailsUtil.environment != "production" ||
                        (authenticateService.isLoggedIn() &&
                                authenticateService.ifAnyGranted("ROLE_ADMIN"))) {
                    return true
                } else {
                    flash.error = app.getMainContext().getMessage(
                            "filter.probihited.credentials", null,
                            Locale.getDefault())
                    redirect(uri: '/')
                    return false
                }
            }
        }

        /**
         * This filter prevents access to the greenmail plug-in under the
         * production environment.
         */
        greenmailPluginRestriction(controller: 'greenmail', action: '*') {
            before = {
                if (GrailsUtil.environment != "production") {
                    return true
                } else {
                    redirect(controller: 'status', action: 'index')
                    return false
                }
            }
        }

        wizardRedirect(controller: '*', action: '*') {
            before = {
                def ignoreWizardControllers = ['login', 'logout']
                if (!ignoreWizardControllers.contains(controllerName)) {
                    log.debug "controller=" + controllerName +
                            " action=" + actionName
                    def wizard = Wizard.getLastActiveWizard()
                    boolean isTargetedUser = authenticateService
                            .ifAnyGranted(wizard?.rolesAsString)
                    if (wizard && isTargetedUser) {
                        log.debug "Wizard dump: " + wizard.dump()
                        if (!wizard.initialized) {
                            wizard.increment()
                            wizard.initialized = true
                            if (!wizard.save()) {
                                log.error "wizard errors: " + wizard.errors
                            }
                        }

                        if (wizard.active && !wizard.done) {
                            def stepHelper = wizard.currentStep?.helper()
                            if (stepHelper?.targetController &&
                                    controllerName != wizard.controller &&
                                    stepHelper.forceTarget &&
                                    (stepHelper.targetController != controllerName ||
                                    (stepHelper.targetAction != actionName &&
                                    !stepHelper.alternateActions.contains(actionName)))) {
                                log.debug "wizard redirecting to " +
                                        stepHelper.targetController + " action: " +
                                        stepHelper.targetAction
                                redirect(controller: stepHelper.targetController,
                                        action: stepHelper.targetAction)
                                return false
                            } else {
                                log.debug "Wizard did not redirect  " + stepHelper?.dump()
                            }
                        }
                    }
                }
            }

            after = { model ->
                if (model) {
                    model['activeWizard'] = Wizard.lastActiveWizard
                    model['allWizards'] = Wizard.list()
                }
            }
        }
        
        defaultPassword(controller: 'login', action: 'auth') {
            after = { model ->
                if (model) {
                    long now = System.currentTimeMillis()
                    def createdDate = SchemaVersion.findByMajorAndMinor(1, 1)?.dateCreated
                    if (createdDate && ((now - createdDate.time) < 8 * 3600000)) {
                        User u = User.findByUsername('admin')
                        model['isDefaultPassword'] = (u.passwd == '21232f297a57a5a743894a0e4a801fc3')
                    }
                }
                return
            }
        }
    }
}
