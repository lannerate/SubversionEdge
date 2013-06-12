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

import org.apache.axis.configuration.BasicClientConfig
import org.apache.axis.EngineConfiguration
import org.apache.axis.client.Service
import org.apache.axis.client.Call
import javax.xml.namespace.QName
import groovyx.net.http.RESTClient
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.auth.AuthScope

/**
 * Groovy script for testing client connectivity via proxy server
 */

soapUrl = "http://cu313.cloud.sp.collab.net/ce-soap/services/ScmListener"
restUrl = "https://api01.codesion.com/1/"

proxyConfig = [ httpProxyHost: "cu182.cloud.sp.collab.net",
        httpProxyPort: "80",
        httpProxyUsername: "proxyuser",
        httpProxyPassword: "proxypass"]

System.setProperty("http.proxyHost", proxyConfig.httpProxyHost)
System.setProperty("http.proxyPort", proxyConfig.httpProxyPort)
System.setProperty("http.proxyUser", proxyConfig.httpProxyUsername)
System.setProperty("http.proxyPassword", proxyConfig.httpProxyPassword)


def standardJava = {

    println "Attempting use of proxy with auth via standard java..."
    // this does not work, 407 auth failure at proxy
    try {
        def t = soapUrl.toURL().text
        println "SUCCESS: Standard java access to '${soapUrl}' succeeded"
    }
    catch (Exception e) {
        println "FAIL: ${e.message}"
    }
}

def axisClient = {
    println "Attempting use of proxy with auth via axis client..."
    def serviceUrl = soapUrl.toURL()
    try {
        EngineConfiguration config = new BasicClientConfig()
        def service = new Service(config)
        Call call = (Call) service.createCall()
        call.setTimeout(2000)
        call.setTargetEndpointAddress(serviceUrl)
        call.setOperationName(new QName("ScmListener", "endpoint"))
        call.invoke([])
    }
    catch (Exception e) {
        if (e.message.contains("No such operation")) {
            println "SUCCESS: received expected axis fault"
        }
        else {
            println "FAIL: ${e.message}"
        }

    }
}

def restClient = {
    println "Attempting use of proxy with auth via rest client..."
    try {
        def t = new RESTClient("https://api01.codesion.com/1/")
        t.setProxy "cu182.cloud.sp.collab.net", 80, "http"
        def c = t.getClient()
        c.getCredentialsProvider().setCredentials(
                new AuthScope("cu182.cloud.sp.collab.net", 80),
                new UsernamePasswordCredentials("proxyuser", "proxypass")
        )
        t.get(path: "/")
    }
    catch (Exception e) {
        if (e.message.contains("peer not authenticated")) {
            println "SUCCESS: received expected SSL fault"
        }
        else {
            println "FAIL: ${e.message}"
        }
    }



//    BASE64Encoder encoder = new BASE64Encoder();
//    String output = encoder.encode("proxyuser:proxypass".getBytes());
//    def headers = t.headers
//    headers['Proxy-Authorization'] = "Basic ${output}"
//    headers['Proxy-Connection'] = "Keep-Alive"
//    t.headers = headers

}


standardJava()
axisClient()
restClient()