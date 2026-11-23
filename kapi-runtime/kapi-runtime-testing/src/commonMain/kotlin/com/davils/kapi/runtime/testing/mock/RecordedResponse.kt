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

import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.decodeToString

private const val FIRST_STATUS: Int = 100

private const val LAST_STATUS: Int = 599

/**
 * A response as a mock engine sent it.
 *
 * A recorded response is immutable. [toString] shows the status and the sizes, never a header value or the body.
 *
 * @property status the status code, between 100 and 599.
 * @property headers every header, `Content-Type` among them.
 * @property body the body; empty when the response has none.
 * @throws IllegalArgumentException if [status] lies outside 100 to 599.
 * @since 1.0.0
 */
public class RecordedResponse(
    public val status: Int,
    public val headers: Headers,
    public val body: ByteString,
) {
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

    init {
        require(status in FIRST_STATUS..LAST_STATUS) { "A status lies between 100 and 599: $status" }
    }

    private val identity: List<Any?> get() = listOf(status, headers.entries(), body)

    override fun equals(other: Any?): Boolean = other is RecordedResponse && identity == other.identity

    override fun hashCode(): Int = identity.hashCode()

    override fun toString(): String = "RecordedResponse($status, ${headers.names().size} headers, ${body.size} bytes)"
}
