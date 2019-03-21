/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.skipper.client.util;

import java.net.URI;

import org.apache.http.HttpHost;
import org.apache.http.client.AuthCache;
import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

/**
 * {@link HttpComponentsClientHttpRequestFactory} extension that aggressively sends HTTP
 * basic authentication credentials without having to first receive an HTTP 401 response
 * from the server.
 *
 * @author Gunnar Hillert
 * @author Mike Heath
 */
public class PreemptiveBasicAuthHttpComponentsClientHttpRequestFactory extends HttpComponentsClientHttpRequestFactory {

	private final HttpHost host;

	public PreemptiveBasicAuthHttpComponentsClientHttpRequestFactory(HttpClient httpClient, HttpHost host) {
		super(httpClient);
		this.host = host;
	}

	@Override
	protected HttpContext createHttpContext(HttpMethod httpMethod, URI uri) {
		final AuthCache authCache = new BasicAuthCache();

		final BasicScheme basicAuth = new BasicScheme();
		authCache.put(host, basicAuth);

		final BasicHttpContext localcontext = new BasicHttpContext();
		localcontext.setAttribute(HttpClientContext.AUTH_CACHE, authCache);
		return localcontext;
	}

}
