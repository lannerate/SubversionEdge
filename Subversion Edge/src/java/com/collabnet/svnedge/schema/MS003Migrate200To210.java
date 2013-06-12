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

import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;

public class MS003Migrate200To210 implements MigrationScript {
    private Logger log = Logger.getLogger(getClass());

    public boolean migrate(SqlUtil db) throws SQLException {
        
        List<String> sql210 = db.loadSql("MS003_quartz.sql");
        for (String sql : sql210) {
            db.executeUpdate(sql);
        }
        sql210 = db.loadSql("MS003_migrate_200_to_210.sql");
        for (String sql : sql210) {
            db.executeUpdate(sql);
        }
        return true;
    }

    public int[] getVersion() {
        return new int[] {2,1,0};
    }
}
