/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.cloud.skipper.server.config;

import javax.sql.DataSource;

import org.springframework.cloud.config.java.AbstractCloudConfig;
import org.springframework.cloud.service.PooledServiceConnectorConfig;
import org.springframework.cloud.service.relational.DataSourceConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Add declarative configuration of the max pool side and max wait time when using the connector library.
 *
 * @author Mark Pollack
 */
@Profile("cloud")
@Configuration
public class DataSourceCloudConfig extends AbstractCloudConfig {

	@Bean
	public DataSource dataSource(SkipperServerProperties skipperServerProperties) {
		PooledServiceConnectorConfig.PoolConfig poolConfig =
				new PooledServiceConnectorConfig.PoolConfig(skipperServerProperties.getCloudFoundry().getMaxPoolSize(),
						skipperServerProperties.getCloudFoundry().getMaxWaitTime());
		DataSourceConfig dbConfig = new DataSourceConfig(poolConfig, null);
		return connectionFactory().dataSource(dbConfig);
	}

}
