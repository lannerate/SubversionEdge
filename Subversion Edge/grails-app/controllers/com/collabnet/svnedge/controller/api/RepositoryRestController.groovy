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



package com.collabnet.svnedge.controller.api

import com.collabnet.svnedge.domain.Server
import grails.converters.JSON
import grails.converters.XML
import org.codehaus.groovy.grails.plugins.springsecurity.Secured
import com.collabnet.svnedge.domain.ServerMode
import com.collabnet.svnedge.domain.Repository
import com.collabnet.svnedge.domain.integration.ReplicatedRepository
import com.collabnet.svnedge.domain.RepoTemplate

/**
 * REST API controller for creating and listing repositories
 * <p><bold>URL:</bold></p>
 * <code>
 *   /csvn/api/1/repository
 * </code>
 */
@Secured(['ROLE_USER'])
class RepositoryRestController extends AbstractRestController {

    def authenticateService
    def svnRepoService
    def repoTemplateService

    /**
     * <p>API to retrieve the list of repositories. For each repository, the name, status,
     * svnUrl, and viewvcUrl are returned. Status currently indicates whether the 
     * Unix permissions are set correctly, so it will always be "OK" on Windows.</p>
     *
     * <p><bold>HTTP Method:</bold></p>
     * <code>
     *     GET
     * </code>
     * 
     * <p><bold>XML-formatted return example:</bold></p>
     * <pre>
     * &lt;map&gt;
     *   &lt;entry key="repositories"&gt;
     *     &lt;map&gt;
     *       &lt;entry key="id"&gt;1&lt;/entry&gt;
     *       &lt;entry key="name"&gt;api-test&lt;/entry&gt;
     *       &lt;entry key="status"&gt;OK&lt;/entry&gt;
     *       &lt;entry key="svnUrl"&gt;http://Homegrown/svn/api-test&lt;/entry&gt;
     *       &lt;entry key="viewvcUrl"&gt;http://Homegrown/viewvc/api-test/&lt;/entry&gt;
     *     &lt;/map&gt;
     *   &lt;/entry&gt;
     * &lt;/map&gt;  
     * </pre>    
     */
    def restRetrieve = {
        def result
        Server server = Server.getServer()

        // only list repos in standalone mode; no detail view allowed yet
        if (server.mode != ServerMode.STANDALONE || params.id) {
            response.status = 405
            result = [errorMessage: message(code: "api.error.405")]
        }
        else {
            def repositories = []
            def domainRepos = null
            if (authenticateService.ifAnyGranted(
                    'ROLE_ADMIN,ROLE_ADMIN_REPO,ROLE_ADMIN_HOOKS')) {
                domainRepos = Repository.list([sort: 'name'])
            } else if (server.advancedConfig().listParentPath) {
                def username = authenticateService.principal().username
                domainRepos = svnRepoService.listAuthorizedRepositories(username, true)
            }
            domainRepos?.each {
                def repository = [id: it.id,
                        name: it.name,
                        status: it.permissionsOk ?
                            (it.verifyOk ?
                                message(code: "repository.page.list.instance.permission.ok") :
                                message(code: "repository.page.list.instance.verify.failed")) :
                            message(code: "repository.page.list.instance.permission.needFix"),
                        svnUrl: "${server.svnURL()}${it.name}",
                        viewvcUrl: "${server.viewvcURL(it.name)}"]
                repositories.add(repository)
            }
            result = [repositories: repositories]
        }

        withFormat {
            json { render result as JSON }
            xml { render result as XML }
        }
    }

    /**
     * <p>API to create a repository with the given name. If the optional parameter <code>applyStandardLayout</code>
     * is true, the default "tags, branches, trunk" structure will be created. If the optional parameter 
     * <code>applyTemplateId</code> is provided, that corresponding <code>Template</code> will be used.
     * Use {@link TemplateRestController#restRetrieve} to fetch the list of available templates and ids. The
     * template ID will have precedence over the standard layout.
     * </p>
     * <p>If successful, the method will return the full details of the new repository.</p>
     *
     * <p><bold>HTTP Method:</bold></p>
     * <code>
     *     POST
     * </code>
     *
     * <p><bold>XML-formatted request body example:</bold></p>
     * <pre>
     * &lt;map&gt;
     *   &lt;entry key="name"&gt;new-repo&lt;/entry&gt;
     *   &lt;entry key="applyStandardLayout"&gt;false&lt;/entry&gt;
     *   &lt;entry key="applyTemplateId"&gt;2&lt;/entry&gt;
     * &lt;/map&gt;
     * </pre>    
     * 
     * <p><bold>XML-formatted return example:</bold></p>
     * <pre>
     * &lt;map&gt;
     *   &lt;entry key="message"&gt;Successfully created&lt;/entry&gt;
     *   &lt;entry key="repository"&gt;
     *      &lt;map&gt;
     *        &lt;entry key="id"&gt;1&lt;/entry&gt;
     *        &lt;entry key="name"&gt;new-repo&lt;/entry&gt;
     *        &lt;entry key="status"&gt;OK&lt;/entry&gt;
     *        &lt;entry key="svnUrl"&gt;http://Homegrown/svn/api-test&lt;/entry&gt;
     *        &lt;entry key="viewvcUrl"&gt;http://Homegrown/viewvc/api-test/&lt;/entry&gt;
     *      &lt;/map&gt;
     *   &lt;/entry&gt;
     * &lt;/map&gt;
     * </pre>    
     */
    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
    def restSave = {
        def result = [:]
        def server = Server.getServer()
        def repo = new Repository(name: getRestParam("name"))

        // determine whether a template (standard or custom) is requested;
        // providing a template id will override the "standard template" param,
        // since a template cannot be restored to a non-empty repo
        RepoTemplate template = null
        def templateIdParam = getRestParam("applyTemplateId")
        def applyStandardLayout = Boolean.valueOf(getRestParam("applyStandardLayout"))
        def applyCustomTemplate = (templateIdParam != null && templateIdParam.length() > 0)

        // multi-stage validation for repositories
        repo.validate()
        if (!repo.hasErrors()) {
            repo.validateName()
        }
        if (!repo.hasErrors()) {
            // validate template 
            if (applyCustomTemplate) {
                try {
                    def templateId = (templateIdParam) ? Integer.parseInt(templateIdParam) : null
                    if (templateId && templateId == RepoTemplate.STANDARD_LAYOUT_ID) {
                        applyStandardLayout = true
                        applyCustomTemplate = false
                    }
                    else {
                        template = (templateId) ? RepoTemplate.get(templateId) : null
                        if (!template) {
                            log.warn("Template ID does not reference a template: '${templateIdParam}'")
                            repo.errors.rejectValue("name", "repository.not.created.message.badTemplateId", 
                                    [repo.name, templateIdParam] as Object[], "repo not created")
                        }
                    }
                }
                catch (NumberFormatException nfe) {
                    log.warn("Template ID cannot be parsed: '${templateIdParam}'")
                    repo.errors.rejectValue("name", "repository.not.created.message.badTemplateId",
                            [repo.name, templateIdParam] as Object[], "repo not created")
                }
            }
        }
        if (!repo.hasErrors()) {
            boolean errorCode = svnRepoService.createRepository(repo, applyStandardLayout)
            if (errorCode) {
                repo.errors.rejectValue("name", "repository.not.created.message", [repo.name])
            }
        }
        // no errors? 
        if (!repo.hasErrors()) {
            repo.save()
            repo.refresh()

            // start job to apply template, if requested
            if (applyCustomTemplate) {
                repoTemplateService.copyTemplateForLoad(template, repo)
                def props = [:]
                props.put("ignoreUuid", true)
                props.put("locale", request.locale)
                def userId = loggedInUserInfo(field: 'id') as Integer
                svnRepoService.scheduleLoad(repo, props, userId)
            }

            def repository = [id: repo.id,
                    name: repo.name,
                    status: repo.permissionsOk ?
                            (it.verifyOk ?
                                message(code: "repository.page.list.instance.permission.ok") :
                                message(code: "repository.page.list.instance.verify.failed")) :
                        message(code: "repository.page.list.instance.permission.needFix"),
                    svnUrl: "${server.svnURL()}${repo.name}",
                    viewvcUrl: "${server.viewvcURL(repo.name)}"]

            response.status = 201
            result['message'] = message(code: "api.message.201")
            result['repository'] = repository
            log.info("Repository created via API: ${repo.name}")
        }
        else {
            response.status = 400
            result['errorMessage'] = message(code: "api.error.400")
            log.warn("Failed to create repository '${getRestParam("name")}': ${repo.errors}")
        }

        withFormat {
            json { render result as JSON }
            xml { render result as XML }
        }
    }
}
