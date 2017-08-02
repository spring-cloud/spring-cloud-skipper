/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.skipper.client;

import java.io.File;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * TODO: This needs to be reconciled with the @Value properties used in ConfigCommands
 *
 * @author Mark Pollack
 */
@ConfigurationProperties("skipper.client")
public class SkipperClientConfigurationProperties {

	private String home = System.getProperty("user.home") + File.separator + ".skipper";

	private String host = "http://localhost:9494";

	public String getHome() {
		return home;
	}

	public void setHome(String home) {
		this.home = home;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}
}
