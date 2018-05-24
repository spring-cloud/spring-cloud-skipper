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
package org.springframework.cloud.skipper.server.service;

import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.deployer.resource.support.DelegatingResourceLoader;
import org.springframework.cloud.deployer.resource.support.LRUCleaningResourceLoader;
import org.springframework.cloud.skipper.PackageDeleteException;
import org.springframework.cloud.skipper.ReleaseNotFoundException;
import org.springframework.cloud.skipper.SkipperException;
import org.springframework.cloud.skipper.domain.ConfigValues;
import org.springframework.cloud.skipper.domain.Info;
import org.springframework.cloud.skipper.domain.InstallProperties;
import org.springframework.cloud.skipper.domain.InstallRequest;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.Repository;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.domain.UpgradeProperties;
import org.springframework.cloud.skipper.domain.UpgradeRequest;
import org.springframework.cloud.skipper.server.AbstractIntegrationTest;
import org.springframework.cloud.skipper.server.repository.AppDeployerDataRepository;
import org.springframework.cloud.skipper.server.repository.PackageMetadataRepository;
import org.springframework.cloud.skipper.server.repository.RepositoryRepository;
import org.springframework.test.context.ActiveProfiles;

import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests ReleaseService methods.
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 * @author Glenn Renfro
 * @author Christian Tzolov
 */
@ActiveProfiles("repo-test")
public class ReleaseServiceTests extends AbstractIntegrationTest {

	private final Logger logger = LoggerFactory.getLogger(ReleaseServiceTests.class);

	@Autowired
	private PackageMetadataRepository packageMetadataRepository;

	@Autowired
	private AppDeployerDataRepository appDeployerDataRepository;

	@Autowired
	private DelegatingResourceLoader delegatingResourceLoader;

	@Autowired
	private RepositoryRepository repositoryRepository;

	@After
	public void afterTests() {
		Repository repo = this.repositoryRepository.findByName("test");
		repo.setLocal(false);
		this.repositoryRepository.save(repo);
	}

	@Test
	public void testResourceLoaderInstance() {
		assertThat(this.delegatingResourceLoader).isNotNull();
		assertThat(this.delegatingResourceLoader instanceof LRUCleaningResourceLoader).isTrue();
	}

	@Test
	public void testBadArguments() {
		assertThatThrownBy(() -> releaseService.install(123L, new InstallProperties()))
				.isInstanceOf(SkipperException.class)
				.hasMessageContaining("can not be found");

		assertThatThrownBy(() -> releaseService.install(123L, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Deploy properties can not be null");

		assertThatThrownBy(() -> releaseService.install((Long) null, new InstallProperties()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Package id can not be null");

		assertThatThrownBy(() -> releaseService.delete(null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testInstallAndUpdatePackageNotFound() throws InterruptedException {
		String releaseName = "logrelease";
		InstallRequest installRequest = new InstallRequest();
		installRequest.setInstallProperties(createInstallProperties(releaseName));
		PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName("log");
		packageIdentifier.setPackageVersion("1.0.0");
		installRequest.setPackageIdentifier(packageIdentifier);
		Release release = install(installRequest);
		installRequest.setPackageIdentifier(packageIdentifier);
		assertThat(release).isNotNull();
		assertThat(release.getPkg().getMetadata().getVersion()).isEqualTo("1.0.0");
		Info info = this.releaseService.status(releaseName);
		assertThat(info).isNotNull();

		UpgradeProperties upgradeProperties = new UpgradeProperties();
		upgradeProperties.setReleaseName(releaseName);
		UpgradeRequest upgradeRequest = new UpgradeRequest();
		upgradeRequest.setUpgradeProperties(upgradeProperties);
		packageIdentifier = new PackageIdentifier();
		String packageName = "random";
		String packageVersion = "1.0.0";
		packageIdentifier.setPackageName(packageName);
		packageIdentifier.setPackageVersion(packageVersion);
		upgradeRequest.setPackageIdentifier(packageIdentifier);
		try {
			upgrade(upgradeRequest);
			fail("Expected to throw SkipperException");
		}
		catch (SkipperException e) {
			assertThat(e.getMessage()).isEqualTo(String.format("Can not find package '%s', version '%s'",
					packageName, packageVersion));
		}

		delete(release.getName());
	}

	@Test
	public void testInstallByLatestPackage() throws InterruptedException {
		InstallRequest installRequest = new InstallRequest();
		installRequest.setInstallProperties(createInstallProperties("latestPackage"));
		PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName("log");
		installRequest.setPackageIdentifier(packageIdentifier);
		Release release = install(installRequest);
		assertThat(release).isNotNull();
		assertThat(release.getPkg().getMetadata().getVersion()).isEqualTo("2.0.0");
		delete(release.getName());

	}

	@Test(expected = ReleaseNotFoundException.class)
	public void testStatusReleaseDoesNotExist() {
		releaseService.status("notexist");
	}

	@Test
	public void testPackageNotFound() {
		boolean exceptionFired = false;
		try {
			this.packageMetadataRepository.findByNameAndOptionalVersionRequired("random", "1.2.4");
		}
		catch (SkipperException se) {
			assertThat(se.getMessage()).isEqualTo("Can not find package 'random', version '1.2.4'");
			exceptionFired = true;
		}
		assertThat(exceptionFired).isTrue();
	}

	@Test
	public void testInstallPackageNotFound() {
		InstallRequest installRequest = new InstallRequest();
		installRequest.setInstallProperties(createInstallProperties("latestPackage"));
		PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName("random");
		installRequest.setPackageIdentifier(packageIdentifier);
		try {
			releaseService.install(installRequest);
			fail("SkipperException is expected for non existing package");
		}
		catch (Exception se) {
			assertThat(se.getMessage()).isEqualTo("Can not find a package named 'random'");
		}
	}

	@Test
	public void testLatestPackageByName() {
		String packageName = "log";
		PackageMetadata packageMetadata = this.packageMetadataRepository.findFirstByNameOrderByVersionDesc(packageName);
		PackageMetadata latestPackageMetadata = this.packageMetadataRepository
				.findByNameAndOptionalVersionRequired(packageName, null);
		assertThat(packageMetadata).isEqualTo(latestPackageMetadata);
	}

	@Test
	public void testInstallReleaseThatIsNotDeleted() throws InterruptedException {
		String releaseName = "installDeployedRelease";
		InstallRequest installRequest = new InstallRequest();
		installRequest.setInstallProperties(createInstallProperties(releaseName));
		PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName("log");
		packageIdentifier.setPackageVersion("1.0.0");
		installRequest.setPackageIdentifier(packageIdentifier);
		Release release = install(installRequest);
		assertThat(release).isNotNull();

		// Now let's install it a second time.
		try {
			install(installRequest);
			fail("Expected to fail when installing already deployed release.");
		}
		catch (SkipperException e) {
			assertThat(e.getMessage()).isEqualTo("Release with the name [" + releaseName + "] already exists "
					+ "and it is not deleted.");
		}
	}

	@Test
	public void testInstallDeletedRelease() throws InterruptedException {
		String releaseName = "deletedRelease";
		InstallRequest installRequest = new InstallRequest();
		installRequest.setInstallProperties(createInstallProperties(releaseName));
		PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName("log");
		packageIdentifier.setPackageVersion("1.0.0");
		installRequest.setPackageIdentifier(packageIdentifier);
		// Install
		Release release = install(installRequest);
		assertThat(release).isNotNull();
		// Delete
		delete(releaseName);
		// Install again
		Release release2 = install(installRequest);
		assertThat(release2.getVersion()).isEqualTo(2);
	}

	@Test
	public void testDeletedReleaseWithPackage() throws InterruptedException {
		// Make the test repo Local
		Repository repo = this.repositoryRepository.findByName("test");
		repo.setLocal(true);
		this.repositoryRepository.save(repo);

		String releaseName = "deletedRelease";
		InstallRequest installRequest = new InstallRequest();
		installRequest.setInstallProperties(createInstallProperties(releaseName));
		PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName("log");
		packageIdentifier.setPackageVersion("1.0.0");
		installRequest.setPackageIdentifier(packageIdentifier);

		List<PackageMetadata> releasePackage = this.packageMetadataRepository.findByNameAndVersionOrderByApiVersionDesc(
				packageIdentifier.getPackageName(), packageIdentifier.getPackageVersion());

		assertThat(releasePackage).isNotNull();
		assertThat(releasePackage.size()).isEqualTo(1);

		assertThat(this.packageMetadataRepository.findByName(packageIdentifier.getPackageName()).size()).isEqualTo(3);

		// Install
		Release release = install(installRequest);
		assertThat(release).isNotNull();
		// Delete
		delete(releaseName, true);

		assertThat(this.packageMetadataRepository.findByName(packageIdentifier.getPackageName()).size()).isEqualTo(0);
	}

	@Test
	public void testDeletedReleaseWithPackageNonLocalRepo() throws InterruptedException {
		// Make the test repo Non-local
		Repository repo = this.repositoryRepository.findByName("test");
		repo.setLocal(false);
		this.repositoryRepository.save(repo);

		String releaseName = "deletedRelease";
		InstallRequest installRequest = new InstallRequest();
		installRequest.setInstallProperties(createInstallProperties(releaseName));
		PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName("log");
		packageIdentifier.setPackageVersion("1.0.0");
		installRequest.setPackageIdentifier(packageIdentifier);

		assertThat(this.packageMetadataRepository.findByName(packageIdentifier.getPackageName()).size()).isEqualTo(3);

		// Install
		Release release = install(installRequest);
		assertThat(release).isNotNull();
		assertReleaseStatus(releaseName, StatusCode.DEPLOYED);

		// Delete attempt
		try {
			delete(releaseName, true);
			fail("Packages from non-local repositories can't be deleted");
		} catch (SkipperException se) {
		}
		assertReleaseStatus(releaseName, StatusCode.DEPLOYED);
		assertThat(this.packageMetadataRepository.findByName(packageIdentifier.getPackageName()).size()).isEqualTo(3);
	}

	@Test
	public void testInstallDeleteOfdMultipleReleasesFromSingePackage() throws InterruptedException {

		Repository repo = this.repositoryRepository.findByName("test");
		repo.setLocal(true);
		this.repositoryRepository.save(repo);

		boolean DELETE_RELEASE_PACKAGE = true;
		String RELEASE_ONE = "RELEASE_ONE";
		String RELEASE_TWO = "RELEASE_TWO";
		String RELEASE_THREE = "RELEASE_THREE";

		// 3 versions of package "log" exists
		PackageIdentifier logPackageIdentifier = new PackageIdentifier();
		logPackageIdentifier.setPackageName("log");
		logPackageIdentifier.setPackageVersion("1.0.0");

		List<PackageMetadata> releasePackage = this.packageMetadataRepository.findByNameAndVersionOrderByApiVersionDesc(
				logPackageIdentifier.getPackageName(), logPackageIdentifier.getPackageVersion());
		assertThat(releasePackage).isNotNull();
		assertThat(releasePackage.size()).isEqualTo(1);
		assertThat(this.packageMetadataRepository.findByName(logPackageIdentifier.getPackageName()).size()).isEqualTo(3);

		// Install 2 releases (RELEASE_ONE, RELEASE_TWO) from the same "log" package
		install(RELEASE_ONE, logPackageIdentifier);
		install(RELEASE_TWO, logPackageIdentifier);

		assertReleaseStatus(RELEASE_ONE, StatusCode.DEPLOYED);
		assertReleaseStatus(RELEASE_TWO, StatusCode.DEPLOYED);

		// Attempt to delete release one together with its package
		try {
			delete(RELEASE_ONE, DELETE_RELEASE_PACKAGE);
			fail("Attempt to delete a package with other deployed releases should fail");
		}
		catch (PackageDeleteException se) {
			assertThat(se.getMessage()).isEqualTo("Can not delete Package Metadata [log:1.0.0] in Repository [test]. " +
					"Not all releases of this package have the status DELETED. Active Releases [RELEASE_TWO]");
		}

		// Verify that neither the releases nor the package have been deleted
		assertReleaseStatus(RELEASE_ONE, StatusCode.DEPLOYED);
		assertReleaseStatus(RELEASE_TWO, StatusCode.DEPLOYED);
		assertThat(this.packageMetadataRepository.findByName(logPackageIdentifier.getPackageName()).size()).isEqualTo(3);

		// Install a third release (RELEASE_THREE) from the same package (log)
		install(RELEASE_THREE, logPackageIdentifier);
		assertReleaseStatus(RELEASE_THREE, StatusCode.DEPLOYED);

		// Attempt to delete release one together with its package
		try {
			delete(RELEASE_ONE, DELETE_RELEASE_PACKAGE);
			fail("Attempt to delete a package with other deployed releases must fail.");
		}
		catch (PackageDeleteException se) {
			assertThat(se.getMessage()).isEqualTo("Can not delete Package Metadata [log:1.0.0] in Repository [test]. " +
					"Not all releases of this package have the status DELETED. Active Releases [RELEASE_THREE,RELEASE_TWO]");
		}

		// Verify that nothing has been deleted
		assertReleaseStatus(RELEASE_ONE, StatusCode.DEPLOYED);
		assertReleaseStatus(RELEASE_TWO, StatusCode.DEPLOYED);
		assertReleaseStatus(RELEASE_THREE, StatusCode.DEPLOYED);
		assertThat(this.packageMetadataRepository.findByName(logPackageIdentifier.getPackageName()).size()).isEqualTo(3);

		// Delete releases two and three without without deleting their package.
		delete(RELEASE_TWO, !DELETE_RELEASE_PACKAGE);
		delete(RELEASE_THREE, !DELETE_RELEASE_PACKAGE);

		// Release One is still deployed
		assertReleaseStatus(RELEASE_ONE, StatusCode.DEPLOYED);

		// Releases Two and Three were undeployed
		assertReleaseStatus(RELEASE_TWO, StatusCode.DELETED);
		assertReleaseStatus(RELEASE_THREE, StatusCode.DELETED);

		// Package "log" still has 3 registered versions
		assertThat(this.packageMetadataRepository.findByName(logPackageIdentifier.getPackageName()).size()).isEqualTo(3);

		// Attempt to delete release one together with its package
		delete(RELEASE_ONE, DELETE_RELEASE_PACKAGE);

		// Successful deletion of release and its package.
		assertReleaseStatus(RELEASE_ONE, StatusCode.DELETED);
		assertThat(this.packageMetadataRepository.findByName(logPackageIdentifier.getPackageName()).size()).isEqualTo(0);
	}

	private Release install(String releaseName, PackageIdentifier packageIdentifier) throws InterruptedException {
		InstallRequest installRequest = new InstallRequest();
		installRequest.setPackageIdentifier(packageIdentifier);
		installRequest.setInstallProperties(createInstallProperties(releaseName));
		Release release = install(installRequest);
		assertThat(release).isNotNull();
		return release;
	}

	private void assertReleaseStatus(String releaseName, StatusCode expectedStatusCode) {
		assertThat(this.releaseRepository.findByNameIgnoreCaseContaining(releaseName).size()).isEqualTo(1);
		assertThat(this.releaseRepository.findByNameIgnoreCaseContaining(releaseName).iterator().next()
				.getInfo().getStatus().getStatusCode()).isEqualTo(expectedStatusCode);
	}

	@Test
	public void testRollbackDeletedRelease() throws InterruptedException {
		String releaseName = "rollbackDeletedRelease";
		InstallRequest installRequest = new InstallRequest();
		InstallProperties installProperties = createInstallProperties(releaseName);
		ConfigValues installConfig = new ConfigValues();
		installConfig.setRaw("log:\n  version: 1.2.0.RC1\ntime:\n  version: 1.2.0.RC1\n");
		installProperties.setConfigValues(installConfig);
		installRequest.setInstallProperties(installProperties);
		PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName("log");
		packageIdentifier.setPackageVersion("1.0.0");
		installRequest.setPackageIdentifier(packageIdentifier);
		// Install
		logger.info("Installing log 1.0.0 package");
		Release release = install(installRequest);
		assertThat(release).isNotNull();
		assertThat(release.getVersion()).isEqualTo(1);
		this.appDeployerDataRepository.findByReleaseNameAndReleaseVersionRequired(releaseName, 1);

		// Upgrade
		UpgradeProperties upgradeProperties = new UpgradeProperties();
		upgradeProperties.setReleaseName(releaseName);
		ConfigValues upgradeConfig = new ConfigValues();
		upgradeConfig.setRaw("log:\n  version: 1.2.0.RELEASE\ntime:\n  version: 1.2.0.RELEASE\n");
		upgradeProperties.setConfigValues(upgradeConfig);
		UpgradeRequest upgradeRequest = new UpgradeRequest();
		upgradeRequest.setUpgradeProperties(upgradeProperties);
		packageIdentifier = new PackageIdentifier();
		String packageName = "log";
		String packageVersion = "2.0.0";
		packageIdentifier.setPackageName(packageName);
		packageIdentifier.setPackageVersion(packageVersion);
		upgradeRequest.setPackageIdentifier(packageIdentifier);
		logger.info("Upgrading to log 2.0.0 package");
		Release upgradedRelease = upgrade(upgradeRequest);

		assertThat(upgradedRelease.getVersion()).isEqualTo(2);
		assertThat(upgradedRelease.getConfigValues().getRaw()).isEqualTo(upgradeRequest.getUpgradeProperties().getConfigValues().getRaw());
		this.appDeployerDataRepository.findByReleaseNameAndReleaseVersionRequired(releaseName, 2);

		// Delete
		delete(releaseName);

		Release deletedRelease = releaseRepository.findByNameAndVersion(releaseName, 2);
		assertThat(deletedRelease.getInfo().getStatus().getStatusCode().equals(StatusCode.DELETED));

		// Rollback
		logger.info("Rolling back the release " + release);

		Release rolledBackRelease = rollback(releaseName, 0);

		assertThat(rolledBackRelease.getManifest()).isEqualTo(release.getManifest());
		assertThat(rolledBackRelease.getConfigValues().getRaw()).isEqualTo(release.getConfigValues().getRaw());
		assertThat(rolledBackRelease.getInfo().getStatus().getStatusCode().equals(StatusCode.DEPLOYED));

		deletedRelease = releaseRepository.findByNameAndVersion(releaseName, 2);
		assertThat(deletedRelease.getInfo().getStatus().getStatusCode().equals(StatusCode.DELETED));

		delete(rolledBackRelease.getName());
	}

}
