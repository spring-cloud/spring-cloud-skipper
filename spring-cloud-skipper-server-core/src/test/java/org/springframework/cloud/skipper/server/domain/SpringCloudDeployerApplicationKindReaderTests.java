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
package org.springframework.cloud.skipper.server.domain;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.springframework.cloud.skipper.server.TestResourceUtils;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Mark Pollack
 */
public class SpringCloudDeployerApplicationKindReaderTests {

	@Test
	public void readTests() throws IOException {
		String manifestAyml = StreamUtils.copyToString(
				TestResourceUtils.qualifiedResource(getClass(), "manifest.yml").getInputStream(),
				Charset.defaultCharset());
		List<SpringCloudDeployerApplicationKind> springCloudDeployerApplicationKindList = SpringCloudDeployerApplicationKindReader
				.read(manifestAyml);

		assertThat(springCloudDeployerApplicationKindList).hasSize(2);
		assertTimeOrLogApp(springCloudDeployerApplicationKindList.get(0));
		assertTimeOrLogApp(springCloudDeployerApplicationKindList.get(1));
	}

	private void assertTimeOrLogApp(SpringCloudDeployerApplicationKind springCloudDeployerApplicationKind) {
		if (springCloudDeployerApplicationKind.getMetadata().containsKey("name")) {
			String name = springCloudDeployerApplicationKind.getMetadata().get("name");
			if (name.equals("time-source")) {
				assertTime(springCloudDeployerApplicationKind);
			}
			else if (name.equals("log-sink")) {
				assertLog(springCloudDeployerApplicationKind);
			}
			else {
				fail("Unknown application name");
			}
		}
	}

	private void assertTime(SpringCloudDeployerApplicationKind springCloudDeployerApplicationKind) {
		assertApiAndKind(springCloudDeployerApplicationKind);
		Map<String, String> metadata = springCloudDeployerApplicationKind.getMetadata();
		assertThat(metadata).containsEntry("name", "time-source")
				.containsEntry("count", "5")
				.containsEntry("type", "source");
		SpringCloudDeployerApplicationSpec spec = springCloudDeployerApplicationKind.getSpec();
		assertThat(spec.getResource())
				.isEqualTo("maven://org.springframework.cloud.stream.app:time-source-rabbit:1.2.0.RELEASE");
		assertThat(spec.getApplicationProperties()).hasSize(1);
		assertThat(spec.getApplicationProperties()).containsEntry("log.level", "DEBUG");

		assertThat(spec.getDeploymentProperties()).hasSize(2);
		assertThat(spec.getDeploymentProperties()).containsEntry("memory", "2048");
		assertThat(spec.getDeploymentProperties()).containsEntry("disk", "4");

	}

	private void assertLog(SpringCloudDeployerApplicationKind springCloudDeployerApplicationKind) {
		assertApiAndKind(springCloudDeployerApplicationKind);
		Map<String, String> metadata = springCloudDeployerApplicationKind.getMetadata();
		assertThat(metadata).containsEntry("name", "log-sink")
				.containsEntry("count", "2")
				.containsEntry("type", "sink");
		SpringCloudDeployerApplicationSpec spec = springCloudDeployerApplicationKind.getSpec();
		assertThat(spec.getResource())
				.isEqualTo("maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE");
		assertThat(spec.getApplicationProperties()).hasSize(2);
		assertThat(spec.getApplicationProperties()).containsEntry("log.level", "INFO");
		assertThat(spec.getApplicationProperties()).containsEntry("log.expression", "hellobaby");

		assertThat(spec.getDeploymentProperties()).hasSize(2);
		assertThat(spec.getDeploymentProperties()).containsEntry("memory", "1024");
		assertThat(spec.getDeploymentProperties()).containsEntry("disk", "2");
	}

	private void assertApiAndKind(SpringCloudDeployerApplicationKind springCloudDeployerApplicationKind) {
		assertThat(springCloudDeployerApplicationKind.getApiVersion()).isEqualTo("skipperPackageMetadata/v1");
		assertThat(springCloudDeployerApplicationKind.getKind()).isEqualTo("SpringCloudDeployerApplication");
	}

}
