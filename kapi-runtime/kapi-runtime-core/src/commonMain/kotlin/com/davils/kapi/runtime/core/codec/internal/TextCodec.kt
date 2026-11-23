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

package com.davils.kapi.runtime.core.codec.internal

import com.davils.kapi.runtime.core.codec.Codec
import com.davils.kapi.runtime.core.codec.Decoding
import com.davils.kapi.runtime.core.internal.Utf8
import com.davils.kapi.runtime.core.negotiation.MediaRange
import com.davils.kapi.runtime.core.negotiation.MediaType
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

private val readers: Map<String, (ByteArray) -> String?> = mapOf(
    "utf-8" to Utf8::decodeOrNull,
    "us-ascii" to Utf8::asciiOrNull,
    "iso-8859-1" to Utf8::latin1,
)

internal class TextCodec(private val json: Json) : Codec {
    private val trees: JsonCodec = JsonCodec(json, 1)

    override val mediaTypes: List<MediaRange> = listOf(MediaRange.of("text/plain"))

    override fun <T> encode(serializer: SerializationStrategy<T>, value: T): ByteString {
        val element = json.encodeToJsonElement(serializer, value)
        val primitive = element as? JsonPrimitive
        require(primitive != null && primitive != JsonNull) { "text/plain carries a single text, number or flag" }
        return primitive.content.encodeToByteString()
    }

    override fun <T> decode(deserializer: DeserializationStrategy<T>, content: ByteString): Decoding<T> =
        decode(deserializer, content, MediaType.TextPlain)

    override fun <T> decode(
        deserializer: DeserializationStrategy<T>,
        content: ByteString,
        mediaType: MediaType,
    ): Decoding<T> {
        val charset = mediaType.charset ?: UTF_8
        val text = readers[charset]?.invoke(content.toByteArray())
        if (text == null) return Decoding.Failed(failureOf(charset))
        return trees.decodeTree(deserializer, JsonPrimitive(text))
    }

    private fun failureOf(charset: String): String {
        if (charset in readers) return "The content is no $charset"
        return "The charset $charset is not supported"
    }

    private companion object {
        const val UTF_8: String = "utf-8"
    }
}
