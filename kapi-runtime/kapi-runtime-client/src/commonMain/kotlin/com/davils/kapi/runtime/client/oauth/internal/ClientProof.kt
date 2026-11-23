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

import com.davils.kapi.runtime.client.oauth.ClientAuthentication
import com.davils.kapi.runtime.client.oauth.OAuthSettings
import com.davils.kapi.runtime.core.security.Secret
import com.davils.kore.core.failure.attempted
import com.davils.krypt.core.SecretBytes
import com.davils.krypt.jose.JoseKey
import com.davils.krypt.jose.jws.JwsAlgorithm
import com.davils.krypt.jose.jwt.Jwt
import com.davils.krypt.jose.jwt.jwtClaims
import com.davils.krypt.primitive.key.SecretKey
import io.ktor.http.encodeURLParameter
import kotlin.io.encoding.Base64
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

private const val CLIENT_ID: String = "client_id"

private const val CLIENT_SECRET: String = "client_secret"

private const val ASSERTION_TYPE: String = "client_assertion_type"

private const val ASSERTION: String = "client_assertion"

private const val JWT_BEARER: String = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer"

private const val AUTHORIZATION: String = "Authorization"

internal class ClientProof(private val settings: OAuthSettings) {
    private val jwt: Jwt = Jwt(settings.crypto)

    private val clock: Clock = settings.crypto.environment.clock

    suspend fun of(audience: String): Proof? = when (val chosen = settings.authentication) {
        ClientAuthentication.None -> {
            Proof(fields = listOf(CLIENT_ID to settings.clientId))
        }

        is ClientAuthentication.SecretBasic -> {
            Proof(headers = listOf(AUTHORIZATION to basic(chosen.secret)))
        }

        is ClientAuthentication.SecretPost -> {
            Proof(fields = listOf(CLIENT_ID to settings.clientId, CLIENT_SECRET to chosen.secret.reveal()))
        }

        is ClientAuthentication.SecretJwt -> {
            asserted(algorithm = chosen.algorithm, audience = audience) {
                val material = SecretBytes.copyOf(chosen.secret.reveal().encodeToByteArray())
                JoseKey.Secret(SecretKey.of(material))
            }
        }

        is ClientAuthentication.PrivateKeyJwt -> {
            asserted(algorithm = chosen.algorithm, audience = audience) { chosen.key }
        }
    }

    private suspend fun asserted(algorithm: JwsAlgorithm, audience: String, key: () -> JoseKey): Proof? {
        val signed = attempted {
            val now = clock.now()
            val identifier = OAuthJson.random(settings.crypto)
            val claims = jwtClaims {
                issuer = settings.clientId
                tokenSubject = settings.clientId
                this.audience = listOf(audience)
                jwtId = identifier
                issuedAt = now
                expiresAt = now + 1.minutes
            }
            jwt.sign(claims = claims, key = key(), algorithm = algorithm)
        }
        val assertion = signed.getOrNull() ?: return null
        val fields = listOf(CLIENT_ID to settings.clientId, ASSERTION_TYPE to JWT_BEARER, ASSERTION to assertion)
        return Proof(fields = fields)
    }

    private fun basic(secret: Secret): String {
        val id = settings.clientId.encodeURLParameter(spaceToPlus = true)
        val pair = id + ":" + secret.reveal().encodeURLParameter(spaceToPlus = true)
        return "Basic " + Base64.Default.encode(pair.encodeToByteArray())
    }
}
