/*
 * Copyright 2019 the original author or authors.
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
package io.pivotal.cfenv.core;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Jens Deppe
 */
public class UriInfoTests {

	@Test
	public void createUri() {
		String uri = "mysql://joe:joes_password@localhost:1527/big_db";
		UriInfo uriInfo = new UriInfo(uri);

		assertUriInfoEquals(uriInfo, "localhost", 1527, "joe", "joes_password", "big_db",
				null);
		assertThat(uri).isEqualTo(uriInfo.getUriString());
		assertThat("//joe:joes_password@localhost:1527/big_db")
				.isEqualTo(uriInfo.getSchemeSpecificPart());
	}

	@Test
	public void createUriWithQuery() {
		String uri = "mysql://joe:joes_password@localhost:1527/big_db?p1=v1&p2=v2";
		UriInfo uriInfo = new UriInfo(uri);

		assertUriInfoEquals(uriInfo, "localhost", 1527, "joe", "joes_password", "big_db",
				"p1=v1&p2=v2");
		assertThat(uri).isEqualTo(uriInfo.getUriString());
	}

	@Test
	public void createNoUsernamePassword() {
		String uri = "mysql://localhost:1527/big_db";
		UriInfo uriInfo = new UriInfo(uri);

		assertUriInfoEquals(uriInfo, "localhost", 1527, null, null, "big_db", null);
		assertThat(uri).isEqualTo(uriInfo.getUriString());
	}

	@Test(expected = IllegalArgumentException.class)
	public void createWithUsernameNoPassword() {
		String uri = "mysql://joe@localhost:1527/big_db";
		new UriInfo(uri);
	}

	@Test
	public void createWithExplicitParameters() {
		String uri = "mysql://joe:joes_password@localhost:1527/big_db";
		UriInfo uriInfo = new UriInfo("mysql", "localhost", 1527, "joe", "joes_password",
				"big_db");

		assertUriInfoEquals(uriInfo, "localhost", 1527, "joe", "joes_password", "big_db",
				null);
		assertThat(uri).isEqualTo(uriInfo.getUriString());
	}

	private void assertUriInfoEquals(UriInfo uriInfo, String host, int port,
									 String username, String password, String path, String query) {
		assertThat(host).isEqualTo(uriInfo.getHost());
		assertThat(port).isEqualTo(uriInfo.getPort());
		assertThat(username).isEqualTo(uriInfo.getUsername());
		assertThat(password).isEqualTo(uriInfo.getPassword());
		assertThat(path).isEqualTo(uriInfo.getPath());
		assertThat(query).isEqualTo(uriInfo.getQuery());
		assertThat("mysql").isEqualTo(uriInfo.getScheme());
	}

}
