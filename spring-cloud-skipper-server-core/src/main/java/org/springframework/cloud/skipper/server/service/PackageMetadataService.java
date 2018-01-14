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
package org.springframework.cloud.skipper.server.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.skipper.SkipperException;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.Repository;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.io.TempFileUtils;
import org.springframework.cloud.skipper.server.repository.PackageMetadataRepository;
import org.springframework.cloud.skipper.server.repository.ReleaseRepository;
import org.springframework.cloud.skipper.server.repository.RepositoryRepository;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * Downloads package metadata from known repositories and deletes PackageMetadata.
 * @author Mark Pollack
 */
public class PackageMetadataService implements ResourceLoaderAware {

	private final Logger logger = LoggerFactory.getLogger(PackageMetadataService.class);

	private final RepositoryRepository repositoryRepository;

	private final PackageMetadataRepository packageMetadataRepository;

	private final ReleaseRepository releaseRepository;

	private ResourceLoader resourceLoader;

	public PackageMetadataService(RepositoryRepository repositoryRepository,
			PackageMetadataRepository packageMetadataRepository,
			ReleaseRepository releaseRepository) {
		this.repositoryRepository = repositoryRepository;
		this.packageMetadataRepository = packageMetadataRepository;
		this.releaseRepository = releaseRepository;
	}

	@Transactional
	public void deleteIfAllReleasesDeleted(String packageName) {
		List<PackageMetadata> packageMetadataList = this.packageMetadataRepository.findByNameRequired(packageName);
		for (PackageMetadata packageMetadata : packageMetadataList) {
			deleteIfAllReleasesDeleted(packageMetadata);
		}
	}

	/**
	 * Delete all versions of the package metadata only if the latest releases currently using
	 * it are in the StatusCode.DELETED state.
	 * @param packageMetadata the package metadata
	 */
	@Transactional
	public void deleteIfAllReleasesDeleted(PackageMetadata packageMetadata) {

		List<Release> releases = this.releaseRepository.findByRepositoryIdAndPackageMetadataIdOrderByNameAscVersionDesc(
				packageMetadata.getRepositoryId(),
				packageMetadata.getId());
		boolean canDelete = true;

		List<Release> releasesFromLocalRepositories = filterReleasesFromLocalRepos(releases, packageMetadata.getName());

		// Only keep latest release per release name.
		Map<String, Release> latestReleaseMap = new HashMap<>();
		for (Release release : releasesFromLocalRepositories) {
			if (!latestReleaseMap.containsKey(release.getName())) {
				latestReleaseMap.put(release.getName(), release);
			}
		}

		// Find releases that are still 'active' so can't be deleted
		List<String> activeReleaseNames = new ArrayList<>();
		for (Release release : latestReleaseMap.values()) {
			if (!release.getInfo().getStatus().getStatusCode().equals(StatusCode.DELETED)) {
				canDelete = false;
				activeReleaseNames.add(release.getName());
			}
		}
		if (!canDelete) {
			Repository repository = this.repositoryRepository.findOne(packageMetadata.getRepositoryId());
			throw new SkipperException(String.format("Can not delete Package Metadata [%s:%s] in Repository [%s]. " +
					"Not all releases of this package have the status DELETED. Active Releases [%s]",
					packageMetadata.getName(), packageMetadata.getVersion(), repository.getName(),
					StringUtils.collectionToCommaDelimitedString(activeReleaseNames)));
		}
		packageMetadataRepository.deleteByRepositoryIdAndName(packageMetadata.getRepositoryId(),
				packageMetadata.getName());
	}

	private List<Release> filterReleasesFromLocalRepos(List<Release> releases, String packageMetadataName) {
		List<Release> releasesFromLocalRepositories = new ArrayList<>();
		for (Release release : releases) {
			Repository repository = this.repositoryRepository.findOne(release.getRepositoryId());
			if (repository == null) {
				throw new SkipperException("Can not delete Package Metadata [" + packageMetadataName + "]. " +
						"Associated repository not found.");
			}
			if (repository.isLocal()) {
				releasesFromLocalRepositories.add(release);
			}
		}
		return releasesFromLocalRepositories;
	}

	/**
	 * Download package metadata from all repositories.
	 * @return A list of package metadata, not yet persisted in the PackageMetadataRepository.
	 */
	public List<PackageMetadata> downloadPackageMetadata() {
		List<PackageMetadata> finalMetadataList = new ArrayList<>();
		Path targetPath = null;
		try {
			targetPath = TempFileUtils.createTempDirectory("skipperIndex");
			for (Repository packageRepository : this.repositoryRepository.findAll()) {
				try {
					if (!packageRepository.isLocal()) {
						Resource resource = resourceLoader.getResource(packageRepository.getUrl()
								+ File.separator + "index.yml");
						if (resource.exists()) {
							logger.info("Downloading package metadata from " + resource);
							File downloadedFile = new File(targetPath.toFile(), computeFilename(resource));
							StreamUtils.copy(resource.getInputStream(), new FileOutputStream(downloadedFile));
							List<File> downloadedFileAsList = new ArrayList<>();
							downloadedFileAsList.add(downloadedFile);
							List<PackageMetadata> downloadedPackageMetadata = deserializeFromIndexFiles(
									downloadedFileAsList);
							for (PackageMetadata packageMetadata : downloadedPackageMetadata) {
								packageMetadata.setRepositoryId(packageRepository.getId());
							}
							finalMetadataList.addAll(downloadedPackageMetadata);
						}
						else {
							logger.info("Package metadata index resource does not exist: "
									+ resource.getDescription());
						}
					}
				}
				catch (IOException e) {
					throw new SkipperException("Could not process package file from " + packageRepository.getName(), e);
				}
			}
		}
		finally {
			if (targetPath != null && !FileSystemUtils.deleteRecursively(targetPath.toFile())) {
				logger.warn("Temporary directory can not be deleted: " + targetPath);
			}
		}
		return finalMetadataList;
	}

	protected List<PackageMetadata> deserializeFromIndexFiles(List<File> indexFiles) {
		List<PackageMetadata> packageMetadataList = new ArrayList<>();
		YAMLMapper yamlMapper = new YAMLMapper();
		yamlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		for (File indexFile : indexFiles) {
			try {
				MappingIterator<PackageMetadata> it = yamlMapper.readerFor(PackageMetadata.class).readValues(indexFile);
				while (it.hasNextValue()) {
					PackageMetadata packageMetadata = it.next();
					packageMetadataList.add(packageMetadata);
				}
			}
			catch (IOException e) {
				throw new IllegalArgumentException("Can't parse Release manifest YAML", e);
			}
		}
		return packageMetadataList;
	}

	// package protected for testing
	String computeFilename(Resource resource) throws IOException {
		URI uri = resource.getURI();
		StringBuilder stringBuilder = new StringBuilder();
		String scheme = uri.getScheme();
		if (scheme.equals("file")) {
			stringBuilder.append("file");
			if (uri.getPath() != null) {
				stringBuilder.append(uri.getPath().replaceAll("/", "_"));
			}
			else {
				String relativeFilename = uri.getSchemeSpecificPart().replaceAll("^./", "/dot/");
				stringBuilder.append(relativeFilename.replaceAll("/", "_"));
			}
		}
		else if (scheme.equals("http") || scheme.equals("https")) {
			stringBuilder.append(uri.getHost()).append(uri.getPath().replaceAll("/", "_"));
		}
		else {
			logger.warn("Package repository with scheme " + scheme
					+ " is not supported.  Skipping processing this repository.");
		}
		return stringBuilder.toString();
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

}
