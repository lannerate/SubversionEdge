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
package com.collabnet.svnedge.console

/**
 * The abstract service provided shared method calls for the grails services.
 * 
 * @author Marcello de Sales (mdesales@collab.net)
 *
 */
abstract class AbstractSvnEdgeService {

    def grailsApplication

    /**
     * Gets an i18n message from the messages.properties file without providing
     * parameters using the default locale.
     * @param key is the key in the messages.properties file.
     * @return the message related to the key in the messages.properties file
     * using the default locale.
      */
    protected def getMessage(String key) {
        return this.getMessage(key, null)
    }

    /**
     * Gets an i18n message from the messages.properties file with the default
     * locale used by the JVM.
     * @param key is the key in the messages.properties file.
     * @param params is the list of parameters to provide the i18n.
     * @return the message related to the key in the messages.properties file
     * using the default locale from the JVM.
     */
    protected def getMessage(String key, List params) {
        return this.getMessage(key, params, Locale.getDefault())
    }

    /**
    * Gets an i18n message from the messages.properties file with the default
    * locale used by the JVM.
    * @param key is the key in the messages.properties file.
    * @param params is the list of parameters to provide the i18n.
    * @return the message related to the key in the messages.properties file
    * using the default locale from the JVM.
    */
   protected def getMessage(String key, Locale locale) {
       return this.getMessage(key, null, locale)
   }

   /**
    * Gets an i18n message from the messages.properties file without providing
    * parameters using the default locale.
    * @param key is the key in the messages.properties file.
    * @param params is the list of parameters to provide the i18n.
    * @return the message related to the key in the messages.properties file
    * using the default locale.
    */
   protected def getMessage(String key, List params, Locale locale) {
       if (!locale) {
           locale = Locale.getDefault()
       }
       def appCtx = grailsApplication.getMainContext()
       return appCtx.getMessage(key, params as Object[], locale)
   }
   
   /**
    * Writes a localized message to the given stream followed by EOL char
    * @param key is the key in the messages.properties file.
    * @param os output stream
    * @param locale optional indicator to specify message language
    */
   protected void println(String key, OutputStream os, Locale locale = null) {
       if (os) {
           os << getMessage(key, locale) + "\n"
       }
   }

   /**
    * Writes a localized message to the given stream followed by EOL char
    * @param key is the key in the messages.properties file.
    * @param params is the list of parameters to provide the i18n.
    * @param os output stream
    * @param locale optional indicator to specify message language
    */
   protected void println(String key, List<String> params, OutputStream os, 
                          Locale locale = null) {
       if (os) {
           os << getMessage(key, params, locale) + "\n"
       }
   }



}
