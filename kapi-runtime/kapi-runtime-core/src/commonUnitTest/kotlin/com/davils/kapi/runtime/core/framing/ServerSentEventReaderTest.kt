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

private const val LF: String = "\n"

private const val CR: String = "\r"

private const val RETRY: Long = 3_000L

private const val TEST: String = "test"

private const val UPDATE: String = "update"

private const val DATA_TEST: String = "data: test"

private const val DATA_X: String = "data: x"

private const val SMALL: Int = 8

private val byteOrderMark: ByteArray = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

private fun lines(vararg lines: String): String = lines.joinToString(LF)

private fun stream(vararg lines: String): String = lines.joinToString(separator = LF, postfix = LF)

private fun read(
    vararg chunks: String,
    reader: ServerSentEventReader = ServerSentEventReader(),
): List<ServerSentEvent> = chunks.flatMap { chunk ->
    val outcome = reader.feed(chunk.encodeToByteString())
    outcome.getOrNull().shouldNotBeNull()
}

private fun event(data: String, id: String? = null): ServerSentEvent = ServerSentEvent(data = data, id = id)

class ServerSentEventReaderTest : FunSpec({
    context("the examples of the WHATWG HTML standard §9.2.6") {
        test("join several data lines with line feeds") {
            read(stream("data: YHOO", "data: +2", "data: 10", "")) shouldBe listOf(event(lines("YHOO", "+2", "10")))
        }

        test("skip comments, keep the last event ID and strip one leading space only") {
            val text = stream(
                ": test stream",
                "",
                "data: first event",
                "id: 1",
                "",
                "data:second event",
                "id",
                "",
                "data:  third event",
                "",
            )

            read(text) shouldBe listOf(event("first event", id = "1"), event("second event"), event(" third event"))
        }

        test("dispatch empty data and discard an event without its empty line") {
            val reader = ServerSentEventReader()

            read(lines("data", "", "data", "data", "", "data:"), reader = reader) shouldBe
                listOf(event(""), event(LF))
            reader.isIncomplete shouldBe true
            reader.finish() shouldBe Outcome.Success(emptyList())
        }

        test("read data with and without a space alike") {
            read(stream("data:test", "", DATA_TEST, "")) shouldBe listOf(event(TEST), event(TEST))
        }
    }

    context("lines") {
        test("end with CR, LF or CRLF, also when the two arrive apart") {
            read("data: a$CR$CR", "data: b$CR", "$LF$CR$LF", stream("data: c", "")) shouldBe
                listOf(event("a"), event("b"), event("c"))
        }

        test("lose a byte order mark at the start of the stream only") {
            val reader = ServerSentEventReader()
            val first = ByteString(byteOrderMark + stream(DATA_X, "").encodeToByteArray())
            val second = ByteString(byteOrderMark + stream("data: y", "").encodeToByteArray())

            reader.feed(first) shouldBe Outcome.Success(listOf(event("x")))
            reader.feed(second) shouldBe Outcome.Success(emptyList())
        }

        test("replace malformed UTF-8") {
            val reader = ServerSentEventReader()
            val bytes = "data: ".encodeToByteArray() + byteArrayOf(0xFF.toByte()) + stream("", "").encodeToByteArray()

            reader.feed(ByteString(bytes)) shouldBe Outcome.Success(listOf(event("�")))
        }
    }

    context("fields") {
        test("set the type, the reconnection time and the ID") {
            val reader = ServerSentEventReader()
            val text = stream("event: update", "retry: 3000", "id: 7", DATA_X, "", "data: y", "")

            read(text, reader = reader) shouldBe listOf(
                ServerSentEvent(data = "x", type = UPDATE, id = "7", retry = RETRY),
                ServerSentEvent(data = "y", id = "7", retry = RETRY),
            )
            reader.lastEventId shouldBe "7"
            reader.retry shouldBe RETRY
        }

        test("ignore an ID with NUL, a retry that is no number and unknown fields") {
            val text = stream("id: 1", "id: a\u0000b", "retry: 1s", "retry", "foo: bar", DATA_X, "")

            read(text) shouldBe listOf(event("x", id = "1"))
        }
    }

    context("limits") {
        test("refuse a line that is too long and stay failed") {
            val reader = ServerSentEventReader(framingLimits { maxRecordBytes = SMALL })
            val refused = Outcome.Failure(FramingFailure.RecordTooLong)

            reader.feed(stream("data: 12", "").encodeToByteString()) shouldBe Outcome.Success(listOf(event("12")))
            reader.feed(stream("data: 123", "").encodeToByteString()) shouldBe refused
            reader.feed(stream("data: 1", "").encodeToByteString()) shouldBe refused
            reader.finish() shouldBe refused
        }

        test("refuse data of one event that grows too large") {
            val reader = ServerSentEventReader(framingLimits { maxEventBytes = SMALL * 2 })
            val two = stream("data: a", "data: b", "")
            val three = stream("data: a", "data: b", "data: c", "")

            reader.feed(two.encodeToByteString()) shouldBe Outcome.Success(listOf(event(lines("a", "b"))))
            reader.feed(three.encodeToByteString()) shouldBe Outcome.Failure(FramingFailure.EventTooLong)
        }
    }

    context("the writer") {
        test("writes every field and splits data into lines that read back") {
            val data = "line 1\r\nline 2${CR}line 3$LF"
            val event = ServerSentEvent(data = data, type = UPDATE, id = "42", retry = RETRY)
            val text = ServerSentEvents.encode(event)
            val written = stream(
                "event: update",
                "id: 42",
                "retry: 3000",
                "data: line 1",
                "data: line 2",
                "data: line 3",
                "data: ",
                "",
            )
            val back = ServerSentEvent(
                data = stream("line 1", "line 2", "line 3"),
                type = UPDATE,
                id = "42",
                retry = RETRY,
            )

            text shouldBe written
            read(text) shouldBe listOf(back)
            ServerSentEvents.encode(event(TEST)) shouldBe stream(DATA_TEST, "")
            ServerSentEvents.frame(event(TEST)) shouldBe stream(DATA_TEST, "").encodeToByteString()
            ServerSentEvents.comment("ping") shouldBe stream(":ping")
        }

        test("refuses what would end an event early or inject a field") {
            shouldThrow<IllegalArgumentException> { ServerSentEvent(data = TEST, type = "a${LF}data: x") }
            shouldThrow<IllegalArgumentException> { ServerSentEvent(data = TEST, id = "1\r\nevent: x") }
            shouldThrow<IllegalArgumentException> { ServerSentEvent(data = TEST, id = "a\u0000") }
            shouldThrow<IllegalArgumentException> { ServerSentEvent(data = TEST, retry = -1L) }
            shouldThrow<IllegalArgumentException> { ServerSentEvents.comment(lines("a", "", DATA_X)) }
        }

        test("compares events by value and shows them escaped") {
            event(TEST).hashCode() shouldBe event(TEST).hashCode()
            event(lines("a", "b")).toString() shouldNotContain LF
            event(TEST, id = "1").toString() shouldBe "ServerSentEvent(type=message, id=1, retry=none, data=test)"
        }
    }
})
