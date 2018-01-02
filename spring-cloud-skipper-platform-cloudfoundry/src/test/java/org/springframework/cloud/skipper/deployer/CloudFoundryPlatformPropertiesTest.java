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
package org.springframework.cloud.skipper.deployer;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.skipper.deployer.cloudfoundry.CloudFoundryPlatformProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Donovan Muller
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = CloudFoundryPlatformPropertiesTest.TestConfig.class)
@ActiveProfiles("platform-properties")
public class CloudFoundryPlatformPropertiesTest {

	@Autowired
	private CloudFoundryPlatformProperties cloudFoundryPlatformProperties;

	@Test
	public void deserializationTest() {
		Map<String, CloudFoundryPlatformProperties.CloudFoundryProperties> cfAccounts = this.cloudFoundryPlatformProperties
				.getAccounts();
		assertThat(cfAccounts).hasSize(2);
		assertThat(cfAccounts).containsKeys("dev", "qa");
		assertThat(cfAccounts.get("dev").getConnection().getOrg()).isEqualTo("myOrg");
		assertThat(cfAccounts.get("qa").getConnection().getOrg()).isEqualTo("myOrgQA");
		assertThat(cfAccounts.get("dev").getDeployment().getMemory()).isEqualTo("512m");
		assertThat(cfAccounts.get("dev").getDeployment().getDisk()).isEqualTo("2048m");
		assertThat(cfAccounts.get("dev").getDeployment().getInstances()).isEqualTo(4);
		assertThat(cfAccounts.get("dev").getDeployment().getServices())
				.containsExactly("rabbit", "mysql");
		assertThat(cfAccounts.get("qa").getDeployment().getMemory()).isEqualTo("756m");
		assertThat(cfAccounts.get("qa").getDeployment().getDisk()).isEqualTo("724m");
		assertThat(cfAccounts.get("qa").getDeployment().getInstances()).isEqualTo(2);
		assertThat(cfAccounts.get("qa").getDeployment().getServices())
				.containsExactly("rabbitQA", "mysqlQA");
	}

	@Configuration
	@EnableConfigurationProperties(CloudFoundryPlatformProperties.class)
	static class TestConfig {
	}
}
