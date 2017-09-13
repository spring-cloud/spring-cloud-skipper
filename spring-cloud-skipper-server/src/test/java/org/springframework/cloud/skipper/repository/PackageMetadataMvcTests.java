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
package org.springframework.cloud.skipper.repository;

import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.AbstractMockMvcTests;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class PackageMetadataMvcTests extends AbstractMockMvcTests {

	@Autowired
	private PackageMetadataRepository packageMetadataRepository;

	@Test
	public void shouldReturnRepositoryIndex() throws Exception {
		mockMvc.perform(get("/")).andDo(print()).andExpect(status().isOk()).andExpect(
				jsonPath("$._links.packageMetadata").exists());
	}

	@Test
	public void testProjection() throws Exception {
		PackageMetadataCreator.createTwoPackages(packageMetadataRepository);
		PackageMetadata packageMetadata = packageMetadataRepository.findByNameAndVersion("package1", "1.0.0");
		assertThat(packageMetadata.getId()).isNotNull();
		packageMetadata = packageMetadataRepository.findByNameAndVersion("package2", "2.0.0");
		assertThat(packageMetadata.getId()).isNotNull();
		mockMvc.perform(get("/packageMetadata?projection=summary")).andDo(print()).andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.packageMetadata[0].version").value("1.0.0"))
				.andExpect(jsonPath("$._embedded.packageMetadata[0].iconUrl")
						.value("http://www.gilligansisle.com/images/a2.gif"))
				.andExpect(jsonPath("$._embedded.packageMetadata[0].description").doesNotExist())
				.andExpect(jsonPath("$._embedded.packageMetadata[0]._links.deploy.href")
						.value("http://localhost/package/deploy"))
				.andExpect(jsonPath("$._embedded.packageMetadata[1].version").value("2.0.0"))
				.andExpect(jsonPath("$._embedded.packageMetadata[1].iconUrl")
						.value("http://www.gilligansisle.com/images/a1.gif"))
				.andExpect(jsonPath("$._embedded.packageMetadata[1]._links.deploy.href")
						.value("http://localhost/package/deploy"))
				.andExpect(jsonPath("$._embedded.packageMetadata[1].description").doesNotExist());

		mockMvc.perform(get("/packageMetadata")).andDo(print()).andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.packageMetadata[0].version").value("1.0.0"))
				.andExpect(jsonPath("$._embedded.packageMetadata[0].description").value("A very cool project"))
				.andExpect(jsonPath("$._embedded.packageMetadata[0]._links.deploy.href")
						.value("http://localhost/package/deploy"))
				.andExpect(jsonPath("$._embedded.packageMetadata[1].version").value("2.0.0"))
				.andExpect(jsonPath("$._embedded.packageMetadata[1].description").value("Another very cool project"))
				.andExpect(jsonPath("$._embedded.packageMetadata[1]._links.deploy.href")
						.value("http://localhost/package/deploy"));
	}

}
