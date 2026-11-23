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

package com.davils.kapi.spec.schema.internal.value

private const val MAXIMUM_EXPONENT_DIGITS: Int = 15

internal class DecimalScanner(private val text: String) {
    private var position: Int = 0

    private var isPlain: Boolean = true

    fun scan(): Decimal? {
        val isNegative = accept('-')
        val integer = integerPart()
        val fraction = integer?.let { fractionPart() }
        val exponent = fraction?.let { exponentPart() }
        val isComplete = integer != null && fraction != null && exponent != null && position == text.length
        if (!isComplete) return null
        return Decimal.of(isNegative, integer, fraction, exponent, isPlain)
    }

    private fun integerPart(): String? {
        val start = position
        val isZero = accept('0')
        if (!isZero) skipDigits()
        return text.substring(start, position).takeIf { it.isNotEmpty() }
    }

    private fun fractionPart(): String? {
        if (!accept('.')) return ""
        isPlain = false
        val start = position
        skipDigits()
        return text.substring(start, position).takeIf { it.isNotEmpty() }
    }

    private fun exponentPart(): Long? {
        if (!accept('e') && !accept('E')) return 0L
        isPlain = false
        val isNegative = accept('-')
        if (!isNegative) accept('+')
        val start = position
        skipDigits()
        val digits = text.substring(start, position)
        val significant = digits.trimStart('0')
        val isBounded = digits.isNotEmpty() && significant.length <= MAXIMUM_EXPONENT_DIGITS
        val sign = (-1L).takeIf { isNegative } ?: 1L
        return significant.ifEmpty { "0" }.takeIf { isBounded }?.let { it.toLong() * sign }
    }

    private fun accept(expected: Char): Boolean {
        val isMatch = position < text.length && text[position] == expected
        if (isMatch) position++
        return isMatch
    }

    private fun skipDigits() {
        while (position < text.length && text[position] in '0'..'9') position++
    }
}
