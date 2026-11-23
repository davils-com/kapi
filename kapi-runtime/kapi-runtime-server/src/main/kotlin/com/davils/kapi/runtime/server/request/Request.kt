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

package com.davils.kapi.runtime.server.request

import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kapi.runtime.core.response.Headers
import com.davils.kapi.runtime.server.internal.BodySource
import com.davils.kapi.runtime.server.internal.HeaderChecks
import com.davils.kapi.runtime.server.internal.PathTemplates
import com.davils.kapi.runtime.server.internal.RequestLine
import com.davils.kapi.runtime.server.internal.RequestState
import com.davils.kapi.runtime.server.internal.ServerContext
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.principal.Principal
import com.davils.kore.core.text.SafeText
import kotlinx.io.bytestring.ByteString

private const val LAST_EVENT_ID: String = "last-event-id"

private const val CONTENT_TYPE: String = "content-type"

private const val MOST_EVENT_ID_LENGTH: Int = 1_024

/**
 * A request of an operation as the server read it: its method, its path, its query, its header fields, its peer, the
 * principal it was authorized as and its body, which is read once through [input].
 *
 * Everything a request carries may come from an attacker. The query was read within the limits of the server, the
 * header fields mask the credentials they carry in [Headers.toString], and [toString] shows the method and the
 * operation only. A request is immutable but for its body, which is read at most once and is not safe to read from
 * two coroutines.
 *
 * @param line the operation, the method and the target of the request.
 * @param pathParameters the values of the parameters of the path template, percent-decoded.
 * @param query the names and values of the query in order, percent-decoded.
 * @property headers the header fields.
 * @property peer what the server knows of the peer.
 * @property principal the principal the request was authorized as, or `null` if the operation asks for no
 * credentials; the handler finds it as `currentPrincipal()` of Kore, too.
 * @param body the body, read at most once.
 * @param context the codecs, the limits and the problems of the server.
 * @param state what reading the request decided for its reply.
 * @since 1.0.0
 */
public class Request internal constructor(
    private val line: RequestLine,
    pathParameters: Map<String, String>,
    query: List<Pair<String, String>>,
    public val headers: Headers,
    public val peer: Peer,
    public val principal: Principal?,
    internal val body: BodySource,
    internal val context: ServerContext,
    internal val state: RequestState,
) {
    /**
     * The name of the operation the request is for.
     *
     * @since 1.0.0
     */
    public val operation: String get() = line.operation

    /**
     * The method, such as `GET` or `QUERY`.
     *
     * @since 1.0.0
     */
    public val method: String get() = line.method

    /**
     * The scheme of the connection, `http` or `https`, in lower case.
     *
     * @since 1.0.0
     */
    public val scheme: String get() = line.scheme

    /**
     * The host and port the request was sent to from its `Host`, in lower case and without the
     * default port of [scheme], or an empty text if it named none.
     *
     * @since 1.0.0
     */
    public val authority: String get() = line.authority

    /**
     * The path as it was sent, without the query.
     *
     * @since 1.0.0
     */
    public val path: String get() = line.path

    /**
     * The query as it was sent, without its `?`, or `null` if the request has none.
     *
     * @since 1.0.0
     */
    public val rawQuery: String? get() = line.rawQuery

    /**
     * The values of the parameters of the path template, percent-decoded.
     *
     * @since 1.0.0
     */
    public val pathParameters: Map<String, String> = pathParameters.toMap()

    /**
     * The names and values of the query in order, percent-decoded.
     *
     * @since 1.0.0
     */
    public val query: List<Pair<String, String>> = query.toList()

    /**
     * The media type of the body, or `null` if the request names none or a malformed one.
     *
     * @since 1.0.0
     */
    public val contentType: MediaType? get() = headers[CONTENT_TYPE]?.let(MediaType::parseOrNull)

    /**
     * The `Last-Event-ID` of a client that reconnects to a stream of server-sent events, or `null` if it sent none or
     * one longer than 1024 characters or with a line break or a NUL, which no event id can hold.
     *
     * @since 1.0.0
     */
    public val lastEventId: String?
        get() = headers[LAST_EVENT_ID]?.takeIf { id -> id.length <= MOST_EVENT_ID_LENGTH && HeaderChecks.isLine(id) }

    /**
     * Whether the request carries a body: a positive `Content-Length`, or a body of no declared length that is
     * chunked or names its `Content-Type`, as HTTP/2 sends it. A body that turns out to be empty is missing for
     * [input].
     *
     * @since 1.0.0
     */
    public val hasBody: Boolean get() = body.isPresent

    /**
     * Starts reading the request as a whole: its parameters and its body, every problem collected into one answer.
     *
     * @return the input of the request.
     * @since 1.0.0
     */
    public fun input(): Input = Input(this)

    internal fun authorized(principal: Principal?): Request = copy(principal = principal, body = body)

    internal fun buffered(content: ByteString): Request =
        copy(principal = principal, body = BodySource.buffered(content, context.limits))

    private fun copy(principal: Principal?, body: BodySource): Request = Request(
        line = line,
        pathParameters = pathParameters,
        query = query,
        headers = headers,
        peer = peer,
        principal = principal,
        body = body,
        context = context,
        state = state,
    )

    @OptIn(DavilsInternal::class)
    override fun toString(): String = "Request($method, ${SafeText.text(operation)})"

    /**
     * Builds requests without Ktor.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Builds a request for [operation] as the server would have read it, without Ktor and without a connection,
         * such as for a test that calls the handler of an operation directly.
         *
         * The query is read within the limits of the server as a request would be; the request is neither
         * authorized nor verified, so the principal of [configure] is what the handler sees. The body is held in
         * memory and is read at most once, as any body. Nothing is sent anywhere: the request is meant for tests and
         * tools, never as a way around the checks of a mounted server.
         *
         * @param operation the name of the operation the request is for.
         * @param method the method, a token in upper case, such as `GET` or `QUERY`.
         * @param path the path as it was sent, without the query.
         * @param configure sets the query, the header fields, the body, the peer, the principal and the server.
         * @return the request.
         * @throws IllegalArgumentException if [method] is no token in upper case, [path] does not start with `/`, a
         * header field is no field a request can carry, or the query is longer than the limits of the server allow,
         * has too many fields or is no percent-encoded UTF-8.
         * @throws com.davils.kore.core.validation.report.ValidationException if a setting of the server is invalid.
         * @since 1.0.0
         */
        public fun of(
            operation: String,
            method: String,
            path: String,
            configure: RequestBuilder.() -> Unit = {},
        ): Request {
            require(PathTemplates.isMethod(method)) { "The method of a request is a token in upper case" }
            require(path.startsWith('/')) { "The path of a request starts with /" }
            val builder = RequestBuilder()
            builder.configure()
            return builder.build(operation = operation, method = method, path = path)
        }
    }
}
