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

package com.davils.kapi.runtime.server.channel

import com.davils.kapi.runtime.core.negotiation.MediaType
import kotlinx.serialization.KSerializer

private const val TEXT: String = "text"

private const val JSON: String = "json"

/**
 * How the messages of one direction of a channel are written and read: their serializer and the media type of the
 * `contentType` of AsyncAPI 3, whose codec among the codecs of the server writes and reads them.
 *
 * A message of a textual media type, such as `application/json` or `text/plain`, travels in a text frame, every other
 * one, such as `application/cbor`, in a binary frame; a message is read from either kind of frame. A codec is
 * immutable.
 *
 * @param T the type of the messages.
 * @property serializer the serializer of a message.
 * @property mediaType the media type of a message; `application/json` by default.
 * @since 1.0.0
 */
public class MessageCodec<T>(public val serializer: KSerializer<T>, public val mediaType: MediaType = MediaType.Json) {
    /**
     * Whether a message travels in a text frame.
     *
     * @since 1.0.0
     */
    public val isText: Boolean get() = mediaType.type == TEXT || mediaType.subtype == JSON || mediaType.suffix == JSON

    private val parts: List<Any> get() = listOf(serializer, mediaType)

    override fun equals(other: Any?): Boolean = other is MessageCodec<*> && parts == other.parts

    override fun hashCode(): Int = parts.hashCode()

    override fun toString(): String = "MessageCodec(${serializer.descriptor.serialName}, $mediaType)"
}
