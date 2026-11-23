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

private const val HASH_FACTOR: Int = 31

internal class Decimal private constructor(
    private val isNegative: Boolean,
    val digits: String,
    val exponent: Long,
    val isPlain: Boolean,
) : Comparable<Decimal> {
    val isZero: Boolean get() = digits.isEmpty()

    val isInteger: Boolean get() = isZero || exponent >= 0L

    val sign: Int
        get() {
            val nonZero = (-1).takeIf { isNegative } ?: 1
            return 0.takeIf { isZero } ?: nonZero
        }

    val canonical: String
        get() {
            val minus = "-".takeIf { isNegative } ?: ""
            return "0".takeIf { isZero } ?: "$minus${digits}e$exponent"
        }

    private val magnitude: Long get() = digits.length + exponent

    override fun compareTo(other: Decimal): Int {
        val signs = sign.compareTo(other.sign)
        if (signs != 0 || isZero) return signs
        return compareMagnitude(other) * sign
    }

    private fun compareMagnitude(other: Decimal): Int {
        val positions = magnitude.compareTo(other.magnitude)
        if (positions != 0) return positions
        val length = maxOf(digits.length, other.digits.length)
        return digits.padEnd(length, '0').compareTo(other.digits.padEnd(length, '0'))
    }

    override fun equals(other: Any?): Boolean = other is Decimal && compareTo(other) == 0

    override fun hashCode(): Int = HASH_FACTOR * canonical.hashCode()

    override fun toString(): String = canonical

    companion object {
        const val MAXIMUM_LENGTH: Int = 4096

        fun parse(text: String): Decimal? {
            val bounded = text.takeIf { it.length in 1..MAXIMUM_LENGTH }
            return bounded?.let { DecimalScanner(it).scan() }
        }

        fun of(isNegative: Boolean, integer: String, fraction: String, exponent: Long, isPlain: Boolean): Decimal {
            val all = (integer + fraction).trimStart('0')
            val significand = all.trimEnd('0')
            val power = exponent - fraction.length + (all.length - significand.length)
            val isZero = significand.isEmpty()
            val normalized = 0L.takeIf { isZero } ?: power
            return Decimal(isNegative && !isZero, significand, normalized, isPlain)
        }
    }
}
