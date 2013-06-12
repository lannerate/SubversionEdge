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

import java.io.File
import java.io.FileNotFoundException
import java.io.PrintStream;
import java.util.zip.GZIPInputStream
import org.apache.tools.tar.TarInputStream

/**
 * This category overrides the left-shift operator for "untarring" a file to
 * a destination directory. If the destination directory does not exists,
 * it creates one.
 * <pre>
 *   use(UntarCategory) {
 *      new File("/tmp/backup") << new File("/tmp/download.tar.gz")
 *   }
 * </pre>
 * 
 * @author Marcello de Sales (mdesales@collab.net)
 *
 */
final class UntarCategory {

    /**
     * Sets the progress output stream.
     */
    def static PrintStream progressPrintStream
    /**
     * If the root directory of the zipped file should be removed.
     */
    def static boolean removeRootDir

    /**
     * Overrides the left-shift operator.
     * @param toDir is the destination directory.
     * @param tarFile is the tar file to be extracted.
     * @throws FileNotFoundException in case the given tarFile does not exist.
     */
    def static leftShift(File toDir, File tarFile)
        throws FileNotFoundException {

        if (!tarFile.exists()) {
            throw new FileNotFoundException("The tar file '${tarFile}' does " +
                "not exist!")
        } else if (!toDir.isDirectory()) {
            toDir.mkdirs()
        }

        if (progressPrintStream) {
            progressPrintStream.println("Extracting contents from '${tarFile}'")
            if (removeRootDir) {
                progressPrintStream.println("Removing the root directory...")
            }
        }
        //TODO: support .tar, .gz as well. This is only for .tar.gz
        TarInputStream tin = new TarInputStream(new GZIPInputStream(
            new FileInputStream(tarFile)))
        def tarEntry
        def counter = 0
        def root
        while ((tarEntry = tin.getNextEntry()) != null) {
            if (removeRootDir && ++counter == 1 && tarEntry.isDirectory()) {
                root = tarEntry.getName()
                continue
            }
            def entryName = root ? tarEntry.getName().replaceFirst(root, "") : 
                tarEntry.getName()
            def destPath = new File(toDir.canonicalPath + File.separatorChar +
                entryName)
            if (progressPrintStream) {
                progressPrintStream.println("'${destPath.canonicalPath}'")
            }
            if (tarEntry.isDirectory()){
                destPath.mkdir()
            } else {
                def fout = new FileOutputStream(destPath)
                tin.copyEntryContents(fout)
                fout.close()
            }
        }
        if (progressPrintStream) {
            progressPrintStream.println("Finished extracting '${tarFile}'")
        }
        tin.close();
    }
}
