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

package org.springframework.cloud.skipper.server.autoconfigure.security;

import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.NoneNestedConditions;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.WebSecurityEnablerConfiguration;
import org.springframework.cloud.common.security.support.OnSecurityEnabledAndOAuth2Enabled;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * Skipper basic security control. By default the security for all endpoints is disabled.
 *
 * When {@code spring.cloud.skipper.security.enabled} is not true
 * and {@code security.oauth2.client.client-id} is not set (e.g. the default condition) then
 * the Skipper security is disabled allowing unauthenticated access to all Skipper endpoints.
 *
 * If {@code spring.cloud.skipper.security.enabled} is set to true the security falls back to
 * the {@link ManagementWebSecurityAutoConfiguration} allowing unauthenticated access only
 * to the HealthEndpoint and InfoEndpoint.
 *
 * If {@code security.oauth2.client.client-id} is set (e.g. OnSecurityEnabledAndOAuth2Enabled condition
 * is matched) then the security configuration is handled by the
 * {@link org.springframework.cloud.skipper.server.config.security.SkipperOAuthSecurityConfiguration}.
 *
 * Note: if the {@link OnSecurityEnabledAndOAuth2Enabled} condition is true then the
 * {@link OnSecurityDisabled.SkipperBasicSecurityEnabled} is ignored.
 *
 * @author Christian Tzolov
 */
@Configuration
@ConditionalOnClass(WebSecurityConfigurerAdapter.class)
@ConditionalOnMissingBean(WebSecurityConfigurerAdapter.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.ANY)
@Conditional(SkipperWebSecurityAutoConfiguration.OnSecurityDisabled.class)
@AutoConfigureBefore({ ManagementWebSecurityAutoConfiguration.class, SecurityAutoConfiguration.class })
@Import({ SkipperWebSecurityConfigurerAdapter.class, WebSecurityEnablerConfiguration.class })
public class SkipperWebSecurityAutoConfiguration {

	public static class OnSecurityDisabled extends NoneNestedConditions {

		public OnSecurityDisabled() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnProperty(name = "spring.cloud.skipper.security.enabled", havingValue = "true")
		static class SkipperBasicSecurityEnabled {
		}

		@Conditional(OnSecurityEnabledAndOAuth2Enabled.class)
		static class OAuth2Enabled {
		}
	}
}
