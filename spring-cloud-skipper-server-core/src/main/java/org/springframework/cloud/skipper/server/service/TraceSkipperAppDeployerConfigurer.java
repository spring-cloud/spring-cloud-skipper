/*
 * Copyright 2017-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.skipper.server.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.skipper.domain.Deployer;
import org.springframework.cloud.sleuth.instrument.deployer.TraceAppDeployer;
import org.springframework.core.env.Environment;

/**
 * A {@link DeployerConfigurer} that wraps {@link AppDeployer} in its
 * trace representation.
 *
 * @author Marcin Grzejszczak
 * @since 2.7.0
 */
public class TraceSkipperAppDeployerConfigurer implements DeployerConfigurer {

	private static final Log log = LogFactory.getLog(TraceSkipperAppDeployerConfigurer.class);

	private final BeanFactory beanFactory;

	private final Environment environment;

	public TraceSkipperAppDeployerConfigurer(BeanFactory beanFactory, Environment environment) {
		this.beanFactory = beanFactory;
		this.environment = environment;
	}

	@Override
	public Deployer configure(Deployer deployer) {
		AppDeployer appDeployer = deployer.getAppDeployer();
		if (!(appDeployer instanceof TraceAppDeployer)) {
			TraceAppDeployer traceAppDeployer = new TraceAppDeployer(appDeployer, beanFactory, environment);
			deployer.setAppDeployer(traceAppDeployer);
			log.info("Wrapped the current deployer [" + appDeployer + "] in its trace representation");
		}
		return deployer;
	}
}
