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

@file:OptIn(DavilsInternal::class)

package com.davils.kapi.runtime.server.request

import com.davils.kapi.runtime.core.codec.form.FormUrlEncoding
import com.davils.kapi.runtime.core.codec.form.formLimits
import com.davils.kapi.runtime.core.header.FieldSyntax
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kapi.runtime.core.response.headersOf
import com.davils.kapi.runtime.server.ServerLimits
import com.davils.kapi.runtime.server.ServerSettings
import com.davils.kapi.runtime.server.ServerSettingsBuilder
import com.davils.kapi.runtime.server.internal.BodySource
import com.davils.kapi.runtime.server.internal.HeaderChecks
import com.davils.kapi.runtime.server.internal.ProblemReplies
import com.davils.kapi.runtime.server.internal.RequestLine
import com.davils.kapi.runtime.server.internal.RequestState
import com.davils.kapi.runtime.server.internal.ServerContext
import com.davils.kore.core.annotation.DavilsDsl
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.outcome.Outcome
import com.davils.kore.core.principal.Principal
import kotlinx.io.bytestring.ByteString

private const val CONTENT_TYPE: String = "Content-Type"

/**
 * Configures a [Request] that [Request.of] builds without Ktor; every part starts empty.
 *
 * A builder is not thread-safe and builds one request.
 *
 * @since 1.0.0
 */
@DavilsDsl
public class RequestBuilder internal constructor() {
    private val fields: MutableList<Pair<String, String>> = mutableListOf()

    private var configureServer: ServerSettingsBuilder.() -> Unit = {}

    /**
     * The query without its `?`, percent-encoded as it would be sent, such as `limit=5&tag=a%20b`, or `null` for
     * none.
     *
     * @since 1.0.0
     */
    public var rawQuery: String? = null

    /**
     * The values of the parameters of the path template, percent-decoded.
     *
     * @since 1.0.0
     */
    public var pathParameters: Map<String, String> = emptyMap()

    /**
     * The body; empty for none.
     *
     * @since 1.0.0
     */
    public var body: ByteString = ByteString()

    /**
     * What the server knows of the peer.
     *
     * @since 1.0.0
     */
    public var peer: Peer = Peer()

    /**
     * The principal the request was authorized as, or `null`.
     *
     * @since 1.0.0
     */
    public var principal: Principal? = null

    /**
     * The scheme of the connection; `https` by default.
     *
     * @since 1.0.0
     */
    public var scheme: String = HTTPS

    /**
     * The host and port the request was sent to; `localhost` by default.
     *
     * @since 1.0.0
     */
    public var authority: String = LOCALHOST

    /**
     * Adds the header field [name] with [value].
     *
     * @param name the name, a token.
     * @param value the value, without a control character other than a tab.
     * @throws IllegalArgumentException if [name] is no token or [value] holds a control character.
     * @since 1.0.0
     */
    public fun header(name: String, value: String) {
        require(FieldSyntax.isToken(name) && HeaderChecks.isValue(value)) {
            "A header field of a request has a token as its name and no control character in its value"
        }
        fields += name to value
    }

    /**
     * Sets [content] as the body and names its [mediaType] in `Content-Type`.
     *
     * @param mediaType the media type of the body.
     * @param content the body.
     * @since 1.0.0
     */
    public fun body(mediaType: MediaType, content: ByteString) {
        header(name = CONTENT_TYPE, value = mediaType.toString())
        body = content
    }

    /**
     * Configures the server whose codecs, limits and problems the request is read with, as `install(Kapi) { … }`
     * would; a later call replaces an earlier one.
     *
     * @param configure configures the server.
     * @since 1.0.0
     */
    public fun server(configure: ServerSettingsBuilder.() -> Unit) {
        configureServer = configure
    }

    internal fun build(operation: String, method: String, path: String): Request {
        val builder = ServerSettingsBuilder()
        builder.configureServer()
        val settings = builder.build()
        return Request(
            line = RequestLine(
                operation = operation,
                method = method,
                scheme = scheme,
                authority = authority,
                path = path,
                rawQuery = rawQuery,
            ),
            pathParameters = pathParameters,
            query = queryOf(rawQuery, settings.limits),
            headers = headersOf(entriesOf(fields), settings.sensitiveHeaders),
            peer = peer,
            principal = principal,
            body = BodySource.buffered(body, settings.limits),
            context = contextOf(settings),
            state = RequestState(),
        )
    }

    private fun contextOf(settings: ServerSettings): ServerContext = ServerContext(
        codecs = settings.codecs,
        limits = settings.limits,
        problems = ProblemReplies(settings.codecs, settings.problems),
        clock = settings.clock,
    )

    private fun entriesOf(fields: List<Pair<String, String>>): Map<String, List<String>> =
        fields.groupBy(keySelector = { (name, _) -> name }, valueTransform = { (_, value) -> value })

    private fun queryOf(rawQuery: String?, limits: ServerLimits): List<Pair<String, String>> {
        val text = rawQuery.orEmpty()
        require(text.length <= limits.maxQueryLength) { "The query is longer than the server allows" }
        val bounds = formLimits {
            maxLength = limits.maxQueryLength
            maxFields = limits.maxQueryParameters
            maxFieldLength = limits.maxQueryLength
        }
        val decoded = FormUrlEncoding.decode(text, bounds)
        require(decoded is Outcome.Success) { "The query has too many fields or is no percent-encoded UTF-8" }
        return decoded.value
    }

    private companion object {
        const val HTTPS: String = "https"

        const val LOCALHOST: String = "localhost"
    }
}
