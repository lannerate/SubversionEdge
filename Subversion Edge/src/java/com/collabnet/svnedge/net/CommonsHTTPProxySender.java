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

import java.net.URL;

import org.apache.axis.MessageContext;
import org.apache.axis.transport.http.CommonsHTTPSender;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;

/**
 * This subclass of the Axis CommonsHTTPSender provides proxy configuration
 * for the underlying Http Client
 */
public class CommonsHTTPProxySender extends CommonsHTTPSender {
	private String proxyHost;
	private Integer proxyPort;
	private String proxyUser;
	private String proxyPassword;
	private static HttpClient httpClient;

	private static final long serialVersionUID = 1L;

	private static final int HTTP_PORT = 80;

	private static final int HTTPS_PORT = 443;

	public CommonsHTTPProxySender(String proxyHost, Integer proxyPort, String username, String password) {
		super();
		this.proxyHost = proxyHost;
		this.proxyPort = proxyPort;
        this.proxyUser = username;
        this.proxyPassword = password;
	}

	@Override
    protected HostConfiguration getHostConfiguration(HttpClient client, MessageContext context, URL url) {
        client.getHostConfiguration().setProxy(proxyHost, proxyPort);
        if (this.proxyUser != null) {
            client.getState().setProxyCredentials(
                    new AuthScope(proxyHost, proxyPort),
                    new UsernamePasswordCredentials(proxyUser, proxyPassword)
            );
        }

        setupHttpClient(client, url.toString());
        // This needs to be set to 1.0 otherwise errors
        client.getHostConfiguration().getParams().setParameter(HttpClientParams.PROTOCOL_VERSION, HttpVersion.HTTP_1_0);
        httpClient = client;
        return client.getHostConfiguration();
    }

	public static void setupHttpClient(String repositoryUrl) {
		if (httpClient != null) {
			setupHttpClient(httpClient, repositoryUrl);
		}
	}

	public static void setupHttpClient(HttpClient client, String repositoryUrl) {
		if (isRepositoryHttps(repositoryUrl)) {
			Protocol acceptAllSsl = new Protocol("https",
					(ProtocolSocketFactory) SslProtocolSocketFactory.getInstance(),
					getPort(repositoryUrl));
			client.getHostConfiguration().setHost(getHost(repositoryUrl),
					getPort(repositoryUrl), acceptAllSsl);
			Protocol.registerProtocol("https", acceptAllSsl);
		} else {
			client.getHostConfiguration().setHost(getHost(repositoryUrl),
					getPort(repositoryUrl));
		}
	}

	static boolean isRepositoryHttps(String repositoryUrl) {
		return repositoryUrl.matches("https.*");
	}

	public static String getHost(String repositoryUrl) {
		String result = repositoryUrl;
		int colonSlashSlash = repositoryUrl.indexOf("://");

		if (colonSlashSlash >= 0) {
			result = repositoryUrl.substring(colonSlashSlash + 3);
		}

		int colonPort = result.indexOf(':');
		int requestPath = result.indexOf('/');

		int substringEnd;

		// minimum positive, or string length
		if (colonPort > 0 && requestPath > 0) {
			substringEnd = Math.min(colonPort, requestPath);
		} else if (colonPort > 0) {
			substringEnd = colonPort;
		} else if (requestPath > 0) {
			substringEnd = requestPath;
		} else {
			substringEnd = result.length();
		}

		return result.substring(0, substringEnd);
	}

	public static int getPort(String repositoryUrl) {
		int colonSlashSlash = repositoryUrl.indexOf("://");
		int firstSlash = repositoryUrl.indexOf("/", colonSlashSlash + 3);
		int colonPort = repositoryUrl.indexOf(':', colonSlashSlash + 1);
		if (firstSlash == -1) {
			firstSlash = repositoryUrl.length();
		}
		if (colonPort < 0 || colonPort > firstSlash) {
			return isRepositoryHttps(repositoryUrl) ? HTTPS_PORT : HTTP_PORT;
		}

		int requestPath = repositoryUrl.indexOf('/', colonPort + 1);
		int end = requestPath < 0 ? repositoryUrl.length() : requestPath;
		String port = repositoryUrl.substring(colonPort + 1, end);
		if (port.length() == 0) {
			return isRepositoryHttps(repositoryUrl) ? HTTPS_PORT : HTTP_PORT;
		}

		return Integer.parseInt(port);
	}
}
