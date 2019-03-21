/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.skipper.server.config;

import org.springframework.core.env.Environment;

/**
 * @author Mark Pollack
 */
public interface CloudProfileProvider {

	/**
	 * Identifies if the application is running on a cloud platform
	 *
	 * @param environment the environment to base the decision upon.
	 * @return {@code true} is running on a cloud platform, {@code false} otherwise.
	 */
	boolean isCloudPlatform(Environment environment);

	/**
	 * Get the profile name to add to the spring environment
	 * @return profile name for the cloud
	 */
	String getCloudProfile();
}

