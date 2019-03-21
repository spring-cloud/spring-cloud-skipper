/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.cloud.skipper.shell.command.support;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Pollack
 */
public class TargetCredentialsTests {

	@Test
	public void testToString() throws Exception {
		TargetCredentials targetCredentials = new TargetCredentials(true);
		assertThat(targetCredentials.toString()).isEqualTo("[Uses OAuth2 Access Token]");
		targetCredentials = new TargetCredentials("username", "password");
		assertThat(targetCredentials.toString()).isEqualTo("Credentials [username='username', password='********']");

		assertThat(targetCredentials).isEqualTo(new TargetCredentials("username", "password"));
	}

}
