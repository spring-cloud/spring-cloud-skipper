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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains the Map of values and type-safe accessors for properties of a single service
 * defined in VCAP_SERVICES.
 *
 * @author Mark Pollack
 */
public class CfService {

	private static final String TAGS = "tags";

	private static final String CREDENTIALS = "credentials";

	private final Map<String, Object> serviceData;

	private final CfCredentials cfCredentials;

	public CfService(Map<String, Object> serviceData) {
		this.serviceData = serviceData;
		this.cfCredentials = createCredentials();
	}

	public CfCredentials createCredentials() {
		Map<String, Object> credentials = new HashMap<>();
		if (this.serviceData.containsKey(CREDENTIALS)) {
			credentials = (Map<String, Object>) this.serviceData.get(CREDENTIALS);
		}
		return new CfCredentials(credentials);
	}

	public Map<String, Object> getMap() {
		return this.serviceData;
	}

	public CfCredentials getCredentials() {
		return this.cfCredentials;
	}

	public List<String> getTags() {
		List<String> tags = new ArrayList<>();
		if (this.serviceData.containsKey(TAGS)) {
			tags = (List<String>) this.serviceData.get(TAGS);
		}
		return tags;
	}

	public String getLabel() {
		return this.getString("label");
	}

	public String getPlan() {
		return this.getString("plan");
	}

	public String getName() {
		return this.getString("name");
	}

	public String getString(String... keys) {
		if (this.serviceData != null) {
			for (String key : keys) {
				if (this.serviceData.containsKey(key)) {
					return this.serviceData.get(key).toString();
				}
			}
		}
		return null;
	}

	// Query methods

	public boolean existsByTagIgnoreCase(String... tags) {
		if (tags != null) {
			for (String serviceTag : getTags()) {
				for (String tagToMatch : tags) {
					if (tagToMatch != null && tagToMatch.length() > 0) {
						if (tagToMatch.equalsIgnoreCase(serviceTag)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	public boolean existsByUriSchemeStartsWith(String... uriSchemes) {
		if (uriSchemes != null) {
			CfCredentials credentials = this.getCredentials();
			String uri = credentials.getUri();
			if (uri != null) {
				for (String uriScheme : uriSchemes) {
					if (uri.startsWith(uriScheme + "://")) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * There are common variations for the name of the uri field that prepend the schema
	 * portion of the URI, e.g. if the uri schema is 'mysql' the field name in the JSON
	 * maybe 'mysqlUri', 'mysqluri', 'mysqlUrl' or 'mysqlurl'. This method searches for
	 * the presence of these alternatively named fields in the credentials of the service
	 * @param uriSchemes
	 * @return
	 */
	public boolean existsByCredentialsContainsUriField(String... uriSchemes) {
		if (uriSchemes != null) {
			CfCredentials cfCredentials = this.getCredentials();
			for (String uriScheme : uriSchemes) {
				if (cfCredentials.getMap().containsKey(uriScheme + "Uri")
						|| cfCredentials.getMap().containsKey(uriScheme + "uri")
						|| cfCredentials.getMap().containsKey(uriScheme + "Url")
						|| cfCredentials.getMap().containsKey(uriScheme + "url")) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean existsByLabelStartsWith(String label) {
		String cfLabel = this.getLabel();
		if (cfLabel != null && cfLabel.length() > 0) {
			return cfLabel.startsWith(label);
		}
		return false;
	}

}
