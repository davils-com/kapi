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

import com.davils.kapi.runtime.client.internal.EventSink
import com.davils.kapi.runtime.client.oauth.internal.Answer
import com.davils.kapi.runtime.client.oauth.internal.Discoverer
import com.davils.kapi.runtime.client.oauth.internal.EndpointPost
import com.davils.kapi.runtime.client.oauth.internal.Grants
import com.davils.kapi.runtime.client.oauth.internal.HeldToken
import com.davils.kapi.runtime.client.oauth.internal.OAuthHttp
import com.davils.kapi.runtime.client.oauth.internal.Reply
import com.davils.kapi.runtime.client.oauth.internal.TokenEndpoint
import com.davils.kapi.runtime.core.security.RefreshingTokenSource
import com.davils.kapi.runtime.core.security.Secret
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val OK: Int = 200

private const val INVALID_GRANT: String = "invalid_grant"

private const val TOKEN_TYPE: String = "token type"

private const val REFRESH: String = "refresh_token"

private const val ACCESS: String = "access_token"

/**
 * An OAuth 2 token source of RFC 6749 that a client hands to its credentials: it gets an access token by its grant,
 * keeps it until shortly before it expires, replaces it when the server rejects it and revokes it on request.
 *
 * Every endpoint is `https` (`http` only on this machine) and passes the destination policy; every answer is read up
 * to 64 KiB and only a `bearer` token, or a `DPoP` token when the source binds tokens to a DPoP key, is accepted.
 * Tokens are held as [Secret] and never shown; events name the grant and the reason a token was refused, never a
 * token. The metadata of the issuer, when there is one, are discovered once and kept. Concurrent callers wait for one
 * request; the source is safe to share between coroutines and threads. An interactive grant, the authorization code
 * with PKCE or the device authorization, runs in the coroutine that asks for a token and waits for the user.
 *
 * @since 1.0.0
 */
public class OAuthTokens internal constructor(
    http: HttpClient,
    /**
     * The settings of the source.
     *
     * @since 1.0.0
     */
    public val settings: OAuthSettings,
) : RefreshingTokenSource {
    private val post: EndpointPost = EndpointPost(OAuthHttp(http, settings), settings)

    private val endpoint: TokenEndpoint = TokenEndpoint(post, settings)

    private val discoverer: Discoverer = Discoverer(OAuthHttp(http, settings), settings)

    private val grants: Grants = Grants(settings = settings, endpoint = endpoint, discoverer = discoverer, post = post)

    private val events: EventSink<OAuthEvent> = EventSink(settings.events, OAuthEvent.TOPIC)

    private val mutex: Mutex = Mutex()

    private var held: HeldToken? = null

    private val mutableGrant: MutableStateFlow<Grant> = MutableStateFlow(settings.grant)

    /**
     * The grant the source asks with next: the configured one until the server hands out a refresh token, then the
     * refresh token grant with the latest refresh token.
     *
     * @since 1.0.0
     */
    public val grant: StateFlow<Grant> = mutableGrant.asStateFlow()

    override suspend fun token(): Secret? = mutex.withLock { current()?.access }

    override suspend fun refresh(rejected: Secret?): Boolean = mutex.withLock {
        held = held?.takeIf { found -> found.access != rejected }
        val fresh = current()
        fresh != null && fresh.access != rejected
    }

    /**
     * Returns the metadata of the issuer, discovered on the first call and kept.
     *
     * @return the metadata, or `null` without an issuer or when they could not be read or trusted.
     * @since 1.0.0
     */
    public suspend fun metadata(): ServerMetadata? = discoverer.metadata()

    /**
     * Asks the server to revoke the refresh token of the source, or its access token when it holds none, by RFC 7009,
     * and forgets its tokens when the server agrees.
     *
     * @return how the revocation ended.
     * @since 1.0.0
     */
    public suspend fun revoke(): Revocation = mutex.withLock {
        val revocation = revoked()
        events.emit { OAuthEvent.Revoked(settings.clientId, revocation) }
        revocation
    }

    override fun toString(): String = "OAuthTokens($settings)"

    private suspend fun revoked(): Revocation {
        val refresh = held?.refresh ?: (mutableGrant.value as? Grant.RefreshToken)?.token
        val access = held?.run { access to ACCESS }
        val token = refresh?.let { found -> found to REFRESH } ?: access ?: return Revocation.NothingHeld
        return revokedAt(token)
    }

    private suspend fun revokedAt(token: Pair<Secret, String>): Revocation {
        val url = discoverer.revocationUrl() ?: return Revocation.Unsupported
        val (secret, hint) = token
        val fields = listOf("token" to secret.reveal(), "token_type_hint" to hint)
        val reply = post.post(url = url, fields = fields, isProven = false)
        val isRevoked = (reply as? Reply.Answered)?.status == OK
        if (isRevoked) forget()
        return Revocation.Revoked.takeIf { isRevoked } ?: Revocation.Failed
    }

    private fun forget() {
        held = null
        mutableGrant.value = settings.grant
    }

    private suspend fun current(): HeldToken? = held?.takeIf(HeldToken::isValid) ?: fetched()

    private suspend fun fetched(): HeldToken? {
        val grant = mutableGrant.value
        val token = when (val answer = grants.obtain(grant)) {
            is Answer.Given -> endpoint.held(answer.response) ?: refused(grant, Answer.Refused(TOKEN_TYPE))
            is Answer.Refused -> refused(grant, answer)
        }
        held = token
        token?.let { found -> granted(grant, found) }
        return token
    }

    private fun granted(grant: Grant, token: HeldToken) {
        events.emit { OAuthEvent.Granted(clientId = settings.clientId, grant = grant.name) }
        token.refresh?.let { rotated -> mutableGrant.value = Grant.RefreshToken(rotated) }
    }

    private fun refused(grant: Grant, answer: Answer.Refused): HeldToken? {
        val reason = listOfNotNull(answer.reason, answer.error).joinToString(" ")
        events.emit { OAuthEvent.Refused(clientId = settings.clientId, grant = grant.name, reason = reason) }
        val isExpiredRefresh = grant is Grant.RefreshToken && answer.error == INVALID_GRANT
        if (isExpiredRefresh && settings.grant !is Grant.RefreshToken) mutableGrant.value = settings.grant
        return null
    }
}
