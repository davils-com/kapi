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

import com.davils.kapi.runtime.client.dpop.DpopProofs
import com.davils.kapi.runtime.core.destination.DestinationPolicy
import com.davils.konfetti.core.EventBus
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.dsl.ValidatingBuilder
import com.davils.krypt.primitive.Crypto
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Configures the [OAuthSettings] of a token source.
 *
 * A builder is not thread-safe; the settings it builds are.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class OAuthSettingsBuilder internal constructor(internal val isFollowingRedirects: Boolean) :
    ValidatingBuilder<OAuthSettings>() {
    override val subject: String get() = "oauth settings"

    /**
     * The token endpoint; empty to discover it from [issuer].
     *
     * @since 1.0.0
     */
    public var tokenUrl: String = ""
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The issuer identifier of the authorization server, whose metadata name every endpoint left empty; empty for a
     * source that discovers nothing.
     *
     * @since 1.0.0
     */
    public var issuer: String = ""
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * Where the metadata of [issuer] are discovered; RFC 8414 by default.
     *
     * @since 1.0.0
     */
    public var discovery: Discovery = Discovery.OAuth
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The authorization endpoint of the authorization code grant; empty to discover it.
     *
     * @since 1.0.0
     */
    public var authorizationUrl: String = ""
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The device authorization endpoint of RFC 8628; empty to discover it.
     *
     * @since 1.0.0
     */
    public var deviceAuthorizationUrl: String = ""
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The revocation endpoint of RFC 7009; empty to discover it.
     *
     * @since 1.0.0
     */
    public var revocationUrl: String = ""
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The client id.
     *
     * @since 1.0.0
     */
    public var clientId: String = ""
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * How the client proves who it is; a public client by default.
     *
     * @since 1.0.0
     */
    public var authentication: ClientAuthentication = ClientAuthentication.None
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * How the source gets its first token; the client credentials grant by default.
     *
     * @since 1.0.0
     */
    public var grant: Grant = Grant.ClientCredentials
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The scopes the source asks for.
     *
     * @since 1.0.0
     */
    public var scopes: Set<String> = emptySet()
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * How long before its expiry a token is replaced; half a minute by default.
     *
     * @since 1.0.0
     */
    public var leeway: Duration = DEFAULT_LEEWAY
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * How long the metadata of the issuer is kept before it is discovered again; an hour by default, at most a day.
     * Metadata that could not be discovered is never kept, so the next use asks again.
     *
     * @since 1.2.0
     */
    public var metadataLifetime: Duration = DEFAULT_METADATA_LIFETIME
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * How long one request to an endpoint may take; half a minute by default.
     *
     * @since 1.0.0
     */
    public var timeout: Duration = DEFAULT_TIMEOUT
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The policy every endpoint must pass; `https` to public hosts by default.
     *
     * @since 1.0.0
     */
    public var destinations: DestinationPolicy = DestinationPolicy.Default
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The cryptography of krypt that draws the verifiers, states and identifiers, hashes and signs.
     *
     * @since 1.0.0
     */
    public var crypto: Crypto = Crypto()
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The DPoP proofs of RFC 9449 that bind every token to their key, or `null` for bearer tokens.
     *
     * @since 1.0.0
     */
    public var dpop: DpopProofs? = null
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The event bus of konfetti the source reports on, or `null`.
     *
     * @since 1.0.0
     */
    public var events: EventBus? = null
        set(value) {
            checkOpen()
            field = value
        }

    /**
     * The time source of the expiry of tokens and of polling.
     *
     * @since 1.0.0
     */
    public var timeSource: TimeSource.WithComparableMarks = TimeSource.Monotonic
        set(value) {
            checkOpen()
            field = value
        }

    override fun assemble(): OAuthSettings = OAuthSettings(this)

    /**
     * The defaults of the settings.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The leeway of a source the application does not configure.
         *
         * @since 1.0.0
         */
        public val DEFAULT_LEEWAY: Duration = 30.seconds

        /**
         * The timeout of a source the application does not configure.
         *
         * @since 1.0.0
         */
        public val DEFAULT_TIMEOUT: Duration = 30.seconds

        /**
         * How long discovered metadata is kept when the application does not say.
         *
         * @since 1.2.0
         */
        public val DEFAULT_METADATA_LIFETIME: Duration = 1.hours

        /**
         * The longest [metadataLifetime] any source accepts.
         *
         * @since 1.2.0
         */
        public val HARD_METADATA_LIFETIME: Duration = 1.days
    }
}
