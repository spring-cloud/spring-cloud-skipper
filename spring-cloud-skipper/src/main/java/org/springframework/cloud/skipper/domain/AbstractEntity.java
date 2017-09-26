/*
 * Copyright 2012-2015 the original author or authors.
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
package org.springframework.cloud.skipper.domain;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.springframework.hateoas.Identifiable;

/**
 * Base class for entity implementations. Uses a {@link String} id.
 * @author Oliver Gierke
 */
@MappedSuperclass
//TODO consider using Long instead of String
public class AbstractEntity implements Identifiable<String> {

	private final @Id @GeneratedValue(strategy = GenerationType.AUTO) @JsonIgnore String id;

	protected AbstractEntity() {
		this.id = null;
	}

	@Override
	public String getId() {
		return this.id;
	}
}

