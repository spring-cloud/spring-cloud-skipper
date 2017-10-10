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

import org.springframework.http.MediaType;

import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Gunnar Hillert
 */
public class AboutDocumentation extends BaseDocumentation {

	@Test
	public void getMetaInformation() throws Exception {
		this.mockMvc.perform(get("/about").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
			.andDo(this.documentationHandler.document(
				responseFields(
					fieldWithPath("name").description("Name of the Skipper server."),
					fieldWithPath("version").description(
						"Version of the Skipper server.")
				)
			)
		);
	}
}
