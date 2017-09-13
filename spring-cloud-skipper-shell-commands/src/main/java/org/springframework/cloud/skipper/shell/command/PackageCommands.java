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
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.cloud.skipper.client.SkipperClient;
import org.springframework.cloud.skipper.domain.skipperpackage.DeployProperties;
import org.springframework.cloud.skipper.shell.command.support.SkipperClientUpdatedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.FileSystemResource;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.util.Assert;

/**
 * @author Ilayaperumal Gopinathan
 */
@ShellComponent
public class PackageCommands {

	private SkipperClient skipperClient;

	@Autowired
	public PackageCommands(SkipperClient skipperClient) {
		this.skipperClient = skipperClient;
	}

	@ShellMethod(key = "package list", value = "Get the package metadata")
	public String packageMetadata(
			@ShellOption(help = "boolean to set for more detailed package metadata", defaultValue = "false") boolean details) {
		return skipperClient.getPackageMetadata(details);
	}

	@ShellMethod(key = "package deploy", value = "Deploy the package metadata")
	public String deploy(
			@ShellOption(help = "name of the package to deploy") String packageName,
			@ShellOption(help = "version of the package to deploy") String packageVersion,
			@ShellOption(help = "the properties file to use to deploy") File propertiesFile,
			@ShellOption(help = "the release name to use") String releaseName,
			@ShellOption(help = "the platform name to use") String platformName)
			throws IOException {
		// todo: Make releaseName & propertiesFile mutually exclusive
		return skipperClient
				.deploy(getDeployProperties(packageName, packageVersion, releaseName, platformName, propertiesFile));
	}

	@ShellMethod(key = "package update", value = "Update a specific release")
	public String update(
			@ShellOption(help = "name of the package to deploy") String packageName,
			@ShellOption(help = "version of the package to deploy") String packageVersion,
			@ShellOption(help = "the release name to use") String releaseName,
			@ShellOption(help = "the platform name to use", defaultValue = "default") String platformName,
			@ShellOption(help = "the properties file to use to deploy") File propertiesFile)
			throws IOException {
		return skipperClient
				.update(getDeployProperties(packageName, packageVersion, releaseName, platformName, propertiesFile));
	}

	private DeployProperties getDeployProperties(String packageName, String packageVersion, String releaseName,
			String platformName, File propertiesFile)
			throws IOException {
		DeployProperties deployProperties = new DeployProperties();
		if (releaseName != null) {
			deployProperties.setPackageName(packageName);
			deployProperties.setPackageVersion(packageVersion);
			deployProperties.setReleaseName(releaseName);
			deployProperties.setPlatformName(platformName);
		}
		else {
			String extension = FilenameUtils.getExtension(propertiesFile.getName());
			Properties props = null;
			if (extension.equals("yaml") || extension.equals("yml")) {
				YamlPropertiesFactoryBean yamlPropertiesFactoryBean = new YamlPropertiesFactoryBean();
				yamlPropertiesFactoryBean.setResources(new FileSystemResource(propertiesFile));
				yamlPropertiesFactoryBean.afterPropertiesSet();
				props = yamlPropertiesFactoryBean.getObject();
			}
			else {
				props = new Properties();
				try (FileInputStream fis = new FileInputStream(propertiesFile)) {
					props.load(fis);
				}
			}
			if (props != null) {
				Assert.notNull(props.getProperty("release-name"), "Release name must not be null");
				deployProperties.setReleaseName(props.getProperty("release-name"));
				deployProperties.setPlatformName(props.getProperty("platform-name", platformName));
				// todo: support config values
				// deployProperties.setConfigValues();
			}
		}
		return deployProperties;
	}

	@EventListener
	void handle(SkipperClientUpdatedEvent event) {
		this.skipperClient = event.getSkipperClient();
	}
}
