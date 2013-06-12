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

import com.collabnet.svnedge.domain.MonitoringConfiguration
import com.collabnet.svnedge.domain.NetworkConfiguration

/**
 * The networking service is responsible for the network layer of the app. It
 * is responsible for network interfaces, ip addresses, etc. 
 *
 * @author Marcello de Sales (mdesales@collab.net)
 */
public class NetworkingService extends AbstractSvnEdgeService {

    boolean transactional = false

    def operatingSystemService
    def securityService

    /**
     * The default network interface is one that is not the loopback interface
     * in case there is network connectivity. It is the one selected at 
     * bootstrap, selected from the OS. If the user has selected the value on
     * the database, then this is the one.
     */
    def selectedInterface
    /**
     * The list of all network interfaces.
     */
    def networkInterfacesWithIP
    /**
     * The hostname of the server as defined on the environment variables
     * HOSTNAME, COMPUTERNAME. If none is defined, then "localhost" is used.
     */
    def hostname
    /**
     * The value of the environmental variable HTTP_PROXY in case one is
     * set. The environment variable in its lower-case value is also considered.
     */
    def httpProxy

    def bootStrap = {
        log.info("########## Networking Service ##########")
        MonitoringConfiguration config = MonitoringConfiguration.getConfig()
        if (!config) {
            config = new MonitoringConfiguration(
                    netInterface: this.selectedInterface.name,
                    ipAddress: this.ipAddress.hostAddress,
                    networkEnabled: true, repoDiskEnabled: true,
                    frequency: MonitoringConfiguration.Frequency.HALF_HOUR,
                    repoDiskFrequencyHours: 1)
            config.save()
            log.info "Saved new monitoring config"
        }
        if (config?.networkEnabled && config.netInterface) {
            setSelectedInterface(config.netInterface)
        }
        log.info("# Network Interface: ${this.selectedInterface.name}")
        log.info("# IP Address: ${getIpAddress().hostAddress}")
        log.info("# Hostname: ${getHostname()}")
        if (this.httpProxy) {
            log.info("# HTTP PROXY: ${httpProxy}")
        }
        log.info("########################################")
                  
        // set the proxy configuration for the VM if needed.
        // first use persistent setting from console
        // and fallback to environment. If there is no 
        // console config, but we do have an environment var, 
        // we will parse that into the db for use 
        // in the UI.
        NetworkConfiguration nc = getNetworkConfiguration()
        String envHttpProxy = getHttpProxyFromEnvironment()
       
        if (nc) {
            setHttpProxy(nc.proxyUrl)
        }
        else if (envHttpProxy) {
            setHttpProxy(envHttpProxy)
        }
    }

    String getHostname() {
        if (!this.hostname) {
            this.hostname = System.getenv("HOSTNAME") ? System.getenv("HOSTNAME") :
                System.getenv("COMPUTERNAME")
            if (!this.hostname || this.hostname.indexOf('.') < 0) {
                this.hostname = getIpAddress().getHostName()
            }
        }
        return this.hostname
    }

    /**
     * returns the http proxy configuration for the VM using first the internal NetworkConfiguration,
     * then the environment var
     * @return
     */
    String getHttpProxy() {
        return getNetworkConfiguration()?.proxyUrl ?: getHttpProxyFromEnvironment()
    }
    
    String getHttpProxyFromEnvironment() {
        return httpProxy = System.getenv("http_proxy") ?: System.getenv("HTTP_PROXY")
    }

    /**
     * Sets the proxy configuration for the VM, with persistence.
     * @param proxyUrl
     * @return
     */
    void setHttpProxy(String proxyUrl) {
        // clear proxy settings with empty or zero-len string
        if (!proxyUrl) {
            removeNetworkConfiguration()
        }
        else {
            // else save the proxy url to our db representation
            def netCfg = getNetworkConfiguration() ?: new NetworkConfiguration()
            if (netCfg.proxyUrl != proxyUrl) {
                netCfg.proxyUrl = proxyUrl
                saveNetworkConfiguration(netCfg)
            }
        }
        // set the vm properties
        setHttpProxySystemProps()
    }

    /**
     * using the NetworkConfiguration, set the java vm props
     */
    void setHttpProxySystemProps() {
        def netCfg = getNetworkConfiguration()
        if (!netCfg) {
            ["http.proxyHost", "http.proxyPort", "http.proxyUser", "http.proxyPassword"].each {
                System.clearProperty(it)
            }
        }
        else {
            System.setProperty("http.proxyHost", netCfg.httpProxyHost)
            System.setProperty("http.proxyPort", "${netCfg.httpProxyPort}")
            if (netCfg.httpProxyUsername) {
                System.setProperty("http.proxyUser", netCfg.httpProxyUsername)
            }
            if (netCfg.httpProxyPassword) {
                System.setProperty("http.proxyPassword", netCfg.httpProxyPassword)
            }
        }
    }

    /**
     * Obtain the NetworkConfiguration instance, with proxy password decrypted
     * @return pseudo singleton instance of NetworkConfiguration or null
     */
    def getNetworkConfiguration() {
        def networkConfigs = NetworkConfiguration.list()
        if (networkConfigs) {
            def config = networkConfigs.last()
            if (config.httpProxyPassword) {
                try {
                    // if encrypted this will succeed
                    config.httpProxyPassword = securityService.decrypt(config.httpProxyPassword)
                    config.discard()
                }
                catch (Exception e) {
                    // on exception, assume that the password is not encrypted
                    log.warn("NetworkConfiguration proxy password is stored in clear text")
                }
            }
            return config
        }
        else {
            return null
        }
    }

    /**
     * save the NetworkConfiguration, with proxy password encrypted
     * @param config
     * @return boolean indicating success
     */
    def saveNetworkConfiguration(NetworkConfiguration config) {
        // encrypt the password if needed
        if (config.httpProxyPassword) {
            try {
                // if already encrypted this will succeed so no additional
                // encryption required
                securityService.decrypt(config.httpProxyPassword)
            }
            catch (Exception e) {
                // on exception, assume it's not encrypted and encrypt
                config.httpProxyPassword = securityService.encrypt(config.httpProxyPassword)
            }
        }
        // delete any stray rows
        def toDelete = NetworkConfiguration.list()
        if (toDelete) {
            toDelete.findAll { it.id != config.id}*.delete(flush: true)
        }
        
        // save this instance 
        config = config.merge()
        def saved = config.save(flush: true)
        
        // set the java props
        if (saved) {
            setHttpProxySystemProps()
        }
        // indicate persistence success
        return saved
    }

    /**
     * delete the proxy configuration 
     * @return
     */
    def removeNetworkConfiguration() {
        if (getNetworkConfiguration()) {
            getNetworkConfiguration().delete(flush: true)
        }
    }
        
       
    /**
     * @return the IPv4 (preferred, IPv6 if necessary) version assigned to the default 
     * interface. 
     */
    def getIpAddress() {
        def ipAddresses = this.selectedInterface.getInetAddresses().toList()
        InetAddress ip = ipAddresses.find { it instanceof Inet4Address }        
        if (!ip) {
            ip = ipAddresses.find { it instanceof Inet6Address }
        }

        if (!ip) {
            // this shouldn't happen, but added as protection against 
            // downstream NPEs
            ip = InetAddress.getByAddress("unknown", [0,0,0,0] as byte[])
        }
        return ip
    }

    /**
     * @return the currently selected interface.
     */
    def getSelectedInterface() {
        if (!this.selectedInterface) {
            this.selectedInterface = this.getDefaultNetworkInterface()
        }
        return this.selectedInterface
    }

    /**
     * Sets the selected interface
     * @param interfaceName the interface name
     * @return void
     */
    def setSelectedInterface(String interfaceName) {
        if (interfaceName && (interfaceName != selectedInterface?.name)) {
            selectedInterface = getNetworkInterfacesWithIPAddresses().find() { it.name == interfaceName }
        }
    }

    /**
     * @return the default {@link NetworkInterface} that is not the loopback 
     * address. In case no IP address is assigned, then the loopback is used.
     */
    def getDefaultNetworkInterface() {
        def ifs = this.networkInterfacesWithIP ?: 
            this.getNetworkInterfacesWithIPAddresses()
        def defaultIf
        if (ifs.size() > 1) {
            ifs.each { NetworkInterface netIf ->
                if (!netIf.name.startsWith("lo")) {
                    defaultIf = netIf
                    return
                }
            }
        }
        return defaultIf ?: ifs[0]
    }
    
    /**
     * Returns network interfaces which have been assigned site-local
     * or global IP addresses along with the loopback interface.  
     * Interfaces with only a link-local address are not included.
     */
    def getNetworkInterfacesWithIPAddresses() {
        if (!networkInterfacesWithIP) {

        def networkInterfaces = Collections
            .list(NetworkInterface.getNetworkInterfaces())
        def niWithIP = []
        for (ni in networkInterfaces) {
            def addresses = Collections.list(ni.getInetAddresses())
            if (!addresses.isEmpty()) {
                boolean add = false;
                for (addr in addresses) {
                    if (!addr.isLinkLocalAddress()) {
                        add = true
                    }
                }
                if (add) {
                    niWithIP << ni
                }
            }
        }
        networkInterfacesWithIP = niWithIP.sort { a, b -> 
            a.isLoopback() && !b.isLoopback() ? 1 : 
            !a.isLoopback() && b.isLoopback() ? -1 : a.name <=> b.name }
        }
        return networkInterfacesWithIP
    }

    /**
     * Retrieves the hostnames which can be found for this host.  IP addresses
     * without a separate hostname (such as via DNS) are not included.
     * @return list of string hostnames
     */
    def availableHostnames() {
        def hostnames = []
        getNetworkInterfacesWithIPAddresses().each { ni ->
            Collections.list(ni.getInetAddresses()).each { addr ->
                if (!addr.isAnyLocalAddress() && !addr.isLinkLocalAddress() && 
                        !addr.isLoopbackAddress()) {
                    def hostname = addr.canonicalHostName
                    if (hostname != addr.hostAddress) {
                        hostnames << hostname
                    } 
                }
            }
        }
        return hostnames
    }
    
    def getInetAddressNetworkInterfaceMap() {
        def addrInterfaceMap = new HashMap()
        def networkInterfaces = Collections
            .list(NetworkInterface.getNetworkInterfaces())
        for (ni in networkInterfaces) {
            def addressesForNI = Collections.list(ni.getInetAddresses())
            for (addr in addressesForNI) {
                if (!addr.isLinkLocalAddress()) {
                    def iface = addrInterfaceMap[addr.hostAddress]
                    if (iface) {
                        iface << ni.name
                    } else {
                        iface = [ni.name]
                        addrInterfaceMap[addr.hostAddress] = iface
                    }
                }
            }
        }
        addrInterfaceMap
    }

    /**
     * Returns useful IPv4 addresses assigned to network interfaces.
     * Link-local addresses (starting with 169) are not returned.
     * Loopback address will be at the end of the list.
     */
    def getIPv4Addresses() {
        getAddresses { it instanceof Inet4Address &&
                       !it.isLinkLocalAddress() }
    }

    /**
     * @return useful IPv4 addresses assigned to network interfaces.
     * Link-local addresses (starting with fe80) are not returned.
     * Loopback address will be at the end of the list.
     */
    def getIPv6Addresses() {
        getAddresses { it instanceof Inet6Address &&
                       !it.isLinkLocalAddress() }
    }

    private def getAddresses(Closure c) {
        def inetAddresses = []
        def loopback = null
        def networkInterfaces = Collections
            .list(NetworkInterface.getNetworkInterfaces())
        for (ni in networkInterfaces) {
            def addressesForNI = Collections.list(ni.getInetAddresses())
            for (addr in addressesForNI) {
                if (c(addr)) {
                    if (addr.isLoopbackAddress()) {
                        loopback = addr
                    } else {
                        inetAddresses << addr
                    }
                }
            }
        }
        if (loopback) {
            inetAddresses << loopback
        }
        inetAddresses
    }

    /**
     * @return the network statistics for the currently selected network card.
     */
    def getNetworkInterfaceStatistics() {
        return this.getNetworkInterfaceStatistics(this.selectedInterface.name)
    }

    /**
     * @param interfaceName the given network interface name e.g.: eth0
     * @return the network statistics for the given network interface
     */
    def getNetworkInterfaceStatistics(interfaceName) {
        if (interfaceName) {
            return operatingSystemService.sigar.getNetInterfaceStat(
                interfaceName)
        } else {
            throw IllegalArgumentException("The interface name must be " +
                "provided")
        }
    }

    /**
     * Formats the throughput for the user.
     * @param rateIn is the frequency of bytes received.
     * @param timeIn is the time interval of reception.
     * @param rateOut is the frequency of bytes transmitted.
     * @param timeOut is the time interval of transmission.
     * @return a human-readable format of the throughput data.
     */
    public String formatThroughput(throughputData, locale) {
        def rateIn = throughputData[0]
        def timeIn = throughputData[1]
        def rateOut = throughputData[2]
        def timeOut = throughputData[3]
        def throughPutTxt = ""
        if (rateIn == null) {
            return getMessage("statistics.throughput.in.noData", locale)
        } else {
            def bytes = OperatingSystemService.formatBytes(rateIn)
            throughPutTxt = getMessage("statistics.throughput.in.bytes",
                [bytes], locale)
            if (timeIn) {
                def min = OperatingSystemService.formatMinutes(timeIn)
                throughPutTxt += " (" + 
                    getMessage("statistics.throughput.overMinutes", [min],
                        locale) + ")"
            }
        }
        throughPutTxt += "; "
        if (rateOut == null) {
            throughPutTxt += getMessage("statistics.throughput.out.noData",
                    locale)
        } else {
            def bytes = OperatingSystemService.formatBytes(rateOut)
            throughPutTxt += getMessage("statistics.throughput.out.bytes",
                [bytes], locale)
            if (timeIn) {
                def min = OperatingSystemService.formatMinutes(timeOut)
                throughPutTxt += " (" +
                    getMessage("statistics.throughput.overMinutes", [min],
                        locale) + ")"
            }
        }
        return throughPutTxt
    }
}
