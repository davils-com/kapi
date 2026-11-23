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

import com.davils.kapi.spec.schema.pattern.charCount
import com.davils.kapi.spec.schema.pattern.codePointAt

private const val DECIMAL_RADIX: Int = 10

internal fun isDigit(char: Char): Boolean = char in '0'..'9'

internal fun isHexDigit(char: Char): Boolean = isDigit(char) || char in 'a'..'f' || char in 'A'..'F'

internal fun isLetter(char: Char): Boolean = char in 'a'..'z' || char in 'A'..'Z'

internal fun isAllDigits(text: String): Boolean = text.isNotEmpty() && text.all(::isDigit)

internal fun number(text: String, start: Int, count: Int): Int? {
    val isInside = start >= 0 && start + count <= text.length
    if (!isInside) return null
    var value = 0
    var isNumber = true
    repeat(count) { offset ->
        val char = text[start + offset]
        isNumber = isNumber && isDigit(char)
        value = value * DECIMAL_RADIX + (char - '0')
    }
    return value.takeIf { isNumber }
}

internal fun codePoints(text: String): IntArray {
    val points = mutableListOf<Int>()
    var index = 0
    while (index < text.length) {
        val point = codePointAt(text, index)
        points += point
        index += charCount(point)
    }
    return points.toIntArray()
}
