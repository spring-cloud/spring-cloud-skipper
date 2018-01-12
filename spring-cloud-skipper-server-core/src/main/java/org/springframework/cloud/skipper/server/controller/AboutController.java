/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.cloud.skipper.server.controller;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.skipper.domain.AboutResource;
import org.springframework.cloud.skipper.domain.Dependency;
import org.springframework.cloud.skipper.domain.VersionInfo;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * REST controller that provides meta information regarding the skipper server.
 *
 * @author Janne Valkealahti
 *
 */
@RestController
@RequestMapping("/api/about")
public class AboutController {

	private static final Logger logger = LoggerFactory.getLogger(AboutController.class);
	private final VersionInfoProperties versionInfoProperties;

	public AboutController(VersionInfoProperties versionInfoProperties) {
		this.versionInfoProperties = versionInfoProperties;
	}

	/**
	 * Return meta information about the Skipper server.
	 *
	 * @return Detailed information about the enabled features, versions of implementation
	 * libraries, and security configuration
	 */
	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public AboutResource getAboutResource() {
		final AboutResource aboutResource = new AboutResource();
		final VersionInfo versionInfo = getVersionInfo();
		aboutResource.setVersionInfo(versionInfo);
		return aboutResource;
	}

	private VersionInfo getVersionInfo() {
		final VersionInfo versionInfo = new VersionInfo();

		updateDependency(versionInfo.getServer(),
				versionInfoProperties.getDependencies().getSpringCloudSkipperServer());
		updateDependency(versionInfo.getShell(),
				versionInfoProperties.getDependencies().getSpringCloudSkipperShell());

		if (versionInfoProperties.getDependencyFetch().isEnabled()) {
			versionInfo.getShell().setChecksumSha1(getChecksum(
					versionInfoProperties.getDependencies().getSpringCloudSkipperShell().getChecksumSha1(),
					versionInfoProperties.getDependencies().getSpringCloudSkipperShell().getChecksumSha1Url(),
					versionInfoProperties.getDependencies().getSpringCloudSkipperShell().getVersion()));
			versionInfo.getShell().setChecksumSha256(getChecksum(
					versionInfoProperties.getDependencies().getSpringCloudSkipperShell().getChecksumSha256(),
					versionInfoProperties.getDependencies().getSpringCloudSkipperShell().getChecksumSha256Url(),
					versionInfoProperties.getDependencies().getSpringCloudSkipperShell().getVersion()));
		}
		return versionInfo;
	}

	private String getChecksum(String defaultValue, String url,
			String version) {
		String result = defaultValue;
		if (result == null && StringUtils.hasText(url)) {
			CloseableHttpClient httpClient = HttpClients.custom()
					.setSSLHostnameVerifier(new NoopHostnameVerifier())
					.build();
			HttpComponentsClientHttpRequestFactory requestFactory
					= new HttpComponentsClientHttpRequestFactory();
			requestFactory.setHttpClient(httpClient);
			url = constructUrl(url, version);
			try {
				ResponseEntity<String> response
						= new RestTemplate(requestFactory).exchange(
						url, HttpMethod.GET, null, String.class);
				if (response.getStatusCode().equals(HttpStatus.OK)) {
					result = response.getBody();
				}
			}
			catch (HttpClientErrorException httpException) {
				// no action necessary set result to undefined
				logger.debug("Didn't retrieve checksum because", httpException);
			}
		}
		return result;
	}

	private void updateDependency(Dependency dependency, VersionInfoProperties.DependencyAboutInfo dependencyAboutInfo) {
		dependency.setName(dependencyAboutInfo.getName());
		if (dependencyAboutInfo.getUrl() != null) {
			dependency.setUrl(constructUrl(dependencyAboutInfo.getUrl(),
					dependencyAboutInfo.getVersion()));
		}
		dependency.setVersion(dependencyAboutInfo.getVersion());
	}

	private String constructUrl(String url, String version) {
		final String VERSION_TAG = "{version}";
		final String REPOSITORY_TAG = "{repository}";
		if (url.contains(VERSION_TAG)) {
			url = StringUtils.replace(url, VERSION_TAG, version);
			url = StringUtils.replace(url, REPOSITORY_TAG, repoSelector(version));
		}
		return url;
	}

	private String repoSelector(String version) {
		final String BUILD_SNAPSHOT_CRITERIA = "BUILD-SNAPSHOT";
		final String REPO_SNAPSHOT_ROOT = "https://repo.spring.io/libs-snapshot";
		final String REPO_MILESTONE_ROOT = "https://repo.spring.io/libs-milestone";
		final String REPO_RELEASE_ROOT = "https://repo.spring.io/libs-release";
		final String MAVEN_ROOT = "https://repo1.maven.org/maven2";
		String result = MAVEN_ROOT;
		if (version.endsWith(BUILD_SNAPSHOT_CRITERIA)) {
			result = REPO_SNAPSHOT_ROOT;
		}
		else if (version.contains(".M")) {
			result = REPO_MILESTONE_ROOT;
		}
		else if (version.contains(".RC")) {
			result = REPO_RELEASE_ROOT;
		}
		return result;
	}
}
