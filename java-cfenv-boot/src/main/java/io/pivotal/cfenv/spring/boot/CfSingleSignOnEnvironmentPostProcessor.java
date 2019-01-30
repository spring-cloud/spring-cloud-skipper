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
package io.pivotal.cfenv.spring.boot;

import java.util.LinkedHashMap;
import java.util.Map;

import io.pivotal.cfenv.core.CfCredentials;
import io.pivotal.cfenv.core.CfEnv;
import io.pivotal.cfenv.core.CfService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.context.config.ConfigFileApplicationListener;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLog;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.CommandLinePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

/**
 * @author Mark Pollack
 */
public class CfSingleSignOnEnvironmentPostProcessor implements EnvironmentPostProcessor,
		Ordered, ApplicationListener<ApplicationEvent> {

	private static final String PIVOTAL_SSO_LABEL = "p-identity";

	private static final String PROPERTY_SOURCE_NAME = "cfSingleSignOnEnvironmentPostProcessor";

	private static DeferredLog DEFERRED_LOG = new DeferredLog();

	private static int invocationCount;

	// Before ConfigFileApplicationListener so values there can use these ones
	private int order = ConfigFileApplicationListener.DEFAULT_ORDER - 1;

	@Override
	public int getOrder() {
		return this.order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment,
			SpringApplication application) {
		increaseInvocationCount();
		if (CloudPlatform.CLOUD_FOUNDRY.isActive(environment)) {
			CfEnv cfEnv = CfEnvSingleton.getCfEnvInstance();
			CfService cfService;
			try {
				cfService = cfEnv.findServiceByLabel(PIVOTAL_SSO_LABEL);
			}
			catch (Exception e) {
				if (invocationCount == 1) {
					DEFERRED_LOG.debug(
							"Skipping execution of CfSingleSignOnEnvironmentPostProcessor.  "
									+ e.getMessage());
				}
				return;
			}

			if (cfService != null) {
				CfCredentials cfCredentials = cfService.getCredentials();
				String clientId = cfCredentials.getString("client_id");
				String clientSecret = cfCredentials.getString("client_secret");
				String authDomain = cfCredentials.getString("auth_domain");

				Map<String, Object> properties = new LinkedHashMap<>();
				properties.put("security.oauth2.client.clientId", clientId);
				properties.put("security.oauth2.client.clientSecret", clientSecret);
				properties.put("security.oauth2.client.accessTokenUri",
						authDomain + "/oauth/token");
				properties.put("security.oauth2.client.userAuthorizationUri",
						authDomain + "/oauth/authorize");
				properties.put("ssoServiceUrl", authDomain);
				properties.put("security.oauth2.resource.userInfoUri",
						authDomain + "/userinfo");
				properties.put("security.oauth2.resource.tokenInfoUri",
						authDomain + "/check_token");
				properties.put("security.oauth2.resource.jwk.key-set-uri",
						authDomain + "/token_keys");

				MutablePropertySources propertySources = environment.getPropertySources();
				if (propertySources.contains(
						CommandLinePropertySource.COMMAND_LINE_PROPERTY_SOURCE_NAME)) {
					propertySources.addAfter(
							CommandLinePropertySource.COMMAND_LINE_PROPERTY_SOURCE_NAME,
							new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
				}
				else {
					propertySources.addFirst(
							new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
				}

				if (invocationCount == 1) {
					DEFERRED_LOG.info(
							"Setting security.oauth2.client properties from bound service ["
									+ cfService.getName() + "]");
				}
			}
		}
		else {
			if (invocationCount == 1) {
				DEFERRED_LOG.debug(
						"Not setting security.oauth2.client properties, not in Cloud Foundry Environment");
			}
		}
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ApplicationPreparedEvent) {
			DEFERRED_LOG.switchTo(CfSingleSignOnEnvironmentPostProcessor.class);
		}
	}

	/**
	 * EnvironmentPostProcessors can end up getting called twice due to spring-cloud-commons
	 * functionality
	 */
	private void increaseInvocationCount() {
		synchronized (this) {
			invocationCount++;
		}
	}

}
