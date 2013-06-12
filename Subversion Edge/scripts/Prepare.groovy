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

includeTargets << new File("scripts", "_CommonTargets.groovy")

/**
 * Gant script that prepares the development environment for development, 
 * getting the latest version of the csvn-binaries from the BPL
 *
 * @author Marcello de Sales (mdesales@collab.net)
 *
 * @since 0.1.1
 */
target(build: 'Builds the distribution file structure') {

    distDir = "${basedir}/svn-server"
    Ant.property(name: "distDir", value: distDir)
    prepare()
    copyResources()
    osName = Ant.project.properties.'osName'

    Ant.echo(message: "Preparing CSVN binaries / development environment for " +
            "$osName.")
    downloadArtifacts()
    unpackArtifacts()
}

target(unpackArtifacts: 'Moves downloaded artifacts to dist directory') {
    Ant.echo(message: "Organizing the CSVN binary directories for development")
    if (osName == "linux" || osName == "solaris") {
        Ant.exec(dir:"${distDir}", executable: "gunzip") {
            arg(line: archiveFile)
        }
        // remove the .gz
        archiveFile = archiveFile.substring(0, archiveFile.length() - 3)
        Ant.exec(dir:"${distDir}", executable: "tar") {
            arg(line: "-xpf")
            arg(line: archiveFile)
        }

    } else if (osName == "windows") {
        Ant.unzip(src: archiveFile, dest: distDir)
    } else
    if (osName == "mac") {

    }
    Ant.delete(file: archiveFile)
    Ant.echo(message: "CSVN development binary: ${distDir}")
}

setDefaultTarget("build")
