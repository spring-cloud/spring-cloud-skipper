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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.samskivert.mustache.Mustache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.domain.Info;
import org.springframework.cloud.skipper.domain.InstallProperties;
import org.springframework.cloud.skipper.domain.InstallRequest;
import org.springframework.cloud.skipper.domain.Package;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.Status;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.domain.Template;
import org.springframework.cloud.skipper.domain.UpgradeProperties;
import org.springframework.cloud.skipper.domain.UpgradeRequest;
import org.springframework.cloud.skipper.index.PackageException;
import org.springframework.cloud.skipper.repository.DeployerRepository;
import org.springframework.cloud.skipper.repository.PackageMetadataRepository;
import org.springframework.cloud.skipper.repository.ReleaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Service responsible for the lifecycle of packages and releases, install/delete a
 * package, upgrade/rollback a release, and get status on a release.
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
@Service
public class ReleaseService {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private final PackageMetadataRepository packageMetadataRepository;

	private final ReleaseRepository releaseRepository;

	private final PackageService packageService;

	private final ReleaseManager releaseManager;

	private final DeployerRepository deployerRepository;

	@Autowired
	public ReleaseService(PackageMetadataRepository packageMetadataRepository,
			ReleaseRepository releaseRepository,
			PackageService packageService,
			ReleaseManager releaseManager,
			DeployerRepository deployerRepository) {
		this.packageMetadataRepository = packageMetadataRepository;
		this.releaseRepository = releaseRepository;
		this.packageService = packageService;
		this.releaseManager = releaseManager;
		this.deployerRepository = deployerRepository;
	}

	/**
	 * Downloads the package metadata and package zip file specified by the given Id and
	 * deploys the package on the target platform.
	 * @param id of the package
	 * @param installProperties contains the name of the release, the platfrom to install to,
	 * and configuration values to replace in the package template.
	 * @return the Release object associated with this deployment
	 * @throws PackageException if the package to install can not be found.
	 */
	public Release install(String id, InstallProperties installProperties) {
		Assert.notNull(installProperties, "Deploy properties can not be null");
		Assert.hasText(id, "Package id can not be null");
		PackageMetadata packageMetadata = this.packageMetadataRepository.findOne(id);
		if (packageMetadata == null) {
			throw new PackageException(String.format("Package with id='%s' can not be found.", id));
		}
		return install(packageMetadata, installProperties);
	}

	/**
	 * Downloads the package metadata and package zip file specified by PackageIdentifier
	 * property of the DeploymentRequest. Deploys the package on the target platform.
	 * @param installRequest the install request
	 * @return the Release object associated with this deployment
	 */
	public Release install(InstallRequest installRequest) {
		// TODO install request validation.
		PackageIdentifier packageIdentifier = installRequest.getPackageIdentifier();
		String packageName = packageIdentifier.getPackageName();
		String packageVersion = packageIdentifier.getPackageVersion();
		PackageMetadata packageMetadata;
		if (!StringUtils.hasText(packageVersion)) {
			List<PackageMetadata> packageMetadataList = this.packageMetadataRepository.findByName(packageName);
			if (packageMetadataList.size() == 1) {
				packageMetadata = packageMetadataList.get(0);
			}
			else if (packageMetadataList == null) {
				throw new PackageException("Can not find a package named " + packageName);
			}
			else {
				// TODO find latest version
				throw new PackageException("Package name " + packageName + " is not unique.  Finding latest version " +
						" not yet implemented");
			}
		}
		else {
			packageMetadata = this.packageMetadataRepository.findByNameAndVersion(packageName, packageVersion);
			if (packageMetadata == null) {
				throw new PackageException(String.format("Can not find package '%s', version '%s'",
						packageName, packageVersion));
			}
		}
		return install(packageMetadata.getId(), installRequest.getInstallProperties());
	}

	protected Release install(PackageMetadata packageMetadata, InstallProperties installProperties) {
		Assert.notNull(packageMetadata, "Can't download package, PackageMetadata is a null value.");
		Release release = createInitialRelease(installProperties, this.packageService.downloadPackage(packageMetadata));
		return install(release);
	}

	protected Release install(Release release) {
		Map<String, Object> mergedMap = ConfigValueUtils.mergeConfigValues(release.getPkg(), release.getConfigValues());
		// Render yaml resources
		String manifest = createManifest(release.getPkg(), mergedMap);
		release.setManifest(manifest);
		// Deployment
		return this.releaseManager.install(release);
	}

	public Release delete(String releaseName) {
		Assert.notNull(releaseName, "Release name must not be null");
		Release release = this.releaseRepository.findLatestRelease(releaseName);
		return this.releaseManager.delete(release);
	}

	public Release status(String releaseName, Integer version) {
		return status(this.releaseRepository.findByNameAndVersion(releaseName, version));
	}

	public Release status(Release release) {
		return this.releaseManager.status(release);
	}

	public Release getLatestRelease(String releaseName) {
		return this.releaseRepository.findLatestRelease(releaseName);
	}

	public Release upgrade(UpgradeRequest upgradeRequest) {
		UpgradeProperties upgradeProperties = upgradeRequest.getUpgradeProperties();
		Release oldRelease = getLatestRelease(upgradeProperties.getReleaseName());
		PackageIdentifier packageIdentifier = upgradeRequest.getPackageIdentifier();
		// todo: search multi repository
		PackageMetadata packageMetadata = this.packageMetadataRepository
				.findByNameAndVersion(packageIdentifier.getPackageName(), packageIdentifier.getPackageVersion());
		Release newRelease = createReleaseForUpgrade(packageMetadata, oldRelease.getVersion() + 1, upgradeProperties,
				oldRelease.getPlatformName());
		Map<String, Object> model = ConfigValueUtils.mergeConfigValues(newRelease.getPkg(),
				newRelease.getConfigValues());
		String manifest = createManifest(newRelease.getPkg(), model);
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
		Release release = this.releaseManager.install(replacingRelease);
		// TODO UpgradeStrategy (manfiestSave, healthCheck)
		this.releaseManager.delete(existingRelease);
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

		Release newRelease = new Release();
		newRelease.setName(releaseName);
		newRelease.setPkg(releaseToRollback.getPkg());
		newRelease.setManifest(releaseToRollback.getManifest());
		newRelease.setVersion(currentRelease.getVersion() + 1);
		newRelease.setPlatformName(releaseToRollback.getPlatformName());
		// Do not set ConfigValues since the manifest from the previous release has already
		// resolved those...
		newRelease.setInfo(createNewInfo());

		return upgrade(currentRelease, newRelease);
	}

	/**
	 * Iterate overall the template files, replacing placeholders with model values. One
	 * string is returned that contain all the YAML of multiple files using YAML file
	 * delimiter.
	 * @param packageToDeploy The top level package that contains all templates where
	 * placeholders are to be replaced
	 * @param model The placeholder values.
	 * @return A YAML string containing all the templates with replaced values.
	 */
	public String createManifest(Package packageToDeploy, Map<String, Object> model) {

		// Aggregate all valid manifests into one big doc.
		StringBuilder sb = new StringBuilder();
		// Top level templates.
		List<Template> templates = packageToDeploy.getTemplates();
		if (templates != null) {
			for (Template template : templates) {
				String templateAsString = new String(template.getData());
				com.samskivert.mustache.Template mustacheTemplate = Mustache.compiler().compile(templateAsString);
				sb.append("\n---\n# Source: " + template.getName() + "\n");
				sb.append(mustacheTemplate.execute(model));
			}
		}

		for (Package pkg : packageToDeploy.getDependencies()) {
			String packageName = pkg.getMetadata().getName();
			Map<String, Object> modelForDependency;
			if (model.containsKey(packageName)) {
				modelForDependency = (Map<String, Object>) model.get(pkg.getMetadata().getName());
			}
			else {
				modelForDependency = new TreeMap<>();
			}
			sb.append(createManifest(pkg, modelForDependency));
		}

		return sb.toString();
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
	 * Get the revisions of the release.
	 *
	 * @param releaseName the specific release name to search for.3
	 * @param maxRevisions the maximum number of revisions to fetch
	 * @return the revisions of the release by the given name
	 */
	public List<Release> getReleaseRevisions(String releaseName, int maxRevisions) {
		return this.releaseRepository.findReleaseRevisions(releaseName, maxRevisions);
	}

	/**
	 * Get the latest revision of the release that is in either deployed or failed state.
	 *
	 * @param releaseName the release name in wildcard expression to search for
	 * @return list of all the latest revision of the releases by the given name
	 */
	public List<Release> getLatestDeployedOrFailed(String releaseName) {
		return this.releaseRepository.findLatestDeployedOrFailed(releaseName);
	}

	/**
	 * Get the latest revision of all the releases that is in either deployed or failed state.
	 *
	 * @return list of all the latest revision of all the releases
	 */
	public List<Release> getLatestDeployedOrFailed() {
		return this.releaseRepository.findLatestDeployedOrFailed();
	}
}
