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

package com.davils.kapi.runtime.xml

import com.davils.kapi.runtime.core.bounded
import com.davils.kapi.runtime.core.codec.Codec
import com.davils.kapi.runtime.core.codec.Codecs
import com.davils.kapi.runtime.core.codec.Decoding
import com.davils.kapi.runtime.core.negotiation.MediaRange
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.failure.contained
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import com.davils.kore.core.validation.violation.Severity
import com.davils.kxml.core.Xml
import com.davils.kxml.core.XmlDoctype
import com.davils.kxml.core.XmlParseResult
import com.davils.kxml.core.diagnostic.XmlDiagnostic
import com.davils.kxml.core.emit.XmlEmitOptions
import com.davils.kxml.core.limit.XmlLimits
import com.davils.kxml.core.source.XmlSource
import com.davils.kxml.core.xml
import com.davils.kxml.serialization.XmlDecoding
import com.davils.kxml.serialization.XmlFormat
import com.davils.kxml.serialization.xmlFormat
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.modules.SerializersModule

private const val UNREADABLE: String = "The content cannot be read as XML of what was expected"

/**
 * The codec of `application/xml`, `text/xml` and every `+xml` type, such as `application/atom+xml`, on the XML
 * format of kxml.
 *
 * A class is an element named after its serial name without its package, its properties are child elements named
 * after their serial names, a list repeats the element of its property and a `null` property is left out, as
 * `kxml-serialization` maps them. Content is read with the hardened reader of kxml within [limits]: a document type
 * declaration is refused, so no entity is expanded and no external resource is loaded, and a document that breaks a
 * limit, is not well-formed or does not fit the type is answered with [Decoding.Failed], never with an exception.
 * Bytes are decoded as XML 1.0 detects their encoding, with the `charset` of the media type taking precedence as
 * RFC 7303 orders it. Content is written compactly in UTF-8 with an XML declaration.
 *
 * Register it with [xml][com.davils.kapi.runtime.xml.xml] in the codecs of a client or a server. The codec is
 * immutable and safe to share between threads.
 *
 * @since 1.2.0
 */
public class XmlCodec internal constructor(builder: XmlCodecBuilder) : Codec, Validatable {
    /**
     * The limits every document is read within.
     *
     * @since 1.2.0
     */
    public val limits: XmlLimits = builder.limits

    /**
     * Whether an element or an attribute the type does not declare is skipped.
     *
     * @since 1.2.0
     */
    public val isUnknownElementIgnored: Boolean = builder.isUnknownElementIgnored

    /**
     * The module of contextual and polymorphic serializers.
     *
     * @since 1.2.0
     */
    public val serializersModule: SerializersModule = builder.serializersModule

    private val maximumDepth: Int get() = limits.maximumDepth

    private val maximumInputLength: Int get() = limits.maximumInputLength

    private val reader: Xml = xml {
        doctype = XmlDoctype.Refuse
        resolver = null
        limits = this@XmlCodec.limits
        emitter = XmlEmitOptions.Compact
    }

    private val format: XmlFormat = xmlFormat {
        xml = reader
        isUnknownElementIgnored = this@XmlCodec.isUnknownElementIgnored
        serializersModule = this@XmlCodec.serializersModule
    }

    override val mediaTypes: List<MediaRange> = listOf(
        MediaRange.of("application/xml"),
        MediaRange.of("text/xml"),
        MediaRange.of("application/*+xml"),
    )

    /**
     * Writes [value] as an XML document in UTF-8.
     *
     * @param T the type of the value.
     * @param serializer the serializer of the value.
     * @param value the value.
     * @return the UTF-8 bytes of the document.
     * @throws IllegalArgumentException if XML cannot carry the value, such as a name that is no XML name, or the text
     * is longer than the output limit of [limits].
     * @since 1.2.0
     */
    override fun <T> encode(serializer: SerializationStrategy<T>, value: T): ByteString =
        format.encodeToString(serializer, value).encodeToByteString()

    /**
     * Reads [content] as an XML document whose encoding the document itself declares.
     *
     * @param T the type of the value.
     * @param deserializer the deserializer of the expected value.
     * @param content the content, which may come from an attacker.
     * @return the value, or why the content holds none.
     * @since 1.2.0
     */
    override fun <T> decode(deserializer: DeserializationStrategy<T>, content: ByteString): Decoding<T> =
        read(deserializer, XmlSource.of(content.toByteArray()))

    /**
     * Reads [content] as an XML document, decoded in the `charset` of [mediaType] if it names one.
     *
     * @param T the type of the value.
     * @param deserializer the deserializer of the expected value.
     * @param content the content, which may come from an attacker.
     * @param mediaType the media type the content was sent as.
     * @return the value, or why the content holds none: a charset kxml does not read, a malformed document, a
     * document type declaration, a broken limit or content that does not fit the type.
     * @since 1.2.0
     */
    override fun <T> decode(
        deserializer: DeserializationStrategy<T>,
        content: ByteString,
        mediaType: MediaType,
    ): Decoding<T> = read(deserializer, XmlSource.of(bytes = content.toByteArray(), charset = mediaType.charset))

    override fun ValidationScope.validate() {
        bounded(::maximumDepth, HARD_DEPTH.toLong())
        bounded(::maximumInputLength, HARD_INPUT_LENGTH.toLong())
    }

    override fun toString(): String = "XmlCodec($mediaTypes, $limits)"

    private fun <T> read(deserializer: DeserializationStrategy<T>, source: XmlSource): Decoding<T> {
        val parsed = reader.parse(source)
        if (parsed !is XmlParseResult.Parsed) return Decoding.Failed(reasonOf(parsed.diagnostics))
        val decoding = contained { format.decodeOrFailure(deserializer, parsed.document.root) }
        return decoding.fold(
            onSuccess = { found -> decoded(found) },
            onFailure = { _ -> Decoding.Failed(UNREADABLE) },
        )
    }

    private fun <T> decoded(decoding: XmlDecoding<T>): Decoding<T> {
        if (decoding is XmlDecoding.Decoded) return Decoding.Decoded(decoding.value)
        return Decoding.Failed(reasonOf(decoding.diagnostics))
    }

    private fun reasonOf(diagnostics: List<XmlDiagnostic>): String {
        val errors = diagnostics.filter { diagnostic -> diagnostic.severity == Severity.Error }
        return (errors.map(XmlDiagnostic::message) + UNREADABLE).first()
    }

    /**
     * The hard ceilings of the limits of an XML codec.
     *
     * @since 1.2.0
     */
    public companion object {
        /**
         * The hard ceiling of the depth of elements, the hard ceiling of nesting of the codecs of kapi.
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
