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

import java.security.SecureRandom
import javax.crypto.Cipher
import sun.misc.BASE64Encoder
import sun.misc.BASE64Decoder
import java.security.Key
import javax.crypto.spec.DESKeySpec
import javax.crypto.SecretKeyFactory
import javax.crypto.SecretKey

class SecurityService {

    boolean transactional = true
    SecureRandom randomNumGen = new SecureRandom()
    Key key

    public SecurityService() {

        // todo: needs a better persistent key
        byte[] desKeyData = [ 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08 ] as byte[]
        DESKeySpec desKeySpec = new DESKeySpec(desKeyData)
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES")
        key = keyFactory.generateSecret(desKeySpec)

    }

    String generatePassword(int minPassLength, int maxPassLength) {
        int variation = maxPassLength - minPassLength
        if (variation < 0) {
            throw new IllegalArgumentException("Maximum password length " +
                "must be greater than or equal to the minimum length.")
        }
        int passLength = minPassLength
        if (variation > 0) {
            passLength += randomNumGen.nextInt(variation)
        }
        StringBuilder sb = new StringBuilder(passLength)
        for (int i = 0; i < passLength; i++) {
            // printable ascii characters (or those between '!' and '~')
            sb.append((char) (((int)'!') + randomNumGen.nextInt(93)))
        }
        sb.toString()
    }

    /**
     * generates a random password using letters, numbers, and underscore only
     * @param length number of characters for the password
     * @return the random password
     */
    String generateAlphaNumericPassword(int length) {
        def pool = ['a'..'z','A'..'Z',0..9,'_'].flatten()
        Random rand = new Random(System.currentTimeMillis())

        def randomString = (1..length).collect { pool[rand.nextInt(pool.size())] }
        return randomString.join()
    }

    /**
     * Encrypts and base-64 encodes a clear-text input
     * @param input
     * @return encrypted
     * @throws IllegalArgumentException for null input
     */
    public String encrypt(String input) throws IllegalArgumentException {

        if (!input) {
            throw new IllegalArgumentException("The input parameter cannot be null")
        }

        // Get a cipher object.
        Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);

        // Gets the raw bytes to encrypt, UTF8 is needed for
        // having a standard character set
        byte[] stringBytes = input.getBytes("UTF8");

        // encrypt using the cypher
        byte[] raw = cipher.doFinal(stringBytes);

        // converts to base64 for easier display.
        BASE64Encoder encoder = new BASE64Encoder();
        String encodedOutput = encoder.encode(raw);

        return encodedOutput;
    }

    /**
     * Decrypts an encrypted and base-64 encoded input (created by <code>encrypt()</code> method)
     * @param encrypted text
     * @return clear text
     * @throws IllegalArgumentException for null input
     */
    public String decrypt(String encrypted) throws IllegalArgumentException {

        if (!encrypted) {
            throw new IllegalArgumentException("The input parameter cannot be null")
        }

        // Get a cipher object.
        Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);

        //decode the BASE64 coded message
        BASE64Decoder decoder = new BASE64Decoder();
        byte[] raw = decoder.decodeBuffer(encrypted);

        //decode the message
        byte[] stringBytes = cipher.doFinal(raw);

        //converts the decoded message to a String
        String clear = new String(stringBytes, "UTF8");
        return clear;
    }
}
