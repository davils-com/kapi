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
import com.davils.kapi.runtime.core.negotiation.MediaRange
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlin.io.encoding.Base64

private const val NOT_BYTES: String =
    "application/octet-stream carries only bytes, written as Base64 by their serializer"

internal class OctetStreamCodec(private val json: Json) : Codec {
    private val trees: JsonCodec = JsonCodec(json, 1)

    override val mediaTypes: List<MediaRange> = listOf(MediaRange.of("application/octet-stream"))

    override fun <T> encode(serializer: SerializationStrategy<T>, value: T): ByteString {
        val primitive = json.encodeToJsonElement(serializer, value) as? JsonPrimitive
        require(primitive != null && primitive.isString) { NOT_BYTES }
        return ByteString(Base64.Default.decode(primitive.content))
    }

    override fun <T> decode(deserializer: DeserializationStrategy<T>, content: ByteString): Decoding<T> =
        trees.decodeTree(deserializer, JsonPrimitive(Base64.Default.encode(content.toByteArray())))
}
