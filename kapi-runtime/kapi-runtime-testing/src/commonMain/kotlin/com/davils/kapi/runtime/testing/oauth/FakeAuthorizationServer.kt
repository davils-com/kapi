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

@file:OptIn(ExperimentalAtomicApi::class, DavilsInternal::class)

package com.davils.kapi.runtime.testing.oauth

import com.davils.kapi.runtime.client.oauth.AuthorizationRedirect
import com.davils.kapi.runtime.client.oauth.ClientAuthentication
import com.davils.kapi.runtime.client.oauth.Grant
import com.davils.kapi.runtime.client.oauth.OAuthSettingsBuilder
import com.davils.kapi.runtime.client.oauth.OAuthTokens
import com.davils.kapi.runtime.client.oauth.oauthTokens
import com.davils.kapi.runtime.core.security.Secret
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.failure.contained
import com.davils.kore.core.text.SafeText
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import com.davils.krypt.primitive.Crypto
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.decodeURLQueryComponent
import io.ktor.http.headersOf
import io.ktor.http.parseQueryString
import io.ktor.util.flattenEntries
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.io.encoding.Base64
import kotlin.time.Duration

private const val UNAUTHORIZED: Int = 401

private const val NOT_FOUND: Int = 404

private const val OK: Int = 200

private const val SCHEME_END: String = "://"

private const val BASIC: String = "Basic "

private const val HTTPS: String = "https://"

private const val FORBIDDEN: String = "?# "

private val authenticated: Set<Endpoint> = setOf(Endpoint.Token, Endpoint.DeviceAuthorization, Endpoint.Revocation)

/**
 * A fake OAuth 2 authorization server on a Ktor [MockEngine], for the grants of the client runtime: client
 * credentials, refresh tokens, the authorization code with PKCE through [redirect], the device authorization grant
 * of RFC 8628 with scripted `authorization_pending` and `slow_down` answers, and the token exchange of RFC 8693, as
 * well as revocation and the metadata of RFC 8414 and OpenID Connect Discovery.
 *
 * The server issues the tokens `access-token-1` with `refresh-token-1` and so on, counting every grant, code and
 * device code it creates, so that a test can name them. It authenticates the client by HTTP Basic or by the form
 * and answers every other client with `invalid_client`. A refresh token works once: refreshing it issues a new one. An
 * authorization code works once and only with the PKCE verifier and the redirect URI it was issued for. A device
 * code works until it is granted. Unless [AuthorizationServerBuilder.refuse] scripts otherwise, a request of a known
 * grant with valid fields is granted. The server checks no signature: a client assertion is accepted only for a
 * client without a secret. It is safe to call from several coroutines at once; its requests are handled one at a time.
 * It is a test double and never belongs on a production class path.
 *
 * @since 1.0.0
 */
public class FakeAuthorizationServer internal constructor(builder: AuthorizationServerBuilder) : Validatable {
    /**
     * The issuer.
     *
     * @since 1.0.0
     */
    public val issuer: String = builder.issuer

    /**
     * The client the server knows.
     *
     * @since 1.0.0
     */
    public val clientId: String = builder.clientId

    private val clientSecret: String? = builder.clientSecret

    internal val tokenType: String = builder.tokenType

    internal val lifetime: Duration? = builder.lifetime

    internal val isRefreshTokenIssued: Boolean = builder.isRefreshTokenIssued

    internal val acceptedRefreshTokens: Set<String> = builder.refreshTokens.toSet()

    internal val interval: Duration = builder.interval

    internal val deviceLifetime: Duration = builder.deviceLifetime

    internal val devicePolls: List<DevicePoll> = builder.devicePolls.toList()

    internal val refusals: List<Pair<String, String>> = builder.refusals

    private val lock: Mutex = Mutex()

    private val state: ServerState = ServerState(server = this, crypto = Crypto())

    private val recorded: AtomicReference<List<EndpointRequest>> = AtomicReference(emptyList())

    /**
     * The token endpoint: `<issuer>/oauth/token`.
     *
     * @since 1.0.0
     */
    public val tokenUrl: String get() = "$issuer/oauth/token"

    /**
     * The authorization endpoint: `<issuer>/oauth/authorize`, which only [redirect] visits.
     *
     * @since 1.0.0
     */
    public val authorizationUrl: String get() = "$issuer/oauth/authorize"

    /**
     * The device authorization endpoint: `<issuer>/oauth/device`.
     *
     * @since 1.0.0
     */
    public val deviceAuthorizationUrl: String get() = "$issuer/oauth/device"

    /**
     * The revocation endpoint: `<issuer>/oauth/revoke`.
     *
     * @since 1.0.0
     */
    public val revocationUrl: String get() = "$issuer/oauth/revoke"

    /**
     * The engine that answers; hand it to an [HttpClient] of its own or use [httpClient].
     *
     * @since 1.0.0
     */
    public val engine: MockEngine = MockEngine { data ->
        val request = requestOf(data)
        val reply = lock.withLock {
            recorded.store(recorded.load() + request)
            replyTo(request)
        }
        respond(
            content = reply.body,
            status = HttpStatusCode.fromValue(reply.status),
            headers = headersOf(name = HttpHeaders.ContentType, value = "application/json"),
        )
    }

    /**
     * Approves every authorization request of the known client at once, as a user who consents would, and returns
     * the code; a request of another client is not approved.
     *
     * @since 1.0.0
     */
    public val redirect: AuthorizationRedirect = AuthorizationRedirect { request ->
        lock.withLock { state.approve(request) }
    }

    /**
     * Every request so far, in order.
     *
     * @since 1.0.0
     */
    public val requests: List<EndpointRequest> get() = recorded.load()

    /**
     * Every access token issued so far, in order.
     *
     * @since 1.0.0
     */
    public val issuedTokens: List<String> get() = state.issued.load()

    /**
     * Every token revoked so far, in order.
     *
     * @since 1.0.0
     */
    public val revokedTokens: List<String> get() = state.revoked.load()

    /**
     * Returns an [HttpClient] on [engine] that follows no redirects, as the client runtime of kapi requires.
     *
     * @param configure further configuration of the client.
     * @return the client, which the caller closes.
     * @since 1.0.0
     */
    public fun httpClient(configure: HttpClientConfig<*>.() -> Unit = {}): HttpClient = HttpClient(engine) {
        followRedirects = false
        configure()
    }

    /**
     * Returns OAuth tokens of the client runtime that use this server: its endpoints, its client and, if it has a
     * secret, HTTP Basic.
     *
     * @param http the client to reach the server with, normally [httpClient].
     * @param grant the grant; the client credentials grant by default.
     * @param configure further settings, applied last.
     * @return the tokens.
     * @throws com.davils.kore.core.validation.report.ValidationException if the settings are not valid.
     * @since 1.0.0
     */
    public fun tokens(
        http: HttpClient,
        grant: Grant = Grant.ClientCredentials,
        configure: OAuthSettingsBuilder.() -> Unit = {},
    ): OAuthTokens = oauthTokens(http) {
        tokenUrl = this@FakeAuthorizationServer.tokenUrl
        authorizationUrl = this@FakeAuthorizationServer.authorizationUrl
        deviceAuthorizationUrl = this@FakeAuthorizationServer.deviceAuthorizationUrl
        revocationUrl = this@FakeAuthorizationServer.revocationUrl
        clientId = this@FakeAuthorizationServer.clientId
        authentication = clientSecret?.let { secret -> ClientAuthentication.SecretBasic(Secret(secret)) }
            ?: ClientAuthentication.None
        this.grant = grant
        configure()
    }

    /**
     * Returns the requests that reached [endpoint].
     *
     * @param endpoint the endpoint.
     * @return the requests, in order.
     * @since 1.0.0
     */
    public fun requestsTo(endpoint: Endpoint): List<EndpointRequest> =
        requests.filter { request -> request.endpoint == endpoint }

    override fun ValidationScope.validate() {
        val rest = issuer.removePrefix(HTTPS)
        val isShaped = rest.isNotEmpty() && !issuer.endsWith('/') && rest.none(FORBIDDEN::contains)
        val isIssuer = issuer.startsWith(HTTPS) && isShaped
        if (!isIssuer) fail(::issuer, "is no https URL without query, fragment and a slash at its end")
        if (clientId.isBlank()) fail(::clientId, "is blank")
        val isTiming = !interval.isNegative() && deviceLifetime.isPositive() && lifetime?.isPositive() != false
        if (!isTiming) fail("has a lifetime that is not above zero or an interval below zero")
    }

    override fun toString(): String = "FakeAuthorizationServer(${SafeText.text(issuer)}, " +
        "clientId=${SafeText.text(clientId)}, requests=${requests.size})"

    private suspend fun requestOf(data: HttpRequestData): EndpointRequest {
        val text = data.body.toByteArray().decodeToString()
        val fields = parseQueryString(text).flattenEntries()
        val basic = basicOf(data.headers[HttpHeaders.Authorization])
        val id = basic?.first ?: fields.firstOrNull { (name, _) -> name == "client_id" }?.second
        val secret = basic?.second ?: fields.firstOrNull { (name, _) -> name == "client_secret" }?.second
        val isSecret = clientSecret == null || Secret(secret.orEmpty()) == Secret(clientSecret)
        return EndpointRequest(
            endpoint = endpointOf(data.url.toString().substringBefore('?')),
            url = data.url.toString(),
            fields = fields,
            clientId = id,
            isAuthenticated = id == clientId && isSecret,
        )
    }

    private suspend fun replyTo(request: EndpointRequest): ServerReply {
        val isRefused = request.endpoint in authenticated && !request.isAuthenticated
        if (isRefused) return ServerReply.error(code = "invalid_client", status = UNAUTHORIZED)
        return when (request.endpoint) {
            Endpoint.Token -> state.token(request)
            Endpoint.DeviceAuthorization -> state.device()
            Endpoint.Revocation -> state.revoke(request)
            Endpoint.Metadata -> ServerReply(status = OK, body = metadata())
            Endpoint.Unknown -> ServerReply.error(code = "not_found", status = NOT_FOUND)
        }
    }

    private fun endpointOf(url: String): Endpoint {
        val endpoints = mapOf(
            tokenUrl to Endpoint.Token,
            deviceAuthorizationUrl to Endpoint.DeviceAuthorization,
            revocationUrl to Endpoint.Revocation,
            oauthMetadataUrl() to Endpoint.Metadata,
            "$issuer/.well-known/openid-configuration" to Endpoint.Metadata,
        )
        return endpoints[url] ?: Endpoint.Unknown
    }

    private fun oauthMetadataUrl(): String {
        val rest = issuer.substringAfter(SCHEME_END)
        val path = rest.substringAfter(delimiter = '/', missingDelimiterValue = "")
        val suffix = "/$path".takeIf { path.isNotEmpty() }.orEmpty()
        return issuer.substringBefore(SCHEME_END) +
            SCHEME_END +
            rest.substringBefore('/') +
            "/.well-known/oauth-authorization-server" +
            suffix
    }

    private fun metadata(): String {
        val endpoints = listOf(
            "token_endpoint" to tokenUrl,
            "authorization_endpoint" to authorizationUrl,
            "device_authorization_endpoint" to deviceAuthorizationUrl,
            "revocation_endpoint" to revocationUrl,
        )
        val document = buildJsonObject {
            put(key = "issuer", value = issuer)
            endpoints.forEach { (name, url) -> put(key = name, value = url) }
            putJsonArray("code_challenge_methods_supported") { add("S256") }
        }
        return document.toString()
    }

    private fun basicOf(authorization: String?): Pair<String, String>? {
        val isBasic = authorization?.startsWith(BASIC, ignoreCase = true) == true
        val encoded = authorization?.takeIf { isBasic }?.substring(startIndex = BASIC.length)
        val bytes = encoded?.let { text -> contained { Base64.Default.decode(text.trim()) }.getOrNull() }
        val text = bytes?.decodeToString()
        val pair = text?.takeIf { found -> ':' in found } ?: return null
        val id = decoded(pair.substringBefore(':'))
        return id to decoded(pair.substringAfter(':'))
    }

    private fun decoded(text: String): String =
        contained { text.decodeURLQueryComponent(plusIsSpace = true) }.getOrNull() ?: text
}
