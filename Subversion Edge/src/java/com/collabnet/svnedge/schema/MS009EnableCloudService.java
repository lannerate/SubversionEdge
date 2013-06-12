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
package com.collabnet.svnedge.schema;

import org.apache.log4j.Logger;

import java.sql.SQLException;

public class MS009EnableCloudService implements MigrationScript {
    private Logger log = Logger.getLogger(getClass());

    public boolean migrate(SqlUtil db) throws SQLException {
        // insert row, if it does not exist
        db.executeUpdateSql("insert into CLOUD_SERVICES_CONFIGURATION " +
                "(select 1 as ID, 0 as VERSION, '' as DOMAIN, '' as USERNAME," +
                " '' as PASSWORD, true as ENABLED from " +
                "(select 1 as ID, count(*) from CLOUD_SERVICES_CONFIGURATION)" +
                " where ID not in " +
                "(select ID from CLOUD_SERVICES_CONFIGURATION))");
        // for installations where a row already existed
        db.executeUpdateSql("update CLOUD_SERVICES_CONFIGURATION " +
                "set ENABLED = true");
        return false;
    }
    
    public int[] getVersion() {
        return new int[] {3,0,0};
    }
}
