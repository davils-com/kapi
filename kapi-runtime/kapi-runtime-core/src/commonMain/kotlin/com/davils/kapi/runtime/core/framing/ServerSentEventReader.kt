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

import com.davils.kapi.runtime.core.framing.internal.Splitter
import com.davils.kapi.runtime.core.internal.FieldGrammar
import com.davils.kore.core.outcome.Outcome
import kotlinx.io.bytestring.ByteString

private const val CARRIAGE_RETURN: Byte = 0x0D

private const val LINE_FEED: Byte = 0x0A

private const val COMMENT: Char = ':'

private const val SEPARATOR: Char = ':'

private const val NUL: Char = '\u0000'

private const val MESSAGE: String = "message"

private const val MOST_RETRY_DIGITS: Int = 18

private val byteOrderMark: ByteArray = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

/**
 * Reads a `text/event-stream` by the event stream interpretation of the WHATWG HTML standard §9.2.6, as the chunks of
 * the stream arrive.
 *
 * Lines end with a carriage return, a line feed or both, also when the two arrive in different chunks; a byte order
 * mark at the start of the stream is dropped, and the text is decoded as UTF-8 with every malformed sequence replaced,
 * as the standard demands. Comments, the lines that start with a colon, and unknown fields are ignored. An `id` with a
 * NUL is ignored, and a `retry` that is not all digits. An event is dispatched by an empty line; one without data is
 * not dispatched. At the end of the stream the standard discards an event that was not dispatched: [finish] does so
 * and [isIncomplete] tells whether anything was discarded.
 *
 * A line longer than [FramingLimits.maxRecordBytes] fails the stream with [FramingFailure.RecordTooLong], data of one
 * event beyond [FramingLimits.maxEventBytes] with [FramingFailure.EventTooLong]; the reader then stays failed. A
 * reader holds the state of one stream and is not safe to share between threads.
 *
 * @param limits the limits of the stream.
 * @since 1.0.0
 */
public class ServerSentEventReader(private val limits: FramingLimits = FramingLimits.Default) {
    private val splitter: Splitter =
        Splitter(setOf(CARRIAGE_RETURN, LINE_FEED), limits.maxRecordBytes, isCrLfOne = true)

    private val data: StringBuilder = StringBuilder()

    private var dataBytes: Long = 0L

    private var eventType: String = ""

    private var isFirstLine: Boolean = true

    private var failure: FramingFailure? = null

    /**
     * The last event ID of the stream, empty until an `id` field sets one.
     *
     * @since 1.0.0
     */
    public var lastEventId: String = ""
        private set

    /**
     * The reconnection time in milliseconds the stream set, or `null` if it set none.
     *
     * @since 1.0.0
     */
    public var retry: Long? = null
        private set

    private val handlers: Map<String, (String, Int) -> Unit> = mapOf(
        "event" to { value, _ -> eventType = value },
        "data" to ::appendData,
        "id" to { value, _ -> if (NUL !in value) lastEventId = value },
        "retry" to { value, _ -> retryOf(value)?.let { milliseconds -> retry = milliseconds } },
    )

    /**
     * Whether a line or an event is pending, which [finish] discards.
     *
     * @since 1.0.0
     */
    public val isIncomplete: Boolean get() = splitter.hasPending || data.isNotEmpty()

    /**
     * Reads the next chunk of the stream.
     *
     * @param chunk the bytes that arrived, which may come from an attacker.
     * @return the events the chunk completed, or the failure of the stream.
     * @since 1.0.0
     */
    public fun feed(chunk: ByteString): Outcome<List<ServerSentEvent>, FramingFailure> {
        val events = ArrayList<ServerSentEvent>()
        if (failure == null) splitter.feed(chunk.toByteArray()) { bytes, _ -> line(bytes, events) }
        if (splitter.isOverlong) failure = failure ?: FramingFailure.RecordTooLong
        val failed = failure ?: return Outcome.Success(events)
        return Outcome.Failure(failed)
    }

    /**
     * Ends the stream and discards an event that was not dispatched, as the standard demands.
     *
     * @return no events, or the failure of the stream.
     * @since 1.0.0
     */
    public fun finish(): Outcome<List<ServerSentEvent>, FramingFailure> {
        val failed = failure ?: return Outcome.Success(emptyList())
        return Outcome.Failure(failed)
    }

    private fun line(bytes: ByteArray, events: MutableList<ServerSentEvent>) {
        val isMarked = isFirstLine &&
            bytes.size >= byteOrderMark.size &&
            bytes.copyOf(byteOrderMark.size).contentEquals(byteOrderMark)
        isFirstLine = false
        val content = bytes.copyOfRange(fromIndex = byteOrderMark.size.takeIf { isMarked } ?: 0, toIndex = bytes.size)
        val text = content.decodeToString()
        if (text.isEmpty()) dispatch(events)
        if (text.isNotEmpty() && text.first() != COMMENT) field(text, content.size)
    }

    private fun field(text: String, size: Int) {
        val name = text.substringBefore(SEPARATOR)
        val value = text.substringAfter(SEPARATOR, "").removePrefix(" ")
        handlers[name]?.invoke(value, size)
    }

    private fun appendData(value: String, size: Int) {
        dataBytes += size
        if (dataBytes > limits.maxEventBytes) failure = failure ?: FramingFailure.EventTooLong
        if (failure == null) data.append(value).append('\n')
    }

    private fun retryOf(value: String): Long? {
        val isNumber = value.length in 1..MOST_RETRY_DIGITS && value.all(FieldGrammar::isDigit)
        return value.takeIf { isNumber }?.toLong()
    }

    private fun dispatch(events: MutableList<ServerSentEvent>) {
        val text = data.toString().removeSuffix("\n")
        val type = eventType.ifEmpty { MESSAGE }
        val isDispatched = data.isNotEmpty() && failure == null
        data.clear()
        dataBytes = 0L
        eventType = ""
        if (isDispatched) {
            events += ServerSentEvent(
                data = text,
                type = type,
                id = lastEventId.ifEmpty { null },
                retry = retry,
            )
        }
    }
}
