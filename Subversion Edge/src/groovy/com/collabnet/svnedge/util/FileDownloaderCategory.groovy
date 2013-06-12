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
package com.collabnet.svnedge.util


import groovy.time.TimeCategory

import java.io.File
import java.io.FileNotFoundException
import java.io.PrintStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.TimeZone


/**
 * In order to download a binary file easily, just transfer the contents
 * from a URL to a File object. It overrides the leftShift operator in groovy,
 * that is, the construct "localFile << remoteFile" seems to be very readable
 * to developers.
 * 
 * The normal use is as follows:
 * <pre>
 *   use(FileBinaryDownloaderCategory) {
 *     new File("/tmp/logo-bkp.gif") << "http://www.google.com/images/logo.gif".toURL()
 *   }
 * </pre>
 * @author Marcello de Sales (mdesales@collab.net)
 *
 */
final class FileDownloaderCategory {

    /**
     * Sets the progress output stream.
     */
    def static PrintStream progressPrintStream
    /**
     * Set to true if the downloader must override the downloaded file. If
     * that is not set, IllegalStateException is thrown.
     */
    def static boolean override
    /**
     * Internal flag that verifies that the download has finished.
     */
    private static boolean finishedDownload
    /**
     * Internal formatter for the output.
     */
    private static final dateTimeFormat = new SimpleDateFormat()

    static {
        dateTimeFormat.setTimeZone(TimeZone.getDefault());
    }

    /**
     * Sets the system properties to use the http proxy server 
     * identified by the given URL:PORT using the given credentials.
     * @param url is the URL of the HTTP_PROXY.
     * @param port is the port number of the HTTP_PROXY.
     * @param usr is the username to access the HTTP_PROXY.
     * @param pwd is the password for the given username.
     */
    def static setProxy(url, port, usr, pwd) {
        //TODO: set the proxy from the environment variable 
        System.properties.putAll([
            "http.proxyHost": url,
            "http.proxyPort": port,
            "http.proxyUserName": usr,
            "http.proxyPassword": pwd])
    }

    /**
     * Force the use of the system's currently set HTTP proxy server, if one
     * is set.
     */
    def static useSystemProxy() {
        def proxyUrl = System.getenv("http_proxy") ?: System.getenv("HTTP_PROXY")
        if (proxyUrl) {
            //TODO: implement the parsing of the URL and use the setProxy method.
            setProxy (null, null, null, null)
        }
    }

    /**
     * Sets the SSL certificates for the connection using the given truststore
     * and the password to access the file.
     * @param truststoreFilePath is the path to the file.truststore file. The
     * value javax.net.ssl.trustStore will be set to this path.
     * @param keyStorePassword is the password to the trustStore file.
     */
    def static setTruststore(truststoreFilePath, keyStorePassword) 
        throws FileNotFoundException {

        if (!new File(truststoreFilePath).exists()) {
            throw new FileNotFoundException("The truststore file " +
                "'$truststoreFilePath' does not exist!")
        } else if (!keyStorePassword) {
            throw new IllegalArgumentException("The parameter for the ssl " +
                "'keyStorePassword' must be provided")
        }
        System.setProperty( 'javax.net.ssl.trustStore', truststoreFilePath)
        System.setProperty( 'javax.net.ssl.keyStorePassword', keyStorePassword)
    }

    /**
     * @param inPath is a URL instance path
     * @return the name of the file in a URL instance.
     */
    public static String extractFileName(URL path) {
        if (!path) {
            return null;
        }
        def newpath = path.toString().replace('\\','/')
        int start = newpath.lastIndexOf("/");
        if (start == -1) {
            start = 0;
        } else {
            start++
        }
        return newpath.substring(start, newpath.length());
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
    * Format a value given in bytes to something human-readable (i.e. 32.56 GB)
    */
   public static formatBytes(space) {
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

    /**
     * Overrides the operation "<<" by using categories.
     * @param toFile is the local file to receive the binary content.
     * @param fromUrl is the URL containing the file to be received.
     */
    def static leftShift(File toFile, URL fromUrl)
        throws FileNotFoundException, IllegalStateException {

        if (toFile.exists()) {
            if (!override) {
                throw new IllegalStateException("The file '${toFile}' " + 
                    "already exists. Set the overrides property to true to " +
                    "override it.")
            } else {
                toFile.delete()
            }

        } else {
            // make all the parent directories before
            toFile.getParentFile().mkdirs()
        }

        def input
        def output
        def finishedAt
        def startedAt
        try {
            startedAt = System.currentTimeMillis()
            def conn = fromUrl.openConnection()
            def fileSize = conn.getContentLength() //content-length header

            if (progressPrintStream) {
                progressPrintStream.println("Downloading " +
                    "${formatBytes(fileSize)} from ${fromUrl}. Saving it at " +
                    "'${toFile}'.")
            }

            input = conn.getInputStream()
            output = new BufferedOutputStream(new FileOutputStream(toFile))
            if (progressPrintStream) {
                Thread.start("Download of ${fromUrl}"){
                    def counter = 0
                    while (!finishedDownload) {
                        sleep(400)
                        if (++counter % 50 == 0) {
                            counter = 0
                            def currentSize = toFile.length()
                            def percent = Math.round(
                                (currentSize / fileSize) * 100)
                            progressPrintStream.println(". ${percent}%")
                        } else {
                            progressPrintStream.print(".")
                        }
                    }
                }
            }
            output << input
            finishedAt = System.currentTimeMillis()
            finishedDownload = true

        } finally {
            input?.close()
            output?.close()
        }

        if (progressPrintStream) {
            def start = new Date(startedAt)
            def stop = new Date(finishedAt)
            def timeDiff = TimeCategory.minus(stop, start)
            progressPrintStream.println(" 100%")
            progressPrintStream.println("# Download finished: ${toFile}")
            progressPrintStream.println("# File Size: " + 
                "${formatBytes(toFile.length())}")
            progressPrintStream.println("# Started at: " + 
                dateTimeFormat.format(new Date(startedAt)))
            progressPrintStream.println("# Ended at: " + 
                dateTimeFormat.format(new Date(finishedAt)))
            progressPrintStream.println("# Elapsed: " + timeDiff)
        }
        finishedDownload = false
    }
}
