/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.skipper.server.service;

import java.nio.charset.Charset;

import org.junit.Test;

import org.springframework.cloud.skipper.server.TestResourceUtils;
import org.springframework.cloud.skipper.server.util.ArgumentSanitizer;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Glenn Renfro
 */
public class ArgumentSanitizerTests {

	@Test
	public void testNoChange() throws Exception {
		String initialYaml = StreamUtils.copyToString(
				TestResourceUtils.qualifiedResource(getClass(), "nopassword.yaml").getInputStream(),
				Charset.defaultCharset());
		String result = ArgumentSanitizer.sanitizeYml(initialYaml);
		assertThat(result).isEqualTo(initialYaml);
	}

	@Test
	public void testPasswordApps() throws Exception {
		String initialYaml = StreamUtils.copyToString(
				TestResourceUtils.qualifiedResource(getClass(), "password.yaml").getInputStream(),
				Charset.defaultCharset());
		String redactedYaml = StreamUtils.copyToString(
				TestResourceUtils.qualifiedResource(getClass(), "passwordredacted.yaml").getInputStream(),
				Charset.defaultCharset());
		String result = ArgumentSanitizer.sanitizeYml(initialYaml);
		assertThat(result).isEqualTo(redactedYaml);
	}

	@Test
	public void testPasswordDefaultConfig() throws Exception {
		String initialYaml = StreamUtils.copyToString(
				TestResourceUtils.qualifiedResource(getClass(), "configpassword.yaml").getInputStream(),
				Charset.defaultCharset());
		String redactedYaml = StreamUtils.copyToString(
				TestResourceUtils.qualifiedResource(getClass(), "configpasswordredacted.yaml").getInputStream(),
				Charset.defaultCharset());
		String result = ArgumentSanitizer.sanitizeYml(initialYaml);
		assertThat(result).isEqualTo(redactedYaml);
	}
}
