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
package org.springframework.cloud.skipper.server.domain;

import java.util.List;

/**
 * Interface to specify the contracts for operations related to {@link ApplicationManifest} and its derivatives.
 *
 * @author Ilayaperumal Gopinathan
 */
public interface ApplicationManifestReader {

	/**
	 * Get all the supported application kinds.
	 *
	 * @return the string array of application kinds.
	 */
	String[] getSupportedKinds();

	/**
	 * Read the given manifest YAML string and convert to the corresponding {@link ApplicationManifest} type based on
	 * the matching reader.
	 *
	 * @param manifest the YAML string that contains the application manifest.
	 * @return the {@link ApplicationManifest} object representation of the YAML string.
	 */
	List<? extends ApplicationManifest> read(String manifest);
}
