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

package com.davils.kapi.runtime.testing.server

import com.davils.kapi.runtime.core.codec.Codecs
import com.davils.kapi.runtime.core.codec.Decoding
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kapi.runtime.server.reply.Reply
import com.davils.kapi.runtime.server.reply.ReplyBody
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.decodeToString
import kotlinx.serialization.DeserializationStrategy

/**
 * Returns the content of the body of this reply as a server would send it: the bytes of a byte body, or the value of
 * a value body written by [codecs].
 *
 * A streamed body is written while the reply is sent and has no content here.
 *
 * @receiver the reply, such as one of [call].
 * @param codecs the codecs to write a value with; the codecs of the server under test, [Codecs.Default] by default.
 * @return the content; empty for a reply without a body, `null` for a streamed body.
 * @throws IllegalStateException if no codec of [codecs] writes the media type of a value body.
 * @since 1.0.0
 */
public fun Reply.content(codecs: Codecs = Codecs.Default): ByteString? = when (val found = body) {
    ReplyBody.Empty -> ByteString()
    is ReplyBody.Bytes -> found.content
    is ReplyBody.Value -> found.encoded(codecs)
    is ReplyBody.Stream -> null
}

/**
 * Returns the media type of the body of this reply, or `null` for a reply without a body.
 *
 * @receiver the reply.
 * @return the media type.
 * @since 1.0.0
 */
public val Reply.mediaType: MediaType?
    get() = when (val found = body) {
        ReplyBody.Empty -> null
        is ReplyBody.Bytes -> found.mediaType
        is ReplyBody.Value -> found.mediaType
        is ReplyBody.Stream -> found.mediaType
    }

/**
 * Returns the content of the body of this reply as UTF-8 text; see [content].
 *
 * @receiver the reply.
 * @param codecs the codecs to write a value with; [Codecs.Default] by default.
 * @return the text, or `null` for a streamed body.
 * @since 1.0.0
 */
public fun Reply.text(codecs: Codecs = Codecs.Default): String? = content(codecs)?.decodeToString()

/**
 * Reads the content of the body of this reply as a value of [deserializer], by the codec of [codecs] that covers its
 * media type, as a client would read it.
 *
 * @receiver the reply.
 * @param T the type of the value.
 * @param deserializer the deserializer of the value.
 * @param codecs the codecs to write and read with; [Codecs.Default] by default.
 * @return the value.
 * @throws IllegalArgumentException if the reply has no content of a media type, or no codec reads it as the value.
 * @since 1.0.0
 */
public fun <T> Reply.decoded(deserializer: DeserializationStrategy<T>, codecs: Codecs = Codecs.Default): T {
    val bytes = content(codecs)
    val type = mediaType
    require(bytes != null && type != null) { "The reply has no content of a media type to read" }
    val decoding = codecs.decode(deserializer, bytes, type)
    require(decoding is Decoding.Decoded) { "The content of the reply is no such value" }
    return decoding.value
}
