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
package org.springframework.cloud.skipper.server.deployer;

import org.springframework.cloud.skipper.domain.SkipperRelease;

/**
 * Manages the lifecycle of a releases.
 *
 * The current implementation is a simple sequence of AppDeployer commands, but more
 * sophisticated implementations based on Spring Cloud State Machine are possible.
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
public interface ReleaseManager {

	/**
	 * Install the requested release.
	 * @param release the requested release
	 * @return the release object after requesting installation
	 */
	// TODO return just release name and version
	SkipperRelease install(SkipperRelease release);

	/**
	 * Create a report of what apps should be updated and deleted upon upgrade. The return
	 * value is expected to be passed into the update method.
	 * @param existingRelease the existing release that is deployed
	 * @param replacingRelease the release that is to be deployed in place of the existing
	 * release
	 * @return a report describing the actions to take to update
	 */
	ReleaseAnalysisReport createReport(SkipperRelease existingRelease, SkipperRelease replacingRelease);

	/**
	 * Given a report of what should be upgraded, perform the upgrade. It is expected this is
	 * a long lived operation executed asynchronously to the client request.
	 * @param releaseAnalysisReport report describing what actions to take to upgrade
	 */
	void upgrade(ReleaseAnalysisReport releaseAnalysisReport);

	/**
	 * Delete the release
	 * @param release the release to delete
	 * @return the updated release object after deltion
	 */
	SkipperRelease delete(SkipperRelease release);

	/**
	 * Get the status of the release, by querying the database. The
	 * {@link org.springframework.cloud.skipper.server.service.ReleaseStateUpdateService} is
	 * scheduled ot update the state in the database periodically.
	 * @param release the release to update state for
	 * @return the updated release
	 */
	SkipperRelease status(SkipperRelease release);

}
