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

private const val DIGITS: String = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-_"
private const val DIGIT_BITS: Int = 6
private const val LENGTH_WIDTH: Int = 2
private const val BINARY_LENGTH_WIDTH: Int = 3

internal class UnicodeRuns private constructor(private val starts: IntArray, private val values: IntArray) {
    fun valueOf(codePoint: Int): Int {
        var low = 0
        var high = starts.size - 1
        while (low < high) {
            val middle = low + high + 1 ushr 1
            val isAtOrBelow = starts[middle] <= codePoint
            if (isAtOrBelow) low = middle
            if (!isAtOrBelow) high = middle - 1
        }
        return values[low]
    }

    fun setOf(accepts: (Int) -> Boolean): CodePointSet {
        val ranges = CodePointSet.Ranges()
        starts.indices.forEach { index ->
            val last = (starts.getOrNull(index + 1) ?: (LAST_CODE_POINT + 1)) - 1
            val isAccepted = accepts(values[index]) && last >= starts[index]
            if (isAccepted) ranges.add(starts[index], last)
        }
        return ranges.toSet()
    }

    companion object {
        fun valued(encoded: String, valueWidth: Int): UnicodeRuns {
            val step = valueWidth + LENGTH_WIDTH
            val count = encoded.length / step
            val starts = IntArray(count)
            val values = IntArray(count)
            var start = 0
            repeat(count) { index ->
                val offset = index * step
                starts[index] = start
                values[index] = number(encoded, offset = offset, width = valueWidth)
                start += number(encoded, offset = offset + valueWidth, width = LENGTH_WIDTH)
            }
            return UnicodeRuns(starts = starts, values = values)
        }

        fun binary(encoded: String): UnicodeRuns {
            val count = encoded.length / BINARY_LENGTH_WIDTH
            val starts = IntArray(count)
            val values = IntArray(count)
            var start = 0
            repeat(count) { index ->
                starts[index] = start
                values[index] = index % 2
                start += number(encoded, offset = index * BINARY_LENGTH_WIDTH, width = BINARY_LENGTH_WIDTH)
            }
            return UnicodeRuns(starts = starts, values = values)
        }

        private fun number(encoded: String, offset: Int, width: Int): Int {
            var value = 0
            repeat(width) { index -> value = (value shl DIGIT_BITS) or DIGITS.indexOf(encoded[offset + index]) }
            return value
        }
    }
}
