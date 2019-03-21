/*
 * Copyright 2017-2019 the original author or authors.
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
package org.springframework.cloud.skipper.server.repository;

import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.deployer.spi.local.LocalAppDeployer;
import org.springframework.cloud.deployer.spi.local.LocalDeployerProperties;
import org.springframework.cloud.skipper.domain.Deployer;
import org.springframework.cloud.skipper.server.AbstractIntegrationTest;
import org.springframework.cloud.skipper.server.repository.map.DeployerRepository;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Pollack
 */
@ActiveProfiles("local")
public class DeployerRepositoryTests extends AbstractIntegrationTest {

	@Autowired
	private DeployerRepository deployerRepository;

	@Test
	public void basicCrud() {
		LocalAppDeployer localAppDeployer = new LocalAppDeployer(new LocalDeployerProperties());
		Deployer deployer = new Deployer("localDeployer", "local", localAppDeployer);
		deployer.setDescription("This is a test local Deployer.");
		this.deployerRepository.save(deployer);
		// Count is 2 including the default one which was added at the time of bootstrap.
		assertThat(deployerRepository.count()).isEqualTo(2);
		assertThat(deployer.getId()).isNotEmpty();
		assertThat(deployerRepository.findByName("localDeployer")).isNotNull();
		assertThat(deployerRepository.findByName("localDeployer").getDescription()).isNotNull();
		assertThat(deployerRepository.findByName("default").getDescription()).isNotNull();
	}
}
