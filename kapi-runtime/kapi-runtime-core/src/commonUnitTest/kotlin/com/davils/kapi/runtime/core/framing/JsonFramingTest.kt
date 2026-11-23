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

package com.davils.kapi.runtime.core.framing

import com.davils.kore.core.outcome.Outcome
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString

private const val FIRST: String = """{"a":1}"""

private const val SECOND: String = "[2]"

private const val SMALL: Int = 4

private const val SEPARATOR: Char = '\u001E'

class JsonFramingTest : FunSpec({
    context("JSON Lines") {
        test("splits records across chunks, drops CR and blank lines and hands out the last on finish") {
            val reader = JsonLinesReader()

            reader.feed("{\"a\"".encodeToByteString()) shouldBe Outcome.Success(emptyList())
            reader.feed(":1}\r\n\n  \n[".encodeToByteString()) shouldBe Outcome.Success(listOf(FIRST))
            reader.feed("2]".encodeToByteString()) shouldBe Outcome.Success(emptyList())
            reader.finish() shouldBe Outcome.Success(listOf(SECOND))
        }

        test("refuses a record above the limit and one that is no UTF-8") {
            val small = JsonLinesReader(framingLimits { maxRecordBytes = SMALL })
            val broken = JsonLinesReader()

            small.feed("1234\n".encodeToByteString()) shouldBe Outcome.Success(listOf("1234"))
            small.feed("12345\n".encodeToByteString()) shouldBe Outcome.Failure(FramingFailure.RecordTooLong)
            small.finish() shouldBe Outcome.Failure(FramingFailure.RecordTooLong)
            broken.feed(ByteString(byteArrayOf(0xC3.toByte(), 0x0A))) shouldBe Outcome.Failure(FramingFailure.NotUtf8)
        }

        test("frames compact records and refuses line breaks") {
            JsonLines.frame(FIRST) shouldBe "$FIRST\n".encodeToByteString()
            shouldThrow<IllegalArgumentException> { JsonLines.frame("{\n}") }
            shouldThrow<IllegalArgumentException> { JsonLines.frame("{\r}") }
        }
    }

    context("JSON text sequences of RFC 7464") {
        test("split records, skip what precedes the first separator and count truncated records") {
            val reader = JsonSequenceReader()
            val stream = "junk$SEPARATOR$FIRST\n$SEPARATOR$SEPARATOR{\"cut\"$SEPARATOR$SECOND\n"

            reader.feed(stream.encodeToByteString()) shouldBe Outcome.Success(listOf(FIRST))
            reader.finish() shouldBe Outcome.Success(listOf(SECOND))
            reader.truncatedRecords shouldBe 1
        }

        test("count a last record without a line feed as truncated") {
            val reader = JsonSequenceReader()

            reader.feed("$SEPARATOR$SECOND".encodeToByteString()) shouldBe Outcome.Success(emptyList())
            reader.finish() shouldBe Outcome.Success(emptyList())
            reader.truncatedRecords shouldBe 1
        }

        test("refuse a record above the limit and one that is no UTF-8") {
            val small = JsonSequenceReader(framingLimits { maxRecordBytes = SMALL })
            val broken = JsonSequenceReader()
            val bytes = byteArrayOf(SEPARATOR.code.toByte(), 0xC3.toByte(), 0x0A, SEPARATOR.code.toByte())

            small.feed("${SEPARATOR}12345".encodeToByteString()) shouldBe Outcome.Failure(FramingFailure.RecordTooLong)
            broken.feed(ByteString(bytes)) shouldBe Outcome.Failure(FramingFailure.NotUtf8)
            broken.finish() shouldBe Outcome.Failure(FramingFailure.NotUtf8)
        }

        test("frame records and refuse a record separator inside one") {
            JsonLines.frameSequence(SECOND) shouldBe "$SEPARATOR$SECOND\n".encodeToByteString()
            shouldThrow<IllegalArgumentException> { JsonLines.frameSequence("[$SEPARATOR]") }
        }
    }

    context("the limits") {
        test("keep their hard ceiling") {
            framingLimits().toString() shouldContain "record=1048576"
            framingLimits { maxEventBytes = FramingLimits.HARD_BYTES }.maxEventBytes shouldBe FramingLimits.HARD_BYTES
            shouldThrow<ValidationException> { framingLimits { maxRecordBytes = FramingLimits.HARD_BYTES + 1 } }
            shouldThrow<ValidationException> { framingLimits { maxRecordBytes = 0 } }
        }
    }
})
