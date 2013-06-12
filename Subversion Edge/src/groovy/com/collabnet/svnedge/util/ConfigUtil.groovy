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
package com.collabnet.svnedge.util

import java.io.File
import org.apache.log4j.Logger
import org.codehaus.groovy.grails.commons.ConfigurationHolder

/**
 * The configuration utility class is used during the bootstrap and services
 * that needs the values from the configuration as shortcuts. It can be used
 * via static methods or as spring bean 'configUtil' (where test mocking is
 * needed)
 * 
 * @author Marcello de Sales (mdesales@collab.net)
 *
 */
public final class ConfigUtil {

    def static configuration
    static String confDirPath

    private static def getConfig() {
        if (!configuration) {
            configuration = ConfigurationHolder.config
        }
        return configuration
    }
        
    def static appHome() {
        return new File(getConfig().svnedge.appHome).absolutePath
    }

    def static httpdPath() {
        return getConfig().svnedge.svn.httpdPath ? getConfig().svnedge.svn.httpdPath :
            new File(appHome(), "bin/httpd").absolutePath
    }

    def static httpdBindPath() {
        return new File(appHome(), "/lib/httpd_bind").absolutePath
    }

    def static exeHttpdBindPath() {
        return new File(httpdBindPath(), "httpd_bind").absolutePath
    }

    def static libHttpdBindPath() {
        return new File(httpdBindPath(), "libhttpd_bind.so.1").absolutePath
    }

    def static htpasswdPath() {
        return getConfig().svnedge.svn.htpasswdPath ?
            getConfig().svnedge.svn.htpasswdPath : 
            new File(appHome(), "bin/htpasswd").absolutePath
    }

    def static opensslPath() {
        return getConfig().svnedge.opensslPath ?
            getConfig().svnedge.opensslPath :
            new File(appHome(), "bin/openssl").absolutePath
    }

    def static svnPath() {
        return getConfig().svnedge.svn.svnPath ? getConfig().svnedge.svn.svnPath :
            new File(appHome(), "bin/svn").absolutePath
    }

    def static svnConfigDirPath() {
        return new File(appHome(), "data/svn_client_config").absolutePath
    }

    def static svnadminPath() {
        return getConfig().svnedge.svn.svnadminPath ? 
            getConfig().svnedge.svn.svnadminPath : 
            new File(appHome(), "bin/svnadmin").absolutePath
    }

    def static svnauthzPath() {
        return getConfig().svnedge.svn.svnauthzPath ? 
            getConfig().svnedge.svn.svnauthzPath : 
            new File(appHome(), "bin/svnauthz").absolutePath
    }

    def static svnrdumpPath() {
        return getConfig().svnedge.replica.svn.svnrdumpPath ?:
                new File(appHome(), "bin/svnrdump").absolutePath
    }

    def static svnsyncPath() {
        return getConfig().svnedge.replica.svn.svnsyncPath ?
            getConfig().svnedge.replica.svn.svnsyncPath : 
            new File(appHome(), "bin/svnsync").absolutePath
    }

    def static dataDirPath() {
        return new File(appHome(), "data").absolutePath
    }

    def static logsDirPath() {
        return getConfig().svnedge.logsDirPath ?:
            new File(dataDirPath(), "logs").absolutePath
    }

    def static binDirPath() {
        return getConfig().svnedge.binDirPath ?:
            new File(appHome(), "bin").absolutePath
    }

    def static dumpDirPath() {
        return getConfig().svnedge.dumpDirPath ?:
            new File(dataDirPath(), "dumps").absolutePath
    }

    def static viewvcTemplateDir() {
        return getConfig().svnedge.svn.viewvcTemplatesPath ? 
            getConfig().svnedge.svn.viewvcTemplatesPath : 
            new File(appHome(), "www/viewvc").absolutePath
    }

    def static viewvcTemplateDirIntegrated() {
        return getConfig().svnedge.svn.viewvcTemplatesIntegratedPath ?:
            new File(appHome(), "www/viewvc-integrated").absolutePath
    }

    def static viewvcTemplatesDirPath() {
        return getConfig().svnedge.svn.viewvcTemplatesPath ?
            getConfig().svnedge.svn.viewvcTemplatesPath : 
            new File(appHome(), "www/viewvc").absolutePath
    }

    def static confDirPath() {
        if (!confDirPath) {
            Logger.getLogger("com.collabnet.svnedge.console")
                .debug("confDirPath not cached yet.  Config value is: " + 
                       getConfig().svnedge.svn.confDirPath)
            confDirPath = getConfig().svnedge.svn.confDirPath ?:
                new File(appHome(), "etc/conf").absolutePath
        }
        return confDirPath 
    }

    def static distDir() {
        return getConfig().svnedge.svn.distDirPath ?:
            new File(appHome(), "dist").absolutePath
    }

    def static serviceName() {
        return getConfig().svnedge.osName == "Win" ? 
            getConfig().svnedge.svn.serviceName : null
    }

    def static viewvcLibPath() {
        return getConfig().svnedge.svn.viewvcLibPath ?
            getConfig().svnedge.svn.viewvcLibPath :
            new File(appHome(), "lib/viewvc").absolutePath
    }

    static File httpdPidFile() {
        File runFile = new File(dataDirPath(), "run")
        return new File(runFile, "httpd.pid")
    }

    def static replicaSvnMasterVersion() {
        return getConfig().svnedge.replica.masterSvnVersion
    }
}
