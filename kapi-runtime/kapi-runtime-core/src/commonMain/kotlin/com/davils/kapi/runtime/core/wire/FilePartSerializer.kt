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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Writes the content of a [FilePart] as Base64 text and reads it back as a part without a name or a type.
 *
 * @since 1.0.0
 */
public object FilePartSerializer : KSerializer<FilePart> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FilePart", PrimitiveKind.STRING)

    /**
     * Writes the content of [value] as Base64 text.
     *
     * @param encoder the encoder.
     * @param value the file part.
     * @since 1.0.0
     */
    override fun serialize(encoder: Encoder, value: FilePart) {
        Base64Serializer.serialize(encoder, value.content)
    }

    /**
     * Reads Base64 text as the content of a file part.
     *
     * @param decoder the decoder.
     * @return the file part.
     * @throws kotlinx.serialization.SerializationException if the text is no Base64.
     * @since 1.0.0
     */
    override fun deserialize(decoder: Decoder): FilePart = FilePart(Base64Serializer.deserialize(decoder))
}
