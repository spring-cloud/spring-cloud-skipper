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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.codearte.props2yaml.Props2YAML;
import org.apache.commons.io.FilenameUtils;
import org.yaml.snakeyaml.Yaml;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.client.SkipperClient;
import org.springframework.cloud.skipper.domain.ConfigValues;
import org.springframework.cloud.skipper.domain.InstallProperties;
import org.springframework.cloud.skipper.domain.InstallRequest;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.UpgradeProperties;
import org.springframework.cloud.skipper.domain.UpgradeRequest;
import org.springframework.cloud.skipper.domain.UploadRequest;
import org.springframework.cloud.skipper.shell.command.support.TableUtils;
import org.springframework.hateoas.Resources;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.table.BeanListTableModel;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModel;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

import static org.springframework.shell.standard.ShellOption.NULL;

/**
 * @author Ilayaperumal Gopinathan
 * @author Mark Pollack
 */
@ShellComponent
public class SkipperCommands extends AbstractSkipperCommand {

	@Autowired
	public SkipperCommands(SkipperClient skipperClient) {
		this.skipperClient = skipperClient;
	}

	@ShellMethod(key = "search", value = "Search for the packages")
	public Object searchPackage(
			@ShellOption(help = "wildcard expression to search for the package name", defaultValue = NULL) String name,
			@ShellOption(help = "boolean to set for more detailed package metadata") boolean details)
			throws Exception {
		Resources<PackageMetadata> resources = skipperClient.getPackageMetadata(name, details);
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

	@ShellMethod(key = "install", value = "Install a package")
	public String installPackage(
			@ShellOption(help = "name of the package to install") String name,
			@ShellOption(help = "version of the package to install", defaultValue = NULL) String version,
			// TODO specify a specific package repository
			@ShellOption(help = "the YAML properties file to use to install", defaultValue = NULL) File file,
			@ShellOption(help = "the comma set of properties to override during install", defaultValue = NULL) String set,
			// TODO support generation of a release name
			@ShellOption(help = "the release name to use") String releaseName,
			@ShellOption(help = "the platform name to use", defaultValue = "default") String platformName)
			throws IOException {
		assertMutuallyExclusiveFileAndProperties(file, set);
		Release release = skipperClient
				.installPackage(getInstallRequest(name, version, file, set, releaseName, platformName));
		return "Released " + release.getName();
	}

	@ShellMethod(key = "upgrade", value = "Upgrade a release")
	public String upgradeRelease(
			@ShellOption(help = "the name of the release to upgrade") String releaseName,
			@ShellOption(help = "the name of the package to use for the upgrade") String packageName,
			@ShellOption(help = "the version of the package to use for the upgrade") String packageVersion,
			@ShellOption(help = "the properties file to use to install during the upgrade", defaultValue = NULL) File propertiesFile,
			@ShellOption(help = "the comma set of properties to override during install", defaultValue = NULL) String set)
			throws IOException {
		assertMutuallyExclusiveFileAndProperties(propertiesFile, set);
		Release release = skipperClient
				.upgrade(getUpgradeRequest(releaseName, packageName, packageVersion, propertiesFile, set));
		StringBuilder sb = new StringBuilder();
		sb.append(release.getName() + " has been updated.\n");
		sb.append("Last Deployed: " + release.getInfo().getLastDeployed() + "\n");
		sb.append("Status: " + release.getInfo().getStatus().getPlatformStatus() + "\n");
		return sb.toString();
	}

	private void assertMutuallyExclusiveFileAndProperties(File yamlFile, String set) {
		Assert.isTrue(!(yamlFile != null && set != null), "The options 'file' and 'set' options "
				+ "are mutually exclusive.");
		String extension = FilenameUtils.getExtension(yamlFile.getName());
		Assert.isTrue((extension.equalsIgnoreCase("yml") || extension.equalsIgnoreCase("yaml")),
				"The file should be YAML file");
	}

	@ShellMethod(key = "rollback", value = "Rollback the release to a previous or a specific release")
	public String rollback(
			@ShellOption(help = "the name of the release to rollback") String releaseName,
			@ShellOption(help = "the specific release version to rollback to. " +
					"Not specifying the value rolls back to the previous release.", defaultValue = "0") int releaseVersion) {
		Release release = skipperClient.rollback(releaseName, releaseVersion);
		StringBuilder sb = new StringBuilder();
		sb.append(release.getName() + " has been rolled back.\n");
		sb.append("Last Deployed: " + release.getInfo().getLastDeployed() + "\n");
		sb.append("Status: " + release.getInfo().getStatus().getPlatformStatus() + "\n");
		return sb.toString();
	}

	@ShellMethod(key = "delete", value = "Delete the release")
	public String delete(
			@ShellOption(help = "the name of the release to delete") String releaseName) {
		Release release = skipperClient.delete(releaseName);
		StringBuilder sb = new StringBuilder();
		sb.append(release.getName() + " has been undeployed.\n");
		return sb.toString();
	}

	private UpgradeRequest getUpgradeRequest(String releaseName, String packageName, String packageVersion,
			File propertiesFile, String propertiesToOverride) throws IOException {
		UpgradeRequest upgradeRequest = new UpgradeRequest();
		UpgradeProperties upgradeProperties = new UpgradeProperties();
		upgradeProperties.setReleaseName(releaseName);
		String configValuesYML = getRawConfig(propertiesFile, propertiesToOverride);
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

	private InstallRequest getInstallRequest(String packageName, String packageVersion, File propertiesFile,
			String propertiesToOverride, String releaseName, String platformName) throws IOException {
		InstallProperties installProperties = getInstallProperties(releaseName, platformName, propertiesFile,
				propertiesToOverride);
		InstallRequest installRequest = new InstallRequest();
		installRequest.setInstallProperties(installProperties);
		PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName(packageName);
		packageIdentifier.setPackageVersion(packageVersion);
		installRequest.setPackageIdentifier(packageIdentifier);
		return installRequest;
	}

	@ShellMethod(key = "upload", value = "Upload a package")
	public String uploadPackage(@ShellOption(help = "the package to be uploaded") String path,
			@ShellOption(help = "the local repository name to upload to", defaultValue = NULL) String repoName) {
		UploadRequest properties = new UploadRequest();
		try {
			File file = ResourceUtils.getFile(path);
			StringTokenizer tokenizer = new StringTokenizer(file.getName(), "-");
			String fileName = (String) tokenizer.nextElement();
			String versionAndExtension = (String) tokenizer.nextElement();
			String extension = versionAndExtension.substring(versionAndExtension.lastIndexOf("."));
			String version = versionAndExtension.replaceAll(extension, "");
			properties.setName(fileName);
			properties.setVersion(version);
			properties.setExtension(extension);
			properties.setRepoName(StringUtils.hasText(repoName) ? repoName : "local");
			properties.setPackageFileAsBytes(Files.readAllBytes(file.toPath()));
		}
		catch (FileNotFoundException e) {
			throw new IllegalArgumentException("File Not found: " + e.getMessage());
		}
		catch (IOException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
		PackageMetadata packageMetadata = skipperClient.upload(properties);
		return "Package uploaded successfully:[" + packageMetadata.getName() + ":" + packageMetadata.getVersion() + "]";
	}

	private InstallProperties getInstallProperties(String releaseName, String platformName, File yamlFile,
			String propertiesToOverride) throws IOException {
		InstallProperties installProperties = new InstallProperties();
		if (StringUtils.hasText(releaseName)) {
			installProperties.setReleaseName(releaseName);
			installProperties.setPlatformName(platformName);
		}
		String configValuesYML = getRawConfig(yamlFile, propertiesToOverride);
		if (StringUtils.hasText(configValuesYML)) {
			ConfigValues configValues = new ConfigValues();
			configValues.setRaw(configValuesYML);
			installProperties.setConfigValues(configValues);
		}
		return installProperties;
	}

	private String getRawConfig(File yamlFile, String propertiesToOverride) throws IOException {
		String configValuesYML = null;
		if (yamlFile != null) {
			Yaml yaml = new Yaml();
			configValuesYML = yaml.dump(yaml.load(new FileInputStream(yamlFile)));
		}
		else if (StringUtils.hasText(propertiesToOverride)) {
			Set<String> propertiesSet = StringUtils.commaDelimitedListToSet(propertiesToOverride);
			StringBuilder propertiesString = new StringBuilder();
			String newLineChar = System.getProperty("line.separator");
			for (String properties : propertiesSet) {
				propertiesString.append(properties);
				propertiesString.append(newLineChar);
			}
			configValuesYML = Props2YAML.fromContent(propertiesString.toString()).convert();
		}
		return configValuesYML;
	}

}
