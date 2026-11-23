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

package com.davils.kapi.runtime.server.webhook

import com.davils.kapi.runtime.server.fixture.PROBE
import com.davils.kapi.runtime.server.fixture.crypto
import com.davils.kapi.runtime.server.fixture.problemOf
import com.davils.kapi.runtime.server.fixture.requestOf
import com.davils.kapi.runtime.server.fixture.serve
import com.davils.kapi.runtime.server.operation
import com.davils.kapi.runtime.server.operation.CredentialSource
import com.davils.kapi.runtime.server.operation.Operation
import com.davils.kapi.runtime.server.operation.SchemeUse
import com.davils.kapi.runtime.server.problem.ServerCodes
import com.davils.kapi.runtime.server.reply.reply
import com.davils.kapi.runtime.server.security.ApiKeyAuthorizer
import com.davils.kore.core.principal.principal
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.server.testing.ApplicationTestBuilder
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.decodeToString
import kotlinx.io.bytestring.encodeToByteString

private const val TINY: Long = 4L

private const val ACCEPTING: String = "accepting"

private const val OK: Int = 200

private const val UNAUTHORIZED: Int = 401

private const val TOO_LARGE: Int = 413

private const val BODY: String = """{"hello": "world"}"""

private const val SHA_256: String = "sha-256=:X48E9qOokqqrvdts8nOJRJN3OWDUoyWxBf7kbu9DBPE=:"

private const val SHA_512: String =
    "sha-512=:WZDPaVn/7XgHaAy8pmojAkGWoRx2UFChF41A2svX+TaPm+AbwAgBWnrIiYllu7BNNyealdVLvRwEmTHWXvJwew==:"

private const val DIGEST: String = "Content-Digest"

private const val KEY_HEADER: String = "X-Key"

private val digests: ContentDigestVerifier = ContentDigestVerifier(crypto)

private fun hook(security: List<List<SchemeUse>> = emptyList(), seen: MutableList<String> = mutableListOf()) =
    Operation(name = "petAdopted", method = "POST", path = PROBE, security = security, isDelivery = true) { request ->
        val bytes = request.input().bytes(isRequired = true)
        seen += bytes?.decodeToString().orEmpty()
        reply(OK)
    }

private suspend fun ApplicationTestBuilder.deliver(vararg headers: Pair<String, String>): HttpResponse =
    client.post(PROBE) {
        header(key = "Content-Type", value = "application/json")
        headers.forEach { (name, value) -> header(name, value) }
        setBody(BODY)
    }

private fun digested(vararg values: String) = requestOf(headers = mapOf("content-digest" to values.toList()))

class DeliveryTest : FunSpec({
    context("the digest of a delivery") {
        test("matches the content by SHA-256 or SHA-512, the strongest first") {
            val content = BODY.encodeToByteString()

            digests.verify(digested(SHA_256), content) shouldBe Verdict.Accepted
            digests.verify(digested(SHA_512), content) shouldBe Verdict.Accepted
            digests.verify(digested("$SHA_256, $SHA_512"), content) shouldBe Verdict.Accepted
            digests.toString() shouldBe "ContentDigestVerifier"
        }

        test("is refused when it is missing, malformed, only of deprecated algorithms or differs") {
            val content = BODY.encodeToByteString()

            digests.verify(digested(), content) shouldBe Verdict.Refused(MISSING)
            digests.verify(digested("sha-256=x"), content) shouldBe Verdict.Refused(MISSING)
            digests.verify(digested("md5=:AAAA:"), content) shouldBe Verdict.Refused(MISSING)
            digests.verify(digested(SHA_256), "{}".encodeToByteString()) shouldBe Verdict.Refused(MISMATCH)
        }
    }

    context("a delivery") {
        test("is served when the verifier accepts its bytes, which the handler reads again") {
            val seen = mutableListOf<String>()
            serve(settings = { verifier = digests }, routes = { operation(hook(seen = seen)) }) {
                deliver(DIGEST to SHA_256).status.value shouldBe OK
            }

            seen shouldBe listOf(BODY)
        }

        test("is refused as unverified when the verifier refuses it or fails, and never reaches its handler") {
            val seen = mutableListOf<String>()
            val failing = DeliveryVerifier { _, _ -> error("broken") }
            serve(settings = { verifier = digests }, routes = { operation(hook(seen = seen)) }) {
                val response = deliver(DIGEST to "sha-256=:AAAA:")

                response.status.value shouldBe UNAUTHORIZED
                problemOf(response).code shouldBe ServerCodes.UNVERIFIED
            }
            serve(settings = { verifier = failing }, routes = { operation(hook(seen = seen)) }) {
                deliver().status.value shouldBe UNAUTHORIZED
            }

            seen.shouldBeEmpty()
        }

        test("that declares no security is refused until the server has a verifier") {
            serve(routes = { operation(hook()) }) {
                problemOf(deliver(DIGEST to SHA_256)).code shouldBe ServerCodes.UNVERIFIED
            }
        }

        test("that declares security is authorized like any operation, and verified as well when there is a verifier") {
            val keyed = listOf(listOf(SchemeUse("key", CredentialSource.Header(KEY_HEADER))))
            val keys = ApiKeyAuthorizer { key -> principal("partner").takeIf { key.reveal() == "k" } }
            serve(settings = { authorizer("key", keys) }, routes = { operation(hook(keyed)) }) {
                deliver(KEY_HEADER to "k").status.value shouldBe OK
                deliver().status.value shouldBe UNAUTHORIZED
            }
            serve(
                settings = {
                    authorizer("key", keys)
                    verifier = digests
                },
                routes = { operation(hook(keyed)) },
            ) {
                deliver(KEY_HEADER to "k", DIGEST to SHA_256).status.value shouldBe OK
                deliver(KEY_HEADER to "k").status.value shouldBe UNAUTHORIZED
                deliver(DIGEST to SHA_256).status.value shouldBe UNAUTHORIZED
            }
        }

        test("larger than the server reads is refused before it is verified") {
            serve(
                settings = {
                    limits { maxRequestBytes = TINY }
                    verifier = digests
                },
                routes = { operation(hook()) },
            ) {
                deliver(DIGEST to SHA_256).status.value shouldBe TOO_LARGE
            }
        }
    }

    context("verifiers combined") {
        test("accept only what every one of them accepts, asked in order") {
            val asked = mutableListOf<String>()
            val refusing = DeliveryVerifier { _, _ ->
                asked += "refusing"
                Verdict.Refused("no")
            }
            val accepting = DeliveryVerifier { _, _ ->
                asked += ACCEPTING
                Verdict.Accepted
            }

            DeliveryVerifier.allOf(accepting, accepting).verify(requestOf(), ByteString()) shouldBe Verdict.Accepted
            DeliveryVerifier.allOf(refusing, accepting).verify(requestOf(), ByteString()) shouldBe Verdict.Refused("no")
            asked shouldBe listOf(ACCEPTING, ACCEPTING, "refusing")
            shouldThrow<IllegalArgumentException> { DeliveryVerifier.allOf() }
        }

        test("show their verdicts") {
            Verdict.Accepted.toString() shouldBe "Accepted"
            Verdict.Refused("no").toString() shouldBe "Refused(no)"
            Verdict.Refused("no").hashCode() shouldBe "no".hashCode()
        }
    }
})

private const val MISSING: String = "The delivery carries no Content-Digest of an active algorithm"

private const val MISMATCH: String = "The Content-Digest does not match the content"
