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
public class OracleJdbcUrlCreator extends AbstractJdbcUrlCreator {

	public static final String ORACLE_SCHEME = "oracle";

	public static final String ORACLE_LABEL = "oracle";


	@Override
	public boolean isDatabaseService(CfService cfService) {
		// Match tags
		if (jdbcUrlMatchesScheme(cfService, ORACLE_SCHEME)
				|| cfService.existsByLabelStartsWith(ORACLE_LABEL)
				|| cfService.existsByUriSchemeStartsWith(ORACLE_SCHEME)
				|| cfService.existsByCredentialsContainsUriField(ORACLE_SCHEME)) {
			return true;
		}
		return false;
	}

	@Override
	public String getDriverClassName() {
		return "oracle.jdbc.OracleDriver";
	}

	@Override
	public String buildJdbcUrlFromUriField(CfCredentials cfCredentials) {
		UriInfo uriInfo = cfCredentials.getUriInfo(ORACLE_SCHEME);
		return String.format("jdbc:%s:thin:%s/%s@%s:%d/%s", ORACLE_SCHEME,
				uriInfo.getUsername(), uriInfo.getPassword(),
				uriInfo.getHost(), uriInfo.getPort(), uriInfo.getPath());
	}


}
