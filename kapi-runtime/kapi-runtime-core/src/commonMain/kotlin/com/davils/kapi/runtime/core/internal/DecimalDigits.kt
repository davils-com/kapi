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

package com.davils.kapi.runtime.core.internal

private const val BASE: Int = 10

internal object DecimalDigits {
    fun isDividedBy(digits: String, zeros: Int, divisor: String): Boolean {
        val remainder = Remainder(divisor.map(Char::digitToInt).toIntArray())
        digits.forEach { char -> remainder.push(char.digitToInt()) }
        repeat(zeros) { remainder.push(0) }
        return remainder.isZero
    }

    fun compareMagnitude(left: String, right: String): Int {
        if (left.length != right.length) return left.length.compareTo(right.length)
        return left.compareTo(right)
    }

    private class Remainder(private val divisor: IntArray) {
        private val value: IntArray = IntArray(divisor.size + 1)

        private var length: Int = 0

        val isZero: Boolean get() = length == 0

        fun push(digit: Int) {
            if (length > 0 || digit > 0) {
                value[length] = digit
                length += 1
            }
            while (isAtLeastDivisor()) subtract()
        }

        private fun isAtLeastDivisor(): Boolean {
            if (length != divisor.size) return length > divisor.size
            val first = (0 until length).firstOrNull { index -> value[index] != divisor[index] }
            return first == null || value[first] > divisor[first]
        }

        private fun subtract() {
            var borrow = 0
            val offset = length - divisor.size
            for (index in length - 1 downTo 0) {
                val subtrahend = divisor.getOrElse(index - offset) { 0 } + borrow
                val difference = value[index] - subtrahend
                borrow = 1.takeIf { difference < 0 } ?: 0
                value[index] = difference + BASE * borrow
            }
            val leading = (0 until length).firstOrNull { index -> value[index] != 0 } ?: length
            value.copyInto(value, destinationOffset = 0, startIndex = leading, endIndex = length)
            length -= leading
        }
    }
}
