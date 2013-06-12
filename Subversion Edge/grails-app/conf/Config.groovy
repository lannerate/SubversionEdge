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
// locations to search for config files that get merged into the main config
// config files can either be Java properties files or ConfigSlurper scripts

// grails.config.locations = [ "classpath:${appName}-config.properties",
//                             "classpath:${appName}-config.groovy",
//                             "file:${userHome}/.grails/${appName}-config.properties",
//                             "file:${userHome}/.grails/${appName}-config.groovy"]

// if(System.properties["${appName}.config.location"]) {
//    grails.config.locations << "file:" + System.properties["${appName}.config.location"]
// }
grails.mime.file.extensions = true // enables the parsing of file extensions from URLs into the request format
grails.mime.use.accept.header = false
grails.mime.types = [ html: ['text/html','application/xhtml+xml'],
                      xml: ['text/xml', 'application/xml'],
                      text: 'text/plain',
                      js: 'text/javascript',
                      rss: 'application/rss+xml',
                      atom: 'application/atom+xml',
                      css: 'text/css',
                      csv: 'text/csv',
                      all: '*/*',
                      json: ['application/json','text/json'],
                      form: 'application/x-www-form-urlencoded',
                      multipartForm: 'multipart/form-data'
                    ]
// The default codec used to encode data with ${}
grails.views.default.codec="html" // none, html, base64
grails.views.gsp.encoding="UTF-8"
grails.converters.encoding="UTF-8"
grails.json.legacy.builder = false

// enabled native2ascii conversion of i18n properties files
grails.enable.native2ascii = true

// See GRAILS-5726, we are not using jndi
grails.naming.entries = 0 // not instanceof Map

grails.app.context="/csvn"

grails.logging.jul.usebridge = true

// See artf7027 and chapter 7 of the grails user manual
grails.validateable.classes = [com.collabnet.svnedge.controller.integration.CtfConnectionCommand]

def baseDir = new File(new File(".").getAbsoluteFile(), "svn-server").absolutePath
svnedge {
    defaultHighPort = 18080
    defaultApacheAuthHelperPort = 49152
    osName = System.getProperty("os.name").substring(0,3)
    appHome = baseDir
    if (osName == "Win") {
        // This will point to the parent directory of the application once production ready.
        // In development mode, this should point to the location of the binaries, not the webapp
        svn {
            // The following paths have defaults configured with respect to 
            // appHome, but the locations may be overridden using these 
            // properties: svnPath, svnadminPath, httpdPath, httpdPidPath, 
            // htpasswdPath, confDirPath, logDirPath
            svnPath = new File(appHome, "bin/svn.exe").absolutePath
            svnadminPath = new File(appHome, "bin/svnadmin.exe").absolutePath
            viewvcLibPath = new File(appHome, "lib/viewvc").absolutePath
            modPythonPath = new File(appHome, "bin/mod_python").absolutePath
            viewvcTemplatesPath = new File(appHome, "www/viewvc")
                    .absolutePath
            dataDirPath = new File(appHome, "data").absolutePath
            serviceName = "CollabNet Subversion Server"
        }
        opensslPath = new File(appHome, "bin/openssl.exe").absolutePath
    } else {
        svn {
            dataDirPath = appHome + "/data"
        }
    }

    logsDirPath = svn.dataDirPath + "/logs"

    svn.repositoriesParentPath = svn.dataDirPath + "/repositories"
    svn.confDirPath = svn.dataDirPath + "/conf"
    // in minutes
    svn.parseLogRate = 5

    cloudServices {
        baseUrl = "https://api.cloudforge.com/api/1/"
        credentials {
            developerOrganization = "svnEdge"
            developerKey = "0f75a56b3c5966afa4693314f631fcd03682b4f7"
        }
        defaultProductSKU = "Base:CloudService-Free-1201"
    }

    ctfMaster {
        ssl = false
        domainName = "cu125.cloud.sp.collab.net"
        username = "admin"
        password = "admin"
        port = 80
        systemId = "exsy1002"
        replicaId = "replica1044"
        apiKey = "gwcjyiiBYiDL08lxDr7y"
    }

    replica {
        cache {
            //This is the rate at which a positive cache entry will last in
            //MINUTES. They are defined for the long-term cache entries.
            positiveExpirationRate = 10
            //This is the rate at which a negative cache entry will last in
            //SECONDS. They are defined for the short-term cache entries.
            negativeExpirationRate = 2
            //This is the rate at which the flusher will clean the entire cache
            //for both the short and long-term cache entries. It defines the
            //number of the week day [1:mon..7:sun] and the time [0(am)..23(pm)]
            cacheFlushPeriod = 23
        }
        error {
            uploadRate = 5
            minLevel = org.apache.log4j.Level.WARN.toInt()
        }
        stat {
            uploadRate = 5
        }
        xmlrpc {
            serverPort = 22000
        }
        ssl {
            trustStoreFileName = "/WEB-INF/trust.keystore"
            trustStorePasswd = "hippies"
        }
        svn {
            if (osName == "Win") {
                svnsyncPath = new File(appHome, "bin/svnsync.exe").absolutePath
            }
            //svnsyncRate is in minute.
            svnsyncRate = 1
        }
        logging {
            commandOutput = true
            commandStateTransitions = false
        }
    }
    // Multi-cast DNS properties
    mdns {
        serviceName = "collabnetsvn"
        port = 8080; // set default value
        if (System.getProperty("jetty.port")) {
            // try to set to the "jetty.port" value
            try {
                port = Integer.parseInt(System.getProperty("jetty.port").trim())
            }
            catch (Exception e) {
                // ignore exception and use default port
            }
        }
        // this path is added to the grails.app.context value
        teamForgeRegistrationPath = "/setupTeamForge/index";
    }
    helpUrl = "http://help.collab.net"

    softwareupdates {
        //hudsonUrl = "http://cu087.cloud.sp.collab.net:8080/"
        //isHudson = System?.getenv("HUDSON_URL")?.equals(hudsonUrl)
        imagepath = new File(".").canonicalPath + "/target/csvn-image-test-${appVersion}"
        currentVersion = appVersion
    }

    httpClient {
        truststore.path = new File(new File(svn.dataDirPath, "certs"), "svnedge-client.jks").absolutePath
        keystore.path = new File(new File(svn.dataDirPath, "certs"), "svnedge-client.jks").absolutePath
        keystore.pass = 'svnedge'
        cert.hostnameVerifier = 'BROWSER_COMPATIBLE'
        sslSocketFactory.enforce = true
    }
}

// set per-environment serverURL stem for creating absolute links
environments {
    test {
        svnedge {
            replica {
                logging {
                    commandStateTransitions = true
                }
            }
        }
        ldapServers {
            dev1 {
                base = "dc=collabnet,dc=com"
                port = 10389
            }
        }
    }
    development {
        ldapServers {
            dev1 {
                base = "dc=collabnet,dc=com"
                port = 10389
            }
        }
    }
    production {
        grails.serverURL = "http://www.changeme.com"
        grails.plugin.excludes = "greenmail"
        baseDir = new File(new File(".").getAbsoluteFile().
                       getParentFile().parentFile, "").absolutePath
        svnedge {
            defaultHighPort = 18080
            osName = System.getProperty("os.name").substring(0,3)
            appHome = baseDir
            if (osName == "Win") {
                // This will point to the parent directory of the application once production ready.
                // In development mode, this should point to the location of the binaries, not the webapp
                svn {
                    // The following paths have defaults configured with respect to 
                    // appHome, but the locations may be overridden using these 
                    // properties: svnPath, svnadminPath, httpdPath, httpdPidPath, 
                    // htpasswdPath, confDirPath, logDirPath
                    svnPath = new File(appHome, "bin/svn.exe").absolutePath
                    svnadminPath = new File(appHome, "bin/svnadmin.exe").absolutePath
                    viewvcLibPath = new File(appHome, "lib/viewvc").absolutePath
                    modPythonPath = new File(appHome, "bin/mod_python").absolutePath
                    viewvcTemplatesPath = new File(appHome, "www/viewVC")
                            .absolutePath
                    dataDirPath = new File(appHome, "data").absolutePath
                    serviceName = "CollabNet Subversion Server"
                }
                opensslPath = new File(appHome, "bin/openssl.exe").absolutePath
            } else {
                svn {
                    dataDirPath = appHome + "/data"
                }
            }

            logsDirPath = svn.dataDirPath + "/logs"
            svn.repositoriesParentPath = svn.dataDirPath + "/repositories"
            svn.confDirPath = svn.dataDirPath + "/conf"
            // in minutes
            svn.parseLogRate = 5

            ctfMaster {
                ssl = false
                domainName = "cu125.cloud.sp.collab.net"
                username = "admin"
                password = "admin"
                port = 80
                systemId = "exsy1002"
                apiKey = "gwcjyiiBYiDL08lxDr7y"
            }

            replica {
                cache {
                    //This is the rate at which a positive cache entry will last in
                    //MINUTES. They are defined for the long-term cache entries.
                    positiveExpirationRate = 10
                    //This is the rate at which a negative cache entry will last in
                    //SECONDS. They are defined for the short-term cache entries.
                    negativeExpirationRate = 2
                    //This is the rate at which the flusher will clean the entire cache
                    //for both the short and long-term cache entries. It defines the
                    //number of the week day [1:mon..7:sun] and the time [0(am)..23(pm)]
                    cacheFlushPeriod = 23
                }
                error {
                    uploadRate = 5
                    minLevel = org.apache.log4j.Level.WARN.toInt()
                }
                stat {
                    uploadRate = 5
                }
                xmlrpc {
                    serverPort = 22000
                }
                ssl {
                    trustStoreFileName = "/WEB-INF/trust.keystore"
                    trustStorePasswd = "hippies"
                }
                svn {
                    if (osName == "Win") {
                        svnsyncPath = new File(appHome, "bin/svnsync.exe").absolutePath
                    }
                    //svnsyncRate is in minute.
                    svnsyncRate = 1
                }
            }
            helpUrl = "http://help.collab.net"

            softwareupdates {
                imagepath = appHome
            }

            httpClient {
                truststore.path = new File(new File(svn.dataDirPath, "certs"), "svnedge-client.jks").absolutePath
                keystore.path = new File(new File(svn.dataDirPath, "certs"), "svnedge-client.jks").absolutePath
            }
        }

        def catalinaBase = System.properties.getProperty('catalina.base')
        if (!catalinaBase) catalinaBase = '.'   // just in case

        log4j = {

            appenders {
                //not to log to the regular svnedge.log
                'null' name:'stacktrace'
            }

            root {
                warn 'console'
                additivity = true
            }
            // Commenting out to not output to svnedge.log
            warn 'grails.app'
        }
        codenarc {
            reportName = 'CodeNarcReport.xml'
            reportType = 'xml'
        }
    }
}

// log4j configuration
log4j = {
    // Example of changing the log pattern for the default console
    // appender:
    //
    //appenders {
    //    console name:'stdout', layout:pattern(conversionPattern: '%c{2} %m%n')
    //}

    error  'org.codehaus.groovy.grails', // grails core and web
           'org.codehaus.groovy.grails.plugins', // plugins
           'org.codehaus.groovy.grails.orm.hibernate', // hibernate integration
           'org.springframework',
           'org.hibernate',
           'grails.plugin.springevents'

    warn   'org.mortbay.log',
           'org.apache',
           'net.sf.ehcache',
           'org.quartz'

    debug  'grails.app'

    root {
           debug 'console'
           additivity = true
    }
}

codenarc {
    reportName = 'target/test-reports/CodeNarcReport.html'
    reportType = 'html'
}

beans {
    applicationEventMulticaster {
        taskExecutor = java.util.concurrent.Executors.newCachedThreadPool()
    }
}

def overridesPath = "${baseDir}/data/conf/overrides.properties"
File f = new File(overridesPath)
if (f.exists()) {
    grails.config.locations = [ f.toURI().toString() ]
}
