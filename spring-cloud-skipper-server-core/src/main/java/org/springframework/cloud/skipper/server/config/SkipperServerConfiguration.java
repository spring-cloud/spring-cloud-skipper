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
package org.springframework.cloud.skipper.server.config;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.web.ErrorAttributes;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.deployer.resource.docker.DockerResourceLoader;
import org.springframework.cloud.deployer.resource.maven.MavenResourceLoader;
import org.springframework.cloud.deployer.resource.support.DelegatingResourceLoader;
import org.springframework.cloud.skipper.io.DefaultPackageReader;
import org.springframework.cloud.skipper.io.DefaultPackageWriter;
import org.springframework.cloud.skipper.io.PackageReader;
import org.springframework.cloud.skipper.io.PackageWriter;
import org.springframework.cloud.skipper.server.controller.RootController;
import org.springframework.cloud.skipper.server.controller.SkipperController;
import org.springframework.cloud.skipper.server.controller.SkipperErrorAttributes;
import org.springframework.cloud.skipper.server.deployer.AppDeployerReleaseManager;
import org.springframework.cloud.skipper.server.deployer.AppDeploymentRequestFactory;
import org.springframework.cloud.skipper.server.deployer.ReleaseAnalyzer;
import org.springframework.cloud.skipper.server.deployer.ReleaseManager;
import org.springframework.cloud.skipper.server.deployer.strategies.DeleteStep;
import org.springframework.cloud.skipper.server.deployer.strategies.DeployAppStep;
import org.springframework.cloud.skipper.server.deployer.strategies.HandleHealthCheckStep;
import org.springframework.cloud.skipper.server.deployer.strategies.HealthCheckProperties;
import org.springframework.cloud.skipper.server.deployer.strategies.HealthCheckStep;
import org.springframework.cloud.skipper.server.deployer.strategies.SimpleRedBlackUpgradeStrategy;
import org.springframework.cloud.skipper.server.deployer.strategies.UpgradeStrategy;
import org.springframework.cloud.skipper.server.index.PackageMetadataResourceProcessor;
import org.springframework.cloud.skipper.server.index.PackageSummaryResourceProcessor;
import org.springframework.cloud.skipper.server.index.SkipperControllerResourceProcessor;
import org.springframework.cloud.skipper.server.repository.AppDeployerDataRepository;
import org.springframework.cloud.skipper.server.repository.DeployerRepository;
import org.springframework.cloud.skipper.server.repository.PackageMetadataRepository;
import org.springframework.cloud.skipper.server.repository.ReleaseRepository;
import org.springframework.cloud.skipper.server.repository.RepositoryRepository;
import org.springframework.cloud.skipper.server.service.DeployerInitializationService;
import org.springframework.cloud.skipper.server.service.PackageMetadataService;
import org.springframework.cloud.skipper.server.service.PackageService;
import org.springframework.cloud.skipper.server.service.ReleaseReportService;
import org.springframework.cloud.skipper.server.service.ReleaseService;
import org.springframework.cloud.skipper.server.service.ReleaseStateUpdateService;
import org.springframework.cloud.skipper.server.service.RepositoryInitializationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.map.repository.config.EnableMapRepositories;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main configuration class for the server.
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 * @author Janne Valkealahti
 * @author Gunnar Hillert
 */
@Configuration
@EnableConfigurationProperties({ SkipperServerProperties.class, CloudFoundryPlatformProperties.class,
		LocalPlatformProperties.class, KubernetesPlatformProperties.class,
		MavenConfigurationProperties.class, HealthCheckProperties.class })
@EntityScan({ "org.springframework.cloud.skipper.domain", "org.springframework.cloud.skipper.server.domain" })
@EnableMapRepositories(basePackages = "org.springframework.cloud.skipper.server.repository")
@EnableJpaRepositories(basePackages = "org.springframework.cloud.skipper.server.repository")
@EnableTransactionManagement
@EnableAsync
public class SkipperServerConfiguration implements AsyncConfigurer {

	public static final String SKIPPER_THREAD_POOL_EXECUTOR = "skipperThreadPoolTaskExecutor";

	private final Logger logger = LoggerFactory.getLogger(SkipperServerConfiguration.class);

	@Bean
	public ErrorAttributes errorAttributes() {
		// override boot's DefaultErrorAttributes
		return new SkipperErrorAttributes();
	}

	@Bean
	public PackageSummaryResourceProcessor packageSummaryResourceProcessor() {
		return new PackageSummaryResourceProcessor();
	}

	@Bean
	public PackageMetadataResourceProcessor packageMetadataResourceProcessor() {
		return new PackageMetadataResourceProcessor();
	}

	@Bean
	public SkipperControllerResourceProcessor skipperControllerResourceProcessor() {
		return new SkipperControllerResourceProcessor();
	}

	@Bean
	public SkipperController skipperController(ReleaseService releaseService, PackageService packageService) {
		return new SkipperController(releaseService, packageService);
	}

	@Bean
	public RootController rootController() {
		return new RootController();
	}

	// Services

	@Bean
	public DeployerInitializationService deployerInitializationService(DeployerRepository deployerRepository,
			LocalPlatformProperties localPlatformProperties,
			CloudFoundryPlatformProperties cloudFoundryPlatformProperties,
			KubernetesPlatformProperties kubernetesPlatformProperties,
			SkipperServerProperties skipperServerProperties) {
		return new DeployerInitializationService(deployerRepository, localPlatformProperties,
				cloudFoundryPlatformProperties, kubernetesPlatformProperties,
				skipperServerProperties);
	}

	@Bean
	public PackageMetadataService packageMetadataService(RepositoryRepository repositoryRepository) {
		return new PackageMetadataService(repositoryRepository);
	}

	@Bean
	public PackageService packageService(RepositoryRepository repositoryRepository,
			PackageMetadataRepository packageMetadataRepository,
			PackageReader packageReader) {
		return new PackageService(repositoryRepository, packageMetadataRepository, packageReader);
	}

	@Bean
	public DelegatingResourceLoader delegatingResourceLoader(MavenConfigurationProperties mavenProperties) {
		DockerResourceLoader dockerLoader = new DockerResourceLoader();
		MavenResourceLoader mavenResourceLoader = new MavenResourceLoader(mavenProperties);
		Map<String, ResourceLoader> loaders = new HashMap<>();
		loaders.put("docker", dockerLoader);
		loaders.put("maven", mavenResourceLoader);
		return new DelegatingResourceLoader(loaders);
	}

	@Bean
	public ReleaseReportService releaseReportService(PackageMetadataRepository packageMetadataRepository,
			ReleaseRepository releaseRepository,
			PackageService packageService,
			ReleaseManager releaseManager) {
		return new ReleaseReportService(packageMetadataRepository, releaseRepository, packageService, releaseManager);
	}

	@Bean
	public ReleaseService releaseService(PackageMetadataRepository packageMetadataRepository,
			ReleaseRepository releaseRepository,
			PackageService packageService,
			ReleaseManager releaseManager,
			DeployerRepository deployerRepository,
			ReleaseReportService releaseReportService) {
		return new ReleaseService(packageMetadataRepository, releaseRepository,
				packageService, releaseManager,
				deployerRepository, releaseReportService);
	}

	@Bean
	@ConditionalOnProperty("spring.cloud.skipper.server.disableReleaseStateUpdateService")
	public ReleaseStateUpdateService releaseStateUpdateService(ReleaseManager releaseManager,
			ReleaseRepository releaseRepository) {
		return new ReleaseStateUpdateService(releaseManager, releaseRepository);
	}

	@Bean
	public RepositoryInitializationService repositoryInitializationService(RepositoryRepository repositoryRepository,
			PackageMetadataRepository packageMetadataRepository,
			PackageMetadataService packageMetadataService,
			SkipperServerProperties skipperServerProperties) {
		return new RepositoryInitializationService(repositoryRepository, packageMetadataRepository,
				packageMetadataService, skipperServerProperties);
	}

	// Deployer Package

	@Bean
	public AppDeployerReleaseManager appDeployerReleaseManager(ReleaseRepository releaseRepository,
			AppDeployerDataRepository appDeployerDataRepository,
			DeployerRepository deployerRepository,
			ReleaseAnalyzer releaseAnalyzer,
			AppDeploymentRequestFactory appDeploymentRequestFactory,
			UpgradeStrategy updateStrategy) {
		return new AppDeployerReleaseManager(releaseRepository, appDeployerDataRepository, deployerRepository,
				releaseAnalyzer, appDeploymentRequestFactory, updateStrategy);
	}

	@Bean
	public DeleteStep deleteStep(ReleaseRepository releaseRepository,
			DeployerRepository deployerRepository) {
		return new DeleteStep(releaseRepository, deployerRepository);
	}

	@Bean
	public UpgradeStrategy updateStrategy(DeployerRepository deployerRepository,
			AppDeployerDataRepository appDeployerDataRepository,
			AppDeploymentRequestFactory appDeploymentRequestFactory,
			HealthCheckStep healthCheckStep,
			HandleHealthCheckStep healthCheckAndDeleteStep,
			ReleaseRepository releaseRepository, DeployAppStep deployAppStep) {
		return new SimpleRedBlackUpgradeStrategy(deployerRepository,
				appDeployerDataRepository, appDeploymentRequestFactory, healthCheckStep, healthCheckAndDeleteStep,
				deployAppStep,
				releaseRepository);
	}

	@Bean
	public HealthCheckStep healthCheckStep(AppDeployerDataRepository appDeployerDataRepository,
			DeployerRepository deployerRepository,
			HealthCheckProperties healthCheckProperties) {
		return new HealthCheckStep(appDeployerDataRepository, deployerRepository, healthCheckProperties);
	}

	@Bean
	public DeployAppStep DeployAppStep(DeployerRepository deployerRepository,
			AppDeploymentRequestFactory appDeploymentRequestFactory,
			AppDeployerDataRepository appDeployerDataRepository, ReleaseRepository releaseRepository) {
		return new DeployAppStep(deployerRepository, appDeploymentRequestFactory, appDeployerDataRepository,
				releaseRepository);
	}

	@Bean
	public HandleHealthCheckStep healthCheckAndDeleteStep(ReleaseRepository releaseRepository,
			DeployerRepository deployerRepository,
			AppDeployerDataRepository appDeployerDataRepository,
			DeleteStep deleteStep,
			HealthCheckProperties healthCheckProperties) {
		return new HandleHealthCheckStep(releaseRepository,
				deployerRepository,
				appDeployerDataRepository,
				deleteStep,
				healthCheckProperties);
	}

	@Bean(name = SKIPPER_THREAD_POOL_EXECUTOR)
	@Override
	public Executor getAsyncExecutor() {
		return new ThreadPoolTaskExecutor();
	}

	@Override
	public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return (throwable, method, objects) -> logger.error("Exception thrown in @Async Method " + method.getName(),
				throwable);
	}

	@Bean
	public ReleaseAnalyzer releaseAnalysisService() {
		return new ReleaseAnalyzer();
	}

	@Bean
	public AppDeploymentRequestFactory appDeploymentRequestFactory(DelegatingResourceLoader delegatingResourceLoader) {
		return new AppDeploymentRequestFactory(delegatingResourceLoader);
	}

	@Bean
	public PackageReader packageReader() {
		return new DefaultPackageReader();
	}

	@Bean
	public PackageWriter packageWriter() {
		return new DefaultPackageWriter();
	}

}
