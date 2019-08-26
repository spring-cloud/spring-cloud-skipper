/*
 * Copyright 2019 the original author or authors.
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
package org.springframework.cloud.skipper.server.local.security.support.oauth2testserver;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.ConsumerTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Gunnar Hillert
 *
 */
@RestController
public class UserController {

	@Autowired
	private ConsumerTokenServices tokenServices;

	@Autowired
	private TokenStore tokenStore;

	@RequestMapping({"/user", "/me"})
	public Principal user(Principal principal) {
		return principal;
	}

	@RequestMapping("/revoke_token")
	public boolean revokeToken() {
		final OAuth2Authentication auth = (OAuth2Authentication) SecurityContextHolder
				.getContext().getAuthentication();
		final String token = this.tokenStore.getAccessToken(auth).getValue();
		return tokenServices.revokeToken(token);
	}

}
