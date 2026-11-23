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

package com.davils.kapi.runtime.server.request

import com.davils.kapi.runtime.core.codec.Codecs
import com.davils.kapi.runtime.core.codec.codecs
import com.davils.kapi.runtime.core.codec.multipart.MultipartPart
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kapi.runtime.server.fixture.Note
import com.davils.kapi.runtime.server.reply.Reply
import com.davils.kapi.runtime.server.reply.ReplyBody
import com.davils.kapi.runtime.server.reply.reply
import com.davils.kore.core.principal.principal
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.io.bytestring.decodeToString
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private const val OPERATION: String = "probe"

private const val PATH: String = "/probe"

private const val POST: String = "POST"

private const val NOTE: String = "note"

private const val NOTE_A: String = """{"text":"a"}"""

private const val COUNT: String = "count"

private const val SECRET: String = "secret"

private const val SEVEN: Int = 7

private const val OK: Int = 200

private const val JSON: String = "application/json"

private const val THREE: Int = 3

private val lenient: Codecs = codecs { json = Json { isLenient = true } }

private fun request(configure: RequestBuilder.() -> Unit = {}): Request =
    Request.of(operation = OPERATION, method = POST, path = PATH, configure = configure)

private fun note(text: String, type: String? = null): MultipartPart {
    val disposition = "Content-Disposition" to "form-data; name=\"$NOTE\""
    val typed = type?.let { found -> listOf("Content-Type" to found) }.orEmpty()
    return MultipartPart(listOf(disposition) + typed, text.encodeToByteString())
}

private fun Input.notePart(parts: List<MultipartPart>?, name: String = NOTE, isRequired: Boolean = true): Note? =
    part(parts = parts, name = name, deserializer = Note.serializer(), isRequired = isRequired)

private fun Input.numberPart(parts: List<MultipartPart>?): Int? =
    part(parts = parts, name = NOTE, deserializer = Int.serializer(), isRequired = true)

private fun jsonNote(text: String): MultipartPart = note(text = text, type = JSON)

private fun Input.number(name: String, text: String?, isRequired: Boolean = true): Int? =
    field(name = name, text = text, deserializer = Int.serializer(), isRequired = isRequired)

private fun problemText(reply: Reply): String {
    val body = reply.body.shouldBeInstanceOf<ReplyBody.Bytes>()
    return body.content.decodeToString()
}

class RequestFactoryTest : FunSpec({
    context("a request built without Ktor") {
        test("carries what it was built with and reads its query within the limits of the server") {
            val member = principal("alice")
            val built = Request.of(operation = OPERATION, method = "QUERY", path = "/items/5") {
                rawQuery = "limit=5&tag=a%20b"
                pathParameters = mapOf("id" to "5")
                principal = member
                header(name = "Authorization", value = SECRET)
                body(MediaType.Json, NOTE_A.encodeToByteString())
            }

            built.method shouldBe "QUERY"
            built.scheme shouldBe "https"
            built.authority shouldBe "localhost"
            built.rawQuery shouldBe "limit=5&tag=a%20b"
            built.query shouldBe listOf("limit" to "5", "tag" to "a b")
            built.pathParameters shouldBe mapOf("id" to "5")
            built.principal shouldBe member
            built.peer shouldBe Peer()
            built.hasBody shouldBe true
            built.headers.masked()["authorization"] shouldNotBe listOf(SECRET)
            built.input().body(Note.serializer(), isRequired = true) shouldBe Note("a")
        }

        test("takes the scheme, the authority and the server it is read with") {
            val built = request {
                scheme = "http"
                authority = "api.example.com"
                body = "x".encodeToByteString()
                server { limits { maxRequestBytes = 1 } }
            }

            built.scheme shouldBe "http"
            built.authority shouldBe "api.example.com"
            built.hasBody shouldBe true
            built.contentType.shouldBeNull()
        }

        test("refuses a method, a path, a header or a query the server would never read") {
            shouldThrow<IllegalArgumentException> { Request.of(operation = OPERATION, method = "get", path = PATH) }
            shouldThrow<IllegalArgumentException> { Request.of(operation = OPERATION, method = POST, path = "probe") }
            shouldThrow<IllegalArgumentException> { request { header(name = "a b", value = "1") } }
            shouldThrow<IllegalArgumentException> { request { header(name = "a", value = "1\r\nb: 2") } }
            shouldThrow<IllegalArgumentException> { request { rawQuery = "a=%FF" } }
            shouldThrow<IllegalArgumentException> {
                request {
                    rawQuery = "a=1&b=2"
                    server { limits { maxQueryParameters = 1 } }
                }
            }
            shouldThrow<IllegalArgumentException> {
                request {
                    rawQuery = "a=12"
                    server { limits { maxQueryLength = 2 } }
                }
            }
        }
    }

    context("a typed part") {
        test("is decoded by its own type or the given one through the codecs of the server") {
            val input = request { server { codecs = lenient } }.input()
            val json = MediaType.Json

            input.notePart(listOf(jsonNote("{text:a}"))) shouldBe Note("a")
            input.part(
                parts = listOf(note("""{"text":"b"}""")),
                name = NOTE,
                deserializer = Note.serializer(),
                isRequired = true,
                mediaType = json,
            ) shouldBe Note("b")
            input.numberPart(listOf(note("7"))) shouldBe SEVEN
            input.isValid shouldBe true
        }

        test("reports a missing, malformed or invalid part under its name") {
            val input = request().input()
            val blank = listOf(jsonNote("""{"text":" "}"""))

            input.notePart(emptyList()).shouldBeNull()
            input.notePart(emptyList(), name = "optional", isRequired = false).shouldBeNull()
            input.notePart(null, name = "absent").shouldBeNull()
            input.notePart(listOf(jsonNote("{"))).shouldBeNull()
            input.notePart(blank) shouldBe Note(" ")

            input.isValid shouldBe false
            val text = problemText(input.refused())

            text shouldContain "\"body.note\""
            text shouldContain "\"body.note.text\""
            text shouldNotContain "\"body.absent\""
            text shouldNotContain "\"body.optional\""
        }

        test("of repeated parts are decoded in order, or refused as a whole") {
            val input = request().input()
            val parts = listOf(note("1"), note("2"), MultipartPart.field(name = "other", value = "3"))

            input.partItems(parts, NOTE, Int.serializer()) shouldBe listOf(1, 2)
            val missing = input.partItems(parts, "missing", Int.serializer())

            missing.shouldNotBeNull().shouldBeEmpty()
            input.partItems(null, NOTE, Int.serializer()).shouldBeNull()
            input.partItems(listOf(note("1"), note("x")), NOTE, Int.serializer()).shouldBeNull()
            input.isValid shouldBe false
        }
    }

    context("a field of a form") {
        test("is decoded as JSON by default and through any other codec of the server") {
            val input = request().input()

            val note = input.field(name = NOTE, text = NOTE_A, deserializer = Note.serializer(), isRequired = true)

            note shouldBe Note("a")
            input.number(name = COUNT, text = "3") shouldBe THREE
            input.field(
                name = "plain",
                text = "text",
                deserializer = String.serializer(),
                isRequired = true,
                mediaType = MediaType.TextPlain,
            ) shouldBe "text"
            input.number("optional", null, isRequired = false).shouldBeNull()
            input.isValid shouldBe true
        }

        test("reports a malformed or missing field under its name") {
            val input = request().input()

            input.number(name = COUNT, text = "three").shouldBeNull()
            input.number("missing", null).shouldBeNull()

            val text = problemText(input.refused())

            text shouldContain "\"body.count\""
            text shouldContain "\"body.missing\""
        }
    }

    context("a value of a reply") {
        test("is written by the codecs it is given, as the server would") {
            val body = reply(status = OK) { json(Note.serializer(), Note("a")) }.body

            val value = body.shouldBeInstanceOf<ReplyBody.Value>()

            value.encoded(Codecs.Default).decodeToString() shouldBe NOTE_A
        }
    }
})
