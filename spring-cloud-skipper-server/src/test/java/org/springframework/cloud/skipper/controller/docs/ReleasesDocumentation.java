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

package org.springframework.cloud.skipper.controller.docs;

import org.junit.Test;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Gunnar Hillert
 */
@ActiveProfiles("repo-test")
@TestPropertySource(properties = { "spring.cloud.skipper.server.platform.local.accounts[test].key=value",
		"maven.remote-repositories.repo1.url=http://repo.spring.io/libs-snapshot" })
public class ReleasesDocumentation extends BaseDocumentation {

	@Test
	public void getAllReleases() throws Exception {
		this.mockMvc.perform(
			get("/releases")
				.param("page", "0")
				.param("size", "10"))
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(this.documentationHandler.document(
				super.paginationRequestParameterProperties,
				super.paginationProperties.and(
					fieldWithPath("_embedded.releases").description("Provides a list of releases")
				).and(super.defaultLinkProperties),
				super.linksForSkipper()
			)
		);
	}
}
