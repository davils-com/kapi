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

private const val WILDCARD: String = "*"

private const val SUFFIX_WILDCARD: String = "*+"

private const val HASH_FACTOR: Int = 31

private const val ANY_SPECIFICITY: Int = 0

private const val FAMILY_SPECIFICITY: Int = 1_000

private const val SUFFIX_SPECIFICITY: Int = 2_000

private const val EXACT_SPECIFICITY: Int = 3_000

/**
 * A media range of RFC 9110 §12.5.1: a [MediaType] or a set of them, such as `*&#47;*`, `text&#47;*` or
 * `text/plain; charset=utf-8`.
 *
 * Besides the ranges of RFC 9110, a range may name every type with a structured syntax suffix of RFC 6838, such as
 * `application&#47;*+json` or `*&#47;*+json`, which codecs use to claim every JSON-based type.
 *
 * [matches] compares the type and the subtype only, which is how a codec is chosen for a content type; [includes]
 * also demands every parameter of the range, which is how `Accept` is read. Names are compared without case and kept
 * in lower case. A media range is immutable and safe to share between threads.
 *
 * @property type the top-level type in lower case, or `*`.
 * @property subtype the subtype in lower case, `*` or `*+suffix`.
 * @property parameters the parameters in the order they were written, with their names in lower case.
 * @since 1.0.0
 */
public class MediaRange private constructor(
    public val type: String,
    public val subtype: String,
    public val parameters: List<Pair<String, String>>,
) {
    /**
     * How specific the range is: an exact type ranks above a suffix range, which ranks above a family such as
     * `text&#47;*`, which ranks above `*&#47;*`, and among ranges of the same kind the one with more parameters ranks
     * first.
     *
     * @since 1.0.0
     */
    public val specificity: Int get() = kindSpecificity() + parameters.size

    /**
     * Returns whether [mediaType] lies in this range by its type and subtype, whatever its parameters.
     *
     * @param mediaType the media type.
     * @return `true` if the type and the subtype lie in this range.
     * @since 1.0.0
     */
    public fun matches(mediaType: MediaType): Boolean {
        val isFamily = type == WILDCARD || type == mediaType.type
        return isFamily && matchesSubtype(mediaType)
    }

    /**
     * Reads [mediaType] and returns whether it lies in this range like [matches].
     *
     * @param mediaType the text of a media type, which may come from an attacker, or `null`.
     * @return `true` if [mediaType] is a media type that lies in this range; `false` for `null` and for text that is
     * no media type.
     * @since 1.0.0
     */
    public fun matches(mediaType: String?): Boolean {
        val parsed = mediaType?.let(MediaType::parseOrNull) ?: return false
        return matches(parsed)
    }

    /**
     * Returns whether [mediaType] lies in this range by RFC 9110: its type and subtype match and it carries every
     * parameter of this range with the same value.
     *
     * @param mediaType the media type.
     * @return `true` if [mediaType] lies in this range.
     * @since 1.0.0
     */
    public fun includes(mediaType: MediaType): Boolean {
        val hasParameters = parameters.all { (name, value) -> mediaType.parameter(name) == value }
        return matches(mediaType) && hasParameters
    }

    override fun equals(other: Any?): Boolean =
        other is MediaRange && type == other.type && subtype == other.subtype && parameters == other.parameters

    override fun hashCode(): Int = (type.hashCode() * HASH_FACTOR + subtype.hashCode()) *
        HASH_FACTOR +
        parameters.hashCode()

    override fun toString(): String {
        val written = parameters.joinToString("") { (name, value) -> "; $name=${FieldGrammar.tokenOrQuoted(value)}" }
        return "$type/$subtype$written"
    }

    private fun matchesSubtype(mediaType: MediaType): Boolean {
        if (subtype == WILDCARD || subtype == mediaType.subtype) return true
        val suffix = subtype.removePrefix(SUFFIX_WILDCARD).takeIf { subtype.startsWith(SUFFIX_WILDCARD) }
        return suffix != null && suffix == mediaType.suffix
    }

    private fun kindSpecificity(): Int {
        if (subtype == WILDCARD) return ANY_SPECIFICITY.takeIf { type == WILDCARD } ?: FAMILY_SPECIFICITY
        return SUFFIX_SPECIFICITY.takeIf { subtype.startsWith(SUFFIX_WILDCARD) } ?: EXACT_SPECIFICITY
    }

    /**
     * Reads and makes media ranges, and the ranges the runtime knows by name.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * `*&#47;*`, every media type.
         *
         * @since 1.0.0
         */
        public val Any: MediaRange = of("*/*")

        /**
         * `application/json`.
         *
         * @since 1.0.0
         */
        public val Json: MediaRange = of("application/json")

        /**
         * Reads [text] as a media range.
         *
         * Whitespace around the text is ignored. Text longer than 4096 characters, with more than 64 parameters, with
         * a parameter named twice, with a wildcard type before a concrete subtype or with anything RFC 9110 does not
         * allow is refused.
         *
         * @param text the text, which may come from an attacker.
         * @return the media range, or `null` if [text] is none.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String): MediaRange? = MediaTypeReader.parse(text)?.let(::rangeOf)

        /**
         * Reads [text] as a media range like [parseOrNull].
         *
         * @param text the text, such as `application&#47;*+json`.
         * @return the media range.
         * @throws IllegalArgumentException if [text] is no media range.
         * @since 1.0.0
         */
        public fun of(text: String): MediaRange = requireNotNull(parseOrNull(text)) {
            "${SafeText.quoted(text)} is no media range such as application/json or text/*"
        }

        /**
         * Returns the range that holds exactly [mediaType] with its parameters.
         *
         * @param mediaType the media type.
         * @return the range.
         * @since 1.0.0
         */
        public fun of(mediaType: MediaType): MediaRange =
            MediaRange(type = mediaType.type, subtype = mediaType.subtype, parameters = mediaType.parameters)

        internal fun rangeOf(parts: MediaTypeParts): MediaRange? {
            val isAnyType = parts.type == WILDCARD
            val isWildSubtype = parts.subtype == WILDCARD || parts.subtype.startsWith(SUFFIX_WILDCARD)
            val isSubtypeValid = isWildSubtype || WILDCARD !in parts.subtype
            val isValid = isSubtypeValid && (!isAnyType || isWildSubtype) && WILDCARD !in parts.type.drop(1)
            return MediaRange(
                type = parts.type,
                subtype = parts.subtype,
                parameters = parts.parameters,
            ).takeIf { isValid }
        }
    }
}
