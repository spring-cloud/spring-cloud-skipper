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
package org.springframework.cloud.skipper.server.autoconfigure;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.core.env.ConfigurableEnvironment;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Chris Schaefer
 * @author Mark Pollack
 */
@RunWith(MockitoJUnitRunner.class)
public class ProfileApplicationListenerTests {

	private static final String[] ACTIVE_PROFILES = new String[0];

	@Mock
	private ConfigurableEnvironment environment;

	@Mock
	private ApplicationEnvironmentPreparedEvent event;

	private ProfileApplicationListener profileApplicationListener;

	@Before
	public void before() {
		when(environment.getActiveProfiles()).thenReturn(ACTIVE_PROFILES);
		when(event.getEnvironment()).thenReturn(environment);
		profileApplicationListener = new ProfileApplicationListener();
	}

	@Test
	public void shouldEnableLocalProfile() {
		profileApplicationListener.onApplicationEvent(event);
		verify(environment).addActiveProfile("local");
	}

	@Test
	public void shouldNotEnableLocalProfileRunningOnKubernetes() {
		when(environment.containsProperty("kubernetes_service_host")).thenReturn(true);
		profileApplicationListener.onApplicationEvent(event);
		verify(environment, never()).addActiveProfile("local");
	}

	@Test
	public void shouldNotEnableLocalProfileRunningOnCloudFoundry() {
		when(environment.containsProperty("VCAP_APPLICATION")).thenReturn(true);
		profileApplicationListener.onApplicationEvent(event);
		verify(environment, never()).addActiveProfile("local");
	}

	public void testAddedSpringCloudKubernetesConfigEnabledIsFalse() {
		// TODO
	}
}
