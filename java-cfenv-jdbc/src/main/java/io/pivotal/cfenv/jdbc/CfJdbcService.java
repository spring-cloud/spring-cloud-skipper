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

import java.util.Map;

import io.pivotal.cfenv.core.CfService;

/**
 * @author Mark Pollack
 */
public class CfJdbcService extends CfService {

	public CfJdbcService(Map<String, Object> serviceData) {
		super(serviceData);
	}

	public String getUrl() {
		return getCredentials().getDerivedCredentials().get("jdbcUrl");
	}

	public String getUsername() {
		return getCredentials().getUsername();
	}

	public String getPassword() {
		return getCredentials().getPassword();
	}

	public Object getDriverClassName() {
		return getCredentials().getDerivedCredentials().get("driver-class-name");
	}

}
