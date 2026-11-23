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

package com.davils.kapi.runtime.client

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRedirect
import io.ktor.client.plugins.pluginOrNull

/**
 * Builds an [ApiClient] over [http], the `HttpClient` of the application with whatever engine it chose.
 *
 * The client never creates, configures or closes [http]. It must not follow redirects itself: create it with
 * `followRedirects = false` and let [ClientSettingsBuilder.redirects] follow them, so every hop passes the destination
 * policy and credentials stay with their origin.
 *
 * @param http the `HttpClient` of the application.
 * @param configure changes the settings from their defaults.
 * @return the client.
 * @throws com.davils.kore.core.validation.report.ValidationException if the settings are invalid or [http] follows
 * redirects itself.
 * @since 1.0.0
 */
public fun apiClient(http: HttpClient, configure: ClientSettingsBuilder.() -> Unit = {}): ApiClient {
    val builder = ClientSettingsBuilder(isFollowingRedirects = http.pluginOrNull(HttpRedirect) != null)
    builder.configure()
    return ApiClient(http, builder.build())
}
