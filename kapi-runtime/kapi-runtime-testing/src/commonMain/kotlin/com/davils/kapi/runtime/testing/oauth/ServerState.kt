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

@file:OptIn(ExperimentalAtomicApi::class)

package com.davils.kapi.runtime.testing.oauth

import com.davils.kapi.runtime.client.oauth.AuthorizationRequest
import com.davils.kapi.runtime.client.oauth.AuthorizationResponse
import com.davils.kapi.runtime.client.oauth.TokenTypes
import com.davils.kapi.runtime.core.security.Secret
import com.davils.krypt.core.algorithm.DigestAlgorithm
import com.davils.krypt.primitive.Crypto
import io.ktor.http.Url
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.io.encoding.Base64

private const val OK: Int = 200

private const val INVALID_GRANT: String = "invalid_grant"

private const val UNSUPPORTED: String = "unsupported_grant_type"

private const val REFRESH_TOKEN: String = "refresh_token"

private const val DEVICE: String = "urn:ietf:params:oauth:grant-type:device_code"

private const val EXCHANGE: String = "urn:ietf:params:oauth:grant-type:token-exchange"

private val base64Url: Base64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)

internal class ServerState(private val server: FakeAuthorizationServer, private val crypto: Crypto) {
    private val unsupported: suspend (EndpointRequest) -> ServerReply = { _ -> ServerReply.error(UNSUPPORTED) }

    val issued: AtomicReference<List<String>> = AtomicReference(emptyList())

    val revoked: AtomicReference<List<String>> = AtomicReference(emptyList())

    private var counter: Int = 0

    private val refreshTokens: MutableSet<String> = server.acceptedRefreshTokens.toMutableSet()

    private val codes: MutableMap<String, Pair<String?, String?>> = mutableMapOf()

    private val deviceCodes: MutableSet<String?> = mutableSetOf()

    private val polls: ArrayDeque<DevicePoll> = ArrayDeque(server.devicePolls)

    private val refusals: MutableList<Pair<String, String>> = server.refusals.toMutableList()

    private val grants: Map<String, suspend (EndpointRequest) -> ServerReply> = mapOf(
        "client_credentials" to { request -> granted(request) },
        REFRESH_TOKEN to { request -> refreshed(request) },
        "authorization_code" to { request -> exchangedCode(request) },
        DEVICE to { request -> polled(request) },
        EXCHANGE to { request -> exchanged(request) },
    )

    suspend fun token(request: EndpointRequest): ServerReply {
        val grant = request.grantType.orEmpty()
        val refusal = refusals.firstOrNull { (name, _) -> name == grant }
        if (refusal != null) return refused(refusal)
        val handler = grants[grant] ?: unsupported
        return handler(request)
    }

    private fun refused(refusal: Pair<String, String>): ServerReply {
        refusals -= refusal
        return ServerReply.error(refusal.second)
    }

    fun device(): ServerReply {
        counter += 1
        val code = "device-code-$counter"
        val userCode = "USER-$counter"
        deviceCodes += code
        val page = "${server.issuer}/device"
        val body = buildJsonObject {
            put(key = "device_code", value = code)
            put(key = "user_code", value = userCode)
            put(key = "verification_uri", value = page)
            put(key = "verification_uri_complete", value = "$page?user_code=$userCode")
            put(key = "expires_in", value = server.deviceLifetime.inWholeSeconds)
            put(key = "interval", value = server.interval.inWholeSeconds)
        }
        return ServerReply(status = OK, body = body.toString())
    }

    fun revoke(request: EndpointRequest): ServerReply {
        val token = request.field("token")
        token?.let { found ->
            refreshTokens -= found
            revoked.store(revoked.load() + found)
        }
        return ServerReply(status = OK, body = "")
    }

    fun approve(request: AuthorizationRequest): AuthorizationResponse? {
        val url = Url(request.url)
        val challenge = url.parameters["code_challenge"]
        val isClient = url.parameters["client_id"] == server.clientId
        counter += 1
        val code = "authorization-code-$counter"
        codes[code] = challenge to request.redirectUri
        return AuthorizationResponse(code = Secret(code), state = request.state).takeIf { isClient }
    }

    private fun granted(request: EndpointRequest, issuedTokenType: String? = null): ServerReply {
        counter += 1
        val access = "access-token-$counter"
        val refresh = "refresh-token-$counter".takeIf { server.isRefreshTokenIssued }
        refresh?.let(refreshTokens::add)
        issued.store(issued.load() + access)
        val body = buildJsonObject {
            put(key = "access_token", value = access)
            put(key = "token_type", value = server.tokenType)
            server.lifetime?.let { lifetime -> put(key = "expires_in", value = lifetime.inWholeSeconds) }
            refresh?.let { token -> put(key = REFRESH_TOKEN, value = token) }
            request.field("scope")?.let { scope -> put(key = "scope", value = scope) }
            issuedTokenType?.let { type -> put(key = "issued_token_type", value = type) }
        }
        return ServerReply(status = OK, body = body.toString())
    }

    private fun refreshed(request: EndpointRequest): ServerReply {
        val token = request.field(REFRESH_TOKEN)
        val isKnown = token != null && refreshTokens.remove(token)
        if (!isKnown) return ServerReply.error(INVALID_GRANT)
        return granted(request)
    }

    private suspend fun exchangedCode(request: EndpointRequest): ServerReply {
        val pending = codes.remove(request.field("code").orEmpty())
        val verifier = request.field("code_verifier").orEmpty()
        val digest = crypto.digests.digest(data = verifier.encodeToByteString(), algorithm = DigestAlgorithm.Sha256)
        val expected = base64Url.encode(digest.toByteArray()) to request.field("redirect_uri")
        if (pending != expected) return ServerReply.error(INVALID_GRANT)
        return granted(request)
    }

    private fun polled(request: EndpointRequest): ServerReply {
        val code = request.field("device_code")
        if (code !in deviceCodes) return ServerReply.error(INVALID_GRANT)
        return answered(code = code, poll = polls.removeFirstOrNull(), request = request)
    }

    private fun answered(code: String?, poll: DevicePoll?, request: EndpointRequest): ServerReply {
        if (poll != null) return ServerReply.error(poll.error)
        deviceCodes -= code
        return granted(request)
    }

    private fun exchanged(request: EndpointRequest): ServerReply {
        val subject = request.field("subject_token").orEmpty()
        val type = request.field("subject_token_type").orEmpty()
        if (subject.isEmpty() || type.isEmpty()) return ServerReply.error("invalid_request")
        val requested = request.field("requested_token_type") ?: TokenTypes.ACCESS_TOKEN
        return granted(request = request, issuedTokenType = requested)
    }
}
