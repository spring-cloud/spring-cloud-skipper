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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.skipper.SkipperException;
import org.springframework.util.StringUtils;

/**
 * The {@link ApplicationManifestReader} implementation that delegates to the specific reader based on the matching
 * {@link ApplicationManifest#kind}s.
 *
 * @author Ilayaperumal Gopinathan
 */
public class DelegatingApplicationManifestReader implements ApplicationManifestReader {

	private final static Logger logger = LoggerFactory.getLogger(DelegatingApplicationManifestReader.class);

	private final List<ApplicationManifestReader> applicationManifestReaders;

	public DelegatingApplicationManifestReader(List<ApplicationManifestReader> applicationManifestReaders) {
		this.applicationManifestReaders = applicationManifestReaders;
	}

	@Override
	public String[] getSupportedKinds() {
		return new String[0];
	}

	@Override
	public List<? extends ApplicationManifest> read(String manifest) {
		YAMLMapper mapper = new YAMLMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		List<ApplicationManifest> applicationManifests = new ArrayList<>();
		try {
			// read the manifest to get all the application kinds specified in the manifest.
			MappingIterator<ApplicationManifest> it = mapper.readerFor(ApplicationManifest.class).readValues(manifest);
			List<String> appKinds = new ArrayList<>();
			while (it.hasNextValue()) {
				appKinds.add(it.next().getKind());
			}
			ApplicationManifestReader applicationManifestReader = getMatchingSpecReader(appKinds);
			applicationManifests.addAll(applicationManifestReader.read(manifest));
			return applicationManifests;
		}
		catch (JsonMappingException e) {
			logger.error("Can't parse Package's manifest YAML = " + manifest);
			throw new SkipperException("JsonMappingException - Can't parse Package's manifest YAML = " + manifest, e);
		}
		catch (IOException e) {
			logger.error("Can't parse Package's manifest YAML = " + manifest);
			throw new SkipperException("IOException - Can't parse Package's manifest YAML = " + manifest, e);
		}
	}

	private ApplicationManifestReader getMatchingSpecReader(List<String> applicationKinds) {
		// find the first matching reader for the given application kind
		for (ApplicationManifestReader applicationManifestReader : this.applicationManifestReaders) {
			if (Arrays.asList(applicationManifestReader.getSupportedKinds()).containsAll(applicationKinds)) {
				return applicationManifestReader;
			}
		}
		throw new IllegalStateException("No reader available to read all the kinds [ " +
				StringUtils.collectionToCommaDelimitedString(applicationKinds) + " ] in the spec");
	}
}
