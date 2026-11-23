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

import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Configures a [FakeAuthorizationServer] in the block of [fakeAuthorizationServer].
 *
 * A builder is not thread-safe; it is meant for one block of the DSL.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class AuthorizationServerBuilder internal constructor() : ValidatingBuilder<FakeAuthorizationServer>() {
    private val refused: MutableList<Pair<String, String>> = mutableListOf()

    override val subject: String get() = "fake authorization server"

    internal val refusals: List<Pair<String, String>> get() = refused.toList()

    /**
     * The issuer, an `https` URL without query and fragment; every endpoint lies below it.
     *
     * @since 1.0.0
     */
    public var issuer: String = "https://login.example.com"

    /**
     * The client the server knows; a request of any other client is answered with `invalid_client`.
     *
     * @since 1.0.0
     */
    public var clientId: String = "client"

    /**
     * The secret of the client, which it must present with HTTP Basic or in the form; `client-secret` by default, or
     * `null` for a public client that presents only its id.
     *
     * @since 1.0.0
     */
    public var clientSecret: String? = "client-secret"

    /**
     * The `token_type` of every token; `Bearer` by default, `DPoP` for a client that binds its tokens.
     *
     * @since 1.0.0
     */
    public var tokenType: String = "Bearer"

    /**
     * The lifetime of every access token, sent as `expires_in` in whole seconds, or `null` to send none; one hour by
     * default.
     *
     * @since 1.0.0
     */
    public var lifetime: Duration? = 1.hours

    /**
     * Whether every grant also issues a refresh token; `true` by default.
     *
     * @since 1.0.0
     */
    public var isRefreshTokenIssued: Boolean = true

    /**
     * The refresh tokens the server accepts besides those it issued, such as the one of a
     * [com.davils.kapi.runtime.client.oauth.Grant.RefreshToken].
     *
     * @since 1.0.0
     */
    public var refreshTokens: Set<String> = emptySet()

    /**
     * The `interval` of a device authorization in whole seconds; zero by default, so that a test does not wait.
     *
     * @since 1.0.0
     */
    public var interval: Duration = Duration.ZERO

    /**
     * The `expires_in` of a device authorization in whole seconds; ten minutes by default.
     *
     * @since 1.0.0
     */
    public var deviceLifetime: Duration = 10.minutes

    /**
     * The answers to the polls of the device authorization grant before the token is granted, in order; none by
     * default, which grants the first poll.
     *
     * @since 1.0.0
     */
    public var devicePolls: List<DevicePoll> = emptyList()

    /**
     * Refuses the next request of [grant] with [error] and status `400`; each call refuses one more request of the
     * grant before the server grants it again.
     *
     * @param grant the `grant_type`, such as `client_credentials`.
     * @param error the `error` of the answer; `invalid_grant` by default.
     * @throws IllegalStateException if the server was built already.
     * @since 1.0.0
     */
    public fun refuse(grant: String, error: String = "invalid_grant") {
        checkOpen()
        refused += grant to error
    }

    override fun assemble(): FakeAuthorizationServer = FakeAuthorizationServer(this)
}
