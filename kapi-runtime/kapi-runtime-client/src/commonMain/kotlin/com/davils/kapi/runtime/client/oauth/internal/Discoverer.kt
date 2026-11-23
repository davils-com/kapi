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
import com.davils.kapi.runtime.client.oauth.Discovery
import com.davils.kapi.runtime.client.oauth.OAuthSettings
import com.davils.kapi.runtime.client.oauth.ServerMetadata
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val OK: Int = 200

private const val SCHEME_END: String = "://"

private const val OAUTH_PATH: String = "/.well-known/oauth-authorization-server"

private const val OPENID_PATH: String = "/.well-known/openid-configuration"

internal class Discoverer(private val transport: OAuthHttp, private val settings: OAuthSettings) {
    private val lock: Mutex = Mutex()

    private var cached: ServerMetadata? = null

    suspend fun metadata(): ServerMetadata? {
        if (settings.issuer.isEmpty()) return null
        return lock.withLock {
            val found = cached ?: fetched()
            cached = found
            found
        }
    }

    suspend fun tokenUrl(): String? = endpointOf(settings.tokenUrl) { found -> found.tokenEndpoint }

    suspend fun authorizationUrl(): String? =
        endpointOf(settings.authorizationUrl) { found -> found.authorizationEndpoint }

    suspend fun deviceUrl(): String? =
        endpointOf(settings.deviceAuthorizationUrl) { found -> found.deviceAuthorizationEndpoint }

    suspend fun revocationUrl(): String? = endpointOf(settings.revocationUrl) { found -> found.revocationEndpoint }

    private suspend fun endpointOf(configured: String, published: (ServerMetadata) -> String?): String? =
        configured.ifEmpty { metadata()?.let(published) }

    private suspend fun fetched(): ServerMetadata? {
        val reply = (transport.get(wellKnown()) as? Reply.Answered)?.takeIf { found -> found.status == OK }
        return reply?.let { found -> MetadataResponse.of(found.content) }?.let(::validated)
    }

    private fun validated(response: MetadataResponse): ServerMetadata? {
        val token = response.tokenEndpoint
        val optional = listOfNotNull(
            response.authorizationEndpoint,
            response.deviceAuthorizationEndpoint,
            response.revocationEndpoint,
        )
        val isTrusted = response.issuer == settings.issuer && optional.all(Texts::isProtectedUrl)
        val endpoint = token?.takeIf { url -> isTrusted && Texts.isProtectedUrl(url) } ?: return null
        return ServerMetadata(
            issuer = response.issuer,
            tokenEndpoint = endpoint,
            authorizationEndpoint = response.authorizationEndpoint,
            deviceAuthorizationEndpoint = response.deviceAuthorizationEndpoint,
            revocationEndpoint = response.revocationEndpoint,
            codeChallengeMethods = response.codeChallengeMethods,
        )
    }

    private fun wellKnown(): String = when (settings.discovery) {
        Discovery.OpenIdConnect -> {
            settings.issuer.trimEnd('/') + OPENID_PATH
        }

        Discovery.OAuth -> {
            val scheme = settings.issuer.substringBefore(SCHEME_END)
            val rest = settings.issuer.substringAfter(SCHEME_END)
            val path = rest.substringAfter(delimiter = '/', missingDelimiterValue = "").trimEnd('/')
            val suffix = "/$path".takeIf { path.isNotEmpty() }.orEmpty()
            scheme + SCHEME_END + rest.substringBefore('/') + OAUTH_PATH + suffix
        }
    }
}
