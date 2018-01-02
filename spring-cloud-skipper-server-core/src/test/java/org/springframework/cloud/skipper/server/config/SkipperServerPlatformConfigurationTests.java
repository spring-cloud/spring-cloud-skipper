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
package org.springframework.cloud.skipper.server.config;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.core.RuntimeEnvironmentInfo;
import org.springframework.cloud.skipper.domain.Deployer;
import org.springframework.cloud.skipper.domain.Platform;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.statemachine.boot.autoconfigure.StateMachineJpaRepositoriesAutoConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Donovan Muller
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
		SkipperServerPlatformConfigurationTests.AllPlatformsConfigurationTest.class,
		SkipperServerPlatformConfigurationTests.ExternalPlatformsOnlyConfigurationTest.class
})
public class SkipperServerPlatformConfigurationTests {

	@RunWith(SpringRunner.class)
	@SpringBootTest(classes = TestConfig.class)
	@ActiveProfiles("platform-configuration")
	public static class AllPlatformsConfigurationTest {

		@Autowired
		private List<Platform> platforms;

		@Test
		public void allPlatformsConfiguredTest() {
			assertThat(platforms).extracting("name").containsExactly("Local", "Test");
		}
	}

	@RunWith(SpringRunner.class)
	@SpringBootTest(classes = TestConfig.class, properties = "spring.cloud.skipper.server.enableLocalPlatform=false")
	@ActiveProfiles("platform-configuration")
	public static class ExternalPlatformsOnlyConfigurationTest {

		@Autowired
		private List<Platform> platforms;

		@Test
		public void localPlatformDisabledTest() {
			assertThat(platforms).extracting("name").containsExactly("Test");
		}
	}

	@Configuration
	@ImportAutoConfiguration(classes = { EmbeddedDataSourceConfiguration.class, HibernateJpaAutoConfiguration.class,
			StateMachineJpaRepositoriesAutoConfiguration.class, SkipperServerPlatformConfiguration.class,
			TestPlatformAutoConfiguration.class })
	@Import(SkipperServerConfiguration.class)
	static class TestConfig {
	}

	@Configuration
	static class TestPlatformAutoConfiguration {

		@Bean
		public Platform testPlatform() {
			return new Platform("Test", Collections.singletonList(
					new Deployer("test", "test", new AppDeployer() {

						@Override
						public String deploy(AppDeploymentRequest request) {
							return null;
						}

						@Override
						public void undeploy(String id) {
						}

						@Override
						public AppStatus status(String id) {
							return null;
						}

						@Override
						public RuntimeEnvironmentInfo environmentInfo() {
							return null;
						}
					})));
		}
	}
}
