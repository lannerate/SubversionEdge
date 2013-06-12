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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.codehaus.groovy.grails.commons.GrailsApplication;

/**
 * Spring bean to handle database creation and migration
 */
public class DatabaseInitializer implements InitializingBean {

    private Logger log = Logger.getLogger(getClass());
    private GrailsApplication application;
    private Connection conn = null;
    
    public void afterPropertiesSet() {
        log.debug("DatabaseInitializer...");
        Properties config = application.getConfig().toProperties();
        String url = config.getProperty("dataSource.url");
        String driver = config.getProperty("dataSource.driverClassName");
        String username = config.getProperty("dataSource.username");
        String password = config.getProperty("dataSource.password");
        try {
            Class.forName(driver);
            conn = DriverManager.getConnection(url, username, password);
            SqlUtil db = new SqlUtil(conn);
            
            for (MigrationScript script : getScripts()) {
                String scriptName = script.getClass().getSimpleName();
                int[] version = script.getVersion();
                if (db.isSchemaCurrent(version)) {
                    log.debug("Migration " + scriptName + 
                        " was previously applied.");
                } else {
                    log.info("Migrating schema using " + scriptName);
                    if (!script.migrate(db)) {
                        db.updateSchemaVersion(version, scriptName);
                    } else {
                        log.debug("Migration " + scriptName + 
                            " has more steps which require business logic to" +
                            " be run during bootstrap.");
                    }
                }
            }
            
        } catch (SQLException e) {
            log.error("Failed to initialize the database. Application is unlikely to work!", e);
        } catch (ClassNotFoundException e) {
            log.error("Failed to initialize the database! Application is unlikely to work!", e);
        } finally {
            if (conn != null) {
                try {
                    conn.commit();
                    conn.close();
                } catch (SQLException e) {
                    log.error("Ignoring exception closing connection", e);
                }
            }
        }
    }

    private MigrationScript[] getScripts() {
        // TODO genericise this
        return new MigrationScript[] {
            new MS001CreateDatabase(),
            new MS002MigrateTo200(),
            new MS003Migrate200To210(),
            new MS004RepoTemplate(),
            new MS005ProxySupport(),
            new MS006DisableCloudService(),
            new MS007AddEmailConfig(),
            new MS008ForceUsernameCase(),
            new MS009EnableCloudService(),
            new MS010AddUserPropertyTable(),
            new MS011RenameBackupTriggers(),
            new MS012AddSvnBasePath(),
            new MS013AddRepoSyncToAddress(),
            new MS014AddMonitorConfiguration(),
            new MS015AddReplicaCommandRetries(),
            new MS016AddGettingStartedWizard(),
            new MS017AddRepositoryVerifyOk(),
            new MS018ServerDropUseHttpV2(),
            new MS019AddLogConfiguration(),
            new MS020AddAdvancedConfiguration(),
           // new MS021AddGroupTable(),
           // new MS022AddGroupPeopleTable(),
        };
    }

    public void setApplication(GrailsApplication app) {
        this.application = app;
    }
}
