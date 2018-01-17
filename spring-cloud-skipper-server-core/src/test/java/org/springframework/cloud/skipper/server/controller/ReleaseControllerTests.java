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
package org.springframework.cloud.skipper.server.controller;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;

import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.domain.InstallProperties;
import org.springframework.cloud.skipper.domain.InstallRequest;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.Repository;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.server.repository.RepositoryRepository;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 * @author Christian Tzolov
 */
@ActiveProfiles("repo-test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ReleaseControllerTests extends AbstractControllerTests {

	@Autowired
	private RepositoryRepository repositoryRepository;

	@Test
	public void deployTickTock() throws Exception {
		Release release = install("ticktock", "1.0.0", "myTicker");
		assertThat(release.getVersion()).isEqualTo(1);
	}

	@Test
	public void packageDeployRequest() throws Exception {
		String releaseName = "myLogRelease";
		InstallRequest installRequest = new InstallRequest();
		PackageIdentifier packageIdentifier = new PackageIdentifier();
		packageIdentifier.setPackageName("log");
		packageIdentifier.setPackageVersion("1.0.0");
		packageIdentifier.setRepositoryName("notused");
		installRequest.setPackageIdentifier(packageIdentifier);
		InstallProperties installProperties = createInstallProperties(releaseName);
		installRequest.setInstallProperties(installProperties);

		Release release = installPackage(installRequest);
		assertReleaseIsDeployedSuccessfully(releaseName, 1);
		assertThat(release.getVersion()).isEqualTo(1);
	}

	@Test
	public void checkDeployStatus() throws Exception {

		// Deploy
		String releaseName = "test1";
		Release release = install("log", "1.0.0", releaseName);
		assertThat(release.getVersion()).isEqualTo(1);

		// Undeploy
		mockMvc.perform(delete("/api/release/" + releaseName)).andDo(print())
				.andExpect(status().isOk()).andReturn();
		Release deletedRelease = this.releaseRepository.findByNameAndVersion(releaseName, 1);
		assertThat(deletedRelease.getInfo().getStatus().getStatusCode()).isEqualTo(StatusCode.DELETED);
	}

	@Test
	public void checkDeleteReleaseWithPackage() throws Exception {

		// Make the test repo Local
		Repository repo = this.repositoryRepository.findByName("test");
		repo.setLocal(true);
		this.repositoryRepository.save(repo);

		// Deploy
		String releaseNameOne = "test1";
		Release release = install("log", "1.0.0", releaseNameOne);
		assertThat(release.getVersion()).isEqualTo(1);

		String releaseNameTwo = "test2";
		Release release2 = install("log", "1.0.0", releaseNameTwo);
		assertThat(release2.getVersion()).isEqualTo(1);

		// Undeploy
		boolean deletePackage = true;

		MvcResult result = mockMvc.perform(delete("/api/release/" + releaseNameOne + "/" + deletePackage))
				.andDo(print()).andExpect(status().isConflict()).andReturn();

		assertThat(result.getResponse().getContentAsString())
				.contains("Can not delete Package Metadata [log:1.0.0] in Repository [test]. Not all releases of " +
						"this package have the status DELETED. Active Releases [test2]");

		assertThat(this.packageMetadataRepository.findByName("log").size()).isEqualTo(3);

		// Delete the 'release2' only not the package.
		mockMvc.perform(delete("/api/release/" + releaseNameTwo + "/" + false))
				.andDo(print()).andExpect(status().isOk()).andReturn();
		assertThat(this.packageMetadataRepository.findByName("log").size()).isEqualTo(3);

		// Second attempt to delete 'release1' along with its package 'log'.
		mockMvc.perform(delete("/api/release/" + releaseNameOne + "/" + deletePackage))
				.andDo(print()).andExpect(status().isOk()).andReturn();
		assertThat(this.packageMetadataRepository.findByName("log").size()).isEqualTo(0);

	}

	@Test
	public void releaseRollbackAndUndeploy() throws Exception {

		// Deploy
		String releaseName = "test2";
		Release release = install("log", "1.0.0", releaseName);
		assertThat(release.getVersion()).isEqualTo(1);

		// Check manifest
		MvcResult result = mockMvc.perform(get("/api/release/manifest/" + releaseName)).andDo(print())
				.andExpect(status().isOk()).andReturn();
		assertThat(result.getResponse().getContentAsString()).isNotEmpty();

		// Upgrade
		String releaseVersion = "2";
		release = upgrade("log", "1.1.0", releaseName);
		assertThat(release.getVersion()).isEqualTo(2);

		// Check manifest
		result = mockMvc.perform(get("/api/release/manifest/" + releaseName + "/2")).andDo(print())
				.andExpect(status().isOk()).andReturn();
		assertThat(result.getResponse().getContentAsString()).isNotEmpty();

		// Rollback to release version 1, creating a third release version equivalent to
		// the 1st.
		releaseVersion = "3";

		Release rollbackRelease = rollback(releaseName, 1);

		release = this.releaseRepository.findByNameAndVersion(releaseName, Integer.valueOf(releaseVersion));
		assertReleaseIsDeployedSuccessfully(releaseName, 3);

		// TODO the common assert doesn't check for this status code.
		assertThat(release.getInfo().getStatus().getStatusCode()).isEqualTo(StatusCode.DEPLOYED);

		// Undeploy
		mockMvc.perform(delete("/api/release/" + releaseName + "/false"))
				.andDo(print())
				.andExpect(status().isOk()).andReturn();
		Release deletedRelease = this.releaseRepository.findByNameAndVersion(releaseName,
				Integer.valueOf(releaseVersion));
		assertThat(deletedRelease.getInfo().getStatus().getStatusCode()).isEqualTo(StatusCode.DELETED);
	}

	@Test
	public void packageDeployAndUpgrade() throws Exception {
		String releaseName = "myLog";
		Release release = install("log", "1.0.0", releaseName);
		assertThat(release.getVersion()).isEqualTo(1);

		// Upgrade
		release = upgrade("log", "1.1.0", releaseName);

		assertThat(release.getVersion()).isEqualTo(2);
	}

	@Test
	public void testStatusReportsErrorForMissingRelease() throws Exception {
		// In a real container the response is carried over into the error dispatcher, but
		// in the mock a new one is created so we have to assert the status at this
		// intermediate point
		MvcResult result = mockMvc.perform(get("/api/release/status/myLog")).andDo(print())
				.andExpect(status().is4xxClientError()).andReturn();
		MvcResult response = this.mockMvc.perform(new ErrorDispatcher(result, "/error"))
				.andReturn();
		assertThat(response.getResponse().getContentAsString()).contains("ReleaseNotFoundException");
	}

	private class ErrorDispatcher implements RequestBuilder {

		private MvcResult result;

		private String path;

		ErrorDispatcher(MvcResult result, String path) {
			this.result = result;
			this.path = path;
		}

		@Override
		public MockHttpServletRequest buildRequest(ServletContext servletContext) {
			MockHttpServletRequest request = this.result.getRequest();
			request.setDispatcherType(DispatcherType.ERROR);
			request.setRequestURI(this.path);
			return request;
		}
	}
}
