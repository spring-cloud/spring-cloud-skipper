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
package org.springframework.cloud.skipper.server.domain;

/**
 * A kubernetes resource style representation of a Spring Boot application that will be
 * deployed using the Spring Cloud Deployer API.
 *
 * This class is commonly referred to as 'the manifest', meaning the complete list of the
 * application resource, properties, metadata and deployment properties. It is
 * serialized/deserialized from YAML. An example is: {@literal
 * apiVersion: skipperPackageMetadata/v1
 * kind: SpringCloudDeployerApplication
 * metadata:
 *   name: log-sink
 *   type: sink
 * spec:
 * resource: maven://org.springframework.cloud.stream.app:log-sink-rabbit:1.2.0.RELEASE
 * resourceMetadata: maven://org.springframework.cloud.stream.app:log-sink-rabbit:jar:metadata:1.2.0.RELEASE
 * applicationProperties:
 *  log.level: INFO
 *  log.expression: hellobaby
 * deploymentProperties:
 *   memory: 1024
 *   disk: 2
 * }
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
public class SpringCloudDeployerApplicationManifest extends ApplicationManifest {

	protected SpringCloudDeployerApplicationSpec spec;

	public SpringCloudDeployerApplicationManifest() {
	}

	public SpringCloudDeployerApplicationSpec getSpec() {
		return spec;
	}

	public void setSpec(SpringCloudDeployerApplicationSpec spec) {
		this.spec = spec;
	}
}
