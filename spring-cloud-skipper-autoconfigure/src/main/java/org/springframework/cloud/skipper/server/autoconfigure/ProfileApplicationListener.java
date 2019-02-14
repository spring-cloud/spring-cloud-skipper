/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.cloud.skipper.server.autoconfigure;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.cloud.skipper.deployer.kubernetes.KubernetesCloudProfileProvider;
import org.springframework.cloud.skipper.server.config.CloudProfileProvider;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.CommandLinePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

/**
 * @author Mark Pollack
 */
public class ProfileApplicationListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

	private static final Logger logger = LoggerFactory.getLogger(ProfileApplicationListener.class);

	private ConfigurableEnvironment environment;

	@Override
	public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
		this.environment = event.getEnvironment();
		Iterable<CloudProfileProvider> cloudProfileProviders = ServiceLoader.load(CloudProfileProvider.class);
		boolean addedCloudProfile = false;
		boolean addedKubernetesProfile = false;
		for (CloudProfileProvider cloudProfileProvider : cloudProfileProviders) {
			if (cloudProfileProvider.isCloudPlatform(environment)) {
				String profileToAdd = cloudProfileProvider.getCloudProfile();
				if (!Arrays.asList(environment.getActiveProfiles()).contains(profileToAdd)) {
					if (profileToAdd.equals(KubernetesCloudProfileProvider.PROFILE)) {
						addedKubernetesProfile = true;
					}
					environment.addActiveProfile(profileToAdd);
					addedCloudProfile = true;
				}
			}
		}
		if (!addedKubernetesProfile) {
			Map<String, Object> properties = new LinkedHashMap<>();
			properties.put("spring.cloud.kubernetes.enabled", false);
			logger.debug("Setting property 'spring.cloud.kubernetes.enabled' to false.");
			MutablePropertySources propertySources = environment.getPropertySources();
			if (propertySources != null) {
				if (propertySources.contains(
						CommandLinePropertySource.COMMAND_LINE_PROPERTY_SOURCE_NAME)) {
					propertySources.addAfter(
							CommandLinePropertySource.COMMAND_LINE_PROPERTY_SOURCE_NAME,
							new MapPropertySource("skipperProfileApplicationListener", properties));
				}
				else {
					propertySources
							.addFirst(new MapPropertySource("skipperProfileApplicationListener", properties));
				}
			}
		}
		if (!addedCloudProfile) {
			environment.addActiveProfile("local");
		}
	}

	@Override
	public int getOrder() {
		return 0;
	}
}
