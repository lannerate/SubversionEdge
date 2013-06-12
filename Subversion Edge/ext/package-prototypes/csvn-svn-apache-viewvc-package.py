pkg = {

    "name" : "csvn-svn",

    "version" : "@{version}",

    "attributes" : {
        "pkg.summary" : "CollabNet Subversion native binaries",
        "pkg.description" : "The native binaries for Apache httpd, Subversion, ViewVC and required dependencies.",
        "info.classification" : "System Tools"
    },

    "defaults" : { "file" : {"mode" : "0755", "group" : "bin", "owner" : "root",},
                   "dir"  : {"mode" : "0755", "group" : "bin", "owner" : "root",},
    },

    "files" : {
        "lib/integration-scripts.zip" : {},
    },

    "dirtrees" : {
        "bin"        : {"os" : [ "solaris", "unix" ]},
        "dist"       : {},
        "lib"        : {"os" : [ "solaris", "unix" ]},
        "licenses"   : {},
        "temp-data"  : {},
        "updates"    : {"os" : "windows"},
        "www"        : {"os" : [ "solaris", "unix" ]},
    },

    "excludefiles" : [
        "bin/csvn",
        "bin/csvn-httpd",
        "bin/wrapper-linux-x86-32",
        "bin/wrapper-linux-x86-64",
        "bin/wrapper-solaris-x86-32",
        "bin/wrapper-solaris-x86-64",
        "bin/wrapper-solaris-sparc-32",
        "bin/wrapper-solaris-sparc-64",
        "temp-data/conf/csvn-wrapper.conf",
        "lib/libsigar-amd64-linux.so",
        "lib/libsigar-x86-linux.so",
        "lib/libsigar-amd64-solaris.so",
        "lib/libsigar-x86-solaris.so",
        "lib/libwrapper-solaris-x86-32.so",
        "lib/libwrapper-solaris-x86-64.so",
        "lib/libwrapper-solaris-sparc-32.so",
        "lib/libwrapper-solaris-sparc-64.so",
        "lib/libwrapper-linux-x86-32.so",
        "lib/libwrapper-linux-x86-64.so",
        "lib/wrapper.jar",
        "updates/bin/csvn.bat",
        "updates/bin/InstallCsvn-NT.bat",
        "updates/bin/UninstallCsvn-NT.bat",
        "updates/bin/wrapper.exe",
        "updates/bin/sigar-amd64-winnt.dll",
        "updates/bin/sigar-x86-winnt.dll",
        "updates/lib/wrapper.dll",
        "updates/lib/wrapper.jar",
    ],
    
    "excludedirs" : [
        "appserver",
        "lib/svn-python/libsvn",
        "lib/svn-python/svn",
        "updates/appserver",
        "updates/svcwrapper",
    ],

    "licenses" : {
        "licenses/subversion.txt" : {"license" : "Subversion License" },
        "licenses/apache-2.0.txt" : {"license" : "Apache 2.0" },
        "licenses/lgpl-2.1.txt" : {"license" : "LGPLv2.1 - Neon" },
        "licenses/openssl.txt" : {"license" : "OpenSSL License" },
        "licenses/viewvc.txt" : {"license" : "ViewVC License" },
    },
}
