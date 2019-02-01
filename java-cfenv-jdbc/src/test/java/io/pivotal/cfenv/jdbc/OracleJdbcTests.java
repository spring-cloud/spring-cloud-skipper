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
public class OracleJdbcTests extends AbstractJdbcTests {


	private static final String INSTANCE_NAME = "database";
	private static final String SERVICE_NAME = "oracle-ups";

	@Test
	public void oracleServiceCreation() {
		mockVcapServices(getServicesPayload(
				getUserProvidedServicePayload(SERVICE_NAME, hostname, port, username, password, INSTANCE_NAME, OracleJdbcUrlCreator.ORACLE_SCHEME + ":")
		));

		CfJdbcEnv cfJdbcEnv = new CfJdbcEnv();
		CfJdbcService cfJdbcService = cfJdbcEnv.findJdbcServiceByName(SERVICE_NAME);
		assertThat(cfJdbcService.getUsername()).isEqualTo(username);
		assertThat(cfJdbcService.getPassword()).isEqualTo(password);
		assertThat(cfJdbcService.getDriverClassName()).isEqualTo("oracle.jdbc.OracleDriver");

		String jdbcUrl = cfJdbcEnv.findJdbcServiceByName(SERVICE_NAME).getUrl();
		String expectedJdbcUrl = getExpectedJdbcUrl(OracleJdbcUrlCreator.ORACLE_SCHEME, INSTANCE_NAME);
		assertThat(expectedJdbcUrl).isEqualTo(jdbcUrl);
		UriInfo uriInfo = cfJdbcService.getCredentials().getUriInfo();
		assertUriInfo(uriInfo, OracleJdbcUrlCreator.ORACLE_SCHEME, INSTANCE_NAME);
	}

	@Test
	public void oracleServiceCreationWithNoUri() {
		mockVcapServices(getServicesPayload(
				getUserProvidedServicePayloadWithNoUri(SERVICE_NAME, hostname, port, username, password, INSTANCE_NAME)
		));

		CfJdbcEnv cfJdbcEnv = new CfJdbcEnv();
		// Connector library returns a ServiceInfo via CloudFoundryFallbackServiceInfoCreator of type BaseServiceInfo.
		// In this case we would need to fall back to using the standard CfEnv APIs to access the information.
		assertThatThrownBy(() -> {
			cfJdbcEnv.findJdbcServiceByName(SERVICE_NAME);
		}).isInstanceOf(IllegalArgumentException.class).hasMessage(
				"No database service with name [oracle-ups] was found.");

		CfCredentials cfCredentials = cfJdbcEnv.findCredentialsByName(SERVICE_NAME);
		assertThat(cfCredentials.getHost()).isEqualTo(hostname);

	}

	@Test
	public void oracleServiceCreationWithJdbcUrl() {
		mockVcapServices(getServicesPayload(
				getOracleServicePayloadWithJdbcurl(SERVICE_NAME, hostname, port, username, password, INSTANCE_NAME, OracleJdbcUrlCreator.ORACLE_SCHEME + ":")
		));

		CfJdbcEnv cfJdbcEnv = new CfJdbcEnv();
		String jdbcUrl = cfJdbcEnv.findJdbcServiceByName(SERVICE_NAME).getUrl();
		String expectedJdbcUrl = getExpectedJdbcUrl(OracleJdbcUrlCreator.ORACLE_SCHEME, INSTANCE_NAME);
		assertThat(expectedJdbcUrl).isEqualTo(jdbcUrl);

		CfJdbcService cfJdbcService = cfJdbcEnv.findJdbcServiceByName(SERVICE_NAME);
		UriInfo uriInfo = cfJdbcService.getCredentials().getUriInfo();
		assertUriInfo(uriInfo, OracleJdbcUrlCreator.ORACLE_SCHEME, INSTANCE_NAME);
	}


	protected String getExpectedJdbcUrl(String scheme, String name) {
		return String.format("%s%s:thin:%s/%s@%s:%d/%s", AbstractJdbcUrlCreator.JDBC_PREFIX, scheme, username, password, hostname, port, name);
	}

	protected String getOracleServicePayloadWithJdbcurl(String serviceName, String hostname, int port,
														String user, String password, String name, String scheme) {
		String payload = getRelationalPayload("test-oracle-info-jdbc-url.json", serviceName,
				hostname, port, user, password, name);
		return payload.replace("$scheme", scheme);
	}
}
