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

package com.davils.kapi.runtime.client.stream

import com.davils.kapi.runtime.client.ApiClient
import com.davils.kapi.runtime.client.ClientEvent
import com.davils.kapi.runtime.client.call.Call
import com.davils.kapi.runtime.client.call.CallFailure
import com.davils.kapi.runtime.client.internal.FieldText
import com.davils.kapi.runtime.client.internal.Lines
import com.davils.kapi.runtime.client.internal.Records
import com.davils.kapi.runtime.client.internal.StreamSink
import com.davils.kapi.runtime.core.codec.Decoding
import com.davils.kapi.runtime.core.framing.FramingFailure
import com.davils.kapi.runtime.core.framing.ServerSentEvent
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kapi.runtime.core.response.Headers
import com.davils.kore.core.outcome.Outcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.serialization.DeserializationStrategy

private const val MALFORMED: String = "An item of the stream is not what the document declares"

private const val LAST_EVENT_ID: String = "Last-Event-ID"

private const val CONTENT_TYPE: String = "content-type"

private const val NO_CONTENT: Int = 204

private val json: String = MediaType.Json.toString()

/**
 * Streams the content of [call] as the chunks the server sends, without holding it in memory.
 *
 * The time of the whole request does not apply; the time between two reads does. A call that carries a refusal, has
 * no credential or cannot reach the server ends at once.
 *
 * @receiver the client.
 * @param call the call; it is sent once, without retries.
 * @return the chunks of a successful answer, then [StreamEvent.Ended]; only [StreamEvent.Ended] with the whole answer
 * when the server refused.
 * @since 1.0.0
 */
public fun ApiClient.bytes(call: Call): Flow<StreamEvent<ByteString>> = channelFlow {
    val sink = StreamSink(
        accept = { chunk ->
            send(StreamEvent.Item(chunk))
            null
        },
    )
    send(StreamEvent.Ended(withContext(sink) { streamed(call) }))
}

/**
 * Streams the content of [call] as lines ended by a line feed, with a carriage return before it removed.
 *
 * A line longer than the `maxRecordBytes` of the framing limits of the client ends the stream with
 * [CallFailure.TooLarge], after the lines before it; nothing is cut silently.
 *
 * @receiver the client.
 * @param call the call; it is sent once, without retries.
 * @return the lines, then [StreamEvent.Ended].
 * @since 1.0.0
 */
public fun ApiClient.lines(call: Call): Flow<StreamEvent<String>> = channelFlow {
    val limit = settings.framing.maxRecordBytes
    val lines = Lines(limit)
    val sink = StreamSink(
        accept = { chunk ->
            lines.accept(chunk.toByteArray()).forEach { line -> send(StreamEvent.Item(line)) }
            CallFailure.TooLarge(limit.toLong()).takeIf { lines.isOverLimit }
        },
        finish = {
            lines.rest().forEach { line -> send(StreamEvent.Item(line)) }
            null
        },
    )
    send(StreamEvent.Ended(withContext(sink) { streamed(call) }))
}

/**
 * Streams the content of [call] as JSON records of [framing], each read as [deserializer] by the codecs of the client.
 *
 * A record that is not what [deserializer] expects is [StreamEvent.Malformed] and the stream goes on; a record longer
 * than the framing limits of the client or text that is no UTF-8 ends the stream with [CallFailure.Framing].
 *
 * @receiver the client.
 * @param T the type of the records.
 * @param deserializer the deserializer of a record.
 * @param call the call; it is sent once, without retries.
 * @param framing how the records are framed; JSON Lines by default.
 * @return the records, then [StreamEvent.Ended].
 * @since 1.0.0
 */
public fun <T> ApiClient.records(
    deserializer: DeserializationStrategy<T>,
    call: Call,
    framing: RecordFraming = RecordFraming.JsonLines,
): Flow<StreamEvent<T>> = channelFlow {
    val records = Records(framing, settings.framing)
    val deliver: suspend (Outcome<List<String>, FramingFailure>) -> CallFailure? = { outcome ->
        handled(outcome) { record -> send(decoded(deserializer = deserializer, text = record, id = null, type = null)) }
    }
    val sink = StreamSink(accept = { chunk -> deliver(records.feed(chunk)) }, finish = { deliver(records.finish()) })
    send(StreamEvent.Ended(withContext(sink) { streamed(call) }))
}

/**
 * Streams the server-sent events of [call] by the event source of the WHATWG HTML standard.
 *
 * The answer is read as a stream only when it is successful and of the media type `text/event-stream`; an event
 * longer than the framing limits of the client ends the stream with [CallFailure.Framing]. With [reconnection], the
 * stream reconnects as [Reconnection] describes, with `Last-Event-ID`, the `retry` of the server and a bounded number
 * of reconnections, each reported as [ClientEvent.Reconnecting].
 *
 * @receiver the client.
 * @param call the call; it is sent once per connection, without retries.
 * @param reconnection how the stream reconnects, or `null` to end it with its first connection.
 * @return the events with their IDs and types, then [StreamEvent.Ended] with how the last connection ended.
 * @since 1.0.0
 */
public fun ApiClient.events(call: Call, reconnection: Reconnection? = null): Flow<StreamEvent<ServerSentEvent>> =
    channelFlow {
        val source = EventSource(client = this@events, call = call)
        val deliver: suspend (ServerSentEvent) -> Unit = { event ->
            send(StreamEvent.Item(event, id = event.id, type = event.type))
        }
        val ended = reconnection?.let { found -> source.reconnecting(found, deliver) } ?: source.connect(deliver)
        send(StreamEvent.Ended(ended))
    }

/**
 * Streams the server-sent events of [call] like [events], with the data of each read as [deserializer] by the JSON
 * codec of the client.
 *
 * Data that is not what [deserializer] expects is [StreamEvent.Malformed] and the stream goes on.
 *
 * @receiver the client.
 * @param T the type of the data.
 * @param deserializer the deserializer of the data of an event.
 * @param call the call.
 * @param reconnection how the stream reconnects, or `null`.
 * @return the events, then [StreamEvent.Ended].
 * @since 1.0.0
 */
public fun <T> ApiClient.events(
    deserializer: DeserializationStrategy<T>,
    call: Call,
    reconnection: Reconnection? = null,
): Flow<StreamEvent<T>> = events(call, reconnection).map { event ->
    val ended = event as? StreamEvent.Ended
    ended ?: (event as StreamEvent.Item<ServerSentEvent>).let { found ->
        decoded(deserializer = deserializer, text = found.value.data, id = found.id, type = found.type)
    }
}

private fun <T> ApiClient.decoded(
    deserializer: DeserializationStrategy<T>,
    text: String,
    id: String?,
    type: String?,
): StreamEvent<T> = when (val decoding = settings.codecs.decode(deserializer, text.encodeToByteString(), json)) {
    is Decoding.Decoded -> StreamEvent.Item(decoding.value, id = id, type = type)
    is Decoding.Failed -> StreamEvent.Malformed(MALFORMED)
}

internal suspend fun <R> handled(outcome: Outcome<List<R>, FramingFailure>, each: suspend (R) -> Unit): CallFailure? =
    when (outcome) {
        is Outcome.Failure -> {
            CallFailure.Framing(outcome.failure)
        }

        is Outcome.Success -> {
            outcome.value.forEach { item -> each(item) }
            null
        }
    }

internal fun resumed(call: Call, lastId: String): Call {
    val isWritable = lastId.isNotEmpty() && FieldText.isValue(lastId)
    if (!isWritable) return call
    return call.change { header(name = LAST_EVENT_ID, value = lastId) }
}

internal fun isEventStream(status: Int, headers: Headers): Boolean {
    val type = headers[CONTENT_TYPE]?.let(MediaType::parseOrNull)
    return StreamSink.isOpened(status) && status != NO_CONTENT && type?.essence == MediaType.EventStream.essence
}
