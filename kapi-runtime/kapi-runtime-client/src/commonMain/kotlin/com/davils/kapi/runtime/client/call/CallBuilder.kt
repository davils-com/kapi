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

package com.davils.kapi.runtime.client.call

import com.davils.kapi.runtime.client.internal.FieldText
import com.davils.kapi.runtime.core.codec.multipart.Boundary
import com.davils.kapi.runtime.core.header.IdempotencyKey
import com.davils.kapi.runtime.core.negotiation.MediaRange
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kapi.runtime.core.parameter.ParameterEntry
import com.davils.kapi.runtime.core.parameter.ParameterFormat
import com.davils.kapi.runtime.core.parameter.ParameterValue
import com.davils.kapi.runtime.core.parameter.entries
import com.davils.kapi.runtime.core.parameter.headerTextOrNull
import com.davils.kapi.runtime.core.parameter.pathSegmentOrNull
import com.davils.kore.core.annotation.DavilsDsl
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.io.bytestring.ByteString
import kotlin.time.Duration

private const val CHUNK: Int = 8_192

/**
 * Builds a [Call]; generated clients call it, and an interceptor changes a call through [Call.change].
 *
 * The builder never throws: a value that cannot be written safely, such as a path segment of `..` or a header value
 * with a line break, records the first [CallFailure] as [Call.refusal], and a refused call is never sent. Path
 * parameters are percent-encoded by their style; query parameters are encoded when the call is sent. A builder is not
 * thread-safe.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class CallBuilder internal constructor(internal val operation: String, internal val method: String) {
    private val path: StringBuilder = StringBuilder()

    internal val queryEntries: MutableList<ParameterEntry> = mutableListOf()

    internal val headerEntries: MutableList<ParameterEntry> = mutableListOf()

    internal val cookieEntries: MutableList<ParameterEntry> = mutableListOf()

    internal val accepted: MutableList<MediaRange> = mutableListOf()

    internal val alternatives: MutableList<List<SecurityUse>> = mutableListOf()

    internal var content: Body? = null

    internal var refusal: CallFailure? = null

    internal var limits: Timeout? = null

    internal var isMarkedIdempotent: Boolean = false

    internal var key: IdempotencyKey? = null

    internal var target: String? = null

    internal var streamed: Upload? = null

    internal var switched: Upgrade? = null

    internal val pathText: String get() = path.toString()

    /**
     * Sends the call to the absolute URL [url] instead of the base URL and the path, such as a webhook or a link.
     *
     * The URL passes the destination policy of the client when the call is sent.
     *
     * @param url the absolute URL.
     * @since 1.0.0
     */
    public fun destination(url: String) {
        target = url
    }

    /**
     * Appends [text] of the path template to the path as it is.
     *
     * @param text a literal part of the path, such as `/pets/`.
     * @since 1.0.0
     */
    public fun path(text: String) {
        path.append(text)
    }

    /**
     * Appends the path parameter [value] written in the style of [format].
     *
     * A value that would make the segment `.` or `..` refuses the call with [CallFailure.PathRefused].
     *
     * @param format the name and style of the parameter.
     * @param value the value.
     * @since 1.0.0
     */
    public fun path(format: ParameterFormat, value: ParameterValue) {
        val segment = format.pathSegmentOrNull(value)
        if (segment == null) refuse(CallFailure.PathRefused(format.name))
        path.append(segment.orEmpty())
    }

    /**
     * Adds the query parameter [value] written in the style of [format].
     *
     * @param format the name and style of the parameter.
     * @param value the value.
     * @since 1.0.0
     */
    public fun query(format: ParameterFormat, value: ParameterValue) {
        queryEntries += format.entries(value)
    }

    /**
     * Adds the query parameter [name] with [value] as it is.
     *
     * @param name the name.
     * @param value the value, percent-encoded when the call is sent.
     * @since 1.0.0
     */
    public fun query(name: String, value: String) {
        queryEntries += ParameterEntry(name, value)
    }

    /**
     * Adds the header parameter [value] written in the style of [format].
     *
     * A value a header field cannot carry, such as one with a line break, refuses the call with
     * [CallFailure.ParameterRefused].
     *
     * @param format the name and style of the parameter.
     * @param value the value.
     * @since 1.0.0
     */
    public fun header(format: ParameterFormat, value: ParameterValue) {
        val text = format.headerTextOrNull(value)
        if (text == null) refuse(CallFailure.ParameterRefused(format.name))
        text?.let { written -> header(name = format.name, value = written) }
    }

    /**
     * Adds the header field [name] with [value].
     *
     * A name that is no token or a value with a control character other than a tab refuses the call with
     * [CallFailure.ParameterRefused], which guards against header injection.
     *
     * @param name the name of the field.
     * @param value the value.
     * @since 1.0.0
     */
    public fun header(name: String, value: String) {
        val isSafe = FieldText.isToken(name) && FieldText.isValue(value)
        if (!isSafe) refuse(CallFailure.ParameterRefused(name))
        if (isSafe) headerEntries += ParameterEntry(name, value)
    }

    /**
     * Adds the cookie parameter [value] written in the style of [format].
     *
     * A name that is no token or a value outside the cookie octets of RFC 6265 §4.1.1, such as one with a semicolon
     * that would inject another cookie, refuses the call with [CallFailure.ParameterRefused].
     *
     * @param format the name and style of the parameter.
     * @param value the value.
     * @since 1.0.0
     */
    public fun cookie(format: ParameterFormat, value: ParameterValue) {
        val written = format.entries(value)
        val isSafe = written.all { entry -> FieldText.isToken(entry.name) && FieldText.isCookieValue(entry.value) }
        if (!isSafe) refuse(CallFailure.ParameterRefused(format.name))
        if (isSafe) cookieEntries += written
    }

    /**
     * Adds [range] to the media types the call accepts in an answer, in the order of the document.
     *
     * @param range the media range, such as `application/json`.
     * @since 1.0.0
     */
    public fun accept(range: MediaRange) {
        accepted += range
    }

    /**
     * Sends [body] as the content of the call.
     *
     * @param body the content.
     * @since 1.0.0
     */
    public fun body(body: Body) {
        content = body
    }

    /**
     * Sends [content] of the media type [contentType].
     *
     * @param contentType the media type.
     * @param content the content.
     * @since 1.0.0
     */
    public fun body(contentType: MediaType, content: ByteString) {
        body(Body(contentType, content))
    }

    /**
     * Streams [chunks] as the content of the call, which then is sent once.
     *
     * @param contentType the media type.
     * @param chunks the content.
     * @param length the number of bytes, or `null` to send the content in chunks.
     * @since 1.0.0
     */
    public fun upload(contentType: MediaType, chunks: Flow<ByteString>, length: Long? = null) {
        streamed = Upload(contentType, chunks, length)
    }

    /**
     * Streams what [channel] holds as the content of the call, which then is sent once.
     *
     * @param contentType the media type.
     * @param channel the content; it is read once, when the call is sent.
     * @param length the number of bytes, or `null` to send the content in chunks.
     * @since 1.0.0
     */
    public fun upload(contentType: MediaType, channel: ByteReadChannel, length: Long? = null) {
        val chunks = flow {
            val buffer = ByteArray(CHUNK)
            var count = channel.readAvailable(buffer)
            while (count >= 0) {
                emit(ByteString(buffer, startIndex = 0, endIndex = count))
                count = channel.readAvailable(buffer)
            }
        }
        upload(contentType = contentType, chunks = chunks, length = length)
    }

    /**
     * Switches the call to [protocol] and writes [input] after the switch.
     *
     * @param protocol the protocol of the `Upgrade` field.
     * @param input what the client writes after the switch.
     * @since 1.0.0
     */
    public fun upgrade(protocol: String, input: Flow<ByteString> = emptyFlow()) {
        switched = Upgrade(protocol, input)
    }

    /**
     * Sends the fields [configure] adds as `application/x-www-form-urlencoded`.
     *
     * @param configure adds the fields.
     * @since 1.0.0
     */
    public fun form(configure: FormBuilder.() -> Unit) {
        val builder = FormBuilder()
        builder.configure()
        content = builder.build()
    }

    /**
     * Sends the parts [configure] adds as `multipart/form-data` of RFC 7578.
     *
     * A part name or a file name with a control character refuses the call with [CallFailure.ParameterRefused].
     *
     * @param boundary the boundary between the parts; a random one by default, which no content can predict.
     * @param configure adds the parts.
     * @since 1.0.0
     */
    public fun multipart(boundary: Boundary = Boundary.random(), configure: MultipartBuilder.() -> Unit) {
        val builder = MultipartBuilder()
        builder.configure()
        content = builder.build(boundary)
        builder.refusal?.let(::refuse)
    }

    /**
     * Sets the time the call allows itself; a part left `null` is that of the client.
     *
     * @param request the time of the whole request.
     * @param connect the time to connect.
     * @param socket the longest pause between two reads or writes.
     * @since 1.0.0
     */
    public fun timeout(request: Duration? = null, connect: Duration? = null, socket: Duration? = null) {
        limits = Timeout(request = request, connect = connect, socket = socket)
    }

    /**
     * Declares the call idempotent although its method is not, so a resilience policy may retry it.
     *
     * @since 1.0.0
     */
    public fun idempotent() {
        isMarkedIdempotent = true
    }

    /**
     * Sends [key] in the field `Idempotency-Key`, so the server recognizes a retry of the call and a resilience policy
     * may retry it whatever its method.
     *
     * @param key the key; a random one by default, which every retry of the call repeats.
     * @since 1.0.0
     */
    public fun idempotencyKey(key: IdempotencyKey = IdempotencyKey.random()) {
        this.key = key
    }

    /**
     * Adds an alternative of the security requirement; the client meets the first alternative it can.
     *
     * @param alternative the schemes that must all be met, each with where its credential goes.
     * @since 1.0.0
     */
    public fun security(alternative: List<SecurityUse>) {
        alternatives += alternative
    }

    internal fun refuse(failure: CallFailure) {
        refusal = refusal ?: failure
    }

    internal fun clearQuery() {
        queryEntries.clear()
    }

    internal fun build(): Call = Call(this)

    internal companion object {
        fun from(call: Call): CallBuilder {
            val builder = CallBuilder(operation = call.operation, method = call.method)
            builder.path.append(call.path)
            builder.queryEntries += call.query
            builder.headerEntries += call.headers
            builder.cookieEntries += call.cookies
            builder.accepted += call.accept
            builder.alternatives += call.security
            builder.content = call.body
            builder.refusal = call.refusal
            builder.limits = call.timeout
            builder.isMarkedIdempotent = call.isIdempotent
            builder.key = call.idempotencyKey
            builder.target = call.destination
            builder.streamed = call.upload
            builder.switched = call.upgrade
            return builder
        }
    }
}
