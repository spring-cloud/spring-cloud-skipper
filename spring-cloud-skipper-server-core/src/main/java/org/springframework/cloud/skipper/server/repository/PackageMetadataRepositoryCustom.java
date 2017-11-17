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

import org.springframework.cloud.skipper.SkipperException;
import org.springframework.cloud.skipper.domain.SkipperPackageMetadata;
import org.springframework.data.repository.query.Param;

/**
 * @author Ilayaperumal Gopinathan
 */
public interface PackageMetadataRepositoryCustom {

	/**
	 * Find the {@link SkipperPackageMetadata} with the given name, version and also from the
	 * repository that has the highest order set.
	 *
	 * @param name the name of the package metadata
	 * @param version the version of the package metadata
	 * @return the package metadata
	 */
	SkipperPackageMetadata findByNameAndVersionByMaxRepoOrder(@Param("name") String name, @Param("version") String version);

	/**
	 * Find the list of {@link SkipperPackageMetadata} by the given package name.
	 *
	 * @param name the package name
	 * @return the list of package metadata by the given name
	 * @throws {@link org.springframework.cloud.skipper.SkipperException} if there is no
	 * package exists with the given name.
	 */
	List<SkipperPackageMetadata> findByNameRequired(@Param("name") String name) throws SkipperException;

	/**
	 * Find the {@link SkipperPackageMetadata} given the package name and version. If packageVersion
	 * is specified, delegate to findByNameAndVersionByMaxRepoOrder, otherwise delegate to
	 * findFirstByNameOrderByVersionDesc. Throw an e
	 * @param packageName the name of the package
	 * @param packageVersion the version, maybe empty.
	 * @return the package metadata
	 * @throws {@link org.springframework.cloud.skipper.SkipperException} if there is no
	 * package exists with the given name.
	 */
	SkipperPackageMetadata findByNameAndOptionalVersionRequired(String packageName, String packageVersion);

}
