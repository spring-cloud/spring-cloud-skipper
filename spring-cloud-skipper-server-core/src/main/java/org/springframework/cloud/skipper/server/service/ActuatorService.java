/*
 * Copyright 2022 the original author or authors.
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

package org.springframework.cloud.skipper.server.service;

import org.springframework.cloud.deployer.spi.app.ActuatorOperations;
import org.springframework.cloud.skipper.domain.ActuatorPostRequest;

/**
 * Service used to access an actuator endpoint for a deployed instance.
 *
 * @author David Turanski
 */
public class ActuatorService {

	private final ReleaseService releaseService;

	private final ActuatorOperations actuatorOperations;

	public ActuatorService(ReleaseService releaseService, ActuatorOperations actuatorOperations) {
		this.releaseService = releaseService;
		this.actuatorOperations = actuatorOperations;
	}

	/**
	 *
	 * @param releaseName the release name.
	 * @param appName the deployment ID for the app
	 * @param appId
	 * @param endpoint
	 * @return
	 */
	public String getFromActuator(String releaseName, String appName, String appId, String endpoint) {
		return actuatorOperations.getFromActuator(deploymentId(releaseName, appName), appId, endpoint, String.class);
	}

	/**
	 *
	 * @param releaseName
	 * @param appName
	 * @param appId
	 * @param postRequest
	 * @return
	 */
	public Object postToActuator(String releaseName, String appName, String appId, ActuatorPostRequest postRequest) {
		return actuatorOperations.postToActuator(deploymentId(releaseName, appName), appId, postRequest.getEndpoint(),
			postRequest.getBody(), Object.class);
	}

	private String deploymentId(String releaseName, String appName) {
		return this.releaseService.status(releaseName).getStatus().getAppStatusList().stream()
				.filter(as -> appName.equals(as.getDeploymentId()))
				.map(as -> as.getDeploymentId())
				.findFirst().orElseThrow(() -> new IllegalArgumentException(
						String.format("app %s is not found in release %s", appName, releaseName)));
	}
}
