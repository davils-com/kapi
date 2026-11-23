/*
 * Copyright 2026 Davils
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.davils.kapi.runtime.client.oauth

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRedirect
import io.ktor.client.plugins.pluginOrNull

/**
 * Builds an OAuth 2 token source over [http], the `HttpClient` of the application, to hand to the credentials of a
 * client as the source of a scheme.
 *
 * @param http the `HttpClient` of the application; it must not follow redirects itself.
 * @param configure the endpoints, the client, its authentication and the grant.
 * @return the token source.
 * @throws com.davils.kore.core.validation.report.ValidationException if the settings are invalid or [http] follows
 * redirects itself.
 * @since 1.0.0
 */
public fun oauthTokens(http: HttpClient, configure: OAuthSettingsBuilder.() -> Unit): OAuthTokens {
    val builder = OAuthSettingsBuilder(isFollowingRedirects = http.pluginOrNull(HttpRedirect) != null)
    builder.configure()
    return OAuthTokens(http, builder.build())
}
