/*
 * Copyright 2018 the original author or authors.
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

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.springframework.test.context.ActiveProfiles;

/**
 * @author Janne Valkealahti
 */
@ActiveProfiles("repo-test")
public class CancelDocumentation extends BaseDocumentation {

	@Test
	public void cancelRelease() throws Exception {
		final String releaseName = "myLogRelease";
		install("testapp", "1.0.0", releaseName);
		upgrade("testapp", "1.1.0", releaseName, false);

		this.mockMvc.perform(
				post("/api/release/cancel/{releaseName}", releaseName)).andDo(print())
				.andExpect(status().isAccepted())
				.andDo(this.documentationHandler.document(
						))
				.andReturn();
	}
}
