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

package com.davils.kapi.runtime.core.codec.multipart

import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kore.core.outcome.Outcome
import com.davils.kore.core.outcome.getOrNull
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.decodeToString
import kotlinx.io.bytestring.encodeToByteString

private const val CRLF: String = "\r\n"

private const val OPEN: String = "--b"

private const val CLOSE: String = "--b--"

private const val CONTENT: String = "x"

private const val SECRET: String = "secret"

private const val PADDING: Int = 65

private const val LONGEST_BOUNDARY: Int = 70

private const val BODY_LIMIT: Int = 64

private const val SMALL: Long = 4L

private const val FIELD: String = """Content-Disposition: form-data; name="a""""

private val boundary: Boundary = Boundary("b")

private fun lines(vararg parts: String): String = parts.joinToString(CRLF)

private fun decode(text: String, limits: MultipartLimits = MultipartLimits.Default): List<MultipartPart>? {
    val outcome = MultipartFormData.decode(text.encodeToByteString(), boundary, limits)
    return outcome.getOrNull()
}

private fun failure(text: String, limits: MultipartLimits = MultipartLimits.Default): MultipartFailure? {
    val outcome = MultipartFormData.decode(text.encodeToByteString(), boundary, limits)
    return (outcome as? Outcome.Failure)?.failure
}

private fun simple(field: String, content: String): String = lines(OPEN, field, "", content, CLOSE)

class MultipartTest : FunSpec({
    context("the example of RFC 7578 §4.5") {
        test("reads the field with its headers") {
            val body = lines(
                "--AaB03x",
                """content-disposition: form-data; name="field1"""",
                "content-type: text/plain;charset=UTF-8",
                "content-transfer-encoding: quoted-printable",
                "",
                "Joe owes =E2=82=AC100.",
                "--AaB03x--",
                "",
            )
            val outcome = MultipartFormData.decode(body.encodeToByteString(), Boundary("AaB03x"))
            val parts = outcome.getOrNull().shouldNotBeNull()
            val part = parts.single()

            part.name shouldBe "field1"
            part.contentType shouldBe MediaType.parse("text/plain; charset=utf-8")
            part.header("Content-Transfer-Encoding") shouldBe "quoted-printable"
            part.content.decodeToString() shouldBe "Joe owes =E2=82=AC100."
        }
    }

    context("writing") {
        test("writes fields and files that read back") {
            val photo = ByteString(byteArrayOf(0, 1))
            val png = MediaType.parse("image/png")
            val parts = listOf(
                MultipartPart.field(name = "title", value = "Grüße"),
                MultipartPart.file(name = "photo", filename = """a"b.png""", content = photo, contentType = png),
            )
            val body = MultipartFormData.encode(parts, Boundary("x-y"))
            val read = MultipartFormData.decode(body, Boundary("x-y")).getOrNull()

            body.decodeToString() shouldStartWith
                lines("--x-y", """Content-Disposition: form-data; name="title"""", "", "Grüße")
            read shouldBe parts
            read.shouldNotBeNull()[1].filename shouldBe "a%22b.png"
        }

        test("refuses a header that would inject a field and content that holds the delimiter") {
            shouldThrow<IllegalArgumentException> { MultipartPart.field(name = "a${CRLF}X-Injected: 1", value = "v") }
            shouldThrow<IllegalArgumentException> {
                MultipartPart.file(name = "a", filename = "b\n.png", content = ByteString())
            }
            shouldThrow<IllegalArgumentException> { MultipartPart(listOf("X: a${CRLF}b" to "c"), ByteString()) }
            shouldThrow<IllegalArgumentException> { MultipartPart(listOf("X" to "a\nb"), ByteString()) }
            shouldThrow<IllegalArgumentException> {
                MultipartFormData.encode(listOf(MultipartPart.field(name = "a", value = "x$CRLF$CLOSE")), boundary)
            }
        }

        test("names its media type and its boundary") {
            val type = MultipartFormData.contentType(boundary)
            val spaced = MediaType.parse("""multipart/form-data; boundary="a b """")

            type.toString() shouldBe "multipart/form-data; boundary=b"
            MultipartFormData.boundaryOf(type) shouldBe boundary
            MultipartFormData.boundaryOf(spaced).shouldBeNull()
            MultipartFormData.boundaryOf(MediaType.MultipartFormData).shouldBeNull()
        }
    }

    context("a boundary") {
        test("has 1 to 70 characters of the set of RFC 2046 and is random when made") {
            Boundary.parseOrNull("a".repeat(LONGEST_BOUNDARY)).shouldNotBeNull()
            listOf("", "a".repeat(LONGEST_BOUNDARY + 1), "a ", "a${CRLF}b", "a;b", """a"b""").forEach { text ->
                Boundary.parseOrNull(text).shouldBeNull()
            }
            Boundary.random() shouldNotBe Boundary.random()
            Boundary.random().value shouldStartWith "kapi-"
            shouldThrow<IllegalArgumentException> { Boundary("") }
        }
    }

    context("decoding") {
        test("skips preamble and epilogue and allows padding after a boundary") {
            val text = lines("preamble", "--b  \t", "", "first", OPEN, "", "second", "--b--  epilogue --b", "")
            val parts = decode(text).shouldNotBeNull()

            parts.map { part -> part.content.decodeToString() } shouldBe listOf("first", "second")
            parts.first().headers shouldBe emptyList()
        }

        test("streams a part split at every byte") {
            val decoder = MultipartDecoder(boundary)
            val bytes = simple(field = FIELD, content = "value").encodeToByteArray()
            val events = bytes.flatMap { byte ->
                val outcome = decoder.feed(ByteString(byteArrayOf(byte)))
                outcome.getOrNull().shouldNotBeNull()
            }
            val pieces = events.filterIsInstance<MultipartEvent.Content>()

            pieces.joinToString("") { event -> event.bytes.decodeToString() } shouldBe "value"
            events.first().toString() shouldBe "Started(MultipartPart(name=a, filename=none, 0 bytes))"
            events.last() shouldBe MultipartEvent.Finished
            decoder.finish() shouldBe Outcome.Success(emptyList())
        }

        test("keeps a nested multipart body as opaque content") {
            val inner = lines("--inner", "Content-Type: text/plain", "", "nested --b-ish", "--inner--")
            val text = lines(OPEN, "Content-Type: multipart/mixed; boundary=inner", "", inner, CLOSE)
            val parts = decode(text).shouldNotBeNull()
            val part = parts.single()

            part.content.decodeToString() shouldBe inner
        }

        test("refuses a boundary that is followed by anything else") {
            failure(lines(OPEN, "", CONTENT, "--bx", CLOSE)) shouldBe MultipartFailure.MalformedDelimiter
            failure(lines("--b junk", "", CONTENT, CLOSE)) shouldBe MultipartFailure.MalformedDelimiter
            failure(lines(OPEN + " ".repeat(PADDING), "", CONTENT, CLOSE)) shouldBe MultipartFailure.MalformedDelimiter
        }

        test("refuses malformed headers") {
            failure(simple(field = "No colon", content = CONTENT)) shouldBe MultipartFailure.MalformedHeader
            failure(lines(OPEN, "A: 1", " folded", "", CONTENT, CLOSE)) shouldBe MultipartFailure.MalformedHeader
            failure(simple(field = "A b: 1", content = CONTENT)) shouldBe MultipartFailure.MalformedHeader
            failure(simple(field = "A: \u0001", content = CONTENT)) shouldBe MultipartFailure.MalformedHeader
            val broken = "$OPEN${CRLF}A: ".encodeToByteArray() +
                byteArrayOf(0xC3.toByte()) +
                lines("", "", CONTENT, CLOSE).encodeToByteArray()
            val outcome = MultipartFormData.decode(ByteString(broken), boundary)

            (outcome as? Outcome.Failure)?.failure shouldBe MultipartFailure.MalformedHeader
        }

        test("refuses a body that ends early") {
            failure(lines(OPEN, "", CONTENT)) shouldBe MultipartFailure.Truncated
            failure("") shouldBe MultipartFailure.Truncated
            failure(lines(OPEN, "", CONTENT, OPEN)) shouldBe MultipartFailure.Truncated
        }

        test("keeps every limit at the limit and refuses one past it") {
            val limits = multipartLimits {
                maxParts = 2
                maxHeaders = 1
                maxHeaderBytes = 8
                maxPartBytes = SMALL
                maxBodyBytes = 64
                maxPreambleBytes = 4
            }
            val atLimits = lines(OPEN, "", "1234", OPEN, "A: 12345", "", "", CLOSE)
            val tooMany = lines(OPEN, "", "", OPEN, "", "", OPEN, "", "", CLOSE)

            decode(atLimits, limits).shouldNotBeNull() shouldHaveSize 2
            failure(tooMany, limits) shouldBe MultipartFailure.TooManyParts
            failure(lines(OPEN, "A: 1", "B: 2", "", "", CLOSE), multipartLimits { maxHeaders = 1 }) shouldBe
                MultipartFailure.TooManyHeaders
            failure(lines(OPEN, "A: 123456", "", "", CLOSE), limits) shouldBe MultipartFailure.HeadersTooLong
            failure(lines(OPEN, "A: 123456789"), limits) shouldBe MultipartFailure.HeadersTooLong
            failure(lines(OPEN, "", "12345", CLOSE), limits) shouldBe MultipartFailure.PartTooLarge
            failure(lines("1234", OPEN, "", "", CLOSE), limits).shouldBeNull()
            failure(lines("12345", OPEN, "", "", CLOSE), limits) shouldBe MultipartFailure.PreambleTooLong
            failure(
                lines(OPEN, "", "x".repeat(BODY_LIMIT)),
                multipartLimits { maxBodyBytes = BODY_LIMIT.toLong() },
            ) shouldBe
                MultipartFailure.BodyTooLarge
        }

        test("stays failed") {
            val decoder = MultipartDecoder(boundary)
            val refused = Outcome.Failure(MultipartFailure.MalformedDelimiter)

            decoder.feed("--b junk".encodeToByteString()) shouldBe refused
            decoder.feed(simple(field = FIELD, content = CONTENT).encodeToByteString()) shouldBe refused
            decoder.finish() shouldBe refused
        }
    }

    context("the values") {
        test("show sizes and names, never content") {
            val part = MultipartPart.field(name = "né", value = SECRET)
            val same = MultipartPart.field(name = "né", value = SECRET)

            part.toString() shouldNotContain SECRET
            part.hashCode() shouldBe same.hashCode()
            MultipartEvent.Content(ByteString(byteArrayOf(1))).toString() shouldBe "Content(1 bytes)"
            MultipartEvent.Content(ByteString()).hashCode() shouldBe ByteString().hashCode()
            MultipartEvent.Started(part).hashCode() shouldBe part.hashCode()
            MultipartEvent.Ended.toString() shouldBe "Ended"
        }

        test("keep their limits at the hard ceilings") {
            multipartLimits { maxParts = MultipartLimits.HARD_PARTS }.maxParts shouldBe MultipartLimits.HARD_PARTS
            shouldThrow<ValidationException> { multipartLimits { maxParts = MultipartLimits.HARD_PARTS + 1 } }
            shouldThrow<ValidationException> { multipartLimits { maxBodyBytes = MultipartLimits.HARD_BODY_BYTES + 1 } }
        }
    }
})
