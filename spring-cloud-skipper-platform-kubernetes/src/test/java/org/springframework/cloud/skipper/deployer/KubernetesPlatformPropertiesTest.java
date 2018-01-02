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
import org.springframework.cloud.deployer.spi.kubernetes.EntryPointStyle;
import org.springframework.cloud.deployer.spi.kubernetes.ImagePullPolicy;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesDeployerProperties;
import org.springframework.cloud.skipper.deployer.kubernetes.KubernetesPlatformProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Donovan Muller
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = KubernetesPlatformPropertiesTest.TestConfig.class)
@ActiveProfiles("platform-properties")
public class KubernetesPlatformPropertiesTest {

	@Autowired
	private KubernetesPlatformProperties kubernetesPlatformProperties;

	@Test
	public void deserializationTest() {
		Map<String, KubernetesDeployerProperties> k8sAccounts = this.kubernetesPlatformProperties.getAccounts();
		assertThat(k8sAccounts).hasSize(2);
		assertThat(k8sAccounts).containsKeys("dev", "qa");
		assertThat(k8sAccounts.get("dev").getNamespace()).isEqualTo("devNamespace");
		assertThat(k8sAccounts.get("dev").getImagePullPolicy()).isEqualTo(ImagePullPolicy.Always);
		assertThat(k8sAccounts.get("dev").getEntryPointStyle()).isEqualTo(EntryPointStyle.exec);
		assertThat(k8sAccounts.get("dev").getLimits().getCpu()).isEqualTo("4");
		assertThat(k8sAccounts.get("qa").getNamespace()).isEqualTo("qaNamespace");
		assertThat(k8sAccounts.get("qa").getImagePullPolicy()).isEqualTo(ImagePullPolicy.IfNotPresent);
		assertThat(k8sAccounts.get("qa").getEntryPointStyle()).isEqualTo(EntryPointStyle.boot);
		assertThat(k8sAccounts.get("qa").getLimits().getMemory()).isEqualTo("1024m");
	}

	@Configuration
	@EnableConfigurationProperties(KubernetesPlatformProperties.class)
	static class TestConfig {
	}
}
