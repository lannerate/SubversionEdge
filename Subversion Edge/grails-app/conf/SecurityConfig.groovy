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
security {
    
    // see DefaultSecurityConfig.groovy for all settable/overridable properties
    
    active = true

    loginUserDomainClass = "com.collabnet.svnedge.domain.User"
    authorityDomainClass = "com.collabnet.svnedge.domain.Role"

    useRequestMapDomainClass = false
    useControllerAnnotations = true
   
    algorithm = "MD5"
    encodeHashAsBase64 = false
 
    providerNames = ['csvnAuthenticationProvider']

    filterInvocationDefinitionSourceMap = [
        '/api/**': 'noHttpSessionContextIntegrationFilter,' +
                   'basicProcessingFilter,' +
                   'anonymousProcessingFilter,' +
                   'basicExceptionTranslationFilter,' +
                   'filterInvocationInterceptor',
        '/**': 'httpSessionContextIntegrationFilter,' +
               'logoutFilter,' +
               'authenticationProcessingFilter,' +
               'anonymousProcessingFilter,' +
               'exceptionTranslationFilter,' +
               'filterInvocationInterceptor'
     ]
}
