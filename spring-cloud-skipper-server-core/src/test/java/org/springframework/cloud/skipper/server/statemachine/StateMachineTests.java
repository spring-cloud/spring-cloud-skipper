/*
 * Copyright 2017-2018 the original author or authors.
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
package org.springframework.cloud.skipper.server.statemachine;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.skipper.domain.DeleteProperties;
import org.springframework.cloud.skipper.domain.Info;
import org.springframework.cloud.skipper.domain.InstallProperties;
import org.springframework.cloud.skipper.domain.InstallRequest;
import org.springframework.cloud.skipper.domain.Manifest;
import org.springframework.cloud.skipper.domain.Package;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.Status;
import org.springframework.cloud.skipper.domain.StatusCode;
import org.springframework.cloud.skipper.domain.UpgradeRequest;
import org.springframework.cloud.skipper.domain.deployer.ReleaseDifference;
import org.springframework.cloud.skipper.server.deployer.ReleaseAnalysisReport;
import org.springframework.cloud.skipper.server.deployer.ReleaseManager;
import org.springframework.cloud.skipper.server.deployer.strategies.DeployAppStep;
import org.springframework.cloud.skipper.server.deployer.strategies.HandleHealthCheckStep;
import org.springframework.cloud.skipper.server.deployer.strategies.HealthCheckProperties;
import org.springframework.cloud.skipper.server.deployer.strategies.HealthCheckStep;
import org.springframework.cloud.skipper.server.deployer.strategies.UpgradeStrategy;
import org.springframework.cloud.skipper.server.deployer.strategies.UpgradeStrategyFactory;
import org.springframework.cloud.skipper.server.repository.ReleaseRepository;
import org.springframework.cloud.skipper.server.service.PackageService;
import org.springframework.cloud.skipper.server.service.ReleaseReportService;
import org.springframework.cloud.skipper.server.service.ReleaseService;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperEventHeaders;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperEvents;
import org.springframework.cloud.skipper.server.statemachine.SkipperStateMachineService.SkipperStates;
import org.springframework.cloud.skipper.server.statemachine.StateMachineTests.TestConfig;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.persist.StateMachineRuntimePersister;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.statemachine.support.DefaultExtendedState;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.statemachine.test.StateMachineTestPlan;
import org.springframework.statemachine.test.StateMachineTestPlanBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;

/**
 * Generic tests for skipper statemachine logic. In these tests we simply
 * want to test machine logic meaning we control actions by using
 * mocks for classes actions are using.
 *
 * @author Janne Valkealahti
 *
 */
@SuppressWarnings("unchecked")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class StateMachineTests {

	@Autowired
	private ApplicationContext context;

	@MockBean
	private StateMachineRuntimePersister<SkipperStates, SkipperEvents, String> stateMachineRuntimePersister;

	@MockBean
	private ReleaseManager releaseManager;

	@MockBean
	private PackageService packageService;

	@MockBean
	private ReleaseReportService releaseReportService;

	@MockBean
	private UpgradeStrategy upgradeStrategy;

	@MockBean
	private UpgradeStrategyFactory upgradeStrategyFactory;

	@MockBean
	private DeployAppStep deployAppStep;

	@MockBean
	private HealthCheckStep healthCheckStep;

	@MockBean
	private HandleHealthCheckStep handleHealthCheckStep;

	@MockBean
	private ReleaseService releaseService;

	@MockBean
	private ReleaseRepository releaseRepository;

	@MockBean
	private HealthCheckProperties healthCheckProperties;

	@SpyBean
	private UpgradeCancelAction upgradeCancelAction;

	@SpyBean
	private ErrorAction errorAction;

	@Test
	public void testFactory() {
		StateMachineFactory<SkipperStates, SkipperEvents> factory = context.getBean(StateMachineFactory.class);
		assertThat(factory).isNotNull();
		StateMachine<SkipperStates, SkipperEvents> stateMachine = factory.getStateMachine("testFactory");
		assertThat(stateMachine).isNotNull();
	}

	@Test
	public void testSimpleInstallShouldNotError() throws Exception {
		Mockito.when(packageService.downloadPackage(any()))
				.thenReturn(new org.springframework.cloud.skipper.domain.Package());
		Mockito.when(releaseService.install(any(), any())).thenReturn(new Release());

		Message<SkipperEvents> message = MessageBuilder
			.withPayload(SkipperEvents.INSTALL)
			.setHeader(SkipperEventHeaders.PACKAGE_METADATA, new PackageMetadata())
			.setHeader(SkipperEventHeaders.INSTALL_PROPERTIES, new InstallProperties())
			.setHeader(SkipperEventHeaders.VERSION, 1)
			.build();

		StateMachineFactory<SkipperStates, SkipperEvents> factory = context.getBean(StateMachineFactory.class);
		StateMachine<SkipperStates, SkipperEvents> stateMachine = factory.getStateMachine("testInstall");

		StateMachineTestPlan<SkipperStates, SkipperEvents> plan =
				StateMachineTestPlanBuilder.<SkipperStates, SkipperEvents>builder()
					.defaultAwaitTime(10)
					.stateMachine(stateMachine)
					.step()
						.expectStateMachineStarted(1)
						.expectStates(SkipperStates.INITIAL)
						.and()
					.step()
						.sendEvent(message)
						.expectStates(SkipperStates.INITIAL)
						.expectStateChanged(3)
						.expectStateEntered(SkipperStates.INSTALL,
								SkipperStates.INSTALL_INSTALL,
								SkipperStates.INITIAL)
						.and()
					.build();
		plan.test();

		Mockito.verify(errorAction, never()).execute(any());
	}

	@Test
	public void testRestoreFromInstallUsingInstallRequest() throws Exception {
		Mockito.when(releaseService.install(any(InstallRequest.class))).thenReturn(new Release());

		DefaultExtendedState extendedState = new DefaultExtendedState();
		extendedState.getVariables().put(SkipperEventHeaders.INSTALL_REQUEST, new InstallRequest());

		StateMachineContext<SkipperStates, SkipperEvents> stateMachineContext = new DefaultStateMachineContext<>(
				SkipperStates.INSTALL, SkipperEvents.INSTALL, null, extendedState);
		Mockito.when(stateMachineRuntimePersister.read(any())).thenReturn(stateMachineContext);

		StateMachineService<SkipperStates, SkipperEvents> stateMachineService = context.getBean(StateMachineService.class);
		StateMachine<SkipperStates, SkipperEvents> stateMachine = stateMachineService
				.acquireStateMachine("testRestoreFromInstallUsingInstallRequest", false);

		StateMachineTestPlan<SkipperStates, SkipperEvents> plan =
				StateMachineTestPlanBuilder.<SkipperStates, SkipperEvents>builder()
					.defaultAwaitTime(10)
					.stateMachine(stateMachine)
					.step()
						.expectStates(SkipperStates.INITIAL)
						.expectStateChanged(2)
						.and()
					.build();
		plan.test();

		Mockito.verify(upgradeCancelAction, never()).execute(any());
		Mockito.verify(errorAction, never()).execute(any());
	}

	@Test
	public void testRestoreFromUpgradeUsingUpgradeRequest() throws Exception {
		Manifest manifest = new Manifest();
		Release release = new Release();
		release.setManifest(manifest);
		Mockito.when(releaseReportService.createReport(any(), any(), any(boolean.class))).thenReturn(new ReleaseAnalysisReport(
				new ArrayList<>(), new ReleaseDifference(), release, release, new ArrayList<>(), false));
		Mockito.when(upgradeStrategy.checkStatus(any()))
				.thenReturn(true);
		Mockito.when(upgradeStrategyFactory.getUpgradeStrategy(any())).thenReturn(upgradeStrategy);

		DefaultExtendedState extendedState = new DefaultExtendedState();
		extendedState.getVariables().put(SkipperEventHeaders.UPGRADE_REQUEST, new UpgradeRequest());

		StateMachineContext<SkipperStates, SkipperEvents> stateMachineContext = new DefaultStateMachineContext<>(
				SkipperStates.UPGRADE, SkipperEvents.UPGRADE, null, extendedState);
		Mockito.when(stateMachineRuntimePersister.read(any())).thenReturn(stateMachineContext);

		StateMachineService<SkipperStates, SkipperEvents> stateMachineService = context.getBean(StateMachineService.class);
		StateMachine<SkipperStates, SkipperEvents> stateMachine = stateMachineService
				.acquireStateMachine("testRestoreFromUpgradeUsingUpgradeRequest", false);

		StateMachineTestPlan<SkipperStates, SkipperEvents> plan =
				StateMachineTestPlanBuilder.<SkipperStates, SkipperEvents>builder()
					.defaultAwaitTime(10)
					.stateMachine(stateMachine)
					.step()
						.expectStates(SkipperStates.INITIAL)
						.expectStateChanged(8)
						.and()
					.build();
		plan.test();
		Mockito.verify(upgradeCancelAction, never()).execute(any());
		Mockito.verify(errorAction, never()).execute(any());
	}

	@Test
	public void testRestoreFromInstallUsingInstallProperties() throws Exception {
		Mockito.when(releaseService.install(any(), any(InstallProperties.class))).thenReturn(new Release());

		DefaultExtendedState extendedState = new DefaultExtendedState();
		extendedState.getVariables().put(SkipperEventHeaders.INSTALL_PROPERTIES, new InstallProperties());

		StateMachineContext<SkipperStates, SkipperEvents> stateMachineContext = new DefaultStateMachineContext<>(
				SkipperStates.INSTALL, SkipperEvents.INSTALL, null, extendedState);
		Mockito.when(stateMachineRuntimePersister.read(any())).thenReturn(stateMachineContext);

		StateMachineService<SkipperStates, SkipperEvents> stateMachineService = context.getBean(StateMachineService.class);
		StateMachine<SkipperStates, SkipperEvents> stateMachine = stateMachineService
				.acquireStateMachine("testRestoreFromInstallUsingInstallProperties", false);

		StateMachineTestPlan<SkipperStates, SkipperEvents> plan =
				StateMachineTestPlanBuilder.<SkipperStates, SkipperEvents>builder()
					.defaultAwaitTime(10)
					.stateMachine(stateMachine)
					.step()
						.expectStates(SkipperStates.INITIAL)
						.expectStateChanged(2)
						.and()
					.build();
		plan.test();

		Mockito.verify(upgradeCancelAction, never()).execute(any());
		Mockito.verify(errorAction, never()).execute(any());
	}

	@Test
	public void testSimpleUpgradeShouldNotError() throws Exception {		
		Manifest manifest = new Manifest();
		Release release = new Release();
		release.setManifest(manifest);
		Mockito.when(releaseReportService.createReport(any(), any(), any(boolean.class))).thenReturn(new ReleaseAnalysisReport(
				new ArrayList<>(), new ReleaseDifference(), release, release, new ArrayList<>(), false));
		Mockito.when(upgradeStrategy.checkStatus(any()))
				.thenReturn(true);
		Mockito.when(upgradeStrategyFactory.getUpgradeStrategy(any())).thenReturn(upgradeStrategy);

		UpgradeRequest upgradeRequest = new UpgradeRequest();

		Message<SkipperEvents> message1 = MessageBuilder
				.withPayload(SkipperEvents.UPGRADE)
				.setHeader(SkipperEventHeaders.UPGRADE_REQUEST, upgradeRequest)
				.build();

		StateMachineFactory<SkipperStates, SkipperEvents> factory = context.getBean(StateMachineFactory.class);
		StateMachine<SkipperStates, SkipperEvents> stateMachine = factory.getStateMachine("testSimpleUpgradeShouldNotError");

		StateMachineTestPlan<SkipperStates, SkipperEvents> plan =
				StateMachineTestPlanBuilder.<SkipperStates, SkipperEvents>builder()
					.defaultAwaitTime(10)
					.stateMachine(stateMachine)
					.step()
						.expectStateMachineStarted(1)
						.expectStates(SkipperStates.INITIAL)
						.and()
					.step()
						.sendEvent(message1)
						.expectStates(SkipperStates.INITIAL)
						.expectStateChanged(9)
						.and()
					.build();
		plan.test();
		Mockito.verify(upgradeCancelAction, never()).execute(any());
		Mockito.verify(errorAction, never()).execute(any());
	}

	@Test
	public void testUpgradeFailsNewAppFailToDeploy() throws Exception {
		Manifest manifest = new Manifest();
		Release release = new Release();
		release.setManifest(manifest);
		Mockito.when(releaseReportService.createReport(any(), any(), any(boolean.class))).thenReturn(new ReleaseAnalysisReport(
				new ArrayList<>(), new ReleaseDifference(), release, release, new ArrayList<>(), false));
		Mockito.when(upgradeStrategy.checkStatus(any()))
				.thenReturn(false);
		Mockito.when(upgradeStrategyFactory.getUpgradeStrategy(any())).thenReturn(upgradeStrategy);

		UpgradeRequest upgradeRequest = new UpgradeRequest();

		// timeout 0 for things to fail immediately
		Message<SkipperEvents> message1 = MessageBuilder
				.withPayload(SkipperEvents.UPGRADE)
				.setHeader(SkipperEventHeaders.UPGRADE_REQUEST, upgradeRequest)
				.setHeader(SkipperEventHeaders.UPGRADE_TIMEOUT, 0L)
				.build();

		StateMachineFactory<SkipperStates, SkipperEvents> factory = context.getBean(StateMachineFactory.class);
		StateMachine<SkipperStates, SkipperEvents> stateMachine = factory.getStateMachine("testUpgradeFailsNewAppFailToDeploy");

		StateMachineTestPlan<SkipperStates, SkipperEvents> plan =
				StateMachineTestPlanBuilder.<SkipperStates, SkipperEvents>builder()
					.defaultAwaitTime(10)
					.stateMachine(stateMachine)
					.step()
						.expectStateMachineStarted(1)
						.expectStates(SkipperStates.INITIAL)
						.and()
					.step()
						.sendEvent(message1)
						.expectStates(SkipperStates.INITIAL)
						.expectStateChanged(9)
						.expectStateEntered(SkipperStates.UPGRADE,
								SkipperStates.UPGRADE_START,
								SkipperStates.UPGRADE_DEPLOY_TARGET_APPS,
								SkipperStates.UPGRADE_WAIT_TARGET_APPS,
								SkipperStates.UPGRADE_CHECK_TARGET_APPS,
								SkipperStates.UPGRADE_WAIT_TARGET_APPS,
								SkipperStates.UPGRADE_DEPLOY_TARGET_APPS_FAILED,
								SkipperStates.UPGRADE_CANCEL,
								SkipperStates.INITIAL)
						.and()
					.build();
		plan.test();

		Mockito.verify(upgradeCancelAction).execute(any());
		Mockito.verify(errorAction, never()).execute(any());
	}

	@Test
	public void testUpgradeCancelWhileCheckingApps() throws Exception {
		Manifest manifest = new Manifest();
		Release release = new Release();
		release.setManifest(manifest);
		Mockito.when(releaseReportService.createReport(any(), any(), any(boolean.class))).thenReturn(new ReleaseAnalysisReport(
				new ArrayList<>(), new ReleaseDifference(), release, release, new ArrayList<>(), false));
		Mockito.when(upgradeStrategy.checkStatus(any()))
				.thenReturn(false);
		Mockito.when(upgradeStrategyFactory.getUpgradeStrategy(any())).thenReturn(upgradeStrategy);

		UpgradeRequest upgradeRequest = new UpgradeRequest();

		// timeout 60s giving time to try cancel
		Message<SkipperEvents> message1 = MessageBuilder
				.withPayload(SkipperEvents.UPGRADE)
				.setHeader(SkipperEventHeaders.UPGRADE_REQUEST, upgradeRequest)
				.setHeader(SkipperEventHeaders.UPGRADE_TIMEOUT, 60000L)
				.build();

		Message<SkipperEvents> message2 = MessageBuilder
				.withPayload(SkipperEvents.UPGRADE_CANCEL)
				.build();

		StateMachineFactory<SkipperStates, SkipperEvents> factory = context.getBean(StateMachineFactory.class);
		StateMachine<SkipperStates, SkipperEvents> stateMachine = factory.getStateMachine("testUpgradeCancelWhileCheckingApps");

		StateMachineTestPlan<SkipperStates, SkipperEvents> plan =
				StateMachineTestPlanBuilder.<SkipperStates, SkipperEvents>builder()
					.defaultAwaitTime(10)
					.stateMachine(stateMachine)
					.step()
						.expectStateMachineStarted(1)
						.expectStates(SkipperStates.INITIAL)
						.and()
					.step()
						.sendEvent(message1)
						.expectStateChanged(4)
						.expectStateEntered(SkipperStates.UPGRADE,
								SkipperStates.UPGRADE_START,
								SkipperStates.UPGRADE_DEPLOY_TARGET_APPS,
								SkipperStates.UPGRADE_WAIT_TARGET_APPS)
						.and()
					.step()
						.sendEvent(message2)
						.expectStateChanged(2)
						.expectStateEntered(SkipperStates.UPGRADE_CANCEL,
								SkipperStates.INITIAL)
						.and()
					.build();
		plan.test();

		Mockito.verify(upgradeCancelAction).execute(any());
		Mockito.verify(errorAction, never()).execute(any());
	}


	@Test
	public void testRollbackInstall() throws Exception {
		Release release = new Release();
		Status status = new Status();
		status.setStatusCode(StatusCode.DELETED);
		Info info = Info.createNewInfo("xxx");
		info.setStatus(status);
		release.setPkg(createPkg());
		release.setInfo(info);
		Mockito.when(releaseRepository.findLatestReleaseForUpdate(any())).thenReturn(release);
		Mockito.when(releaseRepository.findReleaseToRollback(any())).thenReturn(release);
		Mockito.when(releaseService.install(any(InstallRequest.class))).thenReturn(release);


		Message<SkipperEvents> message1 = MessageBuilder
				.withPayload(SkipperEvents.ROLLBACK)
				.setHeader(SkipperEventHeaders.RELEASE_NAME, "testRollbackInstall")
				.setHeader(SkipperEventHeaders.ROLLBACK_VERSION, 0)
				.build();

		StateMachineFactory<SkipperStates, SkipperEvents> factory = context.getBean(StateMachineFactory.class);
		StateMachine<SkipperStates, SkipperEvents> stateMachine = factory.getStateMachine("testRollbackInstall");

		StateMachineTestPlan<SkipperStates, SkipperEvents> plan =
				StateMachineTestPlanBuilder.<SkipperStates, SkipperEvents>builder()
					.defaultAwaitTime(10)
					.stateMachine(stateMachine)
					.step()
						.expectStateMachineStarted(1)
						.expectStates(SkipperStates.INITIAL)
						.and()
					.step()
						.sendEvent(message1)
						.expectStates(SkipperStates.INITIAL)
						.expectStateChanged(5)
						.expectStateEntered(SkipperStates.ROLLBACK,
								SkipperStates.ROLLBACK_START,
								SkipperStates.INSTALL,
								SkipperStates.INSTALL_INSTALL,
								SkipperStates.INITIAL)
						.and()
					.build();
		plan.test();

		Mockito.verify(errorAction, never()).execute(any());
	}

	@Test
	public void testDeleteSucceed() throws Exception {
		Mockito.when(releaseService.delete(any(String.class), any(boolean.class))).thenReturn(new Release());
		DeleteProperties deleteProperties = new DeleteProperties();
		Message<SkipperEvents> message1 = MessageBuilder
				.withPayload(SkipperEvents.DELETE)
				.setHeader(SkipperEventHeaders.RELEASE_NAME, "testDeleteSucceed")
				.setHeader(SkipperEventHeaders.RELEASE_DELETE_PROPERTIES, deleteProperties)
				.build();

		StateMachineFactory<SkipperStates, SkipperEvents> factory = context.getBean(StateMachineFactory.class);
		StateMachine<SkipperStates, SkipperEvents> stateMachine = factory.getStateMachine("testDeleteSucceed");

		StateMachineTestPlan<SkipperStates, SkipperEvents> plan =
				StateMachineTestPlanBuilder.<SkipperStates, SkipperEvents>builder()
					.defaultAwaitTime(10)
					.stateMachine(stateMachine)
					.step()
						.expectStateMachineStarted(1)
						.expectStates(SkipperStates.INITIAL)
						.and()
					.step()
						.sendEvent(message1)
						.expectStates(SkipperStates.INITIAL)
						.expectStateChanged(3)
						.expectStateEntered(SkipperStates.DELETE,
								SkipperStates.DELETE_DELETE,
								SkipperStates.INITIAL)
						.and()
					.build();
		plan.test();

		Mockito.verify(errorAction, never()).execute(any());
	}

	@Test
	public void testRestoreFromDeleteUsingDeleteProperties() throws Exception {
		Mockito.when(releaseService.delete(any(String.class), any(boolean.class))).thenReturn(new Release());
		DeleteProperties deleteProperties = new DeleteProperties();

		DefaultExtendedState extendedState = new DefaultExtendedState();
		extendedState.getVariables().put(SkipperEventHeaders.RELEASE_DELETE_PROPERTIES, deleteProperties);

		StateMachineContext<SkipperStates, SkipperEvents> stateMachineContext = new DefaultStateMachineContext<>(
				SkipperStates.DELETE, SkipperEvents.DELETE, null, extendedState);
		Mockito.when(stateMachineRuntimePersister.read(any())).thenReturn(stateMachineContext);

		StateMachineService<SkipperStates, SkipperEvents> stateMachineService = context.getBean(StateMachineService.class);
		StateMachine<SkipperStates, SkipperEvents> stateMachine = stateMachineService
				.acquireStateMachine("testRestoreFromDeleteUsingDeleteProperties", false);

		StateMachineTestPlan<SkipperStates, SkipperEvents> plan =
				StateMachineTestPlanBuilder.<SkipperStates, SkipperEvents>builder()
					.defaultAwaitTime(10)
					.stateMachine(stateMachine)
					.step()
						.expectStates(SkipperStates.INITIAL)
						.expectStateChanged(2)
						.and()
					.build();
		plan.test();

		Mockito.verify(upgradeCancelAction, never()).execute(any());
		Mockito.verify(errorAction, never()).execute(any());
	}

	private Package createPkg() {
		PackageMetadata packageMetadata1 = new PackageMetadata();
		packageMetadata1.setApiVersion("skipper.spring.io/v1");
		packageMetadata1.setKind("SpringCloudDeployerApplication");
		setId(PackageMetadata.class, packageMetadata1, "id", 1L);
		packageMetadata1.setRepositoryId(1L);
		packageMetadata1.setName("package1");
		packageMetadata1.setVersion("1.0.0");
		Package pkg1 = new Package();
		pkg1.setMetadata(packageMetadata1);
		return pkg1;
	}

	private static void setId(Class<?> clazz, Object instance, String fieldName, Object value) {
		try {
			Field field = ReflectionUtils.findField(clazz, fieldName);
			field.setAccessible(true);
			int modifiers = field.getModifiers();
			Field modifierField = field.getClass().getDeclaredField("modifiers");
			modifiers = modifiers & ~Modifier.FINAL;
			modifierField.setAccessible(true);
			modifierField.setInt(field, modifiers);
			ReflectionUtils.setField(field, instance, value);
		} catch (ReflectiveOperationException e) {
			throw new IllegalArgumentException(e);
		}
	}

	@Test
	public void testInstallDeniedWhileUpgrading() throws Exception {
		Manifest manifest = new Manifest();
		Release release = new Release();
		release.setManifest(manifest);
		Mockito.when(releaseReportService.createReport(any(), any(), any(boolean.class))).thenReturn(new ReleaseAnalysisReport(
				new ArrayList<>(), new ReleaseDifference(), release, release, new ArrayList<>(), false));
		Mockito.when(upgradeStrategy.checkStatus(any()))
				.thenReturn(false);
		Mockito.when(upgradeStrategyFactory.getUpgradeStrategy(any())).thenReturn(upgradeStrategy);

		UpgradeRequest upgradeRequest = new UpgradeRequest();

		Message<SkipperEvents> message1 = MessageBuilder
				.withPayload(SkipperEvents.UPGRADE)
				.setHeader(SkipperEventHeaders.UPGRADE_REQUEST, upgradeRequest)
				.build();

		Message<SkipperEvents> message2 = MessageBuilder
				.withPayload(SkipperEvents.INSTALL)
				.setHeader(SkipperEventHeaders.PACKAGE_METADATA, new PackageMetadata())
				.setHeader(SkipperEventHeaders.INSTALL_PROPERTIES, new InstallProperties())
				.setHeader(SkipperEventHeaders.VERSION, 1)
				.build();

		StateMachineFactory<SkipperStates, SkipperEvents> factory = context.getBean(StateMachineFactory.class);
		StateMachine<SkipperStates, SkipperEvents> stateMachine = factory.getStateMachine("testInstallDeniedWhileUpgrading");

		StateMachineTestPlan<SkipperStates, SkipperEvents> plan =
				StateMachineTestPlanBuilder.<SkipperStates, SkipperEvents>builder()
					.defaultAwaitTime(10)
					.stateMachine(stateMachine)
					.step()
						.expectStateMachineStarted(1)
						.expectStates(SkipperStates.INITIAL)
						.and()
					.step()
						.sendEvent(message1)
						.expectStateChanged(6)
						.and()
					.build();
		plan.test();

		// install event is not accepted
		boolean accepted = stateMachine.sendEvent(message2);
		assertThat(accepted).isFalse();
	}

	@Import(StateMachineConfiguration.class)
	static class TestConfig {

		@Bean
		public TaskExecutor skipperStateMachineTaskExecutor() {
			ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
			executor.setCorePoolSize(1);
			return executor;
		}

	}
}
