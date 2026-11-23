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
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString

private const val RETRY: Long = 3_000L

private const val TEST: String = "test"

private const val SMALL: Int = 8

private fun read(vararg chunks: String, reader: ServerSentEventReader = ServerSentEventReader()): List<ServerSentEvent> =
    chunks.flatMap { chunk -> reader.feed(chunk.encodeToByteString()).getOrNull().shouldNotBeNull() }

class ServerSentEventReaderTest : FunSpec({
    context("the examples of the WHATWG HTML standard §9.2.6") {
        test("join several data lines with line feeds") {
            read("data: YHOO\ndata: +2\ndata: 10\n\n") shouldBe listOf(ServerSentEvent("YHOO\n+2\n10"))
        }

        test("skip comments, keep the last event ID and strip one leading space only") {
            val stream = ": test stream\n\ndata: first event\nid: 1\n\ndata:second event\nid\n\ndata:  third event\n\n"

            read(stream) shouldBe listOf(
                ServerSentEvent("first event", id = "1"),
                ServerSentEvent("second event"),
                ServerSentEvent(" third event"),
            )
        }

        test("dispatch empty data and discard an event without its empty line") {
            val reader = ServerSentEventReader()

            read("data\n\ndata\ndata\n\ndata:", reader = reader) shouldBe
                listOf(ServerSentEvent(""), ServerSentEvent("\n"))
            reader.isIncomplete shouldBe true
            reader.finish() shouldBe Outcome.Success(emptyList())
        }

        test("read data with and without a space alike") {
            read("data:test\n\ndata: test\n\n") shouldBe listOf(ServerSentEvent(TEST), ServerSentEvent(TEST))
        }
    }

    context("lines") {
        test("end with CR, LF or CRLF, also when the two arrive apart") {
            read("data: a\r\r", "data: b\r", "\n\r\n", "data: c\n\n") shouldBe
                listOf(ServerSentEvent("a"), ServerSentEvent("b"), ServerSentEvent("c"))
        }

        test("lose a byte order mark at the start of the stream only") {
            val bom = ByteString(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            val reader = ServerSentEventReader()
            val events = reader.feed(ByteString(bom.toByteArray() + "data: x\n\n".encodeToByteArray()))

            events shouldBe Outcome.Success(listOf(ServerSentEvent("x")))
            reader.feed(ByteString(bom.toByteArray() + "data: y\n\n".encodeToByteArray())) shouldBe
                Outcome.Success(emptyList())
        }

        test("replace malformed UTF-8") {
            val reader = ServerSentEventReader()
            val bytes = "data: ".encodeToByteArray() + byteArrayOf(0xFF.toByte()) + "\n\n".encodeToByteArray()

            reader.feed(ByteString(bytes)) shouldBe Outcome.Success(listOf(ServerSentEvent("�")))
        }
    }

    context("fields") {
        test("set the type, the reconnection time and the ID") {
            val reader = ServerSentEventReader()
            val events = read("event: update\nretry: 3000\nid: 7\ndata: x\n\ndata: y\n\n", reader = reader)

            events shouldBe listOf(
                ServerSentEvent("x", type = "update", id = "7", retry = RETRY),
                ServerSentEvent("y", id = "7", retry = RETRY),
            )
            reader.lastEventId shouldBe "7"
            reader.retry shouldBe RETRY
        }

        test("ignore an ID with NUL, a retry that is no number and unknown fields") {
            read("id: 1\nid: a\u0000b\nretry: 1s\nretry\nfoo: bar\ndata: x\n\n") shouldBe
                listOf(ServerSentEvent("x", id = "1"))
        }
    }

    context("limits") {
        test("refuse a line that is too long and stay failed") {
            val reader = ServerSentEventReader(framingLimits { maxRecordBytes = SMALL })

            reader.feed("data: 12\n\n".encodeToByteString()) shouldBe Outcome.Success(listOf(ServerSentEvent("12")))
            reader.feed("data: 123\n\n".encodeToByteString()) shouldBe Outcome.Failure(FramingFailure.RecordTooLong)
            reader.feed("data: 1\n\n".encodeToByteString()) shouldBe Outcome.Failure(FramingFailure.RecordTooLong)
            reader.finish() shouldBe Outcome.Failure(FramingFailure.RecordTooLong)
        }

        test("refuse data of one event that grows too large") {
            val reader = ServerSentEventReader(framingLimits { maxEventBytes = SMALL * 2 })

            reader.feed("data: a\ndata: b\n\n".encodeToByteString()) shouldBe
                Outcome.Success(listOf(ServerSentEvent("a\nb")))
            reader.feed("data: a\ndata: b\ndata: c\n\n".encodeToByteString()) shouldBe
                Outcome.Failure(FramingFailure.EventTooLong)
        }
    }

    context("the writer") {
        test("writes every field and splits data into lines that read back") {
            val event = ServerSentEvent("line 1\r\nline 2\rline 3\n", type = "update", id = "42", retry = RETRY)
            val text = ServerSentEvents.encode(event)

            text shouldBe "event: update\nid: 42\nretry: 3000\ndata: line 1\ndata: line 2\ndata: line 3\ndata: \n\n"
            read(text) shouldBe listOf(ServerSentEvent("line 1\nline 2\nline 3\n", "update", "42", RETRY))
            ServerSentEvents.encode(ServerSentEvent(TEST)) shouldBe "data: test\n\n"
            ServerSentEvents.frame(ServerSentEvent(TEST)) shouldBe "data: test\n\n".encodeToByteString()
            ServerSentEvents.comment("ping") shouldBe ":ping\n"
        }

        test("refuses what would end an event early or inject a field") {
            shouldThrow<IllegalArgumentException> { ServerSentEvent(TEST, type = "a\ndata: x") }
            shouldThrow<IllegalArgumentException> { ServerSentEvent(TEST, id = "1\r\nevent: x") }
            shouldThrow<IllegalArgumentException> { ServerSentEvent(TEST, id = "a\u0000") }
            shouldThrow<IllegalArgumentException> { ServerSentEvent(TEST, retry = -1L) }
            shouldThrow<IllegalArgumentException> { ServerSentEvents.comment("a\n\ndata: x") }
        }

        test("compares events by value and shows them escaped") {
            ServerSentEvent(TEST).hashCode() shouldBe ServerSentEvent(TEST).hashCode()
            ServerSentEvent("a\nb").toString() shouldNotContain "\n"
            ServerSentEvent(TEST, id = "1").toString() shouldBe "ServerSentEvent(type=message, id=1, retry=null, data=test)"
        }
    }
})
