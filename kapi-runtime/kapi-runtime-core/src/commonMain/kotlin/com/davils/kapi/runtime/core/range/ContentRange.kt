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

package com.davils.kapi.runtime.core.range

import com.davils.kapi.runtime.core.range.internal.RangeNumbers

private const val UNIT: String = "bytes "

private const val UNKNOWN: String = "*"

private const val MAX_LENGTH: Int = 128

/**
 * The value of `Content-Range` of RFC 9110 §14.4 for the unit `bytes`.
 *
 * Its [toString] is the field value; every part is a number, so it can carry nothing but what it says.
 *
 * @since 1.0.0
 */
public sealed interface ContentRange {
    /**
     * A range that is sent, such as `bytes 42-1233/1234` or `bytes 42-1233&#47;*` when the length is unknown.
     *
     * @property range the range.
     * @property completeLength the length of the whole representation, or `null` if it is unknown.
     * @throws IllegalArgumentException if the range does not lie inside [completeLength].
     * @since 1.0.0
     */
    public class Satisfied(public val range: ResolvedRange, public val completeLength: Long? = null) : ContentRange {
        init {
            require(completeLength == null || range.last < completeLength) { "A range lies inside its representation" }
        }

        override fun equals(other: Any?): Boolean =
            other is Satisfied && range == other.range && completeLength == other.completeLength

        override fun hashCode(): Int = listOf(range, completeLength).hashCode()

        override fun toString(): String = "$UNIT$range/${completeLength ?: UNKNOWN}"
    }

    /**
     * The answer to an unsatisfiable range, such as `bytes *&#47;1234`.
     *
     * @property completeLength the length of the whole representation.
     * @throws IllegalArgumentException if [completeLength] is negative.
     * @since 1.0.0
     */
    public class Unsatisfied(public val completeLength: Long) : ContentRange {
        init {
            require(completeLength >= 0) { "A representation has no negative length" }
        }

        override fun equals(other: Any?): Boolean = other is Unsatisfied && completeLength == other.completeLength

        override fun hashCode(): Int = completeLength.hashCode()

        override fun toString(): String = "$UNIT$UNKNOWN/$completeLength"
    }

    /**
     * Reads `Content-Range`.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Reads the value of `Content-Range` with the unit `bytes`.
         *
         * @param text the field value, which may come from an attacker.
         * @return the content range, or `null` if [text] is malformed, has another unit, or names a range outside
         * its length.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String): ContentRange? {
            val isBytes = text.length <= MAX_LENGTH && text.startsWith(UNIT)
            val rest = text.removePrefix(UNIT).takeIf { isBytes } ?: return null
            val range = rest.substringBefore('/')
            val length = rest.substringAfter('/', "")
            return unsatisfied(range, length) ?: satisfied(range, length)
        }

        private fun unsatisfied(range: String, length: String): ContentRange? {
            val complete = RangeNumbers.parse(length)
            return complete?.takeIf { range == UNKNOWN }?.let(::Unsatisfied)
        }

        private fun satisfied(range: String, length: String): ContentRange? {
            val first = RangeNumbers.parse(range.substringBefore('-'))
            val last = RangeNumbers.parse(range.substringAfter('-', ""))
            val complete = RangeNumbers.parse(length)
            val isUnknown = length == UNKNOWN
            val isValid = first != null && last != null && first <= last
            val isInside = isUnknown || (complete != null && last != null && last < complete)
            if (!isValid || !isInside) return null
            return Satisfied(ResolvedRange(first, last), complete)
        }
    }
}
