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
package com.collabnet.svnedge.services

import java.util.Random
import com.collabnet.svnedge.console.SecurityService 
import grails.test.GrailsUnitTestCase
import org.junit.Test


class SecurityServiceTests extends GrailsUnitTestCase {

    def securityService
    private Random rndm = new Random()

    protected void setUp() {
        super.setUp()

        // mock the service dependencies
        //mockLogging (SecurityService, true)
        securityService = new SecurityService()
    }

    protected void tearDown() {
        super.tearDown()
    }


    void testGeneratePassword() {
        for (int i = 0; i < 100; i++) {
            int min = rndm.nextInt(25) + 1
            int max = rndm.nextInt(200) + min
            String password = securityService.generatePassword(min, max)
            println "Generated password: " + password
            assertTrue "Password is too short, min=" + min + " actual=" +
                password.length() + " password=" + password, 
                min <= password.length()
            assertTrue "Password is too long, max=" + max + " actual=" +
                password.length() + " password=" + password, 
                max >= password.length()
        }
    }

    void testMaxLessThanMin() {
        try {
            securityService.generatePassword(10, 1)
            fail("Minimum password length was greater than maximum.")
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
    
    void testEncryptDecrypt() {
        
        String plainText = "Test Input String"
        String cipherText = securityService.encrypt(plainText)
        log.info("The plaintext and cipher text are: ${plainText}, ${cipherText}")
        
        assertNotSame("Plain text and cipher text must differ", plainText, cipherText)
        
        String decrypted = securityService.decrypt(cipherText)
        log.info("The cipher text and decrypted text are: ${cipherText}, ${decrypted}")
        
        assertEquals("Plain text and decrypted should be equal", plainText, decrypted)
                
    }

    @Test(expected = IllegalArgumentException.class)
    void testEncryptBadInput() {

        String plainText = null
        String encrypted = securityService.encrypt(plainText)

    }

    @Test(expected = IllegalArgumentException.class)
    void testDecryptBadInput() {

        String cipherText = null
        String clearText = securityService.decrypt(cipherText)

    }
    
}
