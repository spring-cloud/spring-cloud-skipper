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
package org.springframework.cloud.skipper.shell.command;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.skipper.ReleaseNotFoundException;
import org.springframework.cloud.skipper.client.SkipperClient;
import org.springframework.cloud.skipper.domain.ConfigValues;
import org.springframework.cloud.skipper.domain.Info;
import org.springframework.cloud.skipper.domain.InstallProperties;
import org.springframework.cloud.skipper.domain.InstallRequest;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.UpgradeProperties;
import org.springframework.cloud.skipper.domain.UpgradeRequest;
import org.springframework.cloud.skipper.domain.UploadRequest;
import org.springframework.cloud.skipper.shell.command.support.DeploymentStateDisplay;
import org.springframework.cloud.skipper.shell.command.support.TableUtils;
import org.springframework.cloud.skipper.shell.command.support.YmlUtils;
import org.springframework.hateoas.Resources;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.table.ArrayTableModel;
import org.springframework.shell.table.BeanListTableModel;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModel;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

import static org.springframework.shell.standard.ShellOption.NULL;

/**
 * The main skipper commands that deal with packages and releases.
 * @author Ilayaperumal Gopinathan
 * @author Mark Pollack
 */
@ShellComponent
public class SkipperCommands extends AbstractSkipperCommand {

	private static final Logger logger = LoggerFactory.getLogger(SkipperCommands.class);

	@Autowired
	public SkipperCommands(SkipperClient skipperClient) {
		this.skipperClient = skipperClient;
	}

	/**
	 * Aggregate the set of app states into a single state for a stream.
	 *
	 * @param states set of states for apps of a stream
	 * @return the stream state based on app states
	 */
	public static DeploymentState aggregateState(List<DeploymentState> states) {
		if (states.size() == 1) {
			DeploymentState state = states.iterator().next();
			logger.debug("aggregateState: Deployment State Set Size = 1.  Deployment State " + state);
			// a stream which is known to the stream definition repository
			// but unknown to deployers is undeployed
			if (state == DeploymentState.unknown) {
				logger.debug("aggregateState: Returning " + DeploymentState.undeployed);
				return DeploymentState.undeployed;
			}
			else {
				logger.debug("aggregateState: Returning " + state);
				return state;
			}
		}
		if (states.isEmpty() || states.contains(DeploymentState.error)) {
			logger.debug("aggregateState: Returning " + DeploymentState.error);
			return DeploymentState.error;
		}
		if (states.contains(DeploymentState.failed)) {
			logger.debug("aggregateState: Returning " + DeploymentState.failed);
			return DeploymentState.failed;
		}
		if (states.contains(DeploymentState.deploying)) {
			logger.debug("aggregateState: Returning " + DeploymentState.deploying);
			return DeploymentState.deploying;
		}

		if (allAppsDeployed(states)) {
			return DeploymentState.deployed;
		}

		logger.debug("aggregateState: Returning " + DeploymentState.partial);
		return DeploymentState.partial;
	}

	private static boolean allAppsDeployed(List<DeploymentState> deploymentStateList) {
		boolean allDeployed = true;
		for (DeploymentState deploymentState : deploymentStateList) {
			if (deploymentState != DeploymentState.deployed) {
				allDeployed = false;
				break;
			}
		}
		return allDeployed;
	}

	@ShellMethod(key = "search", value = "Search for the packages.")
	public Object search(
			@ShellOption(help = "wildcard expression to search for the package name", defaultValue = NULL) String name,
			@ShellOption(help = "boolean to set for more detailed package metadata") boolean details)
			throws Exception {
		Resources<PackageMetadata> resources = skipperClient.search(name, details);
		if (!details) {
			LinkedHashMap<String, Object> headers = new LinkedHashMap<>();
			headers.put("name", "Name");
			headers.put("version", "Version");
			headers.put("description", "Description");
			TableModel model = new BeanListTableModel<>(resources.getContent(), headers);
			TableBuilder tableBuilder = new TableBuilder(model);
			TableUtils.applyStyle(tableBuilder);
			return tableBuilder.build();
		}
		else {
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			PackageMetadata[] packageMetadataResources = resources.getContent().toArray(new PackageMetadata[0]);
			List<Table> tableList = new ArrayList<>();
			for (int i = 0; i < resources.getContent().size(); i++) {
				String json = mapper.writeValueAsString(packageMetadataResources[i]);
				Map<String, String> map = mapper.readValue(json, new TypeReference<Map<String, String>>() {
				});
				map.remove("id");
				LinkedHashMap<String, Object> headers = new LinkedHashMap<>();
				headers.put("key", "Name");
				headers.put("value", "Value");
				TableModel model = new BeanListTableModel<>(map.entrySet(), headers);
				TableBuilder tableBuilder = new TableBuilder(model);
				TableUtils.applyStyle(tableBuilder);
				tableList.add(tableBuilder.build());
			}
			return tableList;
		}
	}

	@ShellMethod(key = "install", value = "Install a package.")
	public String install(
			@ShellOption(help = "name of the package to install") String packageName,
			@ShellOption(help = "version of the package to install, if not specified latest version will be used", defaultValue = NULL) String packageVersion,
			// TODO specify a specific package repository
			@ShellOption(help = "specify values in a YAML file", defaultValue = NULL) File file,
			@ShellOption(help = "the comma separated set of properties to override during install", defaultValue = NULL) String properties,
			// TODO support generation of a release name
			@ShellOption(help = "the release name to use") String releaseName,
			// TODO investigate server side support of 'default'
			@ShellOption(help = "the platform name to use", defaultValue = "default") String platformName)
			throws IOException {
		// Commented out until https://github.com/spring-cloud/spring-cloud-skipper/issues/263 is
		// addressed
		// assertMutuallyExclusiveFileAndProperties(file, properties);
		Release release = skipperClient
				.install(getInstallRequest(packageName, packageVersion, file, properties, releaseName, platformName));
		return "Released " + release.getName() + ". Now at version v" + release.getVersion() + ".";
	}

	private InstallRequest getInstallRequest(String packageName, String packageVersion, File yamlFile,
			String properties, String releaseName, String platformName) throws IOException {
		InstallProperties installProperties = getInstallProperties(releaseName, platformName, yamlFile,
				properties);
		InstallRequest installRequest = new InstallRequest();
		installRequest.setInstallProperties(installProperties);
		PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName(packageName);
		packageIdentifier.setPackageVersion(packageVersion);
		installRequest.setPackageIdentifier(packageIdentifier);
		return installRequest;
	}

	private InstallProperties getInstallProperties(String releaseName, String platformName, File yamlFile,
			String propertiesToOverride) throws IOException {
		InstallProperties installProperties = new InstallProperties();
		if (StringUtils.hasText(releaseName)) {
			installProperties.setReleaseName(releaseName);
		}
		// There is a 'default' value for platformName
		installProperties.setPlatformName(platformName);
		String configValuesYML = YmlUtils.getYamlConfigValues(yamlFile, propertiesToOverride);
		if (StringUtils.hasText(configValuesYML)) {
			ConfigValues configValues = new ConfigValues();
			configValues.setRaw(configValuesYML);
			installProperties.setConfigValues(configValues);
		}
		return installProperties;
	}

	@ShellMethod(key = "upgrade", value = "Upgrade a release.")
	public Object upgrade(
			@ShellOption(help = "the name of the release to upgrade") String releaseName,
			@ShellOption(help = "the name of the package to use for the upgrade") String packageName,
			@ShellOption(help = "the version of the package to use for the upgrade", defaultValue = NULL) String packageVersion,
			@ShellOption(help = "specify values in a YAML file", defaultValue = NULL) File file,
			@ShellOption(help = "the comma separated set of properties to override during upgrade", defaultValue = NULL) String properties)
			throws IOException {
		// Commented out until https://github.com/spring-cloud/spring-cloud-skipper/issues/263 is
		// addressed
		// assertMutuallyExclusiveFileAndProperties(file, properties);
		Release release = skipperClient
				.upgrade(getUpgradeRequest(releaseName, packageName, packageVersion, file, properties));
		StringBuilder sb = new StringBuilder();
		sb.append(release.getName() + " has been upgraded.  Now at version v" + release.getVersion() + ".");
		return sb.toString();
	}

	private void updateStatus(StringBuilder sb, Release release) {
		sb.append("Release Status: " + release.getInfo().getStatus().getStatusCode() + "\n");
		if (StringUtils.hasText(release.getInfo().getStatus().getPlatformStatus())) {
			sb.append("Platform Status: " + release.getInfo().getStatus().getPlatformStatusPrettyPrint());
		}
		else {
			sb.append("Platform Status: unknown");
		}
	}

	private void assertMutuallyExclusiveFileAndProperties(File yamlFile, String properties) {
		Assert.isTrue(!(yamlFile != null && properties != null), "The options 'file' and 'properties' options "
				+ "are mutually exclusive.");
		if (yamlFile != null) {
			String extension = FilenameUtils.getExtension(yamlFile.getName());
			Assert.isTrue((extension.equalsIgnoreCase("yml") || extension.equalsIgnoreCase("yaml")),
					"The file should be YAML file");
		}
	}

	private UpgradeRequest getUpgradeRequest(String releaseName, String packageName, String packageVersion,
			File propertiesFile, String propertiesToOverride) throws IOException {
		UpgradeRequest upgradeRequest = new UpgradeRequest();
		UpgradeProperties upgradeProperties = new UpgradeProperties();
		upgradeProperties.setReleaseName(releaseName);
		String configValuesYML = YmlUtils.getYamlConfigValues(propertiesFile, propertiesToOverride);
		if (StringUtils.hasText(configValuesYML)) {
			ConfigValues configValues = new ConfigValues();
			configValues.setRaw(configValuesYML);
			upgradeProperties.setConfigValues(configValues);
		}
		upgradeRequest.setUpgradeProperties(upgradeProperties);
		PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName(packageName);
		packageIdentifier.setPackageVersion(packageVersion);
		upgradeRequest.setPackageIdentifier(packageIdentifier);
		upgradeRequest.setPackageIdentifier(packageIdentifier);
		return upgradeRequest;
	}

	@ShellMethod(key = "rollback", value = "Rollback the release to a previous or a specific release.")
	public String rollback(
			@ShellOption(help = "the name of the release to rollback") String releaseName,
			@ShellOption(help = "the specific release version to rollback to. " +
					"Not specifying the value rolls back to the previous release.", defaultValue = "0") int releaseVersion) {
		Release release = skipperClient.rollback(releaseName, releaseVersion);
		StringBuilder sb = new StringBuilder();
		sb.append(release.getName() + " has been rolled back.  Now at version v" + release.getVersion() + ".");
		return sb.toString();
	}

	@ShellMethod(key = "delete", value = "Delete the release.")
	public String delete(
			@ShellOption(help = "the name of the release to delete") String releaseName) {
		Release release = skipperClient.delete(releaseName);
		StringBuilder sb = new StringBuilder();
		sb.append(release.getName() + " has been deleted.");
		return sb.toString();
	}

	@ShellMethod(key = "upload", value = "Upload a package.")
	public String upload(@ShellOption(help = "the package to be uploaded") String path,
			@ShellOption(help = "the local repository name to upload to", defaultValue = NULL) String repoName) {
		UploadRequest uploadRequest = new UploadRequest();
		try {
			File file = ResourceUtils.getFile(path);
			String zipFileName = file.getName();
			String fileName = zipFileName.substring(0, zipFileName.lastIndexOf("-"));
			String versionAndExtension = zipFileName.substring(fileName.length() + 1);
			String extension = versionAndExtension.substring(versionAndExtension.lastIndexOf(".") + 1);
			String version = versionAndExtension.replaceAll("." + extension, "");
			uploadRequest.setName(fileName);
			uploadRequest.setVersion(version);
			uploadRequest.setExtension(extension);
			uploadRequest.setRepoName(StringUtils.hasText(repoName) ? repoName : "local");
			uploadRequest.setPackageFileAsBytes(Files.readAllBytes(file.toPath()));
		}
		catch (FileNotFoundException e) {
			throw new IllegalArgumentException("File Not found: " + e.getMessage());
		}
		catch (IOException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
		PackageMetadata packageMetadata = skipperClient.upload(uploadRequest);
		return "Package uploaded successfully:[" + packageMetadata.getName() + ":" + packageMetadata.getVersion() + "]";
	}

	@ShellMethod(key = "list", value = "List the latest version of releases with status of deployed or failed.")
	public Table list(
			@ShellOption(help = "wildcard expression to search by release name", defaultValue = NULL) String releaseName) {
		List<Release> releases = this.skipperClient.list(releaseName);
		LinkedHashMap<String, Object> headers = new LinkedHashMap<>();
		headers.put("name", "Name");
		headers.put("version", "Version");
		headers.put("info.lastDeployed", "Last updated");
		headers.put("info.status.statusCode", "Status");
		headers.put("pkg.metadata.name", "Package Name");
		headers.put("pkg.metadata.version", "Package Version");
		headers.put("platformName", "Platform Name");
		headers.put("info.status.platformStatusPrettyPrint", "Platform Status");
		TableModel model = new BeanListTableModel<>(releases, headers);
		TableBuilder tableBuilder = new TableBuilder(model);
		TableUtils.applyStyle(tableBuilder);
		return tableBuilder.build();
	}

	@ShellMethod(key = "history", value = "List the history of versions for a given release.")
	public Table history(
			@ShellOption(help = "wildcard expression to search by release name") @NotNull String releaseName,
			@ShellOption(help = "maximum number of revisions to include in the history", defaultValue = NULL) String max) {
		Collection<Release> releases;
		if (StringUtils.hasText(max)) {
			assertMaxIsIntegerAndGreaterThanZero(max);
			releases = this.skipperClient.history(releaseName, max);
		}
		else {
			releases = this.skipperClient.history(releaseName).getContent();
		}
		LinkedHashMap<String, Object> headers = new LinkedHashMap<>();
		headers.put("version", "Version");
		headers.put("info.lastDeployed", "Last updated");
		headers.put("info.status.statusCode", "Status");
		headers.put("pkg.metadata.name", "Package Name");
		headers.put("pkg.metadata.version", "Package Version");
		headers.put("info.description", "Description");
		TableModel model = new BeanListTableModel<>(releases, headers);
		TableBuilder tableBuilder = new TableBuilder(model);
		TableUtils.applyStyle(tableBuilder);
		return tableBuilder.build();
	}

	@ShellMethod(key = "status", value = "Status for a last known release version.")
	public Object status(
			@ShellOption(help = "release name") @NotNull String releaseName,
			@ShellOption(help = "the specific release version.", defaultValue = NULL) Integer releaseVersion) {
		Info info;
		try {
			if (releaseVersion == null) {
				info = this.skipperClient.status(releaseName);
			}
			else {
				info = this.skipperClient.status(releaseName, releaseVersion);
			}
		}
		catch (ReleaseNotFoundException e) {
			return "Release with name '" + e.getReleaseName() + "' not found";
		}
		Object[][] data = new Object[3][];
		data[0] = new Object[] { "Last Deployed", info.getFirstDeployed() };
		data[1] = new Object[] { "Status", info.getStatus().getStatusCode().toString() };

		DeploymentState aggregateState = aggregateState(info.getStatus().getDeploymentStateList());
		StringBuilder sb = new StringBuilder();
		sb.append(DeploymentStateDisplay.fromKey(aggregateState.name()).getDescription() + "\n");
		sb.append(info.getStatus().getPlatformStatusPrettyPrint());
		data[2] = new Object[] { "Platform Status", sb.toString() };
		TableModel model = new ArrayTableModel(data);
		TableBuilder tableBuilder = new TableBuilder(model);
		TableUtils.applyStyleNoHeader(tableBuilder);
		return tableBuilder.build();
	}

	private void assertMaxIsIntegerAndGreaterThanZero(String max) {
		try {
			int maxInt = Integer.parseInt(max);
			Assert.isTrue(maxInt > 0, "The maximum number of revisions should be greater than zero.");
		}
		catch (NumberFormatException e) {
			throw new NumberFormatException("The maximum number of revisions is not an integer. Input string = " + max);
		}
	}

}
