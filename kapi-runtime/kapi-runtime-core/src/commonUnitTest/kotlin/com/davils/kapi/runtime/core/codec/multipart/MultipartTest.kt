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

private const val RFC_BODY: String = "--AaB03x\r\n" +
    "content-disposition: form-data; name=\"field1\"\r\n" +
    "content-type: text/plain;charset=UTF-8\r\n" +
    "content-transfer-encoding: quoted-printable\r\n" +
    "\r\n" +
    "Joe owes =E2=82=AC100.\r\n" +
    "--AaB03x--\r\n"

private const val SIMPLE: String = "--b\r\nContent-Disposition: form-data; name=\"a\"\r\n\r\nvalue\r\n--b--"

private const val SMALL: Int = 4

private val boundary: Boundary = Boundary("b")

private fun decode(text: String, limits: MultipartLimits = MultipartLimits.Default): Outcome<List<MultipartPart>, MultipartFailure> =
    MultipartFormData.decode(text.encodeToByteString(), boundary, limits)

private fun failure(text: String, limits: MultipartLimits = MultipartLimits.Default): MultipartFailure? =
    (decode(text, limits) as? Outcome.Failure)?.failure

class MultipartTest : FunSpec({
    context("the example of RFC 7578 §4.5") {
        test("reads the field with its headers") {
            val parts = MultipartFormData.decode(RFC_BODY.encodeToByteString(), Boundary("AaB03x"))
                .getOrNull().shouldNotBeNull()
            val part = parts.single()

            part.name shouldBe "field1"
            part.contentType shouldBe MediaType.parse("text/plain; charset=utf-8")
            part.header("Content-Transfer-Encoding") shouldBe "quoted-printable"
            part.content.decodeToString() shouldBe "Joe owes =E2=82=AC100."
        }
    }

    context("writing") {
        test("writes fields and files that read back") {
            val parts = listOf(
                MultipartPart.field("title", "Grüße"),
                MultipartPart.file("photo", "a\"b.png", ByteString(byteArrayOf(0, 1)), MediaType.parse("image/png")),
            )
            val body = MultipartFormData.encode(parts, Boundary("x-y"))

            body.decodeToString() shouldStartWith "--x-y\r\nContent-Disposition: form-data; name=\"title\"\r\n\r\nGrüße"
            val read = MultipartFormData.decode(body, Boundary("x-y")).getOrNull().shouldNotBeNull()
            read shouldBe parts
            read[1].filename shouldBe "a%22b.png"
            read[1].name shouldBe "photo"
        }

        test("refuses a header that would inject a field and content that holds the delimiter") {
            shouldThrow<IllegalArgumentException> { MultipartPart.field("a\r\nX-Injected: 1", "v") }
            shouldThrow<IllegalArgumentException> { MultipartPart.file("a", "b\n.png", ByteString()) }
            shouldThrow<IllegalArgumentException> { MultipartPart(listOf("X: a\r\nb" to "c"), ByteString()) }
            shouldThrow<IllegalArgumentException> { MultipartPart(listOf("X" to "a\nb"), ByteString()) }
            shouldThrow<IllegalArgumentException> {
                MultipartFormData.encode(listOf(MultipartPart.field("a", "x\r\n--b--")), boundary)
            }
        }

        test("names its media type and its boundary") {
            val type = MultipartFormData.contentType(boundary)

            type.toString() shouldBe "multipart/form-data; boundary=b"
            MultipartFormData.boundaryOf(type) shouldBe boundary
            MultipartFormData.boundaryOf(MediaType.parse("multipart/form-data; boundary=\"a b \"")).shouldBeNull()
            MultipartFormData.boundaryOf(MediaType.MultipartFormData).shouldBeNull()
        }
    }

    context("a boundary") {
        test("has 1 to 70 characters of the set of RFC 2046 and is random when made") {
            Boundary.parseOrNull("a".repeat(70)).shouldNotBeNull()
            listOf("", "a".repeat(71), "a ", "a\r\nb", "a;b", "a\"b").forEach { text ->
                Boundary.parseOrNull(text).shouldBeNull()
            }
            Boundary.random() shouldNotBe Boundary.random()
            Boundary.random().value shouldStartWith "kapi-"
            shouldThrow<IllegalArgumentException> { Boundary("") }
        }
    }

    context("decoding") {
        test("skips preamble and epilogue and allows padding after a boundary") {
            val text = "preamble\r\n--b  \t\r\n\r\nfirst\r\n--b\r\n\r\nsecond\r\n--b--  epilogue --b\r\n"
            val parts = decode(text).getOrNull().shouldNotBeNull()

            parts.map { part -> part.content.decodeToString() } shouldBe listOf("first", "second")
            parts.first().headers shouldBe emptyList()
        }

        test("streams a part split at every byte") {
            val decoder = MultipartDecoder(boundary)
            val events = SIMPLE.encodeToByteArray().flatMap { byte ->
                decoder.feed(ByteString(byteArrayOf(byte))).getOrNull().shouldNotBeNull()
            }
            val content = events.filterIsInstance<MultipartEvent.Content>().joinToString("") { event ->
                event.bytes.decodeToString()
            }

            content shouldBe "value"
            events.first().shouldNotBeNull().toString() shouldBe "Started(MultipartPart(name=a, filename=null, 0 bytes))"
            events.last() shouldBe MultipartEvent.Finished
            decoder.finish() shouldBe Outcome.Success(emptyList())
        }

        test("keeps a nested multipart body as opaque content") {
            val inner = "--inner\r\nContent-Type: text/plain\r\n\r\nnested --b-ish\r\n--inner--"
            val text = "--b\r\nContent-Type: multipart/mixed; boundary=inner\r\n\r\n$inner\r\n--b--"

            decode(text).getOrNull().shouldNotBeNull().single().content.decodeToString() shouldBe inner
        }

        test("refuses a boundary that is followed by anything else") {
            failure("--b\r\n\r\nx\r\n--bx\r\n--b--") shouldBe MultipartFailure.MalformedDelimiter
            failure("--b junk\r\n\r\nx\r\n--b--") shouldBe MultipartFailure.MalformedDelimiter
            failure("--b" + " ".repeat(65) + "\r\n\r\nx\r\n--b--") shouldBe MultipartFailure.MalformedDelimiter
        }

        test("refuses malformed headers") {
            failure("--b\r\nNo colon\r\n\r\nx\r\n--b--") shouldBe MultipartFailure.MalformedHeader
            failure("--b\r\nA: 1\r\n folded\r\n\r\nx\r\n--b--") shouldBe MultipartFailure.MalformedHeader
            failure("--b\r\nA b: 1\r\n\r\nx\r\n--b--") shouldBe MultipartFailure.MalformedHeader
            failure("--b\r\nA: \u0001\r\n\r\nx\r\n--b--") shouldBe MultipartFailure.MalformedHeader
            val broken = "--b\r\nA: ".encodeToByteArray() + byteArrayOf(0xC3.toByte()) + "\r\n\r\nx\r\n--b--".encodeToByteArray()
            (MultipartFormData.decode(ByteString(broken), boundary) as? Outcome.Failure)?.failure shouldBe
                MultipartFailure.MalformedHeader
        }

        test("refuses a body that ends early") {
            failure("--b\r\n\r\nx") shouldBe MultipartFailure.Truncated
            failure("") shouldBe MultipartFailure.Truncated
            failure("--b\r\n\r\nx\r\n--b") shouldBe MultipartFailure.Truncated
        }

        test("keeps every limit at the limit and refuses one past it") {
            val limits = multipartLimits {
                maxParts = 2
                maxHeaders = 1
                maxHeaderBytes = 8
                maxPartBytes = SMALL.toLong()
                maxBodyBytes = 64
                maxPreambleBytes = SMALL
            }

            decode("--b\r\n\r\n1234\r\n--b\r\nA: 12345\r\n\r\n\r\n--b--", limits).getOrNull().shouldNotBeNull() shouldHaveSize 2
            failure("--b\r\n\r\n\r\n--b\r\n\r\n\r\n--b\r\n\r\n\r\n--b--", limits) shouldBe MultipartFailure.TooManyParts
            failure("--b\r\nA: 1\r\nB: 2\r\n\r\n\r\n--b--", multipartLimits { maxHeaders = 1 }) shouldBe
                MultipartFailure.TooManyHeaders
            failure("--b\r\nA: 123456\r\n\r\n\r\n--b--", limits) shouldBe MultipartFailure.HeadersTooLong
            failure("--b\r\nA: 123456789", limits) shouldBe MultipartFailure.HeadersTooLong
            failure("--b\r\n\r\n12345\r\n--b--", limits) shouldBe MultipartFailure.PartTooLarge
            failure("1234\r\n--b\r\n\r\n\r\n--b--", limits).shouldBeNull()
            failure("12345\r\n--b\r\n\r\n\r\n--b--", limits) shouldBe MultipartFailure.PreambleTooLong
            failure("--b\r\n\r\n" + "x".repeat(64), multipartLimits { maxBodyBytes = 64 }) shouldBe
                MultipartFailure.BodyTooLarge
        }

        test("stays failed") {
            val decoder = MultipartDecoder(boundary)

            decoder.feed("--b junk".encodeToByteString()) shouldBe Outcome.Failure(MultipartFailure.MalformedDelimiter)
            decoder.feed(SIMPLE.encodeToByteString()) shouldBe Outcome.Failure(MultipartFailure.MalformedDelimiter)
            decoder.finish() shouldBe Outcome.Failure(MultipartFailure.MalformedDelimiter)
        }
    }

    context("the values") {
        test("show sizes and names, never content") {
            val part = MultipartPart.field("n\u00e9", "secret")

            part.toString() shouldNotContain "secret"
            part.hashCode() shouldBe MultipartPart.field("n\u00e9", "secret").hashCode()
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
