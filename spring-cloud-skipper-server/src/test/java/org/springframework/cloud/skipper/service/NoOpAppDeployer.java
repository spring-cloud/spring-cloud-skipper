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
package org.springframework.cloud.skipper.service;

import java.util.UUID;

import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.core.RuntimeEnvironmentInfo;
import org.springframework.cloud.deployer.spi.util.RuntimeVersionUtils;

/**
 * @author Mark Pollack
 */
public class NoOpAppDeployer implements AppDeployer {

	@Override
	public String deploy(AppDeploymentRequest request) {
		return UUID.randomUUID().toString();
	}

	@Override
	public void undeploy(String id) {

	}

	@Override
	public AppStatus status(String id) {
		return AppStatus.of(id)
				.generalState(DeploymentState.deployed)
				.build();
	}

	@Override
	public RuntimeEnvironmentInfo environmentInfo() {
		return new RuntimeEnvironmentInfo.Builder()
				.spiClass(NoOpAppDeployer.class)
				.implementationName(NoOpAppDeployer.class.getSimpleName())
				.implementationVersion(RuntimeVersionUtils.getVersion(NoOpAppDeployer.class))
				.platformType("NoOp")
				.platformApiVersion(System.getProperty("os.name") + " " + System.getProperty("os.version"))
				.platformClientVersion(System.getProperty("os.version"))
				.platformHostVersion(System.getProperty("os.version"))
				.build();
	}
}
