/*
 * CollabNet Subversion Edge
 * Copyright (C) 2012, CollabNet Inc. All rights reserved.
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

package com.collabnet.svnedge.schema;

import org.apache.log4j.Logger;

import java.sql.SQLException;

/**
 * This class renames the "Backup" trigger group to "RepoJob" for existing
 * triggers, so that all repo-specific jobs can be 
 * handled the same way. Also, "RepoDump-" prefix is removed from job names.
 */
public class MS011RenameBackupTriggers implements MigrationScript {
    private Logger log = Logger.getLogger(getClass());

    public boolean migrate(SqlUtil db) throws SQLException {
        db.executeUpdateSql(
                "SET REFERENTIAL_INTEGRITY FALSE;" +
                "" +        
                "update QRTZ_TRIGGERS " +
                "set TRIGGER_GROUP = 'RepoJob', " +
                "TRIGGER_NAME = replace(TRIGGER_NAME, 'RepoDump-', '') where " +
                "TRIGGER_GROUP = 'Backup'; " +
                "" +
                "update QRTZ_CRON_TRIGGERS " +
                "set TRIGGER_GROUP = 'RepoJob', " +
                "TRIGGER_NAME = replace(TRIGGER_NAME, 'RepoDump-', '') where " +
                "TRIGGER_GROUP = 'Backup'; " +
                "" +
                "SET REFERENTIAL_INTEGRITY TRUE;");
        return false;    
    }

    public int[] getVersion() {
        return new int[] {3,0,3};
    }
}
