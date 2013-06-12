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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class SqlUtil {

    private Logger log = Logger.getLogger(getClass());
    private Connection conn;
    private Statement stmt;
    
    SqlUtil(Connection conn) {
        this.conn = conn;
    }
    
    private Statement getStatement() throws SQLException {
        if (stmt != null) {
            close(stmt);
        }
        stmt = conn.createStatement();
        return stmt;
    }
    
    ResultSet executeQuery(String sql) throws SQLException {
        log.debug("Executing query sql: " + sql);
        Statement stmt = getStatement();
        try {
            ResultSet rs = stmt.executeQuery(sql);
            return rs;

        } catch (SQLException e) {
            close(stmt);
            throw e;
        }
    }
    
    private Pattern CREATE_TABLE_REGEX = 
        Pattern.compile("^create\\s+\\w*\\s*table\\s+(\\w+)", 
                        Pattern.CASE_INSENSITIVE);

    int executeUpdate(String sql) throws SQLException {
        int result = 0;
        Matcher m = CREATE_TABLE_REGEX.matcher(sql);
        if (m.find()) {
            String tableName = m.group(1);
            if (tableMissing(tableName)) {
                result = executeUpdateSql(sql);
            } else {
                log.info("Create table SQL skipped as table exists: " + sql);
                result = -1;
            }
        } else {
            result = executeUpdateSql(sql);
        }
        return result;
    }

    int executeUpdateSql(String sql) throws SQLException {
        log.debug("Executing update sql: " + sql);
        Statement stmt = getStatement();
        try {
            return stmt.executeUpdate(sql);

        } catch (SQLException e) {
            close(stmt);
            throw e;
        }
    }
    
    void close(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                log.warn("Failed to close jdbc statement", e);
            }
        }
    }

    boolean tableMissing(String tableName) throws SQLException {
        DatabaseMetaData dbmd = conn.getMetaData();
        ResultSet tmd = dbmd.getTables(null, null, tableName, null);
        return !tmd.next();
    }
    
    List<String> getTableColumns(String tableName) throws SQLException {
        DatabaseMetaData dbmd = conn.getMetaData();
        ResultSet cmd = dbmd.getColumns(null, null, tableName, null);
        List<String> tableCols = new ArrayList<String>();
        while (cmd.next()) {
            tableCols.add(cmd.getString("COLUMN_NAME"));
        }
        return tableCols;
    }
    
    boolean isSchemaCurrent(int[] version) throws SQLException {
        if (null == version) {
            // script will take care of sanity check
            return false;
        }
        if (tableMissing("SCHEMA_VERSION")) {
            return false;
        }
        int major = version[0];
        int minor = version[1];
        int revision = version[2];
        ResultSet rs = executeQuery(
            "select count(*) from SCHEMA_VERSION where MAJOR=" + major + 
            " and MINOR=" + minor + " and REVISION >= " + revision);
        return rs.next() && rs.getInt(1) > 0;
    }

    void updateSchemaVersion(int[] version, String desc) throws SQLException {
        if (null == version) {
            return;
        }
        int major = version[0];
        int minor = version[1];
        int revision = version[2];
        String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        executeUpdateSql("insert into SCHEMA_VERSION " + 
            "(VERSION, DATE_CREATED, DESCRIPTION, MAJOR, MINOR, REVISION) " +
            "values (0, '" + ts + "', '" + desc + "', " + 
            major + ", " + minor + ", " + revision + ")");
    }        

    
    List<String> loadSql(String filename) {
        List<String> sql = new ArrayList<String>();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream(filename), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String s;
            while ((s = in.readLine()) != null) {
                s = s.trim();
                if (s.endsWith(";")) {
                    sql.add(sb.append(' ').append(s)
                        .substring(1, sb.length() - 1));
                    sb = new StringBuilder();
                } else if (s.length() > 0 && !s.startsWith("//")) {
                    sb.append(' ').append(s);
                }
            }
            
        } catch (UnsupportedEncodingException e) {
            log.warn("UTF-8 encoding should always be supported.");
        } catch (IOException e) {
            log.error("Error reading" + filename + ". Application may not function!", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    log.warn("Unable to close " + filename, e);
                }
            }
        }
        return sql;
    }

    public Connection getConnection() {
        return conn;
    }
}
