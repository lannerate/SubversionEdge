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

includeTargets << grailsScript("Init")

target(main: "Produces the REST API documentation from the sources") {
    ant.taskdef(name:"groovydoc", classname:"org.codehaus.groovy.ant.Groovydoc")
    baseDir = "."
    targetDir = "${basedir}/target/docs/restapi"
    
    event("DocStart", ['groovydoc'])
    try {
        ant.groovydoc(destdir: targetDir, 
                sourcepath: "${baseDir}/grails-app/controllers",
                packagenames: "com.collabnet.svnedge.controller.api.*",
                use: "true",
                windowtitle: "Subversion Edge REST API",
                private: "false",
                overview: "true") {
                        link(packages:"java.,org.xml.,javax.,org.xml.",href:"http://download.oracle.com/javase/6/docs/api")
                        link(packages:"groovy.,org.codehaus.groovy.",  href:"http://groovy.codehaus.org/api")
                        link(packages:"org.apache.tools.ant.",         href:"http://evgeny-goldin.org/javadoc/ant/api")
                        link(packages:"org.junit.,junit.framework.",   href:"http://kentbeck.github.com/junit/javadoc/latest")
                        link(packages:"org.codehaus.gmaven.",          href:"http://evgeny-goldin.org/javadoc/gmaven")
                }
    }
    catch(Exception e) {
        event("StatusError", ["Error generating groovydoc: ${e.message}"])
    }
    event("DocEnd", ['groovydoc'])
}

setDefaultTarget(main)
