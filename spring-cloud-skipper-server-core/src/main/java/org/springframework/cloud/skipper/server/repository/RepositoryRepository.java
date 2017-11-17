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

import org.springframework.cloud.skipper.domain.SkipperRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
@RepositoryRestResource(path = "repositories", collectionResourceRel = "repositories", itemResourceRel = "repository")
public interface RepositoryRepository extends PagingAndSortingRepository<SkipperRepository, Long> {

	SkipperRepository findByName(@Param("name") String name);

	/**
	 * Get all the repositories with their repository order in descending order.
	 *
	 * @return the list of repositories
	 */
	List<SkipperRepository> findAllByOrderByRepoOrderDesc();

}
