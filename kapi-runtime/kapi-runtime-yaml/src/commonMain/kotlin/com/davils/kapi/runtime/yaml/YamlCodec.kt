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

@file:OptIn(DavilsInternal::class)

package com.davils.kapi.runtime.yaml

import com.davils.kaml.core.Yaml
import com.davils.kaml.core.YamlParseResult
import com.davils.kaml.core.YamlUnknownTags
import com.davils.kaml.core.diagnostic.YamlDiagnostic
import com.davils.kaml.core.limit.YamlLimits
import com.davils.kaml.core.source.YamlSource
import com.davils.kaml.core.yaml
import com.davils.kaml.serialization.json.YamlJsonConversion
import com.davils.kaml.serialization.json.toJsonElement
import com.davils.kaml.serialization.json.toYamlNode
import com.davils.kapi.runtime.core.bounded
import com.davils.kapi.runtime.core.codec.Codec
import com.davils.kapi.runtime.core.codec.Codecs
import com.davils.kapi.runtime.core.codec.Decoding
import com.davils.kapi.runtime.core.negotiation.MediaRange
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.failure.contained
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import com.davils.kore.core.validation.violation.Severity
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

private const val UNREADABLE: String = "The content cannot be read as YAML of what was expected"

private const val NOT_SINGLE: String = "The content holds no document or more than one"

/**
 * The codec of `application/yaml`, `application/x-yaml` and every `+yaml` type of RFC 9512, on kaml.
 *
 * A value reads and writes as it does in JSON: content is read into the data of one document, which must be what JSON
 * can hold exactly, and mapped to the value with [json]; a value is mapped to JSON data with [json] and written as
 * YAML. Content is read with the hardened reader of kaml within [limits]: a repeated key and an unknown tag are
 * refused, an alias counts against the expansion of aliases, so a billion laughs document fails at its budget, and
 * content that holds more than one document, is no YAML, breaks a limit or does not fit the type is answered with
 * [Decoding.Failed], never with an exception. kaml never constructs an object from a tag.
 *
 * Register it with [yaml][com.davils.kapi.runtime.yaml.yaml] in the codecs of a client or a server. The codec is
 * immutable and safe to share between threads.
 *
 * @since 1.2.0
 */
public class YamlCodec internal constructor(builder: YamlCodecBuilder) : Codec, Validatable {
    /**
     * The limits every document is read and written within.
     *
     * @since 1.2.0
     */
    public val limits: YamlLimits = builder.limits

    /**
     * The JSON configuration that maps values to the data of a document and back.
     *
     * @since 1.2.0
     */
    public val json: Json = builder.json

    private val maximumDepth: Int get() = limits.maximumDepth

    private val maximumInputLength: Int get() = limits.maximumInputLength

    private val reader: Yaml = yaml {
        unknownTags = YamlUnknownTags.Refuse
        limits = this@YamlCodec.limits
    }

    override val mediaTypes: List<MediaRange> = listOf(
        MediaRange.of("application/yaml"),
        MediaRange.of("application/x-yaml"),
        MediaRange.of("application/*+yaml"),
    )

    /**
     * Writes [value] as a YAML document in UTF-8.
     *
     * @param T the type of the value.
     * @param serializer the serializer of the value.
     * @param value the value.
     * @return the UTF-8 bytes of the document.
     * @throws IllegalArgumentException if YAML cannot carry the value, such as a number outside the range of a
     * `Double`, or the value breaks [limits].
     * @since 1.2.0
     */
    override fun <T> encode(serializer: SerializationStrategy<T>, value: T): ByteString {
        val node = json.encodeToJsonElement(serializer, value).toYamlNode(limits)
        return reader.emit(node).encodeToByteString()
    }

    /**
     * Reads [content] as one YAML document.
     *
     * @param T the type of the value.
     * @param deserializer the deserializer of the expected value.
     * @param content the content, which may come from an attacker.
     * @return the value, or why the content holds none: it is no YAML, holds no document or more than one, breaks a
     * limit, holds data JSON cannot hold exactly, such as a key that is no string, or does not fit the type.
     * @since 1.2.0
     */
    override fun <T> decode(deserializer: DeserializationStrategy<T>, content: ByteString): Decoding<T> {
        val parsed = reader.parse(YamlSource.of(content.toByteArray()))
        if (parsed !is YamlParseResult.Parsed) return Decoding.Failed(reasonOf(parsed.diagnostics))
        return single(deserializer, parsed)
    }

    override fun ValidationScope.validate() {
        bounded(::maximumDepth, HARD_DEPTH.toLong())
        bounded(::maximumInputLength, HARD_INPUT_LENGTH.toLong())
    }

    override fun toString(): String = "YamlCodec($mediaTypes, $limits)"

    private fun <T> single(deserializer: DeserializationStrategy<T>, parsed: YamlParseResult.Parsed): Decoding<T> {
        val document = parsed.stream.single ?: return Decoding.Failed(NOT_SINGLE)
        return mapped(deserializer, document.root.toJsonElement(limits))
    }

    private fun <T> mapped(deserializer: DeserializationStrategy<T>, conversion: YamlJsonConversion): Decoding<T> =
        when (conversion) {
            is YamlJsonConversion.Refused -> Decoding.Failed(conversion.reason)
            is YamlJsonConversion.Converted -> decoded(deserializer, conversion.element)
        }

    private fun <T> decoded(deserializer: DeserializationStrategy<T>, element: JsonElement): Decoding<T> =
        contained { json.decodeFromJsonElement(deserializer, element) }.fold(
            onSuccess = { value -> Decoding.Decoded(value) },
            onFailure = { failure -> Decoding.Failed(failure.message ?: UNREADABLE) },
        )

    private fun reasonOf(diagnostics: List<YamlDiagnostic>): String {
        val errors = diagnostics.filter { diagnostic -> diagnostic.severity == Severity.Error }
        return (errors.map(YamlDiagnostic::message) + UNREADABLE).first()
    }

    /**
     * The hard ceilings of the limits of a YAML codec.
     *
     * @since 1.2.0
     */
    public companion object {
        /**
         * The hard ceiling of the depth of nesting, the hard ceiling of nesting of the codecs of kapi.
         *
         * @since 1.2.0
         */
        public const val HARD_DEPTH: Int = Codecs.HARD_DEPTH

        /**
         * The hard ceiling of the length of a document, the hard ceiling of content of the codecs of kapi.
         *
         * @since 1.2.0
         */
        public const val HARD_INPUT_LENGTH: Int = Codecs.HARD_CONTENT_BYTES
    }
}
