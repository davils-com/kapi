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

import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kapi.runtime.core.response.Headers
import com.davils.kapi.runtime.core.response.Response
import kotlinx.io.bytestring.ByteString

private const val HASH_FACTOR: Int = 31

private const val CONTENT_TYPE: String = "content-type"

/**
 * How a call ended: with an answer of the server, whatever its status, or with a failure.
 *
 * @since 1.0.0
 */
public sealed interface Exchange {
    /**
     * The server answered.
     *
     * [toString] shows the status, the headers with their sensitive fields masked and the size of the content.
     *
     * @property status the status code.
     * @property headers the header fields, with the sensitive ones masked when shown.
     * @property content the content, at most as large as the client reads; empty for a stream, whose content went to
     * its flow.
     * @since 1.0.0
     */
    public class Answered(
        override val status: Int,
        override val headers: Headers,
        public val content: ByteString,
    ) : Exchange, Response {
        /**
         * The media type the `Content-Type` field names, or `null` without one or for one that is no media type.
         *
         * @since 1.0.0
         */
        public val contentType: MediaType? get() = headers[CONTENT_TYPE]?.let(MediaType::parseOrNull)

        override fun equals(other: Any?): Boolean = other is Answered &&
            status == other.status &&
            headers.toMap() == other.headers.toMap() &&
            content == other.content

        override fun hashCode(): Int {
            val head = status * HASH_FACTOR + headers.toMap().hashCode()
            return head * HASH_FACTOR + content.hashCode()
        }

        override fun toString(): String = "Answered($status, $headers, ${content.size} bytes)"
    }

    /**
     * The call got no answer.
     *
     * @property failure why.
     * @since 1.0.0
     */
    public class Failed(public val failure: CallFailure) : Exchange {
        override fun equals(other: Any?): Boolean = other is Failed && failure == other.failure

        override fun hashCode(): Int = failure.hashCode()

        override fun toString(): String = "Failed($failure)"
    }
}
