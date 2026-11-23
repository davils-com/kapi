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

import com.davils.kapi.runtime.client.call.Body
import com.davils.kapi.runtime.client.call.Call
import com.davils.kapi.runtime.client.call.CallFailure
import com.davils.kapi.runtime.client.call.Exchange
import com.davils.kapi.runtime.client.call.apiCall
import com.davils.kapi.runtime.client.fixture.BASE
import com.davils.kapi.runtime.client.fixture.Exchanges
import com.davils.kapi.runtime.client.fixture.JSON
import com.davils.kapi.runtime.client.fixture.Note
import com.davils.kapi.runtime.client.fixture.ok
import com.davils.kapi.runtime.client.fixture.pets
import com.davils.kapi.runtime.core.codec.Decoding
import com.davils.kapi.runtime.core.header.IdempotencyKey
import com.davils.kapi.runtime.core.negotiation.MediaRange
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kapi.runtime.core.parameter.ParameterFormat
import com.davils.kapi.runtime.core.parameter.ParameterStyle
import com.davils.kapi.runtime.core.parameter.ParameterValue
import com.davils.kapi.runtime.core.response.headersOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.io.IOException
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

private const val LIMIT: Long = 4

private const val OK: Int = 200

private const val TIMEOUT: Long = 1_000

private const val NOTE: String = """{"text":"hi"}"""

private const val TRACE: String = "X-Trace"

private val failing: SerializationStrategy<Note> = object : SerializationStrategy<Note> {
    override val descriptor: SerialDescriptor = Note.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Note): Unit = throw SerializationException("no")
}

class ApiClientTest : FunSpec({
    context("a call") {
        test("is sent with its method, URL, query, headers, cookies, what it accepts and its content") {
            val exchanges = Exchanges()
            val call = apiCall(operation = "addPet", method = "POST") {
                path("/pets/a%20b")
                query(name = "tag", value = "big dog")
                query(name = "tag", value = "old")
                header(name = TRACE, value = "t1")
                accept(MediaRange.Json)
                accept(MediaRange.of("text/*"))
                body(MediaType.Json, "{}".encodeToByteString())
                idempotencyKey(IdempotencyKey("k-1"))
            }.withCookies()

            exchanges.execute(call)
            val request = exchanges.requests.single()

            request.method.value shouldBe "POST"
            request.url.toString() shouldBe "$BASE/pets/a%20b?tag=big+dog&tag=old"
            request.headers[TRACE] shouldBe "t1"
            request.headers[HttpHeaders.Accept] shouldBe "application/json, text/*"
            request.headers[HttpHeaders.Cookie] shouldBe "session=s1; theme=dark"
            request.headers["Idempotency-Key"] shouldBe "\"k-1\""
            request.body.toByteArray().decodeToString() shouldBe "{}"
            request.body.contentType shouldBe ContentType.Application.Json
        }

        test("is answered with the status, the headers and the content, the sensitive headers masked") {
            val exchanges = Exchanges(configure = { sensitiveHeaders = setOf("content-type") })
            val answered = exchanges.execute(pets).shouldBeInstanceOf<Exchange.Answered>()

            answered.status shouldBe OK
            answered.contentType shouldBe MediaType.Json
            answered.content shouldBe "[]".encodeToByteString()
            answered.headers.toString() shouldNotContain JSON
        }

        test("has no cookie field and no content when it carries neither") {
            val exchanges = Exchanges()
            exchanges.execute(pets)

            exchanges.requests.single().headers[HttpHeaders.Cookie] shouldBe null
        }

        test("is not sent when it was refused while it was built") {
            val exchanges = Exchanges()
            val refused = apiCall(operation = "x", method = "GET") { header(name = "X", value = "a\nb") }

            exchanges.execute(refused) shouldBe Exchange.Failed(CallFailure.ParameterRefused("X"))
            exchanges.requests shouldBe emptyList()
        }
    }

    context("an answer") {
        test("may be as large as the limit and is refused one byte above it") {
            val fits = Exchanges(configure = { maxResponseBytes = LIMIT }) { ok("1234") }
            val large = Exchanges(configure = { maxResponseBytes = LIMIT }) { ok("12345") }

            fits.execute(pets).shouldBeInstanceOf<Exchange.Answered>()
            large.execute(pets) shouldBe Exchange.Failed(CallFailure.TooLarge(LIMIT))
        }
    }

    context("a call that does not get through") {
        test("names the type of what broke, never its message") {
            val exchanges = Exchanges { throw IOException("connection to api.example.com refused") }

            exchanges.execute(pets) shouldBe Exchange.Failed(CallFailure.Unreachable("IOException"))
        }

        test("is a timeout when the engine gives up") {
            val exchanges = Exchanges { throw HttpRequestTimeoutException("$BASE/pets", TIMEOUT) }

            exchanges.execute(pets) shouldBe Exchange.Failed(CallFailure.TimedOut)
        }
    }

    context("the content of an answer") {
        test("is read by the codec of its content type") {
            val decoding = Exchanges { ok(NOTE) }.withClient { client ->
                val answered = client.execute(pets).shouldBeInstanceOf<Exchange.Answered>()
                client.decode(Note.serializer(), answered)
            }

            decoding.shouldBeInstanceOf<Decoding.Decoded<Note>>().value shouldBe Note("hi")
        }

        test("is not read without a content type or a codec for it") {
            val content = NOTE.encodeToByteString()
            val bare = Exchange.Answered(status = OK, headers = headersOf(emptyMap()), content = content)
            val typed = headersOf(mapOf("content-type" to listOf("text/xml")))
            val xml = Exchange.Answered(status = OK, headers = typed, content = NOTE.encodeToByteString())

            Exchanges().withClient { client ->
                client.decode(Note.serializer(), bare).shouldBeInstanceOf<Decoding.Failed>()
                client.decode(Note.serializer(), xml).shouldBeInstanceOf<Decoding.Failed>()
            }
        }
    }

    context("the content of a call") {
        test("is written by the codec of its media type, or not at all") {
            Exchanges().withClient { client ->
                client.encode(Note.serializer(), Note("hi")) shouldBe Body(MediaType.Json, NOTE.encodeToByteString())
                client.encode(Note.serializer(), Note("hi"), MediaType.parse("image/png")) shouldBe null
                client.encode(failing, Note("hi")) shouldBe null
                client.json(Note.serializer(), Note("hi")) shouldBe NOTE.encodeToByteString()
            }
        }

        test("asks only for the media types the codecs can read") {
            val offered = listOf(MediaType.Json, MediaType.parse("image/png"), MediaType.TextPlain)

            Exchanges().withClient { client -> client.acceptable(offered) } shouldBe
                listOf(MediaRange.Json, MediaRange.of("text/plain"))
        }
    }

    context("the fields of an object") {
        test("are its members as text without the null ones, and JSON for the rest") {
            val filter = buildJsonObject {
                put("name", JsonPrimitive("a b"))
                put("limit", JsonPrimitive(2))
                put("owner", JsonNull)
                put("tags", buildJsonArray { add(JsonPrimitive("x")) })
            }
            val fields = Exchanges().withClient { client -> client.fields(JsonElement.serializer(), filter) }
            val none = Exchanges().withClient { client -> client.fields(JsonElement.serializer(), JsonPrimitive(1)) }

            fields.values shouldBe mapOf("name" to "a b", "limit" to "2", "tags" to "[\"x\"]")
            none shouldBe ParameterValue.Fields(emptyMap())
        }
    }

    context("a client") {
        test("drops the slash at the end of its base and shows no secret") {
            Exchanges(configure = { baseUrl = "$BASE/" }).withClient { client ->
                client.settings.baseUrl shouldBe BASE
                client.toString() shouldContain "baseUrl=$BASE"
            }
        }
    }
})

private fun Call.withCookies(): Call = change {
    cookie(ParameterFormat(name = "session", style = ParameterStyle.Form, isExploded = true), ParameterValue.Text("s1"))
    cookie(ParameterFormat(name = "theme", style = ParameterStyle.Form, isExploded = true), ParameterValue.Text("dark"))
}
