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

package com.davils.kapi.runtime.testing.mock

import com.davils.kapi.runtime.core.codec.Codecs
import com.davils.kapi.runtime.core.codec.Decoding
import com.davils.kapi.runtime.core.framing.JsonLines
import com.davils.kapi.runtime.core.framing.ServerSentEvent
import com.davils.kapi.runtime.core.framing.ServerSentEvents
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kapi.runtime.core.problem.Problem
import com.davils.kore.core.annotation.DavilsDsl
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.ByteStringBuilder
import kotlinx.io.bytestring.append
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.serializer

private const val FIRST_STATUS: Int = 100

private const val LAST_STATUS: Int = 599

private const val OK: Int = 200

private const val SERVER_ERROR: Int = 500

/**
 * The scope of a route of a [MockApi]: what the request asked for and the calls that answer it.
 *
 * The last `respond` call of a route wins; a route that calls none answers `500` with a problem that says so. Bodies
 * are written through the [codecs] of the mock API, so JSON, CBOR, forms and text come out exactly as the codecs of
 * the client runtime read them. A responder belongs to one request and is not shared between threads.
 *
 * @property request the request being answered.
 * @property codecs the codecs that write and read bodies.
 * @param parameters the values of the placeholders of the path template, percent-decoded.
 * @since 1.0.0
 */
@DavilsDsl
public class Responder internal constructor(
    public val request: RecordedRequest,
    public val codecs: Codecs,
    parameters: List<Pair<String, String>>,
) {
    private val added: MutableList<Pair<String, String>> = mutableListOf()

    internal var answer: Answer? = null
        private set

    internal val headers: List<Pair<String, String>> get() = added.toList()

    /**
     * The values of the placeholders of the path template by name, percent-decoded, such as `id` to `42` for
     * `/pets/{id}`.
     *
     * @since 1.0.0
     */
    public val pathParameters: Map<String, String> = parameters.toMap()

    /**
     * Adds the header [key] with [value] to the response; a header may be added several times.
     *
     * @param key the name of the header.
     * @param value the value.
     * @since 1.0.0
     */
    public fun header(key: String, value: String) {
        added += key to value
    }

    /**
     * Answers with [status] and [content] of [mediaType].
     *
     * @param status the status, between 100 and 599.
     * @param content the body; empty by default.
     * @param mediaType the content type of the body, or `null` for none.
     * @throws IllegalArgumentException if [status] lies outside 100 to 599.
     * @since 1.0.0
     */
    public fun respond(status: Int, content: ByteString = ByteString(), mediaType: MediaType? = null) {
        require(status in FIRST_STATUS..LAST_STATUS) { "A status lies between 100 and 599: $status" }
        answer = Answer(status = status, contentType = mediaType?.toString(), content = content)
    }

    /**
     * Answers with [status] and [value] written by the codec of [mediaType].
     *
     * @param T the type of [value].
     * @param status the status, between 100 and 599.
     * @param serializer the serializer of [value].
     * @param value the value.
     * @param mediaType the content type of the body; JSON by default.
     * @throws IllegalArgumentException if [status] lies outside 100 to 599 or no codec writes [mediaType].
     * @since 1.0.0
     */
    public fun <T> respond(
        status: Int,
        serializer: SerializationStrategy<T>,
        value: T,
        mediaType: MediaType = MediaType.Json,
    ) {
        val codec = requireNotNull(codecs.codecFor(mediaType)) { "No codec writes $mediaType" }
        respond(status = status, content = codec.encode(serializer, value), mediaType = mediaType)
    }

    /**
     * Answers with [status] and [value] as JSON, through the serializer of [T].
     *
     * @param T the type of [value], which has a serializer.
     * @param status the status, between 100 and 599.
     * @param value the value.
     * @throws IllegalArgumentException if [status] lies outside 100 to 599.
     * @throws kotlinx.serialization.SerializationException if [T] has no serializer.
     * @since 1.0.0
     */
    public inline fun <reified T> respond(status: Int, value: T) {
        respond(status = status, serializer = serializer<T>(), value = value, mediaType = MediaType.Json)
    }

    /**
     * Answers with [status] and [text] of [mediaType].
     *
     * @param status the status, between 100 and 599.
     * @param text the body.
     * @param mediaType the content type; `text/plain` by default.
     * @throws IllegalArgumentException if [status] lies outside 100 to 599.
     * @since 1.0.0
     */
    public fun respondText(status: Int, text: String, mediaType: MediaType = MediaType.TextPlain) {
        respond(status = status, content = text.encodeToByteString(), mediaType = mediaType)
    }

    /**
     * Answers with a problem of RFC 9457 as `application/problem+json`.
     *
     * @param problem the problem.
     * @param status the status; the status of [problem] or 500 by default.
     * @throws IllegalArgumentException if [status] lies outside 100 to 599.
     * @since 1.0.0
     */
    public fun respondProblem(problem: Problem, status: Int = problem.status ?: SERVER_ERROR) {
        val text = codecs.json.encodeToString(Problem.serializer(), problem)
        respond(status = status, content = text.encodeToByteString(), mediaType = MediaType.ProblemJson)
    }

    /**
     * Answers with a stream of server-sent events as `text/event-stream`; the whole stream is one body that ends
     * after the last event.
     *
     * @param events the events.
     * @param status the status; 200 by default.
     * @throws IllegalArgumentException if [status] lies outside 100 to 599.
     * @since 1.0.0
     */
    public fun respondEvents(events: List<ServerSentEvent>, status: Int = OK) {
        val frames = events.map(ServerSentEvents::frame)
        respond(status = status, content = joined(frames), mediaType = MediaType.EventStream)
    }

    /**
     * Answers with records written as JSON, one per line for NDJSON and JSON Lines or framed by record separators for
     * `application/json-seq`.
     *
     * @param T the type of a record.
     * @param serializer the serializer of a record.
     * @param records the records.
     * @param mediaType the content type; `application/x-ndjson` by default.
     * @param status the status; 200 by default.
     * @throws IllegalArgumentException if [status] lies outside 100 to 599.
     * @since 1.0.0
     */
    public fun <T> respondRecords(
        serializer: SerializationStrategy<T>,
        records: List<T>,
        mediaType: MediaType = MediaType.NdJson,
        status: Int = OK,
    ) {
        val isSequence = mediaType.essence == MediaType.JsonSequence.essence
        val frames = records.map { record ->
            val text = codecs.json.encodeToString(serializer, record)
            JsonLines.frameSequence(text).takeIf { isSequence } ?: JsonLines.frame(text)
        }
        respond(status = status, content = joined(frames), mediaType = mediaType)
    }

    /**
     * Decodes the body of the request through the codecs, by its `Content-Type`.
     *
     * @param T the type of the body.
     * @param deserializer the deserializer.
     * @return the decoded value, or why it could not be decoded.
     * @since 1.0.0
     */
    public fun <T> body(deserializer: DeserializationStrategy<T>): Decoding<T> =
        codecs.decode(deserializer, request.body, request.contentType)

    /**
     * Decodes the body of the request as [T] through the codecs.
     *
     * @param T the type of the body, which has a serializer.
     * @return the decoded value, or `null` if the body cannot be decoded as [T].
     * @since 1.0.0
     */
    public inline fun <reified T> body(): T? {
        val decoding = body(serializer<T>())
        return (decoding as? Decoding.Decoded<T>)?.value
    }

    private fun joined(frames: List<ByteString>): ByteString {
        val builder = ByteStringBuilder()
        frames.forEach { frame -> builder.append(frame) }
        return builder.toByteString()
    }
}
