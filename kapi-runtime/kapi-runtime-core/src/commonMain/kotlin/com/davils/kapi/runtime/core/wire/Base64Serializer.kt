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

import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.io.encoding.Base64

/**
 * Writes bytes as the padded Base64 text of RFC 4648 §4 and reads them back, for the `format: byte` of OpenAPI and
 * the `contentEncoding: base64` of JSON Schema.
 *
 * @since 1.0.0
 */
public object Base64Serializer : KSerializer<ByteString> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Base64", PrimitiveKind.STRING)

    /**
     * Writes [value] as Base64 text.
     *
     * @param encoder the encoder.
     * @param value the bytes.
     * @since 1.0.0
     */
    override fun serialize(encoder: Encoder, value: ByteString) {
        encoder.encodeString(Base64.Default.encode(value.toByteArray()))
    }

    /**
     * Reads Base64 text as bytes.
     *
     * @param decoder the decoder.
     * @return the bytes.
     * @throws SerializationException if the text is no Base64.
     * @since 1.0.0
     */
    override fun deserialize(decoder: Decoder): ByteString = try {
        ByteString(Base64.Default.decode(decoder.decodeString()))
    } catch (failure: IllegalArgumentException) {
        throw SerializationException("The text is no Base64", failure)
    }
}
