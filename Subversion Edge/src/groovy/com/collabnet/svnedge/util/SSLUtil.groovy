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

package com.collabnet.svnedge.util

import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.SocketFactory
import java.security.KeyStore

/**
 * Utility class for SSL-related functions
 */
class SSLUtil {
    
    private static TrustManager[] trustAllCerts 
    private static SSLContext sslContext 

    /**
     * static constructor
     */
    static {
        
        // Create the all-trusting manager
        TrustManager[] trustAllCerts = [ new X509TrustManager() {
                @Override
                public void checkClientTrusted( final X509Certificate[] chain, final String authType ) {
                }
                @Override
                public void checkServerTrusted( final X509Certificate[] chain, final String authType ) {
                }
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            }].toArray(new TrustManager[1])
        
            
        // Install the all-trusting trust manager
        sslContext = SSLContext.getInstance( "SSL" );
        sslContext.init( null, trustAllCerts, new java.security.SecureRandom() );
    }

    /**
     * Creates a client Socket that will trust all SSL certificates
     * @param host
     * @param port
     * @return an SSL-capable socket that ignores certificate trust problems
     */
    public static Socket createTrustingSocket(String host, int port) {
        return createTrustingSocketFactory().createSocket(host, port)
    }

    /**
     * creates a client socket factory that will trust all SSL certifactes
     * @return a new SocketFactory
     */
    public static SSLSocketFactory createTrustingSocketFactory() {
        // Create an ssl socket factory with all-trusting manager
        return sslContext.getSocketFactory();
    }

    /**
     * creates an SSL context that trusts all certifactes
     * @return a new sslContext
     */
    public static SSLContext createTrustingSSLContext() {
        def sslContext = SSLContext.getInstance( "SSL" );
        sslContext.init( null, trustAllCerts, new java.security.SecureRandom() );
        return sslContext
    }

    /**
     * Fetches the SvnEdge keystore that ships with the application
     * @return the application KeyStore
     */
    public static KeyStore getApplicationKeyStore() {
        File file = new File(ConfigUtil.configuration.svnedge.httpClient.keystore.path)
        FileInputStream is = new FileInputStream(file);
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        String password = ConfigUtil.configuration.svnedge.httpClient.keystore.pass;
        keystore.load(is, password.toCharArray());
        return keystore
    }

    

    
    
}
