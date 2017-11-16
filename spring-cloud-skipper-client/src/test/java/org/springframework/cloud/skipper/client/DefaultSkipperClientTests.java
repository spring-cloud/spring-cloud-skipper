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
package org.springframework.cloud.skipper.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import org.springframework.cloud.skipper.ReleaseNotFoundException;
import org.springframework.cloud.skipper.SkipperException;
import org.springframework.cloud.skipper.domain.Info;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests for {@link DefaultSkipperClient}.
 *
 * @author Mark Pollack
 * @author Janne Valkealahti
 */
public class DefaultSkipperClientTests {

	private final String ERROR1 = "{\"timestamp\":1508161424577," +
			"\"status\":404," +
			"\"error\":\"Not Found\"," +
			"\"exception\":\"org.springframework.cloud.skipper.ReleaseNotFoundException\"," +
			"\"message\":\"Release not found\",\"path\":\"/api/status/mylog\",\"releaseName\":\"mylog\"}";

	private final String ERROR2 = "{\"timestamp\":1508161424577," +
			"\"status\":404," +
			"\"error\":\"Not Found\"," +
			"\"exception\":\"org.springframework.cloud.skipper.SkipperException\"," +
			"\"message\":\"Some skipper message\",\"path\":\"/api/status/mylog\"}";

	@Test
	public void genericTemplateTest() {
		SkipperClient skipperClient = new DefaultSkipperClient("http://localhost:7577");
		assertThat(skipperClient.getSpringCloudDeployerApplicationTemplate()).isNotNull();
		assertThat(skipperClient.getSpringCloudDeployerApplicationTemplate().getData()).isNotEmpty();
	}

	@Test
	public void testStatusReleaseNameFound() {
		RestTemplate restTemplate = new RestTemplate();
		SkipperClient skipperClient = new DefaultSkipperClient("", restTemplate);

		MockRestServiceServer mockServer = MockRestServiceServer.bindTo(restTemplate).build();
		mockServer.expect(requestTo("/status/mylog")).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

		Info status = skipperClient.status("mylog");
		mockServer.verify();

		assertThat(status).isNotNull();
		assertThat(status).isInstanceOf(Info.class);
	}

	@Test(expected = ReleaseNotFoundException.class)
	public void testStatusReleaseNameNotFound() {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setErrorHandler(new SkipperClientResponseErrorHandler(new ObjectMapper()));
		SkipperClient skipperClient = new DefaultSkipperClient("", restTemplate);

		MockRestServiceServer mockServer = MockRestServiceServer.bindTo(restTemplate).build();
		mockServer.expect(requestTo("/status/mylog"))
				.andRespond(withStatus(HttpStatus.NOT_FOUND).body(ERROR1).contentType(MediaType.APPLICATION_JSON));

		skipperClient.status("mylog");
	}

	@Test(expected = SkipperException.class)
	public void testSkipperException() {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setErrorHandler(new SkipperClientResponseErrorHandler(new ObjectMapper()));
		SkipperClient skipperClient = new DefaultSkipperClient("", restTemplate);

		MockRestServiceServer mockServer = MockRestServiceServer.bindTo(restTemplate).build();
		mockServer.expect(requestTo("/status/mylog"))
				.andRespond(withStatus(HttpStatus.NOT_FOUND).body(ERROR2).contentType(MediaType.APPLICATION_JSON));

		skipperClient.status("mylog");
	}
}
