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
import grails.util.Metadata

import com.sun.pkg.client.Image
import com.sun.pkg.client.Image.FmriState

/**
 * Gant script that creates a tar.gz package with the console app. More
 * info at http://gant.codehaus.org/Targets
 *
 * @author Marcello de Sales (mdesales@collab.net)
 *
 * @since 0.1.1
 */

includeTargets << grailsScript("_GrailsWar")
includeTargets << new File("scripts", "_CommonTargets.groovy")

target(build: 'Builds the distribution file structure') {
    depends(war)

    distDir = "${basedir}/dist"
    Ant.property(name: "distDir", value: distDir)
    prepare()
    copyResources()
    osName = Ant.project.properties.'osName'
    bits = Ant.project.properties."bits"
    arch = Ant.project.properties."arch"

    Ant.echo(message: "Building the distribution system for $osName")
    def version = metadata.getApplicationVersion()
    if(version) {
        version = '-'+version
    } else {
        version = ''
    }
    createDistributionStructure()
}

target(createDistributionStructure: 'Creates the distribution structure') {
    Ant.echo(message: "Creating a fresh distribution structure")

    Ant.delete(dir: distDir)
    Ant.mkdir(dir: distDir)

    libDir = distDir + "/lib"

    tmpDir = distDir + "/tmp"
    Ant.mkdir(dir: tmpDir )

    if (osName == "windows") {
        // On Windows, put all files in updates directory
        updatesDir = "${distDir}/updates"
        Ant.mkdir(dir: updatesDir)
        updatesLibDir = "${updatesDir}/lib"
        Ant.mkdir(dir: updatesLibDir)
        updatesBinDir = "${updatesDir}/bin"
        Ant.mkdir(dir: updatesBinDir)
        updatesWebAppsDir = updatesDir + "/appserver/webapps"
    }
    webAppsDir = distDir + "/appserver/webapps"
    Ant.mkdir(dir: webAppsDir)
    Ant.mkdir(dir: distDir + "/lib")

    downloadArtifacts()
    rearrangingArtifacts()
}

target(rearrangingArtifacts: 'Moves downloaded artifacts to dist directory') {
    Ant.echo(message: "Building the distribution system for ${osName}")

    if (osName == "linux") {
        Ant.exec(dir:"${distDir}", executable: "gunzip") {
            arg(line: archiveFile)
        }
        // remove the .gz
        archiveFile = archiveFile.substring(0, archiveFile.length() - 3)
        Ant.exec(dir: "${distDir}", executable: "tar") {
            arg(line: "-xpf")
            arg(line: archiveFile)
        }
        Ant.delete(file: archiveFile)

         //Copying the service wrapper artifacts
        Ant.copy(file: "${basedir}/csvn-service-wrapper" +
            "/linux/bin/csvn",
            todir: "${distDir}/bin")
        Ant.copy(file: "${basedir}/csvn-service-wrapper" +
            "/linux/bin/csvn-httpd",
            todir: "${distDir}/bin")
        Ant.copy(file: "${basedir}/csvn-service-wrapper" +
            "/linux/bin/wrapper-linux-x86-32",
            todir: "${distDir}/bin")
        Ant.copy(file: "${basedir}/csvn-service-wrapper" +
            "/linux/bin/wrapper-linux-x86-64",
            todir: "${distDir}/bin")
        Ant.chmod(dir: distDir + "/bin", perm: "a+x",
            includes: "csvn*")
        Ant.chmod(dir: distDir + "/bin", perm: "a+x",
            includes: "wrapper-linux-x86*")
        Ant.copy(file: "${basedir}/csvn-service-wrapper" +
            "/linux/conf/csvn-wrapper.conf",
            todir: "${distDir}/data/conf")
        Ant.copy(file: "${basedir}/csvn-service-wrapper" +
            "/linux/conf/csvn.conf.dist",
            todir: "${distDir}/data/conf")
        Ant.copy(file: "${basedir}/csvn-service-wrapper" +
            "/linux/lib/wrapper.jar",
            todir: "${distDir}/lib")
        Ant.copy(file: "${basedir}/csvn-service-wrapper" +
            "/linux/lib/libwrapper-linux-x86-32.so",
            todir: "${distDir}/lib")
       Ant.copy(file: "${basedir}/csvn-service-wrapper" +
            "/linux/lib/libwrapper-linux-x86-64.so",
            todir: "${distDir}/lib")

        // Copy the SIGAR libraries to lib folder which is on java.library.path
       Ant.copy(file: "${basedir}/ext" +
            "/sigar/libsigar-amd64-linux.so",
            todir: "${distDir}/lib")
       Ant.copy(file: "${basedir}/ext" +
            "/sigar/libsigar-x86-linux.so",
            todir: "${distDir}/lib")

    } else if (osName == "solaris") {

        Ant.exec(dir:"${distDir}", executable: "gunzip") {
            arg(line: archiveFile)
        }
        // remove the .gz
        archiveFile = archiveFile.substring(0, archiveFile.length() - 3)
        Ant.exec(dir: "${distDir}", executable: "tar") {
            arg(line: "-xpf")
            arg(line: archiveFile)
        }
        Ant.delete(file: archiveFile)

         //Copying the service wrapper artifacts
        Ant.copy(file: "${basedir}/csvn-service-wrapper" +
            "/solaris/bin/csvn",
            todir: "${distDir}/bin")
        Ant.copy(file: "${basedir}/csvn-service-wrapper" +
            "/solaris/bin/csvn-httpd",
            todir: "${distDir}/bin")
        Ant.copy(file: "${basedir}/csvn-service-wrapper" +
            "/solaris/bin/wrapper-solaris-x86-64",
            todir: "${distDir}/bin")
        Ant.copy(file: "${basedir}/csvn-service-wrapper" +
            "/solaris/bin/wrapper-solaris-sparc-64",
            todir: "${distDir}/bin")
        Ant.copy(file: "${basedir}/csvn-service-wrapper" +
            "/solaris/bin/wrapper-solaris-x86-32",
            todir: "${distDir}/bin")
        Ant.copy(file: "${basedir}/csvn-service-wrapper" +
            "/solaris/bin/wrapper-solaris-sparc-32",
            todir: "${distDir}/bin")
        Ant.chmod(dir: distDir + "/bin", perm: "a+x",
            includes: "csvn*")
        Ant.chmod(dir: distDir + "/bin", perm: "a+x",
            includes: "wrapper-solaris*")
        Ant.copy(file: "${basedir}/csvn-service-wrapper" +
            "/solaris/conf/csvn-wrapper.conf",
            todir: "${distDir}/data/conf")
        Ant.copy(file: "${basedir}/csvn-service-wrapper" +
            "/solaris/conf/csvn.conf.dist",
            todir: "${distDir}/data/conf")
        Ant.copy(file: "${basedir}/csvn-service-wrapper" +
            "/solaris/lib/wrapper.jar",
            todir: "${distDir}/lib")
        Ant.copy(file: "${basedir}/csvn-service-wrapper" +
            "/solaris/lib/libwrapper-solaris-x86-32.so",
            todir: "${distDir}/lib")
        Ant.copy(file: "${basedir}/csvn-service-wrapper" +
            "/solaris/lib/libwrapper-solaris-sparc-32.so",
            todir: "${distDir}/lib")
        Ant.copy(file: "${basedir}/csvn-service-wrapper" +
            "/solaris/lib/libwrapper-solaris-x86-64.so",
            todir: "${distDir}/lib")
        Ant.copy(file: "${basedir}/csvn-service-wrapper" +
            "/solaris/lib/libwrapper-solaris-sparc-64.so",
            todir: "${distDir}/lib")
        Ant.copy(file: "${basedir}/csvn-service-wrapper" +
            "/solaris/lib/libwrapper-solaris-x86-32.so",
            todir: "${distDir}/lib")
        Ant.copy(file: "${basedir}/csvn-service-wrapper" +
            "/solaris/lib/libwrapper-solaris-sparc-32.so",
            todir: "${distDir}/lib")

        Ant.copy(file: "${basedir}/ext" +
            "/sigar/libsigar-sparc-solaris.so",
            todir: "${distDir}/lib")
        Ant.copy(file: "${basedir}/ext" +
            "/sigar/libsigar-sparc64-solaris.so",
            todir: "${distDir}/lib")
        Ant.copy(file: "${basedir}/ext" +
            "/sigar/libsigar-x86-solaris.so",
            todir: "${distDir}/lib")
        Ant.copy(file: "${basedir}/ext" +
            "/sigar/libsigar-amd64-solaris.so",
            todir: "${distDir}/lib")

    } else
    if (osName == "windows") {
        // On Windows, put all files in updates directory
        Ant.unzip(src: archiveFile, dest: updatesDir)
        Ant.delete(file: archiveFile)

        //copying the service wrapper artifacts
        Ant.copy(todir: "${updatesBinDir}") {
            fileset(dir: "${basedir}/csvn-service-wrapper" +
                "/windows-x86-32/bin",
                includes:"**/*")
        }
        Ant.copy(todir: "${updatesLibDir}") {
            fileset(dir: "${basedir}/csvn-service-wrapper" +
                "/windows-x86-32/lib",
                includes:"**/*")
        }
        Ant.copy(todir: "${updatesDir}/data/conf") {
            fileset(dir: "${basedir}/csvn-service-wrapper" +
                "/windows-x86-32/conf",
                includes:"**/*")
        }
        Ant.copy(todir: "${updatesDir}/svcwrapper") {
                fileset(dir: "${basedir}/svcwrapper",
                includes:"**/*")
        }
        // Copy SIGAR library to bin folder which is on PATH
        Ant.copy(todir: "${updatesBinDir}") {
            fileset(dir: "${basedir}/ext/sigar/",
                includes:"**/sigar-*-winnt.dll")
        }

    } else
    if (osName == "mac") {

    }

    //copying all the version-controlled artifacts (data, config, statics, etc)
    if (osName == "windows") {
        //move the console war file to the library dir
        Ant.copy(todir: updatesDir) {
            fileset(dir: "${basedir}/svn-server",
            includes:"**/*")
        }
        // copy the version string to a file for use in the View VC "About" dialog
        Ant.echo(message: "Release: ${metadata.getApplicationVersion()}", file: updatesDir + "/www/viewvc/app_version.txt")
        Ant.move(file: warName,
                 tofile: "${updatesWebAppsDir}/csvn.war")
            //move the data directory as temp-data (artf62798) for packaging
            //The bootstrap process must move this directory back to data
        Ant.move(file: updatesDir + "/data", tofile:
                distDir + "/temp-data" )

        Ant.move(file: "${webAppsDir}/integration.war",
                 tofile: "${updatesWebAppsDir}/integration.war")

        Ant.move(file: "${distDir}/lib/integration-scripts.zip",
                 tofile: "${updatesLibDir}/integration-scripts.zip")

    } else {
        //move the console war file to the library dir
        Ant.copy(todir: distDir) {
            fileset(dir: "${basedir}/svn-server",
                    includes:"**/*")
        }
        // copy the version string to a file for use in the View VC "About" dialog
        Ant.echo(message: "Release: ${metadata.getApplicationVersion()}", file: distDir + "/www/viewvc/app_version.txt")
        Ant.move(file: warName, tofile: "${webAppsDir}/" +
                "csvn.war")
        Ant.chmod(dir: distDir + "/bin/cgi-bin",
            perm: "a+x", includes: "*.cgi")
        //move the data directory as temp-data (artf62798) for packaging
        //The bootstrap process must move this directory back to data
        Ant.move(file: distDir + "/data", tofile:
            distDir + "/temp-data" )

        Ant.chmod(file: distDir + "/bin/collabnetsvn-config", perm: "+x")
        Ant.chmod(file: distDir + "/bin/svndbadmin", perm: "+x")

        if (osName == "solaris") {
           Ant.copy(file: "${basedir}/ext/ocn-files/readme-solaris.txt",
               tofile: "${distDir}/README")
        } else {
           Ant.copy(file: "${basedir}/ext/ocn-files/readme-linux.txt",
               tofile: "${distDir}/README")
        }
    }
    // Make logs directory.  App needs it to start
    Ant.mkdir(dir: "${distDir}/temp-data/logs")
    Ant.delete(dir: tmpDir)

    // create and populate the dist directory which contains our base 
    // configuration files these will be installed on users system for 
    // backup and reference purposes
    distdataDir = "${distDir}/dist"
    Ant.mkdir(dir: distdataDir)
    Ant.copy(file: "${distDir}/temp-data/conf/httpd.conf.dist",
         todir: "${distdataDir}")
    Ant.copy(file: "${distDir}/temp-data/conf/viewvc.conf.dist",
         todir: "${distdataDir}")
    Ant.copy(file: "${distDir}/temp-data/conf/teamforge.properties.dist",
         todir: "${distdataDir}")
    Ant.copy(file: "${distDir}/temp-data/conf/csvn-wrapper.conf",
         todir: "${distdataDir}")
    if (osName == "linux" || osName == "solaris") {
	    Ant.copy(file: "${distDir}/temp-data/conf/csvn.conf.dist",
	         todir: "${distdataDir}")
    }
    Ant.delete(file: "${distDir}/temp-data/conf/httpd.conf")
    if (osName == "windows") {
        Ant.copy(file: "${updatesDir}/appserver/etc/jetty.xml",
                todir: "${distdataDir}")
    } else {
        Ant.copy(file: "${distDir}/appserver/etc/jetty.xml", 
                todir: "${distdataDir}")
    }
    // Move the Windows install-updates files to the dist directory
    if (osName == "windows") {
        Ant.move(file: "${updatesBinDir}/install-updates.bat",
                toDir: distdataDir)
        Ant.move(file: "${updatesBinDir}/wait.bat",
                toDir: distdataDir)
        // Copy everything from updates folder to dist folder so local 
        // testing can be done
        Ant.copy(todir: distDir) {
            fileset(dir: updatesDir, includes:"**/*")
        }
    }

    Ant.echo(message: "Deleting remaining python compiled artifacts from " +
        "${distDir}")
    def deleteSuffixedArtifacts
    // Define closure
    deleteSuffixedArtifacts = { 
        it.eachDir(deleteSuffixedArtifacts)
        it.eachFile {
            if (it.name.contains(".pyc")) {
                Ant.echo(message: "Deleting ${it.canonicalPath}")
                it.delete()
            }
        }
    }
    deleteSuffixedArtifacts( new File("${distDir}") )

    event("StatusFinal", ["Distribution directory created successfully: " +
                  "${distDir}"])
}

setDefaultTarget("build")
