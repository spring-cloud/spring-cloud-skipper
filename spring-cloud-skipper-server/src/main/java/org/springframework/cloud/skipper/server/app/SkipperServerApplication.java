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
package org.springframework.cloud.skipper.server.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.session.SessionAutoConfiguration;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeployerAutoConfiguration;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesAutoConfiguration;
import org.springframework.cloud.deployer.spi.local.LocalDeployerAutoConfiguration;
import org.springframework.cloud.skipper.deployer.CloudFoundryPlatformAutoConfiguration;
import org.springframework.cloud.skipper.deployer.KubernetesPlatformAutoConfiguration;
import org.springframework.cloud.skipper.server.EnableSkipperServer;
import org.springframework.context.annotation.Import;

/**
 * Runs the Skipper Server Application.
 *
 * @author Gunnar Hillert
 *
 */
@SpringBootApplication(exclude = {
		CloudFoundryDeployerAutoConfiguration.class,
		KubernetesAutoConfiguration.class,
		LocalDeployerAutoConfiguration.class,
		ManagementWebSecurityAutoConfiguration.class,
		SecurityAutoConfiguration.class,
		SessionAutoConfiguration.class
	})
@EnableSkipperServer
@Import({KubernetesPlatformAutoConfiguration.class, CloudFoundryPlatformAutoConfiguration.class})
public class SkipperServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SkipperServerApplication.class, args);
	}
}
