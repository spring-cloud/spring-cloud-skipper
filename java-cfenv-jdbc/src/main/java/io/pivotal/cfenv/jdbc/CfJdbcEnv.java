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

import io.pivotal.cfenv.core.CfEnv;

/**
 * Subclass that adds methods specifc for easy access to JDBC related credentails.
 *
 * @author Mark Pollack
 */
public class CfJdbcEnv extends CfEnv {

	public CfJdbcEnv() {
		super();
	}

	public List<CfJdbcService> findJdbcServices() {
		CfJdbcUrlCreator cfJdbcUrlCreator = new CfJdbcUrlCreator(this.findAllServices());
		return cfJdbcUrlCreator.findJdbcServices();
	}

	public CfJdbcService findJdbcServiceByName(String... spec) {
		CfJdbcUrlCreator cfJdbcUrlCreator = new CfJdbcUrlCreator(
				this.findServicesByName(spec));
		return cfJdbcUrlCreator.findJdbcServiceByName(spec);
	}

	public CfJdbcService findJdbcService() {
		CfJdbcUrlCreator cfJdbcUrlCreator = new CfJdbcUrlCreator(this.findAllServices());
		return cfJdbcUrlCreator.findJdbcService();
	}

}
