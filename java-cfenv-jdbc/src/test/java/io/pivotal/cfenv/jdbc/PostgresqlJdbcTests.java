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

import java.util.List;

import io.pivotal.cfenv.core.UriInfo;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Mark Pollack
 */
public class PostgresqlJdbcTests extends AbstractJdbcTests {

	@Test
	public void postgresqlServiceCreation() {
		String name1 = "database-1";
		String name2 = "database-2";


		mockVcapServices(getServicesPayload(
				getPostgresqlServicePayload("postgresql-1", hostname, port, username, password, name1),
				getPostgresqlServicePayload("postgresql-2", hostname, port, username, password, name2)));

		assertJdbcServiceValues(name1, name2);

		CfJdbcEnv cfJdbcEnv = new CfJdbcEnv();
		CfJdbcService cfJdbcService = cfJdbcEnv.findJdbcServiceByName("postgresql-1");
		assertThat(cfJdbcService.getUsername()).isEqualTo(username);
		assertThat(cfJdbcService.getPassword()).isEqualTo(password);
		assertThat(cfJdbcService.getDriverClassName())
				.isEqualTo("org.postgresql.Driver");

		assertThatThrownBy(() -> {
			cfJdbcEnv.findJdbcServiceByName("postgresql.*");
		}).isInstanceOf(IllegalArgumentException.class).hasMessage(
				"No unique database service matching by name [postgresql.*] was found.  Matching service names are [postgresql-1, postgresql-2]");
	}


	@Test
	public void postgresqlWithSpecialCharsServiceCreation() {
		String userWithSpecialChars = "u%u:u+";
		String passwordWithSpecialChars = "p%p:p+";
		String name1 = "database-1";

		mockVcapServices(getServicesPayload(
				getPostgresqlServicePayload("postgresql-1", hostname,
						port, userWithSpecialChars, passwordWithSpecialChars, name1)));

		CfJdbcEnv cfJdbcEnv = new CfJdbcEnv();
		CfJdbcService cfJdbcService = cfJdbcEnv.findJdbcServiceByName("postgresql-1");

		String jdbcUrl = cfJdbcEnv.findJdbcServiceByName("postgresql-1").getUrl();
		String expectedJdbcUrl = getExpectedJdbcUrl(name1, userWithSpecialChars, passwordWithSpecialChars);
		assertThat(expectedJdbcUrl).isEqualTo(jdbcUrl);

		UriInfo uriInfo = cfJdbcService.getCredentials().getUriInfo();
		assertUriInfo(uriInfo, PostgresqlJdbcUrlCreator.POSTGRESQL_SCHEME, name1, userWithSpecialChars,
				passwordWithSpecialChars);
	}

	@Test
	public void postgresqlServiceCreationNoLabelNoTags() {
		String name1 = "database-1";
		String name2 = "database-2";

		mockVcapServices(getServicesPayload(
				getPostgresqlServicePayloadNoLabelNoTags("postgresql-1", hostname, port, username, password, name1),
				getPostgresqlServicePayloadNoLabelNoTags("postgresql-2", hostname, port, username, password, name2)));


		CfJdbcEnv cfJdbcEnv = new CfJdbcEnv();
		assertJdbcUrlAndUriInfo(name1, name2, cfJdbcEnv);
	}

	private void assertJdbcUrlAndUriInfo(String name1, String name2, CfJdbcEnv cfJdbcEnv) {
		String jdbcUrl = cfJdbcEnv.findJdbcServiceByName("postgresql-1").getUrl();
		String expectedJdbcUrl = getExpectedJdbcUrl(PostgresqlJdbcUrlCreator.POSTGRESQL_SCHEME, name1);
		assertThat(expectedJdbcUrl).isEqualTo(jdbcUrl);

		UriInfo uriInfo = cfJdbcEnv.findJdbcServiceByName("postgresql-1").getCredentials().getUriInfo();
		assertUriInfo(uriInfo, PostgresqlJdbcUrlCreator.POSTGRESQL_SCHEME, name1, username,
				password);

		jdbcUrl = cfJdbcEnv.findJdbcServiceByName("postgresql-2").getUrl();
		expectedJdbcUrl = getExpectedJdbcUrl(PostgresqlJdbcUrlCreator.POSTGRESQL_SCHEME, name2);
		assertThat(expectedJdbcUrl).isEqualTo(jdbcUrl);


		uriInfo = cfJdbcEnv.findJdbcServiceByName("postgresql-2").getCredentials().getUriInfo();
		assertUriInfo(uriInfo, PostgresqlJdbcUrlCreator.POSTGRESQL_SCHEME, name2, username,
				password);
	}

	@Test
	public void postgresqlServiceCreationWithJdbcUrl() {

		String name1 = "database-1";
		String name2 = "database-2";
		mockVcapServices(getServicesPayload(
				getPostgresqlServicePayloadWithJdbcUrl("postgresql-1", hostname, port, username, password, name1),
				getPostgresqlServicePayloadWithJdbcUrl("postgresql-2", hostname, port, username, password, name2)
		));

		CfJdbcEnv cfJdbcEnv = new CfJdbcEnv();
		assertJdbcUrlAndUriInfo(name1, name2, cfJdbcEnv);
	}


	private void assertJdbcServiceValues(String name1, String name2) {
		CfJdbcEnv cfJdbcEnv = new CfJdbcEnv();
		String jdbcUrl1 = cfJdbcEnv.findJdbcServiceByName("postgresql-1").getUrl();
		String jdbcUrl2 = cfJdbcEnv.findJdbcServiceByName("postgresql-2").getUrl();

		assertThat(getExpectedJdbcUrl(PostgresqlJdbcUrlCreator.POSTGRESQL_SCHEME, name1))
				.isEqualTo(jdbcUrl1);
		assertThat(getExpectedJdbcUrl(PostgresqlJdbcUrlCreator.POSTGRESQL_SCHEME, name2))
				.isEqualTo(jdbcUrl2);

		CfJdbcService cfJdbcService1 = cfJdbcEnv.findJdbcServiceByName("postgresql-1");
		CfJdbcService cfJdbcService2 = cfJdbcEnv.findJdbcServiceByName("postgresql-2");
		jdbcUrl1 = cfJdbcService1.getUrl();
		jdbcUrl2 = cfJdbcService2.getUrl();
		assertThat(getExpectedJdbcUrl(PostgresqlJdbcUrlCreator.POSTGRESQL_SCHEME, name1))
				.isEqualTo(jdbcUrl1);
		assertThat(getExpectedJdbcUrl(PostgresqlJdbcUrlCreator.POSTGRESQL_SCHEME, name2))
				.isEqualTo(jdbcUrl2);

		List<CfJdbcService> cfJdbcServices = cfJdbcEnv.findJdbcServices();
		assertThat(cfJdbcServices.size()).isEqualTo(2);

		assertThatThrownBy(() -> {
			cfJdbcEnv.findJdbcService().getUrl();
		}).isInstanceOf(IllegalArgumentException.class).hasMessage(
				"No unique database service found. Found database service names [postgresql-1, postgresql-2]");

		CfJdbcService cfJdbcService = cfJdbcEnv.findJdbcServiceByName("postgresql-1");
		assertThat(cfJdbcService.getDriverClassName())
				.isEqualTo("org.postgresql.Driver");

	}

	private String getPostgresqlServicePayload(String serviceName,
											   String hostname, int port,
											   String user, String password, String name) {
		return getRelationalPayload("test-postgresql-info.json", serviceName,
				hostname, port, user, password, name);
	}

	private String getPostgresqlServicePayloadNoLabelNoTags(String serviceName,
															String hostname, int port,
															String user, String password, String name) {
		return getRelationalPayload("test-postgresql-info-no-label-no-tags.json", serviceName,
				hostname, port, user, password, name);
	}

	private String getPostgresqlServicePayloadWithJdbcUrl(String serviceName,
														  String hostname, int port,
														  String user, String password, String name) {
		return getRelationalPayload("test-postgresql-info-jdbc-url.json", serviceName,
				hostname, port, user, password, name);
	}


	@Override
	protected String getExpectedJdbcUrl(String databaseType, String name) {
		return String.format("jdbc:postgresql://%s:%d/%s?user=%s&password=%s", hostname,
				port, name, UriInfo.urlEncode(username), UriInfo.urlEncode(password));
	}

	protected String getExpectedJdbcUrl(String name, String uname, String pwd) {
		return String.format("jdbc:postgresql://%s:%d/%s?user=%s&password=%s", hostname,
				port, name, UriInfo.urlEncode(uname), UriInfo.urlEncode(pwd));
	}
}
