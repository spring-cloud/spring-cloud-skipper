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
package org.springframework.cloud.skipper.server.index;

import org.springframework.cloud.skipper.server.controller.SkipperController;
import org.springframework.cloud.skipper.server.domain.PackageSummary;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 * @author Mark Pollack
 */
@Component
public class PackageSummaryResourceProcessor implements ResourceProcessor<Resource<PackageSummary>> {

	@Override
	public Resource<PackageSummary> process(Resource<PackageSummary> packageSummaryResource) {
		Link link = linkTo(
				methodOn(SkipperController.class).install(Long.valueOf(packageSummaryResource.getContent().getId()), null))
						.withRel("install");
		packageSummaryResource.add(link);
		return packageSummaryResource;
	}
}
