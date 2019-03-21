/*
 * Copyright 2016-2017 the original author or authors.
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

package org.springframework.cloud.skipper.client.util;

import java.util.ArrayList;
import java.util.List;

import org.springframework.hateoas.ResourceSupport;

/**
 * Provides security related meta-information. E.g. is security enabled, username, roles
 * etc.
 *
 * @author Gunnar Hillert
 */
public class SecurityInfoResource extends ResourceSupport {

	private boolean authenticationEnabled;

	private boolean authorizationEnabled;

	private boolean formLogin;

	private boolean authenticated;

	private String username;

	private List<String> roles = new ArrayList<>(0);

	/**
	 * Default constructor for serialization frameworks.
	 */
	public SecurityInfoResource() {
	}

	/**
	 * @return true if the authentication feature is enabled, false otherwise
	 */
	public boolean isAuthenticationEnabled() {
		return authenticationEnabled;
	}

	public void setAuthenticationEnabled(boolean authenticationEnabled) {
		this.authenticationEnabled = authenticationEnabled;
	}

	/**
	 * @return true if the authorization feature is enabled, false otherwise.
	 */
	public boolean isAuthorizationEnabled() {
		return authorizationEnabled;
	}

	public void setAuthorizationEnabled(boolean authorizationEnabled) {
		this.authorizationEnabled = authorizationEnabled;
	}

	/**
	 * @return True if the user is authenticated
	 */
	public boolean isAuthenticated() {
		return authenticated;
	}

	public void setAuthenticated(boolean authenticated) {
		this.authenticated = authenticated;
	}

	/**
	 * @return The username of the authenticated user, null otherwise.
	 */
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Will only contain values if {@link #isAuthorizationEnabled()} is {@code true}.
	 *
	 * @return List of Roles, if no roles are associated, an empty collection is returned.
	 */
	public List<String> getRoles() {
		return roles;
	}

	public void setRoles(List<String> roles) {
		this.roles = roles;
	}

	/**
	 * @param role Adds the role to {@link #roles}
	 * @return the resource with an additional role
	 */
	public SecurityInfoResource addRole(String role) {
		this.roles.add(role);
		return this;
	}

	/**
	 * Returns {@code true} if form-login is used. In case of OAuth2 authentication,
	 * {@code false} is returned.
	 *
	 * @return True if form-login is, false if OAuth2 authentication is used
	 */
	public boolean isFormLogin() {
		return formLogin;
	}

	public void setFormLogin(boolean formLogin) {
		this.formLogin = formLogin;
	}

}
