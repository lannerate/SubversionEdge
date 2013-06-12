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
package com.collabnet.svnedge.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.rpc.ServiceException;

import org.apache.axis.EngineConfiguration;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.configuration.BasicClientConfig;
import com.collabnet.svnedge.domain.NetworkConfiguration
import org.apache.axis.configuration.SimpleProvider
import org.apache.axis.SimpleTargetedChain
import com.collabnet.svnedge.net.CommonsHTTPProxySender
import org.apache.axis.transport.http.CommonsHTTPSender;

/**
 * The <code>SoapClient</code> class provides helper methods for SOAP.
 */
public class SoapClient {
    /* The SOAP service url */
    private final URL mServiceUrl

    /* The SOAP service name */
    private final String mServiceName

    /* The SOAP service handle */
    private final Service mService

    /* The timeout duration */
    private static final Integer DEFAULT_TIMEOUT = Integer.MAX_VALUE

    /**
     * Constructor with information on the remote SOAP service URL.
     * 
     * @param serviceUrl SOAP service URL.
     * @param nc NetworkConfiguration to use for the connection
     * @throws MalformedURLException Thrown when the specified URL is malfored.
     */
    public SoapClient(String serviceUrl, NetworkConfiguration nc) throws MalformedURLException {
        mServiceUrl = new URL(serviceUrl)
        int slash = serviceUrl.lastIndexOf("/")
        if (slash > 0 && slash < serviceUrl.length() - 2) {
            mServiceName = serviceUrl.substring(slash + 1)
        } else {
            throw new MalformedURLException(serviceUrl)
        }

        final EngineConfiguration config = getEngineConfiguration(nc)
        mService = new Service(config)
    }

    /**
     * Invokes a service method with the specified parameters.
     * 
     * @param methodName Service method name.
     * @param params Service method parameters.
     * @return Return value from the SOAP service call.
     * @throws ServiceException See org.apache.axis.client.Service#createCall.
     * @throws RemoteException  See org.apache.axis.client.Call#invoke
     */
    public def invoke(String methodName, List params) 
        throws ServiceException, RemoteException {

        return invoke(methodName, params, DEFAULT_TIMEOUT)
    }

    /**
     * Invokes a service method with the specified parameters.
     * 
     * @param methodName Service method name.
     * @param params Service method parameters.
     * @param timeout how long before we timeout this connection.
     * @return Return value from the SOAP service call.
     * @throws ServiceException See org.apache.axis.client.Service#createCall.
     * @throws RemoteException  See org.apache.axis.client.Call#invoke
     */
    public def invoke(String methodName, List params, Integer timeout)
        throws ServiceException, RemoteException {

        Call call = (Call) mService.createCall()
        call.setTimeout(timeout)
        call.setTargetEndpointAddress(mServiceUrl)
        call.setOperationName(new QName(mServiceName, methodName))
        return call.invoke(params as Object[])
    }

    /**
     * Utility method to construct an apache axis EngineConfiguration
     * supporting the proxy settings of the NetworkConfiguration
     * @param nc the NetworkConfiguration
     * @return axis EngineConfiguration
     */
    public static def org.apache.axis.EngineConfiguration getEngineConfiguration(NetworkConfiguration nc) {
        SimpleProvider config = new SimpleProvider();
        if (nc?.httpProxyHost) {
            config.deployTransport("http", new SimpleTargetedChain(new CommonsHTTPProxySender(
                    nc.httpProxyHost,
                    nc.httpProxyPort,
                    nc.httpProxyUsername,
                    nc.httpProxyPassword
            )));
            config.deployTransport("https", new SimpleTargetedChain(new CommonsHTTPProxySender(
                    nc.httpProxyHost,
                    nc.httpProxyPort,
                    nc.httpProxyUsername,
                    nc.httpProxyPassword
            )));
        }
        else {
            config.deployTransport("http", new SimpleTargetedChain(new CommonsHTTPSender()));
            config.deployTransport("https", new SimpleTargetedChain(new CommonsHTTPSender()));
        }
		return config;
    }
}
