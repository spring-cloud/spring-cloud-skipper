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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.Status;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.server.deployer.ReleaseManager;
import org.springframework.cloud.skipper.server.domain.AppDeployerData;
import org.springframework.cloud.skipper.server.repository.AppDeployerDataRepository;
import org.springframework.cloud.skipper.server.repository.DeployerRepository;
import org.springframework.cloud.skipper.server.repository.ReleaseRepository;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responsible for taking action based on the health of the latest deployed release. If
 * healthy, then delete applications from the previous release. Otherwise delete the
 * latest deployed release. Delegates to {@link DeleteStep}.
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
public class HandleHealthCheckStep {

	private final Logger logger = LoggerFactory.getLogger(HandleHealthCheckStep.class);

	private final ReleaseRepository releaseRepository;

	private final DeployerRepository deployerRepository;

	private final AppDeployerDataRepository appDeployerDataRepository;

	private final DeleteStep deleteStep;

	private final HealthCheckProperties healthCheckProperties;

	private ReleaseManager releaseManager;

	public HandleHealthCheckStep(ReleaseRepository releaseRepository,
			DeployerRepository deployerRepository,
			AppDeployerDataRepository appDeployerDataRepository,
			DeleteStep deleteStep,
			HealthCheckProperties healthCheckProperties) {
		this.releaseRepository = releaseRepository;
		this.deployerRepository = deployerRepository;
		this.appDeployerDataRepository = appDeployerDataRepository;
		this.deleteStep = deleteStep;
		this.healthCheckProperties = healthCheckProperties;
	}

	@Transactional
	public void handleHealthCheck(boolean healthy, Release existingRelease,
			List<String> applicationNamesToUpgrade,
			Release replacingRelease) {
		if (healthy) {
			updateReplacingReleaseState(replacingRelease);
			deleteExistingRelease(existingRelease, applicationNamesToUpgrade);
		}
		else {
			deleteReplacingRelease(replacingRelease);
		}
	}

	private void updateReplacingReleaseState(Release replacingRelease) {
		// Update Status in DB
		Status status = new Status();
		status.setStatusCode(StatusCode.DEPLOYED);
		replacingRelease.getInfo().setStatus(status);
		replacingRelease.getInfo().setDescription("Upgrade complete");
		this.releaseRepository.save(replacingRelease);
		logger.info("Release {}-v{} has been DEPLOYED", replacingRelease.getName(),
				replacingRelease.getVersion());
		logger.info("Apps in release {}-v{} are healthy.", replacingRelease.getName(),
				replacingRelease.getVersion());
	}

	private void deleteReplacingRelease(Release replacingRelease) {
		try {
			logger.error("New release " + replacingRelease.getName() + " was not detected as healthy after " +
					this.healthCheckProperties.getTimeoutInMillis() + "milliseconds.  " +
					"Keeping existing release, and Deleting apps of replacing release");
			this.releaseManager.delete(replacingRelease);
			Status status = new Status();
			status.setStatusCode(StatusCode.FAILED);
			replacingRelease.getInfo().setStatus(status);
			replacingRelease.getInfo().setStatus(status);
			replacingRelease.getInfo().setDescription("Did not detect apps in repalcing release as healthy after " +
					this.healthCheckProperties.getSleepInMillis() + " ms.");
			this.releaseRepository.save(replacingRelease);
		}
		catch (Exception e) {
			// Update Status in DB
			Status status = new Status();
			status.setStatusCode(StatusCode.FAILED);
			replacingRelease.getInfo().setStatus(status);
			replacingRelease.getInfo().setDescription("Could not delete replacing release application, " +
					"Manual intervention needed.  Sorry it didn't work out.");
			this.releaseRepository.save(replacingRelease);
			logger.info("Release {}-v{} could not be deleted.", replacingRelease.getName(),
					replacingRelease.getVersion());
		}
	}

	private void deleteExistingRelease(Release existingRelease, List<String> applicationNamesToUpgrade) {
		try {
			AppDeployerData existingAppDeployerData = this.appDeployerDataRepository
					.findByReleaseNameAndReleaseVersionRequired(
							existingRelease.getName(), existingRelease.getVersion());
			logger.info("Deleting changed applications from existing release {}-v{}",
					existingRelease.getName(),
					existingRelease.getVersion());
			this.deleteStep.delete(existingRelease, existingAppDeployerData, applicationNamesToUpgrade);
		}
		catch (Exception e) {
			// Update Status in DB
			Status status = new Status();
			status.setStatusCode(StatusCode.FAILED);
			existingRelease.getInfo().setStatus(status);
			existingRelease.getInfo().setDescription("Could not delete existing application, " +
					"manual intervention needed.  Sorry it didn't work out.");
			this.releaseRepository.save(existingRelease);
			logger.info("Release {}-v{} could not be deleted.", existingRelease.getName(),
					existingRelease.getVersion());
		}
	}

	@EventListener
	public void initialize(ApplicationReadyEvent event) {
		// NOTE circular ref will go away with introduction of state machine.
		this.releaseManager = event.getApplicationContext().getBean(ReleaseManager.class);
	}
}
