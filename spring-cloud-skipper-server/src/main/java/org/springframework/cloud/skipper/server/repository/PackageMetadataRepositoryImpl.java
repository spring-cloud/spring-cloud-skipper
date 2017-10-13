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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.SkipperUtils;
import org.springframework.cloud.skipper.domain.PackageMetadata;

/**
 * @author Ilayaperumal Gopinathan
 */
public class PackageMetadataRepositoryImpl implements PackageMetadataRepositoryCustom {

	@Autowired
	private PackageMetadataRepository packageMetadataRepository;

	@Override
	public PackageMetadata findLatestPackage(String packageName) {
		List<PackageMetadata> packageMetadataList = this.packageMetadataRepository.findByName(packageName);
		PackageMetadata latestPackage = null;
		int lastVersion = 0;
		for (PackageMetadata packageMetadata : packageMetadataList) {
			// Find the latest package
			int currentVersion = SkipperUtils.getNumberedVersion(packageMetadata.getVersion());
			if (currentVersion > lastVersion) {
				lastVersion = currentVersion;
				latestPackage = packageMetadata;
			}
		}
		return latestPackage;
	}
}
