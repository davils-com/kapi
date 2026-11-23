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

package com.davils.kapi.spec.core.value

private const val MAXIMUM_EXPONENT_DIGITS: Int = 6

internal class NumberScanner(private val text: String) {
    private var index: Int = 0

    fun scan(): NumberValue? {
        val isNegative = accept('-')
        val parts = parts() ?: return null
        val (integer, fraction, exponent) = parts
        val value = NumberValue.of(
            literal = text,
            isNegative = isNegative,
            integer = integer,
            fraction = fraction,
            exponent = exponent,
        )
        return value.takeIf { index == text.length }
    }

    private fun parts(): Triple<String, String, Int>? {
        val integer = integer()
        val fraction = integer?.let { fraction() }
        val exponent = fraction?.let { exponent() }
        return exponent?.let { Triple(first = integer, second = fraction, third = exponent) }
    }

    private fun integer(): String? {
        if (accept('0')) return "0"
        return digits().takeIf { it.isNotEmpty() }
    }

    private fun fraction(): String? {
        if (!accept('.')) return ""
        return digits().takeIf { it.isNotEmpty() }
    }

    private fun exponent(): Int? {
        val hasExponent = accept('e') || accept('E')
        if (!hasExponent) return 0
        val isNegative = accept('-')
        if (!isNegative) accept('+')
        val digits = digits()
        val significant = digits.trimStart('0')
        val isBounded = digits.isNotEmpty() && significant.length <= MAXIMUM_EXPONENT_DIGITS
        val written = significant.ifEmpty { "0" }
        val value = written.takeIf { isBounded }?.toInt()
        val factor = (-1).takeIf { isNegative } ?: 1
        return value?.times(factor)
    }

    private fun digits(): String {
        val start = index
        while (index < text.length && text[index].isAsciiDigit()) index++
        return text.substring(startIndex = start, endIndex = index)
    }

    private fun accept(char: Char): Boolean {
        val isMatch = index < text.length && text[index] == char
        if (isMatch) index++
        return isMatch
    }

    private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'
}
