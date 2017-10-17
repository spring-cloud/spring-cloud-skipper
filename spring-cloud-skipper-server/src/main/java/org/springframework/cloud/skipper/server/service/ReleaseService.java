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

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.skipper.ReleaseNotFoundException;
import org.springframework.cloud.skipper.SkipperException;
import org.springframework.cloud.skipper.domain.Info;
import org.springframework.cloud.skipper.domain.InstallProperties;
import org.springframework.cloud.skipper.domain.InstallRequest;
import org.springframework.cloud.skipper.domain.Package;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.Status;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.domain.UpgradeProperties;
import org.springframework.cloud.skipper.domain.UpgradeRequest;
import org.springframework.cloud.skipper.server.deployer.ReleaseAnalyzer;
import org.springframework.cloud.skipper.server.deployer.ReleaseManager;
import org.springframework.cloud.skipper.server.repository.DeployerRepository;
import org.springframework.cloud.skipper.server.repository.PackageMetadataRepository;
import org.springframework.cloud.skipper.server.repository.ReleaseRepository;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Service responsible for the lifecycle of packages and releases, install/delete a
 * package, upgrade/rollback a release, and get status on a release.
 *
 * It handles the validation of requests, retrieval of metadata and release information,
 * as well as merging of yaml files in a template. Delegates to a {@link ReleaseManager}
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
public class ReleaseService {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private final PackageMetadataRepository packageMetadataRepository;

	private final ReleaseRepository releaseRepository;

	private final PackageService packageService;

	private final ReleaseManager releaseManager;

	private final DeployerRepository deployerRepository;

	private ReleaseAnalyzer releaseAnalyzer;

	public ReleaseService(PackageMetadataRepository packageMetadataRepository,
			ReleaseRepository releaseRepository,
			PackageService packageService,
			ReleaseManager releaseManager,
			DeployerRepository deployerRepository,
			ReleaseAnalyzer releaseAnalyzer) {
		this.packageMetadataRepository = packageMetadataRepository;
		this.releaseRepository = releaseRepository;
		this.packageService = packageService;
		this.releaseManager = releaseManager;
		this.deployerRepository = deployerRepository;
		this.releaseAnalyzer = releaseAnalyzer;
	}

	/**
	 * Downloads the package metadata and package zip file specified by the given Id and
	 * deploys the package on the target platform.
	 * @param id of the package
	 * @param installProperties contains the name of the release, the platfrom to install to,
	 * and configuration values to replace in the package template.
	 * @return the Release object associated with this deployment
	 * @throws SkipperException if the package to install can not be found.
	 */
	public Release install(String id, InstallProperties installProperties) {
		Assert.notNull(installProperties, "Deploy properties can not be null");
		Assert.hasText(id, "Package id can not be null");
		PackageMetadata packageMetadata = this.packageMetadataRepository.findOne(id);
		if (packageMetadata == null) {
			throw new SkipperException(String.format("Package with id='%s' can not be found.", id));
		}
		return install(packageMetadata, installProperties);
	}

	/**
	 * Downloads the package metadata and package zip file specified by PackageIdentifier
	 * property of the DeploymentRequest. Deploys the package on the target platform.
	 *
	 * @param installRequest the install request
	 * @return the Release object associated with this deployment
	 */
	public Release install(InstallRequest installRequest) {
		validateInstallRequest(installRequest);
		PackageIdentifier packageIdentifier = installRequest.getPackageIdentifier();
		String packageVersion = packageIdentifier.getPackageVersion();
		String packageName = packageIdentifier.getPackageName();
		PackageMetadata packageMetadata;
		if (!StringUtils.hasText(packageVersion)) {
			List<PackageMetadata> packageMetadataList = this.packageMetadataRepository.findByName(packageName);
			if (packageMetadataList.size() == 1) {
				packageMetadata = packageMetadataList.get(0);
			}
			else if (packageMetadataList == null) {
				throw new SkipperException("Can not find a package named " + packageName);
			}
			else {
				packageMetadata = this.packageMetadataRepository.findFirstByNameOrderByVersionDesc(packageName);
			}
		}
		else {
			packageMetadata = getPackageMetadata(packageName, packageVersion);
		}
		return install(packageMetadata, installRequest.getInstallProperties());
	}

	PackageMetadata getPackageMetadata(String packageName, String packageVersion) {
		PackageMetadata packageMetadata = this.packageMetadataRepository.findByNameAndVersion(packageName,
				packageVersion);
		if (packageMetadata == null) {
			throw new SkipperException(String.format("Can not find package '%s', version '%s'",
					packageName, packageVersion));
		}
		return packageMetadata;
	}

	private void validateInstallRequest(InstallRequest installRequest) {
		Assert.notNull(installRequest.getInstallProperties(), "Install properties must not be null");
		Assert.isTrue(StringUtils.hasText(installRequest.getInstallProperties().getPlatformName()),
				"Platform name must not be empty");
		Assert.isTrue(StringUtils.hasText(installRequest.getInstallProperties().getReleaseName()),
				"Release name must not be empty");
		Assert.notNull(installRequest.getPackageIdentifier(), "Package identifier must not be null");
		Assert.isTrue(StringUtils.hasText(installRequest.getPackageIdentifier().getPackageName()),
				"Package name must not be empty");
		try {
			Release release = this.releaseRepository.findLatestRelease(installRequest.getInstallProperties()
					.getReleaseName());
			throw new SkipperException("Release with the name [" + release.getName() + "] already exists.");
		}
		catch (ReleaseNotFoundException e) {
			// ignore as this is expected.
		}
	}

	protected Release install(PackageMetadata packageMetadata, InstallProperties installProperties) {
		Assert.notNull(packageMetadata, "Can't download package, PackageMetadata is a null value.");
		Release release = createInitialRelease(installProperties, this.packageService.downloadPackage(packageMetadata));
		return install(release);
	}

	protected Release install(Release release) {
		Map<String, Object> mergedMap = ConfigValueUtils.mergeConfigValues(release.getPkg(), release.getConfigValues());
		// Render yaml resources
		String manifest = ManifestUtils.createManifest(release.getPkg(), mergedMap);
		logger.debug("Manifest = " + manifest);
		release.setManifest(manifest);
		// Deployment
		Release releaseToReturn = this.releaseManager.install(release);
		return releaseToReturn;
	}

	public Release delete(String releaseName) {
		Assert.notNull(releaseName, "Release name must not be null");
		Release release = this.releaseRepository.findLatestRelease(releaseName);
		return this.releaseManager.delete(release);
	}

	public Info status(String releaseName) {
		Release release = this.releaseRepository.findTopByNameOrderByVersionDesc(releaseName);
		if (release == null) {
			throw new ReleaseNotFoundException(releaseName);
		}
		release = status(release);
		// TODO check contract for status wrt to returning null.
		if (release == null) {
			throw new ReleaseNotFoundException(releaseName);
		}
		return release.getInfo();
	}

	public Info status(String releaseName, Integer version) {
		return status(this.releaseRepository.findByNameAndVersion(releaseName, version)).getInfo();
	}

	public String manifest(String releaseName) {
		Release release = this.releaseRepository.findTopByNameOrderByVersionDesc(releaseName);
		if (release == null) {
			throw new ReleaseNotFoundException(releaseName);
		}
		return release.getManifest();
	}

	public String manifest(String releaseName, Integer version) {
		return this.releaseRepository.findByNameAndVersion(releaseName, version).getManifest();
	}

	public Release status(Release release) {
		return this.releaseManager.status(release);
	}

	public Release upgrade(UpgradeRequest upgradeRequest) {
		Assert.notNull(upgradeRequest.getUpgradeProperties(), "UpgradeProperties can not be null");
		Assert.notNull(upgradeRequest.getPackageIdentifier(), "PackageIdentifier can not be null");
		UpgradeProperties upgradeProperties = upgradeRequest.getUpgradeProperties();
		Release oldRelease = this.releaseRepository.findLatestRelease(upgradeProperties.getReleaseName());
		PackageIdentifier packageIdentifier = upgradeRequest.getPackageIdentifier();
		// todo: search multi repository
		PackageMetadata packageMetadata = getPackageMetadata(packageIdentifier.getPackageName(), packageIdentifier
				.getPackageVersion());
		Release newRelease = createReleaseForUpgrade(packageMetadata, oldRelease.getVersion() + 1, upgradeProperties,
				oldRelease.getPlatformName());
		Map<String, Object> model = ConfigValueUtils.mergeConfigValues(newRelease.getPkg(),
				newRelease.getConfigValues());
		String manifest = ManifestUtils.createManifest(newRelease.getPkg(), model);
		newRelease.setManifest(manifest);
		return upgrade(oldRelease, newRelease);
	}

	public Release createReleaseForUpgrade(PackageMetadata packageMetadata, Integer newVersion,
			UpgradeProperties upgradeProperties, String platformName) {
		Assert.notNull(upgradeProperties, "Upgrade Properties can not be null");
		Package packageToInstall = this.packageService.downloadPackage(packageMetadata);
		Release release = new Release();
		release.setName(upgradeProperties.getReleaseName());
		release.setPlatformName(platformName);
		release.setConfigValues(upgradeProperties.getConfigValues());
		release.setPkg(packageToInstall);
		release.setVersion(newVersion);
		Info info = createNewInfo("Upgrade install underway");
		release.setInfo(info);
		return release;
	}

	protected Info createNewInfo(String description) {
		Info info = new Info();
		info.setFirstDeployed(new Date());
		info.setLastDeployed(new Date());
		Status status = new Status();
		status.setStatusCode(StatusCode.UNKNOWN);
		info.setStatus(status);
		info.setDescription(description);
		return info;
	}

	protected Info createNewInfo() {
		return createNewInfo("Initial install underway");
	}

	public Release upgrade(Release existingRelease, Release replacingRelease) {
		Assert.notNull(existingRelease, "Existing Release must not be null");
		Assert.notNull(replacingRelease, "Replacing Release must not be null");

		Release release = this.releaseManager.upgrade(existingRelease, replacingRelease, "simple");

		return status(release);
	}

	/**
	 * Rollback the release name to the specified version. If the version is 0, then rollback
	 * to the previous release.
	 *
	 * @param releaseName the name of the release
	 * @param rollbackVersion the version of the release to rollback to
	 * @return the Release
	 */
	public Release rollback(final String releaseName, final int rollbackVersion) {
		Assert.notNull(releaseName, "Release name must not be null");
		Assert.isTrue(rollbackVersion >= 0,
				"Rollback version can not be less than zero.  Value = " + rollbackVersion);

		Release currentRelease = this.releaseRepository.findLatestRelease(releaseName);
		Assert.notNull(currentRelease, "Could not find release = [" + releaseName + "]");

		int rollbackVersionToUse = rollbackVersion;
		if (rollbackVersion == 0) {
			rollbackVersionToUse = currentRelease.getVersion() - 1;
		}
		Assert.isTrue(rollbackVersionToUse != 0, "Can not rollback to before version 1");

		Release releaseToRollback = this.releaseRepository.findByNameAndVersion(releaseName, rollbackVersionToUse);
		Assert.notNull(releaseToRollback, "Could not find Release to rollback to [releaseName,releaseVersion] = ["
				+ releaseName + "," + rollbackVersionToUse + "]");

		logger.info("Rolling back releaseName={}.  Current version={}, Target version={}", releaseName,
				currentRelease.getVersion(), rollbackVersionToUse);

		Release newRollbackRelease = new Release();
		newRollbackRelease.setName(releaseName);
		newRollbackRelease.setPkg(releaseToRollback.getPkg());
		newRollbackRelease.setManifest(releaseToRollback.getManifest());
		newRollbackRelease.setVersion(currentRelease.getVersion() + 1);
		newRollbackRelease.setPlatformName(releaseToRollback.getPlatformName());
		// Do not set ConfigValues since the manifest from the previous release has
		// already resolved those...
		newRollbackRelease.setInfo(createNewInfo());
		return upgrade(currentRelease, newRollbackRelease);
	}

	protected Release createInitialRelease(InstallProperties installProperties, Package packageToInstall) {
		Release release = new Release();
		release.setName(installProperties.getReleaseName());
		release.setPlatformName(installProperties.getPlatformName());
		release.setConfigValues(installProperties.getConfigValues());
		release.setPkg(packageToInstall);
		release.setVersion(1);
		Info info = createNewInfo();
		release.setInfo(info);
		validateInitialRelease(release);
		return release;
	}

	/**
	 * Do up front checks before deploying
	 * @param release the initial release object this data provided by the end user.
	 */
	protected void validateInitialRelease(Release release) {
		this.deployerRepository.findByNameRequired(release.getPlatformName());
	}

	/**
	 * List the history of versions for a given release.
	 *
	 * @param releaseName the release name of the release to search for
	 * @param maxRevisions the maximum number of revisions to get
	 * @return the list of all releases by the given name and revisions max.
	 */
	public List<Release> history(String releaseName, int maxRevisions) {
		return this.releaseRepository.findReleaseRevisions(releaseName, maxRevisions);
	}

	/**
	 * List the latest version of releases with status of deployed or failed.
	 *
	 * @param releaseNameLike the wildcard name of releases to search for
	 * @return the list of all matching releases
	 */
	public List<Release> list(String releaseNameLike) {
		return this.releaseRepository.findLatestDeployedOrFailed(releaseNameLike);
	}

	/**
	 * Get the latest revision of all releases with status of deployed or failed state.
	 *
	 * @return the list of all matching releases
	 */
	public List<Release> list() {
		return this.releaseRepository.findLatestDeployedOrFailed();
	}

}
