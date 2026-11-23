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

import com.davils.kapi.spec.schema.internal.Meter

private const val LONG_DIGITS: Int = 17

private const val RADIX: Int = 10

private const val TWO: Int = 2

private const val FIVE: Int = 5

internal object Divisibility {
    fun isMultiple(value: Decimal, divisor: Decimal, meter: Meter): Boolean {
        if (value.isZero) return true
        val shift = value.exponent - divisor.exponent
        return shift >= 0L &&
            dividesScaled(
                value = digitsOf(value.digits),
                divisor = digitsOf(divisor.digits),
                shift = shift,
                meter = meter,
            )
    }

    private fun dividesScaled(value: IntArray, divisor: IntArray, shift: Long, meter: Meter): Boolean {
        meter.charge(divisor.size.toLong() * divisor.size)
        val twos = factored(divisor, TWO, Long.MAX_VALUE)
        val fives = factored(twos.second, FIVE, Long.MAX_VALUE)
        val hasTwos = hasFactors(value = value, prime = TWO, needed = twos.first - shift, meter = meter)
        val hasFives = hasTwos && hasFactors(value = value, prime = FIVE, needed = fives.first - shift, meter = meter)
        return hasFives && divides(value, fives.second, meter)
    }

    private fun hasFactors(value: IntArray, prime: Int, needed: Long, meter: Meter): Boolean {
        if (needed <= 0L) return true
        meter.charge(value.size.toLong() * value.size)
        return factored(value, prime, needed).first >= needed
    }

    private fun factored(digits: IntArray, prime: Int, maximum: Long): Pair<Long, IntArray> {
        var count = 0L
        var rest = digits
        var quotient = divided(rest, prime)
        while (quotient != null && count < maximum) {
            count++
            rest = quotient
            quotient = divided(rest, prime)
        }
        return count to rest
    }

    private fun divided(digits: IntArray, divisor: Int): IntArray? {
        val quotient = IntArray(digits.size)
        var remainder = 0
        digits.forEachIndexed { index, digit ->
            val current = remainder * RADIX + digit
            quotient[index] = current / divisor
            remainder = current % divisor
        }
        return stripped(quotient).takeIf { remainder == 0 }
    }

    private fun divides(value: IntArray, modulus: IntArray, meter: Meter): Boolean {
        meter.charge(value.size.toLong() * modulus.size)
        if (modulus.size <= LONG_DIGITS) return dividesByLong(value, modulus)
        return dividesByDigits(value, modulus)
    }

    private fun dividesByLong(value: IntArray, modulus: IntArray): Boolean {
        val divisor = modulus.fold(0L) { total, digit -> total * RADIX + digit }
        val remainder = value.fold(0L) { total, digit -> (total * RADIX + digit) % divisor }
        return remainder == 0L
    }

    private fun dividesByDigits(value: IntArray, modulus: IntArray): Boolean {
        val divisor = modulus.reversedArray()
        val remainder = IntArray(divisor.size + 1)
        value.forEach { digit ->
            remainder.copyInto(destination = remainder, destinationOffset = 1, startIndex = 0, endIndex = divisor.size)
            remainder[0] = digit
            while (compare(remainder, divisor) >= 0) subtract(remainder, divisor)
        }
        return remainder.all { it == 0 }
    }

    private fun compare(remainder: IntArray, divisor: IntArray): Int {
        var index = remainder.size - 1
        var order = 0
        while (index >= 0 && order == 0) {
            val other = divisor.getOrElse(index) { 0 }
            order = remainder[index].compareTo(other)
            index--
        }
        return order
    }

    private fun subtract(remainder: IntArray, divisor: IntArray) {
        var borrow = 0
        remainder.indices.forEach { index ->
            val difference = remainder[index] - divisor.getOrElse(index) { 0 } - borrow
            borrow = 1.takeIf { difference < 0 } ?: 0
            remainder[index] = difference + RADIX * borrow
        }
    }

    private fun digitsOf(text: String): IntArray = IntArray(text.length) { index -> text[index] - '0' }

    private fun stripped(digits: IntArray): IntArray {
        val first = digits.indexOfFirst { it != 0 }
        return digits.copyOfRange(fromIndex = first.coerceAtLeast(0), toIndex = digits.size)
    }
}
