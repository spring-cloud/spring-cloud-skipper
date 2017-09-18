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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import com.samskivert.mustache.Mustache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.cloud.skipper.domain.CommonDeploymentProperties;
import org.springframework.cloud.skipper.domain.ConfigValues;
import org.springframework.cloud.skipper.domain.DeployProperties;
import org.springframework.cloud.skipper.domain.DeployRequest;
import org.springframework.cloud.skipper.domain.Info;
import org.springframework.cloud.skipper.domain.Package;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.Status;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.domain.Template;
import org.springframework.cloud.skipper.domain.UpdateProperties;
import org.springframework.cloud.skipper.domain.UpdateRequest;
import org.springframework.cloud.skipper.index.PackageException;
import org.springframework.cloud.skipper.repository.DeployerRepository;
import org.springframework.cloud.skipper.repository.PackageMetadataRepository;
import org.springframework.cloud.skipper.repository.ReleaseRepository;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
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
	 * @param deployProperties contains the name of the release, the platfrom to deploy
	 * to, and configuration values to replace in the package template.
	 * @return the Release object associated with this deployment
	 * @throws PackageException if the package to deploy can not be found.
	 */
	public Release deploy(String id, DeployProperties deployProperties) {
		Assert.notNull(deployProperties, "Deploy Properties can not be null");
		Assert.hasText(id, "Package ID can not be null");
		PackageMetadata packageMetadata = this.packageMetadataRepository.findOne(id);
		if (packageMetadata == null) {
			throw new PackageException(String.format("Package with id='%s' can not be found.", id));
		}
		return deploy(packageMetadata, deployProperties);
	}

	/**
	 * Downloads the package metadata and package zip file specified by PackageIdentifier
	 * property of the DeploymentRequest. Deploys the package on the target platform.
	 * @param deployRequest the deploymentRequest
	 * @return the Release object associated with this deployment
	 */
	public Release deploy(DeployRequest deployRequest) {
		// TODO deployRequest validation.
		PackageIdentifier packageIdentifier = deployRequest.getPackageIdentifier();
		PackageMetadata packageMetadata = this.packageMetadataRepository.findByNameAndVersion(
				packageIdentifier.getPackageName(),
				packageIdentifier.getPackageVersion());
		// TODO - what about multi-repository support....
		// deployRequest.getPackageIdentifier().getRepositoryName()
		return deploy(packageMetadata.getId(), deployRequest.getDeployProperties());
	}

	protected Release deploy(PackageMetadata packageMetadata, DeployProperties deployProperties) {
		Assert.notNull(packageMetadata, "Can't download PackageMetadata, it is a null value.");
		this.packageService.downloadPackage(packageMetadata);
		Package packageToInstall = this.packageService.loadPackage(packageMetadata);
		Release release = createInitialRelease(deployProperties, packageToInstall);
		return deploy(release);
	}

	private Release deploy(Release release) {
		Map<String, Object> mergedMap = ConfigValueUtils.mergeConfigValues(release.getPkg(), release.getConfigValues());
		// Properties model = mergeConfigValues(release.getPkg().getConfigValues(),
		// release.getConfigValues());
		// Render yaml resources
		String manifest = createManifest(release.getPkg(), mergedMap);
		release.setManifest(manifest);
		// Deployment
		return this.releaseManager.deploy(release);
	}

	public Release undeploy(String releaseName) {
		Assert.notNull(releaseName, "Release name must not be null");
		Release release = this.releaseRepository.findLatestRelease(releaseName);
		return this.releaseManager.undeploy(release);
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

	public Release update(UpdateRequest updateRequest) {
		UpdateProperties updateProperties = updateRequest.getUpdateProperties();
		Release oldRelease = getLatestRelease(updateProperties.getReleaseName());
		PackageIdentifier packageIdentifier = updateRequest.getPackageIdentifier();
		// todo: search multi repository
		PackageMetadata packageMetadata = this.packageMetadataRepository
				.findByNameAndVersion(packageIdentifier.getPackageName(), packageIdentifier.getPackageVersion());
		Release newRelease = createNewRelease(packageMetadata, oldRelease.getVersion() + 1, updateProperties);
		Map<String, Object> model = ConfigValueUtils.mergeConfigValues(newRelease.getPkg(),
				newRelease.getConfigValues());
		String manifest = createManifest(newRelease.getPkg(), model);
		newRelease.setManifest(manifest);
		return update(oldRelease, newRelease);
	}

	public Release createNewRelease(PackageMetadata packageMetadata, Integer newVersion,
			CommonDeploymentProperties deployProperties) {
		Assert.notNull(deployProperties, "Deploy Properties can not be null");
		this.packageService.downloadPackage(packageMetadata);
		Package packageToInstall = this.packageService.loadPackage(packageMetadata);
		packageToInstall.getMetadata().setId(packageMetadata.getId());
		Release release = new Release();
		release.setName(deployProperties.getReleaseName());
		release.setPlatformName(deployProperties.getPlatformName());
		release.setConfigValues(deployProperties.getConfigValues());
		release.setPkg(packageToInstall);
		release.setVersion(newVersion);
		Info info = createNewInfo("Update deploy underway");
		release.setInfo(info);
		return release;
	}

	protected Info createNewInfo(String update_deploy_underway) {
		Info info = new Info();
		info.setFirstDeployed(new Date());
		info.setLastDeployed(new Date());
		Status status = new Status();
		status.setStatusCode(StatusCode.UNKNOWN);
		info.setStatus(status);
		info.setDescription(update_deploy_underway);
		return info;
	}

	public Release update(Release existingRelease, Release replacingRelease) {
		Assert.notNull(existingRelease, "Existing Release must not be null");
		Assert.notNull(replacingRelease, "Replacing Release must not be null");
		Release release = this.releaseManager.deploy(replacingRelease);
		// TODO UpdateStrategy (manfiestSave, healthCheck)
		this.releaseManager.undeploy(existingRelease);
		return release;
	}

	/**
	 * Rollback the release name to the specified version. If the version is 0, then
	 * rollback to the previous release.
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
		// Do not set ConfigValues since the manifest from the previous release has
		// already
		// resolved those...
		newRelease.setInfo(createNewInfo());

		update(currentRelease, newRelease);
		return releaseToRollback;
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

	/**
	 * Merge the properties, derived from YAML format, contained in
	 * commandLineConfigValues and templateConfigValue, giving preference to
	 * commandLineConfigValues. Assumes that the YAML is stored as "raw" data in the
	 * ConfigValues object. If the "raw" data is empty or null, an empty property object
	 * is returned.
	 *
	 * @param templateConfigValue YAML data defined in the template.yaml file
	 * @param commandLineConfigValues YAML data passed at the application runtime
	 * @return A Properties object that is the merger of both ConfigValue objects,
	 * commandLineConfig values override values in templateConfig.
	 */
	public Properties mergeConfigValues(ConfigValues templateConfigValue, ConfigValues commandLineConfigValues) {
		Properties commandLineOverrideProperties;
		if (commandLineConfigValues == null) {
			commandLineOverrideProperties = new Properties();
		}
		else {
			commandLineOverrideProperties = convertYamlToProperties(commandLineConfigValues.getRaw());
		}
		Properties templateVariables;
		if (templateConfigValue == null) {
			templateVariables = new Properties();
		}
		else {
			templateVariables = convertYamlToProperties(templateConfigValue.getRaw());
		}

		Properties model = new Properties();
		model.putAll(templateVariables);
		model.putAll(commandLineOverrideProperties);
		return model;
	}

	/**
	 * Return a Properties object given a String that contains YAML. The Properties
	 * created by this factory have nested paths for hierarchical objects. All exposed
	 * values are of type {@code String}</b> for access through the common
	 * {@link Properties#getProperty} method. See YamlPropertiesFactoryBean for more
	 * information.
	 * @param yamlString String that contains YAML
	 * @return properties object containing contents of YAML file
	 */
	public Properties convertYamlToProperties(String yamlString) {
		YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();

		Properties values;
		if (StringUtils.hasText(yamlString)) {
			try (InputStream is = new ByteArrayInputStream(yamlString.getBytes())) {
				yaml.setResources(new InputStreamResource(is));
				yaml.afterPropertiesSet();
				values = yaml.getObject();
			}
			catch (Exception e) {
				throw new IllegalArgumentException(
						"Could not convert YAML to properties object from string " + yamlString, e);
			}
		}
		else {
			values = new Properties();
		}
		return values;

	}

	private Release createInitialRelease(DeployProperties deployProperties, Package packageToInstall) {
		Release release = new Release();
		release.setName(deployProperties.getReleaseName());
		release.setPlatformName(deployProperties.getPlatformName());
		release.setConfigValues(deployProperties.getConfigValues());
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
	private void validateInitialRelease(Release release) {
		this.deployerRepository.findByNameRequired(release.getPlatformName());
	}

	private Info createNewInfo() {
		Info info = new Info();
		info.setFirstDeployed(new Date());
		info.setLastDeployed(new Date());
		Status status = new Status();
		status.setStatusCode(StatusCode.UNKNOWN);
		info.setStatus(status);
		info.setDescription("Initial deploy underway");
		return info;
	}

}
