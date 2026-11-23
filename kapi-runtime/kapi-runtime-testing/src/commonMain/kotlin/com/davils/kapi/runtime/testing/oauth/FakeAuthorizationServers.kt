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

package com.davils.kapi.runtime.testing.oauth

/**
 * Creates a fake OAuth 2 authorization server on a Ktor `MockEngine`, configured by [configure].
 *
 * ```kotlin
 * val server = fakeAuthorizationServer { devicePolls = listOf(DevicePoll.Pending) }
 * server.httpClient().use { http ->
 *     val tokens = server.tokens(http, grant = Grant.DeviceCode(prompt = { _ -> }))
 *     tokens.token()
 * }
 * ```
 *
 * @param configure the issuer, the client, the tokens and the scripted answers.
 * @return the server.
 * @throws com.davils.kore.core.validation.report.ValidationException if the configuration is not valid, such as an
 * issuer that is no `https` URL; its report names every problem.
 * @since 1.0.0
 */
public fun fakeAuthorizationServer(configure: AuthorizationServerBuilder.() -> Unit = {}): FakeAuthorizationServer {
    val builder = AuthorizationServerBuilder()
    builder.configure()
    return builder.build()
}
