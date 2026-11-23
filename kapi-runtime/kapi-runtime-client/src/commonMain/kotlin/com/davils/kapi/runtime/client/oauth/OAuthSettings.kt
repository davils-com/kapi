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

import com.davils.kapi.runtime.client.dpop.DpopProofs
import com.davils.kapi.runtime.client.internal.Texts
import com.davils.kapi.runtime.core.destination.DestinationPolicy
import com.davils.konfetti.core.EventBus
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import com.davils.krypt.jose.jws.JwsAlgorithm
import com.davils.krypt.primitive.Crypto
import kotlin.time.Duration
import kotlin.time.TimeSource

private const val PROTECTED: String = "must be https, or http on this machine, without a query or a fragment"

private val macBytes: Map<JwsAlgorithm, Int> =
    mapOf(JwsAlgorithm.HS256 to 32, JwsAlgorithm.HS384 to 48, JwsAlgorithm.HS512 to 64)

private const val SCOPE_EXCLUDED: String = "\"\\"

private const val SCHEME_SYMBOLS: String = "+.-"

/**
 * The settings of an [OAuthTokens] source, built by [oauthTokens].
 *
 * Settings are refused when an endpoint or the issuer is not `https` (`http` only on this machine) or carries a query
 * or a fragment, the token endpoint is neither set nor discoverable, the client id is blank, the authentication does
 * not fit the grant or its algorithm, the grant lacks what it needs, a scope is malformed, a time is out of its
 * bounds, or the `HttpClient` follows redirects itself. Settings are immutable and safe to share; [toString] never
 * shows a secret.
 *
 * @since 1.0.0
 */
public class OAuthSettings internal constructor(builder: OAuthSettingsBuilder) : Validatable {
    /**
     * The token endpoint, or empty when it is discovered.
     *
     * @since 1.0.0
     */
    public val tokenUrl: String = builder.tokenUrl

    /**
     * The issuer identifier, or empty.
     *
     * @since 1.0.0
     */
    public val issuer: String = builder.issuer

    /**
     * Where the metadata of the issuer are discovered.
     *
     * @since 1.0.0
     */
    public val discovery: Discovery = builder.discovery

    /**
     * The authorization endpoint, or empty.
     *
     * @since 1.0.0
     */
    public val authorizationUrl: String = builder.authorizationUrl

    /**
     * The device authorization endpoint, or empty.
     *
     * @since 1.0.0
     */
    public val deviceAuthorizationUrl: String = builder.deviceAuthorizationUrl

    /**
     * The revocation endpoint, or empty.
     *
     * @since 1.0.0
     */
    public val revocationUrl: String = builder.revocationUrl

    /**
     * The client id.
     *
     * @since 1.0.0
     */
    public val clientId: String = builder.clientId

    /**
     * How the client proves who it is.
     *
     * @since 1.0.0
     */
    public val authentication: ClientAuthentication = builder.authentication

    /**
     * How the source gets its first token.
     *
     * @since 1.0.0
     */
    public val grant: Grant = builder.grant

    /**
     * The scopes the source asks for.
     *
     * @since 1.0.0
     */
    public val scopes: Set<String> = builder.scopes.toSet()

    /**
     * How long before its expiry a token is replaced.
     *
     * @since 1.0.0
     */
    public val leeway: Duration = builder.leeway

    /**
     * How long one request to an endpoint may take.
     *
     * @since 1.0.0
     */
    public val timeout: Duration = builder.timeout

    /**
     * The policy every endpoint must pass.
     *
     * @since 1.0.0
     */
    public val destinations: DestinationPolicy = builder.destinations

    /**
     * The cryptography of the source.
     *
     * @since 1.0.0
     */
    public val crypto: Crypto = builder.crypto

    /**
     * The DPoP proofs of the source, or `null`.
     *
     * @since 1.0.0
     */
    public val dpop: DpopProofs? = builder.dpop

    /**
     * The event bus, or `null`.
     *
     * @since 1.0.0
     */
    public val events: EventBus? = builder.events

    /**
     * The time source of the source.
     *
     * @since 1.0.0
     */
    public val timeSource: TimeSource.WithComparableMarks = builder.timeSource

    private val isFollowingRedirects: Boolean = builder.isFollowingRedirects

    override fun ValidationScope.validate() {
        validateUrls()
        if (clientId.isBlank() || clientId.any(Char::isISOControl)) {
            fail(::clientId, "must not be blank nor hold a control character", CODES.CLIENT_ID_INVALID)
        }
        validateAuthentication()
        validateGrant()
        if (scopes.any { scope -> scope.isEmpty() || !scope.all(::isScopeChar) }) {
            fail(::scopes, "must each be a non-empty token of RFC 6749 §3.3", CODES.SCOPE_INVALID)
        }
        if (leeway.isNegative() || leeway.isInfinite()) {
            fail(::leeway, "must be finite and not below zero", CODES.TIME_INVALID)
        }
        if (!timeout.isPositive() || timeout.isInfinite()) {
            fail(::timeout, "must be finite and above zero", CODES.TIME_INVALID)
        }
        if (isFollowingRedirects) {
            fail(::tokenUrl, FOLLOWED, CODES.REDIRECTS_FOLLOWED)
        }
    }

    override fun toString(): String = "OAuthSettings(tokenUrl=${SafeText.text(tokenUrl)}, " +
        "issuer=${SafeText.text(issuer)}, clientId=${SafeText.text(clientId)}, grant=$grant, " +
        "authentication=$authentication)"

    private fun ValidationScope.validateUrls() {
        val optional = listOf(issuer, authorizationUrl, deviceAuthorizationUrl, revocationUrl)
        val isTokenUrlValid = Texts.isProtectedUrl(tokenUrl) || (tokenUrl.isEmpty() && issuer.isNotEmpty())
        val areOthersValid = optional.all { url -> url.isEmpty() || Texts.isProtectedUrl(url) }
        if (!isTokenUrlValid || !areOthersValid) fail(::tokenUrl, PROTECTED, CODES.URL_INVALID)
    }

    private fun ValidationScope.validateAuthentication() {
        val chosen = authentication
        val isPublicMisused = chosen == ClientAuthentication.None && grant == Grant.ClientCredentials
        val isMacInvalid = chosen is ClientAuthentication.SecretJwt && !isMacUsable(chosen)
        val isKeyInvalid = chosen is ClientAuthentication.PrivateKeyJwt && chosen.algorithm in macBytes
        if (isPublicMisused || isMacInvalid || isKeyInvalid) {
            fail(::authentication, "does not fit the grant or its algorithm", CODES.AUTHENTICATION_INVALID)
        }
    }

    private fun isMacUsable(chosen: ClientAuthentication.SecretJwt): Boolean {
        val needed = macBytes[chosen.algorithm] ?: return false
        val bytes = chosen.secret.reveal().encodeToByteArray()
        return bytes.size >= needed
    }

    private fun ValidationScope.validateGrant() {
        val isDiscoverable = issuer.isNotEmpty()
        val isUsable = when (val chosen = grant) {
            is Grant.AuthorizationCode -> {
                isRedirectUri(chosen.redirectUri) && (authorizationUrl.isNotEmpty() || isDiscoverable)
            }

            is Grant.DeviceCode -> {
                isDeviceUsable(chosen) && (deviceAuthorizationUrl.isNotEmpty() || isDiscoverable)
            }

            is Grant.TokenExchange -> {
                chosen.subjectTokenType.isNotBlank()
            }

            is Grant.RefreshToken -> {
                true
            }

            Grant.ClientCredentials -> {
                true
            }
        }
        if (!isUsable) fail(::grant, "lacks an endpoint, a redirect URI or a time it needs", CODES.GRANT_INVALID)
    }

    private fun isDeviceUsable(chosen: Grant.DeviceCode): Boolean {
        val isWaitValid = chosen.maxWait.isPositive() && chosen.maxWait.isFinite()
        return isWaitValid && !chosen.minimumInterval.isNegative() && chosen.minimumInterval.isFinite()
    }

    private fun isRedirectUri(uri: String): Boolean {
        val scheme = uri.substringBefore(delimiter = ':', missingDelimiterValue = "")
        val isWritten = scheme.all { char -> char.isLetterOrDigit() || char in SCHEME_SYMBOLS }
        val isScheme = scheme.firstOrNull()?.isLetter() == true && isWritten
        return isScheme && '#' !in uri && uri.all { char -> char in '!'..'~' }
    }

    private fun isScopeChar(char: Char): Boolean = char in '!'..'~' && char !in SCOPE_EXCLUDED

    private companion object {
        const val FOLLOWED: String = "is asked by an HttpClient that follows redirects itself, which hands the " +
            "client secret to wherever a server points; create it with followRedirects = false"

        val CODES: OAuthViolationCodes = OAuthViolationCodes
    }
}
