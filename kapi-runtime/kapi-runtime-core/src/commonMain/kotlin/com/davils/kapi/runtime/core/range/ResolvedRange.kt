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
 * A range of bytes that lies inside a representation: from [first] to [last], both included.
 *
 * @property first the first byte, counted from zero.
 * @property last the last byte, included.
 * @throws IllegalArgumentException if [first] is negative or [last] lies before it.
 * @since 1.0.0
 */
public class ResolvedRange(public val first: Long, public val last: Long) {
    init {
        require(first in 0..last) { "A resolved range runs forward from byte 0 on" }
    }

    /**
     * The number of bytes in the range.
     *
     * @since 1.0.0
     */
    public val length: Long get() = last - first + 1

    override fun equals(other: Any?): Boolean = other is ResolvedRange && first == other.first && last == other.last

    override fun hashCode(): Int = first.hashCode() * HASH_FACTOR + last.hashCode()

    override fun toString(): String = "$first-$last"
}
