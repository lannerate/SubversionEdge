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


/**
 * Gant script that creates a tar.gz package with the console app. This variant will
 * post-process the distribution to be more developer-firendly (explode war, enable debugging)
 */

includeTargets << new File("scripts", "Dist.groovy")

target(createDebuggingRelease: 'Builds and post-processes the release distribution for developer use') {

    // create the default distro image
    depends(build)

    // apply developer changes
    explodeWar()
    enableDebugging()

}

target(explodeWar: 'Expands the csvn.war file to a directory, excluding the precompiled gsps') {

    ant.echo(message: "DEBUG DIST: Expanding csvn.war file to directory of same name, excluding precompiled gsps")
    distDir = "${basedir}/dist"
    webAppsDir = "${distDir}/appserver/webapps"
    if (osName == "windows") {
        // for windows, delete "dist/appserver/webapps/csvn.war"
        // and instead unpack the artifact in the "updates" dir
        ant.delete (file: "${webAppsDir}/csvn.war")
        webAppsDir = "${distDir}/updates/appserver/webapps"
    }

    // explode the war file to a same-named directory, but exclude the precompiled gsps
    csvnWar = "${webAppsDir}/csvn.war"
    explodedCsvnWarTemp = "${webAppsDir}/csvn.war.temp"
    ant.mkdir(dir: explodedCsvnWarTemp)
    ant.unzip(src: csvnWar, dest: explodedCsvnWarTemp)

    ant.delete(file: csvnWar)
    ant.mkdir (dir: csvnWar)

    ant.move(toDir: csvnWar) {
      fileset (dir: explodedCsvnWarTemp) {
        include (name: "**/*")
        exclude (name: "**/gsp_*")
      }
    }

    ant.deltree(dir: explodedCsvnWarTemp)
}

target(enableDebugging: 'Modify wrapper scripts for debugging the target JVM') {

    ant.echo(message: "DEBUG DIST: uncommenting the debug params in the wrapper scripts")
    distDir = "${basedir}/dist"
    confDir = "${distDir}/temp-data/conf"
    wrapperConf = "${confDir}/csvn-wrapper.conf"
    if (osName == "windows") {
        confDir = "${distDir}/updates/svcwrapper/conf"
        wrapperConf = "${confDir}/wrapper.conf"
    }

    wrapperConfTemp = "${confDir}/csvn-wrapper.conf.temp"

    def confFile = new File(wrapperConf)
    def tempFile = new File(wrapperConfTemp)
    if (tempFile.exists()) {
        tempFile.delete()
    }

    confFile.eachLine {
        String output = it
        if (output.contains("-Xdebug") || output.contains("-Xrunjdwp")) {
            output = (output.startsWith("#")) ? output.substring(1) : output
        }
        tempFile << output + "\n"
    }

    confFile.delete()
    tempFile.renameTo(wrapperConf)

}

setDefaultTarget("createDebuggingRelease")
