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
package org.springframework.cloud.skipper.repository;

import java.util.List;

import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
@RepositoryRestResource(path = "packageMetadata", collectionResourceRel = "packageMetadata")
public interface PackageMetadataRepository extends PagingAndSortingRepository<PackageMetadata, String> {

	List<PackageMetadata> findByName(@Param("name") String name);

	List<PackageMetadata> findByNameLike(@Param("name") String name);

	PackageMetadata findByNameAndVersion(@Param("name") String name, @Param("version") String version);

}
