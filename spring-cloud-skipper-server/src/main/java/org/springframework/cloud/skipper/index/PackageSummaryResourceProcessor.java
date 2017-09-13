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
package org.springframework.cloud.skipper.index;

import org.springframework.cloud.skipper.controller.PackageController;
import org.springframework.cloud.skipper.domain.PackageSummary;
import org.springframework.cloud.skipper.domain.skipperpackage.DeployProperties;
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
		DeployProperties deployProperties = new DeployProperties();
		deployProperties.setPackageName(packageSummaryResource.getContent().getName());
		deployProperties.setPackageVersion(packageSummaryResource.getContent().getVersion());
		Link link = linkTo(
				methodOn(PackageController.class).deploy(deployProperties)).withRel("deploy");
		packageSummaryResource.add(link);
		return packageSummaryResource;
	}
}
