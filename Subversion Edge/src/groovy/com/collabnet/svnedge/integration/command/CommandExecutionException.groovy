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
 * Exception for the Abstract Commands, which has references to
 * the originating command, and the exceptions that might have occurred
 * during either/both the execution or/and roll-back.
 * 
 * @author Marcello de Sales (mdesales@collab.net)
 */
public class CommandExecutionException extends SvnEdgeException {

    def originatingCommand

    def executionThrowable

    def undoThrowable

    /**
     * The command execution exception describes if the exception occurred
     * during either/both the execution or/and the undoException. In the
     * case both exceptions occur, the execution exception will have priority
     * on the traceability of this Exception.
     */
    def CommandExecutionException(AbstractCommand command, 
            Throwable executionException, Throwable undoException) {
        super("Errors occurred while executing the action command '"+
                "${command.getClass().getName()}: " +
                executionException ? "While executing the command: " +
                        "${executionException.getMessage()}" : " " + 
                undoException ? "While undoing the command: " +
                        "${undoException.getMessage()}" : "", 
                        executionException ?:undoException)
        executionException = executionException
        undoException = undoException
        originatingCommand = command
    }

    def getUsedCommandParameters() {
        return originatingCommand.params
    }

    def getExecutionThrowable() {
        return executionThrowable
    }

    def getUndoThrowable() {
        return undoThrowable
    }
}
