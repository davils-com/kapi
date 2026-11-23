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

package com.davils.kapi.runtime.core.conditional

import com.davils.kapi.runtime.core.internal.FieldGrammar
import com.davils.kapi.runtime.core.internal.FieldScanner
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

private const val WEAK: String = "W/"

private const val QUOTE: Char = '"'

private const val HASH_FACTOR: Int = 31

private const val MAX_LENGTH: Int = 8_192

/**
 * An entity tag of RFC 9110 §8.8.3, the value of `ETag` such as `"xyzzy"` or the weak `W/"xyzzy"`.
 *
 * The opaque tag consists of the visible ASCII characters but the double quote; the obsolete octets RFC 9110 still
 * tolerates are refused, so [toString] is always a valid field value. An entity tag is immutable.
 *
 * @property opaque the opaque tag without quotes.
 * @property isWeak whether the tag is weak, marked by `W/`.
 * @throws IllegalArgumentException if [opaque] holds a character an entity tag cannot carry, such as a quote, a space
 * or a line break.
 * @since 1.0.0
 */
public class EntityTag(public val opaque: String, public val isWeak: Boolean = false) {
    init {
        require(opaque.all(::isTagChar)) { "${SafeText.quoted(opaque)} is no opaque entity tag" }
    }

    /**
     * Compares strongly by RFC 9110 §8.8.3.2: both tags are strong and their opaque tags are equal.
     *
     * @param other the other tag.
     * @return `true` if the tags match strongly.
     * @since 1.0.0
     */
    public fun matchesStrongly(other: EntityTag): Boolean = !isWeak && !other.isWeak && opaque == other.opaque

    /**
     * Compares weakly by RFC 9110 §8.8.3.2: the opaque tags are equal, whether either tag is weak or not.
     *
     * @param other the other tag.
     * @return `true` if the tags match weakly.
     * @since 1.0.0
     */
    public fun matchesWeakly(other: EntityTag): Boolean = opaque == other.opaque

    override fun equals(other: Any?): Boolean = other is EntityTag && opaque == other.opaque && isWeak == other.isWeak

    override fun hashCode(): Int = opaque.hashCode() * HASH_FACTOR + isWeak.hashCode()

    override fun toString(): String {
        val prefix = WEAK.takeIf { isWeak } ?: ""
        return "$prefix$QUOTE$opaque$QUOTE"
    }

    /**
     * Reads entity tags.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Reads [text], with blanks around it ignored, as an entity tag.
         *
         * @param text the text, which may come from an attacker.
         * @return the tag, or `null` if [text] is none or longer than 8192 characters.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String): EntityTag? {
            if (text.length > MAX_LENGTH) return null
            val scanner = FieldScanner(FieldGrammar.trimmed(text))
            return read(scanner)?.takeIf { scanner.isAtEnd }
        }

        /**
         * Reads [text] like [parseOrNull].
         *
         * @param text the text.
         * @return the tag.
         * @throws IllegalArgumentException if [text] is no entity tag.
         * @since 1.0.0
         */
        public fun parse(text: String): EntityTag =
            requireNotNull(parseOrNull(text)) { "${SafeText.quoted(text)} is no entity tag such as \"v1\"" }

        internal fun read(scanner: FieldScanner): EntityTag? {
            val isWeak = scanner.consume(WEAK)
            val isOpen = scanner.consume(QUOTE)
            val opaque = scanner.span(::isTagChar).orEmpty()
            val isClosed = isOpen && scanner.consume(QUOTE)
            return EntityTag(opaque, isWeak).takeIf { isClosed }
        }

        private fun isTagChar(char: Char): Boolean = char in '!'..'~' && char != QUOTE
    }
}
