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
package com.collabnet.svnedge.controller

import org.apache.commons.io.FileUtils
import org.codehaus.groovy.grails.plugins.springsecurity.Secured

import com.collabnet.svnedge.ValidationException
import com.collabnet.svnedge.console.DumpBean
import com.collabnet.svnedge.console.SchedulerBean
import com.collabnet.svnedge.domain.*
import com.collabnet.svnedge.domain.integration.CloudServicesConfiguration
import com.collabnet.svnedge.integration.AuthenticationCloudServicesException
import com.collabnet.svnedge.integration.InvalidNameCloudServicesException
import com.collabnet.svnedge.integration.QuotaCloudServicesException
import com.collabnet.svnedge.util.ControllerUtil
import com.collabnet.svnedge.util.ServletContextSessionLock

@Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
class RepoController {

    def cloudServicesRemoteClientService
    def svnRepoService
    def serverConfService
    def packagesUpdateService
    def repoTemplateService
    def statisticsService
    def fileUtil
    def authenticateService
    def lifecycleService
    def replicationService

    @Secured(['ROLE_USER'])
    def index = { 
        boolean isAdmin = false
        boolean isSystemAdmin = false
        if (authenticateService.ifAnyGranted(
                'ROLE_ADMIN,ROLE_ADMIN_REPO,ROLE_ADMIN_HOOKS')) {
            redirect(action: list, params: params)
            return
        } else if (authenticateService.ifAnyGranted(
                'ROLE_ADMIN_SYSTEM,ROLE_ADMIN_USERS')) {
            isAdmin = true
            isSystemAdmin = authenticateService.ifAnyGranted('ROLE_ADMIN_SYSTEM')
        } else {        
            request.info = message(code: 'repository.page.index.welcome')
        }
        return [server: Server.getServer(), isStarted: lifecycleService.isStarted(),
                isAdmin: isAdmin, isSystemAdmin: isSystemAdmin
                ]
    }

    // the delete, save and update actions only accept POST requests
    static allowedMethods = [delete: 'POST', save: 'POST', update: 'POST']

    static final int ROLE_USER_MAX_REPOSITORY_LIST_SIZE = 100    
    
    @Secured(['ROLE_USER'])
    def listMatching = {
        String repoQuery = params['q']
        def username = authenticateService.principal().username
        def repos = svnRepoService.listMatchingRepositories(
                repoQuery, username)?.collect { it.name }
        render(contentType: "text/json") {
            result(repositories: repos ?: [])
        }
    }

    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO', 'ROLE_ADMIN_HOOKS'])
    def list = {
        ControllerUtil.setDefaultSort(params, "name")
        Server server = Server.getServer()
        // in Managed Mode, only super user can access the repo listing
        if (server.managedByCtf() &&
                !authenticateService.ifAnyGranted("ROLE_ADMIN")) {
            flash.error = message(code: "filter.probihited.mode.managed")
            redirect(controller: "status")
            return
        }
    
        def repoList = Repository.list()
        return [repositoryInstanceList: repoList,
                repositoryInstanceTotal: repoList ? repoList.size() : 0,
                server: server
        ]
    }

    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
    def discover = {
        try {
            svnRepoService.syncRepositories();
            flash.message = message(code: 'repository.action.discover.success')

        }
        catch (Exception e) {
            log.error("Unable to discover repositories", e)
            flash.error = message(code: 'repository.action.discover.failure')
        }
        redirect(action: list)
    }

    private Repository selectRepository() {
        def id = params.id
        Repository repo = Repository.get(id)
        if (!repo) {
            def ids = ControllerUtil.getListViewSelectedIds(params)
            if (!ids) {
                flash.error = message(code: 'repository.action.not.found',
                        args: ['null'])
                redirect(action: list)
                return

            } else if (ids.size() > 1) {
                flash.error = message(code: 'repository.action.multiple.unsupported')
                redirect(action: list)
                return
            } else {
                id = ids[0]
                repo = Repository.get(id)
            }
        }
        if (!repo && !flash.error) {
            flash.error = message(code: 'repository.action.not.found',
                    args: [id])
            redirect(action: list)
        }
        return repo
    }

    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
    def loadOptions = {
        def repo = selectRepository()
        if (repo) {
            File loadDir = svnRepoService.getLoadDirectory(repo)
            if (!session["loadRepo" + repo.id] &&
                    !isLoadInProgress(repo, loadDir)) {
                def repoParentDir = serverConfService.server.repoParentDir
                def repoPath = new File(repoParentDir, repo.name).absolutePath
                def headRev = svnRepoService.findHeadRev(repo)
                def repoUUID = svnRepoService.getReposUUID(repo)
                return [repositoryInstance: repo,
                        repoPath: repoPath,
                        headRev: headRev,
                        repoUUID: repoUUID,
                        uploadProgressKey: "loadRepo" + repo.id
                ]
            } else {
                flash.unfiltered_error = message(
                        code: 'loadFileUpload.action.multiple.load.unsupported',
                        args: [createLink(controller: 'job', action: 'list')])
                redirect(action: list)
            }
        }
    }

    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
    def loadFileUpload = {
        if (params.cancelButton) {
            redirect(action: list)
            return
        }
        def repo = selectRepository()
        try {
            if (repo) {
                handleLoadFileUpload(repo)
            }
        } finally {
            session.removeAttribute("loadRepo" + repo.id)
        }
    }

    private void handleLoadFileUpload(repo) {
        boolean ignoreUUID = params.ignoreUuid
        def uploadedFile = request.getFile('dumpFile')
        if (uploadedFile.empty) {
            flash.error = message(code: 'loadFileUpload.action.no.file')
            redirect(action: loadOptions, id: repo.id)
        } else {
            File loadDir = svnRepoService.getLoadDirectory(repo)
            if (!isLoadInProgress(repo, loadDir)) {
                uploadedFile.transferTo(
                        new File(loadDir, uploadedFile.originalFilename))
                def props = [:]
                props.put("ignoreUuid", ignoreUUID)
                props.put("locale", request.locale)
                def userId = loggedInUserInfo(field: 'id') as Integer
                svnRepoService.scheduleLoad(repo, props, userId)
                flash.unfiltered_message = message(
                        code: 'loadFileUpload.action.success',
                        args: [repo.name.encodeAsHTML(),
                                createLink(controller: 'job', action: 'list')])
                redirect(action: list)
            } else {
                flash.unfiltered_error = message(code:
                'loadFileUpload.action.multiple.load.unsupported',
                        args: [createLink(controller: 'job', action: 'list')])
                redirect(action: list)
            }
        }
    }

    private boolean isLoadInProgress(repo, loadDir) {
        return loadDir.listFiles({ return it.isFile() } as FileFilter).length > 0
    }

    def uploadProgress = {
        def key = params.uploadProgressKey
        response.addHeader("Cache-Control", "max-age=0,no-cache,no-store")
        render(contentType: "text/json") {
            uploadStats(session[key] ?: [:])
        }
    }


    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
    def dumpOptions = {
        def repo = selectRepository()
        if (repo) {
            def repoParentDir = serverConfService.server.repoParentDir
            def repoPath = new File(repoParentDir, repo.name).absolutePath
            def headRev = svnRepoService.findHeadRev(repo)
            def dumpDir = serverConfService.server.dumpDir

            DumpBean cmd = flash.dumpBean
            if (!cmd) {
                cmd = new DumpBean()
                bindData(cmd, params)
            }
            return [repositoryInstance: repo,
                    repoPath: repoPath,
                    dumpDir: dumpDir,
                    headRev: headRev,
                    dump: cmd
            ]
        }
    }

    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
    def createDumpFile = { DumpBean cmd ->
        if (params.cancelButton) {
            redirect(action: list)
            return
        }
        if (cmd.hasErrors()) {
            flash.dumpBean = cmd
            redirect(action: 'dumpOptions', params: params)
            return
        }

        def repo = Repository.get(params.id)
        if (!repo) {
            flash.error = message(code: 'repository.action.not.found',
                    args: [params.id])
            redirect(action: list)

        } else {
            try {
                def userId = loggedInUserInfo(field: 'id') as Integer
                cmd.userLocale = request.locale
                def filename = svnRepoService.scheduleDump(cmd, repo, userId)
                flash.message = message(code: 'repository.action.createDumpfile.success',
                        args: [filename])
                redirect(action: 'dumpFileList', params: [id: params.id])
            } catch (ValidationException e) {
                log.debug "Rejecting " + e.field + " with message " + e.message
                if (e.field) {
                    cmd.errors.rejectValue(e.field, e.message)
                } else {
                    cmd.errors.reject(e.message)
                    flash.error = message(code: e.message)
                }
                flash.dumpBean = cmd
                redirect(action: 'dumpOptions', params: params)
            }
        }
    }

    @Secured(['ROLE_USER'])
    def show = {
        def repoId = (params.id) ?: ControllerUtil.getListViewSelectedIds(params)[0]
        redirect(action: firstTabView(), id: repoId)
    }
    
    private String firstTabView() {
        if (Server.server.mode == ServerMode.REPLICA) {
            return 'reports'
        }
/*        return authenticateService.ifAnyGranted('ROLE_ADMIN,ROLE_ADMIN_HOOKS') ?
                'hooksList' : 'dumpFileList'   */
				
		return authenticateService.ifAnyGranted('ROLE_ADMIN,ROLE_ADMIN_HOOKS') ?
				'repoGroupsList' : 'hooksList'
				
    }

    @Secured(['ROLE_USER'])
    def reports = {
        def id = params.id
        if (!id) {
            id = ControllerUtil.getListViewSelectedIds(params)[0]
            params.id = id
        }
        def repo = Repository.get(params.id)
        if (!repo) {
            flash.error = message(code: 'repository.action.not.found',
                    args: [params.id])
            redirect(action: list)

        } else {
            def repoParentDir = serverConfService.server.repoParentDir
            def repoPath = new File(repoParentDir, repo.name).absolutePath
            def username = serverConfService.httpdUser
            def group = serverConfService.httpdGroup
            def minPackedRev = svnRepoService.findMinPackedRev(repo)
            def headRev = svnRepoService.findHeadRev(repo)
            def sharded = svnRepoService.getReposSharding(repo)
            def fsType = svnRepoService.getReposFsType(repo)
            def fsFormat = svnRepoService.getReposFsFormat(repo)
            def repoFormat = svnRepoService.getReposFormat(repo)
            def repoUUID = svnRepoService.getReposUUID(repo)
            def svnVersion = packagesUpdateService.getInstalledSvnVersionNumber()
            def diskUsage = statisticsService.getRepoUsedDiskspace(repo)
            def repSharing = svnRepoService.getReposRepSharing(repo)
            def repoSupport = svnRepoService.getRepoFeatures(repo, fsFormat)

            def timespans = [[index: 0,
                    title: message(code: "statistics.graph.timespan.lastHour"),
                    seconds: 60 * 60, pattern: "HH:mm"],
                    [index: 1,
                            title: message(code: "statistics.graph.timespan.lastDay"),
                            seconds: 60 * 60 * 24, pattern: "HH:mm"],
                    [index: 2,
                            title: message(code: "statistics.graph.timespan.lastWeek"),
                            seconds: 60 * 60 * 24 * 7, pattern: "MM/dd HH:mm"],
                    [index: 3,
                            title: message(code: "statistics.graph.timespan.lastMonth"),
                            seconds: 60 * 60 * 24 * 30, pattern: "MM/dd"]]

            def timespanSelect = timespans.inject([:]) { map, ts ->
                map[ts.index] = ts.title
                map
            }

            return [timespanSelect: timespanSelect,
                    initialGraph: "DISKSPACE_CHART",
                    repositoryInstance: repo,
                    svnUser: username,
                    svnGroup: group,
                    repoPath: repoPath,
                    minPackedRev: minPackedRev,
                    headRev: headRev,
                    sharded: sharded,
                    fsType: fsType,
                    fsFormat: fsFormat,
                    repoFormat: repoFormat,
                    repoUUID: repoUUID,
                    svnVersion: svnVersion,
                    diskUsage: diskUsage,
                    repSharing: repSharing,
                    repoSupport: repoSupport]
        }
    }

    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
    def dumpFileList = {
        def model = reports()
        def repo = Repository.get(params.id)
        if (repo) {
            // set default sort to be date descending, if neither sort 
            // parameter is present
            ControllerUtil.setDefaultSort(params, "date", "desc")
            ControllerUtil.decorateFileClass()
            def sortBy = params.sort
            boolean isAscending = params.order == "asc"
            model["dumpFileList"] =
                svnRepoService.listDumpFiles(repo, sortBy, isAscending)
        }
        return model
    }

	
	@Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
	def listRepoGroup = {
		def repoGroupInstanceList = RepositoryGroups.findAllByRepoid(params.id)
		def repoGroupUrls = []
		def repoGroupInstances =[]
		for(repoGroupInstance in repoGroupInstanceList){
			if(!repoGroupUrls.contains(repoGroupInstance.url)){
				repoGroupUrls.add(repoGroupInstance.url)
				repoGroupInstances.add(repoGroupInstance)
			}
		}
		return [repoGroupInstanceList: repoGroupInstances,
			repoGroupInstanceTotal: repoGroupInstances ? repoGroupInstances.size() : 0,
			repoid:params.id
	]
	}
	
	
	@Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
	def createRepoGroup = {
		def model = [:]
		def repo = Repository.get(params.id)
		def groups = Groups.getAll()
		def userList = getUserList()
		if (repo) {
			model["userList"] = userList
			model["repo"] = repo
			model["groups"] =groups
		}
		return model
	}
	
	@Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
	def editRepoGroup = {
		def model = [:]
		def purl = params.urls
		def id_url = params.id 
		Repository repo = Repository.get(params.id)
		def groups = Groups.getAll()
		def users = getUserList()
		//current groups and users by editing
		def listrepoGroup = []
		def listrepoUser = []
		//last groups and users by adding
		def groupsLast = []
		def usersLast = []
		//ids
		def groupsids =[]
		def usersids = []
		
		def groupsRemove = []
		def repoGroupList
		def urls = []
		RepositoryGroups rp
		if (repo) {
			repoGroupList = RepositoryGroups.findAllByRepoid(params.id)
			if(repoGroupList&&repoGroupList.size>0){
				for(repogroup in repoGroupList){

				   if(purl==repogroup.url){
					   if(!groupsids.contains(repogroup.groupsid)&&"-2"!=repogroup.groupsid){
						   listrepoGroup.add(repogroup)
						   groupsids.add(repogroup.groupsid)
					   }
					   if(!usersids.contains(repogroup.userid)&&"-1"!=repogroup.userid){
						   listrepoUser.add(repogroup)
						   usersids.add(repogroup.userid)
					   }
					   rp = repogroup
				   }
				}
			}
			
			//calc last groups
			groupsLast = groups
			if(groupsids){
				for(gid in groupsids){
					def g = Groups.get(gid)
					groupsLast.remove(g)
				}
			}
			//calc last users
			usersLast = users
			if(usersids){
				for(uid in usersids){
					def g = User.get(uid)
					groupsLast.remove(g)
				}
			}
			
			
			model["listrepoGroup"] = listrepoGroup
			model["groupsLast"] =groupsLast
			
			model["listrepoUser"] = listrepoUser
			model["usersLast"] =usersLast
			
			model["groups"] =groups
			model["users"] =users
			model["rp"] = rp
			model["repo"] =repo
		}
		return model
    }
	
	
	
	@Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
	def deleteRepoGroup = {
		def url = params.urls
		def repoid =params.id
		
		def repoGroupList = RepositoryGroups.findAllByRepoid(repoid)
		if(repoGroupList.size>0){
			for(repogroup in repoGroupList){
			   try {
				    if(url==repogroup.url)
						repogroup.delete()
				}catch (Exception e) {
					log.warn("Could not create repogroup", e)
				}
			 
			}
		}
		
		try{
			SaveAccessRules(getAccessrules())
		}catch(Exception e) {
					log.warn("Could not create repogroup", e)
		}
		
		def repogroups = RepositoryGroups.findAllByRepoid(repoid)
		if(repogroups){
		 redirect(action: listRepoGroup,id:repoid)
		}else{
		 redirect(action:list)
		}
	}
	
	
	@Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
	def saveRepoGroup = {
		String idrw = params.idrw
		String uidrw = params.uidrw
		def uri = params.uri.trim()
		def url = params.url.trim()
		def repoid =params.repoid.trim()
		RepositoryGroups repoGroup
		
		if(url){
			if(url.indexOf("/")==-1){
				url="/"+url
			}
		}
		def repoGroupList = RepositoryGroups.findAllByRepoid(repoid)
		if(repoGroupList.size>0){
			for(repogroup in repoGroupList){
			   try {
				    if(url==repogroup.url)
						repogroup.delete()
				}catch (Exception e) {
					log.warn("Could not create repogroup", e)
				}
			 
			}
		}
		
		
		if(idrw.lastIndexOf(',')!=-1){
			def idrwArr = idrw.split(',')
			for (b in idrwArr) {
				def a = b.split('=')
				def group = Groups.get(a[0])
				if(a.length>1&&group){
				 repoGroup = new RepositoryGroups(repoid:repoid,
					groupsid:a[0],groupsname:group.name,userid:'-1',username:'-1',rw:a[1],uri:uri,url:url)
				 try {
					 repoGroup.save(flush: true)
				 }catch (Exception e) {
					 log.warn("Could not create roles", e)
				 }
				}
			}
		}
		
		if(uidrw.lastIndexOf(',')!=-1){
			def uidrwArr = uidrw.split(',')
			for (b in uidrwArr) {
				def a = b.split('=')
				def user = User.get(a[0])
				if(a.length>1&&user){
				 repoGroup = new RepositoryGroups(repoid:repoid,groupsid:'-2',groupsname:'-2',
					userid:a[0],username:user.username,rw:a[1],uri:uri,url:url)
				 try {
					 repoGroup.save(flush: true)
				 }catch (Exception e) {
					 log.warn("Could not create roles", e)
				 }
				}
			}
		}
		
		
			if(!repoGroup){
				//flash.message = "repoGroup not found with id $params.id"
				render(contentType: "text/json") {
					[result: "fail"]
				}
				return
			}else{
						 
		  if(SaveAccessRules(getAccessrules())){
			   //flash.clear()
			   render(contentType: "text/json") {
					[result: "sucess"]
				 }
			}else{
			//flash.clear()
			render(contentType: "text/json") {
				[result: "fail"]
			 }
			}
		}
	}
	
	
	def getAccessrules(){

		def groupContext =""""""
		def groupList = Groups.getAll()
		if(groupList&&groupList.size>0){
			groupContext+="""
[groups]"""			

			def unames=""
			def userList = getUserList()
					for(group in groupList){
						def gname =group.name
						def selectedUsers = group?.people.collect { it.id }
						unames=""
						if(userList){
							for(user in userList){
								if(selectedUsers.contains(user.id)){
									def uname = user.username
									unames+=uname+","
								}
							}
						}
				 
				 if(unames!=""){
				 groupContext+="""
${gname} = ${unames}"""
				 }
			}
		}
		
		def repogroupstr=""""""
		def repoGroupList = RepositoryGroups.getAll()
		if(repoGroupList&&repoGroupList.size>0){
			def reposids =[]
			for(repogroup in repoGroupList){
				 def repoid     =repogroup.repoid
				 def groupsid   =repogroup.groupsid
				 def groupsname =repogroup.groupsname
				 def uri		=repogroup.uri
				 def rw		    =repogroup.rw
				 if(!reposids.toList().contains(repoid)){
					 reposids.add(repoid)
				 }

			}
			
			if(reposids){
				for(id in reposids){
					def repos = RepositoryGroups.findAllByRepoid(id)
					if(repos){
/*					RepositoryGroups r = repos[0]
					def wuri = r.uri
					def wurl = r.url
					if(wurl){
						if(wurl.indexOf("/")==-1){
							wurl+="/"
						}
					}
				    repogroupstr+="""
[${wuri}:${wurl}]"""*/	
					 //add single collectioins
					def refGroups = []
					def urlArr = []
					for(rs in repos){
						def url = rs.url
						if(!urlArr.toList().contains(url)){
							urlArr.add(url)
							refGroups.add(rs)
						}
					}
					
					 for(ref in refGroups){
						 def wuri = ref.uri
						 def wurl = ref.url
						 if(wurl){
							 if(wurl.indexOf("/")==-1){
								 wurl="/"+wurl
							 }
						 }
						 repogroupstr+="""
[${wuri}:${wurl}]"""
					     def grouprule=""""""
						 for(re in repos){
							 if(wurl==re.url&&("-2"!=re.groupsid)){
							 def repoid     =re.repoid
							 def groupsid   =re.groupsid
							 def groupsname =re.groupsname
							 def uri		=re.uri
							 def rw		    =re.rw
							 if(rw=="none"){
							 	rw=""
							 }
							 grouprule+="""
@${groupsname} = ${rw}"""
							 }
							 if(wurl==re.url&&("-1"!=re.userid)){
								 def repoid     =re.repoid
								 def userid     =re.userid
								 def username   =re.username
								 def uri		=re.uri
								 def urw		=re.rw
								 if(urw=="none"){
									 urw=""
								 }
								 grouprule+="""
${username} = ${urw}"""
								 }
						}
						 repogroupstr+="""${grouprule}"""
					 } 
				  }
				}
			}
		}
		
		String accessrule = """
${groupContext}
${repogroupstr}
"""
		
		return accessrule

	}
	
	private List<User> getUserList() {
		
				def users = User.list().sort({it.username})
/*				String principal = authenticateService.principal().getUsername()
		
				// remove active session user from this list (cannot modify own privileges)
				users = users.findAll {it -> it.username != principal}*/
		
				return users
	}
	
	
	@Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
	def repoGroupsList = {
		def model = reports()
		def repo = Repository.get(params.id)
		def groups = Groups.getAll()
		if (repo) {
			// set default sort to be date descending, if neither sort
			// parameter is present
	/*		ControllerUtil.setDefaultSort(params, "date", "desc")
			ControllerUtil.decorateFileClass()
			def sortBy = params.sort
			boolean isAscending = params.order == "asc"*/
			RepositoryGroups repoGroups = RepositoryGroups.findAllByRepoid(repo.id+'')[0] 
			model["repoGroups"] = repoGroups
			model["groups"] =groups
		}
		return model
	}
	
	
    /**
     * fetches a JSON representation of all the dumps / backups for all repos
     */
    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
    def dumpFileListAll = {
        ControllerUtil.setDefaultSort(params, "name")
        def sortBy = params.sort
        boolean isAscending = params.order == "asc"

        def repoDumps = svnRepoService.listBackupsOnFilesystem(sortBy, isAscending)
        def repoDumpNames = [:]
        repoDumps.each { k, v ->
            repoDumpNames.put(k, v.collect { it.name })
        }

        render(contentType: "text/json") {
            result(repoDumps: repoDumpNames)
        }
    }

    /**
     * fetches a JSON representation of the cloud backups for all repos
     */
    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
    def cloudBackupList = {
        def projectMap = cloudServicesRemoteClientService.retrieveSvnProjects()
        def projects = projectMap.values().collect {it}
        projects.sort { a, b ->
            a.name.compareTo(b.name)
        }
        render(contentType: "text/json") {
            result(projects: projects)
        }
    }
    
    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
    def bkupSchedule = {
        def model = reports()
        def repo = Repository.get(params.id)
        if (!repo) {
            flash.error = message(code: 'repository.action.not.found',
                    args: [id])
            redirect(action: list)

        } else {
            DumpBean cmd = flash.dumpBean
            if (!cmd) {
                params.type = params.type ?: 'none'
                cmd = new DumpBean()
                bindData(cmd, params)
            }
            model["dump"] = cmd
            
            def repoMap = [repoId: repo.id, repoName: repo.name, cloudName: repo.cloudName, jobCount: 0]
            model['repoList'] = [repoMap]
            def repoBackupJobList = []
            def jobs = svnRepoService.retrieveScheduledJobs(repo)
            if (jobs) {
                repoMap.jobCount = jobs.size()
                for (b in jobs) {
                    def job = populateJobMap(b)
                    job.repoId = repo.id
                    job.repoName = repo.name.encodeAsHTML()
                    job.cloudName = repo.cloudName
                    repoBackupJobList << job
                }
            }
            model['repoBackupJobMap'] = [(repo.id): repoBackupJobList]
        }
        def cloudConfig = CloudServicesConfiguration.getCurrentConfig()
        model['cloudRegistrationRequired'] = !cloudConfig || !cloudConfig.domain
        model['cloudEnabled'] = cloudConfig?.enabled
        model['verifyEnabled'] = true
        return model
    }
    
    
    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
    def addBkupSchedule = { DumpBean cmd ->
        params.remove('_action_addBkupSchedule')
        updateBkupSchedule(cmd)
        if (flash.error) {
            flash.tabPane = 'newJobs'
        }
    }

    private def parseCombinedIds() {
        def idList = []
        def combinedIdList = ControllerUtil.getListViewSelectedIds(params)
        if (combinedIdList) {
            for (id in combinedIdList) {
                int delim = id.indexOf('__')
                if (delim > 0) {
                    idList << [id.substring(0, delim), id.substring(delim + 2)]
                } else {
                    idList << [id, null]
                }
            }
        }
        return idList
    }

    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
    def deleteBkupSchedule = {
        params.remove('_action_deleteBkupSchedule')
        def idList = parseCombinedIds()
        if (idList) {
            idList.each {
                def repoId = it[0]
                def jobId = it[1]
                if (!jobId) {
                    flash.error = message(code: 'repository.action.job.notFound')
                    return
                }
                svnRepoService.deleteScheduledJob(jobId)
            }
        }
        flash.message = message(code: 'repository.action.job.deleted')
        bkupRedirect()
    }

    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
    def updateBkupSchedule = { DumpBean cmd ->
        params.remove('_action_updateBkupSchedule')
        // handle single or multiple repo backups with same action
        def repoIdList = parseCombinedIds()
        if (!repoIdList) {
            repoIdList = [[params.id, null]]
        }
        def type = params.type
        if (type == 'dump'  && cmd.deltas) {
            type = 'dump_delta'
        }
        try {
            def cloudConfig = CloudServicesConfiguration.getCurrentConfig()
            def projects = (type == "cloud" && 
                            cloudConfig && cloudConfig.domain) ?
                cloudServicesRemoteClientService.listProjects() : null
            repoIdList.each {
                def repoId = it[0]
                def jobId = it[1]
                Repository repo = Repository.get(repoId)
                if (!repo) {
                    flash.error = message(code: 'repository.action.not.found',
                            args: [params.id])
                    redirect(action: list)
                    return
                }

                if (type == "cloud") {
                    if (!cloudConfig) {
                        flash.error = message(code: 'repository.action.bkupSchedule.cloud.not.configured')
                        redirect(controller: 'setupCloudServices', action: 'index')
                        return
                    }

                    if (confirmCloudProject(repo, projects)) {
                        cmd.cloud = true
                        scheduleBackup(cmd, repo, type, jobId)
                    }
                } else if (type == "verify") {
                    def userId = loggedInUserInfo(field: 'id') as Integer
                    svnRepoService.scheduleVerifyJob(cmd.schedule, repo, userId, jobId, request.locale)
                    flash.unfiltered_message = message(code: 'repository.action.updateVerifyJob.success')
                    MailConfiguration config = MailConfiguration.getConfiguration()
                    if (!config.enabled) {
                        flash.unfiltered_message += " " + message(code: 'mailConfiguration.required')
                    }
                } else if (type == "none") {
                    flash.error = message(code: 'repository.action.updateBkupSchedule.none')
                } else {
                    scheduleBackup(cmd, repo, type, jobId)
                }
            }
        } catch (QuotaCloudServicesException quota) {
            flash.error = message(code: 'repository.action.bkupSchedule.cloud.quota.met')
        } catch (AuthenticationCloudServicesException auth) {
            flash.unfiltered_error = message(code: auth.message,
                    args: [1, createLink(controller: 'setupCloudServices', action: 'index')])
        }
        bkupRedirect()
    }
    
    /**  redirect based on origin (single repo or multiple-repo backup) */
    private void bkupRedirect() {
        params.keySet().collect({it}).each() {
            if (it.startsWith('listViewItem_')) {
                params.remove(it)
            }
        }
        if (params.id) {
            redirect(action: 'bkupSchedule', params: params)
        }
        else {
            redirect(action: 'bkupScheduleMultiple', params: params)
        }
    }

    private boolean confirmCloudProject(repo, projects) throws QuotaCloudServicesException {
        def cloudName = params["cloudName${repo.id}"]
        if (cloudName) {
            for (def p: projects) {
                if (p['shortName'] == cloudName) {
                    flash.error = message(code:
                    "repository.action.bkupSchedule.cloud.existing.project",
                            args: [cloudName])
                    return false
                }
            }
            repo.cloudName = cloudName
            repo.save()
            return createProject(repo)
        }

        cloudName = repo.cloudName ?: repo.name
        for (def p: projects) {
            if (p['shortName'] == cloudName) {
                return true
            }
        }
        return createProject(repo)
    }

    private boolean createProject(repo) throws QuotaCloudServicesException {
        try {
            // creating the project in request thread, so invalid name error
            // can be handled.  Then in the async call, the project will be
            // looked up and not created again.
            cloudServicesRemoteClientService.createProject(repo)
            // creating the service can take quite a bit of time, so starting
            // the process here, so it is less likely to time out when the 
            // backup job runs
            runAsync {
                try {
                    cloudServicesRemoteClientService.setupProjectAndService(repo)
                } catch (Exception e) {
                    log.warn(
                        "Exception creating svn service for backup of repo: " + 
                        repo.name, e)
                }
            }
            return true
        } catch (InvalidNameCloudServicesException invalidName) {
            flash.error = message(code: 'repository.action.bkupSchedule.cloud.name.invalid',
                    args: [(repo.cloudName ?: repo.name)])
            flash["nameAdjustmentRequired${repo.id}"] = true
        }
        return false
    }

    private void scheduleBackup(DumpBean cmd, Repository repo, def type, def jobId) {
        try {
            cmd.userLocale = request.locale
            cmd.hotcopy = (type == 'hotcopy')
            cmd.deltas = (type == 'dump_delta')
            cmd.backup = true
            if (cmd.hasErrors()) {
                flash.dumpBean = cmd
                flash.error = "There were errors"
            }
            else {
                svnRepoService.scheduleDump(cmd, repo, null, jobId)
                flash.message = message(code: 'repository.action.updateBkupSchedule.success')
            }
        }
        catch (ValidationException e) {
            log.debug "Rejecting " + e.field + " with message " + e.message
            if (e.field) {
                cmd.errors.rejectValue(e.field, e.message)
            } else {
                cmd.errors.reject(e.message)
                flash.error = message(code: e.message)
            }
            flash.dumpBean = cmd
        }
    }

    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
    def bkupScheduleMultiple = { DumpBean cmd ->

        def model = [:]
        def repoBackupJobMap = [:]
        def repoList = []
        
        // fetch list of repositories using request params to sort if possible, then add backup trigger info
        def listParams = [:]
        listParams.sort = "name"
        listParams.order = (params.sort == "repoName") ? params.order : null
        def backups = svnRepoService.retrieveScheduledBackups()
        def jobMap = [:]
        if (backups) {
            for (b in backups) {
                def repoId = b.repoId
                def repoJobs = jobMap[repoId]
                if (!repoJobs) {
                    repoJobs = []
                    jobMap[repoId] = repoJobs
                }
                repoJobs << b
            }
        }
        Repository.list(listParams).each {
            def repoMap = [:]
            repoMap.repoId = it.id
            repoMap.repoName = it.name.encodeAsHTML()
            repoMap.cloudName = it.cloudName
            repoList << repoMap
            
            def currentJobs = jobMap[it.id]
            if (currentJobs) {
                def jobList = []
                repoBackupJobMap[it.id] = jobList
                for (b in currentJobs) {
                    def job = populateJobMap(b)
                    job.repoName = it.name
                    job.cloudName = it.cloudName
                    jobList << job
                }
                repoMap.jobCount = currentJobs.size()
            } else {
                repoMap.jobCount = 0
            }
        }

        // sort the resulting job Collection according to params if needed
        if (params.sort && params.sort != "repoName") {
            repoBackupJobList = repoBackupJobList.sort { it."${params.sort}"}
            if (params.order == "desc") {
                repoBackupJobList = repoBackupJobList.reverse()
            }
        }

        // add to model
        model["dump"] = cmd
        model["repoList"] = repoList
        model["repoBackupJobMap"] = repoBackupJobMap
        def cloudConfig = CloudServicesConfiguration.getCurrentConfig()
        model['cloudRegistrationRequired'] =  !cloudConfig || !cloudConfig.domain
        model['cloudEnabled'] = cloudConfig?.enabled
        return model
    }
    
    private def populateJobMap(Map b) {
        def job = [:]
        job.putAll(b)
        if (b.jobName == RepoVerifyJob.jobName) {
            job.typeCode = "verify"
            job.type = message(code: "repository.page.bkupSchedule.type.verify")
        }
        else {
            job.typeCode = b.cloud ? 'cloud' : (b.deltas ? 'dump_delta' : (b.hotcopy ? "hotcopy" : 'dump'))
            job.type = (b.cloud) ? message(code: "repository.page.bkupSchedule.type.cloud") : (b.deltas) ?
                    message(code: "repository.page.bkupSchedule.type.fullDumpDelta") : (b.hotcopy) ?
                    message(code: "repository.page.bkupSchedule.type.hotcopy") :
                    message(code: "repository.page.bkupSchedule.type.fullDump")
        }
        job.keepNumber = b.numberToKeep
        DumpBean db = DumpBean.fromMap(b)
        job.schedule = db.schedule
        job.scheduleFormatted = formatSchedule(db.schedule)
        return job
    }

    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
    def downloadDumpFile = {
        fileAction('dumpFileList') { repo, filename ->
            def contentType = filename.endsWith(".zip") ?
                    "application/zip" : "application/octet-stream"
            response.setContentType(contentType)
            response.setHeader("Content-disposition",
                    'attachment;filename="' + filename + '"')
            if (!svnRepoService.copyDumpFile(filename, repo, response.outputStream)) {
                throw new FileNotFoundException()
            }
        }
        return null
    }
    
    private def fileAction(String tabAction, Closure c) {
        def repo = Repository.get(params.id)
        if (repo) {
            def ids = ControllerUtil.getListViewSelectedIds(params)
            def filename = ids ? ids[0] : params.filename
            if (filename) {
                try {
                    c(repo, filename)
                    
                } catch (FileNotFoundException e) {
                    flash.error = message(code: 'repository.action.file.not.found',
                                          args: [filename])
                    redirect(action: tabAction, id: params.id)
                }
            } else {
                flash.error = message(code: 'repository.action.file.not.found',
                                      args: [null])
                redirect(action: tabAction, id: params.id)
            }

        } else {
            flash.error = message(code: 'repository.action.not.found',
                                  args: [params.id])
            redirect(action: 'list')
        }
    }

    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
    def deleteDumpFiles = {
        def repo = Repository.get(params.id)
        if (repo) {
            def ids = ControllerUtil.getListViewSelectedIds(params)
            if (ids) {
                ids.each { filename ->
                    try {
                        // TODO messaging needs improvement to support
                        // multiple file delete
                        if (svnRepoService.deleteDumpFile(filename, repo)) {
                            flash.message = message(code: 'repository.action.deleteDumpFile.success',
                                    args: [filename])

                        } else {
                            flash.error = message(code: 'repository.action.deleteDumpFile.fail',
                                    args: [filename])
                        }
                    } catch (FileNotFoundException e) {
                        flash.error = message(code: 'repository.action.downloadDumpFile.not.found',
                                args: [filename])
                    }
                }
            } else {
                flash.error = message(code: 'repository.action.downloadDumpFile.not.found',
                        args: [null])
            }
        } else {
            flash.error = message(code: 'repository.action.not.found',
                    args: [params.id])
        }
        redirect(action: 'dumpFileList', id: params.id)
    }

    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
    def delete = {
        def repo = Repository.get(params.id)
        if (repo) {
            try {
                svnRepoService.archivePhysicalRepository(repo)
                def msg = svnRepoService.removeRepository(repo)
                flash.message = msg
                redirect(action: list)
            }
            catch (org.springframework.dao.DataIntegrityViolationException e) {
                flash.error = message(code: 'repository.action.cant.delete',
                        [params.id] as String[])
                redirect(action: firstTabView(), id: params.id)
            }
        }
        else {
            flash.error = message(code: 'repository.action.not.found',
                    [params.id] as String[])
            redirect(action: list)
        }
    }

    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
    def deleteMultiple = {
        // in Managed Mode, local delete is prohibited
        if (Server.server.mode == ServerMode.MANAGED) {
            flash.error = message(code: "filter.probihited.mode.managed")
            redirect(action: "list")
            return
        }

        ControllerUtil.getListViewSelectedIds(params).each {
            def repo = Repository.get(it)
            if (repo) {
                def repoName = repo.name
                try {
                    svnRepoService.deletePhysicalRepository(repo)
                    svnRepoService.removeRepository(repo)
                    def msg = message(code: 'repository.deleted.message', args: [repoName])
                    if (flash.message) {
                        msg = flash.message + "\n" + msg
                    }
                    flash.message = msg
                }
                catch (org.springframework.dao.DataIntegrityViolationException e) {
                    def msg = message(code: 'repository.not.deleted.message', args: [repoName])
                    if (flash.error) {
                        msg = flash.error + "\n" + msg
                    }
                    flash.error = msg
                }
            }
            else {
                def msg = message(code: 'repository.action.not.found', args: [params.id])
                if (flash.error) {
                    msg = flash.error + "\n" + msg
                }
                flash.error = msg
            }
        }
        redirect(action: list)
    }

    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
    def updatePermissions = {

        def repo = Repository.get(params.id)
        if (repo && svnRepoService.validateRepositoryPermissions(repo)) {
            repo.setPermissionsOk(true)
            repo.save(validate: false, flush: true)
            flash.message = message(code: 'repository.action.permissions.set.ok')
            redirect(action: firstTabView(), id: params.id)
        }
        else {
            flash.error = message(code: 'repository.action.permissions.set.notOk')
            redirect(action: firstTabView(), id: params.id)
        }
    }

    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
    def create = {
        // in Managed Mode, local create is prohibited
        if (Server.server.mode == ServerMode.MANAGED) {
            flash.error = message(code: "filter.probihited.mode.managed")
            redirect(action: "list")
            return
        }
        def repo = request.repo
        if (!repo) {
            repo = new Repository()
            repo.properties = params
        }
        
        def templates = repoTemplateService.retrieveActiveTemplates()
        if (templates) {
            for (RepoTemplate t : templates) {
                substituteL10nName(t)
            }
        } else {
            RepoTemplate t = new RepoTemplate(
                    name: message(code: 'repoTemplate.default.empty.label'))
            // can't set id in ctor, not sure why
            t.id = 1
            templates = [t]
        }
        CloudServicesConfiguration csConfig = 
                CloudServicesConfiguration.currentConfig
        boolean showCloud = csConfig && csConfig.enabled && csConfig.domain
        return [repo: repo, templateList: templates,
                showCloudBackups: showCloud]
    }

    private void substituteL10nName(RepoTemplate template) {
        if (template.name.startsWith('l10n_')) {
            template.discard()
            template.name = message(code: template.name[5..-1])
        }
    }

    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
    def save = {
        // in Managed Mode, local create is prohibited
        if (Server.server.mode == ServerMode.MANAGED) {
            flash.error = message(code: "filter.probihited.mode.managed")
            redirect(action: "list")
            return
        }
        def repo = new Repository(params)
        def success = false
        repo.validate()

        if (repo.validateName() && !repo.hasErrors()) {
            
            if (params.initOption == 'useBackup' && !params.initOptionSelected) {
                request.error = message(code: 'repository.action.save.no.backup.selected')
                request.repo = repo
                forward(action: 'create')
                return  
            } else if (params.initOption == 'useCloud' && !params.cloudBackup) {
                request.error = message(code: 'repository.action.save.no.cloud.backup.selected')
                request.repo = repo
                forward(action: 'create')
                return  
            }


            int templateId = params.templateId as int
            def isTemplate = (params.initOption == 'useTemplate' && 
                    templateId == RepoTemplate.STANDARD_LAYOUT_ID)
            def result = svnRepoService.createRepository(repo, isTemplate)
            if (result == 0) {
                repo.save(flush: true)
                if (params.initOption == 'useBackup' && params.initOptionSelected) {
                    String backupDir = params.initOptionSelected.split("/")[0]
                    String dumpFileName = params.initOptionSelected.split("/")[1]
                    File dumpFile = new File(new File(Server.getServer()
                            .dumpDir, backupDir), dumpFileName)
                    scheduleLoad(repo, dumpFile, false)
                    
                } else if (params.initOption == 'useTemplate' && 
                        templateId > RepoTemplate.STANDARD_LAYOUT_ID) {
                    String templateDir = repoTemplateService.getTemplateDirectory()
                    String filename = RepoTemplate.get(templateId).location
                    File templateFile = new File(templateDir, filename)
                    log.debug(templateDir + " file=" + filename)
                    scheduleLoad(repo, templateFile, true)

                } else if (params.initOption == 'useCloud' && params.cloudBackup) {
                    int projectId = params.cloudBackup as int
                    def userId = loggedInUserInfo(field: 'id') as Integer            
                    cloudServicesRemoteClientService.loadSvnrdumpProject(repo,
                            projectId, userId, request.locale)
                    flash.unfiltered_message = message(
                        code: 'repository.action.save.success.loading',
                        args: [createLink(controller: 'job', action: 'list')])
        
                }
                else {
                    flash.message = message(code: 'repository.action.save.success')
                }

                redirect(action: firstTabView(), id: repo.id)
                success = true
            } else {
                repo.errors.reject('repository.action.save.failure')
            }
        }
        if (!success) {
            request.error = message(code: 'default.errors.summary')
            request.repo = repo
            forward(action: 'create')
        }
    }

    private void scheduleLoad(Repository repo, 
            File dumpFile, boolean ignoreUUID) {
        File loadDir = svnRepoService.getLoadDirectory(repo)
        FileUtils.copyFileToDirectory(dumpFile, loadDir)
        
        def props = [:]
        props.put("ignoreUuid", ignoreUUID)
        props.put("locale", request.locale)
        def userId = loggedInUserInfo(field: 'id') as Integer
        svnRepoService.scheduleLoad(repo, props, userId)
        flash.unfiltered_message = message(
                code: 'repository.action.save.success.loading',
                args: [createLink(controller: 'job', action: 'list')])
    }
    
    private static final String ACCESS_RULES_LOCK_KEY = "access_rules_lock"
    
    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
    def showAuthorization = {
        User owner
        ServletContextSessionLock lock = 
                ServletContextSessionLock.obtain(session, ACCESS_RULES_LOCK_KEY)
        if (lock) {
            lock.release(session)
            lock = null
        } else {
            lock = ServletContextSessionLock.peek(session, ACCESS_RULES_LOCK_KEY)
            owner = User.get(lock.userId)
        }
        [accessRules: serverConfService.readSvnAccessFile(),
                authzEnabled: AdvancedConfiguration.getConfig().pathAuthz,
                lock: lock, lockOwner: owner]
    }

    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
    def cancelEditAuthorization = {
        ServletContextSessionLock lock = 
                ServletContextSessionLock.obtain(session, ACCESS_RULES_LOCK_KEY)
        if (lock) {
            lock.release(session)
        }
        redirect(action: 'showAuthorization')
    }

    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
    def editAuthorization = {
        // in Managed Mode, local edit of access rules is prohibited
        if (Server.server.mode == ServerMode.MANAGED) {
            flash.error = message(code: "filter.probihited.mode.managed")
            redirect(action: "list")
            return
        }
        flash.clear()
        ServletContextSessionLock lock = 
                ServletContextSessionLock.obtain(session, ACCESS_RULES_LOCK_KEY)
        if (lock) {
            lock.userId = loggedInUserInfo(field:'id') as int
            String accessRules = serverConfService.readSvnAccessFile()
            def command = new AuthzRulesCommand(fileContent: accessRules)
            if (Server.getServer().forceUsernameCase) {
                request['unfiltered_warn'] = message(code: "repository.page.editAuthorization.caseNormalization",
                        args: [createLink(controller: 'server', action: 'editAuthentication')])
            }
            return [authRulesCommand: command]
        } else {
            redirect(action: 'showAuthorization')
        }
    }
	
	
	
	def editAuthorizationInvoke() {
		// in Managed Mode, local edit of access rules is prohibited
		if (Server.server.mode == ServerMode.MANAGED) {
			flash.error = message(code: "filter.probihited.mode.managed")
			//redirect(action: "list")
			return
		}
		flash.clear()
		ServletContextSessionLock lock =
				ServletContextSessionLock.obtain(session, ACCESS_RULES_LOCK_KEY)
		if (lock) {
			lock.userId = loggedInUserInfo(field:'id') as int
			String accessRules = serverConfService.readSvnAccessFile()
			def command = new AuthzRulesCommand(fileContent: accessRules)
			if (Server.getServer().forceUsernameCase) {
				request['unfiltered_warn'] = message(code: "repository.page.editAuthorization.caseNormalization",
						args: [createLink(controller: 'server', action: 'editAuthentication')])
			}
			return [authRulesCommand: command]
		} else {
			return
		}
	}
	


		
	void testEditAuthorization() {
		
				this.serverConfService = serverConfService
				this.metaClass.loggedInUserInfo = { return 1 }
		
				// should fetch an svn_access_file from services
				def model = this.editAuthorization()
				assertNotNull "Expected 'authRulesCommand' model object",
					model.authRulesCommand
			}
		
	boolean SaveAccessRules(String accessrules) {
				try{
					this.serverConfService = serverConfService
					//this.metaClass.loggedInUserInfo = { return 1 }
			
					// obtain lock
					this.editAuthorizationInvoke()
			
					// save the original file to restore after test
					String original = serverConfService.readSvnAccessFile()
			
					// content we will submit to controller
					String testFile = accessrules
					def cmd = new AuthzRulesCommand(fileContent: testFile)
					this.saveAuthorizationInvoke(cmd)
				// restore original
				//serverConfService.writeSvnAccessFile(original)
				}catch(Exception e){
					flash.error = message(
                        code: 'repository.action.saveAuthorization.failure')
					flash.message = null
					return false
				}
				return true
	}
			

	
	@Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
	def saveAuthorizationInvoke = { AuthzRulesCommand cmd ->
		// in Managed Mode, local edit of access rules is prohibited
		if (Server.server.mode == ServerMode.MANAGED) {
			flash.error = message(code: "filter.probihited.mode.managed")
			//redirect(action: "list")
			return
		}
		ServletContextSessionLock existingLock =
				ServletContextSessionLock.peek(session, ACCESS_RULES_LOCK_KEY)
		if (!existingLock) {
			log.warn "RepoController.saveAuthorization was called without an existing lock."
			flash.warn = message(code: 'repository.action.saveAuthorization.not.locked')
			//redirect(action: 'showAuthorization')
			return
		}
		ServletContextSessionLock lock =
				ServletContextSessionLock.obtain(session, ACCESS_RULES_LOCK_KEY)
		if (!lock) {
			log.warn "RepoController.saveAuthorization was called without an owning the lock."
			flash.warn = message(code: 'repository.action.saveAuthorization.not.locked')
			//redirect(action: 'showAuthorization')
			return
		}

		def result = serverConfService.validateSvnAccessFile(
				cmd.fileContent)
		def exitStatus = Integer.parseInt(result[0])

		if (exitStatus != 0) {
			def err = result[2].split(": ")
			if (err.length == 3) {
				def line = err[1].split(":")
				flash.error = message(code:
				'repository.action.saveAuthorization.validate.failure.lineno',
						args: [line[line.length - 1], err[2]])
			} else {
				flash.error = message(code:
				'repository.action.saveAuthorization.validate.failure.nolineno',
						args: [err[err.length - 1]])
			}

			flash.message = null
		} else {
			if (!cmd.hasErrors() && serverConfService.writeSvnAccessFile(
					cmd.fileContent)) {
				lock.release(session)
				flash.message = message(
						code: 'repository.action.saveAuthorization.success')
				flash.error = null
				//redirect(action: 'showAuthorization')
				return
			} else {
				flash.error = message(
						code: 'repository.action.saveAuthorization.failure')
				flash.message = null
			}
		}
		return
		//render(view: 'editAuthorizationInvoke', model: [authRulesCommand: cmd])
	}
	
	
    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
    def saveAuthorization = { AuthzRulesCommand cmd ->
        // in Managed Mode, local edit of access rules is prohibited
        if (Server.server.mode == ServerMode.MANAGED) {
            flash.error = message(code: "filter.probihited.mode.managed")
            redirect(action: "list")
            return
        }
        ServletContextSessionLock existingLock =
                ServletContextSessionLock.peek(session, ACCESS_RULES_LOCK_KEY)
        if (!existingLock) {
            log.warn "RepoController.saveAuthorization was called without an existing lock."
            flash.warn = message(code: 'repository.action.saveAuthorization.not.locked')
            redirect(action: 'showAuthorization')
            return
        }
        ServletContextSessionLock lock =
                ServletContextSessionLock.obtain(session, ACCESS_RULES_LOCK_KEY)
        if (!lock) {
            log.warn "RepoController.saveAuthorization was called without an owning the lock."
            flash.warn = message(code: 'repository.action.saveAuthorization.not.locked')
            redirect(action: 'showAuthorization')
            return
        }

        def result = serverConfService.validateSvnAccessFile(
                cmd.fileContent)
        def exitStatus = Integer.parseInt(result[0])

        if (exitStatus != 0) {
            def err = result[2].split(": ")
            if (err.length == 3) {
                def line = err[1].split(":")
                flash.error = message(code:
                'repository.action.saveAuthorization.validate.failure.lineno',
                        args: [line[line.length - 1], err[2]])
            } else {
                flash.error = message(code:
                'repository.action.saveAuthorization.validate.failure.nolineno',
                        args: [err[err.length - 1]])
            }

            flash.message = null
        } else {
            if (!cmd.hasErrors() && serverConfService.writeSvnAccessFile(
                    cmd.fileContent)) {
                lock.release(session)
                flash.message = message(
                        code: 'repository.action.saveAuthorization.success')
                flash.error = null
                redirect(action: 'showAuthorization')
                return
            } else {
                flash.error = message(
                        code: 'repository.action.saveAuthorization.failure')
                flash.message = null
            }
        }

        render(view: 'editAuthorization', model: [authRulesCommand: cmd])
    }

    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_HOOKS'])
    def hooksList = {
        def model = reports()
        def repo = Repository.get(params.id)
        if (repo) {
            // set default sort to be alphabetical, if neither sort
            // parameter is present
            ControllerUtil.setDefaultSort(params, "name")
            ControllerUtil.decorateFileClass()
            def sortBy = params.sort
            boolean isAscending = params.order == "asc"
            model["hooksList"] =
                svnRepoService.listHooks(repo, sortBy, isAscending)
        } else {
            flash.error = message(code: 'repository.action.not.found',
                                  args: [params.id])
            redirect(action: 'list')
        }
        return model
    }
    
    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_HOOKS'])
    def copyHook = {
        fileAction('hooksList') { repo, filename ->
            def copyName = params._confirmDialogText_copyHook
            if (copyName) {
                try {
                    svnRepoService.copyHookFile(repo, filename, copyName)
                    flash.message = message(
                            code: 'repository.action.copyHook.success',
                            args: [filename, copyName])
                    
                } catch (ValidationException e) {
                    flash.error = message(code: e.message,
                            args: [filename, copyName])
                }            
            } else {
                flash.error = message(code: 'repository.action.filename.required')
            }

            redirect(action: 'hooksList', id: params.id)
        }
    }

    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_HOOKS'])
    def renameHook = {
        fileAction('hooksList') { repo, filename ->
            def targetName = params._confirmDialogText_renameHook
            if (targetName) {
                try {
                    svnRepoService.renameHookFile(repo, filename, targetName)
                    flash.message = message(
                            code: 'repository.action.renameHook.success',
                            args: [filename, targetName])
                    
                } catch (ValidationException e) {
                    flash.error = message(code: e.message,
                            args: [filename, targetName])
                }
            } else {
                flash.error = message(code: 'repository.action.filename.required')
            }
            redirect(action: 'hooksList', id: params.id)
        }
    }

    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_HOOKS'])
    def deleteHook = {
        fileAction('hooksList') { repo, filename ->
            svnRepoService.deleteHookFile(repo, filename)
            flash.message = message(
                    code: 'repository.action.deleteHook.success',
                    args: [filename])
                    
            redirect(action: 'hooksList', id: params.id)
        }
    }

    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_HOOKS'])
    def downloadHook = {
        fileAction('hooksList') { repo, filename ->
            // Not sure how sophisticated we need to be here, so just
            // treating any non-ascii file as binary
            File hookFile = svnRepoService.getHookFile(repo, filename)
            def contentType = fileUtil.isAsciiText(hookFile) ? "text/plain" : 
                    "application/octet-stream"
            response.setContentType(contentType)
            response.setHeader("Content-disposition",
                    'attachment;filename="' + filename + '"')
            if (!svnRepoService.streamHookFile(repo, filename, response.outputStream)) {
                throw new FileNotFoundException()
            }
        }
        return null
    }

    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_HOOKS'])
    def editHook = {
        fileAction('hooksList') { repo, filename ->

            def model = reports() 
            def identifier = "repo:${repo.id};hookName:${filename}"
            User owner
            ServletContextSessionLock lock =
                ServletContextSessionLock.obtain(session, identifier)
            if (lock) {
                // if lock is obtained, present the editor
                lock.userId = loggedInUserInfo(field:'id') as int
                log.info("session lock obtained for file '${identifier}' granted to user '${lock.userId}'")
                def file = svnRepoService.getHookFile(repo, filename)
                model << [fileName: file.name, fileId: identifier, fileContent: file.text?.denormalize(), lockToken: identifier]
                return model
            }
            else {
                // else, redirect with warning to the list view
                lock = ServletContextSessionLock.peek(session, identifier)
                owner = User.get(lock.userId)
                log.info("session lock denied for file '${identifier}'; already granted to user '${lock.userId}'")
                flash.error = message (code: "default.fileEditor.isLocked", args: [owner?.realUserName, lock.createdOn])
                redirect(action: 'hooksList', id: repo.id)
            }
        }
    }

    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_HOOKS'])
    def saveHook  = {
        // use the file id to extract current repo context and script name
        def matcher = params.fileId =~ /repo:(\d+);hookName:(.*)/
        def repoId = matcher[0][1]
        def hookName = matcher[0][2]

        // verify and release the file lock, and save the file
        def lock = ServletContextSessionLock.obtain(session, params.fileId)
        if (lock) {
            log.info("saving edits to file '${hookName}' in repo '${repoId}'")
            def file = svnRepoService.getHookFile(Repository.get(repoId), hookName)
            file.text = params.fileContent.toString().denormalize()
            file.setExecutable(true)
            lock.release(session)
            log.info("session lock released for file '${hookName}' from user '${lock.userId}'")
            flash.message = message (code: "repository.page.hookEdit.saved", args: [hookName])
        }
        else {
            // redirect with warning to the list view if lock not owned by current user for some reason
            lock = ServletContextSessionLock.peek(session, params.fileId)
            owner = User.get(lock.userId)
            log.info("edit denied for file '${params.fileId}'; lock granted to user '${lock.userId}'")
            flash.error = message (code: "default.fileEditor.isLocked", args: [owner.realUserName, lock.createdOn])
            redirect(action: 'hooksList', id: repoId)
        }
        redirect(action: 'hooksList', id: repoId)
    }

    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_HOOKS'])
    def cancelHookEdit  = {
        // release the file lock
        def lock = ServletContextSessionLock.obtain(session, params.fileId)
        if (lock) {
            log.info("session lock released for file '${params.fileId}' from user '${lock.userId}'")
            lock.release(session)
        }
        // use the file id to extract current repo context and hookm name
        def matcher = params.fileId =~ /repo:(\d+);hookName:(.*)/
        def repoId = matcher[0][1]
        def hookName = matcher[0][2]
        log.debug("File id indicates repo: '${matcher[0][1]}'")
        if (repoId) {
            flash.message = message(code: "repository.page.hookEdit.editCanceled", args: [hookName] )
            redirect(action: 'hooksList', id: repoId)
        }
        else {
            redirect(action: index)
        }
    }

    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_HOOKS'])
    def createHook = {
        def model = reports()
        model
    }

    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_HOOKS'])
    def uploadHook = { 
        def uploadedFile = request.getFile('fileUpload')
        if (!uploadedFile || uploadedFile.empty) {
            flash.error = message(code:'repository.page.hookCreate.upload.no.file')
            redirect(action: 'createHook', id: params.id)
        }   
        else {
            def repo = Repository.get(params.id)
            def tempFile = File.createTempFile("hookUpload", "tmp")
            uploadedFile.transferTo(tempFile)
            if (svnRepoService.createHook(repo, tempFile, uploadedFile.originalFilename)) {
                flash.message = message(code: "repository.page.hookCreate.upload.success",
                        args: [uploadedFile.originalFilename, repo.name] )
                redirect(action: 'hooksList', id: params.id)
            }
            else {
                flash.error = message(code:'repository.page.hookCreate.upload.duplicate', args: [uploadedFile.originalFilename])
                redirect(action: 'createHook', id: params.id)
            }
        }
    }
    
    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
    def verify = {
        Repository r = selectRepository()
        def userId = loggedInUserInfo(field: 'id') as Integer
        def schedule = new SchedulerBean()
        schedule.frequency = SchedulerBean.Frequency.NOW
        svnRepoService.scheduleVerifyJob(schedule, r, userId, null, request.locale)
        flash.unfiltered_message = message(code: 'repository.action.verify.adHoc')
        MailConfiguration config = MailConfiguration.getConfiguration()
        if (!config.enabled) {
            flash.unfiltered_message += " " + message(code: 'mailConfiguration.required')
        }
        redirect([action: 'list'])
    }

    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
    def replicaSyncRepo = {
        def ids = selectRepositoryIds()
        replicationService.synchronizeRepositories(ids, request.locale)
        flash.unfiltered_message = message(code: 'repository.action.replicaSyncRepos.success')
        redirect([action: 'list'])
    }
    
    @Secured(['ROLE_ADMIN', 'ROLE_ADMIN_REPO'])
    def replicaSyncRevprops = {
        Repository repo = selectRepository()
        if (repo) {
            def revision = null 
            if (params.revisionType == 'specified') {
                revision = params.revision
                if (!revision) {
                    flash.error = message(code: 'repository.action.replicaSyncRevprops.noRevision')
                    redirect([action: 'list'])
                    return
                }
            }
            replicationService.synchronizeRevprops(repo, revision, request.locale)
            flash.unfiltered_message = revision ? 
                    message(code: 'repository.action.replicaSyncRevprops.success.revision',
                            args: [revision]) :
                    message(code: 'repository.action.replicaSyncRevprops.success')
        } 
        redirect([action: 'list'])
    }

    private def selectRepositoryIds() {
        def id = params.id
        if (id) {
            return [id]
        }

        def ids = ControllerUtil.getListViewSelectedIds(params)
        if (!ids) {
            flash.error = message(code: 'repository.action.not.found',
                        args: ['null'])
            redirect(action: list)
        }
        return ids
    }

    /**
     * helper to format a SchedulerBean instance to a human-readable string
     * @param s
     * @return string such as "Weekly on Sunday at 11:15"
     */
    private String formatSchedule(SchedulerBean s) {
        String output = ""
        String minutes = String.format("%02d", s.minute)
        String hour = String.format("%02d", s.hour)
        switch (s.frequency) {
            case SchedulerBean.Frequency.HOURLY:
                output = message(code: "repository.page.bkupSchedule.schedule.hourly",
                        args: [minutes])
                break
            case SchedulerBean.Frequency.DAILY:
                output = message(code: "repository.page.bkupSchedule.schedule.daily",
                        args: [hour, minutes])
                break
            case SchedulerBean.Frequency.WEEKLY:
                def daysOfWeek = ["sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday"]
                int dayOfWeekIndex = s.dayOfWeek - 1
                String scheduleDayOfWeek = message(code: "default.dayOfWeek.${daysOfWeek[dayOfWeekIndex]}")
                output = message(code: "repository.page.bkupSchedule.schedule.weekly",
                        args: [scheduleDayOfWeek, hour, minutes])
                break
        }
        return output
    }
}

/**
 * Command class for 'saveAuthorization' action provides validation
 */
class AuthzRulesCommand {
    String fileContent
    def errors
    static constraints = {
        fileContent(blank: false)
    }
}