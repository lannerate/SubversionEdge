grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"

//grails.project.war.file = "target/${appName}-${appVersion}.war"
grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // uncomment to disable ehcache
        // excludes 'ehcache'
		//excludes group: 'org.mortbay.jetty', name: 'jsp-2.0'
        excludes group: 'tomcat', name:'jasper-compiler'
        excludes group: 'tomcat', name:'jasper-compiler-jdt'
        excludes group: 'tomcat', name:'jasper-runtime'
        excludes group: 'org.eclipse.jdt', name: 'core'
        excludes group: 'javax.servlet', name: 'jstl'
        excludes group: 'apache-taglibs', name: 'standard'
        excludes 'commons-el'
        excludes group: 'net.sourceforge.nekohtml', name: 'nekohtml'
    }
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    repositories {
        grailsPlugins()
        grailsHome()
        grailsCentral()

        // uncomment the below to enable remote dependency resolution
        // from public Maven repositories
        //mavenLocal()
        mavenCentral()
        //mavenRepo "http://snapshots.repository.codehaus.org"
        //mavenRepo "http://repository.codehaus.org"
        //mavenRepo "http://download.java.net/maven/2/"
        //mavenRepo "http://repository.jboss.com/maven2/"
    }
    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.
        // runtime 'mysql:mysql-connector-java:5.1.5'
        compile 'org.ini4j:ini4j:0.5.2'
    }
}
grails.war.resources = { stagingDir ->
    def libDir = new File(stagingDir, 'WEB-INF/lib')
    def deleteJars = { name, excludeList = [] ->
        libDir.eachFile { f ->
           if (f.name.startsWith(name)) {
               boolean isDelete = true
               excludeList?.each {
                   if (f.name.startsWith(it)) {
                       isDelete = false
                   }
               }
               if (isDelete) {
                   f.delete()
                   println "Deleted ${f.name}"
               }
           }
        }
    }
    deleteJars("CodeNarc")
    deleteJars("FastInfoset")
    // acegi plugin includes several auth mechanisms we don't use
    deleteJars("cas-client-core")
    deleteJars("ant-contrib")
    deleteJars("facebook-java-api")
    deleteJars("easymock")
    deleteJars("htmlparser")
    deleteJars("jcifs")
    deleteJars("json-20070829")
    deleteJars("openid4java")
    deleteJars("openxri-client")
    deleteJars("openxri-syntax")    
    deleteJars("spring-ldap")
    deleteJars("spring-security-cas-client")
    deleteJars("spring-security-openid")
    deleteJars("xmlsec")
    //deleteJar("")
    // jetty plugin requires some default globally inherited jars to build, but are
    // not needed in the war.  Should be possible to configure this better, but
    // nothing I've tried in "dependencies" or "plugins" allows the plugin to build.
    // And we also invoke jetty code in 'run-app' mode to deploy integration war.
    deleteJars("jetty", ['jetty-util'])
}
  