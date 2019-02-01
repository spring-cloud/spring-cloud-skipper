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

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.pivotal.cfenv.core.UriInfo;
import mockit.MockUp;
import org.junit.Before;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Pollack
 */
public class AbstractJdbcTests {

	protected static final String hostname = "10.20.30.40";

	protected static final int port = 1234;

	protected static final String password = "mypass";

	protected static final String username = "myuser";

	private static ObjectMapper objectMapper = new ObjectMapper();

	@SuppressWarnings("unchecked")
	private static String getServiceLabel(String servicePayload) {
		try {
			Map<String, Object> serviceMap = objectMapper.readValue(servicePayload,
					Map.class);
			return serviceMap.get("label").toString();
		}
		catch (Exception e) {
			return null;
		}
	}

	protected static String getServicesPayload(String... servicePayloads) {
		Map<String, List<String>> labelPayloadMap = new HashMap<String, List<String>>();

		for (String payload : servicePayloads) {
			String label = getServiceLabel(payload);

			List<String> payloadsForLabel = labelPayloadMap.get(label);
			if (payloadsForLabel == null) {
				payloadsForLabel = new ArrayList<String>();
				labelPayloadMap.put(label, payloadsForLabel);
			}
			payloadsForLabel.add(payload);
		}

		StringBuilder result = new StringBuilder("{\n");
		int labelSize = labelPayloadMap.size();
		int i = 0;

		for (Map.Entry<String, List<String>> entry : labelPayloadMap.entrySet()) {
			result.append(quote(entry.getKey())).append(":");
			result.append(getServicePayload(entry.getValue()));
			if (i++ != labelSize - 1) {
				result.append(",\n");
			}
		}
		result.append("}");

		return result.toString();

	}

	private static String getServicePayload(List<String> servicePayloads) {
		StringBuilder payload = new StringBuilder("[");

		// In Scala, this would have been servicePayloads mkString "," :-)
		for (int i = 0; i < servicePayloads.size(); i++) {
			payload.append(servicePayloads.get(i));
			if (i != servicePayloads.size() - 1) {
				payload.append(",");
			}
		}
		payload.append("]");

		return payload.toString();
	}

	private static String quote(String str) {
		return "\"" + str + "\"";
	}

	protected void mockVcapServices(String serviceJson) {
		Map<String, String> env = System.getenv();
		new MockUp<System>() {
			@mockit.Mock
			public String getenv(String name) {
				if (name.equalsIgnoreCase("VCAP_SERVICES")) {
					return serviceJson;
				}
				return env.get(name);
			}
		};

	}

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	protected String getExpectedJdbcUrl(String databaseType, String name) {
		String jdbcUrlDatabaseType = databaseType;
		if (databaseType.equals("postgres")) {
			jdbcUrlDatabaseType = "postgresql";
		}

		return "jdbc:" + jdbcUrlDatabaseType + "://" + hostname + ":" + port + "/" + name
				+ "?user=" + username + "&password=" + password;
	}

	protected String getRelationalPayload(String templateFile, String serviceName,
			String hostname, int port, String user, String password, String name) {
		String payload = readTestDataFile(templateFile);
		payload = payload.replace("$serviceName", serviceName);
		payload = payload.replace("$hostname", hostname);
		payload = payload.replace("$port", Integer.toString(port));
		payload = payload.replace("$user", UriInfo.urlEncode(user));
		payload = payload.replace("$password", UriInfo.urlEncode(password));
		payload = payload.replace("$name", name);

		return payload;
	}

	protected String readTestDataFile(String fileName) {
		Scanner scanner = null;
		try {
			Reader fileReader = new InputStreamReader(
					getClass().getResourceAsStream(fileName));
			scanner = new Scanner(fileReader);
			return scanner.useDelimiter("\\Z").next();
		}
		finally {
			if (scanner != null) {
				scanner.close();
			}
		}
	}

	protected String getUserProvidedServicePayload(String serviceName, String hostname, int port,
												   String user, String password, String name, String scheme) {
		String payload = getRelationalPayload("test-ups-info.json", serviceName,
				hostname, port, user, password, name);
		return payload.replace("$scheme", scheme);
	}

	protected String getUserProvidedServicePayloadWithNoUri(String serviceName, String hostname, int port,
															String user, String password, String name) {
		return getRelationalPayload("test-ups-info-no-uri.json", serviceName,
				hostname, port, user, password, name);
	}

	protected void assertUriInfo(UriInfo uriInfo, String scheme, String instanceName) {
		assertThat(uriInfo.getScheme()).isEqualTo(scheme);
		assertThat(uriInfo.getPath()).isEqualTo(instanceName);
		assertCommonUriFields(uriInfo);
	}

	protected void assertUriInfo(UriInfo uriInfo, String scheme, String instanceName, String uname, String pwd) {
		assertThat(uriInfo.getScheme()).isEqualTo(scheme);
		assertThat(uriInfo.getPath()).isEqualTo(instanceName);
		assertHostPort(uriInfo);
		assertThat(uriInfo.getUsername()).isEqualTo(uname);
		assertThat(uriInfo.getPassword()).isEqualTo(pwd);
	}

	private void assertCommonUriFields(UriInfo uriInfo) {
		assertHostPort(uriInfo);
		assertThat(uriInfo.getUsername()).isEqualTo(username);
		assertThat(uriInfo.getPassword()).isEqualTo(password);
	}

	private void assertHostPort(UriInfo uriInfo) {
		assertThat(uriInfo.getHost()).isEqualTo(hostname);
		assertThat(uriInfo.getPort()).isEqualTo(port);
	}


}
