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

import com.davils.kapi.runtime.core.codec.Codecs
import com.davils.kapi.runtime.core.negotiation.MediaType
import io.ktor.utils.io.ByteWriteChannel
import kotlinx.io.bytestring.ByteString

private const val UNKNOWN: String = "unknown"

/**
 * The body of a [Reply]: none, bytes, a value the codecs of the server write, or a stream.
 *
 * @since 1.0.0
 */
public sealed interface ReplyBody {
    /**
     * No body, as 204 (No Content) and 304 (Not Modified) demand.
     *
     * @since 1.0.0
     */
    public object Empty : ReplyBody {
        override fun toString(): String = "Empty"
    }

    /**
     * Bytes of a media type.
     *
     * @property mediaType the media type of the bytes.
     * @property content the bytes.
     * @since 1.0.0
     */
    public class Bytes(public val mediaType: MediaType, public val content: ByteString) : ReplyBody {
        private val parts: List<Any> get() = listOf(mediaType, content)

        override fun equals(other: Any?): Boolean = other is Bytes && parts == other.parts

        override fun hashCode(): Int = parts.hashCode()

        override fun toString(): String = "Bytes($mediaType, ${content.size} bytes)"
    }

    /**
     * A value that the codec of [mediaType] among the codecs of the server writes when the reply is sent.
     *
     * @property mediaType the media type to write the value as.
     * @param encode writes the value with the codecs of the server.
     * @since 1.0.0
     */
    public class Value internal constructor(
        public val mediaType: MediaType,
        internal val encode: (Codecs) -> ByteString,
    ) : ReplyBody {
        override fun toString(): String = "Value($mediaType)"
    }

    /**
     * Content written as it is produced, such as a download, JSON Lines or server-sent events.
     *
     * The writer runs in the coroutine of the response, which Ktor owns; when the client goes away, the writer is
     * cancelled.
     *
     * @property mediaType the media type of the content.
     * @property length the length of the content in bytes, or `null` if it is unknown and the content is chunked.
     * @param write writes the content to the channel of the response with the codecs of the server.
     * @since 1.0.0
     */
    public class Stream internal constructor(
        public val mediaType: MediaType,
        public val length: Long?,
        internal val write: suspend (ByteWriteChannel, Codecs) -> Unit,
    ) : ReplyBody {
        override fun toString(): String = "Stream($mediaType, ${length ?: UNKNOWN} bytes)"
    }
}
