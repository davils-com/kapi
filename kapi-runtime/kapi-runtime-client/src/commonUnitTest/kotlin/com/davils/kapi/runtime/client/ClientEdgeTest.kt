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

package com.davils.kapi.runtime.client

import com.davils.kapi.runtime.client.call.CallFailure
import com.davils.kapi.runtime.client.call.CredentialPlacement
import com.davils.kapi.runtime.client.call.Exchange
import com.davils.kapi.runtime.client.call.SecurityUse
import com.davils.kapi.runtime.client.call.apiCall
import com.davils.kapi.runtime.client.dpop.dpopProofs
import com.davils.kapi.runtime.client.fixture.Exchanges
import com.davils.kapi.runtime.client.fixture.pets
import com.davils.kapi.runtime.client.fixture.unauthorized
import com.davils.kapi.runtime.client.pipeline.CallPhase
import com.davils.kapi.runtime.client.pipeline.RoundTrip
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kapi.runtime.core.security.Secret
import com.davils.kapi.runtime.core.security.credentials
import com.davils.krypt.core.algorithm.KeyType
import com.davils.krypt.jose.JoseKey
import com.davils.krypt.jose.jws.JwsAlgorithm
import com.davils.krypt.primitive.Crypto
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeoutCapability
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.flowOf
import kotlinx.io.bytestring.encodeToByteString
import kotlin.time.Duration.Companion.seconds

private const val SCHEME: String = "oauth"

private const val UNAUTHORIZED: Int = 401

private const val SOCKET: Long = 3_000

private val secured: List<SecurityUse> = listOf(SecurityUse(SCHEME, CredentialPlacement.Bearer))

class ClientEdgeTest : FunSpec({
    context("the time a connection or a read may take") {
        test("is handed to the engine when the call names only one of them") {
            val exchanges = Exchanges()

            exchanges.execute(pets.change { timeout(socket = 3.seconds) })
            exchanges.execute(pets.change { timeout(connect = 3.seconds) })
            val limits = exchanges.requests.map { request -> request.getCapabilityOrNull(HttpTimeoutCapability) }

            limits.map { found -> found?.socketTimeoutMillis } shouldBe listOf(SOCKET, null)
            limits.map { found -> found?.connectTimeoutMillis } shouldBe listOf(null, SOCKET)
        }
    }

    context("a call that fails") {
        test("is unreachable by a failure without a name") {
            val exchanges = Exchanges { throw object : IllegalStateException() {} }

            val failed = exchanges.execute(pets).shouldBeInstanceOf<Exchange.Failed>()

            failed.failure.shouldBeInstanceOf<CallFailure.Unreachable>()
        }

        test("is never sent again after 401 when its upload cannot be replayed") {
            val upload = apiCall(operation = "upload", method = "PUT") {
                security(secured)
                upload(MediaType.OctetStream, flowOf("x".encodeToByteString()))
            }
            val exchanges = Exchanges(configure = {
                credentials = credentials { scheme(SCHEME) { Secret("t1") } }
            }) { unauthorized() }

            exchanges.execute(upload).shouldBeInstanceOf<Exchange.Answered>().status shouldBe UNAUTHORIZED
            exchanges.requests.size shouldBe 1
        }

        test("is not sent again for a DPoP challenge without a nonce") {
            val crypto = Crypto()
            val pair = crypto.keys.generate(KeyType.P256)
            val proofs = dpopProofs(
                crypto = crypto,
                key = JoseKey.Private(pair.privateKey),
                publicKey = pair.publicKey,
                algorithm = JwsAlgorithm.ES256,
            )
            val challenge = headersOf(name = HttpHeaders.WWWAuthenticate, value = """DPoP error="use_dpop_nonce"""")
            val exchanges = Exchanges(configure = {
                credentials = credentials { scheme(SCHEME) { Secret("t1") } }
                dpop(SCHEME, proofs)
            }) { respond(content = "", status = HttpStatusCode.Unauthorized, headers = challenge) }

            exchanges.execute(pets.change { security(secured) }).shouldBeInstanceOf<Exchange.Answered>()
            exchanges.requests.size shouldBe 1
        }

        test("is missing credentials when an interceptor hands back a trip that was never sent") {
            var stashed: RoundTrip? = null
            val exchanges = Exchanges(configure = {
                intercept(CallPhase.Prepare, "stash") { trip ->
                    stashed = trip
                    trip
                }
                intercept(CallPhase.Receive, "rewind") { trip -> stashed ?: trip }
            })

            exchanges.execute(pets) shouldBe Exchange.Failed(CallFailure.CredentialMissing)
        }
    }
})
