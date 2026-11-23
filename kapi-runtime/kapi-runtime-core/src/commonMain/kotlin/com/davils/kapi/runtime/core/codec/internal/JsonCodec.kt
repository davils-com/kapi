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
import com.davils.kore.core.failure.contained
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

internal class JsonCodec(private val json: Json, private val maxDepth: Int) : Codec {
    override val mediaTypes: List<MediaRange> = listOf(
        MediaRange.Json,
        MediaRange.of("application/*+json"),
        MediaRange.of("text/json"),
    )

    override fun <T> encode(serializer: SerializationStrategy<T>, value: T): ByteString =
        json.encodeToString(serializer, value).encodeToByteString()

    override fun <T> decode(deserializer: DeserializationStrategy<T>, content: ByteString): Decoding<T> {
        val text = Utf8.decodeOrNull(content.toByteArray()) ?: return Decoding.Failed(NOT_UTF8)
        return decodeText(deserializer, text)
    }

    fun <T> decodeText(deserializer: DeserializationStrategy<T>, text: String): Decoding<T> {
        if (!JsonDepth.isWithin(text, maxDepth)) return Decoding.Failed(tooDeep(maxDepth))
        return attempt { json.decodeFromString(deserializer, text) }
    }

    fun <T> decodeTree(deserializer: DeserializationStrategy<T>, tree: JsonElement): Decoding<T> =
        attempt { json.decodeFromJsonElement(deserializer, tree) }

    companion object {
        const val NOT_UTF8: String = "The content is no UTF-8"

        private const val NOT_READABLE: String = "The content cannot be read as what was expected"

        fun reasonOf(failure: Throwable): String = failure.message ?: NOT_READABLE

        fun tooDeep(maxDepth: Int): String = "The content nests deeper than $maxDepth levels"

        fun <T> attempt(read: () -> T): Decoding<T> = contained(read).fold(
            onSuccess = { value -> Decoding.Decoded(value) },
            onFailure = { failure -> Decoding.Failed(reasonOf(failure)) },
        )
    }
}
