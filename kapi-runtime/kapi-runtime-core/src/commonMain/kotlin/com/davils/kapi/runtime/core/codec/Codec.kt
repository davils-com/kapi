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

package com.davils.kapi.runtime.core.codec

import com.davils.kapi.runtime.core.negotiation.MediaRange
import com.davils.kapi.runtime.core.negotiation.MediaType
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy

/**
 * Writes values as content of some media types and reads them back, through the serializers of kotlinx.serialization.
 *
 * A codec of the application is asked before the built-in ones of [Codecs]. It must never throw from [decode]: content
 * may come from an attacker, and a codec answers what it cannot read with [Decoding.Failed]. It must be safe to call
 * from any thread, since one instance serves every call of a client or a server.
 *
 * @since 1.0.0
 */
public interface Codec {
    /**
     * The media ranges whose types this codec writes and reads.
     *
     * @since 1.0.0
     */
    public val mediaTypes: List<MediaRange>

    /**
     * Writes [value] as content.
     *
     * @param T the type of the value.
     * @param serializer the serializer of the value.
     * @param value the value.
     * @return the content.
     * @throws IllegalArgumentException if the value has a shape the media type cannot carry.
     * @since 1.0.0
     */
    public fun <T> encode(serializer: SerializationStrategy<T>, value: T): ByteString

    /**
     * Reads [content] as a value.
     *
     * @param T the type of the value.
     * @param deserializer the deserializer of the expected value.
     * @param content the content, which may come from an attacker.
     * @return the value, or why the content holds none.
     * @since 1.0.0
     */
    public fun <T> decode(deserializer: DeserializationStrategy<T>, content: ByteString): Decoding<T>

    /**
     * Reads [content] that was sent as [mediaType] as a value, such as text in the charset its type names.
     *
     * The default ignores [mediaType] and calls the other [decode].
     *
     * @param T the type of the value.
     * @param deserializer the deserializer of the expected value.
     * @param content the content, which may come from an attacker.
     * @param mediaType the media type the content was sent as.
     * @return the value, or why the content holds none.
     * @since 1.0.0
     */
    public fun <T> decode(
        deserializer: DeserializationStrategy<T>,
        content: ByteString,
        mediaType: MediaType,
    ): Decoding<T> = decode(deserializer, content)
}
