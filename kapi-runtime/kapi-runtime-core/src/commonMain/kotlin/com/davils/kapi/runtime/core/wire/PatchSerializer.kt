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

package com.davils.kapi.runtime.core.wire

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

private const val ABSENT: String = "An absent field of a patch is left out and cannot be written"

/**
 * Writes and reads a [Patch]: a value as itself, [Patch.Null] as `null`; a field that is missing stays at its default,
 * [Patch.Absent].
 *
 * @param T the type of the value.
 * @param inner the serializer of the value.
 * @since 1.0.0
 */
@OptIn(ExperimentalSerializationApi::class)
public class PatchSerializer<T>(private val inner: KSerializer<T>) : KSerializer<Patch<T>> {
    override val descriptor: SerialDescriptor = inner.descriptor.nullable

    /**
     * Writes [value].
     *
     * @param encoder the encoder.
     * @param value the patch.
     * @throws SerializationException if [value] is [Patch.Absent], which a format must leave out as a default.
     * @since 1.0.0
     */
    override fun serialize(encoder: Encoder, value: Patch<T>) {
        when (value) {
            is Patch.Value -> encoder.encodeSerializableValue(inner, value.value)
            Patch.Null -> encoder.encodeNull()
            Patch.Absent -> throw SerializationException(ABSENT)
        }
    }

    /**
     * Reads a patch that is present: `null` as [Patch.Null], every other value as [Patch.Value].
     *
     * @param decoder the decoder.
     * @return the patch.
     * @since 1.0.0
     */
    override fun deserialize(decoder: Decoder): Patch<T> {
        if (decoder.decodeNotNullMark()) return Patch.Value(decoder.decodeSerializableValue(inner))
        decoder.decodeNull()
        return Patch.Null
    }
}
