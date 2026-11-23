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

import com.davils.kore.core.outcome.Outcome
import com.davils.kore.core.outcome.getOrNull
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.decodeToString
import kotlinx.io.bytestring.encodeToByteString

private const val CRLF: String = "\r\n"

private const val OPEN: String = "--b"

private const val CLOSE: String = "--b--"

private const val CONTENT: String = "x"

private const val DISPOSITION: String = "Content-Disposition"

private val boundary: Boundary = Boundary("b")

private val bytes: ByteString = ByteString(byteArrayOf(1))

private fun feedAll(vararg chunks: String): List<Outcome<List<MultipartEvent>, MultipartFailure>> {
    val decoder = MultipartDecoder(boundary)
    return chunks.map { chunk -> decoder.feed(chunk.encodeToByteString()) } + decoder.finish()
}

private fun failureOf(vararg chunks: String): MultipartFailure? {
    val outcomes = feedAll(*chunks)
    return outcomes.firstNotNullOfOrNull { outcome -> (outcome as? Outcome.Failure)?.failure }
}

class MultipartDecoderTest : FunSpec({
    context("the delimiter") {
        test("waits for the line break after padding that ends a chunk") {
            failureOf("$OPEN  ", "$CRLF$CRLF$CONTENT$CRLF$CLOSE").shouldBeNull()
            failureOf("$OPEN \r", "\n$CRLF$CONTENT$CRLF$CLOSE").shouldBeNull()
            failureOf("$OPEN\r", "\n$CRLF$CONTENT$CRLF$CLOSE").shouldBeNull()
        }

        test("refuses padding followed by anything but a line break") {
            failureOf("$OPEN x", "$CRLF$CRLF$CLOSE") shouldBe MultipartFailure.MalformedDelimiter
            failureOf("$OPEN \rx", "\n$CRLF$CLOSE") shouldBe MultipartFailure.MalformedDelimiter
        }

        test("ignores what follows the closing delimiter") {
            val decoder = MultipartDecoder(boundary)

            val body = decoder.feed("$OPEN$CRLF$CRLF$CONTENT$CRLF$CLOSE".encodeToByteString())

            body.getOrNull().shouldNotBeNull()
            decoder.feed("$CRLF$OPEN junk".encodeToByteString()) shouldBe Outcome.Success(emptyList())
            decoder.finish() shouldBe Outcome.Success(emptyList())
        }
    }

    context("a part") {
        test("has no name, file or type without the headers that carry them") {
            val part = MultipartPart(emptyList(), bytes)

            part.disposition.shouldBeNull()
            part.name.shouldBeNull()
            part.filename.shouldBeNull()
            part.contentType.shouldBeNull()
            part.header(DISPOSITION).shouldBeNull()
            part.toString() shouldBe "MultipartPart(name=none, filename=none, 1 bytes)"
        }

        test("shows the name of its file") {
            val part = MultipartPart.file(name = "a", filename = "b.txt", content = bytes)

            part.toString() shouldBe "MultipartPart(name=a, filename=b.txt, 1 bytes)"
            MultipartPart(listOf(DISPOSITION to "form-data"), bytes).toString() shouldBe
                "MultipartPart(name=none, filename=none, 1 bytes)"
        }

        test("differs in its headers and its content") {
            val part = MultipartPart.field(name = "a", value = CONTENT)

            part shouldNotBe MultipartPart.field(name = "b", value = CONTENT)
            part shouldNotBe MultipartPart.field(name = "a", value = "y")
            part.equals(CONTENT) shouldBe false
        }

        test("is written with a random boundary by default") {
            val part = MultipartPart.field(name = "a", value = CONTENT)
            val written = MultipartFormData.encode(listOf(part))

            written.decodeToString() shouldStartWith "--"
        }
    }

    context("the events") {
        test("differ by kind and by value") {
            val part = MultipartPart(emptyList(), bytes)

            MultipartEvent.Started(part).equals(MultipartEvent.Content(bytes)) shouldBe false
            MultipartEvent.Started(part) shouldBe MultipartEvent.Started(part)
            MultipartEvent.Content(bytes).equals(MultipartEvent.Ended) shouldBe false
            MultipartEvent.Content(bytes) shouldBe MultipartEvent.Content(bytes)
            MultipartEvent.Finished.toString() shouldBe "Finished"
        }
    }

    context("the limits") {
        test("have defaults and show them") {
            multipartLimits().toString() shouldStartWith "MultipartLimits(parts="
        }
    }
})
