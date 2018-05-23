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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import org.springframework.cloud.skipper.PackageDeleteException;
import org.springframework.cloud.skipper.ReleaseNotFoundException;
import org.springframework.cloud.skipper.SkipperException;
import org.springframework.cloud.skipper.domain.Info;
import org.springframework.cloud.skipper.domain.InstallProperties;
import org.springframework.cloud.skipper.domain.InstallRequest;
import org.springframework.cloud.skipper.domain.Manifest;
import org.springframework.cloud.skipper.domain.Package;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.server.deployer.ReleaseAnalysisReport;
import org.springframework.cloud.skipper.server.deployer.ReleaseManager;
import org.springframework.cloud.skipper.server.repository.DeployerRepository;
import org.springframework.cloud.skipper.server.repository.PackageMetadataRepository;
import org.springframework.cloud.skipper.server.repository.ReleaseRepository;
import org.springframework.cloud.skipper.server.util.ArgumentSanitizer;
import org.springframework.cloud.skipper.server.util.ConfigValueUtils;
import org.springframework.cloud.skipper.server.util.ManifestUtils;
import org.springframework.transaction.annotation.Transactional;
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
 * @author Glenn Renfro
 * @author Christian Tzolov
 */
public class ReleaseService {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private final PackageMetadataRepository packageMetadataRepository;

	private final ReleaseRepository releaseRepository;

	private final PackageService packageService;

	private final ReleaseManager releaseManager;

	private final DeployerRepository deployerRepository;

	private PackageMetadataService packageMetadataService;

	public ReleaseService(PackageMetadataRepository packageMetadataRepository,
			ReleaseRepository releaseRepository,
			PackageService packageService,
			ReleaseManager releaseManager,
			DeployerRepository deployerRepository,
			PackageMetadataService packageMetadataService) {
		this.packageMetadataRepository = packageMetadataRepository;
		this.releaseRepository = releaseRepository;
		this.packageService = packageService;
		this.releaseManager = releaseManager;
		this.deployerRepository = deployerRepository;
		this.packageMetadataService = packageMetadataService;
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
	@Transactional
	public Release install(Long id, InstallProperties installProperties) {
		Assert.notNull(installProperties, "Deploy properties can not be null");
		Assert.notNull(id, "Package id can not be null");
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
	@Transactional
	public Release install(InstallRequest installRequest) {
		validateInstallRequest(installRequest);
		PackageIdentifier packageIdentifier = installRequest.getPackageIdentifier();
		String packageVersion = packageIdentifier.getPackageVersion();
		String packageName = packageIdentifier.getPackageName();
		PackageMetadata packageMetadata;
		if (!StringUtils.hasText(packageVersion)) {
			List<PackageMetadata> packageMetadataList = this.packageMetadataRepository.findByNameRequired(packageName);
			if (packageMetadataList.size() == 1) {
				packageMetadata = packageMetadataList.get(0);
			}
			else {
				packageMetadata = this.packageMetadataRepository.findFirstByNameOrderByVersionDesc(packageName);
			}
		}
		else {
			packageMetadata = this.packageMetadataRepository.findByNameAndOptionalVersionRequired(packageName,
					packageVersion);
		}
		return install(packageMetadata, installRequest.getInstallProperties());
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
			Release latestRelease = this.releaseRepository.findLatestRelease(installRequest.getInstallProperties()
					.getReleaseName());
			if (latestRelease != null &&
					!latestRelease.getInfo().getStatus().getStatusCode().equals(StatusCode.DELETED)) {
				throw new SkipperException("Release with the name [" +
						installRequest.getInstallProperties().getReleaseName()
						+ "] already exists and it is not deleted.");
			}
		}
		catch (ReleaseNotFoundException e) {
			// ignore as this is expected.
		}
	}

	protected Release install(PackageMetadata packageMetadata, InstallProperties installProperties) {
		Assert.notNull(packageMetadata, "Can't download package, PackageMetadata is a null value.");
		Release existingDeletedRelease = this.releaseRepository
				.findLatestReleaseIfDeleted(installProperties.getReleaseName());
		int releaseVersion;
		if (existingDeletedRelease != null) {
			logger.info("Re-using existing release name [{}] of previously deleted release.",
					installProperties.getReleaseName());
			releaseVersion = existingDeletedRelease.getVersion() + 1;
		}
		else {
			releaseVersion = 1;
		}

		Release release = createInitialRelease(installProperties, this.packageService.downloadPackage(packageMetadata),
				releaseVersion);
		return install(release);
	}

	public Release install(Release release) {
		Map<String, Object> mergedMap = ConfigValueUtils.mergeConfigValues(release.getPkg(), release.getConfigValues());
		// Render yaml resources
		String manifestData = ManifestUtils.createManifest(release.getPkg(), mergedMap);
		DumperOptions dumperOptions = new DumperOptions();
		dumperOptions.setDefaultScalarStyle(DumperOptions.ScalarStyle.DOUBLE_QUOTED);
		dumperOptions.setPrettyFlow(true);
		Yaml yaml = new Yaml(dumperOptions);
		Iterable<Object> loadedYaml = yaml.loadAll(manifestData);
		logger.debug("Manifest = " + ArgumentSanitizer.sanitizeYml(manifestData));
		Manifest manifest = new Manifest();
		List<Object> yamlList = new ArrayList<>();
		for (Object object: loadedYaml) {
			yamlList.add(object);
		}
		manifest.setData(yaml.dumpAll(yamlList.iterator()));
		release.setManifest(manifest);
		// Deployment
		Release releaseToReturn = this.releaseManager.install(release);
		return releaseToReturn;
	}

	/**
	 * Delete the release.
	 * @param releaseName the name of the release
	 * @return the state of the release after requesting a deletion
	 */
	@Transactional
	public Release delete(String releaseName) {
		return this.delete(releaseName, false);
	}

	/**
	 * If the deleteReleasePackage is true deletes the release along with the package it was created from. The
	 * transactions succeeds only if there are no other deployed release from the same package but the releaseName.
	 * If the deleteReleasePackage is false it behaves as {@link #delete(String)}
	 * @param releaseName the name of the release to be deleted
	 * @param deleteReleasePackage if true tries to delete the package of the releaseName release
	 * @return the state of the release after requesting a deletion
	 */
	@Transactional
	public Release delete(String releaseName, boolean deleteReleasePackage) {
		Assert.notNull(releaseName, "Release name must not be null");
		Release releaseToDelete = this.releaseRepository.findLatestDeployedRelease(releaseName);
		if (deleteReleasePackage) {
			// Note: the app deployer delete is not transactional operations (e.g. we can't revert the Deployer's state
			// in case of failure. Therefore we should call the package delete before the release delete!
			String packageName = releaseToDelete.getPkg().getMetadata().getName();
			if (this.packageMetadataService.filterReleasesFromLocalRepos(
					Arrays.asList(releaseToDelete), packageName).isEmpty()) {
				throw new PackageDeleteException("Can't delete package: " + packageName + " from non-local repository");
			}

			this.packageMetadataService.deleteIfAllReleasesDeleted(packageName,
					r -> {
						// Exclude the release being deleted from the is active check
						if (r.getName().equals(releaseToDelete.getName())
								&& r.getVersion() == releaseToDelete.getVersion()) {
							return false;
						}
						// If not the deleted release follow the default package delete policies
						return PackageMetadataService.DEFAULT_RELEASE_ACTIVITY_CHECK.test(r);
					});
		}
		return this.releaseManager.delete(releaseToDelete);
	}

	/**
	 * Return the current status of the release
	 * @param releaseName the name of the release
	 * @return The latest state of the release as stored in the database
	 */
	@Transactional
	public Info status(String releaseName) {
		Release release = this.releaseRepository.findTopByNameOrderByVersionDesc(releaseName);
		if (release == null) {
			throw new ReleaseNotFoundException(releaseName);
		}
		release = status(release);
		if (release == null) {
			throw new ReleaseNotFoundException(releaseName);
		}
		return release.getInfo();
	}

	/**
	 * Return the current status of the release given the release and version.
	 * @param releaseName name of the release
	 * @param version release version
	 * @return The latest state of the release as stored in the database
	 */
	@Transactional
	public Info status(String releaseName, Integer version) {
		return status(this.releaseRepository.findByNameAndVersion(releaseName, version)).getInfo();
	}

	/**
	 * Return the manifest, the final set of instructions to deploy for a given release.
	 * @param releaseName the name of the release
	 * @return the release manifest
	 */
	@Transactional
	public Manifest manifest(String releaseName) {
		Release release = this.releaseRepository.findTopByNameOrderByVersionDesc(releaseName);
		if (release == null) {
			throw new ReleaseNotFoundException(releaseName);
		}
		return release.getManifest();
	}

	/**
	 * Return the manifest, the final set of instructions to deploy for a given release, given
	 * the name and version.
	 * @param releaseName the name of the release
	 * @param version the release version
	 * @return the release manifest
	 */
	@Transactional
	public Manifest manifest(String releaseName, Integer version) {
		return this.releaseRepository.findByNameAndVersion(releaseName, version).getManifest();
	}

	private Release status(Release release) {
		return this.releaseManager.status(release);
	}

	protected Info createNewInfo() {
		return Info.createNewInfo("Initial install underway");
	}

	@Transactional
	public ReleaseAnalysisReport createReport(Release existingRelease, Release replacingRelease) {
		return this.releaseManager.createReport(existingRelease, replacingRelease, true);
	}

	protected Release createInitialRelease(InstallProperties installProperties, Package packageToInstall,
			int releaseVersion) {
		Release release = new Release();
		release.setName(installProperties.getReleaseName());
		release.setPlatformName(installProperties.getPlatformName());
		release.setConfigValues(installProperties.getConfigValues());
		release.setPkg(packageToInstall);
		release.setVersion(releaseVersion);
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
	@Transactional
	public List<Release> history(String releaseName, int maxRevisions) {
		return this.releaseRepository.findReleaseRevisions(releaseName, maxRevisions);
	}

	/**
	 * List the latest version of releases with status of deployed or failed.
	 *
	 * @param releaseNameLike the wildcard name of releases to search for
	 * @return the list of all matching releases
	 */
	@Transactional
	public List<Release> list(String releaseNameLike) {
		return this.releaseRepository.findLatestDeployedOrFailed(releaseNameLike);
	}

	/**
	 * Get the latest revision of all releases with status of deployed or failed state.
	 *
	 * @return the list of all matching releases
	 */
	@Transactional
	public List<Release> list() {
		return this.releaseRepository.findLatestDeployedOrFailed();
	}

}
