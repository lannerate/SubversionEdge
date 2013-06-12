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
package com.collabnet.svnedge.integration.command

import com.collabnet.svnedge.SvnEdgeException

/**
 * Exception raised when a command has been requested, but there is no 
 * implementing class. The name of the command is retrieved by "commandName".
 * The cause of the exception can be retrieved from the exception as well.
 * 
 * @author Marcello de Sales (mdesales@collab.net)
 *
 */
public class CommandNotImplementedException extends SvnEdgeException {

    private String commandName

    /**
     * When the attempt to execute a method throws a ClassNotFoundException,
     * then it is clear that the command requested has not been implemented
     * in the package com.collabnet.svnedge.replication.command.CMDNAME.
     * @param cmdName is the name of the command.
     */
    def CommandNotImplementedException(classNotFoundExe, cmdName) {
        super("The requested command '" + cmdName + "' is not implemented.", 
                classNotFoundExe)
        commandName = cmdName
    }
}
