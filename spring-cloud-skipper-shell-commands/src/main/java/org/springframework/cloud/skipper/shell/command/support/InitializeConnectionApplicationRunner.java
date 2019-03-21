/*
 * Copyright 2017-2018 the original author or authors.
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
package org.springframework.cloud.skipper.shell.command.support;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.skipper.client.SkipperClientProperties;
import org.springframework.core.annotation.Order;
import org.springframework.shell.ResultHandler;
import org.springframework.shell.jline.InteractiveShellApplicationRunner;

/**
 * An {@link ApplicationRunner} implementation that initialises the connection to the
 * Data Flow Server. Has higher precedence than {@link InteractiveModeApplicationRunner}.
 *
 * @author Eric Bottard
 * @author Janne Valkealahti
 */
@Order(InteractiveShellApplicationRunner.PRECEDENCE - 10)
public class InitializeConnectionApplicationRunner implements ApplicationRunner {

	private TargetHolder targetHolder;

	private SkipperClientProperties skipperClientProperties;

	private ResultHandler<Exception> resultHandler;

	/**
	 * Construct a new InitializeConnectionApplicationRunner instance.
	 * @param targetHolder
	 * @param resultHandler
	 * @param skipperClientProperties
	 */
	public InitializeConnectionApplicationRunner(TargetHolder targetHolder,
			ResultHandler<Exception> resultHandler,
			SkipperClientProperties skipperClientProperties) {
		this.targetHolder = targetHolder;
		this.resultHandler = resultHandler;
		this.skipperClientProperties = skipperClientProperties;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		// user just wants to print help, do not try
		// to init connection. HelpAwareShellApplicationRunner simply output
		// usage and InteractiveModeApplicationRunner forces to run help.
		if (ShellUtils.hasHelpOption(args)) {
			return;
		}

		Target target = new Target(skipperClientProperties.getServerUri(), skipperClientProperties.getUsername(),
				skipperClientProperties.getPassword(), skipperClientProperties.isSkipSslValidation());

		// Attempt connection (including against default values) but do not crash the shell on
		// error
		try {
			targetHolder.changeTarget(target, null);
		}
		catch (Exception e) {
			resultHandler.handleResult(e);
		}

	}
}
