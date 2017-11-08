/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.skipper.server.deployer;

import org.junit.Test;

import org.springframework.cloud.deployer.resource.support.DelegatingResourceLoader;
import org.springframework.cloud.skipper.SkipperException;
import org.springframework.cloud.skipper.server.domain.SpringBootAppKind;
import org.springframework.cloud.skipper.server.domain.SpringBootAppSpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Ilayaperumal Gopinathan
 */
public class AppDeploymentRequestFactoryTests {

	@Test
	@SuppressWarnings("unchecked")
	public void testGetResourceExceptionHandler() {
		DelegatingResourceLoader resourceLoader = mock(DelegatingResourceLoader.class);
		AppDeploymentRequestFactory appDeploymentRequestFactory = new AppDeploymentRequestFactory(resourceLoader);
		when(resourceLoader.getResource(anyString())).thenThrow(Exception.class);
		SpringBootAppKind springBootAppKind = mock(SpringBootAppKind.class);
		SpringBootAppSpec springBootAppSpec = mock(SpringBootAppSpec.class);
		when(springBootAppKind.getSpec()).thenReturn(springBootAppSpec);
		String specResource = "http://test";
		when(springBootAppSpec.getResource()).thenReturn(specResource);
		when(springBootAppSpec.getApplicationProperties()).thenReturn(null);
		try {
			appDeploymentRequestFactory.createAppDeploymentRequest(springBootAppKind, "release1", "1.0.0");
			fail("SkipperException is expected to be thrown.");
		}
		catch (SkipperException e) {
			assertThat(e.getMessage()).contains("Could not load Resource " + specResource + ".");
		}
	}

	@Test
	public void testGetResourceLocation() {
		SpringBootAppSpec springBootAppSpec1 = mock(SpringBootAppSpec.class);
		String mavenSpecResource = "maven://org.springframework.cloud.stream.app:log-sink-rabbit";
		String mavenSpecVersion = "1.2.0.RELEASE";
		when(springBootAppSpec1.getResource()).thenReturn(mavenSpecResource);
		when(springBootAppSpec1.getVersion()).thenReturn(mavenSpecVersion);
		SpringBootAppSpec springBootAppSpec2 = mock(SpringBootAppSpec.class);
		String dockerSpecResource = "docker:springcloudstream/log-sink-rabbit";
		String dockerSpecVersion = "1.2.0.RELEASE";
		when(springBootAppSpec2.getResource()).thenReturn(dockerSpecResource);
		when(springBootAppSpec2.getVersion()).thenReturn(dockerSpecVersion);
		SpringBootAppSpec springBootAppSpec3 = mock(SpringBootAppSpec.class);
		String httpSpecResource = "http://repo.spring.io/libs-release/org/springframework/cloud/stream/app/"
				+ "log-sink-rabbit/1.2.0.RELEASE/log-sink-rabbit-1.2.0.RELEASE.jar";
		when(springBootAppSpec3.getResource()).thenReturn(httpSpecResource);
		when(springBootAppSpec3.getVersion()).thenReturn("1.2.0.RELEASE");
		DelegatingResourceLoader resourceLoader = mock(DelegatingResourceLoader.class);
		AppDeploymentRequestFactory appDeploymentRequestFactory = new AppDeploymentRequestFactory(resourceLoader);
		assertThat(appDeploymentRequestFactory.getResourceLocation(springBootAppSpec1))
				.isEqualTo(String.format("%s:%s", mavenSpecResource, mavenSpecVersion));
		assertThat(appDeploymentRequestFactory.getResourceLocation(springBootAppSpec2))
				.isEqualTo(String.format("%s:%s", dockerSpecResource, dockerSpecVersion));
		assertThat(appDeploymentRequestFactory.getResourceLocation(springBootAppSpec1)).isEqualTo(httpSpecResource);
	}
}
