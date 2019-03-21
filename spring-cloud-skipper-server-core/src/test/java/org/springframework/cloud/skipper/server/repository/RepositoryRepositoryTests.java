/*
 * Copyright 2017 the original author or authors.
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.domain.Repository;
import org.springframework.cloud.skipper.server.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
public class RepositoryRepositoryTests extends AbstractIntegrationTest {

	@Autowired
	private RepositoryRepository repositoryRepository;

	@After
	@Before
	public void cleanupRepository() {
		deleteRepoIfExists("stable");
		deleteRepoIfExists("unstable");
	}

	private void deleteRepoIfExists(String repoName) {
		Repository repo = repositoryRepository.findByName(repoName);
		if (repo != null) {
			this.repositoryRepository.delete(repo);
		}
	}

	@Test
	public void basicCrud() {
		RepositoryCreator.createTwoRepositories(repositoryRepository);
		Iterable<Repository> repositories = repositoryRepository.findAll();
		assertThat(repositories).isNotEmpty();
		Repository repo1 = repositoryRepository.findByName("stable");
		assertThat(repo1.getUrl()).isEqualTo("http://www.example.com/skipper/repository/stable");
		Repository repo2 = repositoryRepository.findByName("unstable");
		assertThat(repo2.getUrl()).isEqualTo("http://www.example.com/skipper/repository/unstable");
	}
}
