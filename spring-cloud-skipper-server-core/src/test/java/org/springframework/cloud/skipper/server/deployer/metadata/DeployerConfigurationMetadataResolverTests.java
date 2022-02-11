/*
 * Copyright 2019-2022 the original author or authors.
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
package org.springframework.cloud.skipper.server.deployer.metadata;

import java.util.List;

import org.junit.Test;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.skipper.server.config.SkipperServerProperties;

import static org.assertj.core.api.Assertions.assertThat;

public class DeployerConfigurationMetadataResolverTests {

	//todo: Brittle. This breaks if you add a deployer property
	private static final int ALL_LOCAL_DEPLOYER_PROPERTIES = 24;

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(Config.class);


	@Test
	public void testNoFiltersFindsAll() {
		this.contextRunner
				.run((context) -> {
					SkipperServerProperties skipperServerProperties = context.getBean(SkipperServerProperties.class);
					DeployerConfigurationMetadataResolver resolver = new DeployerConfigurationMetadataResolver(
							skipperServerProperties.getDeployerProperties());
					resolver.setApplicationContext(context);
					List<ConfigurationMetadataProperty> data = resolver.resolve();
					assertThat(data.size()).isEqualTo(ALL_LOCAL_DEPLOYER_PROPERTIES);
				});
	}

	@Test
	public void testExcludeGroup() {
		this.contextRunner
				.withPropertyValues(
						"spring.cloud.skipper.server.deployer-properties.group-excludes=spring.cloud.deployer.local.port-range"
				)
				.run((context) -> {
					SkipperServerProperties skipperServerProperties = context.getBean(SkipperServerProperties.class);
					DeployerConfigurationMetadataResolver resolver = new DeployerConfigurationMetadataResolver(
							skipperServerProperties.getDeployerProperties());
					resolver.setApplicationContext(context);
					List<ConfigurationMetadataProperty> data = resolver.resolve();
					assertThat(data.size()).isEqualTo(ALL_LOCAL_DEPLOYER_PROPERTIES - 2);
				});
	}

	@Test
	public void testExcludeProperty() {
		this.contextRunner
				.withPropertyValues(
						"spring.cloud.skipper.server.deployer-properties.property-excludes=spring.cloud.deployer.local.port-range.low"
				)
				.run((context) -> {
					SkipperServerProperties skipperServerProperties = context.getBean(SkipperServerProperties.class);
					DeployerConfigurationMetadataResolver resolver = new DeployerConfigurationMetadataResolver(
							skipperServerProperties.getDeployerProperties());
					resolver.setApplicationContext(context);
					List<ConfigurationMetadataProperty> data = resolver.resolve();
					assertThat(data.size()).isEqualTo(ALL_LOCAL_DEPLOYER_PROPERTIES - 1);
				});
	}

	@Test
	public void testIncludeGroup() {
		this.contextRunner
				.withPropertyValues(
						"spring.cloud.skipper.server.deployer-properties.group-includes=spring.cloud.deployer.local.port-range"
				)
				.run((context) -> {
					SkipperServerProperties skipperServerProperties = context.getBean(SkipperServerProperties.class);
					DeployerConfigurationMetadataResolver resolver = new DeployerConfigurationMetadataResolver(
							skipperServerProperties.getDeployerProperties());
					resolver.setApplicationContext(context);
					List<ConfigurationMetadataProperty> data = resolver.resolve();
					assertThat(data.size()).isEqualTo(2);
				});
	}

	@Test
	public void testIncludeProperty() {
		this.contextRunner
				.withPropertyValues(
						"spring.cloud.skipper.server.deployer-properties.property-includes=spring.cloud.deployer.local.port-range.low"
				)
				.run((context) -> {
					SkipperServerProperties skipperServerProperties = context.getBean(SkipperServerProperties.class);
					DeployerConfigurationMetadataResolver resolver = new DeployerConfigurationMetadataResolver(
							skipperServerProperties.getDeployerProperties());
					resolver.setApplicationContext(context);
					List<ConfigurationMetadataProperty> data = resolver.resolve();
					assertThat(data.size()).isEqualTo(1);
				});
	}

	@Test
	public void testIncludeMultipleProperty() {
		this.contextRunner
				.withPropertyValues(
						"spring.cloud.skipper.server.deployer-properties.property-includes=spring.cloud.deployer.local.port-range.low,spring.cloud.deployer.local.port-range.high"
				)
				.run((context) -> {
					SkipperServerProperties skipperServerProperties = context.getBean(SkipperServerProperties.class);
					DeployerConfigurationMetadataResolver resolver = new DeployerConfigurationMetadataResolver(
							skipperServerProperties.getDeployerProperties());
					resolver.setApplicationContext(context);
					List<ConfigurationMetadataProperty> data = resolver.resolve();
					assertThat(data.size()).isEqualTo(2);
				});
	}

	@Test
	public void testIncludeGroupExcludeProperty() {
		this.contextRunner
				.withPropertyValues(
						"spring.cloud.skipper.server.deployer-properties.group-includes=spring.cloud.deployer.local.port-range",
						"spring.cloud.skipper.server.deployer-properties.property-excludes=spring.cloud.deployer.local.port-range.low"
				)
				.run((context) -> {
					SkipperServerProperties skipperServerProperties = context.getBean(SkipperServerProperties.class);
					DeployerConfigurationMetadataResolver resolver = new DeployerConfigurationMetadataResolver(
							skipperServerProperties.getDeployerProperties());
					resolver.setApplicationContext(context);
					List<ConfigurationMetadataProperty> data = resolver.resolve();
					assertThat(data.size()).isEqualTo(1);
				});
	}

	@EnableConfigurationProperties({ SkipperServerProperties.class })
	private static class Config {
	}
}
