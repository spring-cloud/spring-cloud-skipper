/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.cloud.skipper.shell.command;

import javax.validation.constraints.NotNull;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.client.SkipperClient;
import org.springframework.http.HttpStatus;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.web.client.HttpStatusCodeException;

import static org.springframework.shell.standard.ShellOption.NULL;

/**
 * Commands that operation on the manifest.
 * @author Mark Pollack
 */
@ShellComponent
public class ManifestCommands extends AbstractSkipperCommand {

	private Yaml yaml;

	@Autowired
	public ManifestCommands(SkipperClient skipperClient) {
		this.skipperClient = skipperClient;
		DumperOptions dumperOptions = new DumperOptions();
		dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		dumperOptions.setPrettyFlow(true);
		this.yaml = new Yaml(dumperOptions);
	}

	@ShellMethod(key = "manifest get", value = "Get the manifest for a release")
	public Object getManifest(
			@ShellOption(help = "release name") @NotNull String releaseName,
			@ShellOption(help = "specific release version.", defaultValue = NULL) Integer releaseVersion) {
		String manifest;
		try {
			if (releaseVersion == null) {
				manifest = this.skipperClient.manifest(releaseName);
			}
			else {
				manifest = this.skipperClient.manifest(releaseName, releaseVersion);
			}
		}
		catch (HttpStatusCodeException e) {
			if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
				return "Release with name '" + releaseName + "' not found";
			}
			// if something else, rethrow
			throw e;
		}
		return manifest;
	}
}
