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

package com.davils.kapi.runtime.core.pattern

import com.davils.kore.core.annotation.DavilsInternal

/**
 * The last code point of Unicode, `U+10FFFF`.
 *
 * Internal to kapi.
 *
 * @since 1.0.0
 */
@DavilsInternal
public const val LAST_CODE_POINT: Int = 0x10FFFF

private const val CODE_POINT_LIMIT: Int = LAST_CODE_POINT + 1
private const val END_BITS: Int = 21
private const val END_MASK: Long = (1L shl END_BITS) - 1

/**
 * An immutable set of code points, held as sorted ranges, that answers membership in logarithmic time.
 *
 * Internal to kapi: the IDNA rules of `kapi-spec-schema` build their tables with it. It is safe to share between
 * threads.
 *
 * @since 1.0.0
 */
@DavilsInternal
public class CodePointSet private constructor(private val bounds: IntArray) {
    internal val isEmpty: Boolean get() = bounds.isEmpty()

    /**
     * Returns whether [codePoint] is in this set.
     *
     * @param codePoint the code point; any integer is accepted.
     * @return `true` if this set holds [codePoint].
     * @since 1.0.0
     */
    public operator fun contains(codePoint: Int): Boolean {
        var low = 0
        var high = bounds.size
        while (low < high) {
            val middle = low + high ushr 1
            val isAtOrBelow = bounds[middle] <= codePoint
            if (isAtOrBelow) low = middle + 1
            if (!isAtOrBelow) high = middle
        }
        return low % 2 == 1
    }

    internal fun bounds(): IntArray = bounds.copyOf()

    internal fun union(other: CodePointSet): CodePointSet {
        val ranges = Ranges()
        ranges.addBounds(bounds)
        ranges.addBounds(other.bounds)
        return ranges.toSet()
    }

    internal fun complement(): CodePointSet {
        val isStartingAtZero = bounds.isNotEmpty() && bounds[0] == 0
        val isEndingAtLimit = bounds.isNotEmpty() && bounds[bounds.size - 1] == CODE_POINT_LIMIT
        val innerStart = 0.takeUnless { isStartingAtZero } ?: 1
        val inner = bounds.copyOfRange(fromIndex = innerStart, toIndex = innerEnd(isEndingAtLimit))
        val head = intArrayOf(0).takeUnless { isStartingAtZero } ?: IntArray(0)
        val tail = intArrayOf(CODE_POINT_LIMIT).takeUnless { isEndingAtLimit } ?: IntArray(0)
        return CodePointSet(head + inner + tail)
    }

    private fun innerEnd(isEndingAtLimit: Boolean): Int = (bounds.size - 1).takeIf { isEndingAtLimit } ?: bounds.size

    /**
     * Collects ranges of code points for a [CodePointSet]; overlapping and adjacent ranges are merged.
     *
     * A collector is meant for one thread.
     *
     * @since 1.0.0
     */
    public class Ranges {
        private var packed: LongArray = LongArray(INITIAL_CAPACITY)
        private var size: Int = 0

        /**
         * Adds the code points from [first] to [last], both included.
         *
         * @param first the first code point, from 0 to [LAST_CODE_POINT].
         * @param last the last code point, from [first] to [LAST_CODE_POINT].
         * @since 1.0.0
         */
        public fun add(first: Int, last: Int) {
            if (size == packed.size) compact()
            packed[size] = (first.toLong() shl END_BITS) or (last + 1).toLong()
            size++
        }

        internal fun addSingle(codePoint: Int) {
            add(first = codePoint, last = codePoint)
        }

        internal fun addBounds(bounds: IntArray) {
            var index = 0
            while (index < bounds.size) {
                add(first = bounds[index], last = bounds[index + 1] - 1)
                index += 2
            }
        }

        /**
         * Returns the set of every code point added so far.
         *
         * @return the set.
         * @since 1.0.0
         */
        public fun toSet(): CodePointSet = CodePointSet(merged())

        private fun compact() {
            val bounds = merged()
            val isCrowded = bounds.size >= packed.size
            val factor = 2.takeIf { isCrowded } ?: 1
            packed = LongArray(packed.size * factor)
            size = 0
            var index = 0
            while (index < bounds.size) {
                packed[size] = (bounds[index].toLong() shl END_BITS) or bounds[index + 1].toLong()
                size++
                index += 2
            }
        }

        private fun merged(): IntArray {
            val sorted = packed.copyOf(size)
            sorted.sort()
            val merged = IntArray(size * 2)
            var count = 0
            sorted.forEach { range ->
                val start = (range shr END_BITS).toInt()
                val end = (range and END_MASK).toInt()
                val isExtendingLast = count > 0 && start <= merged[count - 1]
                if (isExtendingLast) merged[count - 1] = merged[count - 1].coerceAtLeast(end)
                if (!isExtendingLast) {
                    merged[count] = start
                    merged[count + 1] = end
                    count += 2
                }
            }
            return merged.copyOf(count)
        }

        private companion object {
            const val INITIAL_CAPACITY: Int = 8
        }
    }

    internal companion object {
        val Empty: CodePointSet = CodePointSet(IntArray(0))

        val All: CodePointSet = CodePointSet(intArrayOf(0, CODE_POINT_LIMIT))

        fun of(first: Int, last: Int): CodePointSet = CodePointSet(intArrayOf(first, last + 1))

        fun single(codePoint: Int): CodePointSet = of(first = codePoint, last = codePoint)
    }
}
