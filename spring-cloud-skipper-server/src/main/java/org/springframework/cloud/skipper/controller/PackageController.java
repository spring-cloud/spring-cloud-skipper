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
package org.springframework.cloud.skipper.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.skipperpackage.DeployProperties;
import org.springframework.cloud.skipper.service.ReleaseService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Package related operations such as (deploy/update).
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
@RestController
@RequestMapping("/package")
public class PackageController {

	private final ReleaseService releaseService;

	@Autowired
	public PackageController(ReleaseService releaseService) {
		this.releaseService = releaseService;
	}

	@RequestMapping(path = "/deploy", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public Release deploy(@RequestBody DeployProperties deployProperties) {
		return this.releaseService.deploy(deployProperties);
	}

	@RequestMapping(path = "update", method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.CREATED)
	public Release update(@RequestBody DeployProperties deployProperties) {
		return this.releaseService.update(deployProperties);
	}
}
