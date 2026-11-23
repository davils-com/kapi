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

package com.davils.kapi.runtime.server.reply

import com.davils.kapi.runtime.core.cache.CacheControl
import com.davils.kapi.runtime.core.cache.CacheDirective
import com.davils.kapi.runtime.core.cache.Vary
import com.davils.kapi.runtime.core.codec.Codecs
import com.davils.kapi.runtime.core.conditional.EntityTag
import com.davils.kapi.runtime.core.conditional.HttpDate
import com.davils.kapi.runtime.core.framing.JsonLines
import com.davils.kapi.runtime.core.header.ContentDisposition
import com.davils.kapi.runtime.core.header.LinkValue
import com.davils.kapi.runtime.core.header.Links
import com.davils.kapi.runtime.core.header.RetryAfter
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kapi.runtime.core.ratelimit.RateLimit
import com.davils.kapi.runtime.core.ratelimit.RateLimitPolicy
import com.davils.kapi.runtime.core.ratelimit.RateLimits
import com.davils.kapi.runtime.server.internal.HeaderChecks
import com.davils.kapi.runtime.server.internal.StreamWriters
import com.davils.kore.core.annotation.DavilsDsl
import io.ktor.utils.io.ByteWriteChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.serialization.SerializationStrategy
import kotlin.time.Duration

private val textPlain: MediaType = MediaType.TextPlain.withParameter(name = "charset", value = "utf-8")

private val eventStream: MediaType = MediaType.EventStream.withParameter(name = "charset", value = "utf-8")

/**
 * Builds a [Reply]: its header fields and its body.
 *
 * Every helper writes a field value that the runtime core produced or checked, so it is a valid field value; a field
 * value from the application is checked by [header]. A later body replaces an earlier one. A builder is not
 * thread-safe.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class ReplyBuilder internal constructor(private val status: Int) {
    private val headers: MutableList<Pair<String, String>> = mutableListOf()

    private var body: ReplyBody = ReplyBody.Empty

    /**
     * Adds the header field [name] with [value], or nothing when [value] is `null`.
     *
     * @param name the name, a token.
     * @param value the value, or `null`.
     * @throws IllegalArgumentException if [name] is no token or reserved, or [value] holds a control character or a
     * line break, which could inject a field.
     * @since 1.0.0
     */
    public fun header(name: String, value: String?) {
        val text = value ?: return
        HeaderChecks.requireField(name, text)
        headers += name to text
    }

    /**
     * Sets the body to [value] written as [mediaType] by the codec of the server that covers it, such as the media
     * type `input.accept(...)` chose.
     *
     * @param T the type of the value.
     * @param mediaType the media type.
     * @param serializer the serializer of the value.
     * @param value the value.
     * @since 1.0.0
     */
    public fun <T> body(mediaType: MediaType, serializer: SerializationStrategy<T>, value: T) {
        body = ReplyBody.Value(mediaType) { codecs: Codecs ->
            val codec = checkNotNull(codecs.codecFor(mediaType)) { "No codec of the server writes the reply" }
            codec.encode(serializer, value)
        }
    }

    /**
     * Sets the body to [value] as `application/json`.
     *
     * @param T the type of the value.
     * @param serializer the serializer of the value.
     * @param value the value.
     * @since 1.0.0
     */
    public fun <T> json(serializer: SerializationStrategy<T>, value: T) {
        body(MediaType.Json, serializer, value)
    }

    /**
     * Sets the body to [content] of [mediaType].
     *
     * @param mediaType the media type.
     * @param content the bytes.
     * @since 1.0.0
     */
    public fun bytes(mediaType: MediaType, content: ByteString) {
        body = ReplyBody.Bytes(mediaType, content)
    }

    /**
     * Sets the body to [text] as `text/plain` in UTF-8.
     *
     * @param text the text.
     * @since 1.0.0
     */
    public fun text(text: String) {
        bytes(textPlain, text.encodeToByteString())
    }

    /**
     * Streams [chunks] as the body, each flushed as it comes.
     *
     * @param mediaType the media type.
     * @param chunks the content in pieces.
     * @param length the length of the content, or `null` to send it chunked.
     * @since 1.0.0
     */
    public fun stream(mediaType: MediaType, chunks: Flow<ByteString>, length: Long? = null) {
        body = ReplyBody.Stream(mediaType, length) { channel: ByteWriteChannel, _: Codecs ->
            StreamWriters.chunks(channel, chunks)
        }
    }

    /**
     * Streams [records] as JSON Lines, newline-delimited JSON or a JSON text sequence of RFC 7464, each record written
     * compactly by the JSON of the server and flushed as it comes.
     *
     * @param T the type of the records.
     * @param serializer the serializer of a record.
     * @param records the records.
     * @param mediaType `application/x-ndjson` by default, `application/jsonl` or `application/json-seq`.
     * @since 1.0.0
     */
    public fun <T> lines(
        serializer: SerializationStrategy<T>,
        records: Flow<T>,
        mediaType: MediaType = MediaType.NdJson,
    ) {
        val isSequence = mediaType.essence == MediaType.JsonSequence.essence
        val sequence: (String) -> ByteString = JsonLines::frameSequence
        val frame: (String) -> ByteString = sequence.takeIf { isSequence } ?: JsonLines::frame
        body = ReplyBody.Stream(mediaType, null) { channel: ByteWriteChannel, codecs: Codecs ->
            val framed = records.map { record -> frame(codecs.json.encodeToString(serializer, record)) }
            StreamWriters.chunks(channel, framed)
        }
    }

    /**
     * Streams server-sent events as `text/event-stream`, with a heartbeat comment every [heartbeat].
     *
     * The heartbeat runs in a child of the coroutine of the response, which Ktor owns; it ends with [produce]. The
     * reply carries `Cache-Control: no-cache`. A client that reconnects names the last event it saw in
     * [Request.lastEventId][com.davils.kapi.runtime.server.request.Request.lastEventId].
     *
     * @param heartbeat the time between two heartbeat comments, or `null` for none.
     * @param produce sends the events; the stream ends when it returns.
     * @throws IllegalArgumentException if [heartbeat] is not positive.
     * @since 1.0.0
     */
    public fun events(heartbeat: Duration? = null, produce: suspend EventEmitter.() -> Unit) {
        require(heartbeat == null || heartbeat.isPositive()) { "A heartbeat comes after a positive time" }
        cacheControl(CacheControl.of(CacheDirective.noCache()))
        body = ReplyBody.Stream(eventStream, null) { channel: ByteWriteChannel, _: Codecs ->
            StreamWriters.events(channel, heartbeat, produce)
        }
    }

    /**
     * Sets `Cache-Control` of RFC 9111 §5.2.
     *
     * @param cacheControl the directives.
     * @since 1.0.0
     */
    public fun cacheControl(cacheControl: CacheControl) {
        header(name = CACHE_CONTROL, value = cacheControl.toString())
    }

    /**
     * Sets `Vary` of RFC 9110 §12.5.5.
     *
     * @param vary the request fields the reply depends on.
     * @since 1.0.0
     */
    public fun vary(vary: Vary) {
        header(name = VARY, value = vary.toString())
    }

    /**
     * Sets `Link` of RFC 8288, such as the links to the next and the previous page.
     *
     * @param links the links.
     * @since 1.0.0
     */
    public fun links(links: List<LinkValue>) {
        if (links.isNotEmpty()) header(name = LINK, value = Links.format(links))
    }

    /**
     * Sets `Retry-After` of RFC 9110 §10.2.3, such as on 429 (Too Many Requests) or 503 (Service Unavailable).
     *
     * @param retryAfter the delay or the date.
     * @since 1.0.0
     */
    public fun retryAfter(retryAfter: RetryAfter) {
        header(name = RETRY_AFTER, value = retryAfter.toString())
    }

    /**
     * Sets `RateLimit` of draft-ietf-httpapi-ratelimit-headers.
     *
     * @param limits the states of the quotas.
     * @since 1.0.0
     */
    public fun rateLimits(limits: List<RateLimit>) {
        if (limits.isNotEmpty()) header(name = RATE_LIMIT, value = RateLimits.formatLimits(limits))
    }

    /**
     * Sets `RateLimit-Policy` of draft-ietf-httpapi-ratelimit-headers.
     *
     * @param policies the policies.
     * @since 1.0.0
     */
    public fun rateLimitPolicies(policies: List<RateLimitPolicy>) {
        if (policies.isNotEmpty()) header(name = RATE_LIMIT_POLICY, value = RateLimits.formatPolicies(policies))
    }

    /**
     * Sets `ETag` of RFC 9110 §8.8.3.
     *
     * @param entityTag the entity tag.
     * @since 1.0.0
     */
    public fun entityTag(entityTag: EntityTag) {
        header(name = ETAG, value = entityTag.toString())
    }

    /**
     * Sets `Last-Modified` of RFC 9110 §8.8.2.
     *
     * @param date the time of the last modification.
     * @since 1.0.0
     */
    public fun lastModified(date: HttpDate) {
        header(name = LAST_MODIFIED, value = date.toString())
    }

    /**
     * Sets `Content-Disposition` of RFC 6266, such as an attachment with a file name.
     *
     * @param disposition the disposition.
     * @since 1.0.0
     */
    public fun contentDisposition(disposition: ContentDisposition) {
        header(name = CONTENT_DISPOSITION, value = disposition.toString())
    }

    internal fun build(): Reply = Reply(status = status, headers = headers.toList(), body = body)

    internal companion object {
        const val CACHE_CONTROL: String = "Cache-Control"
        const val VARY: String = "Vary"
        const val LINK: String = "Link"
        const val RETRY_AFTER: String = "Retry-After"
        const val RATE_LIMIT: String = "RateLimit"
        const val RATE_LIMIT_POLICY: String = "RateLimit-Policy"
        const val ETAG: String = "ETag"
        const val LAST_MODIFIED: String = "Last-Modified"
        const val CONTENT_DISPOSITION: String = "Content-Disposition"
    }
}
