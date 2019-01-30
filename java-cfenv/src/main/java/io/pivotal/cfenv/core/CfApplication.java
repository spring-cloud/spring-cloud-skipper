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

import java.util.List;
import java.util.Map;

/**
 * @author Mark Pollack
 */
public class CfApplication {

	private final Map<String, Object> applicationData;

	public CfApplication(Map<String, Object> applicationData) {
		this.applicationData = applicationData;
	}

	public Map<String, Object> getMap() {
		return this.applicationData;
	}

	public String getInstanceId() {
		return getString("instance_id");
	}

	public int getInstanceIndex() {
		return getInt("instance_index");
	}

	public String getHost() {
		return getString("host");
	}

	public int getPort() {
		return getInt("port");
	}

	public String getApplicationVersion() {
		return getString("application_version");
	}

	public String getApplicationName() {
		return getString("application_name");
	}

	public List<String> getApplicationUris() {
		return getStringList("application_uris");
	}

	public String getVersion() {
		return getString("version");
	}

	public String getName() {
		return getString("name");
	}

	public List<String> getUris() {
		return getStringList("uris");
	}

	private String getString(String key) {
		if (applicationData != null && applicationData.containsKey(key)) {
			return applicationData.get(key).toString();
		}
		return null;
	}

	private List<String> getStringList(String key) {
		if (applicationData != null && applicationData.containsKey(key)) {
			Object value = applicationData.get(key);
			if (value instanceof List) {
				return (List<String>) value;
			}
		}
		return null;
	}

	private int getInt(String key) {
		if (applicationData != null && applicationData.containsKey(key)) {
			Object intValue = applicationData.get(key);
			if (intValue instanceof Integer) {
				return ((Integer) intValue).intValue();
			}
		}
		return -1;
	}

}
