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
package org.springframework.cloud.skipper.index;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.skipper.config.SkipperServerProperties;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

/**
 * @author Mark Pollack
 */
@Component
public class PackageIndexDownloader implements ResourceLoaderAware {

	private final Logger logger = LoggerFactory.getLogger(PackageIndexDownloader.class);

	private ResourceLoader resourceLoader;

	private SkipperServerProperties skipperServerProperties;

	@Autowired
	public PackageIndexDownloader(SkipperServerProperties skipperServerProperties) {
		this.skipperServerProperties = skipperServerProperties;
	}

	public void downloadPackageIndexes() {
		for (String packageRepositoryUrl : this.skipperServerProperties.getPackageRepositoryUrls()) {
			Resource resource = resourceLoader.getResource(packageRepositoryUrl);
			try {
				File packageDir = new File(skipperServerProperties.getPackageIndexDir());
				packageDir.mkdirs();
				File downloadedFile = new File(packageDir, getDownloadedFilename(resource));
				StreamUtils.copy(resource.getInputStream(), new FileOutputStream(downloadedFile));
			}
			catch (IOException e) {
				logger.error("Could not process package file from " + packageRepositoryUrl, e);
			}

		}
	}

	private String getDownloadedFilename(Resource resource) throws IOException {
		URI uri = resource.getURI();
		StringBuilder stringBuilder = new StringBuilder();
		if (uri.getScheme().equals("file")) {
			stringBuilder.append("file");
			stringBuilder.append(uri.getPath().replaceAll("/", "_"));
		}
		return stringBuilder.toString();
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}
}
