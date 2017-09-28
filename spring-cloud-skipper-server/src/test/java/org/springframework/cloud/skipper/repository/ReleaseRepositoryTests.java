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

import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.AbstractIntegrationTest;
import org.springframework.cloud.skipper.ReleaseNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Ilayaperumal Gopinathan
 */
public class ReleaseRepositoryTests extends AbstractIntegrationTest {

	@Autowired
	private ReleaseRepository releaseRepository;

	@Test
	public void verifyReleaseNotFoundByName() {
		String releaseName = "random";
		try {
			this.releaseRepository.findLatestRelease(releaseName);
			fail("Expected ReleaseNotFoundException");
		}
		catch (ReleaseNotFoundException e) {
			assertThat(e.getMessage().equals(ReleaseNotFoundException.getExceptionMessage(releaseName)));
		}
	}

	@Test
	public void verifyReleaseNotFoundByNameAndVersion() {
		String releaseName = "random";
		int version = 1;
		try {
			this.releaseRepository.findByNameAndVersion(releaseName, version);
			fail("Expected ReleaseNotFoundException");
		}
		catch (ReleaseNotFoundException e) {
			assertThat(e.getMessage().equals(ReleaseNotFoundException.getExceptionMessage(releaseName, version)));
		}
	}
}
