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

import com.davils.kapi.runtime.core.codec.multipart.Boundary
import com.davils.kapi.runtime.core.codec.multipart.MultipartEvent
import com.davils.kapi.runtime.core.codec.multipart.MultipartFormData
import com.davils.kapi.runtime.core.codec.multipart.MultipartPart
import com.davils.kapi.runtime.core.codec.multipart.multipartLimits
import com.davils.kapi.runtime.core.negotiation.MediaRange
import com.davils.kapi.runtime.core.problem.ProblemViolation
import com.davils.kapi.runtime.server.fixture.JSON
import com.davils.kapi.runtime.server.fixture.Note
import com.davils.kapi.runtime.server.fixture.PROBE
import com.davils.kapi.runtime.server.fixture.deflated
import com.davils.kapi.runtime.server.fixture.gzipped
import com.davils.kapi.runtime.server.fixture.probe
import com.davils.kapi.runtime.server.fixture.probing
import com.davils.kapi.runtime.server.fixture.problemOf
import com.davils.kapi.runtime.server.problem.ServerCodes
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.server.testing.ApplicationTestBuilder
import kotlinx.io.bytestring.decodeToString

private const val OCTETS: String = "application/octet-stream"

private const val BAD_REQUEST: Int = 400

private const val TOO_LARGE: Int = 413

private const val UNSUPPORTED: Int = 415

private const val INTERNAL: Int = 500

private const val SMALL: Long = 8L

private const val BOMB: Int = 100_000

private const val NOTE: String = """{"text":"a"}"""

private const val CONTENT_TYPE: String = "Content-Type"

private const val CONTENT_ENCODING: String = "Content-Encoding"

private val boundary: Boundary = Boundary("b")

private val multipartType: String = MultipartFormData.contentType(boundary).toString()

private val parts: List<MultipartPart> =
    listOf(MultipartPart.field(name = "a", value = "1"), MultipartPart.field(name = "b", value = "2"))

private val fields: ByteArray = MultipartFormData.encode(parts, boundary).toByteArray()

private suspend fun ApplicationTestBuilder.send(
    body: ByteArray,
    type: String? = JSON,
    encoding: String? = null,
): HttpResponse = client.post(PROBE) {
    type?.let { value -> header(CONTENT_TYPE, value) }
    encoding?.let { value -> header(CONTENT_ENCODING, value) }
    setBody(body)
}

private fun bodyProbe(isRequired: Boolean = true, mediaTypes: List<MediaRange> = listOf(MediaRange.Any)) =
    probe { input -> input.body(deserializer = Note.serializer(), isRequired = isRequired, mediaTypes = mediaTypes) }

class BodyTest : FunSpec({
    context("the body of a request") {
        test("is decoded as the JSON the document declares") {
            probing(bodyProbe()) {
                send(NOTE.encodeToByteArray()).bodyAsText() shouldBe Note("a").toString()
            }
        }

        test("is decoded as a form when it is one") {
            probing(bodyProbe()) {
                send("text=b".encodeToByteArray(), type = "application/x-www-form-urlencoded").bodyAsText() shouldBe
                    Note("b").toString()
            }
        }

        test("is refused with what its model does not accept, under the path of the body") {
            probing(bodyProbe()) {
                problemOf(send("""{"text":" "}""".encodeToByteArray())).violations shouldBe
                    listOf(
                        ProblemViolation(path = "body.text", code = "kapi.test.blank", message = "must not be blank"),
                    )
            }
        }

        test("is refused when it is no such JSON or missing though required") {
            probing(bodyProbe()) {
                problemOf(send("{".encodeToByteArray())).violations shouldBe listOf(
                    ProblemViolation(path = "body", code = ServerCodes.MALFORMED, message = MALFORMED_TEXT),
                )
                problemOf(send(ByteArray(0))).violations shouldBe listOf(
                    ProblemViolation(path = "body", code = ServerCodes.REQUIRED, message = "is required"),
                )
            }
        }

        test("is nothing without a finding when it is optional and missing") {
            probing(bodyProbe(isRequired = false)) {
                send(ByteArray(0)).bodyAsText() shouldBe "null"
            }
        }

        test("is refused as an unsupported media type when no codec reads it or the operation does not") {
            probing(bodyProbe(mediaTypes = listOf(MediaRange.Json))) {
                send("<note/>".encodeToByteArray(), type = "application/xml").status.value shouldBe UNSUPPORTED
                problemOf(send("a".encodeToByteArray(), type = "text/plain")).code shouldBe
                    ServerCodes.UNSUPPORTED_MEDIA_TYPE
                send(NOTE.encodeToByteArray(), type = null).status.value shouldBe UNSUPPORTED
            }
            probing(bodyProbe()) {
                send("<note/>".encodeToByteArray(), type = "application/xml").status.value shouldBe UNSUPPORTED
            }
        }

        test("is refused as too large when it is longer than the server reads") {
            probing(bodyProbe(), settings = { limits { maxRequestBytes = SMALL } }) {
                val response = send(NOTE.encodeToByteArray())

                response.status.value shouldBe TOO_LARGE
                problemOf(response).code shouldBe ServerCodes.TOO_LARGE
            }
        }

        test("is its bytes when the document declares no model") {
            val operation = probe { input -> input.bytes(isRequired = true)?.decodeToString() }

            probing(operation) {
                send("raw".encodeToByteArray(), type = OCTETS).bodyAsText() shouldBe "raw"
                send(ByteArray(0)).status.value shouldBe BAD_REQUEST
            }
        }

        test("is read once, and a second read fails the handler without a word of why") {
            val operation = probe { input ->
                input.bytes(isRequired = true)
                input.bytes(isRequired = true)
            }

            probing(operation) {
                val response = send(NOTE.encodeToByteArray())

                response.status.value shouldBe INTERNAL
                response.bodyAsText().contains("once") shouldBe false
            }
        }
    }

    context("a compressed body") {
        test("is inflated from gzip and deflate before it is read") {
            probing(bodyProbe()) {
                send(gzipped(NOTE.encodeToByteArray()), encoding = "gzip").bodyAsText() shouldBe Note("a").toString()
                send(deflated(NOTE.encodeToByteArray()), encoding = "deflate").bodyAsText() shouldBe
                    Note("a").toString()
                send(NOTE.encodeToByteArray(), encoding = "identity").bodyAsText() shouldBe Note("a").toString()
            }
        }

        test("is refused with the codings the server inflates when its coding is unknown or stacked") {
            probing(bodyProbe()) {
                val unknown = send(NOTE.encodeToByteArray(), encoding = "br")
                val stacked = client.post(PROBE) {
                    header(key = CONTENT_TYPE, value = JSON)
                    header(key = CONTENT_ENCODING, value = "gzip")
                    header(key = CONTENT_ENCODING, value = "gzip")
                    setBody(gzipped(gzipped(NOTE.encodeToByteArray())))
                }

                unknown.status.value shouldBe UNSUPPORTED
                unknown.headers["Accept-Encoding"] shouldBe "gzip, deflate"
                problemOf(unknown).code shouldBe ServerCodes.UNSUPPORTED_ENCODING
                stacked.status.value shouldBe UNSUPPORTED
            }
        }

        test("is refused when it inflates beyond the limit, however small it is compressed") {
            val bomb = gzipped(ByteArray(BOMB))

            probing(bodyProbe(), settings = { limits { maxDecompressedBytes = BOMB / 2L } }) {
                send(bomb, encoding = "gzip").status.value shouldBe TOO_LARGE
            }
        }

        test("is refused as malformed when it is no such coding") {
            probing(bodyProbe()) {
                send(NOTE.encodeToByteArray(), encoding = "gzip").status.value shouldBe BAD_REQUEST
            }
        }

        test("is refused when it is larger than the server reads before it is inflated") {
            probing(bodyProbe(), settings = { limits { maxRequestBytes = SMALL } }) {
                send(gzipped(NOTE.encodeToByteArray()), encoding = "gzip").status.value shouldBe TOO_LARGE
            }
        }
    }

    context("a form of multipart/form-data read as a whole") {
        test("is its parts in order") {
            val operation = probe { input -> input.parts(isRequired = true)?.map { part -> part.name } }

            probing(operation) {
                send(fields, type = multipartType).bodyAsText() shouldBe "[a, b]"
            }
        }

        test("is refused when it breaks a limit, its grammar or names no boundary") {
            val operation = probe { input -> input.parts(isRequired = true) }
            val truncated = fields.copyOf(fields.size - CLOSING_LENGTH)

            probing(operation, settings = { limits { multipart = multipartLimits { maxParts = 1 } } }) {
                send(fields, type = multipartType).status.value shouldBe TOO_LARGE
            }
            probing(operation) {
                send(truncated, type = multipartType).status.value shouldBe BAD_REQUEST
                send(fields, type = "multipart/form-data").status.value shouldBe BAD_REQUEST
                send(fields, type = JSON).status.value shouldBe UNSUPPORTED
            }
        }
    }

    context("an upload that streams") {
        test("hands its pieces to the handler as they arrive") {
            val operation = probe { input ->
                val upload = input.upload(isRequired = true, mediaTypes = listOf(MediaRange.of("application/*")))
                var total = 0L
                upload?.read { chunk -> total += chunk.size }
                listOf(upload?.run { contentType.toString() }.orEmpty(), total)
            }

            probing(operation) {
                send(ByteArray(BOMB), type = OCTETS).bodyAsText() shouldBe
                    "[application/octet-stream, $BOMB]"
                send(
                    gzipped(ByteArray(BOMB)),
                    type = OCTETS,
                    encoding = "gzip",
                ).bodyAsText() shouldBe
                    "[application/octet-stream, $BOMB]"
                send(ByteArray(0)).status.value shouldBe BAD_REQUEST
                send(ByteArray(1), type = "text/plain").status.value shouldBe UNSUPPORTED
            }
        }

        test("is refused as too large in the middle of the stream") {
            val operation = probe { input -> input.upload(isRequired = true)?.read { } }

            probing(operation, settings = { limits { maxUploadBytes = SMALL } }) {
                send(ByteArray(BOMB), type = OCTETS).status.value shouldBe TOO_LARGE
                send(gzipped(ByteArray(BOMB)), encoding = "br").status.value shouldBe UNSUPPORTED
            }
        }

        test("hands the events of multipart/form-data to the handler as they are read") {
            val operation = probe { input ->
                val events = mutableListOf<String>()
                input.multipart(isRequired = true)?.read { event -> events += kindOf(event) }
                events
            }

            probing(operation) {
                send(fields, type = multipartType).bodyAsText() shouldBe
                    "[started, content, ended, started, content, ended, finished]"
                send(fields, type = "multipart/form-data").status.value shouldBe BAD_REQUEST
            }
        }

        test("is refused when a streamed form breaks a limit or ends early") {
            val operation = probe { input -> input.multipart(isRequired = true)?.read { } }
            val truncated = fields.copyOf(fields.size - CLOSING_LENGTH)

            probing(operation, settings = { limits { multipart = multipartLimits { maxParts = 1 } } }) {
                send(fields, type = multipartType).status.value shouldBe TOO_LARGE
            }
            probing(operation) {
                send(truncated, type = multipartType).status.value shouldBe BAD_REQUEST
            }
            probing(operation, settings = { limits { maxUploadBytes = SMALL } }) {
                send(fields, type = multipartType).status.value shouldBe TOO_LARGE
            }
        }
    }
})

private const val MALFORMED_TEXT: String = "is not what the document declares"

private const val CLOSING_LENGTH: Int = 6

private fun kindOf(event: MultipartEvent): String = when (event) {
    is MultipartEvent.Started -> "started"
    is MultipartEvent.Content -> "content"
    MultipartEvent.Ended -> "ended"
    MultipartEvent.Finished -> "finished"
}
