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

import com.collabnet.svnedge.console.NetworkingService 
import grails.test.GrailsUnitTestCase;
import com.collabnet.svnedge.domain.NetworkConfiguration
import com.collabnet.svnedge.console.SecurityService;

class NetworkingServiceTests extends GrailsUnitTestCase {
    def networkingService

    protected void setUp() {
        super.setUp()
        // mock the NetworkConfiguration domain class behavior
        def instanceList = [ new NetworkConfiguration() ]
        mockDomain(NetworkConfiguration, instanceList)
        NetworkConfiguration.metaClass.'static'.list = { instanceList }
        NetworkConfiguration.metaClass.merge = { delegate }
        
        // setup the service for testing
        mockLogging(NetworkingService)
        networkingService = new NetworkingService()
        networkingService.securityService = new SecurityService()
    }

    void tearDown() {
        super.tearDown()
        NetworkConfiguration.metaClass = null
        ["http.proxyHost", "http.proxyPort", "http.proxyUser", "http.proxyPassword"].each {
            System.clearProperty(it)
        }
    }

    void testGetNetworkInterfacesWithIPAddresses() {
        def interfaces = networkingService
            .getNetworkInterfacesWithIPAddresses()
        assertTrue "Should be at least one network interface", 
            !interfaces.isEmpty()
        assertTrue "Last interface should be the loopback", 
            interfaces.get(interfaces.size() - 1).isLoopback()
    }

    void testGetIPv4Addresses() {
        def addresses = networkingService.getIPv4Addresses()
        assertTrue "Should be at least one IP address", 
            !addresses.isEmpty()
        assertTrue "Last address should be the loopback", 
            addresses.get(addresses.size() - 1).isLoopbackAddress()
        assertTrue "Link local addresses should not be included",
            addresses.findAll({it.isLinkLocalAddress()}).isEmpty()
    }

    void testGetInetAddressNetworkInterfaceMap() {
        def addrInterfaceMap = networkingService
            .getInetAddressNetworkInterfaceMap()
        assertTrue "Should be at least one IP address", 
            !addrInterfaceMap.isEmpty()
        for (addrInts in addrInterfaceMap.entrySet()) {
            String addr = addrInts.key
            Collection interfaces = addrInts.value
            if (addr.startsWith("127")) {
                interfaces.each { 
                    assertTrue "Expect lo for loopback addresses", it.startsWith("lo")
                }
            } else if (addr.startsWith("169") || addr.startsWith("fe80")) {
                fail "Link-local address found"
            } else {
                assertFalse "Address expected to have an interface", 
                    interfaces.isEmpty()
            }
        }
    }
    
    void testSetHttpProxy() {
        
        assertNull("there should not be proxy config system props", System.getProperty("http.proxyHost"))
        assertNull("there should not be proxy config system props", System.getProperty("http.proxyPort"))
        assertNull("there should not be proxy config system props", System.getProperty("http.proxyUser"))
        assertNull("there should not be proxy config system props", System.getProperty("http.proxyPassword"))
                 
        NetworkConfiguration nc = NetworkConfiguration.list().last()
        assertNull("there should not be proxy config domain props", nc?.httpProxyHost)
        assertNull("there should not be proxy config domain props", nc?.httpProxyPort)
        assertNull("there should not be proxy config domain props", nc?.httpProxyUsername)
        assertNull("there should not be proxy config domain props", nc?.httpProxyPassword)
        
        String proxyUrl = "http://proxyuser:proxypass@proxy.net:81"
        networkingService.setHttpProxy(proxyUrl)
        
        assertEquals("there *should* now be proxy config system props", "proxy.net", System.getProperty("http.proxyHost"))
        assertEquals("there *should* now be proxy config system props", "81", System.getProperty("http.proxyPort"))
        assertEquals("there *should* now be proxy config system props", "proxyuser", System.getProperty("http.proxyUser"))
        assertEquals("there *should* now be proxy config system props", "proxypass", System.getProperty("http.proxyPassword"))
        
        nc = NetworkConfiguration.list().last()
        assertEquals("there *should* now be proxy config domain props", "proxy.net", nc.httpProxyHost)
        assertEquals("there *should* now be proxy config domain props", 81, nc.httpProxyPort)
        assertEquals("there *should* now be proxy config domain props", "proxyuser", nc.httpProxyUsername)
        assertEquals("there *should* now be proxy config domain props", "proxypass", nc.httpProxyPassword)
        
        assertEquals("The domain class should construct the same proxyurl", proxyUrl, nc.proxyUrl)
    }
}
