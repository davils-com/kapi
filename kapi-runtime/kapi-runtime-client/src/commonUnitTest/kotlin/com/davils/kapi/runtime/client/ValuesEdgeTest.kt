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
import com.davils.kapi.runtime.client.call.Timeout
import com.davils.kapi.runtime.client.call.apiCall
import com.davils.kapi.runtime.client.dpop.dpopProofs
import com.davils.kapi.runtime.client.fixture.pets
import com.davils.kapi.runtime.client.internal.Texts
import com.davils.kapi.runtime.client.oauth.fixture.Jwts
import com.davils.kapi.runtime.client.oauth.internal.OAuthJson
import com.davils.kapi.runtime.client.pipeline.RoundTrip
import com.davils.kapi.runtime.client.redirect.RedirectPolicy
import com.davils.kapi.runtime.client.redirect.redirectPolicy
import com.davils.kapi.runtime.client.stream.Reconnection
import com.davils.kapi.runtime.client.stream.StreamEvent
import com.davils.kapi.runtime.client.stream.reconnection
import com.davils.kapi.runtime.core.codec.multipart.Boundary
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kapi.runtime.core.parameter.ParameterFormat
import com.davils.kapi.runtime.core.parameter.ParameterStyle
import com.davils.kapi.runtime.core.parameter.ParameterValue
import com.davils.kapi.runtime.core.wire.FilePart
import com.davils.kore.core.validation.report.ValidationException
import com.davils.krypt.core.algorithm.KeyType
import com.davils.krypt.jose.JoseKey
import com.davils.krypt.jose.jws.JwsAlgorithm
import com.davils.krypt.primitive.Crypto
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.io.bytestring.encodeToByteString
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val CONTROL: String = "a\nb"

private const val LENGTH: Long = 5

private const val MOST_NONCES: Int = 64

private const val URL: String = "https://api.example.com/v1"

private const val NONCE: String = "nonce"

class ValuesEdgeTest : FunSpec({
    context("the values of a client") {
        test("name what is missing and never fail on null parts") {
            val timeout = Timeout(request = 1.seconds, connect = 2.seconds, socket = 3.seconds)

            RoundTrip(pets).toString() shouldBe "RoundTrip($pets, not sent, schemes=[])"
            StreamEvent.Item(null).toString() shouldBe "Item(null, id=none, type=none)"
            StreamEvent.Item("v", id = "1", type = "t").toString() shouldBe "Item(v, id=1, type=t)"
            val socketOnly = Timeout(socket = 1.seconds)
            val upload = apiCall(operation = "up", method = "PUT") {
                upload(contentType = MediaType.OctetStream, chunks = emptyFlow(), length = LENGTH)
            }

            socketOnly.under(timeout) shouldBe Timeout(request = 1.seconds, connect = 2.seconds, socket = 1.seconds)
            Timeout().under(timeout) shouldBe timeout
            timeout.hashCode() shouldNotBe Timeout().hashCode()
            upload.upload?.run { toString() } shouldBe "Upload(application/octet-stream, length=5)"
        }

        test("refuse reconnects, hops and DPoP schemes out of bounds") {
            listOf<() -> Reconnection>(
                { reconnection { maxReconnects = 0 } },
                { reconnection { maxReconnects = Int.MAX_VALUE } },
                { reconnection { delay = (-1).seconds } },
                { reconnection { delay = 2.minutes } },
            ).forEach { build -> shouldThrow<ValidationException> { build() } }
            shouldThrow<ValidationException> { redirectPolicy { maxHops = RedirectPolicy.HARD_HOPS + 1 } }
            val crypto = Crypto()
            val pair = crypto.keys.generate(KeyType.P256)
            val proofs = dpopProofs(
                crypto = crypto,
                key = JoseKey.Private(pair.privateKey),
                publicKey = pair.publicKey,
                algorithm = JwsAlgorithm.ES256,
            )
            val builder = ClientSettingsBuilder(isFollowingRedirects = false)
            builder.dpop(" ", proofs)

            builder.validate().violations.map { violation -> violation.code } shouldBe listOf(
                ClientViolationCodes.DPOP_INVALID,
            )
        }
    }

    context("the parts of a call") {
        test("refuse a cookie of a bad name and every part of a name with control characters") {
            val cookie = apiCall(operation = "c", method = "GET") {
                cookie(
                    ParameterFormat(name = "a b", style = ParameterStyle.Form, isExploded = true),
                    ParameterValue.Text("x"),
                )
            }
            val parts = apiCall(operation = "m", method = "POST") {
                multipart(Boundary("kapi-test-boundary")) {
                    file(name = CONTROL, file = FilePart(content = "x".encodeToByteString(), filename = "x.txt"))
                    part(name = "other\r", contentType = MediaType.Json, content = "{}".encodeToByteString())
                    text(name = "a", value = "\r\n--kapi-test-boundary")
                }
            }

            cookie.refusal shouldBe CallFailure.ParameterRefused("a b")
            parts.refusal shouldBe CallFailure.ParameterRefused(CONTROL)
        }
    }

    context("the texts of the client") {
        test("refuse upper case words, invisible characters, empty hosts and backslashes") {
            listOf("aB", "a b").forEach { name -> Texts.isLowerWords(name) shouldBe false }
            listOf(
                "https://a b.example.com",
                "https://a.example.com/é",
                "https://a.example.com/x\\y",
                "http:///x",
                "file:///x",
            ).map(Texts::isPlainUrl) shouldBe List(size = 5) { false }
            listOf("""{"error":"bad\\\\x"}""", """{"error":"é"}""", """{"error":"a\u0001"}""").map { text ->
                OAuthJson.errorOf(text)
            } shouldBe listOf(null, null, null)
        }
    }

    context("the nonces of DPoP proofs") {
        test("refuse what is empty, too long or invisible, and keep the nonce of a known origin when full") {
            val crypto = Crypto()
            val pair = crypto.keys.generate(KeyType.P256)
            val proofs = dpopProofs(
                crypto = crypto,
                key = JoseKey.Private(pair.privateKey),
                publicKey = pair.publicKey,
                algorithm = JwsAlgorithm.ES256,
            )
            listOf("", "x".repeat(MOST_NONCES * MOST_NONCES), "a b", "aé", "a\\b").forEach { nonce ->
                proofs.acceptNonce(url = URL, nonce = nonce)
            }
            Jwts.text(Jwts.claims(proofs.proof(method = "GET", url = URL)), NONCE) shouldBe null
            repeat(MOST_NONCES) { index -> proofs.acceptNonce(url = "https://h$index.example.com", nonce = "n$index") }
            proofs.acceptNonce(url = "https://h0.example.com", nonce = "again")

            Jwts.text(Jwts.claims(proofs.proof(method = "GET", url = "https://h1.example.com")), NONCE) shouldBe "n1"
            Jwts.text(Jwts.claims(proofs.proof(method = "GET", url = "https://h0.example.com")), NONCE) shouldBe "again"
        }
    }
})
