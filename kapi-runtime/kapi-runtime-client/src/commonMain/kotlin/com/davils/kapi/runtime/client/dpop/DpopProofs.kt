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

package com.davils.kapi.runtime.client.dpop

import com.davils.kapi.runtime.client.internal.Texts
import com.davils.kapi.runtime.core.security.Secret
import com.davils.kontinuity.cache.Cache
import com.davils.kontinuity.cache.Eviction
import com.davils.kontinuity.cache.cache
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.krypt.core.algorithm.DigestAlgorithm
import com.davils.krypt.jose.JoseKey
import com.davils.krypt.jose.JsonValue
import com.davils.krypt.jose.jws.JwsAlgorithm
import com.davils.krypt.jose.jwt.Jwt
import com.davils.krypt.jose.jwt.jwtClaims
import com.davils.krypt.primitive.Crypto
import io.ktor.http.parseUrl
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.io.bytestring.encodeToByteString
import kotlin.time.Duration.Companion.minutes

private const val TYPE: String = "dpop+jwt"

private const val JWK: String = "jwk"

private const val METHOD: String = "htm"

private const val TARGET: String = "htu"

private const val ACCESS_TOKEN_HASH: String = "ath"

private const val NONCE: String = "nonce"

private const val ID_BYTES: Int = 16

private const val MOST_NONCES: Int = 64

private const val MOST_NONCE_LENGTH: Int = 1_024

private const val CACHE_NAME: String = "kapi-dpop-nonces"

/**
 * Proves the possession of a private key for every request by RFC 9449, so a token stolen from the application is
 * useless to whoever holds it.
 *
 * A proof is a JWT of the type `dpop+jwt`, signed by the private key with the public key in its header, that names a
 * fresh `jti`, the method, the URL without its query and fragment, the time it was made and, for a request that
 * carries an access token, the hash of the token. A nonce a server hands out with `DPoP-Nonce` is kept per origin, for
 * at most ten minutes and at most for 64 origins, the one used least recently giving way, and goes into every later
 * proof for that origin; a nonce that is no `NQCHAR` text of RFC 9449 §8 is ignored. The proofs are safe to use from
 * many coroutines at once. [toString] names the algorithm, never the key.
 *
 * @since 1.0.0
 */
public class DpopProofs internal constructor(
    private val crypto: Crypto,
    private val key: JoseKey.Private,
    /**
     * The algorithm of the signatures.
     *
     * @since 1.0.0
     */
    public val algorithm: JwsAlgorithm,
    private val publicKey: JsonValue.Object,
) {
    private val jwt: Jwt = Jwt(crypto)

    private val nonces: Cache<String, String> = cache(CACHE_NAME) {
        maximumSize = MOST_NONCES
        eviction = Eviction.Lru
        expireAfterWrite = 10.minutes
    }

    /**
     * Makes the proof of a request.
     *
     * @param method the method of the request, such as `POST`.
     * @param url the absolute URL of the request; its query and fragment are left out.
     * @param accessToken the access token the request carries, whose hash the proof names, or `null`.
     * @return the compact JWT for the field `DPoP`.
     * @since 1.0.0
     */
    public suspend fun proof(method: String, url: String, accessToken: Secret? = null): String {
        val nonce = nonces[originOf(url)]
        val hash = accessToken?.let { token -> hashOf(token) }
        val identifier = Texts.base64Url.encode(crypto.environment.random.bytes(ID_BYTES))
        val claims = jwtClaims {
            jwtId = identifier
            issuedAt = crypto.environment.clock.now()
            claim(METHOD, JsonValue.Text(method))
            claim(TARGET, JsonValue.Text(url.substringBefore('#').substringBefore('?')))
            hash?.let { found -> claim(ACCESS_TOKEN_HASH, JsonValue.Text(found)) }
            nonce?.let { found -> claim(NONCE, JsonValue.Text(found)) }
        }
        val header = JsonValue.Object.of(JWK to publicKey)
        return jwt.sign(claims = claims, key = key, algorithm = algorithm, type = TYPE, header = header)
    }

    /**
     * Keeps [nonce], the value of a `DPoP-Nonce` field of an answer from [url], for the later proofs of its origin.
     * A caller that is cancelled keeps nothing.
     *
     * @param url the absolute URL the answer came from.
     * @param nonce the nonce, or `null` when the answer named none.
     * @since 1.0.0
     */
    public suspend fun acceptNonce(url: String, nonce: String?) {
        currentCoroutineContext().ensureActive()
        val accepted = nonce?.takeIf(::isNonce) ?: return
        nonces.put(key = originOf(url), value = accepted)
    }

    override fun toString(): String = "DpopProofs(${algorithm.algorithmName})"

    private suspend fun hashOf(token: Secret): String {
        val text = token.reveal().encodeToByteString()
        val digest = crypto.digests.digest(data = text, algorithm = DigestAlgorithm.Sha256)
        return Texts.base64Url.encode(digest.toByteArray())
    }

    private fun isNonce(text: String): Boolean =
        text.length in 1..MOST_NONCE_LENGTH && text.all { char -> char in '!'..'~' && char != '"' && char != '\\' }

    private fun originOf(url: String): String {
        val parsed = parseUrl(url) ?: return url.lowercase()
        return "${parsed.protocol.name}://${parsed.host}:${parsed.port}".lowercase()
    }
}
