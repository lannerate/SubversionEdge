package com.collabnet.svnedge.controller

import org.codehaus.groovy.grails.plugins.springsecurity.Secured
import com.collabnet.svnedge.domain.RepoTemplate

class RepoTemplateController {
    def repoTemplateService

    static allowedMethods = [save: "POST", update: "POST", delete: "POST"]

    def index = {
        redirect(action: "list", params: params)
    }

    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
    def list = {
        params.sort = 'displayOrder'
        def templateList = RepoTemplate.list(params)
        for (RepoTemplate t : templateList) {
            repoTemplateService.substituteL10nName(t, request.locale)
        }
        [repoTemplateInstanceList: templateList, 
                repoTemplateInstanceTotal: RepoTemplate.count()]
    }

    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
    def create = {
        def repoTemplateInstance = new RepoTemplate()
        repoTemplateInstance.properties = params
        return [repoTemplateInstance: repoTemplateInstance]
    }

    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
    def save = {
        if (params.cancelButton) {
            redirect(action: list)
            return
        }

        def repoTemplateInstance = new RepoTemplate(params)
        File templateFile = handleFileUpload(repoTemplateInstance)        
        if (templateFile &&
                 repoTemplateService.saveTemplate(repoTemplateInstance, 
                 templateFile, params.displayFirst ? true : false)) {

            flash.message = message(code: 'repoTemplate.action.created.message',
                    args: [repoTemplateInstance.name])
            redirect(action: "list")
        }
        else {
            if (templateFile && templateFile.exists()) {
                templateFile.delete()
            }
            request.error = message(code: 'default.errors.summary')
            render(view: "create", model: [repoTemplateInstance: repoTemplateInstance])
        }
    }

    // dump files should not be text/plain, but we have to accommodate IE8
    private static final List VALID_TEMPLATE_TYPES = 
            ['application/octet-stream', 'application/zip', 
             'application/x-zip-compressed', 'text/plain']

    private File handleFileUpload(repoTemplateInstance) {
        def uploadedFile = request.getFile('templateUpload')
        log.debug "templateUpload mime-type = " + uploadedFile.contentType
        if (!uploadedFile || uploadedFile.empty) {
            repoTemplateInstance.errors.rejectValue('location', 'repoTemplate.action.save.no.file')

        } else if (!VALID_TEMPLATE_TYPES.contains(uploadedFile.contentType)) {
            repoTemplateInstance.errors.rejectValue('location', 
                    'repoTemplate.action.save.invalid.type')
            log.debug("Attempted to upload an invalid template type: " + 
                    uploadedFile.contentType)
        
        } else {
            File templateDir = repoTemplateService.getUploadDirectory()
            File templateFile = new File(templateDir, uploadedFile.originalFilename)
            uploadedFile.transferTo(templateFile)
            return templateFile
        }
        return null
    }

    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
    def edit = {
        def repoTemplateInstance = RepoTemplate.get(params.id)
        if (!repoTemplateInstance) {
            flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'repoTemplate.label', default: 'RepoTemplate'), params.id])}"
            redirect(action: "list")
        }
        else {
            repoTemplateService.substituteL10nName(repoTemplateInstance, request.locale)
            return [repoTemplateInstance: repoTemplateInstance]
        }
    }

    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
    def update = {
        def repoTemplateInstance = RepoTemplate.get(params.id)
        if (repoTemplateInstance) {
            if (params.version) {
                def version = params.version.toLong()
                if (repoTemplateInstance.version > version) {
                    
                    repoTemplateInstance.errors.rejectValue("version", "default.optimistic.locking.failure", [message(code: 'repoTemplate.label', default: 'RepoTemplate')] as Object[], "Another user has updated this RepoTemplate while you were editing")
                    render(view: "edit", model: [repoTemplateInstance: repoTemplateInstance])
                    return
                }
            }
            repoTemplateInstance.properties = params
            if (!repoTemplateInstance.hasErrors() && repoTemplateInstance.save(flush: true)) {
                flash.message = "${message(code: 'repoTemplate.action.updated.message')}"
                redirect(action: "list", id: repoTemplateInstance.id)
            }
            else {
                render(view: "edit", model: [repoTemplateInstance: repoTemplateInstance])
            }
        }
        else {
            flash.message = "${message(code: 'default.not.found.message', args: [message(code: 'repoTemplate.label', default: 'RepoTemplate'), params.id])}"
            redirect(action: "list")
        }
    }
    
    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
    def updateListOrder = {
        def idList = params['templates[]']
        log.debug("Updating repository template list order: " + 
                idList)
        repoTemplateService.reorderTemplates(idList.split(","))
        render "ok"
    }

    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
    def delete = {
        if (params.id) {
            try {
                if (repoTemplateService.deleteTemplate(params.id)) {
                    flash.message = 
                            message(code: 'repoTemplate.action.deleted.message')
                    redirect(action: "list")
                } else {
                    flash.message = message(code: 'default.not.found.message', 
                            args: [message(code: 'repoTemplate.label', 
                            default: 'RepoTemplate'), params.id])
                    redirect(action: "list")
                }
            }
            catch (org.springframework.dao.DataIntegrityViolationException e) {
                flash.message = message(code: 'default.not.deleted.message', 
                        args: [message(code: 'repoTemplate.label', 
                        default: 'RepoTemplate'), params.id])
                redirect(action: "edit", id: params.id)
            }
        }
        else {
            flash.message = message(code: 'default.not.found.message', 
                    args: [message(code: 'repoTemplate.label', 
                    default: 'RepoTemplate'), params.id])
            redirect(action: "list")
        }
    }
}
