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

public interface MigrationScript {

    /**
     * The logic which will update the schema
     * @param db utility class
     * @return true if further migration requires business logic
     * @throws SQLException
     */
    boolean migrate(SqlUtil db) throws SQLException;

    /**
     * @return A three element array containing major, minor, and revision.
     * May be null, if script will handle determination of previous execution
     */
    int[] getVersion();
}
