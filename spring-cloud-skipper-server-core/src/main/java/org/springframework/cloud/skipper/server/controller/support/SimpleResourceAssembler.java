/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.cloud.skipper.server.controller.support;

import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.Resources;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link ResourceAssembler}/{@link ResourcesAssembler} that focuses purely on the domain type,
 * returning back {@link Resource} and {@link Resources} for that type instead of
 * {@link org.springframework.hateoas.ResourceSupport}.
 *
 * @author Greg Turnquist
 */
public class SimpleResourceAssembler<T> implements ResourceAssembler<T, Resource<T>>, ResourcesAssembler<T, Resource<T>> {

	/**
	 * Converts the given entity into a {@link Resource}.
	 *
	 * @param entity the entity
	 * @return a resource for the entity.
	 */
	@Override
	public Resource<T> toResource(T entity) {

		Resource<T> resource = new Resource<T>(entity);

		addLinks(resource);

		return resource;
	}

	/**
	 * Converts all given entities into resources and wraps the collection as a resource as well.
	 *
	 * @see #toResource(Object)
	 * @param entities must not be {@literal null}.
	 * @return {@link Resources} containing {@link Resource} of {@code T}.
	 */
	public Resources<Resource<T>> toResources(Iterable<? extends T> entities) {

		Assert.notNull(entities, "Entities must not be null!");
		List<Resource<T>> result = new ArrayList<Resource<T>>();

		for (T entity : entities) {
			result.add(toResource(entity));
		}

		Resources<Resource<T>> resources = new Resources<>(result);

		addLinks(resources);

		return resources;
	}

	/**
	 * Define links to add to every individual {@link Resource}.
	 *
	 * @param resource
	 */
	protected void addLinks(Resource<T> resource) {
		// Default adds no links
	}

	/**
	 * Define links to add to the {@link Resources} collection.
	 *
	 * @param resources
	 */
	protected void addLinks(Resources<Resource<T>> resources) {
		// Default adds no links.
	}
}