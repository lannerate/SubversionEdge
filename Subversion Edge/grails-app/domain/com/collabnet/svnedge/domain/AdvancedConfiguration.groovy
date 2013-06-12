/*
 * CollabNet Subversion Edge
 * Copyright (C) 2013, CollabNet Inc. All rights reserved.
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
package com.collabnet.svnedge.domain

/**
 * Defines properties of the svn server that most users could leave at default value. 
 * We expect there to be only one AdvancedConfiguration defined.
 */
class AdvancedConfiguration {
    
    boolean autoVersioning = false
    boolean listParentPath = true
    int compressionLevel = 5
    String hooksEnv
    boolean useUtf8 = false
    boolean pathAuthz = true
    boolean strictAuthz = false
    int inMemoryCacheSize = 16
    boolean cacheFullTexts = false
    boolean cacheTextDeltas = false
    boolean cacheRevProps = false
    boolean allowBulkUpdates = true
    boolean preferBulkUpdates = false
    Integer ldapConnectionPoolTtl = null
    Integer ldapTimeout = null
    
    static final String DEFAULT_SVN_REALM = 'CollabNet Subversion Repository'
    String svnRealm = DEFAULT_SVN_REALM
    static final String DEFAULT_ACCESS_LOG_FORMAT = '%h %l %u %t \\"%r\\" %>s %b %T'
    String accessLogFormat = DEFAULT_ACCESS_LOG_FORMAT
    static final String DEFAULT_SVN_LOG_FORMAT = '%t %u %{SVN-REPOS-NAME}e %{SVN-ACTION}e %T'
    String svnLogFormat = DEFAULT_SVN_LOG_FORMAT

    static final def EDGE_DEFAULT_VALUE_MAP = [
            autoVersioning: false,
            listParentPath: true,
            compressionLevel: 5,
            hooksEnv: null,
            useUtf8: false,
            pathAuthz: true,
            strictAuthz: false,
            inMemoryCacheSize: 16,
            cacheFullTexts: false,
            cacheTextDeltas: false,
            cacheRevProps: false,
            allowBulkUpdates: true,
            preferBulkUpdates: false,
            svnRealm: DEFAULT_SVN_REALM,
            accessLogFormat: DEFAULT_ACCESS_LOG_FORMAT,
            svnLogFormat: DEFAULT_SVN_LOG_FORMAT,
            ldapConnectionPoolTtl: null,
            ldapTimeout: null
        ]
    
    static constraints = {
        hooksEnv(nullable: true)
        svnRealm(nullable: true)
        svnLogFormat(nullable: true)
        accessLogFormat(nullable: true)
        compressionLevel(min:0, max: 9)
        inMemoryCacheSize(min:0)
        ldapConnectionPoolTtl(nullable: true)
        ldapTimeout(nullable: true)
    }
    
    void resetToDefaults() {
        this.properties = EDGE_DEFAULT_VALUE_MAP
    }
     
    // GORM event
    def beforeInsert() {
        handleDefaults()
    }
    // GORM event
    def beforeUpdate() {
        handleDefaults()
    }
    
    private void handleDefaults() {
        if (svnRealm == DEFAULT_SVN_REALM) {
            svnRealm = null
        }
        if (accessLogFormat == DEFAULT_ACCESS_LOG_FORMAT) {
            accessLogFormat = null
        }
        if (svnLogFormat == DEFAULT_SVN_LOG_FORMAT) {
            svnLogFormat = null
        }
    }

    // not a GORM event in 1.3.x
    def afterLoad() {
        if (!svnRealm) {
            svnRealm = DEFAULT_SVN_REALM
        }
        if (!accessLogFormat) {
            accessLogFormat = DEFAULT_ACCESS_LOG_FORMAT
        }
        if (!svnLogFormat) {
            svnLogFormat = DEFAULT_SVN_LOG_FORMAT
        }
    }
 
    public static AdvancedConfiguration getConfig() {
        def config = AdvancedConfiguration.get(1)
        if (!config) {
            config = new AdvancedConfiguration()
            config.save()
        }
        config.afterLoad()
        return config
    }
}