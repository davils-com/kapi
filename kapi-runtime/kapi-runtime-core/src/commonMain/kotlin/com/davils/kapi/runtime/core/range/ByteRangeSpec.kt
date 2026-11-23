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

private const val HASH_FACTOR: Int = 31

/**
 * One range of a `Range: bytes=…` field of RFC 9110 §14.1.1.
 *
 * @since 1.0.0
 */
public sealed interface ByteRangeSpec {
    /**
     * An int-range such as `0-499` or the open `500-`.
     *
     * @property first the first byte, counted from zero.
     * @property last the last byte, included, or `null` for the rest of the representation.
     * @throws IllegalArgumentException if [first] is negative or [last] lies before it.
     * @since 1.0.0
     */
    public class FromTo(public val first: Long, public val last: Long? = null) : ByteRangeSpec {
        init {
            require(first >= 0 && (last == null || last >= first)) { "A byte range runs forward from byte 0 on" }
        }

        override fun equals(other: Any?): Boolean = other is FromTo && first == other.first && last == other.last

        override fun hashCode(): Int = first.hashCode() * HASH_FACTOR + last.hashCode()

        override fun toString(): String = "$first-${last ?: ""}"
    }

    /**
     * A suffix-range such as `-500`, the last bytes of the representation.
     *
     * @property length how many bytes from the end.
     * @throws IllegalArgumentException if [length] is negative.
     * @since 1.0.0
     */
    public class Suffix(public val length: Long) : ByteRangeSpec {
        init {
            require(length >= 0) { "A suffix range has no negative length" }
        }

        override fun equals(other: Any?): Boolean = other is Suffix && length == other.length

        override fun hashCode(): Int = length.hashCode()

        override fun toString(): String = "-$length"
    }
}
