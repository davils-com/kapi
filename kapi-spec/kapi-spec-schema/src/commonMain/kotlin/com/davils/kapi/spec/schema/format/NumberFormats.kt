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

package com.davils.kapi.spec.schema.format

import kotlin.math.abs

private const val MAXIMUM_EXPONENT_DIGITS: Int = 6
private const val EXPONENT_CEILING: Int = 10_000_000
private const val INT32_MAXIMUM: String = "2147483647"
private const val INT32_MINIMUM: String = "2147483648"
private const val INT64_MAXIMUM: String = "9223372036854775807"
private const val INT64_MINIMUM: String = "9223372036854775808"
private const val NOT_A_NUMBER: String = "the value is no JSON number"

internal object NumberFormats {
    fun int32(value: String): String? = integerReason(value, maximum = INT32_MAXIMUM, minimum = INT32_MINIMUM)

    fun int64(value: String): String? = integerReason(value, maximum = INT64_MAXIMUM, minimum = INT64_MINIMUM)

    fun float(value: String): String? {
        val number = JsonNumber.parse(value) ?: return NOT_A_NUMBER
        val magnitude = number.text.toDouble()
        val isInRange = magnitude.isFinite() && abs(magnitude) <= Float.MAX_VALUE.toDouble()
        return "the number lies outside the range of a 32-bit float".takeUnless { isInRange }
    }

    fun double(value: String): String? {
        val number = JsonNumber.parse(value) ?: return NOT_A_NUMBER
        return "the number lies outside the range of a 64-bit double".takeUnless { number.text.toDouble().isFinite() }
    }

    private fun integerReason(value: String, maximum: String, minimum: String): String? {
        val number = JsonNumber.parse(value) ?: return NOT_A_NUMBER
        val integer = number.integerDigits()
        val bound = minimum.takeIf { number.isNegative } ?: maximum
        val isInRange = integer != null && compareDigits(left = integer, right = bound) <= 0
        return "the number is no integer in the range of the format".takeUnless { isInRange }
    }

    private fun compareDigits(left: String, right: String): Int {
        val byLength = left.length.compareTo(right.length)
        return byLength.takeIf { it != 0 } ?: left.compareTo(right)
    }

    private class JsonNumber(
        val text: String,
        val isNegative: Boolean,
        val digits: String,
        val fractionLength: Int,
        val exponent: Int,
    ) {
        fun integerDigits(): String? {
            val significant = digits.trimStart('0')
            if (significant.isEmpty()) return "0"
            val scale = exponent - fractionLength
            val kept = significant.dropLast((-scale).coerceAtLeast(0))
            val droppedCount = (-scale).coerceIn(minimumValue = 0, maximumValue = significant.length)
            val dropped = significant.takeLast(droppedCount)
            val isIntegral = dropped.all { it == '0' } && -scale <= significant.length
            val zeros = "0".repeat(scale.coerceIn(minimumValue = 0, maximumValue = INT64_MAXIMUM.length + 1))
            return (kept + zeros).takeIf { isIntegral }
        }

        companion object {
            fun parse(text: String): JsonNumber? {
                val isNegative = text.startsWith('-')
                val start = 1.takeIf { isNegative } ?: 0
                val integerEnd = digitsEnd(text, start)
                val integer = text.substring(startIndex = start, endIndex = integerEnd)
                val isIntegerValid = integer == "0" || (integer.isNotEmpty() && integer[0] != '0')
                val fractionEnd = fractionEnd(text, integerEnd)
                val end = fractionEnd ?: integerEnd
                val exponent = exponentOf(text, end)
                val fraction = text.substring(startIndex = (integerEnd + 1).coerceAtMost(end), endIndex = end)
                val isValid = isIntegerValid && fractionEnd != null && exponent != null
                val number = JsonNumber(
                    text = text,
                    isNegative = isNegative,
                    digits = integer + fraction,
                    fractionLength = fraction.length,
                    exponent = exponent ?: 0,
                )
                return number.takeIf { isValid }
            }

            private fun digitsEnd(text: String, start: Int): Int {
                var end = start
                while (end < text.length && isDigit(text[end])) end++
                return end
            }

            private fun fractionEnd(text: String, start: Int): Int? {
                val hasFraction = text.getOrNull(start) == '.'
                if (!hasFraction) return start
                val end = digitsEnd(text, start + 1)
                return end.takeIf { it > start + 1 }
            }

            private fun exponentOf(text: String, start: Int): Int? {
                if (start == text.length) return 0
                val marker = text[start]
                val hasExponent = marker == 'e' || marker == 'E'
                val sign = text.getOrNull(start + 1)
                val hasSign = sign == '+' || sign == '-'
                val digitsStart = start + 1 + (1.takeIf { hasSign } ?: 0)
                val end = digitsEnd(text, digitsStart)
                val written = text.substring(startIndex = digitsStart.coerceAtMost(end), endIndex = end)
                val digits = written.trimStart('0').ifEmpty { "0" }
                val isValid = hasExponent && end == text.length && end > digitsStart
                val magnitude = digits.takeIf { it.length <= MAXIMUM_EXPONENT_DIGITS }?.toInt() ?: EXPONENT_CEILING
                val factor = (-1).takeIf { sign == '-' } ?: 1
                return (magnitude * factor).takeIf { isValid }
            }
        }
    }
}
