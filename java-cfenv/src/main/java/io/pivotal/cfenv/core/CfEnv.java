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
package io.pivotal.cfenv.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Provides access to Cloud Foundry environment variables.
 *
 * @author Mark Pollack
 */
public class CfEnv {

	public static final String VCAP_APPLICATION = "VCAP_APPLICATION";

	public static final String VCAP_SERVICES = "VCAP_SERVICES";

	/* TODO pick small json parser and package as a shadowed jar */
	private ObjectMapper objectMapper = new ObjectMapper();

	private List<CfService> cfServices = new ArrayList<>();

	private CfApplication cfApplication;

	public CfEnv() {
		try {
			String vcapServicesJson = System.getenv(VCAP_SERVICES);
			if (vcapServicesJson != null && vcapServicesJson.length() > 0) {
				Map<String, List<Map<String, Object>>> rawServices = this.objectMapper
						.readValue(vcapServicesJson,
								new TypeReference<Map<String, List<Map<String, Object>>>>() {
								}

						);
				for (Map.Entry<String, List<Map<String, Object>>> entry : rawServices
						.entrySet()) {
					for (Map<String, Object> serviceData : entry.getValue()) {
						cfServices.add(new CfService(serviceData));
					}
				}
			}
		}
		catch (Exception e) {
			throw new IllegalStateException(
					"Could not access/parse " + VCAP_SERVICES + " environment variable.",
					e);
		}
		try {
			String vcapApplicationJson = System.getenv(VCAP_APPLICATION);
			if (vcapApplicationJson != null && vcapApplicationJson.length() > 0) {
				Map<String, Object> applicationData = objectMapper
						.readValue(vcapApplicationJson, Map.class);
				this.cfApplication = new CfApplication(applicationData);
			}
		}
		catch (Exception e) {
			// throw new IllegalStateException("Could not access/parse " +
			// VCAP_APPLICATION + "
			// environment variable.", e);
		}
	}

	public CfApplication getApp() {
		return this.cfApplication;
	}

	public List<CfService> findAllServices() {
		return cfServices;
	}

	public List<CfService> findServicesByName(String... spec) {
		List<CfService> cfServices = new ArrayList<>();
		for (CfService cfService : this.cfServices) {
			if (spec != null) {
				for (String regex : spec) {
					String name = cfService.getName();
					if (name != null && name.length() > 0) {
						if (name.matches(regex)) {
							cfServices.add(cfService);
						}
					}
				}
			}
		}
		return cfServices;
	}

	public CfService findServiceByName(String... spec) {
		List<CfService> cfServices = findServicesByName(spec);
		if (cfServices.size() == 1) {
			return cfServices.stream().findFirst().get();
		}
		String specMessage = (spec == null) ? "null" : String.join(", ", spec);
		throwExceptionIfMultipleMatches(cfServices, specMessage, "name");
		throw new IllegalArgumentException(
				"No service with name [" + specMessage + "] was found.");
	}

	private void throwExceptionIfMultipleMatches(List<CfService> cfServices,
			String specMessage, String operation) {
		if (cfServices.size() > 1) {
			String[] names = cfServices.stream().map(CfService::getName)
					.toArray(String[]::new);
			throw new IllegalArgumentException(
					"No unique service matching by " + operation + " [" + specMessage
							+ "] was found.  Matching service names are ["
							+ String.join(", ", names) + "]");
		}
	}

	public CfService findServiceByLabel(String... spec) {
		List<CfService> cfServices = new ArrayList<>();
		for (CfService cfService : this.cfServices) {
			if (spec != null) {
				for (String regex : spec) {
					String name = cfService.getLabel();
					if (name != null && name.length() > 0) {
						if (name.matches(regex)) {
							cfServices.add(cfService);
						}
					}
				}
			}
		}
		if (cfServices.size() == 1) {
			return cfServices.stream().findFirst().get();
		}
		String message = (spec == null) ? "null" : String.join(", ", spec);
		throwExceptionIfMultipleMatches(cfServices, message, "label");
		throw new IllegalArgumentException(
				"No service with label [" + message + "] was found.");
	}

	public CfService findServiceByTag(String... spec) {
		List<CfService> cfServices = new ArrayList<>();
		for (CfService cfService : this.cfServices) {
			if (spec != null) {
				for (String regex : spec) {
					List<String> tags = cfService.getTags();
					for (String tag : tags) {
						if (tag != null && tag.length() > 0) {
							if (tag.matches(regex)) {
								cfServices.add(cfService);
							}
						}
					}
				}
			}
		}
		if (cfServices.size() == 1) {
			return cfServices.stream().findFirst().get();
		}
		String message = (spec == null) ? "null" : String.join(", ", spec);
		throwExceptionIfMultipleMatches(cfServices, message, "tag");
		throw new IllegalArgumentException(
				"No service with tag [" + message + "] was found.");
	}

	public CfCredentials findCredentialsByName(String... spec) {
		CfService cfService = findServiceByName(spec);
		return cfService.getCredentials();
	}

	public CfCredentials findCredentialsByLabel(String... spec) {
		CfService cfService = findServiceByLabel(spec);
		return cfService.getCredentials();
	}

	public CfCredentials findCredentialsByTag(String... spec) {
		CfService cfService = findServiceByTag(spec);
		return cfService.getCredentials();
	}

	/**
	 * Checks that the value of the environment variable VCAP_APPLICATION is not null, usually
	 * indicating that this application is running inside of Cloud Foundry
	 * @return {@code true} if the environment variable VCAP_APPLICATION is not null,
	 * {@code false} otherwise.
	 */
	public boolean isInCf() {
		return System.getenv(VCAP_APPLICATION) != null;
	}

}
