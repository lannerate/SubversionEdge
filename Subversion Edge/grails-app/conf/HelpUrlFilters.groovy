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

import org.codehaus.groovy.grails.commons.ConfigurationHolder

/**
 * this filter defines the help link target for the context
 */
class HelpUrlFilters {

    private static final DEFAULT_HELP_PATH = '/topic/csvn/faq/csvn_toc.html'

    // per-controller help paths
    private static final def controllerHelpPaths = [
            'user': 'manageusers_csvn.html',
            'role': 'manageusers_csvn.html',
            'repo': 'managerepositories.html',
            'repoTemplate': 'managerepositories.html',
            'server': 'configurecsvn.html',
            'log': 'maintainserver_csvn.html',
            'job': 'maintainserver_csvn.html',
            'status': 'maintainserver_csvn.html',
            'setupTeamForge': 'convertcsvntotf.html',
            'setupReplica': 'convertcsvntotfreplica.html',
            'setupCloudServices': 'movetocncloud.html',
            'packagesUpdate': 'upgradecsvn.html',
            'statistics': 'maintainserver_csvn.html'
    ]

    def config = ConfigurationHolder.config

    private def initModel(model) {
        // add the helpUrl to the page model
        if (!model) {
            model = new HashMap()
        }
        if (!model['helpUrl']) {
            String helpBase = config.svnedge.helpUrl
            model.put('helpBaseUrl', helpBase)
            addHelpUrl(model, DEFAULT_HELP_PATH)
        }
        return model
    }
    
    private def addHelpUrl(model, String helpPath) {
        def path = helpPath.startsWith('/') ? 
                helpPath : '/topic/csvn/action/' + helpPath
        model['helpUrl'] = model['helpBaseUrl'] + path
    }
    
    // after running the action, add "helpUrl" to the page model
    def filters = {

        helpUrl(controller: '*', action: '*') {
            after = {model ->
                model = initModel(model)                
                // override if controller has specific url that has not been
                // overridden with a more specific filter
                if (model['helpUrl'].endsWith(DEFAULT_HELP_PATH) && 
                        controllerHelpPaths[params.controller]) {
                    addHelpUrl(model, controllerHelpPaths[params.controller])
                }
            }
        }

        helpUrlLogging(controller: 'log', action: 'configure') {
            after = {model ->
                model = initModel(model)
                addHelpUrl(model, 'configurecsvn.html')
            }
        }
    }
}
