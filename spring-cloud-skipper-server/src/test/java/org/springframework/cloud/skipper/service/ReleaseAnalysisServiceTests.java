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

import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.AbstractIntegrationTest;
import org.springframework.cloud.skipper.deployer.Deployer;
import org.springframework.cloud.skipper.domain.ConfigValues;
import org.springframework.cloud.skipper.domain.InstallProperties;
import org.springframework.cloud.skipper.domain.InstallRequest;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.UpgradeProperties;
import org.springframework.cloud.skipper.domain.UpgradeRequest;
import org.springframework.cloud.skipper.repository.DeployerRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Pollack
 */
@ActiveProfiles("repo-test")
@TestPropertySource(properties = { "maven.remote-repositories.repo1.url=http://repo.spring.io/libs-snapshot" })
public class ReleaseAnalysisServiceTests extends AbstractIntegrationTest {

	@Autowired
	DeployerRepository deployerRepository;

	@Autowired
	ReleaseService releaseService;

	@Autowired
	ReleaseAnalysisService releaseAnalysisService;

	@Test
	public void test() {

		String platformName = "noopPlatform";
		Deployer deployer = new Deployer(platformName, "noop", new NoOpAppDeployer());
		deployerRepository.save(deployer);

		String releaseName = "logrelease";
		String packageName = "log";
		String packageVersion = "2.0.0";
		InstallProperties installProperties = new InstallProperties();
		installProperties.setReleaseName(releaseName);
		installProperties.setPlatformName(platformName);
		InstallRequest installRequest = new InstallRequest();
		installRequest.setInstallProperties(installProperties);

		PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName(packageName);
		packageIdentifier.setPackageVersion(packageVersion);
		installRequest.setPackageIdentifier(packageIdentifier);
		Release installedRelease = releaseService.install(installRequest);

		assertThat(installedRelease.getName()).isEqualTo(releaseName);
		System.out.println("installed release \n" + installedRelease.getManifest());

		UpgradeProperties upgradeProperties = new UpgradeProperties();
		ConfigValues configValues = new ConfigValues();
		// TODO must be a release that exists in a maven repo....
		configValues.setRaw("version: 1.2.0.RELEASE\n");
		upgradeProperties.setConfigValues(configValues);
		upgradeProperties.setReleaseName(releaseName);
		UpgradeRequest upgradeRequest = new UpgradeRequest();
		upgradeRequest.setUpgradeProperties(upgradeProperties);

		packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName(packageName);
		packageIdentifier.setPackageVersion(packageVersion);
		upgradeRequest.setPackageIdentifier(packageIdentifier);

		Release upgradedRelease = releaseService.upgrade(upgradeRequest);

		assertThat(upgradedRelease.getName()).isEqualTo(releaseName);
		System.out.println("upgraded relerase \n" + upgradedRelease.getManifest());
		ReleaseAnalysisReport releaseAnalysisReport = this.releaseAnalysisService.analyze(installedRelease,
				upgradedRelease);

	}

}
