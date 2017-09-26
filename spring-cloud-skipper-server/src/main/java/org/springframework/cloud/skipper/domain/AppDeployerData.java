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
package org.springframework.cloud.skipper.domain;

import javax.persistence.Entity;

/**
 * @author Mark Pollack
 */
@Entity
public class AppDeployerData extends AbstractEntity {

	private String releaseName;

	private Integer releaseVersion;

	// Store deployment Ids associated with the given release.
	private String deploymentData;

	public AppDeployerData() {
	}

	public String getReleaseName() {
		return releaseName;
	}

	public void setReleaseName(String releaseName) {
		this.releaseName = releaseName;
	}

	public Integer getReleaseVersion() {
		return releaseVersion;
	}

	public void setReleaseVersion(Integer releaseVersion) {
		this.releaseVersion = releaseVersion;
	}

	public String getDeploymentData() {
		return deploymentData;
	}

	public void setDeploymentData(String deploymentData) {
		this.deploymentData = deploymentData;
	}
}
