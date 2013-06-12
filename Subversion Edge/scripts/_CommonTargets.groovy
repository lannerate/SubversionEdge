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

target(prepare: 'Prepares properties and fields') {

    Ant.condition(property:"windowsPrepare") {
        and() {
            os(family:"windows")
        }
    }
    Ant.condition(property:"solarisPrepare") {
        and() {
            os(family:"unix", name:"sunos")
        }
    }
    Ant.condition(property:"linuxPrepare") {
        and() {
            os(family:"unix")
            not() { isset(property:"solarisPrepare") }
        }
    }
    Ant.condition(property:"macPrepare") {
        and() {
            os(family:"mac")
        }
    }
    Ant.condition(property:"x64") {
        or() {
            os(arch: "x86_64")
            os(arch: "amd64")
        }
    }
    Ant.condition(property:"sparc") {
        or() {
            os(arch: "sparc")
        }
    }
    Ant.condition(property:"amd64") {
        and() {
            os(arch: "amd64")
        }
    }
    
    if (Ant.project.properties."macPrepare") {
        osName = "mac";
    }
    if (Ant.project.properties."windowsPrepare") {
        osName = "windows"
    }
    if (Ant.project.properties."linuxPrepare") {
        osName = "linux"
    }
    if (Ant.project.properties."solarisPrepare") {
        osName = "solaris"
    }
    Ant.property(name: "osName", value: osName)

    if (Ant.project.properties."amd64") {
        arch = "amd64"

    } else if (Ant.project.properties."sparc") {
        arch = "sparc"

    } else {
        arch = "x86"
    }
    Ant.property(name: "arch", value: arch)

    Ant.property(name: "bits", value: Ant.project.properties."x64" ? "64":"32")

    distDir = Ant.project.properties.'distDir'

    urlPrefix = "http://pkg.collab.net/build/"

    archiveFile = "${distDir}/svn-apache-viewvc-binaries" + 
        ((osName == "windows") ? ".zip" : ".tar.gz")

    webAppsDir = distDir + "/appserver/webapps"
    
}

target(copyResources: 'Copies css and js for use by viewvc and launch.html') {
    Ant.property(file: "${basedir}/application.properties")
    def antProp = Ant.project.properties    
    def resourcesDir = "${basedir}/web-app"
    def viewvcResourcesDir = "${basedir}/svn-server/www/viewvc/docroot"
    
    Ant.copy(file: "${resourcesDir}/js/bootstrap-${antProp.'vendor.twitter-bootstrap.version'}.js", 
            todir: "${viewvcResourcesDir}/js")
    Ant.copy(file: "${resourcesDir}/js/jquery-${antProp.'vendor.jquery.version'}.min.js", 
            todir: "${viewvcResourcesDir}/js")
    Ant.copy(file: "${resourcesDir}/js/jquery.dataTables.min.js",
        todir: "${viewvcResourcesDir}/js")
    Ant.copy(file: "${resourcesDir}/css/bootstrap-${antProp.'vendor.twitter-bootstrap.version'}.css", 
            todir: "${viewvcResourcesDir}/css")
    Ant.copy(file: "${resourcesDir}/css/bootstrap-responsive-${antProp.'vendor.twitter-bootstrap.version'}.css",  
            todir: "${viewvcResourcesDir}/css")
    Ant.copy(file: "${resourcesDir}/css/svnedge-${antProp.'app.svnedgeCss.version'}.css", 
            todir: "${viewvcResourcesDir}/css")
    Ant.copy(file: "${resourcesDir}/img/glyphicons-halflings.png", todir: "${viewvcResourcesDir}/img")
    Ant.copy(file: "${resourcesDir}/img/glyphicons-halflings-white.png", todir: "${viewvcResourcesDir}/img")
    // Version numbers are kept to ensure clients use up-to-date version, but it is easy to overlook launch.html
    // so putting the needed files under a more static name as well.
    Ant.copy(file: "${resourcesDir}/css/bootstrap-${antProp.'vendor.twitter-bootstrap.version'}.css", 
            tofile: "${viewvcResourcesDir}/css/bootstrap-current.css")
    Ant.copy(file: "${resourcesDir}/css/svnedge-${antProp.'app.svnedgeCss.version'}.css", 
            tofile: "${viewvcResourcesDir}/css/svnedge-current.css")
}

target(downloadArtifacts: 'Downloads the csvn binaries') {

    def buildNos = [linux:   ['32': 'latest', '64': 'latest'],
                    solaris: ['x86': ['32': 'latest'], 
                              'SPARC': ['32': 'latest']],
                    windows: ['32': 'latest', '64': 'latest']
        ]

    Ant.echo(message: "Downloading the CSVN binaries")

    //Generating the truststore files for downloading
    //echo | openssl s_client -connect mgr.cubit.sp.collab.net:443 | openssl 
    //  x509 -inform PEM -outform DER -trustout -out outfile.crt
    //keytool -import -storepass together -file outfile.crt -keystore 
    //   trust.keystore -alias mgrcubitsp
    //keytool -keystore trust.keystore -list >> enter password "together"
    def trustStore = "${basedir}/scripts/" +
            "cubit.keystore"
    Ant.echo(message: "Truststore File: " + trustStore)
    System.setProperty( 'javax.net.ssl.trustStore', trustStore )
    System.setProperty( 'javax.net.ssl.keyStorePassword', "together" )

    def bits = Ant.project.properties."bits"
    def build = buildNos[osName][bits]

    //Downloading from the Cubit Project Build Library... "guest" access...
    if (osName == "linux") {
        Ant.get(dest: archiveFile,
                src: urlPrefix + "linux/" +
                "CollabNet_Subversion-Linux-x86_${bits}-${build}.tar.gz")

    } else if (osName == "solaris") {
        def proc = System.getProperty("os.arch").startsWith("sparc") ?
	    "SPARC" : "x86"
        build = buildNos[osName][proc][bits]
        Ant.get(dest: archiveFile, 
                src: urlPrefix + "solaris/" +
                "CollabNet_Subversion-Sol10-${proc}_${bits}-${build}.tar.gz")


    } else if (osName == "windows") {
        Ant.get(dest: archiveFile,
            src: urlPrefix + "windows/CollabNet_Subversion-Win${bits}-${build}.zip")
    } else
    if (osName == "mac") {
        System.err.println("Feature not implemented for Mac")
        System.exit(1)
    }

    Ant.get(dest: "${webAppsDir}/integration.war",
            src: urlPrefix + "CTF/integration-latest.war")
    Ant.get(dest: "${distDir}/lib/integration-scripts.zip",
            src: urlPrefix + "CTF/integration-scripts-latest.zip")
}
