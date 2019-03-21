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
package org.springframework.cloud.skipper.server.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.skipper.SkipperException;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Repository;
import org.springframework.cloud.skipper.server.config.SkipperServerProperties;
import org.springframework.cloud.skipper.server.repository.PackageMetadataRepository;
import org.springframework.cloud.skipper.server.repository.RepositoryRepository;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;

/**
 * Takes repository application configuration properties and update the Repository
 * database on application startup. Entries are only created if no existing Repository
 * with the same name exists.
 *
 * @author Mark Pollack
 * @author Glenn Renfro
 */
public class RepositoryInitializationService {

	private final Logger logger = LoggerFactory.getLogger(RepositoryInitializationService.class);

	private final RepositoryRepository repositoryRepository;

	private final SkipperServerProperties skipperServerProperties;

	private final PackageMetadataService packageMetadataService;

	private final PackageMetadataRepository packageMetadataRepository;

	public RepositoryInitializationService(RepositoryRepository repositoryRepository,
			PackageMetadataRepository packageMetadataRepository,
			PackageMetadataService packageMetadataService,
			SkipperServerProperties skipperServerProperties) {
		this.repositoryRepository = repositoryRepository;
		this.packageMetadataRepository = packageMetadataRepository;
		this.packageMetadataService = packageMetadataService;
		this.skipperServerProperties = skipperServerProperties;
	}

	@EventListener
	@Transactional
	public void initialize(ApplicationReadyEvent event) {
		synchronizeRepositories();
		synchronizePackageMetadata();
	}

	private void synchronizePackageMetadata() {
		if (this.skipperServerProperties.isSynchonizeIndexOnContextRefresh()) {
			loadAllPackageMetadata();
		}
	}

	private void loadAllPackageMetadata() {
		try {
			List<PackageMetadata> packageMetadataList = this.packageMetadataService.downloadPackageMetadata();
			for (PackageMetadata packageMetadata : packageMetadataList) {
				if (this.packageMetadataRepository.findByRepositoryIdAndNameAndVersion(
						packageMetadata.getRepositoryId(),
						packageMetadata.getName(),
						packageMetadata.getVersion()) == null) {
					this.packageMetadataRepository.save(packageMetadata);
				}
			}
		}
		catch (SkipperException e) {
			logger.warn("Could not load package metadata from remote repositories", e);
		}
	}

	private void synchronizeRepositories() {
		List<Repository> configurationRepositories = skipperServerProperties.getPackageRepositories();
		for (Repository configurationRepository : configurationRepositories) {
			if (repositoryRepository.findByName(configurationRepository.getName()) == null) {
				logger.info("Initializing repository database with " + configurationRepository);
				repositoryRepository.save(configurationRepository);
			}
			else {
				logger.warn("Ignoring application configuration for " + configurationRepository +
						".  Repository name already in database.");
			}
		}
	}
}
