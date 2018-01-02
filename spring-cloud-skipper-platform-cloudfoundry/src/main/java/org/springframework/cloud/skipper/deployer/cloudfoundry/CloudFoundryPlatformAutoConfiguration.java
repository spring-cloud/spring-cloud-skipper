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
package org.springframework.cloud.skipper.deployer.cloudfoundry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.github.zafarkhaja.semver.Version;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.info.GetInfoRequest;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryAppDeployer;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryAppNameGenerator;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryConnectionProperties;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeploymentProperties;
import org.springframework.cloud.deployer.spi.core.RuntimeEnvironmentInfo;
import org.springframework.cloud.deployer.spi.util.RuntimeVersionUtils;
import org.springframework.cloud.skipper.domain.Deployer;
import org.springframework.cloud.skipper.domain.Platform;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Donovan Muller
 */
@Configuration
@EnableConfigurationProperties(CloudFoundryPlatformProperties.class)
public class CloudFoundryPlatformAutoConfiguration {

	private static final Logger logger = LoggerFactory
			.getLogger(CloudFoundryPlatformAutoConfiguration.class);

	@Bean
	public Platform cloudFoundryPlatform(
			CloudFoundryPlatformProperties cloudFoundryPlatformProperties) {
		List<Deployer> deployers = new ArrayList<>();
		Map<String, CloudFoundryPlatformProperties.CloudFoundryProperties> cfConnectionProperties = cloudFoundryPlatformProperties
				.getAccounts();
		cfConnectionProperties.forEach((key, value) -> {
			Deployer deployer = createAndSaveCFAppDeployer(key, value);
			deployers.add(deployer);
		});

		return new Platform("Cloud Foundry", deployers);
	}

	private Deployer createAndSaveCFAppDeployer(String account,
			CloudFoundryPlatformProperties.CloudFoundryProperties cloudFoundryProperties) {
		CloudFoundryAppNameGenerator appNameGenerator = new CloudFoundryAppNameGenerator(
				cloudFoundryProperties.getDeployment());
		CloudFoundryDeploymentProperties deploymentProperties = cloudFoundryProperties
				.getDeployment();
		if (deploymentProperties == null) {
			// todo: use server level shared deployment properties
			deploymentProperties = new CloudFoundryDeploymentProperties();
		}
		CloudFoundryConnectionProperties connectionProperties = cloudFoundryProperties
				.getConnection();
		try {
			ConnectionContext connectionContext = DefaultConnectionContext.builder()
					.apiHost(connectionProperties.getUrl().getHost())
					.skipSslValidation(connectionProperties.isSkipSslValidation())
					.build();
			TokenProvider tokenProvider = PasswordGrantTokenProvider.builder()
					.username(connectionProperties.getUsername())
					.password(connectionProperties.getPassword()).build();
			CloudFoundryClient cloudFoundryClient = ReactorCloudFoundryClient.builder()
					.connectionContext(connectionContext).tokenProvider(tokenProvider)
					.build();
			Version version = cloudFoundryClient.info()
					.get(GetInfoRequest.builder().build())
					.map(response -> Version.valueOf(response.getApiVersion()))
					.doOnNext(versionInfo -> logger.info(
							"Connecting to Cloud Foundry with API Version {}",
							versionInfo))
					.block(Duration.ofSeconds(deploymentProperties.getApiTimeout()));
			RuntimeEnvironmentInfo runtimeEnvironmentInfo = new RuntimeEnvironmentInfo.Builder()
					.implementationName(CloudFoundryAppDeployer.class.getSimpleName())
					.spiClass(AppDeployer.class)
					.implementationVersion(
							RuntimeVersionUtils.getVersion(CloudFoundryAppDeployer.class))
					.platformType("Cloud Foundry")
					.platformClientVersion(
							RuntimeVersionUtils.getVersion(cloudFoundryClient.getClass()))
					.platformApiVersion(version.toString()).platformHostVersion("unknown")
					.addPlatformSpecificInfo("API Endpoint",
							connectionProperties.getUrl().toString())
					.build();
			CloudFoundryOperations cloudFoundryOperations = DefaultCloudFoundryOperations
					.builder().cloudFoundryClient(cloudFoundryClient)
					.organization(connectionProperties.getOrg())
					.space(connectionProperties.getSpace()).build();
			CloudFoundryAppDeployer cfAppDeployer = new CloudFoundryAppDeployer(
					appNameGenerator, deploymentProperties, cloudFoundryOperations,
					runtimeEnvironmentInfo);
			Deployer deployer = new Deployer(account, "cloudfoundry", cfAppDeployer);
			deployer.setDescription(String.format("org = [%s], space = [%s], url = [%s]",
					connectionProperties.getOrg(), connectionProperties.getSpace(),
					connectionProperties.getUrl()));
			return deployer;
		}
		catch (Exception e) {
			logger.error("Cloud Foundry platform account" + account
					+ " could not be registered." + e.getMessage());
			return null;
		}
	}
}
