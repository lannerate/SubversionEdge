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
package com.collabnet.svnedge.domain.integration

/**
 * The Command Result registers the result of a given Abstract Command of
 * whether it succeeded or failed.
 * 
 * The result might be optional associated with a replica server or with a
 * replicated repository.
 * 
 * @author Marcello de Sales (mdesales@collab.net)
 *
 */
class CommandResult {

    /**
     * The command execution ID.
     */
    String commandId
    /**
     * The code of the command. Basically it gives the class name of the command.
     */
    String commandCode
    /**
     * If the command succeeded or not.
     */
    Boolean succeeded
    /**
     * GRAILS DEFAULT Automatic timestamping. By merely defining a dateCreated
     * property these will be automatically updated for you by GORM.
     * The time of the command result was created.
     */
    Date dateCreated
    /**
     * GRAILS DEFAULT Automatic timestamping. By merely defining a lastUpdated 
     * property these will be automatically updated for you by GORM.
     * The time of the command was updated.
     */
    Date lastUpdated
    /**
     * Defines if the command result has been transmitted or not.
     */
    Boolean transmitted = false

    static constraints = {
        commandId(unique:true)
        succeeded(nullable:true)
        // http://grails.org/doc/latest/guide/5.%20Object%20Relational%20Mapping%20(GORM).html#5.5.1 Events and Auto Timestamping
        // If you put nullable: false constraints on either dateCreated or 
        // lastUpdated, your domain instances will fail validation
    }

    @Override
    public String toString() {
        return "CommandResult [id=${id}, commandId=" + commandId + ", succeeded=" +
               succeeded + ", dateCreated=" + dateCreated + ", lastUpdated=" +
               lastUpdated + ", transmitted=" + transmitted + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result =
                prime * result +
                        ((commandId == null) ? 0 : commandId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CommandResult other = (CommandResult) obj;
        if (commandId == null) {
            if (other.commandId != null)
                return false;
        } else if (!commandId.equals(other.commandId))
            return false;
        return true;
    }

}
