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
package org.springframework.cloud.skipper.server.repository;

import java.util.List;

import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.domain.SkipperPackageMetadata;
import org.springframework.cloud.skipper.server.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
public class PackageMetadataRepositoryTests extends AbstractIntegrationTest {

	@Autowired
	private PackageMetadataRepository packageMetadataRepository;

	@Autowired
	private RepositoryRepository repositoryRepository;

	@Test
	public void basicCrud() {
		PackageMetadataCreator.createTwoPackages(this.packageMetadataRepository);
		Iterable<SkipperPackageMetadata> packages = this.packageMetadataRepository.findAll();
		assertThat(packages).isNotEmpty();
		assertThat(packages).hasSize(2);
		List<SkipperPackageMetadata> packagesNamed1 = this.packageMetadataRepository.findByNameRequired("package1");
		assertThat(packagesNamed1).isNotEmpty();
		assertThat(packagesNamed1).hasSize(1);
		assertThat(packagesNamed1.get(0).getOrigin()).isEqualTo("www.package-repos.com/repo1");
		assertThat(packagesNamed1.get(0).getMaintainer()).isEqualTo("Alan Hale Jr.");
		List<SkipperPackageMetadata> packagesNamed2 = this.packageMetadataRepository.findByNameRequired("package2");
		assertThat(packagesNamed2).isNotEmpty();
		assertThat(packagesNamed2).hasSize(1);
		assertThat(packagesNamed2.get(0).getMaintainer()).isEqualTo("Bob Denver");
		assertThat(packagesNamed2.get(0).getOrigin()).isEqualTo("www.package-repos.com/repo2");
	}

	@Test
	public void verifyMultipleVersions() {
		PackageMetadataCreator.createPackageWithMultipleVersions(this.packageMetadataRepository);
		Iterable<SkipperPackageMetadata> packages = this.packageMetadataRepository.findAll();
		assertThat(packages).isNotEmpty();
		assertThat(packages).hasSize(4);
		SkipperPackageMetadata latestPackage1 = this.packageMetadataRepository.findFirstByNameOrderByVersionDesc("package1");
		assertThat(latestPackage1.getVersion()).isEqualTo("2.0.0");
		SkipperPackageMetadata latestPackage2 = this.packageMetadataRepository.findFirstByNameOrderByVersionDesc("package2");
		assertThat(latestPackage2.getVersion()).isEqualTo("1.1.0");
	}

	@Test
	public void findByNameQueries() {
		PackageMetadataCreator.createPackageWithMultipleVersions(this.packageMetadataRepository);
		Iterable<SkipperPackageMetadata> packages = this.packageMetadataRepository.findByNameContainingIgnoreCase("PACK");
		assertThat(packages).isNotEmpty();
		assertThat(packages).hasSize(4);
		Iterable<SkipperPackageMetadata> packages2 = this.packageMetadataRepository.findByNameContainingIgnoreCase("age");
		assertThat(packages2).isNotEmpty();
		assertThat(packages2).hasSize(4);
		Iterable<SkipperPackageMetadata> packages3 = this.packageMetadataRepository.findByNameContainingIgnoreCase("1");
		assertThat(packages3).isNotEmpty();
		assertThat(packages3).hasSize(2);
	}

	@Test
	public void findByNameAndVersionWithMultipleRepos() {
		String repoName1 = "local1";
		String repoName2 = "remote1";
		String repoName3 = "remote2";
		RepositoryCreator.createRepository(this.repositoryRepository, repoName1, 0);
		RepositoryCreator.createRepository(this.repositoryRepository, repoName2, 1);
		RepositoryCreator.createRepository(this.repositoryRepository, repoName3, 2);
		PackageMetadataCreator.createTwoPackages(this.packageMetadataRepository,
				this.repositoryRepository.findByName(repoName1).getId(), "1.0.0");
		PackageMetadataCreator.createTwoPackages(this.packageMetadataRepository,
				this.repositoryRepository.findByName(repoName2).getId(), "2.0.1");
		PackageMetadataCreator.createTwoPackages(this.packageMetadataRepository,
				this.repositoryRepository.findByName(repoName3).getId(), "1.0.1");
		List<SkipperPackageMetadata> packageMetadataList = this.packageMetadataRepository
				.findByNameAndVersionOrderByApiVersionDesc("package1", "1.0.0");
		assertThat(packageMetadataList.size()).isEqualTo(3);
		assertThat(packageMetadataList.get(0).getName()).isEqualTo("package1");
		assertThat(packageMetadataList.get(0).getVersion()).isEqualTo("1.0.0");
		assertThat(packageMetadataList.get(0).getRepositoryId()).isEqualTo(this.repositoryRepository.findByName(repoName2)
				.getId());
		SkipperPackageMetadata packageMetadata = this.packageMetadataRepository.findByNameAndVersionByMaxRepoOrder("package1",
				"1.0.0");
		assertThat(packageMetadata.getApiVersion()).isEqualTo("1.0.1");
		assertThat(packageMetadata.getRepositoryId()).isEqualTo(this.repositoryRepository.findByName(repoName3).getId());
	}

}
