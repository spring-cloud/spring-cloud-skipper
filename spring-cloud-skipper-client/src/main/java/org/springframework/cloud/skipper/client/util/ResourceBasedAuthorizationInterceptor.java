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

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;

import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

/**
 * {@link HttpRequestInterceptor} that adds an {@code Authorization} header with a value
 * obtained from a {@link Resource}.
 *
 * @author Mike Heath
 */
public class ResourceBasedAuthorizationInterceptor implements HttpRequestInterceptor {

	private final Resource resource;

	public ResourceBasedAuthorizationInterceptor(Resource resource) {
		this.resource = resource;
	}

	@Override
	public void process(HttpRequest httpRequest, HttpContext httpContext) throws HttpException, IOException {
		final String credentials = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8).trim();
		httpRequest.addHeader(HttpHeaders.AUTHORIZATION, credentials);
	}
}
