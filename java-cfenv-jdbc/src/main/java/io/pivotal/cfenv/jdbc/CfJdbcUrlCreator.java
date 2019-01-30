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

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import io.pivotal.cfenv.core.CfService;

/**
 * @author Mark Pollack
 */
public class CfJdbcUrlCreator {

	private List<CfJdbcService> cfJdbcServices;

	public CfJdbcUrlCreator(List<CfService> cfServices) {
		List<JdbcUrlCreator> jdbcUrlCreators = new ArrayList<>();
		Iterable<JdbcUrlCreator> jdbcUrlCreatorIterable = ServiceLoader
				.load(JdbcUrlCreator.class);
		for (JdbcUrlCreator jdbcUrlCreator : jdbcUrlCreatorIterable) {
			if (jdbcUrlCreator != null) {
				jdbcUrlCreators.add(jdbcUrlCreator);
			}
		}
		this.cfJdbcServices = new ArrayList<>();
		for (CfService cfService : cfServices) {
			for (JdbcUrlCreator jdbcUrlCreator : jdbcUrlCreators) {
				if (jdbcUrlCreator.isDatabaseService(cfService)) {
					CfJdbcService cfJdbcService = new CfJdbcService(cfService.getMap());
					String jdbcUrl = jdbcUrlCreator.createJdbcUrl(cfService);
					cfJdbcService.getCredentials().getDerivedCredentials().put(
							"driver-class-name", jdbcUrlCreator.getDriverClassName());
					cfJdbcService.getCredentials().getDerivedCredentials().put("jdbcUrl",
							jdbcUrl);
					this.cfJdbcServices.add(cfJdbcService);
				}
			}
		}
	}

	public List<CfJdbcService> findJdbcServices() {
		return this.cfJdbcServices;
	}

	public CfJdbcService findJdbcServiceByName(String... spec) {
		List<CfJdbcService> matchingJdbcServices = new ArrayList<>();
		for (CfJdbcService cfJdbcService : this.cfJdbcServices) {
			if (spec != null) {
				for (String regex : spec) {
					String name = cfJdbcService.getName();
					if (name != null && name.length() > 0) {
						if (name.matches(regex)) {
							matchingJdbcServices.add(cfJdbcService);
						}
					}
				}
			}
		}
		if (matchingJdbcServices.size() == 1) {
			return matchingJdbcServices.stream().findFirst().get();
		}
		String specMessage = (spec == null) ? "null" : String.join(", ", spec);
		throwExceptionIfMultipleMatchesByName(matchingJdbcServices, specMessage, "name");
		throw new IllegalArgumentException(
				"No service with name [" + specMessage + "] was found.");
	}

	public CfJdbcService findJdbcService() {
		if (this.cfJdbcServices.size() == 1) {
			return this.cfJdbcServices.stream().findFirst().get();
		}
		throwExceptionIfMultipleMatches(this.cfJdbcServices);
		return null;
	}

	private void throwExceptionIfMultipleMatches(List<CfJdbcService> services) {
		if (services.size() > 1) {
			String[] names = services.stream().map(CfJdbcService::getName)
					.toArray(String[]::new);
			throw new IllegalArgumentException(
					"No unique database service found. Found database service names ["
							+ String.join(", ", names) + "]");
		}
	}

	private void throwExceptionIfMultipleMatchesByName(List<CfJdbcService> services,
			String specMessage, String operation) {
		if (services.size() > 1) {
			String[] names = services.stream().map(CfJdbcService::getName)
					.toArray(String[]::new);
			throw new IllegalArgumentException(
					"No unique database service matching by " + operation + " ["
							+ specMessage + "] was found.  Matching service names are ["
							+ String.join(", ", names) + "]");
		}
	}

}
