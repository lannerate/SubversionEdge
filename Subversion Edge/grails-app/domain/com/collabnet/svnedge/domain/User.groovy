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
package com.collabnet.svnedge.domain

/**
 * User domain class.  Initial revision is mostly grails template code.
 */
class User {
    static transients = ['pass', 'ldapUser']
    static hasMany = [authorities: Role, props: UserProperty,groups:Groups]
    static belongsTo = [Role,Groups]

    private static final String LDAP_PSEUDO_PASSWD = "LDAP_AUTH_ONLY"
    public static final String LDAP_PSEUDO_EMAIL = "null@collab.net"
    private static final String LDAP_USER_DESCRIPTION = "LDAP User"

    /** Username */
    String username
    /** User Real Name*/
    String realUserName
    /** MD5 Password */
    String passwd
    /** enabled */
    boolean enabled = true

    String email

    /** description */
    String description = ''

    /** plain password to create a MD5 password */
    String pass = '[secret]'

    static User newLdapUser(def properties = null) {
        def user = new User(properties)
        user.passwd = LDAP_PSEUDO_PASSWD
        user.email = LDAP_PSEUDO_EMAIL
        user.description = LDAP_USER_DESCRIPTION
        user.enabled = true
        return user
    }
    
    /**
     * determine if given User is from LDAP authentication
     * @param u
     * @return boolean indicating whether User originated in LDAP auth
     */
    boolean isLdapUser() {
        return this.passwd == LDAP_PSEUDO_PASSWD
    }

    static constraints = {
        username(blank: false, unique: true, minSize: 1, maxSize: 31, 
            matches: "[^)(\\|:\"'~^`&\$,<>]*", validator: { val ->
                if (val.indexOf(" ") >= 0) {
                    return "spaces.not.allowed"
                }
        })
        email(email:true, blank:false, validator: { val ->
                if (val.indexOf(" ") >= 0) {
                    return "spaces.not.allowed"
                }
        })
        realUserName(blank: false)
        passwd(blank: false, minSize: 5, maxSize: 255,
            matches: "[^\"]*")
        enabled()
    }
    
    def getPropertiesMap() {
        def m = [:]
        for (UserProperty p in props) {
            m[p.name] = p
        }
        return m
    }
}
