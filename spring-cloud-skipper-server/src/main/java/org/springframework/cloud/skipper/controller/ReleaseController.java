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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.service.ReleaseService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ilayaperumal Gopinathan
 */
@RestController
@RequestMapping("/releases")
public class ReleaseController {

	private final ReleaseService releaseService;

	@Autowired
	public ReleaseController(ReleaseService releaseService) {
		this.releaseService = releaseService;
	}

	@RequestMapping(path = "/revisions/{name}/{max}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public List<Release> getReleaseRevisions(@PathVariable("name") String releaseName,
			@PathVariable("max") int maxRevisions) {
		return this.releaseService.getReleaseRevisions(releaseName, maxRevisions);
	}

	@RequestMapping(path = "/list", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public List<Release> getLatestDeployedOrFailed() {
		return this.releaseService.getLatestDeployedOrFailed();
	}

	@RequestMapping(path = "/listByName/{name}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public List<Release> getLatestDeployedOrFailedByName(@PathVariable("name") String releaseName) {
		return this.releaseService.getLatestDeployedOrFailed(releaseName);
	}

}
