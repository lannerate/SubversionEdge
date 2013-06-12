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
package com.collabnet.svnedge.controller.admin

import grails.converters.JSON

import org.codehaus.groovy.grails.plugins.springsecurity.Secured
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.springframework.web.servlet.support.RequestContextUtils as RCU

/**
 * The packages update controller is used to manage the packages 
 * update and installed packages.
 * 
 * @author Marcello de Sales (mdesales@collab.net)
 */
@Secured(['ROLE_ADMIN','ROLE_ADMIN_SYSTEM'])
class PackagesUpdateController {

    def defaultAction = 'available'

    def packagesUpdateService

    def config = ConfigurationHolder.config

    // start and stop actions use POST requests
    static allowedMethods = [installUpdates: 'POST', installAddOns:'POST', 
                             reloadUpdates:'POST', reloadAddOns:'POST', 
                             confirmStart:'POST', restartServer:'POST']

    def getSortedOrderedPackages(packagesInfo, params) {
        if (!packagesInfo || packagesInfo.length == 0) {
            return packagesInfo
        }
        if (params.sort) {
            packagesInfo = packagesInfo.sort { pkg -> pkg."${params.sort}"}
        } else {
            packagesInfo = packagesInfo.sort { pkg -> pkg.summary}
        }
        if (params.order == "desc") {
            packagesInfo = packagesInfo.reverse()
        }
        return packagesInfo
    }

    def available = {
        if (!flash.error) {
            if (this.packagesUpdateService.areThereUpdatesAvailable()) {
                flash.warn = message(code:
                    'packagesUpdate.action.available.message')
            }
            if (this.packagesUpdateService.systemNeedsRestart()) {
                flash.warn = message(code:
                    'packagesUpdate.status.updates.requiresRestart')
            }
        }

        def originUrl = this.packagesUpdateService.getImageOriginUrl()
        def proxyURL = this.packagesUpdateService.getImageProxyToOriginUrl()
        def pkgsInf = this.packagesUpdateService.getUpgradablePackagesInfo()
        pkgsInf = this.getSortedOrderedPackages(pkgsInf, params)

        def hadConnectionProblems = false
        if (session["connectionProblems"] != null) {
            hadConnectionProblems = true
            session["connectionProblems"] = null
        }
        return [packagesInfo: pkgsInf, 
                imageOriginUrl: originUrl,
                proxyToOriginURL: proxyURL,
                anyConnectionProblem: hadConnectionProblems
        ]
    }

    def getUpdateMessage() {
        def msg = message(code: 'packagesUpdate.status.updates.available')
        def download = message(code:'packagesUpdate.status.updates.forDownload')
        return msg.replace(download,
            "<a href='/csvn/packagesUpdate/available'>${download}</a>")
    }

    def addOns = {
        if (!flash.error) {
            if (this.packagesUpdateService.areThereUpdatesAvailable()) {

                flash.warn = getUpdateMessage()
            }
            if (this.packagesUpdateService.systemNeedsRestart()) {
                flash.warn = message(
                    code: 'packagesUpdate.status.updates.requiresRestart')
            }
        }

        def originUrl = this.packagesUpdateService.getImageOriginUrl()
        def proxyURL = this.packagesUpdateService.getImageProxyToOriginUrl()
        def pkgsInf = this.packagesUpdateService.getNewPackagesInfo()
        pkgsInf = this.getSortedOrderedPackages(pkgsInf, params)

        def hadConnectionProblems = false
        if (session["connectionProblems"] != null) {
            hadConnectionProblems = true
            session["connectionProblems"] = null
        }
        return [packagesInfo: pkgsInf,
                imageOriginUrl: originUrl,
                proxyToOriginURL: proxyURL,
                anyConnectionProblem: hadConnectionProblems
        ]
    }

    def installed = {
        if (!flash.error) {
            if (this.packagesUpdateService.areThereUpdatesAvailable()) {
                flash.warn = getUpdateMessage()
            }
            if (this.packagesUpdateService.systemNeedsRestart()) {
                flash.warn = message(
                    code: 'packagesUpdate.status.updates.requiresRestart')
            }
        }

        def originUrl = this.packagesUpdateService.getImageOriginUrl()
        def proxyURL = this.packagesUpdateService.getImageProxyToOriginUrl()
        def pkgsInf = this.packagesUpdateService.getInstalledPackagesInfo()
        if (!pkgsInf) {
            //if this happens, the application started without any 
            //connectivity with the originURL, resulting with an empty cache
            if (!flash.error) {
                flash.error = this.getNoConnectionErrorMessage(
                    "reloadInstalled")
            }
        } else {
            pkgsInf = this.getSortedOrderedPackages(pkgsInf, params)
        }

        return [packagesInfo: pkgsInf,
                imageOriginUrl: originUrl,
                proxyToOriginURL: proxyURL
        ]
    }

    private String getNoConnectionErrorMessage(packagesType) {
        def server = this.packagesUpdateService.getImageOriginUrl() ?: ""
        def noConMsg = message(code: 'packagesUpdate.error.server.noConnection',
            args: [server])

        def reloadMsg = message(code: 'packagesUpdate.status.reloadCheckNetwork')
        return noConMsg + " " +  reloadMsg
    }

    private String getNoRouteErrorMessage(packagesType) {
        def msg = message(code: 'packagesUpdate.error.server.unreachable',
            args:[this.packagesUpdateService.getImageOriginUrl()])
        def prxServ = message(code: 'packagesUpdate.error.server.proxyReplace')
        def helpUrl = config.svnedge.helpUrl
        def helpPath = "/topic/csvn/action/upgradecsvn_proxy.html"
        def helpLink = "<a href='${helpUrl}${helpPath}' target='csvnHelp'>" +
            prxServ + "</a>."
        return msg.replace(prxServ, helpLink)
    }

    def reloadInstalled = {
        try {
            this.packagesUpdateService.reloadPackagesAndUpdates()

        } catch (UnknownHostException uhe) {
            session["connectionProblems"] = "installed"
            flash.error = this.getNoConnectionErrorMessage("reloadInstalled")
            log.error(flash.error)

        } catch (ConnectException ce) {
            session["connectionProblems"] = "installed"
            //if the connection times out. Same effect as NoRouteToHostException
            flash.error = this.getNoRouteErrorMessage("reloadInstalled")
            log.error(flash.error)

        } catch (NoRouteToHostException nrth) {
            session["connectionProblems"] = "installed"
            flash.error = this.getNoRouteErrorMessage("reloadInstalled")
            log.error(flash.error)

        } catch (Exception e) {
            session["connectionProblems"] = "installed"
            def loadErr = message(code: 
                'packagesUpdate.error.general.loading.installed')
            flash.error = loadErr + ": " + e.getMessage()
            log.error(flash.error, e)
        }
        redirect(action:"installed")
    }

    def reloadUpdates = {
        try {
            this.packagesUpdateService.reloadPackagesAndUpdates()

        } catch (UnknownHostException uhe) {
            session["connectionProblems"] = "updates"
            flash.error = this.getNoConnectionErrorMessage()
            log.error(flash.error)

        } catch (ConnectException ce) {
            session["connectionProblems"] = "updates"
            //if the connection times out. Same as effect NoRouteToHostException
            flash.error = this.getNoRouteErrorMessage()
            log.error(flash.error)

        } catch (NoRouteToHostException nrth) {
            session["connectionProblems"] = "updates"
            flash.error = this.getNoRouteErrorMessage()
            log.error(flash.error)

        } catch (Exception e) {
            session["connectionProblems"] = "updates"
            def loadErr = message(code: 
                'packagesUpdate.error.general.loading.updates')
            flash.error = loadErr + ": " + e.getMessage()
            log.error(flash.error, e)
        }
        redirect(action:"available")
    }

    def reloadAddOns = {
        try {
            this.packagesUpdateService.reloadPackagesAndUpdates()

        } catch (UnknownHostException uhe) {
            session["connectionProblems"] = "addOns"
            flash.error = this.getNoConnectionErrorMessage()
            log.error(flash.error)

        } catch (ConnectException ce) {
            session["connectionProblems"] = "addOns"
            //if the connection times out. Same as effect NoRouteToHostException
            flash.error = this.getNoRouteErrorMessage()
            log.error(flash.error)

        } catch (NoRouteToHostException nrth) {
            session["connectionProblems"] = "addOns"
            flash.error = this.getNoRouteErrorMessage()
            log.error(flash.error)

        } catch (Exception e) {
            session["connectionProblems"] = "addOns"
            def loadErr = message(code: 
                'packagesUpdate.error.general.loading.addOns')
            flash.error = loadErr + ": " + e.getMessage()
            log.error(flash.error, e)
        }
        redirect(action:"addOns")
    }

    def installAddOns = {
        if (this.packagesUpdateService.areThereNewPackagesAddOns()) {
            session["install"] = "addOns"
            redirect(action:"installUpdatesStatus")
        }
    }

    def installUpdates = {
        if (this.packagesUpdateService.areThereUpdatesAvailable()) {
            session["install"] = "updates"
            redirect(action:"installUpdatesStatus")
        }
    }

    def installUpdatesStatus = {
        if (session["install"] == null) {
            redirect(action:"installed")
        }
        return [hideButtons: true]
    }

    def confirmStart = {
        if (session["install"] == null) {
            redirect(action:"installed")
        }
        //non-blocking method call that redirects to the the status
        //page, which shows the current status from the cometd service.
        //The update process occurs in a separate daemon thread.
        if (session["install"].equals("addOns")) {
            this.packagesUpdateService.installPackagesAddOns(RCU.getLocale(request))
        } else
        if (session["install"].equals("updates")){
            this.packagesUpdateService.installPackagesUpdates(RCU.getLocale(request))
        }
        session["install"] = null

        String statusOk = "ok"
        def result = [status: statusOk]
        render result as JSON
    }

    def restartServer = {
        this.packagesUpdateService.restartServer()
        String statusOk = "ok"
        def result = [status: statusOk]
        render result as JSON
    }
}
