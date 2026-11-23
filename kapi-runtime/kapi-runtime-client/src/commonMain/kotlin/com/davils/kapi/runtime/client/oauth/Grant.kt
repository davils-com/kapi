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

@file:OptIn(DavilsInternal::class)

package com.davils.kapi.runtime.client.oauth

import com.davils.kapi.runtime.core.security.Secret
import com.davils.kapi.runtime.core.security.TokenSource
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * How a token source gets its tokens from the token endpoint.
 *
 * After any grant that hands out a refresh token, the source goes on with [RefreshToken], and rotates it whenever
 * the server does. [toString] never shows a token.
 *
 * @since 1.0.0
 */
public sealed interface Grant {
    /**
     * The name of the grant in telemetry, such as `client_credentials`.
     *
     * @since 1.0.0
     */
    public val name: String

    /**
     * The client credentials grant of RFC 6749 §4.4, for a client acting on its own behalf.
     *
     * @since 1.0.0
     */
    public object ClientCredentials : Grant {
        override val name: String get() = "client_credentials"

        override fun toString(): String = "ClientCredentials"
    }

    /**
     * The refresh token grant of RFC 6749 §6.
     *
     * @property token the refresh token.
     * @since 1.0.0
     */
    public class RefreshToken(public val token: Secret) : Grant {
        override val name: String get() = "refresh_token"

        override fun equals(other: Any?): Boolean = other is RefreshToken && token == other.token

        override fun hashCode(): Int = token.hashCode()

        override fun toString(): String = "RefreshToken(${SafeText.REDACTED})"
    }

    /**
     * The authorization code grant of RFC 6749 §4.1 with the proof key for code exchange of RFC 7636, method `S256`.
     *
     * @property redirectUri the redirect URI registered for the client.
     * @property redirect the step that lets the user authorize the client.
     * @since 1.0.0
     */
    public class AuthorizationCode(public val redirectUri: String, public val redirect: AuthorizationRedirect) : Grant {
        override val name: String get() = "authorization_code"

        override fun toString(): String = "AuthorizationCode(${SafeText.text(redirectUri)})"
    }

    /**
     * The device authorization grant of RFC 8628, for a device without a browser.
     *
     * @property prompt shows the user what to do.
     * @property maxWait the longest the source polls, whatever the server allows.
     * @property minimumInterval the shortest wait between two polls, whatever the server asks for.
     * @since 1.0.0
     */
    public class DeviceCode(
        public val prompt: DevicePrompt,
        public val maxWait: Duration = 15.minutes,
        public val minimumInterval: Duration = 1.seconds,
    ) : Grant {
        override val name: String get() = "urn:ietf:params:oauth:grant-type:device_code"

        override fun toString(): String = "DeviceCode(maxWait=$maxWait)"
    }

    /**
     * The token exchange of RFC 8693: the token of [subject] for a token of this client, such as for a call on
     * behalf of a user.
     *
     * @property subject the source of the subject token.
     * @property subjectTokenType the type of the subject token, such as [TokenTypes.ACCESS_TOKEN].
     * @property actor the source of the actor token, or `null`.
     * @property actorTokenType the type of the actor token, or `null`.
     * @property audience the logical name of the service the token is for, or `null`.
     * @property resource the URI of the resource the token is for, or `null`.
     * @property requestedTokenType the type of the token asked for, or `null`.
     * @since 1.0.0
     */
    public class TokenExchange(
        public val subject: TokenSource,
        public val subjectTokenType: String = TokenTypes.ACCESS_TOKEN,
        public val actor: TokenSource? = null,
        public val actorTokenType: String? = null,
        public val audience: String? = null,
        public val resource: String? = null,
        public val requestedTokenType: String? = null,
    ) : Grant {
        override val name: String get() = "urn:ietf:params:oauth:grant-type:token-exchange"

        override fun toString(): String = "TokenExchange(${SafeText.text(subjectTokenType)})"
    }
}
