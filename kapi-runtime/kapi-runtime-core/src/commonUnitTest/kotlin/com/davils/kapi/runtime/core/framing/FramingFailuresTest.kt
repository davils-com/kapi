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
import com.davils.kore.core.outcome.getOrNull
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString

private const val SMALL: Int = 4

private const val INVALID: Byte = -1

private const val LINE_FEED: Byte = 0x0A

private const val SEPARATOR: Byte = 0x1E

private const val ONE: Byte = 0x31

private const val DATA: String = "x"

private const val RETRY: Long = 5L

private const val TOO_MANY_DIGITS: Int = 19

private val notUtf8: Outcome<List<String>, FramingFailure> = Outcome.Failure(FramingFailure.NotUtf8)

private val tooLong: Outcome<List<String>, FramingFailure> = Outcome.Failure(FramingFailure.RecordTooLong)

private fun bytes(vararg values: Byte): ByteString = ByteString(values)

private fun events(reader: ServerSentEventReader, text: String): List<ServerSentEvent> {
    val outcome = reader.feed(text.encodeToByteString())
    return outcome.getOrNull().shouldNotBeNull()
}

class FramingFailuresTest : FunSpec({
    context("JSON Lines") {
        test("keeps the first failure of a chunk and drops the records after it") {
            val reader = JsonLinesReader()

            reader.feed(bytes(INVALID, LINE_FEED, INVALID, LINE_FEED)) shouldBe notUtf8
            reader.feed("1\n".encodeToByteString()) shouldBe notUtf8
            reader.finish() shouldBe notUtf8
        }

        test("drops a record that follows a failure in the same chunk") {
            JsonLinesReader().feed(bytes(INVALID, LINE_FEED, ONE, LINE_FEED)) shouldBe notUtf8
        }

        test("stays failed after an overlong record") {
            val reader = JsonLinesReader(framingLimits { maxRecordBytes = SMALL })

            reader.feed("12345".encodeToByteString()) shouldBe tooLong
            reader.feed("6".encodeToByteString()) shouldBe tooLong
        }
    }

    context("JSON text sequences") {
        test("keeps the first failure of a chunk and stays failed") {
            val reader = JsonSequenceReader()
            val broken = bytes(SEPARATOR, INVALID, LINE_FEED, SEPARATOR, INVALID, LINE_FEED, SEPARATOR)

            reader.feed(broken) shouldBe notUtf8
            reader.feed(bytes(SEPARATOR)) shouldBe notUtf8
            reader.finish() shouldBe notUtf8
        }

        test("stays failed after an overlong record") {
            val reader = JsonSequenceReader(framingLimits { maxRecordBytes = SMALL })

            reader.feed("\u001E12345".encodeToByteString()) shouldBe tooLong
            reader.feed("6".encodeToByteString()) shouldBe tooLong
        }
    }

    context("server-sent events") {
        test("are incomplete while a line or data is pending") {
            val reader = ServerSentEventReader()

            reader.isIncomplete shouldBe false
            reader.feed("da".encodeToByteString())
            reader.isIncomplete shouldBe true
            reader.feed("ta: x\n".encodeToByteString())
            reader.isIncomplete shouldBe true
            reader.feed("\n".encodeToByteString())
            reader.isIncomplete shouldBe false
        }

        test("read a short first line without a byte order mark") {
            events(ServerSentEventReader(), "a\n\ndata: x\n\n") shouldBe listOf(ServerSentEvent(DATA))
        }

        test("ignore a reconnection time that is empty, too long or no number") {
            val reader = ServerSentEventReader()

            events(reader, "retry:\nretry: 1x\nretry: ${"1".repeat(TOO_MANY_DIGITS)}\ndata: x\n\n")
            reader.retry.shouldBeNull()
        }

        test("keep the first failure of an event that is too long") {
            val reader = ServerSentEventReader(framingLimits { maxEventBytes = SMALL })

            reader.feed("data: 12345\ndata: 12345\n".encodeToByteString()) shouldBe
                Outcome.Failure(FramingFailure.EventTooLong)
        }
    }

    context("an event") {
        test("shows its ID and reconnection time") {
            ServerSentEvent(data = DATA, id = "1", retry = RETRY).toString() shouldBe
                "ServerSentEvent(type=message, id=1, retry=5, data=x)"
        }

        test("differs in each of its parts") {
            val event = ServerSentEvent(data = DATA, id = "1", retry = RETRY)

            event shouldBe ServerSentEvent(data = DATA, id = "1", retry = RETRY)
            event shouldNotBe ServerSentEvent(data = "y", id = "1", retry = RETRY)
            event shouldNotBe ServerSentEvent(data = DATA, type = "other", id = "1", retry = RETRY)
            event shouldNotBe ServerSentEvent(data = DATA, id = "2", retry = RETRY)
            event shouldNotBe ServerSentEvent(data = DATA, id = "1")
            event.equals(DATA) shouldBe false
        }

        test("refuses an ID with a line break or NUL and a negative reconnection time") {
            shouldThrow<IllegalArgumentException> { ServerSentEvent(data = DATA, id = "a\nb") }
            shouldThrow<IllegalArgumentException> { ServerSentEvent(data = DATA, id = "a\u0000b") }
            shouldThrow<IllegalArgumentException> { ServerSentEvent(data = DATA, retry = -1L) }
        }
    }
})
