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

package com.davils.kapi.spec.schema.format

import com.davils.kapi.runtime.core.pattern.LAST_CODE_POINT
import com.davils.kore.core.annotation.DavilsInternal

private const val BASE: Int = 36
private const val MINIMUM_THRESHOLD: Int = 1
private const val MAXIMUM_THRESHOLD: Int = 26
private const val SKEW: Int = 38
private const val DAMP: Int = 700
private const val INITIAL_BIAS: Int = 72
private const val INITIAL_CODE_POINT: Int = 0x80
private const val DELIMITER: Char = '-'
private const val DIGIT_LETTERS: Int = 26
private const val LIMIT: Long = Int.MAX_VALUE.toLong()
private const val MAXIMUM_OUTPUT: Int = 256

internal object Punycode {
    fun decode(input: String): IntArray? {
        val delimiter = input.lastIndexOf(DELIMITER)
        val basic = input.substring(startIndex = 0, endIndex = delimiter.coerceAtLeast(0))
        val isBasicAscii = basic.all { it.code < INITIAL_CODE_POINT }
        if (!isBasicAscii) return null
        val output = basic.map { it.code }.toMutableList()
        val start = (delimiter + 1).takeIf { delimiter > 0 } ?: 0
        return insertAll(input, start = start, output = output)
    }

    fun encode(points: IntArray): String {
        val output = StringBuilder()
        points.filter { it < INITIAL_CODE_POINT }.forEach { output.append(it.toChar()) }
        val basicCount = output.length
        if (basicCount > 0) output.append(DELIMITER)
        var handled = basicCount
        var next = INITIAL_CODE_POINT
        var delta = 0L
        var bias = INITIAL_BIAS
        while (handled < points.size) {
            val smallest = points.filter { it >= next }.min()
            delta += (smallest - next).toLong() * (handled + 1)
            next = smallest
            points.forEach { point ->
                if (point < next) delta++
                if (point == next) {
                    appendNumber(output, delta, bias)
                    bias = adapt(delta, handled + 1, isFirst = handled == basicCount)
                    delta = 0
                    handled++
                }
            }
            delta++
            next++
        }
        return output.toString()
    }

    private fun insertAll(input: String, start: Int, output: MutableList<Int>): IntArray? {
        var index = start
        var position = 0L
        var codePoint = INITIAL_CODE_POINT.toLong()
        var bias = INITIAL_BIAS
        var isValid = true
        while (isValid && index < input.length) {
            val previous = position
            val read = readNumber(input, index, bias)
            isValid = read != null && output.size < MAXIMUM_OUTPUT
            index = read?.second ?: input.length
            position += read?.first ?: 0
            val count = output.size + 1
            bias = adapt(position - previous, count, isFirst = previous == 0L)
            codePoint += position / count
            position %= count
            isValid = isValid && codePoint <= LAST_CODE_POINT && !isSurrogate(codePoint.toInt())
            if (isValid) output.add(index = position.toInt(), element = codePoint.toInt())
            position++
        }
        return output.toIntArray().takeIf { isValid }
    }

    private fun readNumber(input: String, start: Int, bias: Int): Pair<Long, Int>? {
        var index = start
        var value = 0L
        var weight = 1L
        var step = BASE
        var isDone = false
        var isValid = true
        while (isValid && !isDone) {
            val digit = digitOf(input.getOrNull(index))
            index++
            isValid = digit >= 0 && value + digit * weight <= LIMIT
            value += digit.coerceAtLeast(0) * weight
            val threshold = threshold(step, bias)
            isDone = digit < threshold
            weight *= (BASE - threshold).takeUnless { isDone } ?: 1
            isValid = isValid && weight <= LIMIT
            step += BASE
        }
        return (value to index).takeIf { isValid }
    }

    private fun appendNumber(output: StringBuilder, number: Long, bias: Int) {
        var rest = number
        var step = BASE
        var isDone = false
        while (!isDone) {
            val threshold = threshold(step, bias)
            isDone = rest < threshold
            val digit = rest.takeIf { isDone } ?: (threshold + (rest - threshold) % (BASE - threshold))
            output.append(letterOf(digit.toInt()))
            rest = (rest - threshold) / (BASE - threshold)
            step += BASE
        }
    }

    private fun threshold(step: Int, bias: Int): Int =
        (step - bias).coerceIn(minimumValue = MINIMUM_THRESHOLD, maximumValue = MAXIMUM_THRESHOLD)

    private fun adapt(delta: Long, count: Int, isFirst: Boolean): Int {
        var scaled = delta / (DAMP.takeIf { isFirst } ?: 2)
        scaled += scaled / count
        var shifts = 0
        while (scaled > (BASE - MINIMUM_THRESHOLD) * MAXIMUM_THRESHOLD / 2) {
            scaled /= BASE - MINIMUM_THRESHOLD
            shifts += BASE
        }
        return (shifts + (BASE - MINIMUM_THRESHOLD + 1) * scaled / (scaled + SKEW)).toInt()
    }

    private fun digitOf(char: Char?): Int {
        val lower = char?.lowercaseChar() ?: return -1
        val letter = (lower - 'a').takeIf { lower in 'a'..'z' }
        val digit = (lower - '0' + DIGIT_LETTERS).takeIf { lower in '0'..'9' }
        return letter ?: digit ?: -1
    }

    private fun letterOf(digit: Int): Char {
        val letter = ('a' + digit).takeIf { digit < DIGIT_LETTERS }
        return letter ?: ('0' + (digit - DIGIT_LETTERS))
    }

    private fun isSurrogate(point: Int): Boolean = point in Char.MIN_SURROGATE.code..Char.MAX_SURROGATE.code
}
