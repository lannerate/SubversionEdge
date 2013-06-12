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

import com.collabnet.svnedge.domain.Repository 
import com.collabnet.svnedge.domain.SvnLog 
import java.text.SimpleDateFormat
import java.util.regex.Pattern

/**
 * The SvnLogService parses data from the subversion.log file and loads each
 * line into the database as an SvnLog object.
 */
class SvnLogService {
    
    boolean transactional = true
    
    def svnLogFile
    
    /**
     * There are a few additional arguments in the log file but I'm not
     * sure what they signify, so they are not included in SvnLog yet.
     */
    def logFilePattern = Pattern.compile("\\[([^]]*)\\] (\\S+) (\\S+) (\\S+)" +
                                             " \\(?([\\S&&[^)]]+)\\)? (\\S+)")

    def dateFormat = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z")

    def bootStrap = { svnLogFile ->
        this.svnLogFile = svnLogFile
    }

    def parseFile() {
        def logFile = new File(svnLogFile)
        if (!logFile.exists()) {
            log.error("The subversion log file (${svnLogFile}) does not " + 
                          "exist.")
            return
        }
        SvnLog last = getLastLog()
        def line
        def svnLogs = []
        SvnLog current
        def fileIn = skipToLast(last, logFile)
        while ((line = fileIn.readLine()) != null) {
            current = parseLine(line, fileIn.getLineNumber())
            if (current) {
                svnLogs << current
            }
        }
        svnLogs.each { it.save() }        
    }

    /**
     * Parse a single line of a log file and return an (unsaved) SvnLog.
     * If the line is unparsable, returns null.
     */
    def parseLine(str, lineNumber) {
        def match = logFilePattern.matcher(str)
        if (!match) {
            log.error("Found unparsable line in log file ($str)")
            return null
        }
        def date = dateFormat.parse(match.group(1))
        def repo = Repository.findByName(match.group(3))
        new SvnLog(lineNumber: lineNumber,
                   timestamp: date.getTime(),
                   username: match.group(2),
                   repo: repo,
                   action: match.group(4),
                   path: match.group(5),
                   revision: match.group(6))     
    }

    /**
     * Return the last successfully parse log, so that we don't reparse the
     * same lines of the log file.
     */
    def getLastLog() {
        def crit = SvnLog.createCriteria()
        def values = crit.list {
            maxResults(1)
            and {
                order("timestamp", "desc")
                order("lineNumber", "desc")
            }
        }
        values? values[0] : null
    }

    /**
     * Return the first successfully parse log.  This can be used by 
     * stat services to set a starting time.
     */
    def getFirstLog() {
        def crit = SvnLog.createCriteria()
        def values = crit.list {
            maxResults(1)
            and {
                order("timestamp", "asc")
                order("lineNumber", "asc")
            }
        }
        values? values[0] : null
    }

    /**
     * Returned a LineNumberReader skipped to the last parsed log or the 
     * beginning of the log file, if we can't find the last parsed log.  
     * The next readLine is the first line that needs to be parsed.
     */
    def skipToLast(last, logFile) {
        def fileIn = new LineNumberReader(new FileReader(logFile))
        SvnLog current
        def line
        // skip to the last remembered lineNumber
        if (last) {
            def lastFound = false
            while (((line = fileIn.readLine()) != null) 
                   && (fileIn.getLineNumber() < last.getLineNumber())) {}
            if (line) {
                current = parseLine(line, fileIn.getLineNumber())
                if (current && current.valuesEqual(last)) {
                    lastFound = true
                }
            }
            if (!lastFound) {
                // the last isn't what we expect, reopen the stream
                // and parse from the beginning
                fileIn.close()
                fileIn = new LineNumberReader(new FileReader(logFile))
            }
        }
        return fileIn
    }
}
