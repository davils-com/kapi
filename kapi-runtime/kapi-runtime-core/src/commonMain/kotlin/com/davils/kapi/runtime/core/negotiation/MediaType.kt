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

package com.davils.kapi.runtime.core.negotiation

import com.davils.kapi.runtime.core.internal.FieldGrammar
import com.davils.kapi.runtime.core.internal.MediaTypeParts
import com.davils.kapi.runtime.core.internal.MediaTypeReader
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

private const val SUFFIX: Char = '+'

private const val WILDCARD: String = "*"

private const val CHARSET: String = "charset"

private const val HASH_FACTOR: Int = 31

/**
 * A media type of RFC 9110 §8.3.1, such as `application/json` or `text/plain; charset=utf-8`.
 *
 * The type, the subtype and the parameter names are compared without case and kept in lower case, and so is the value
 * of the `charset` parameter; every other parameter value is kept as it was written. A media type names neither `*`
 * as its type nor as its subtype: that is a [MediaRange]. A structured syntax suffix of RFC 6838 §4.2.8, such as the
 * `json` of `application/problem+json`, is offered by [suffix].
 *
 * A media type is immutable and safe to share between threads. Its [toString] is a valid field value: every
 * parameter value that is no token is written as a quoted string, and a value that could not be written so is
 * refused when the media type is made.
 *
 * @property type the top-level type, such as `application`, in lower case.
 * @property subtype the subtype, such as `problem+json`, in lower case.
 * @property parameters the parameters in the order they were written, with their names in lower case.
 * @since 1.0.0
 */
public class MediaType private constructor(
    public val type: String,
    public val subtype: String,
    public val parameters: List<Pair<String, String>>,
) {
    /**
     * The type and the subtype without parameters, such as `application/json`.
     *
     * @since 1.0.0
     */
    public val essence: String get() = "$type/$subtype"

    /**
     * The structured syntax suffix of the subtype, such as `json` for `application/problem+json`, or `null` if it has
     * none.
     *
     * @since 1.0.0
     */
    public val suffix: String? get() = subtype.substringAfterLast(SUFFIX, "").takeIf(String::isNotEmpty)

    /**
     * The value of the `charset` parameter in lower case, or `null` if there is none.
     *
     * @since 1.0.0
     */
    public val charset: String? get() = parameter(CHARSET)

    /**
     * Returns the value of the parameter [name], compared without case.
     *
     * @param name the name of the parameter.
     * @return the value, or `null` if the media type has no such parameter.
     * @since 1.0.0
     */
    public fun parameter(name: String): String? {
        val key = name.lowercase()
        return parameters.firstOrNull { (candidate, _) -> candidate == key }?.second
    }

    /**
     * Returns this media type with the parameter [name] set to [value], replacing a parameter of that name.
     *
     * @param name the name of the parameter, a token.
     * @param value the value, made of visible ASCII characters, spaces and tabs.
     * @return the new media type.
     * @throws IllegalArgumentException if [name] is no token or [value] holds a character a field value cannot carry,
     * such as a line break.
     * @since 1.0.0
     */
    public fun withParameter(name: String, value: String): MediaType {
        val key = name.lowercase()
        val kept = parameters.filterNot { (candidate, _) -> candidate == key }
        return of(type = type, subtype = subtype, parameters = kept + (key to value))
    }

    /**
     * Returns this media type without parameters.
     *
     * @return the media type of [essence].
     * @since 1.0.0
     */
    public fun withoutParameters(): MediaType = MediaType(type, subtype, emptyList())

    override fun equals(other: Any?): Boolean =
        other is MediaType && type == other.type && subtype == other.subtype && parameters == other.parameters

    override fun hashCode(): Int = (type.hashCode() * HASH_FACTOR + subtype.hashCode()) *
        HASH_FACTOR +
        parameters.hashCode()

    override fun toString(): String {
        val written = parameters.joinToString("") { (name, value) -> "; $name=${FieldGrammar.tokenOrQuoted(value)}" }
        return essence + written
    }

    /**
     * Reads and makes media types, and the media types the runtime knows by name.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * `application/json`.
         *
         * @since 1.0.0
         */
        public val Json: MediaType = parse("application/json")

        /**
         * `application/problem+json` of RFC 9457.
         *
         * @since 1.0.0
         */
        public val ProblemJson: MediaType = parse("application/problem+json")

        /**
         * `application/cbor` of RFC 8949.
         *
         * @since 1.0.0
         */
        public val Cbor: MediaType = parse("application/cbor")

        /**
         * `text/plain`.
         *
         * @since 1.0.0
         */
        public val TextPlain: MediaType = parse("text/plain")

        /**
         * `application/x-www-form-urlencoded`.
         *
         * @since 1.0.0
         */
        public val FormUrlEncoded: MediaType = parse("application/x-www-form-urlencoded")

        /**
         * `multipart/form-data` of RFC 7578.
         *
         * @since 1.0.0
         */
        public val MultipartFormData: MediaType = parse("multipart/form-data")

        /**
         * `multipart/byteranges` of RFC 9110 §14.6.
         *
         * @since 1.0.0
         */
        public val MultipartByteRanges: MediaType = parse("multipart/byteranges")

        /**
         * `application/octet-stream`.
         *
         * @since 1.0.0
         */
        public val OctetStream: MediaType = parse("application/octet-stream")

        /**
         * `application/x-ndjson`, newline-delimited JSON.
         *
         * @since 1.0.0
         */
        public val NdJson: MediaType = parse("application/x-ndjson")

        /**
         * `application/jsonl`, JSON Lines.
         *
         * @since 1.0.0
         */
        public val JsonLines: MediaType = parse("application/jsonl")

        /**
         * `application/json-seq`, JSON text sequences of RFC 7464.
         *
         * @since 1.0.0
         */
        public val JsonSequence: MediaType = parse("application/json-seq")

        /**
         * `text/event-stream`, server-sent events of the WHATWG HTML standard.
         *
         * @since 1.0.0
         */
        public val EventStream: MediaType = parse("text/event-stream")

        /**
         * Reads [text] as a media type.
         *
         * Whitespace around the text is ignored. Text longer than 4096 characters, with more than 64 parameters, with
         * a parameter named twice, with a wildcard or with anything RFC 9110 does not allow is refused.
         *
         * @param text the text, which may come from an attacker.
         * @return the media type, or `null` if [text] is none.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String): MediaType? = MediaTypeReader.parse(text)?.let(::typeOf)

        /**
         * Reads [text] as a media type like [parseOrNull].
         *
         * @param text the text.
         * @return the media type.
         * @throws IllegalArgumentException if [text] is no media type.
         * @since 1.0.0
         */
        public fun parse(text: String): MediaType =
            requireNotNull(parseOrNull(text)) { "${SafeText.quoted(text)} is no media type such as application/json" }

        /**
         * Makes a media type of its parts.
         *
         * @param type the top-level type, a token other than `*`.
         * @param subtype the subtype, a token other than `*`.
         * @param parameters the parameters, each name a token and named once.
         * @return the media type, with the names in lower case.
         * @throws IllegalArgumentException if a part is no token, a name repeats or a value holds a character a field
         * value cannot carry.
         * @since 1.0.0
         */
        public fun of(type: String, subtype: String, parameters: List<Pair<String, String>> = emptyList()): MediaType {
            val names = parameters.map { (name, _) -> name.lowercase() }
            val isValid = isConcrete(type) && isConcrete(subtype) && names.all(FieldGrammar::isToken)
            require(isValid && names.toSet().size == names.size) {
                "${SafeText.quoted("$type/$subtype")} with its parameters is no media type"
            }
            parameters.forEach { (_, value) -> FieldGrammar.quote(value) }
            val lowered = parameters.map { (name, value) -> normalized(name.lowercase(), value) }
            return MediaType(type.lowercase(), subtype.lowercase(), lowered)
        }

        internal fun typeOf(parts: MediaTypeParts): MediaType? {
            val isConcrete = parts.type != WILDCARD && parts.subtype != WILDCARD && WILDCARD !in parts.subtype
            return MediaType(parts.type, parts.subtype, parts.parameters).takeIf { isConcrete }
        }

        private fun isConcrete(part: String): Boolean = FieldGrammar.isToken(part) && WILDCARD !in part

        private fun normalized(name: String, value: String): Pair<String, String> {
            if (name == CHARSET) return name to value.lowercase()
            return name to value
        }
    }
}
