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

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.ReleaseNotFoundException;
import org.springframework.cloud.skipper.domain.SkipperRelease;
import org.springframework.cloud.skipper.domain.StatusCode;

/**
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
public class ReleaseRepositoryImpl implements ReleaseRepositoryCustom {

	@Autowired
	private ReleaseRepository releaseRepository;

	@Override
	public SkipperRelease findLatestRelease(String releaseName) {
		SkipperRelease latestRelease = this.releaseRepository.findTopByNameOrderByVersionDesc(releaseName);
		if (latestRelease == null) {
			throw new ReleaseNotFoundException(releaseName);
		}
		return latestRelease;
	}

	@Override
	public SkipperRelease findLatestDeployedRelease(String releaseName) {
		List<SkipperRelease> releases = this.releaseRepository.findByNameOrderByVersionDesc(releaseName);
		for (SkipperRelease release : releases) {
			if (release.getInfo().getStatus().getStatusCode().equals(StatusCode.DEPLOYED)) {
				return release;
			}
		}
		throw new ReleaseNotFoundException(releaseName);
	}

	@Override
	public SkipperRelease findLatestReleaseForUpdate(String releaseName) {
		List<SkipperRelease> releases = this.releaseRepository.findByNameOrderByVersionDesc(releaseName);
		for (SkipperRelease release : releases) {
			if (release.getInfo().getStatus().getStatusCode().equals(StatusCode.DEPLOYED) ||
					release.getInfo().getStatus().getStatusCode().equals(StatusCode.DELETED)) {
				return release;
			}
		}
		throw new ReleaseNotFoundException(releaseName);
	}

	@Override
	public SkipperRelease findReleaseToRollback(String releaseName) {
		SkipperRelease latestRelease = this.releaseRepository.findLatestReleaseForUpdate(releaseName);
		List<SkipperRelease> releases = this.releaseRepository.findByNameOrderByVersionDesc(releaseName);
		for (SkipperRelease release : releases) {
			if ((release.getInfo().getStatus().getStatusCode().equals(StatusCode.DEPLOYED) ||
					release.getInfo().getStatus().getStatusCode().equals(StatusCode.DELETED)) &&
					release.getVersion() != latestRelease.getVersion()) {
				return release;
			}
		}
		throw new ReleaseNotFoundException(releaseName);
	}

	@Override
	public SkipperRelease findByNameAndVersion(String releaseName, int version) {
		Iterable<SkipperRelease> releases = this.releaseRepository.findAll();

		SkipperRelease matchingRelease = null;
		for (SkipperRelease release : releases) {
			if (release.getName().equals(releaseName) && release.getVersion() == version) {
				matchingRelease = release;
				break;
			}
		}
		if (matchingRelease == null) {
			throw new ReleaseNotFoundException(releaseName, version);
		}
		return matchingRelease;
	}

	@Override
	public List<SkipperRelease> findReleaseRevisions(String releaseName, int revisions) {
		int latestVersion = findLatestRelease(releaseName).getVersion();
		int lowerVersion = latestVersion - Integer.valueOf(revisions);
		return this.releaseRepository.findByNameAndVersionBetweenOrderByNameAscVersionDesc(releaseName,
				lowerVersion + 1, latestVersion);
	}

	@Override
	public List<SkipperRelease> findLatestDeployedOrFailed(String releaseName) {
		return getDeployedOrFailed(this.releaseRepository.findByNameIgnoreCaseContaining(releaseName));
	}

	@Override
	public List<SkipperRelease> findLatestDeployedOrFailed() {
		return getDeployedOrFailed(this.releaseRepository.findAll());
	}

	private List<SkipperRelease> getDeployedOrFailed(Iterable<SkipperRelease> allReleases) {
		List<SkipperRelease> releases = new ArrayList<>();
		for (SkipperRelease release : allReleases) {
			StatusCode releaseStatusCode = release.getInfo().getStatus().getStatusCode();
			if (releaseStatusCode.equals(StatusCode.DEPLOYED) || releaseStatusCode.equals(StatusCode.FAILED)) {
				releases.add(release);
			}
		}
		return releases;
	}

	@Override
	public SkipperRelease findLatestReleaseIfDeleted(String releaseName) {
		SkipperRelease latestRelease = this.releaseRepository.findTopByNameOrderByVersionDesc(releaseName);
		return (latestRelease != null &&
				latestRelease.getInfo().getStatus().getStatusCode().equals(StatusCode.DELETED)) ? latestRelease : null;
	}
}
