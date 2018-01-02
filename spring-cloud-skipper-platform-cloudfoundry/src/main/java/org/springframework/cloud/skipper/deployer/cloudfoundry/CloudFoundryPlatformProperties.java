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

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryConnectionProperties;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeploymentProperties;

/**
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 * @author Donovan Muller
 */
@ConfigurationProperties("spring.cloud.skipper.server.platform.cloudfoundry")
public class CloudFoundryPlatformProperties {

	private Map<String, CloudFoundryProperties> accounts = new LinkedHashMap<>();

	public Map<String, CloudFoundryProperties> getAccounts() {
		return accounts;
	}

	public void setAccounts(Map<String, CloudFoundryProperties> accounts) {
		this.accounts = accounts;
	}

	public static class CloudFoundryProperties {

		private CloudFoundryConnectionProperties connection;

		private CloudFoundryDeploymentProperties deployment;

		public CloudFoundryConnectionProperties getConnection() {
			return connection;
		}

		public void setConnection(CloudFoundryConnectionProperties connection) {
			this.connection = connection;
		}

		public CloudFoundryDeploymentProperties getDeployment() {
			return deployment;
		}

		public void setDeployment(CloudFoundryDeploymentProperties deployment) {
			this.deployment = deployment;
		}
	}
}
