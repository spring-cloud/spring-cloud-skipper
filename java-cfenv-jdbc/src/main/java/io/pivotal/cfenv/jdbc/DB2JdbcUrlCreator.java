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
public class DB2JdbcUrlCreator extends AbstractJdbcUrlCreator  {

	public static final String DB2_SCHEME = "db2";

	public static final String[] DB2_TAGS = new String[]{"sqldb", "dashDB", "db2"};

	public static final String DB2_LABEL = "db2";

	@Override
	public boolean isDatabaseService(CfService cfService) {
		if (jdbcUrlMatchesScheme(cfService, DB2_SCHEME)
				|| cfService.existsByTagIgnoreCase(DB2_TAGS)
				|| cfService.existsByLabelStartsWith(DB2_LABEL)
				|| cfService.existsByUriSchemeStartsWith(DB2_SCHEME)
				|| cfService.existsByCredentialsContainsUriField(DB2_SCHEME)) {
			return true;
		}
		return false;
	}

	@Override
	public String buildJdbcUrlFromUriField(CfCredentials cfCredentials) {
		UriInfo uriInfo = cfCredentials.getUriInfo(DB2_SCHEME);
		return String.format("jdbc:%s://%s:%d/%s:user=%s;password=%s;",
				DB2_SCHEME, uriInfo.getHost(), uriInfo.getPort(), uriInfo.getPath(),
				UriInfo.urlEncode(uriInfo.getUsername()), UriInfo.urlEncode(uriInfo.getPassword()));
	}


	@Override
	public String getDriverClassName() {
		return "com.ibm.db2.jcc.DB2Driver";
	}
}
