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

package com.davils.kapi.runtime.core.codec

import com.davils.kapi.runtime.core.bounded
import com.davils.kapi.runtime.core.codec.form.FormLimits
import com.davils.kapi.runtime.core.codec.internal.CborCodec
import com.davils.kapi.runtime.core.codec.internal.FormCodec
import com.davils.kapi.runtime.core.codec.internal.JsonCodec
import com.davils.kapi.runtime.core.codec.internal.OctetStreamCodec
import com.davils.kapi.runtime.core.codec.internal.TextCodec
import com.davils.kapi.runtime.core.negotiation.MediaType
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.failure.contained
import com.davils.kore.core.validation.Validatable
import com.davils.kore.core.validation.ValidationScope
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json

/**
 * The codecs a client or a server writes and reads content with.
 *
 * The codecs of the application come first, then the built-in ones: JSON for `application/json`, `text/json` and
 * every `+json` type such as `application/problem+json`; CBOR of RFC 8949 for `application/cbor` and every `+cbor`
 * type; `text/plain` in UTF-8, US-ASCII or ISO-8859-1 by its `charset`; `application/x-www-form-urlencoded`; and
 * `application/octet-stream` for values whose serializer writes Base64, such as
 * [Base64Serializer][com.davils.kapi.runtime.core.wire.Base64Serializer]. [decode] refuses content larger than
 * [maxContentBytes] before any codec reads it, and the JSON and CBOR codecs refuse content that nests deeper than
 * [maxDepth]. The codecs are immutable and safe to share between threads.
 *
 * @since 1.0.0
 */
public class Codecs internal constructor(builder: CodecsBuilder) : Validatable {
    /**
     * The JSON configuration the built-in codecs share.
     *
     * @since 1.0.0
     */
    public val json: Json = builder.json

    /**
     * The most bytes of content [decode] reads.
     *
     * @since 1.0.0
     */
    public val maxContentBytes: Int = builder.maxContentBytes

    /**
     * The deepest nesting JSON and CBOR content may have.
     *
     * @since 1.0.0
     */
    public val maxDepth: Int = builder.maxDepth

    /**
     * The limits of the codec of `application/x-www-form-urlencoded`.
     *
     * @since 1.0.0
     */
    public val formLimits: FormLimits = builder.formLimits

    private val all: List<Codec> = builder.codecs +
        listOf(
            JsonCodec(json, maxDepth),
            CborCodec(json, maxDepth),
            TextCodec(json),
            FormCodec(json, formLimits),
            OctetStreamCodec(json),
        )

    /**
     * Returns the first codec whose media ranges cover [mediaType].
     *
     * @param mediaType the media type.
     * @return the codec, or `null` if none covers [mediaType].
     * @since 1.0.0
     */
    public fun codecFor(mediaType: MediaType): Codec? =
        all.firstOrNull { codec -> codec.mediaTypes.any { range -> range.matches(mediaType) } }

    /**
     * Reads [mediaType] and returns the first codec whose media ranges cover it.
     *
     * @param mediaType the text of a `Content-Type`, which may come from an attacker, or `null`.
     * @return the codec, or `null` if [mediaType] is absent, no media type, or covered by no codec.
     * @since 1.0.0
     */
    public fun codecFor(mediaType: String?): Codec? = mediaType?.let(MediaType::parseOrNull)?.let(::codecFor)

    /**
     * Returns whether a codec covers [mediaType].
     *
     * @param mediaType the text of a `Content-Type`, or `null`.
     * @return `true` if [codecFor] finds a codec.
     * @since 1.0.0
     */
    public fun supports(mediaType: String?): Boolean = codecFor(mediaType) != null

    /**
     * Reads [content] that was sent as [mediaType] with the codec that covers it.
     *
     * @param T the type of the value.
     * @param deserializer the deserializer of the expected value.
     * @param content the content, which may come from an attacker.
     * @param mediaType the text of its `Content-Type`, or `null` if it has none.
     * @return the value, or why the content holds none: it is larger than [maxContentBytes], its type is absent or
     * malformed, no codec covers it, or the codec refused it.
     * @since 1.0.0
     */
    public fun <T> decode(
        deserializer: DeserializationStrategy<T>,
        content: ByteString,
        mediaType: String?,
    ): Decoding<T> {
        val type = mediaType?.let(MediaType::parseOrNull) ?: return Decoding.Failed(refusalOf(content, type = null))
        return decode(deserializer, content, type)
    }

    /**
     * Reads [content] that was sent as the parsed [mediaType] with the codec that covers it, such as the type of a
     * part of a multipart body or the type a server negotiated.
     *
     * @param T the type of the value.
     * @param deserializer the deserializer of the expected value.
     * @param content the content, which may come from an attacker.
     * @param mediaType the media type of [content]; its parameters, such as `charset`, reach the codec.
     * @return the value, or why the content holds none: it is larger than [maxContentBytes], no codec covers its type,
     * or the codec refused it. A codec of the caller is foreign code and runs inside `contained` of Kore: what it
     * throws is a failed decoding, never an exception of this call.
     * @since 1.0.0
     */
    public fun <T> decode(
        deserializer: DeserializationStrategy<T>,
        content: ByteString,
        mediaType: MediaType,
    ): Decoding<T> {
        val codec = codecFor(mediaType)
        val isTooLarge = content.size > maxContentBytes
        if (isTooLarge || codec == null) return Decoding.Failed(refusalOf(content, mediaType))
        val decoding = contained { codec.decode(deserializer, content, mediaType) }
        return decoding.getOrElse { Decoding.Failed(THROWN) }
    }

    override fun ValidationScope.validate() {
        bounded(::maxContentBytes, HARD_CONTENT_BYTES.toLong())
        bounded(::maxDepth, HARD_DEPTH.toLong())
    }

    override fun toString(): String = "Codecs(${all.flatMap(Codec::mediaTypes)})"

    private fun refusalOf(content: ByteString, type: MediaType?): String {
        if (content.size > maxContentBytes) return "The content is larger than $maxContentBytes bytes"
        return MISSING.takeIf { type == null } ?: UNSUPPORTED
    }

    /**
     * The default codecs and the hard ceilings of their limits.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The hard ceiling of [maxContentBytes], 1 GiB.
         *
         * @since 1.0.0
         */
        public const val HARD_CONTENT_BYTES: Int = 1_024 * 1_024 * 1_024

        /**
         * The hard ceiling of [maxDepth].
         *
         * @since 1.0.0
         */
        public const val HARD_DEPTH: Int = 512

        private const val MISSING: String = "The content has no media type that can be read"

        private const val UNSUPPORTED: String = "No codec reads the media type of the content"

        private const val THROWN: String = "The codec failed to read the content"

        /**
         * The built-in codecs with the default JSON and limits.
         *
         * @since 1.0.0
         */
        public val Default: Codecs = CodecsBuilder().build()
    }
}
