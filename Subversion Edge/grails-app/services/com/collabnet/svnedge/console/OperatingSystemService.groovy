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
package com.collabnet.svnedge.console

import org.hyperic.sigar.OperatingSystem
import org.hyperic.sigar.Sigar

import java.lang.reflect.Field
import grails.util.GrailsUtil
import org.hyperic.sigar.ProcState
import org.hyperic.sigar.SigarException

/**
 * The Operating System service is related to process level, services, OS info,
 * and others. It proxies the majority its methods to the ones found at the
 * SIGAR framework.
 *
 * @author Marcello de Sales (mdesales@collab.net)
 */
class OperatingSystemService {

    /**
     * Non-transactional service
     */
    boolean transactional = false
     /**
     * The singleton instance for the sigar instance
     */
    def Sigar sigarInstance
    /**
     * The root path of the volume where the CSVN app is running from.
     */
    def String appRootVolumePath
    /**
     * The root path of the volume where the OS is running from.
     */
    def String sysRootVolumePath
    /**
     * Defines if the service has loaded the SIGAR libraries correctly.
     */
    def boolean loadedLibraries

    /**
     * @param appHomePath is the home directory of the installation.
     * @throws FileNotFoundException if the application root folder is not
     * found.
     */
    def bootstrap = { appHomePath ->
        bootstrapSigarLibraries(appHomePath)
        this.sigarInstance = new Sigar()
        if (isWindows()) {
            def windowsMatcher = appHomePath =~ /[A-Za-z]../
            this.appRootVolumePath = windowsMatcher[0]
            //FIXME: The system root should be the PATH where Windows is installed.
            this.sysRootVolumePath = windowsMatcher[0]
        } else {
            //FIXME: The app volume can be on a mounted file system. Need to find a way to have the correct value.
            this.appRootVolumePath = "/"
            this.sysRootVolumePath = "/"
        }
        loadedLibraries = true
        this.printProperties()
    }

    /**
     * @return whether the service is ready to be used. That means, if all
     * needed libraries were loaded. For this service, the SIGAR framework
     * libraries.
     */
    def boolean isReady() {
        return loadedLibraries
    }

    def printProperties() {
        log.info("########## System information ##########")
        log.info("# CSVN root path: ${this.appRootVolumePath}")
        this.properties.each {key, value -> 
            log.info("# ${key}: ${value}")
        }
        log.info("########################################")
        log.debug("########## System Properties ###########")
        this.systemProperties.each {key, value ->
            log.debug("# ${key}: ${value}")
        }
        log.debug("########################################")
        log.debug("######## Environment Variables #########")
        this.environmentVariables.each {key, value ->
            log.debug("# ${key}: ${value}")
        }
        log.debug("########################################")
    }

    /**
     * Releases all the native code loaded by the Sigar framework, as it is
     * documented in the method call Sigar.close().
     */
    def void destroy() {
        if (this.isReady()) {
            sigarInstance.close()
        }
    }

    /**
     * Adds the SIGAR libraries into the java.library.path while running the
     * application in development or testing environments.
     */
    private void bootstrapSigarLibraries(appHomePath) {
        if (GrailsUtil.environment != "production"){
            log.debug("Bootstrapping the SIGAR libraries at java.library.path")
            def sigarLib = new File(appHomePath, "../ext/sigar").canonicalPath
            try {
                addDirToJavaLibraryPathAtRuntime(sigarLib)
            } catch (Exception e) {
                log.error("Error adding the SIGAR libraries to " + 
                    "java.library.path: " + e.message)
            }
        }
    }

    /**
     * Updates the java.library.path at run-time.
     * @param libraryDirPath
     */
    def void addDirToJavaLibraryPathAtRuntime(String libraryDirPath) 
        throws Exception{
        try {
            Field field = ClassLoader.class.getDeclaredField("usr_paths");
            field.setAccessible(true);
            String[] paths = (String[])field.get(null);
            for (int i = 0; i < paths.length; i++) {
                if (libraryDirPath.equals(paths[i])) {
                    return;
                }
            }
            String[] tmp = new String[paths.length+1];
            System.arraycopy(paths,0,tmp,0,paths.length);
            tmp[paths.length] = libraryDirPath;
            field.set(null,tmp);
            def javaLib = "java.library.path"
            System.setProperty(javaLib, System.getProperty(javaLib) +
                File.pathSeparator + libraryDirPath);

        } catch (IllegalAccessException e) {
            throw new IOException("Failed to get permissions to set " +
                "library path to " + libraryDirPath);
        } catch (NoSuchFieldException e) {
            throw new IOException("Failed to get field handle to set " +
                "library path to " + libraryDirPath);
        }
    }

    /**
     * @return the instance of the OperatingSystem singleton from the Sigar
     * Framework.
     */
    def getOs() {
        try {
            if (this.isReady()) {
                return OperatingSystem.getInstance()
            }
        } catch (Error e) {
            log.error("The system could not load the SIGAR framework libraries")
            loadedLibraries = false
            return null
        }
    }

    /**
     * @return the key-value list of the properties of the Operating System.
     * An example of the properties list is as follows:
     * <li>Arch: i686</li>
     * <li>Name: Linux</li>
     * <li>VendorName: Linux</li>
     * <li>Description: Ubuntu 10.04</li>
     * <li>VendorVersion: 10.04</li>
     * <li>Machine: i686</li>
     * <li>Vendor: Ubuntu</li>
     * <li>VendorCodeName: lucid</li>
     * <li>PatchLevel: unknown</li>
     * <li>Version: 2.6.32-23-generic</li>
     */
    def getProperties() {
        return this?.os?.toMap() ?: [:]
    }

    /**
     * @return if the current running app is running on Windows.
     */
    def boolean isWindows() {
        return OperatingSystem.IS_WIN32
    }

    def boolean isSolaris() {
        return System.getProperty("os.name").startsWith("Sun")
    }
    
    /**
     * @return the single implementation of the sigar.
     */
    def getSigar() {
        if (!this.isReady()) {
            throw new IllegalStateException("The SIGAR library was not loaded.")
        }
        if (!this.sigarInstance) {
            this.sigarInstance = new Sigar()
        }
        return this.sigarInstance
    }

    /**
     * @return the map of the system properties. A proxy to 
     * System.getProperties().
     */
    def getSystemProperties() {
        return System.getProperties()
    }

    /**
     * @return the map of the system environment variables. 
     * A proxy to System.getevn().
     */
    def getEnvironmentVariables() {
        return System.getenv()
    }

    /**
     * tests a given process id for existence 
     * @param pid
     * @return boolean if the pid belongs to a running process
     */
    boolean getProcessExists(String pid) {
        try {
            ProcState procState = sigar.getProcState(pid)
            if([ProcState.IDLE, ProcState.RUN, ProcState.SLEEP].contains(procState.state)) {
                log.info("Process pid '${pid}' belongs to healthy process")
                return true
            }
            else if ([ProcState.STOP, ProcState.ZOMBIE].contains(procState.state)) {
                log.warn("Process pid '${pid}' exists, but is STOPPED or is ZOMBIE")
                return true
            }
        }
        catch (Exception e) {
            log.warn ("Testing for process '${pid}' failed with message: ${e.message}")
            if (e.message == "The SIGAR library was not loaded.")  {
                log.warn("SIGAR loading issue, the pid cannot be tested. Assuming the process is alive")
                return true
            }
            return false
        }
    }

   /**
    * Truncate a number to the given number of digits.  Since we don't have
    * an explicit truncate, multiply the number by 10^digits (so that the
    * last digit we want is in the ones place), take the floor (to drop the
    * decimals), then divide by 10^digits to get back to the original
    * magnitude.
    */
   public static truncate(number, digits) {
       Math.floor(number * 10**digits) / 10**digits
   }

   /**
    * Given a number of seconds, return the approximate number of minutes.
    */
   public static formatMinutes(seconds) {
       return Math.round(seconds / 60)
   }

   /**
    * Format a value given in bytes to something human-readable (i.e. 32.56 GB)
    * @return formatted string or null for null input
    */
   public static formatBytes(space) {
       if (space == null) {
           return null
       }
       def prefixes = ['', 'K', 'M', 'G', 'T', 'P', 'E']
       def mag = prefixes.size() - 1
       for (int i = 0; i < prefixes.size(); i++) {
           if (space < 1024**(i + 1)) {
               mag = i
               break
           }
       }
       def value = space.floatValue() / (1024**mag)
       return truncate(value, 2) + " " + prefixes[mag] + "B"
   }

}