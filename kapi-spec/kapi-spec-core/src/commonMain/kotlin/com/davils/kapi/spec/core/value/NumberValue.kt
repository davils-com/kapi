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

package com.davils.kapi.spec.core.value

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.parseDecimalOrNull

private const val MAXIMUM_LITERAL_LENGTH: Int = 512

private const val MAXIMUM_EXPONENT: Int = 4096

private const val LONG_DIGITS: Int = 19

private const val HASH_FACTOR: Int = 31

/**
 * A number of a document, such as the `minimum`, the `multipleOf` or an example of a schema, kept exactly as it was
 * written.
 *
 * A number is read with [parseOrNull], which accepts exactly the number grammar of JSON (RFC 8259, section 6): an
 * optional minus, an integer part without leading zeros, an optional fraction and an optional exponent. Its value is
 * held as a decimal without rounding, so `0.1` stays `0.1` and `1e400` stays representable, and two numbers are
 * [equal][equals] and [compare][compareTo] by value: `1`, `1.0` and `10e-1` are equal. The [literal] keeps the text as
 * it was written.
 *
 * Reading is bounded: a literal longer than 512 characters, or a value whose decimal exponent lies outside
 * -4096..4096, is refused, so a document cannot make a comparison or a conversion expensive. Reading is a hand-written
 * scanner that runs in time linear in the length of the text.
 *
 * A number is immutable and safe to share between threads.
 *
 * @property literal the text of the number as it was written, such as `1.50`.
 * @since 1.0.0
 */
public class NumberValue private constructor(
    public val literal: String,
    private val sign: Int,
    private val digits: String,
    private val power: Int,
) : Comparable<NumberValue> {
    /**
     * Whether the value is a whole number, such as `3`, `3.0` or `3e2`.
     *
     * @since 1.0.0
     */
    public val isIntegral: Boolean get() = power >= 0

    private val magnitude: Int get() = digits.length + power

    /**
     * Returns the nearest double, ties to even, on every platform alike.
     *
     * @return the nearest double; an infinity with the sign of the value when it is too large for a double, and zero
     * with its sign when it is too small.
     * @since 1.0.0
     */
    public fun toDouble(): Double {
        val parsed = parseDecimalOrNull(literal)
        return parsed ?: (Double.POSITIVE_INFINITY * sign)
    }

    /**
     * Returns the value as a [Long] if it is whole and fits.
     *
     * @return the value, or `null` if it has a fraction or lies outside the range of [Long].
     * @since 1.0.0
     */
    public fun toLongOrNull(): Long? {
        val fits = isIntegral && magnitude <= LONG_DIGITS
        if (!fits) return null
        val minus = "-".repeat(-sign.coerceAtMost(0))
        return (minus + digits.ifEmpty { "0" } + "0".repeat(power)).toLongOrNull()
    }

    /**
     * Returns the value as an [Int] if it is whole and fits.
     *
     * @return the value, or `null` if it has a fraction or lies outside the range of [Int].
     * @since 1.0.0
     */
    public fun toIntOrNull(): Int? {
        val long = toLongOrNull() ?: return null
        return long.toInt().takeIf { long in Int.MIN_VALUE..Int.MAX_VALUE }
    }

    override fun compareTo(other: NumberValue): Int {
        val signs = sign.compareTo(other.sign)
        if (signs != 0 || digits.isEmpty()) return signs
        return compareMagnitude(other) * sign
    }

    private fun compareMagnitude(other: NumberValue): Int {
        val positions = magnitude.compareTo(other.magnitude)
        if (positions != 0) return positions
        val length = maxOf(digits.length, other.digits.length)
        return digits.padEnd(length, '0').compareTo(other.digits.padEnd(length, '0'))
    }

    override fun equals(other: Any?): Boolean = other is NumberValue && compareTo(other) == 0

    override fun hashCode(): Int {
        val significand = HASH_FACTOR * sign + digits.hashCode()
        return HASH_FACTOR * significand + power
    }

    override fun toString(): String = literal

    /**
     * Creates and reads numbers.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Returns the number of [value].
         *
         * @param value the value.
         * @return the number, whose [literal] is the decimal text of [value].
         * @since 1.0.0
         */
        public fun of(value: Long): NumberValue {
            val literal = value.toString()
            return requireNotNull(parseOrNull(literal)) { "Every Long is a JSON number" }
        }

        /**
         * Reads [text] as a JSON number.
         *
         * @param text the text, which may contain anything.
         * @return the number, or `null` if [text] is no JSON number, is longer than 512 characters or has a decimal
         * exponent outside -4096..4096.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String): NumberValue? {
            if (text.length > MAXIMUM_LITERAL_LENGTH) return null
            return NumberScanner(text).scan()
        }

        internal fun of(
            literal: String,
            isNegative: Boolean,
            integer: String,
            fraction: String,
            exponent: Int,
        ): NumberValue? {
            val all = (integer + fraction).trimStart('0')
            val significand = all.trimEnd('0')
            val power = exponent - fraction.length + (all.length - significand.length)
            val negative = (-1).takeIf { isNegative } ?: 1
            val value = NumberValue(literal, negative * significand.length.coerceAtMost(1), significand, power)
            val inRange = significand.isEmpty() || value.magnitude in -MAXIMUM_EXPONENT..MAXIMUM_EXPONENT
            return value.takeIf { inRange }
        }
    }
}
