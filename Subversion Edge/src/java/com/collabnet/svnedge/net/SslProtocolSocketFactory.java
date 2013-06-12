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
package com.collabnet.svnedge.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

/**
 * Provides support for managing SSL connections.
 *
 * @author Nathan Hapke
 * @author Rob Elves
 * @author Steffen Pingel
 * @since 2.0
 */
// API-3.0 move to internal package and merge with PollingSslProtocolSocketFactory
public class SslProtocolSocketFactory implements SecureProtocolSocketFactory {

	static SslProtocolSocketFactory factory = null;

	public static SslProtocolSocketFactory getInstance() {
		if (factory == null) {
			factory = new SslProtocolSocketFactory();
		}
		return factory;
	}

	private SSLSocketFactory socketFactory;

	private SslProtocolSocketFactory() {
		try {
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, new TrustManager[] { new TrustAllTrustManager() }, null);
			this.socketFactory = sslContext.getSocketFactory();
		} catch (Exception e) {
			log(0, "Could not initialize SSL context", e);
		}
	}

	private void log(int i, String string, Exception e) {
		System.out.println(string);
		System.out.println(e.getMessage());
	}

	/**
	 * @since 2.3
	 */
	public SSLSocketFactory getSocketFactory() throws IOException {
		if (socketFactory == null) {
			throw new IOException("Could not initialize SSL context");
		}
		return socketFactory;
	}

	public Socket createSocket(String remoteHost, int remotePort) throws IOException, UnknownHostException {
		return getSocketFactory().createSocket(remoteHost, remotePort);
	}

	public Socket createSocket(String remoteHost, int remotePort, InetAddress clientHost, int clientPort)
			throws IOException, UnknownHostException {
		return getSocketFactory().createSocket(remoteHost, remotePort, clientHost, clientPort);
	}

	public Socket createSocket(String remoteHost, int remotePort, InetAddress clientHost, int clientPort,
			HttpConnectionParams params) throws IOException, UnknownHostException, ConnectTimeoutException {
		if (params == null) {
			throw new IllegalArgumentException("Parameters may not be null");
		}

		int timeout = params.getConnectionTimeout();
		if (timeout == 0) {
			return getSocketFactory().createSocket(remoteHost, remotePort, clientHost, clientPort);
		} else {
			Socket socket = getSocketFactory().createSocket();
			socket.bind(new InetSocketAddress(clientHost, clientPort));
			socket.connect(new InetSocketAddress(remoteHost, remotePort), timeout);
			return socket;
		}
	}

	public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException,
			UnknownHostException {
		return getSocketFactory().createSocket(socket, host, port, autoClose);
	}
}
