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

package com.davils.kapi.runtime.testing.mock

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.decodeToString

/**
 * A request as a mock engine received it: the method, the URL, every header, the content type of the body among
 * them, and the body as bytes.
 *
 * A recorded request is immutable. [toString] shows the method, the URL and the sizes, never a header value or the
 * body, which may carry credentials.
 *
 * @property method the method, such as `GET`.
 * @property url the URL.
 * @property headers every header, `Content-Type` and `Content-Length` of the body included.
 * @property body the body; empty when the request has none.
 * @since 1.0.0
 */
public class RecordedRequest(
    public val method: String,
    public val url: Url,
    public val headers: Headers,
    public val body: ByteString,
) {
    /**
     * The encoded path of the URL, such as `/v1/pets/42`.
     *
     * @since 1.0.0
     */
    public val path: String get() = url.encodedPath

    /**
     * The `Content-Type` of the body, or `null` without one.
     *
     * @since 1.0.0
     */
    public val contentType: String? get() = headers[HttpHeaders.ContentType]

    /**
     * The body as UTF-8 text; malformed sequences become replacement characters.
     *
     * @since 1.0.0
     */
    public val text: String get() = body.decodeToString()

    private val identity: List<Any?> get() = listOf(method, url, headers.entries(), body)

    /**
     * Returns the first value of the header [name], compared without case.
     *
     * @param name the name of the header.
     * @return the value, or `null` if the request has no such header.
     * @since 1.0.0
     */
    public fun header(name: String): String? = headers[name]

    /**
     * Returns the first value of the query parameter [name], percent-decoded.
     *
     * @param name the name of the parameter.
     * @return the value, or `null` if the query has no such parameter.
     * @since 1.0.0
     */
    public fun query(name: String): String? = url.parameters[name]

    override fun equals(other: Any?): Boolean = other is RecordedRequest && identity == other.identity

    override fun hashCode(): Int = identity.hashCode()

    override fun toString(): String = "RecordedRequest(${SafeText.text(method)} ${SafeText.text(url.toString())}, " +
        "${headers.names().size} headers, ${body.size} bytes)"
}
