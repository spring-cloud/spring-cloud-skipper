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

import io.pivotal.cfenv.core.CfService;

/**
 * Strategy interface for creating JDBC URL for various types of database services.
 *
 * @author Mark Pollack
 */
public interface JdbcUrlCreator {

	/**
	 * Identifies the provided service as a database service
	 * @param cfService a Cloud Foundry service
	 * @return {@code true} if the service describes a database service, {@code false}
	 * otherwise.
	 */
	boolean isDatabaseService(CfService cfService);

	String createJdbcUrl(CfService cfService);

	String getDriverClassName();

}
