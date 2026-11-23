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

package com.davils.kapi.runtime.client.oauth.internal

import com.davils.kapi.runtime.client.internal.Texts
import com.davils.kapi.runtime.client.oauth.AuthorizationRequest
import com.davils.kapi.runtime.client.oauth.AuthorizationResponse
import com.davils.kapi.runtime.client.oauth.DeviceAuthorization
import com.davils.kapi.runtime.client.oauth.Grant
import com.davils.kapi.runtime.client.oauth.OAuthSettings
import com.davils.kapi.runtime.core.security.Secret
import com.davils.kore.core.failure.attempted
import io.ktor.http.URLBuilder
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val GRANT_TYPE: String = "grant_type"

private const val SCOPE: String = "scope"

private const val CLIENT_ID: String = "client_id"

private const val REFRESH_TOKEN: String = "refresh_token"

private const val CODE: String = "code"

private const val REDIRECT_URI: String = "redirect_uri"

private const val CODE_VERIFIER: String = "code_verifier"

private const val DEVICE_CODE: String = "device_code"

private const val PENDING: String = "authorization_pending"

private const val SLOW_DOWN: String = "slow_down"

private const val NO_ENDPOINT: String = "no endpoint"

private const val NOT_AUTHORIZED: String = "not authorized"

private const val EXPIRED: String = "expired"

private const val NO_SUBJECT: String = "no subject token"

private const val OK: Int = 200

private const val MOST_DEVICE_SECONDS: Long = 24L * 60 * 60

private val defaultInterval: Duration = 5.seconds

private val slowDown: Duration = 5.seconds

internal class Grants(
    private val settings: OAuthSettings,
    private val endpoint: TokenEndpoint,
    private val discoverer: Discoverer,
    private val post: EndpointPost,
) {
    private val scope: List<Pair<String, String>> =
        listOfNotNull((SCOPE to settings.scopes.joinToString(" ")).takeIf { settings.scopes.isNotEmpty() })

    suspend fun obtain(grant: Grant): Answer {
        val url = discoverer.tokenUrl() ?: return Answer.Refused(NO_ENDPOINT)
        return when (grant) {
            Grant.ClientCredentials -> {
                endpoint.request(url, listOf(GRANT_TYPE to grant.name) + scope)
            }

            is Grant.RefreshToken -> {
                endpoint.request(url, listOf(GRANT_TYPE to grant.name, REFRESH_TOKEN to grant.token.reveal()) + scope)
            }

            is Grant.AuthorizationCode -> {
                authorized(url, grant)
            }

            is Grant.DeviceCode -> {
                deviceAuthorized(url, grant)
            }

            is Grant.TokenExchange -> {
                exchanged(url, grant)
            }
        }
    }

    private suspend fun authorized(url: String, grant: Grant.AuthorizationCode): Answer {
        val pkce = Pkce.create(settings.crypto)
        val code = codeOf(grant = grant, pkce = pkce) ?: return Answer.Refused(NOT_AUTHORIZED)
        val fields = listOf(
            GRANT_TYPE to grant.name,
            CODE to code.reveal(),
            REDIRECT_URI to grant.redirectUri,
            CODE_VERIFIER to pkce.verifier,
        )
        return endpoint.request(url, fields)
    }

    private suspend fun codeOf(grant: Grant.AuthorizationCode, pkce: Pkce): Secret? {
        val authorization = discoverer.authorizationUrl() ?: return null
        val state = OAuthJson.random(settings.crypto)
        val target = authorizationUrlOf(endpoint = authorization, grant = grant, pkce = pkce, state = state)
        val request = AuthorizationRequest(url = target, state = state, redirectUri = grant.redirectUri)
        val response = attempted { grant.redirect.authorize(request) }.getOrNull()
        return response?.takeIf { found -> isAuthentic(found, state) }?.code
    }

    private fun authorizationUrlOf(
        endpoint: String,
        grant: Grant.AuthorizationCode,
        pkce: Pkce,
        state: String,
    ): String {
        val builder = URLBuilder(endpoint)
        val parameters = listOf(
            "response_type" to CODE,
            CLIENT_ID to settings.clientId,
            REDIRECT_URI to grant.redirectUri,
            "state" to state,
            "code_challenge" to pkce.challenge,
            "code_challenge_method" to Pkce.METHOD,
        )
        (parameters + scope).forEach { (name, value) -> builder.parameters.append(name = name, value = value) }
        return builder.buildString()
    }

    private fun isAuthentic(response: AuthorizationResponse, state: String): Boolean {
        val isState = Secret(response.state) == Secret(state)
        val issuer = response.issuer
        return isState && (issuer == null || issuer == settings.issuer)
    }

    private suspend fun deviceAuthorized(url: String, grant: Grant.DeviceCode): Answer {
        val shown = shownOf(grant) ?: return Answer.Refused(NOT_AUTHORIZED)
        val (response, authorization) = shown
        return polled(url = url, grant = grant, response = response, authorization = authorization)
    }

    private suspend fun shownOf(grant: Grant.DeviceCode): Pair<DeviceResponse, DeviceAuthorization>? {
        val posted = discoverer.deviceUrl()?.let { found -> post.post(url = found, fields = scope, isProven = false) }
        val reply = (posted as? Reply.Answered)?.takeIf { found -> found.status == OK }
        val response = reply?.let { found -> DeviceResponse.of(found.content) }
        val authorization = response?.let(::authorizationOf) ?: return null
        val isShown = attempted { grant.prompt.show(authorization) }.isSuccess
        return (response to authorization).takeIf { isShown }
    }

    private fun authorizationOf(response: DeviceResponse): DeviceAuthorization? {
        val complete = response.verificationUriComplete?.takeIf(::isPage)
        val expiresIn = OAuthJson.seconds(value = response.expiresIn, most = MOST_DEVICE_SECONDS)?.seconds
        val isUsable = isPage(response.verificationUri) && response.deviceCode.isNotEmpty()
        val lifetime = expiresIn?.takeIf { isUsable } ?: return null
        return DeviceAuthorization(
            userCode = response.userCode,
            verificationUri = response.verificationUri,
            verificationUriComplete = complete,
            expiresIn = lifetime,
        )
    }

    private fun isPage(url: String): Boolean {
        val query = url.substringAfter(delimiter = '?', missingDelimiterValue = "")
        val isQueryVisible = query.all { char -> char in '!'..'~' && char != '#' }
        return isQueryVisible && Texts.isProtectedUrl(url.substringBefore('?'))
    }

    private suspend fun polled(
        url: String,
        grant: Grant.DeviceCode,
        response: DeviceResponse,
        authorization: DeviceAuthorization,
    ): Answer {
        val deadline = settings.timeSource.markNow() + minOf(a = authorization.expiresIn, b = grant.maxWait)
        val asked = OAuthJson.seconds(value = response.interval, most = MOST_DEVICE_SECONDS)?.seconds ?: defaultInterval
        var interval = maxOf(a = asked, b = grant.minimumInterval)
        var answer: Answer = Answer.Refused(EXPIRED)
        var isPolling = true
        val fields = listOf(GRANT_TYPE to grant.name, DEVICE_CODE to response.deviceCode)
        while (isPolling && deadline.hasNotPassedNow()) {
            delay(interval)
            val polled = endpoint.request(url, fields)
            val error = (polled as? Answer.Refused)?.error
            if (error == SLOW_DOWN) interval += slowDown
            isPolling = error == PENDING || error == SLOW_DOWN
            answer = polled.takeUnless { isPolling } ?: answer
        }
        return answer
    }

    private suspend fun exchanged(url: String, grant: Grant.TokenExchange): Answer {
        val subject = attempted { grant.subject.token() }.getOrNull()
        val actor = grant.actor?.let { source -> attempted { source.token() }.getOrNull() }
        val isActorMissing = grant.actor != null && actor == null
        val token = subject?.takeUnless { isActorMissing } ?: return Answer.Refused(NO_SUBJECT)
        val optional = listOf(
            "actor_token" to actor?.reveal(),
            "actor_token_type" to grant.actorTokenType?.takeIf { actor != null },
            "audience" to grant.audience,
            "resource" to grant.resource,
            "requested_token_type" to grant.requestedTokenType,
        )
        val present = optional.mapNotNull { (name, value) -> value?.let { found -> name to found } }
        val fields = listOf(
            GRANT_TYPE to grant.name,
            "subject_token" to token.reveal(),
            "subject_token_type" to grant.subjectTokenType,
        )
        return endpoint.request(url, fields + present + scope)
    }
}
