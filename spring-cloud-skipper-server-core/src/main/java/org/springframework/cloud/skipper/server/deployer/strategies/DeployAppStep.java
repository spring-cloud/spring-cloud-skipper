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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.Status;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.server.deployer.AppDeploymentRequestFactory;
import org.springframework.cloud.skipper.server.deployer.ReleaseAnalysisReport;
import org.springframework.cloud.skipper.server.domain.AppDeployerData;
import org.springframework.cloud.skipper.server.domain.SpringBootAppKind;
import org.springframework.cloud.skipper.server.domain.SpringBootAppKindReader;
import org.springframework.cloud.skipper.server.repository.AppDeployerDataRepository;
import org.springframework.cloud.skipper.server.repository.DeployerRepository;
import org.springframework.cloud.skipper.server.repository.ReleaseRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responsible for taking the ReleaseAnalysisReport and deploying the apps in the
 * replacing release. Step operates in it's own transaction, catches all exceptions so
 * always commits.
 * @author Mark Pollack
 */
public class DeployAppStep {

	private final DeployerRepository deployerRepository;

	private final AppDeploymentRequestFactory appDeploymentRequestFactory;

	private final AppDeployerDataRepository appDeployerDataRepository;

	private final ReleaseRepository releaseRepository;

	public DeployAppStep(DeployerRepository deployerRepository, AppDeploymentRequestFactory appDeploymentRequestFactory,
			AppDeployerDataRepository appDeployerDataRepository, ReleaseRepository releaseRepository) {
		this.deployerRepository = deployerRepository;
		this.appDeploymentRequestFactory = appDeploymentRequestFactory;
		this.appDeployerDataRepository = appDeployerDataRepository;
		this.releaseRepository = releaseRepository;
	}

	@Transactional
	public List<String> deployApps(Release existingRelease, Release replacingRelease,
			ReleaseAnalysisReport releaseAnalysisReport) {
		List<String> applicationNamesToUpgrade = new ArrayList<>();
		try {
			applicationNamesToUpgrade = releaseAnalysisReport.getApplicationNamesToUpgrade();
			AppDeployer appDeployer = this.deployerRepository.findByNameRequired(replacingRelease.getPlatformName())
					.getAppDeployer();

			// Deploy the application
			Map<String, String> appNameDeploymentIdMap = deploy(replacingRelease, applicationNamesToUpgrade,
					appDeployer);

			// Carry over the applicationDeployment information for apps that were not updated.
			carryOverAppDeploymentIds(existingRelease, appNameDeploymentIdMap);

			AppDeployerData appDeployerData = new AppDeployerData();
			appDeployerData.setReleaseName(replacingRelease.getName());
			appDeployerData.setReleaseVersion(replacingRelease.getVersion());
			appDeployerData.setDeploymentDataUsingMap(appNameDeploymentIdMap);
			this.appDeployerDataRepository.save(appDeployerData);
		}
		catch (Exception e) {
			Status status = new Status();
			status.setStatusCode(StatusCode.FAILED);
			replacingRelease.getInfo().setStatus(status);
			replacingRelease.getInfo().setStatus(status);
			replacingRelease.getInfo().setDescription("Could not deploy app.");
			this.releaseRepository.save(replacingRelease);
			// TODO If app 2 of 3 failed, what to do with the previous app that deployed?
		}
		return applicationNamesToUpgrade;
	}

	private void carryOverAppDeploymentIds(Release existingRelease, Map<String, String> appNameDeploymentIdMap) {
		AppDeployerData existingAppDeployerData = this.appDeployerDataRepository
				.findByReleaseNameAndReleaseVersionRequired(
						existingRelease.getName(), existingRelease.getVersion());
		Map<String, String> existingAppNamesAndDeploymentIds = existingAppDeployerData.getDeploymentDataAsMap();

		for (Map.Entry<String, String> existingEntry : existingAppNamesAndDeploymentIds.entrySet()) {
			String existingName = existingEntry.getKey();
			if (!appNameDeploymentIdMap.containsKey(existingName)) {
				appNameDeploymentIdMap.put(existingName, existingEntry.getValue());
			}
		}
	}

	private Map<String, String> deploy(Release replacingRelease, List<String> applicationNamesToUpgrade,
			AppDeployer appDeployer) {
		List<SpringBootAppKind> springBootAppKindList = SpringBootAppKindReader
				.read(replacingRelease.getManifest());

		Map<String, String> appNameDeploymentIdMap = new HashMap<>();
		for (SpringBootAppKind springBootAppKind : springBootAppKindList) {
			if (applicationNamesToUpgrade.contains(springBootAppKind.getApplicationName())) {
				AppDeploymentRequest appDeploymentRequest = appDeploymentRequestFactory.createAppDeploymentRequest(
						springBootAppKind, replacingRelease.getName(),
						String.valueOf(replacingRelease.getVersion()));
				// =============
				// DEPLOY DEPLOY
				// =============
				String deploymentId = appDeployer.deploy(appDeploymentRequest);
				appNameDeploymentIdMap.put(springBootAppKind.getApplicationName(), deploymentId);
			}
		}
		return appNameDeploymentIdMap;
	}
}
