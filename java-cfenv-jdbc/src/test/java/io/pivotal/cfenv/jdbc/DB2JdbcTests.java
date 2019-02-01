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
package io.pivotal.cfenv.jdbc;

import io.pivotal.cfenv.core.CfCredentials;
import io.pivotal.cfenv.core.UriInfo;
import org.junit.Test;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/**
 * @author Mark Pollack
 */
public class DB2JdbcTests extends AbstractJdbcTests {

	private static final String INSTANCE_NAME = "database";
	private static final String SERVICE_NAME = "db2-ups";


	@Test
	public void db2ServiceCreation() {
		mockVcapServices(getServicesPayload(
				getUserProvidedServicePayload(SERVICE_NAME, hostname, port, username, password, INSTANCE_NAME,
						DB2JdbcUrlCreator.DB2_SCHEME + ":")
		));

		CfJdbcEnv cfJdbcEnv = new CfJdbcEnv();
		CfJdbcService cfJdbcService = cfJdbcEnv.findJdbcServiceByName(SERVICE_NAME);
		String jdbcUrl = cfJdbcService.getUrl();
		String expectedJdbcUrl = getExpectedJdbcUrl(SqlServerJdbcUrlCreator.SQLSERVER_SCHEME, INSTANCE_NAME);
		assertThat(expectedJdbcUrl).isEqualTo(jdbcUrl);

		UriInfo uriInfo = cfJdbcService.getCredentials().getUriInfo();
		assertUriInfo(uriInfo, DB2JdbcUrlCreator.DB2_SCHEME, INSTANCE_NAME);
	}

	@Test
	public void db2ServiceCreationWithSpecialChars() {
		String userWithSpecialChars = "u%u:u+";
		String passwordWithSpecialChars = "p%p:p+";

		mockVcapServices(getServicesPayload(
				getUserProvidedServicePayload(SERVICE_NAME, hostname, port, userWithSpecialChars,
						passwordWithSpecialChars, INSTANCE_NAME, DB2JdbcUrlCreator.DB2_SCHEME + ":")
		));

		CfJdbcEnv cfJdbcEnv = new CfJdbcEnv();
		CfJdbcService cfJdbcService = cfJdbcEnv.findJdbcServiceByName(SERVICE_NAME);

		String jdbcUrl = cfJdbcService.getUrl();
		String expectedJdbcUrl = getExpectedJdbcUrl(SqlServerJdbcUrlCreator.SQLSERVER_SCHEME, INSTANCE_NAME,
				userWithSpecialChars, passwordWithSpecialChars);
		assertThat(expectedJdbcUrl).isEqualTo(jdbcUrl);

		UriInfo uriInfo = cfJdbcService.getCredentials().getUriInfo();
		assertUriInfo(uriInfo, DB2JdbcUrlCreator.DB2_SCHEME, INSTANCE_NAME, userWithSpecialChars,
				passwordWithSpecialChars);
	}

	@Test
	public void db2ServiceCreationWithNoUri() {
		mockVcapServices(getServicesPayload(
				getUserProvidedServicePayloadWithNoUri(SERVICE_NAME, hostname, port, username, password, INSTANCE_NAME)
		));

		CfJdbcEnv cfJdbcEnv = new CfJdbcEnv();
		// Connector library returns a ServiceInfo via CloudFoundryFallbackServiceInfoCreator of type BaseServiceInfo.
		// In this case we would need to fall back to using the standard CfEnv APIs to access the information.
		assertThatThrownBy(() -> {
			cfJdbcEnv.findJdbcServiceByName(SERVICE_NAME);
		}).isInstanceOf(IllegalArgumentException.class).hasMessage(
				"No database service with name [db2-ups] was found.");

		CfCredentials cfCredentials = cfJdbcEnv.findCredentialsByName(SERVICE_NAME);
		assertThat(cfCredentials.getHost()).isEqualTo(hostname);
	}

	@Test
	public void db2ServiceCreationWithJdbcUrl() {
		mockVcapServices(getServicesPayload(
				getDB2ServicePayloadWithJdbcurl(SERVICE_NAME, hostname, port, username, password, INSTANCE_NAME,
						DB2JdbcUrlCreator.DB2_SCHEME + ":")
		));

		CfJdbcEnv cfJdbcEnv = new CfJdbcEnv();
		String jdbcUrl = cfJdbcEnv.findJdbcServiceByName(SERVICE_NAME).getUrl();
		String expectedJdbcUrl = getExpectedJdbcUrl(DB2JdbcUrlCreator.DB2_SCHEME, INSTANCE_NAME);
		assertThat(expectedJdbcUrl).isEqualTo(jdbcUrl);

		CfJdbcService cfJdbcService = cfJdbcEnv.findJdbcServiceByName(SERVICE_NAME);
		UriInfo uriInfo = cfJdbcService.getCredentials().getUriInfo();
		assertUriInfo(uriInfo, DB2JdbcUrlCreator.DB2_SCHEME, INSTANCE_NAME);
	}

	protected String getDB2ServicePayloadWithJdbcurl(String serviceName, String hostname, int port,
													 String user, String password, String name, String scheme) {
		String payload = getRelationalPayload("test-db2-info-jdbc-url.json", serviceName,
				hostname, port, user, password, name);
		return payload.replace("$scheme", scheme);
	}

	protected String getExpectedJdbcUrl(String databaseType, String name) {
		return String.format("jdbc:%s://%s:%d/%s:user=%s;password=%s;",
				DB2JdbcUrlCreator.DB2_SCHEME, hostname, port, name, UriInfo.urlEncode(username), UriInfo.urlEncode(password));
	}

	protected String getExpectedJdbcUrl(String scheme, String name, String uname, String pwd) {
		return String.format("jdbc:%s://%s:%d/%s:user=%s;password=%s;",
				DB2JdbcUrlCreator.DB2_SCHEME, hostname, port, name, UriInfo.urlEncode(uname), UriInfo.urlEncode(pwd));
	}
}
