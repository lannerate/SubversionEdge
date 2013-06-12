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
package com.collabnet.svnedge.util

import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.hyperic.sigar.FileInfo
import org.apache.commons.compress.archivers.zip.ZipFile
import java.util.zip.ZipException

/**
 * General file-handling util methods. This class must be used as via spring bean "fileUtil", as it has
 * service dependencies
 */
public class FileUtil {

    static Log log = LogFactory.getLog(FileUtil.class)
    def operatingSystemService
    def commandLineService

    /**
     * Utility method to unzip an archive to a given directory using Apache Commons Compress
     * @param zipFile the archive to unzip
     * @param parentDir the location into which to unzip
     * @param progress the output stream for capturing output
     */
    public void extractArchive(File zipFile, File parentDir, OutputStream progress = null) {

        ZipFile zf = null
        try {
            zf = new ZipFile(zipFile)
            for (ZipArchiveEntry entry : zf.entries) {
                File f = new File(parentDir.getAbsolutePath() + File.separator + entry.name)
                if (entry.isDirectory()) {
                    f.mkdirs()
                    if (progress) {
                        progress << "Creating dir: ${f.absolutePath}"
                    }
                }
                else {
                    f.parentFile.mkdirs()
                    if (progress) {
                        progress << "Inflating file: ${f.absolutePath}"
                    }
                    f.withOutputStream { it << zf.getInputStream(entry) }
                }

                def mode = entry.getUnixMode()
                if (!operatingSystemService.isWindows() && mode > 0) {
                    def octalMode = Integer.toString(mode, 8)
                    if (octalMode.length() > 4) {
                        octalMode = octalMode.substring(octalMode.length() - 4)
                    }
                    def chmodCmd = ["chmod", octalMode, f.absolutePath]
                    String[] result = commandLineService.execute(chmodCmd, progress, progress)
                }
            }
        }
        finally {
            zf?.close()
        }
    }

    /**
     * Utility method to zip a directory
     * @param directory
     * @param zipFile
     * @param progress the stream into which output is sent (optional)
     */
    public void archiveDirectory(File directory, File zipFile, OutputStream progress = null) {
        if (zipFile.parentFile.equals(directory)) {
            throw new IllegalArgumentException(
                    "Archive should not be written into the directory being archived")
        }
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("directory argument was not a directory")
        }
        boolean storePermissions = !operatingSystemService.isWindows()

        ZipArchiveOutputStream zos = null
        try {
            zos = new ZipArchiveOutputStream(zipFile)
            zos.setCreateUnicodeExtraFields(ZipArchiveOutputStream
                    .UnicodeExtraFieldPolicy.NOT_ENCODEABLE)
            recursiveArchiveDirectory(directory.canonicalPath.length() + 1,
                    directory, zos, storePermissions, progress)
        }
        catch (ZipException e) {
            log.info("Error zipping directory, likely caused by presence of symlink(s); " + 
                     "attempting alternate zip using command line")
            log.debug("ZipException details", e)
            zos?.close()
            zipFile.delete()
            archiveDirectoryUsingCommandLine(directory, zipFile, progress)
        }
        finally {
            zos?.close()
        }
    }

    /**
     * Utility method to zip a directory using the zip command line tool
     * @param directory the directory to zip
     * @param zipFile the target file
     * @param progress the stream into which output is sent (optional)
     */
    public void archiveDirectoryUsingCommandLine(File directory, File zipFile, OutputStream progress = null) {
        String wd = directory.absolutePath
        String[] result = 
                commandLineService.execute(["zip", "-r", zipFile.absolutePath, '.'],
                        progress, progress, ["PWD" : wd])
        if (result[0] != "0") {
            def msg = "Unable to zip directory, make sure 'zip' utility is installed and on path"
            log.error(msg)
            throw new RuntimeException(msg)
        }
    }

    private void recursiveArchiveDirectory(int topLevelPathLength, File directory,
            ZipArchiveOutputStream zos, boolean storePermissions, OutputStream progress)
            throws ZipException {

        directory.eachFile { f ->
            String fullPath = f.canonicalPath
            // check for symlink, throw error if true
            if (!f.canonicalPath.equalsIgnoreCase(f.absolutePath.replace('.' + File.separator, ''))) {
                throw new ZipException("The file appears to be a symlink and cannot be handled properly")
            }
            String relativePath = fullPath.substring(topLevelPathLength, fullPath.length())
            if (f.isDirectory()) {
                relativePath += "/"
            }
            if (progress) {
                progress << relativePath + "\n"
            }
            ZipArchiveEntry ze = new ZipArchiveEntry(f, relativePath)
            if (storePermissions) {
                int perms = 0444
                try {
                    FileInfo fi = operatingSystemService.getSigar().getFileInfo(fullPath)
                    log.debug(relativePath + " permString=" + fi.permissionsString +
                            " asInt=" + fi.mode)
                    perms = Integer.parseInt(fi.mode.toString(), 8)
                } catch (IllegalStateException e) {
                    log.info("Sigar library not loaded, setting hotcopy file " +
                            "permissions for owner only with group and other as defaults")
                    if (f.isFile()) {
                        perms = 0444
                        if (f.canWrite() && f.canExecute()) {
                            perms = 0744
                        } else if (f.canWrite()) {
                            perms = 0644
                        } else if (f.canExecute()) {
                            perms = 0544
                        }
                    } else {
                        perms = 0755
                    }
                }
                ze.setUnixMode(perms)
            }
            zos.putArchiveEntry(ze)
            if (f.isFile()) {
                f.withInputStream { zos << it }
            }
            zos.closeArchiveEntry()

            if (f.isDirectory()) {
                recursiveArchiveDirectory(topLevelPathLength, f, zos,
                        storePermissions, progress)
            }
        }
    }

    /**
     * Checks the given file for non-ascii characters
     */
    boolean isAsciiText(File f) {
        boolean b = true
        f.withInputStream {
            int i
            while ((i = it.read()) != -1) {
                if (i < 1 || i > 127) {
                    b = false
                    return
                }
            }
        }
        return b
    }
}
