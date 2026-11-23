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

package com.davils.kapi.runtime.core.validation

import com.davils.kapi.runtime.core.internal.DecimalDigits
import com.davils.kapi.runtime.core.internal.FieldGrammar
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

private const val ZERO: String = "0"

private const val MINUS: Char = '-'

private const val PLUS: Char = '+'

private const val SIGN: String = "-"

private const val POINT: Char = '.'

private const val HASH_FACTOR: Int = 31

private const val MOST_PLAIN_ZEROS: Int = 21

/**
 * An exact decimal number, such as `0.3` or `-12.5E-7`, for the numeric constraints of JSON Schema that a `Double`
 * cannot check exactly, such as `multipleOf: 0.1`.
 *
 * A decimal is kept as its significant digits and a power of ten, so `1.0`, `1` and `10E-1` are the same number and
 * equal. It is read from the number grammar of JSON (RFC 8259 §6), which also covers what `Double.toString` writes on
 * the JVM and on Wasm. Text longer than [MAX_TEXT_LENGTH] or with an exponent beyond ±[MAX_EXPONENT] is refused, which
 * bounds the work of [compareTo] and [isMultipleOf] whatever the input. A decimal is immutable and safe to share
 * between threads.
 *
 * @since 1.0.0
 */
public class Decimal private constructor(
    private val isNegative: Boolean,
    private val digits: String,
    private val exponent: Int,
) : Comparable<Decimal> {
    /**
     * The sign: `-1`, `0` or `1`.
     *
     * @since 1.0.0
     */
    public val sign: Int get() = (-1).takeIf { isNegative } ?: (0.takeIf { digits == ZERO } ?: 1)

    /**
     * Whether the decimal is a whole number.
     *
     * @since 1.0.0
     */
    public val isInteger: Boolean get() = exponent >= 0

    /**
     * Returns whether this decimal is a whole multiple of [factor], exactly.
     *
     * @param factor the factor.
     * @return `true` if this decimal divided by [factor] is a whole number; `false` if [factor] is zero.
     * @since 1.0.0
     */
    public fun isMultipleOf(factor: Decimal): Boolean {
        if (factor.digits == ZERO) return false
        val shift = exponent - factor.exponent
        return digits == ZERO || (shift >= 0 && DecimalDigits.isDividedBy(digits, shift, factor.digits))
    }

    override fun compareTo(other: Decimal): Int {
        if (sign != other.sign) return sign.compareTo(other.sign)
        return magnitudeCompared(other) * sign
    }

    override fun equals(other: Any?): Boolean =
        other is Decimal && isNegative == other.isNegative && digits == other.digits && exponent == other.exponent

    override fun hashCode(): Int = (digits.hashCode() * HASH_FACTOR + exponent) * HASH_FACTOR + isNegative.hashCode()

    override fun toString(): String {
        val sign = SIGN.takeIf { isNegative }.orEmpty()
        return sign + magnitudeText()
    }

    private fun magnitudeCompared(other: Decimal): Int {
        val adjusted = digits.length + exponent
        val otherAdjusted = other.digits.length + other.exponent
        if (adjusted != otherAdjusted) return adjusted.compareTo(otherAdjusted)
        val width = maxOf(a = digits.length, b = other.digits.length)
        return DecimalDigits.compareMagnitude(left = digits.padEnd(width, '0'), right = other.digits.padEnd(width, '0'))
    }

    private fun magnitudeText(): String {
        val point = digits.length + exponent
        val isScientific = exponent > MOST_PLAIN_ZEROS || point <= -MOST_PLAIN_ZEROS
        if (isScientific) return "${digits}E$exponent"
        return plainText(point)
    }

    private fun plainText(point: Int): String {
        if (exponent >= 0) return digits + ZERO.repeat(exponent)
        val whole = digits.take(maxOf(a = point, b = 0)).ifEmpty { ZERO }
        val fraction = ZERO.repeat(maxOf(a = -point, b = 0)) + digits.drop(maxOf(a = point, b = 0))
        return whole + POINT + fraction
    }

    /**
     * Reads and makes decimals.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The most characters of the text of a decimal.
         *
         * @since 1.0.0
         */
        public const val MAX_TEXT_LENGTH: Int = 1_024

        /**
         * The largest magnitude of the exponent of a decimal, after its digits are normalized.
         *
         * @since 1.0.0
         */
        public const val MAX_EXPONENT: Int = 1_000

        /**
         * Reads [text] as a number of the JSON grammar, such as `-0.5`, `12` or `1.5E-7`.
         *
         * @param text the text, which may come from an attacker.
         * @return the decimal, or `null` if [text] is no number of the grammar, is longer than [MAX_TEXT_LENGTH] or
         * has an exponent beyond ±[MAX_EXPONENT].
         * @since 1.0.0
         */
        public fun parseOrNull(text: String): Decimal? {
            if (text.length > MAX_TEXT_LENGTH) return null
            return Reading(text).read()
        }

        /**
         * Reads [text] like [parseOrNull].
         *
         * @param text the text.
         * @return the decimal.
         * @throws IllegalArgumentException if [text] is no number or exceeds a limit.
         * @since 1.0.0
         */
        public fun parse(text: String): Decimal =
            requireNotNull(parseOrNull(text)) { "${SafeText.quoted(text)} is no decimal number such as 0.25" }

        /**
         * Returns the decimal of [value].
         *
         * @param value the whole number.
         * @return the decimal.
         * @since 1.0.0
         */
        public fun of(value: Long): Decimal = parse(value.toString())

        /**
         * Returns the decimal of the shortest text that reads back as [value], such as `0.1` for `0.1`.
         *
         * @param value the finite number.
         * @return the decimal, or `null` if [value] is not finite.
         * @since 1.0.0
         */
        public fun ofOrNull(value: Double): Decimal? {
            val text = value.takeIf(Double::isFinite)?.toString()
            return text?.let(::parseOrNull)
        }

        internal fun normalized(isNegative: Boolean, digits: String, exponent: Int): Decimal? {
            val significant = digits.trimStart('0')
            if (significant.isEmpty()) return Decimal(isNegative = false, digits = ZERO, exponent = 0)
            val trimmed = significant.trimEnd('0')
            val normalizedExponent = exponent + significant.length - trimmed.length
            val isBounded = normalizedExponent in -MAX_EXPONENT..MAX_EXPONENT
            return Decimal(isNegative, trimmed, normalizedExponent).takeIf { isBounded }
        }
    }

    private class Reading(private val text: String) {
        private var position: Int = 0

        fun read(): Decimal? {
            val isNegative = consume(MINUS)
            val whole = digitsOrNull().orEmpty()
            val hasPoint = consume(POINT)
            val fraction = digitsOrNull()
            val power = exponentOrNull()
            val isWhole = whole == ZERO || (whole.isNotEmpty() && !whole.startsWith('0'))
            val isComplete = hasPoint == (fraction != null) && position == text.length
            if (!isWhole || !isComplete || power == null) return null
            return normalized(isNegative, whole + fraction.orEmpty(), power - fraction.orEmpty().length)
        }

        private fun exponentOrNull(): Int? {
            val hasExponent = consume('e') || consume('E')
            if (!hasExponent) return 0
            val isNegative = consume(MINUS)
            if (!isNegative) consume(PLUS)
            val significant = digitsOrNull()?.trimStart('0')
            val value = significant?.takeIf { digits -> digits.length < MOST_EXPONENT_DIGITS }
            val magnitude = value?.run { ifEmpty { ZERO }.toInt() }
            return magnitude?.let { number -> (-number).takeIf { isNegative } ?: number }
        }

        private fun digitsOrNull(): String? {
            val start = position
            while (position < text.length && FieldGrammar.isDigit(text[position])) position += 1
            return text.substring(startIndex = start, endIndex = position).takeIf(String::isNotEmpty)
        }

        private fun consume(char: Char): Boolean {
            if (text.getOrNull(position) != char) return false
            position += 1
            return true
        }

        private companion object {
            const val MOST_EXPONENT_DIGITS: Int = 7
        }
    }
}
