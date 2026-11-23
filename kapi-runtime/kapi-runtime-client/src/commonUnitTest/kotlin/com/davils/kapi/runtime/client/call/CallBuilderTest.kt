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

package com.davils.kapi.runtime.client.call

import com.davils.kapi.runtime.client.fixture.JSON
import com.davils.kapi.runtime.core.codec.multipart.Boundary
import com.davils.kapi.runtime.core.header.IdempotencyKey
import com.davils.kapi.runtime.core.negotiation.MediaRange
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kapi.runtime.core.parameter.ParameterEntry
import com.davils.kapi.runtime.core.parameter.ParameterFormat
import com.davils.kapi.runtime.core.parameter.ParameterStyle
import com.davils.kapi.runtime.core.parameter.ParameterValue
import com.davils.kapi.runtime.core.wire.FilePart
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.decodeToString
import kotlinx.io.bytestring.encodeToByteString
import kotlin.time.Duration.Companion.seconds

private const val ID: String = "id"

private const val GET_PET: String = "getPet"

private const val TRACE: String = "X-Trace"

private const val OTHER: String = "X-Other"

private const val SESSION: String = "session"

private const val BOUNDARY: String = "kapi-test-boundary"

private const val CONTROL: String = "a\r\nb"

private const val LENGTH: Long = 5

private val simple: ParameterFormat = simple(ID)

private val bearer: List<SecurityUse> = listOf(SecurityUse("bearer", CredentialPlacement.Bearer))

private val json: MediaType = MediaType.parse(JSON)

private fun simple(name: String): ParameterFormat =
    ParameterFormat(name = name, style = ParameterStyle.Simple, isExploded = false)

private fun form(name: String): ParameterFormat =
    ParameterFormat(name = name, style = ParameterStyle.Form, isExploded = true)

private fun text(value: String): ParameterValue = ParameterValue.Text(value)

private fun entries(vararg pairs: Pair<String, String>): List<ParameterEntry> =
    pairs.map { (name, value) -> ParameterEntry(name = name, value = value) }

private fun multipart(configure: MultipartBuilder.() -> Unit): Call =
    apiCall(operation = "upload", method = "POST") { multipart(Boundary(BOUNDARY), configure) }

private fun contentOf(call: Call): String = call.body?.run { content.decodeToString() }.orEmpty()

private fun file(content: String, filename: String? = null, contentType: String? = null): FilePart =
    FilePart(content = content.encodeToByteString(), filename = filename, contentType = contentType)

class CallBuilderTest : FunSpec({
    context("a call that is built") {
        test("has its path, its parameters, what it accepts, its body and its security in their order") {
            val body = "{}".encodeToByteString()
            val call = apiCall(operation = GET_PET, method = "put") {
                path("/pets/")
                path(simple, text("a b"))
                path("/notes")
                query(form("tag"), ParameterValue.Items(listOf("x", "y")))
                query(name = "page", value = "2")
                header(simple(TRACE), text("t1"))
                cookie(form(SESSION), text("s1"))
                accept(MediaRange.Json)
                body(json, body)
                security(bearer)
            }

            call.method shouldBe "PUT"
            call.path shouldBe "/pets/a%20b/notes"
            call.query shouldBe entries("tag" to "x", "tag" to "y", "page" to "2")
            call.headers shouldBe entries(TRACE to "t1")
            call.cookies shouldBe entries(SESSION to "s1")
            call.accept shouldBe listOf(MediaRange.Json)
            call.body shouldBe Body(json, body)
            call.security shouldBe listOf(bearer)
            call.refusal shouldBe null
        }

        test("is refused when a value would change its path, and names the first parameter that would") {
            val call = apiCall(operation = GET_PET, method = "GET") {
                path("/pets/")
                path(simple, text(".."))
                path(simple("other"), text("."))
            }

            call.refusal shouldBe CallFailure.PathRefused(ID)
        }

        test("is refused when a header or a cookie could inject another field") {
            val header = apiCall(operation = GET_PET, method = "GET") { header(simple(TRACE), text("a\r\nX-Evil: 1")) }
            val raw = apiCall(operation = GET_PET, method = "GET") { header(name = "X Bad", value = "v") }
            val cookie = apiCall(operation = GET_PET, method = "GET") { cookie(form(SESSION), text("a; admin=1")) }

            header.refusal shouldBe CallFailure.ParameterRefused(TRACE)
            raw.refusal shouldBe CallFailure.ParameterRefused("X Bad")
            cookie.refusal shouldBe CallFailure.ParameterRefused(SESSION)
            header.headers.shouldBeEmpty()
            cookie.cookies.shouldBeEmpty()
        }

        test("is idempotent by its method or by its document, and retryable with a key as well") {
            val get = apiCall(operation = GET_PET, method = "GET")
            val post = apiCall(operation = GET_PET, method = "POST")
            val marked = apiCall(operation = GET_PET, method = "POST") { idempotent() }
            val keyed = apiCall(operation = GET_PET, method = "POST") { idempotencyKey(IdempotencyKey("k-1")) }
            val random = apiCall(operation = GET_PET, method = "POST") { idempotencyKey() }
            val calls = listOf(get, post, marked, keyed)

            calls.map(Call::isIdempotent) shouldBe listOf(true, false, true, false)
            calls.map(Call::isRetryable) shouldBe listOf(true, false, true, true)
            random.idempotencyKey shouldNotBe null
        }

        test("allows no time and no repetition of its own unless it says so") {
            val plain = apiCall(operation = GET_PET, method = "GET")
            val limited = apiCall(operation = GET_PET, method = "GET") { timeout(request = 5.seconds) }

            plain.timeout shouldBe null
            limited.timeout shouldBe Timeout(request = 5.seconds)
            limited.timeout?.run { toString() } shouldBe "Timeout(request=5s, connect=none, socket=none)"
        }

        test("names its operation and its method and nothing it carries") {
            val call = apiCall(operation = GET_PET, method = "GET") { header(name = "X-Api-Key", value = "secret") }

            call.toString() shouldBe "Call(getPet, GET)"
            call.toString() shouldNotContain "secret"
        }
    }

    context("a call that is changed") {
        test("keeps what it held and adds what the change adds") {
            val call = apiCall(operation = GET_PET, method = "GET") {
                path("/pets")
                header(name = TRACE, value = "t1")
            }
            val changed = call.change { header(name = OTHER, value = "o") }

            changed.path shouldBe "/pets"
            changed.headers shouldBe entries(TRACE to "t1", OTHER to "o")
            call.change { } shouldBe call
            call.hashCode() shouldBe call.change { }.hashCode()
            changed shouldNotBe call
            call.equals(GET_PET) shouldBe false
        }
    }

    context("a call that streams") {
        test("carries its upload or its upgrade and is no longer replayable") {
            val chunks = flowOf("hello".encodeToByteString())
            val upload = apiCall(operation = "build", method = "POST") { upload(MediaType.OctetStream, chunks, LENGTH) }
            val upgrade = apiCall(operation = "attach", method = "POST") { upgrade("tcp", chunks) }
            val streamed = upload.upload

            streamed?.length shouldBe LENGTH
            streamed?.run { chunks.toList() } shouldBe chunks.toList()
            upload.isReplayable shouldBe false
            upgrade.upgrade?.protocol shouldBe "tcp"
            upgrade.isRetryable shouldBe false
            Upload(MediaType.OctetStream, emptyFlow()).toString() shouldBe
                "Upload(application/octet-stream, length=unknown)"
            Upgrade("tcp").toString() shouldBe "Upgrade(tcp)"
        }

        test("reads the content of a channel when it is sent") {
            val call = apiCall(operation = "build", method = "POST") {
                upload(MediaType.OctetStream, ByteReadChannel("channel"))
            }
            val chunks = call.upload?.chunks

            chunks?.toList() shouldBe listOf("channel".encodeToByteString())
        }
    }

    context("the form of a call") {
        test("is its fields in their order, encoded by the form encoder of the runtime core") {
            val call = apiCall(operation = "login", method = "POST") {
                form {
                    field(name = "user", value = "a b")
                    fields(name = "scope", values = listOf("read", "write&delete"))
                }
            }

            call.body?.contentType shouldBe MediaType.FormUrlEncoded
            contentOf(call) shouldBe "user=a+b&scope=read&scope=write%26delete"
        }
    }

    context("the parts of a call") {
        test("are its fields, files and parts with their names and types") {
            val call = multipart {
                text(name = "name", value = "Rex")
                texts(name = "tag", values = listOf("a"))
                file(name = "photo", file = file(content = "1", filename = "../a.gif", contentType = "image/gif"))
                file(name = "declared", file = file(content = "2"), contentType = json)
                files(name = "bare", files = listOf(file(content = "3")))
                part(name = "meta", contentType = json, content = "{}".encodeToByteString())
            }
            val content = contentOf(call)

            call.body?.run { contentType.toString() } shouldBe "multipart/form-data; boundary=$BOUNDARY"
            content shouldContain """name="name""""
            content shouldContain """filename="a.gif""""
            content shouldContain "Content-Type: image/gif"
            content shouldContain """name="declared""""
            content shouldContain "Content-Type: application/json"
            content shouldContain "Content-Type: application/octet-stream"
            call.refusal shouldBe null
        }

        test("are refused for a name with a control character or a part that holds the boundary") {
            val named = multipart { text(name = CONTROL, value = "x") }
            val holding = multipart { text(name = "a", value = "\r\n--$BOUNDARY") }

            named.refusal shouldBe CallFailure.ParameterRefused(CONTROL)
            holding.refusal shouldBe CallFailure.ParameterRefused("multipart")
            holding.body?.content shouldBe ByteString()
        }
    }
})
