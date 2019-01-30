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
package io.pivotal.cfenv.spring.boot;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

import mockit.Mock;
import mockit.MockUp;
import org.junit.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.ResourceUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Pollack
 */
public class DataSourceTests {

	private static final String mysqlJdbcUrl = "jdbc:mysql://10.0.4.35:3306/cf_2e23d10a_8738_8c3c_66cf_13e44422698c?user=mysql_username&password=mysql_password";

	private final CfDataSourceEnvironmentPostProcessor environmentPostProcessor = new CfDataSourceEnvironmentPostProcessor();

	private final ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();

	@Test
	public void testDataSource() throws Exception {

		// To make CloudPlatform test pass
		System.setProperty("VCAP_APPLICATION", "yes");

		// To setup values used by CfEnv
		File file = ResourceUtils.getFile("classpath:vcap-services.json");
		String fileContents = new String(Files.readAllBytes(file.toPath()));
		Map<String, String> env = System.getenv();
		new MockUp<System>() {
			@Mock
			public String getenv(String name) {
				if (name.equalsIgnoreCase("VCAP_SERVICES")) {
					return fileContents;
				}
				return env.get(name);
			}
		};

		environmentPostProcessor.postProcessEnvironment(this.context.getEnvironment(),
				null);
		assertThat(this.context.getEnvironment().getProperty("spring.datasource.url"))
				.isEqualTo(mysqlJdbcUrl);
		assertThat(
				this.context.getEnvironment().getProperty("spring.datasource.username"))
						.isEqualTo("mysql_username");
		assertThat(
				this.context.getEnvironment().getProperty("spring.datasource.password"))
						.isEqualTo("mysql_password");

	}

}
