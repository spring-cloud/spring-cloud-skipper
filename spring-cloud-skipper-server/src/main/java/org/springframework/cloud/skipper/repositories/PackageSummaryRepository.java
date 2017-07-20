package org.springframework.cloud.skipper.repositories;

import org.springframework.cloud.skipper.index.PackageSummary;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/**
 * @author Mark Pollack
 */
@RepositoryRestResource
public interface PackageSummaryRepository extends PagingAndSortingRepository<PackageSummary, Long> {

	List<PackageSummary> findByName(@Param("name") String name);

}
