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
package com.collabnet.svnedge.integration.command.impl

import com.collabnet.svnedge.integration.command.AbstractReplicaCommand 
import com.collabnet.svnedge.integration.command.ShortRunningCommand 
import org.apache.log4j.Logger


/**
 * This command reverts the state of the SvnEdge instance to standalone mode
 * 
 * @author Geoffrey
 */
public class ReplicaUnregisterCommand extends AbstractReplicaCommand 
        implements ShortRunningCommand {

    private Logger log = Logger.getLogger(getClass())

    def constraints() {

    }

    def execute() {
        log.debug("Acquiring the replica setup service...")
        def replicaService = getService("setupReplicaService")

        def errors = []
        replicaService.revertFromReplicaMode(errors, Locale.defaultLocale)

        errors.each { log.error (it) }
   }

   def undo() {
       log.warn("Execute failed... Undoing the command (doesn't do anything)...")
    }
}
