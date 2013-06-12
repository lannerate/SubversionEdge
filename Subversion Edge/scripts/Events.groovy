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
import org.mortbay.jetty.webapp.WebAppContext
import org.codehaus.groovy.grails.commons.ConfigurationHolder

// This will fire when starting jetty via run-app
eventConfigureJetty = { server ->
    println "eventConfigureJetty"
    def appHome = ConfigurationHolder.config.svnedge.appHome
    def ctfWebapp = new File(appHome,
        "appserver/webapps/integration.war")
    if (ctfWebapp.exists() && ctfWebapp.canRead()) {
        println "Deploying the CTF integration webapp"
        WebAppContext resourceContext = 
            new WebAppContext(ctfWebapp.absolutePath, "/integration")
        server.addHandler(resourceContext)
        println "Finished deploying the CTF integration webapp at context " +
                 "'/integration'"
    }
}

eventCleanEnd = {

    Ant.echo(message: "Deleting the CSVN development artifacts.")
    Ant.delete(file: "${basedir}/stacktrace.log")

    Ant.echo(message: "Deleting from all the components")

    csvnbinDir = "${basedir}/svn-server"

    dataDir = csvnbinDir + "/data"
    confDir = dataDir + "/conf"
    repoDir = dataDir + "/repositories"
    libDir = csvnbinDir + "/lib"

    Ant.echo(message: "Deleting the configuration artifacts")
    Ant.delete(file: "${confDir}/httpd.conf")
    Ant.delete(file: "${confDir}/viewvc_httpd.conf")
    Ant.delete(file: "${confDir}/svn_httpd.conf")

    Ant.echo(message: "Deleting the conversion artifacts")
    Ant.delete(dir: "${dataDir}/teamforgeHome")
    Ant.delete(dir: "${dataDir}/teamforge")
    Ant.delete(file: "${confDir}/teamforge.properties")

    Ant.echo(message: "Deleting created repositories")
    Ant.delete(includeEmptyDirs: "true") {
        fileset(dir: "${dataDir}/repositories",
                includes: "**/*")
    }

    Ant.echo(message: "Deleting logs")
    Ant.delete(includeEmptyDirs: "true", quiet: "true") {
        fileset(dir: "${dataDir}/logs",
                includes: "**/*")
    }
	Ant.mkdir(dir: "${dataDir}/logs")

    Ant.echo(message: "Deleting the teamforge vcauth files")
    Ant.delete(includeEmptyDirs: "true", quiet: "true") {
        fileset(dir: "${libDir}/viewvc/vcauth/teamforge", includes: "**/*")
    }
    Ant.delete(dir: "${libDir}/viewvc/vcauth" +
            "/teamforge", quiet: "true")

    Ant.echo(message: "Deleting the deleted repositories")
    Ant.delete(includeEmptyDirs: "true", quiet: "true") {
        fileset(dir: "${dataDir}/deleted-repos",
                includes: "**/*")
    }
    Ant.delete(dir: "${dataDir}/deleted-repos", quiet: "true")

    Ant.echo(message: "Deleting the dev database")
    Ant.delete(quiet: "true") {
        fileset(dir: "${dataDir}", includes: "csvn-dev-hsqldb.*")
    }

    event("StatusFinal", ["CSVN Development environment cleaned!"])
}

eventTestSuiteEnd = { String type ->
    if (type == "integration") {
/*  TODO CTF REPLICA
        def ctfProxyServerService = appCtx?.getBean("ctfProxyServerService")
        if (ctfProxyServerService) {
            println "Stopping CEE proxy Server"
            ctfProxyServerService.stopServer()
        }
*/
    }
}
