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

public class MS022AddGroupPeopleTable implements MigrationScript {
    private Logger log = Logger.getLogger(getClass());

    private static final String CREATE_TABLE_SQL =
            "CREATE MEMORY TABLE GROUP_PEOPLE (" +
            "GROUP_ID BIGINT NOT NULL," +
            "USER_ID BIGINT NOT NULL," +
            "PRIMARY KEY(GROUP_ID, USER_ID)," +
            "CONSTRAINT FK_GP_GROUP_ID FOREIGN KEY(GROUP_ID) " +
            "REFERENCES GROUP(ID))";
    
 
    public boolean migrate(SqlUtil db) throws SQLException {
        // insert row, if it does not exist
        db.executeUpdateSql(CREATE_TABLE_SQL);
        db.executeUpdateSql("ALTER TABLE GROUP_PEOPLE ADD CONSTRAINT FK_GP_USER_ID FOREIGN KEY(USER_ID) REFERENCES USER(ID)");
        return false;
    }
    
    public int[] getVersion() {
        return new int[] {3,0,1};
    }
}
