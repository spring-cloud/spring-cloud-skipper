/*
 * Copyright 2017-2018 the original author or authors.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.operations.applications.ApplicationManifest;
import org.cloudfoundry.operations.applications.PushApplicationManifestRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.SkipperManifestKind;
import org.springframework.cloud.skipper.domain.Status;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.server.deployer.ReleaseAnalysisReport;
import org.springframework.cloud.skipper.server.deployer.ReleaseManager;
import org.springframework.cloud.skipper.server.domain.AppDeployerData;
import org.springframework.cloud.skipper.server.repository.AppDeployerDataRepository;
import org.springframework.cloud.skipper.server.repository.ReleaseRepository;
import org.springframework.cloud.skipper.server.util.ArgumentSanitizer;
import org.springframework.util.Assert;

import static org.springframework.cloud.skipper.deployer.cloudfoundry.CloudFoundryManifestApplicationDeployer.PUSH_REQUEST_TIMEOUT;
import static org.springframework.cloud.skipper.deployer.cloudfoundry.CloudFoundryManifestApplicationDeployer.STAGING_TIMEOUT;
import static org.springframework.cloud.skipper.deployer.cloudfoundry.CloudFoundryManifestApplicationDeployer.STARTUP_TIMEOUT;
import static org.springframework.cloud.skipper.deployer.cloudfoundry.CloudFoundryManifestApplicationDeployer.isNotFoundError;

/**
 * A ReleaseManager implementation that uses an CF manifest based deployer.
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 * @author Janne Valkealahti
 */
public class CloudFoundryReleaseManager implements ReleaseManager {

	public static final String SPRING_CLOUD_DEPLOYER_COUNT = "spring.cloud.deployer.count";

	private static final Logger logger = LoggerFactory.getLogger(CloudFoundryReleaseManager.class);

	private final ReleaseRepository releaseRepository;

	private final AppDeployerDataRepository appDeployerDataRepository;

	private final CloudFoundryReleaseAnalyzer cloudFoundryReleaseAnalyzer;

	private final PlatformCloudFoundryOperations platformCloudFoundryOperations;

	private final CloudFoundryManifestApplicationDeployer cfManifestApplicationDeployer;

	public CloudFoundryReleaseManager(ReleaseRepository releaseRepository,
			AppDeployerDataRepository appDeployerDataRepository,
			CloudFoundryReleaseAnalyzer cloudFoundryReleaseAnalyzer,
			PlatformCloudFoundryOperations platformCloudFoundryOperations,
			CloudFoundryManifestApplicationDeployer cfManifestApplicationDeployer
			) {
		this.releaseRepository = releaseRepository;
		this.appDeployerDataRepository = appDeployerDataRepository;
		this.cloudFoundryReleaseAnalyzer = cloudFoundryReleaseAnalyzer;
		this.platformCloudFoundryOperations = platformCloudFoundryOperations;
		this.cfManifestApplicationDeployer = cfManifestApplicationDeployer;
	}

	@Override
	public Collection<String> getSupportedKinds() {
		return Arrays.asList(SkipperManifestKind.CloudFoundryApplication.name());
	}
	
	public Release install(Release newRelease) {
		Release release = this.releaseRepository.save(newRelease);
		ApplicationManifest applicationManifest = this.cfManifestApplicationDeployer.getCFApplicationManifest(release);
		Assert.isTrue(applicationManifest != null, "CF Application Manifest must be set");
		logger.debug("Manifest = " + ArgumentSanitizer.sanitizeYml(newRelease.getManifest().getData()));
		// Deploy the application
		String applicationName = applicationManifest.getName();
		Map<String, String> appDeploymentData = new HashMap<>();
		appDeploymentData.put(applicationManifest.getName(), applicationManifest.toString());
		this.platformCloudFoundryOperations.getCloudFoundryOperations(newRelease.getPlatformName())
				.applications().pushManifest(
				PushApplicationManifestRequest.builder()
						.manifest(applicationManifest)
						.stagingTimeout(STAGING_TIMEOUT)
						.startupTimeout(STARTUP_TIMEOUT)
						.build())
				.doOnSuccess(v -> logger.info("Done uploading bits for {}", applicationName))
				.doOnError(e -> logger.error(
						String.format("Error creating app %s.  Exception Message %s", applicationName,
								e.getMessage())))
				.timeout(PUSH_REQUEST_TIMEOUT)
				.doOnSuccess(item -> {
					logger.info("Successfully deployed {}", applicationName);
					saveAppDeployerData(release, appDeploymentData);

					// Update Status in DB
					updateInstallComplete(release);
				})
				.doOnError(error -> {
					if (isNotFoundError().test(error)) {
						logger.warn("Unable to deploy application. It may have been destroyed before start completed: " + error.getMessage());
					}
					else {
						logger.error(String.format("Failed to deploy %s", applicationName));
					}
				})
				.block();
		// Store updated state in in DB and compute status
		return status(this.releaseRepository.save(release));
	}

	private void updateInstallComplete(Release release) {
		Status status = new Status();
		status.setStatusCode(StatusCode.DEPLOYED);
		release.getInfo().setStatus(status);
		release.getInfo().setDescription("Install complete");
	}

	private void saveAppDeployerData(Release release, Map<String, String> appNameDeploymentIdMap) {
		AppDeployerData appDeployerData = new AppDeployerData();
		appDeployerData.setReleaseName(release.getName());
		appDeployerData.setReleaseVersion(release.getVersion());
		appDeployerData.setDeploymentDataUsingMap(appNameDeploymentIdMap);
		this.appDeployerDataRepository.save(appDeployerData);
	}

	@Override
	public ReleaseAnalysisReport createReport(Release existingRelease, Release replacingRelease, boolean initial) {
		ReleaseAnalysisReport releaseAnalysisReport = this.cloudFoundryReleaseAnalyzer
				.analyze(existingRelease, replacingRelease);
		if (initial) {
			this.releaseRepository.save(replacingRelease);
		}
		return new ReleaseAnalysisReport(releaseAnalysisReport.getApplicationNamesToUpgrade(),
				releaseAnalysisReport.getReleaseDifference(), existingRelease, replacingRelease);
	}

	public Release status(Release release) {
			release.getInfo().getStatus().setPlatformStatusAsAppStatusList(
					Collections.singletonList(this.cfManifestApplicationDeployer.status(release)));
		return release;
	}

	public Release delete(Release release) {
		this.releaseRepository.save(this.cfManifestApplicationDeployer.delete(release));
		return release;
	}

}
