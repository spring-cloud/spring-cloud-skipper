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
package org.springframework.cloud.skipper.server.deployer.strategies;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.Status;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.server.domain.AppDeployerData;
import org.springframework.cloud.skipper.server.repository.DeployerRepository;
import org.springframework.cloud.skipper.server.repository.ReleaseRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reponsible for checking the health of the latest deployed release, and then deleting
 * those applications from the previous release. Executed asynchronously to avoid the
 * client waiting a long time, potentially minutes, when executing update.
 * @author Mark Pollack
 */
public class HealthCheckAndDeleteStep {

	private final Logger logger = LoggerFactory.getLogger(SimpleRedBlackUpgradeStrategy.class);

	private final ReleaseRepository releaseRepository;

	private final DeployerRepository deployerRepository;

	private final DeleteStep deleteStep;

	private final HealthCheckProperties healthCheckProperties;

	public HealthCheckAndDeleteStep(ReleaseRepository releaseRepository,
			DeployerRepository deployerRepository,
			DeleteStep deleteStep,
			HealthCheckProperties healthCheckProperties) {
		this.releaseRepository = releaseRepository;
		this.deployerRepository = deployerRepository;
		this.deleteStep = deleteStep;
		this.healthCheckProperties = healthCheckProperties;
	}

	@Async("skipperThreadPoolTaskExecutor")
	@Transactional
	public CompletableFuture<Void> waitForNewAppsToDeploy(Release existingRelease,
			AppDeployerData existingAppDeployerData,
			List<String> applicationNamesToUpgrade, Release replacingRelease,
			AppDeployerData replacingAppDeployerData) {
		try {
			logger.info("Waiting for apps in release {}-v{} to be healthy.", replacingRelease.getName(),
					replacingRelease.getVersion());
			boolean healthy = this.isHealthy(replacingRelease, replacingAppDeployerData);

			if (healthy) {
				logger.info("Apps in release {}-v{} are healthy.", replacingRelease.getName(),
						replacingRelease.getVersion());
				logger.info("Deleting changed applications from previous release {}-v{}",
						existingRelease.getName(),
						existingRelease.getVersion());

				this.deleteStep.delete(existingRelease, existingAppDeployerData, applicationNamesToUpgrade);

				// Update Status in DB
				Status status = new Status();
				status.setStatusCode(StatusCode.DEPLOYED);
				replacingRelease.getInfo().setStatus(status);
				replacingRelease.getInfo().setDescription("Upgrade complete");
				this.releaseRepository.save(replacingRelease);
				logger.info("Release {}-v{} has been DEPLOYED", replacingRelease.getName(),
						replacingRelease.getVersion());
			}
			else {
				logger.error("New release " + replacingRelease.getName() + " was not detected as healthy." +
						"Keeping existing releases.");
				Status status = new Status();
				status.setStatusCode(StatusCode.DEPLOYED);
				replacingRelease.getInfo().setStatus(status);
				replacingRelease.getInfo().setStatus(status);
				replacingRelease.getInfo().setDescription("Did not detect apps in release as healthy.");
				this.releaseRepository.save(replacingRelease);
			}
		}
		catch (Exception e) {
			logger.error("Error upgrading new app", e);
			// Update Status in DB
			Status status = new Status();
			status.setStatusCode(StatusCode.FAILED);
			replacingRelease.getInfo().setStatus(status);
			replacingRelease.getInfo().setDescription("Failed upgrade. Exception = [" +
					e.getClass().getName() + "], Message = [" + e.getMessage() + "]");
			this.releaseRepository.save(replacingRelease);
		}
		return null;
	}

	private boolean isHealthy(Release replacingRelease, AppDeployerData replacingAppDeployerData) {
		boolean isHealthy = false;
		long timeout = System.currentTimeMillis() + this.healthCheckProperties.getTimeoutInMillis();
		Map<String, String> appNamesAndDeploymentIds = replacingAppDeployerData.getDeploymentDataAsMap();

		while (!isHealthy && System.currentTimeMillis() < timeout) {
			logger.debug("Health check for replacing release {}-v{}, sleeping for {} milliseconds.",
					replacingRelease.getName(),
					replacingRelease.getVersion(),
					this.healthCheckProperties.getTimeoutInMillis());
			sleep();
			AppDeployer appDeployer = this.deployerRepository.findByNameRequired(replacingRelease.getPlatformName())
					.getAppDeployer();

			logger.debug("Getting status for apps in replacing release {}-v{}", replacingRelease.getName(),
					replacingRelease.getVersion());
			for (Map.Entry<String, String> appNameAndDeploymentId : appNamesAndDeploymentIds.entrySet()) {
				AppStatus status = appDeployer.status(appNameAndDeploymentId.getValue());
				if (status.getState() == DeploymentState.deployed) {
					isHealthy = true;
				}
			}
		}
		return isHealthy;
	}

	private void sleep() {
		try {
			Thread.currentThread().sleep(healthCheckProperties.getSleepInMillis());
		}
		catch (InterruptedException e) {
			logger.info("Interrupted exception ", e);
			Thread.currentThread().interrupt();
		}
	}
}
