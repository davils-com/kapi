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

internal object PointerFormats {
    fun jsonPointer(value: String): String? {
        val isValid = value.isEmpty() || (value.startsWith('/') && hasValidEscapes(value))
        return "a JSON Pointer is empty or starts with / and escapes ~ as ~0 or ~1".takeUnless { isValid }
    }

    fun relativeJsonPointer(value: String): String? {
        val origin = originLength(value)
        val rest = value.substring(origin.coerceAtLeast(0))
        val isValid = origin > 0 && (rest == "#" || jsonPointer(rest) == null)
        return "a relative JSON Pointer is a number, an optional index change and # or a JSON Pointer".takeUnless {
            isValid
        }
    }

    private fun hasValidEscapes(value: String): Boolean =
        value.indices.all { index -> value[index] != '~' || value.getOrNull(index + 1) in setOf('0', '1') }

    private fun originLength(value: String): Int {
        val number = digitsLength(value, 0)
        val isNumber = number == 1 || (number > 1 && value[0] != '0')
        if (!isNumber) return -1
        val sign = value.getOrNull(number)
        val hasManipulation = sign == '+' || sign == '-'
        val manipulation = digitsLength(value, number + 1).takeIf { hasManipulation } ?: 0
        val isManipulationValid = !hasManipulation || (manipulation > 0 && value[number + 1] != '0')
        val manipulationLength = (1 + manipulation).takeIf { hasManipulation } ?: 0
        val length = number + manipulationLength
        return length.takeIf { isManipulationValid } ?: -1
    }

    private fun digitsLength(value: String, start: Int): Int {
        var end = start
        while (end < value.length && isDigit(value[end])) end++
        return end - start
    }
}
