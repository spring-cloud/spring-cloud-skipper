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

package org.springframework.cloud.skipper.server.controller.docs;

import org.junit.Test;

import org.springframework.cloud.skipper.domain.InstallProperties;
import org.springframework.cloud.skipper.domain.InstallRequest;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.StringUtils;

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Gunnar Hillert
 */
@ActiveProfiles("repo-test")
@TestPropertySource(properties = { "spring.cloud.skipper.server.platform.local.accounts[test].key=value",
		"maven.remote-repositories.repo1.url=http://repo.spring.io/libs-snapshot",
		"spring.cloud.skipper.server.disableReleaseStateUpdateService=true" })
public class ListDocumentation extends BaseDocumentation {

	@Test
	public void listRelease() throws Exception {
		final String releaseName = "myLogRelease";
		final InstallRequest installRequest = new InstallRequest();
		final PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName("log");
		packageIdentifier.setPackageVersion("1.0.0");
		packageIdentifier.setRepositoryName("notused");
		installRequest.setPackageIdentifier(packageIdentifier);
		final InstallProperties installProperties = new InstallProperties();
		installProperties.setReleaseName(releaseName);
		installProperties.setPlatformName("test");
		installRequest.setInstallProperties(installProperties);

		installPackage(installRequest);

		this.mockMvc.perform(
			get("/api/list")).andDo(print())
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
					responseFields(
						fieldWithPath("[].name").description("Name of the release"),
						fieldWithPath("[].version").description("Version of the release"),
						fieldWithPath("[].info.status.statusCode").description(
							String.format("StatusCode of the release's status (%s)",
								StringUtils.arrayToCommaDelimitedString(StatusCode.values()))
						),
						fieldWithPath("[].info.status.platformStatus").description("Status from the underlying platform"),
						fieldWithPath("[].info.firstDeployed").description("Date/Time of first deployment"),
						fieldWithPath("[].info.lastDeployed").description("Date/Time of last deployment"),
						fieldWithPath("[].info.deleted").description("Date/Time of when the release was deleted"),
						fieldWithPath("[].info.description").description("Human-friendly 'log entry' about this release"),
						fieldWithPath("[].pkg.metadata.apiVersion").description("The Package Index spec version this file is based on"),
						fieldWithPath("[].pkg.metadata.origin").description("Indicates the origin of the repository (free form text)"),
						fieldWithPath("[].pkg.metadata.repositoryId").description("The repository ID this Package Index file belongs to"),
						fieldWithPath("[].pkg.metadata.kind").description("What type of package system is being used"),
						fieldWithPath("[].pkg.metadata.name").description("The name of the package"),
						fieldWithPath("[].pkg.metadata.version").description("The version of the package"),
						fieldWithPath("[].pkg.metadata.packageSourceUrl").description("Location to source code for this package"),
						fieldWithPath("[].pkg.metadata.packageHomeUrl").description("The home page of the package"),
						fieldWithPath("[].pkg.metadata.tags").description("A comma separated list of tags to use for searching"),
						fieldWithPath("[].pkg.metadata.maintainer").description("Who is maintaining this package"),
						fieldWithPath("[].pkg.metadata.description").description("Brief description of the package"),
						fieldWithPath("[].pkg.metadata.sha256").description("Hash of package binary that will be downloaded using SHA256 hash algorithm"),
						fieldWithPath("[].pkg.metadata.iconUrl").description("Url location of a icon"),
						fieldWithPath("[].pkg.templates[].name").description("Name is the path-like name of the template"),
						fieldWithPath("[].pkg.templates[].data").description("Data is the template as string data"),
						fieldWithPath("[].pkg.dependencies").description("The packages that this package depends upon"),
						fieldWithPath("[].pkg.configValues.raw").description("The raw YAML string of configuration values"),
						fieldWithPath("[].pkg.fileHolders").description("Miscellaneous files in a package, e.g. README, LICENSE, etc."),
						fieldWithPath("[].configValues.raw").description("The raw YAML string of configuration values"),
						fieldWithPath("[].manifest").description("The manifest of the release"),
						fieldWithPath("[].platformName").description("Platform name of the release")
					)
				));
	}

	@Test
	public void listReleasesByReleaseName() throws Exception {
		final String releaseName = "myLogRelease2";
		final InstallRequest installRequest = new InstallRequest();
		final PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName("log");
		packageIdentifier.setPackageVersion("1.0.0");
		packageIdentifier.setRepositoryName("notused");
		installRequest.setPackageIdentifier(packageIdentifier);
		final InstallProperties installProperties = new InstallProperties();
		installProperties.setReleaseName(releaseName);
		installProperties.setPlatformName("test");
		installRequest.setInstallProperties(installProperties);

		installPackage(installRequest);

		this.mockMvc.perform(
			get("/api/list/{releaseName}", releaseName)).andDo(print())
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(
					responseFields(
						fieldWithPath("[].name").description("Name of the release"),
						fieldWithPath("[].version").description("Version of the release"),
						fieldWithPath("[].info.status.statusCode").description(
							String.format("StatusCode of the release's status (%s)",
								StringUtils.arrayToCommaDelimitedString(StatusCode.values()))
						),
						fieldWithPath("[].info.status.platformStatus").description("Status from the underlying platform"),
						fieldWithPath("[].info.firstDeployed").description("Date/Time of first deployment"),
						fieldWithPath("[].info.lastDeployed").description("Date/Time of last deployment"),
						fieldWithPath("[].info.deleted").description("Date/Time of when the release was deleted"),
						fieldWithPath("[].info.description").description("Human-friendly 'log entry' about this release"),
						fieldWithPath("[].pkg.metadata.apiVersion").description("The Package Index spec version this file is based on"),
						fieldWithPath("[].pkg.metadata.origin").description("Indicates the origin of the repository (free form text)"),
						fieldWithPath("[].pkg.metadata.repositoryId").description("The repository ID this Package Index file belongs to"),
						fieldWithPath("[].pkg.metadata.kind").description("What type of package system is being used"),
						fieldWithPath("[].pkg.metadata.name").description("The name of the package"),
						fieldWithPath("[].pkg.metadata.version").description("The version of the package"),
						fieldWithPath("[].pkg.metadata.packageSourceUrl").description("Location to source code for this package"),
						fieldWithPath("[].pkg.metadata.packageHomeUrl").description("The home page of the package"),
						fieldWithPath("[].pkg.metadata.tags").description("A comma separated list of tags to use for searching"),
						fieldWithPath("[].pkg.metadata.maintainer").description("Who is maintaining this package"),
						fieldWithPath("[].pkg.metadata.description").description("Brief description of the package"),
						fieldWithPath("[].pkg.metadata.sha256").description("Hash of package binary that will be downloaded using SHA256 hash algorithm"),
						fieldWithPath("[].pkg.metadata.iconUrl").description("Url location of a icon"),
						fieldWithPath("[].pkg.templates[].name").description("Name is the path-like name of the template"),
						fieldWithPath("[].pkg.templates[].data").description("Data is the template as string data"),
						fieldWithPath("[].pkg.dependencies").description("The packages that this package depends upon"),
						fieldWithPath("[].pkg.configValues.raw").description("The raw YAML string of configuration values"),
						fieldWithPath("[].pkg.fileHolders").description("Miscellaneous files in a package, e.g. README, LICENSE, etc."),
						fieldWithPath("[].configValues.raw").description("The raw YAML string of configuration values"),
						fieldWithPath("[].manifest").description("The manifest of the release"),
						fieldWithPath("[].platformName").description("Platform name of the release")
					)
				));
	}
}
