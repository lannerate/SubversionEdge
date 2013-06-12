package com.collabnet.svnedge.replication.command

import java.text.SimpleDateFormat;

/**
 * Helper class for common code used in testing the replication command subsystem
 */
class CommandTestsHelper {

    /**
     * creates a random command id in the form "cmdexecNNNN"
     * @return
     */
    public static String createCommandId(existingIds = null) {
        String cmdId = null
        def prefix = "cmdexec"
        while (!cmdId) {
            cmdId = prefix + Math.round(Math.random() * 8999) + 1000
            if (existingIds?.contains(cmdId)) {
                cmdId = null
            } else if (existingIds != null) {
                existingIds << cmdId
            }
        }
        return cmdId
    }
    
    /**
    * Creates a new repository in CTF
    * @return the repository name
    */
   public static def createTestRepository(config, clientService) {
       def suffix = new SimpleDateFormat("HHmmss").format(new Date()) + 
           String.valueOf(Math.round(Math.random() * 1000000))
       def projectName = "testproject-" + suffix
       def repoName = "testrepo-" + suffix
       def ctfUrl = makeCtfBaseUrl(config)
       def sessionId = clientService.login(ctfUrl, config.svnedge.ctfMaster.username,
           config.svnedge.ctfMaster.password, null)
       def projectId = clientService.createProject(ctfUrl, sessionId, projectName, 
           projectName, "Test project for commands")
       clientService.createRepository(ctfUrl, sessionId, projectId, 
           config.svnedge.ctfMaster.systemId, repoName,
           "Test repository for commands", false, false)
       clientService.logoff(ctfUrl, config.svnedge.ctfMaster.username, sessionId)
       return [repoName: repoName, 
           projectName: projectName, projectId: projectId]
   }
   
   public static void deleteTestProject(config, clientService, projectName) {
       def ctfUrl = makeCtfBaseUrl(config)
       def sessionId = clientService.login(ctfUrl, config.svnedge.ctfMaster.username,
           config.svnedge.ctfMaster.password, null)
       clientService.deleteProject(ctfUrl, sessionId, projectName)
       clientService.logoff(ctfUrl, config.svnedge.ctfMaster.username, sessionId)
   }
        
           
   public static def makeCtfBaseUrl(config) {
       boolean ssl = config.svnedge.ctfMaster.ssl
       def ctfProto = ssl ? "https://" : "http://"
       def ctfHost = config.svnedge.ctfMaster.domainName
       def port = config.svnedge.ctfMaster.port
       def ctfPort = (!ssl && port == 80) || (ssl && port == 443) ? 
           "" : ":" + port
       return ctfProto + ctfHost + ctfPort
   }




}
