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
import io.pivotal.cfenv.core.CfService;
import io.pivotal.cfenv.core.UriInfo;

/**
 * @author Mark Pollack
 */
public class PostgresqlJdbcUrlCreator extends AbstractJdbcUrlCreator {

	public static final String POSTGRESQL_SCHEME = "postgres";

	public static final String POSTGRES_JDBC_SCHEME = "postgresql";

	public static final String POSTGRESQL_TAG = "postgresql";

	public static final String POSTGRESQL_LABEL = "postgresql";

	@Override
	public boolean isDatabaseService(CfService cfService) {
		if (jdbcUrlMatchesScheme(cfService, POSTGRESQL_SCHEME, POSTGRES_JDBC_SCHEME)
				|| cfService.existsByTagIgnoreCase(POSTGRESQL_TAG)
				|| cfService.existsByLabelStartsWith(POSTGRESQL_LABEL)
				|| cfService.existsByUriSchemeStartsWith(POSTGRESQL_SCHEME)
				|| cfService.existsByCredentialsContainsUriField(POSTGRESQL_SCHEME)) {
			return true;
		}
		return false;
	}

	@Override
	public String buildJdbcUrlFromUriField(CfCredentials cfCredentials) {
		UriInfo uriInfo = cfCredentials.getUriInfo(POSTGRES_JDBC_SCHEME);
		return String.format("%s%s://%s%s/%s%s%s", JDBC_PREFIX, POSTGRES_JDBC_SCHEME,
				uriInfo.getHost(), uriInfo.formatPort(), uriInfo.getPath(),
				uriInfo.formatUserNameAndPassword(), uriInfo.formatQuery());
	}

	@Override
	public String getDriverClassName() {
		return "org.postgresql.Driver";
	}
}
