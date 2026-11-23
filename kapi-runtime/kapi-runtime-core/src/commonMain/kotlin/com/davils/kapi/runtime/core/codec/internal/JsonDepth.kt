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

package com.davils.kapi.runtime.core.codec.internal

private const val QUOTE: Char = '"'

private const val BACKSLASH: Char = '\\'

private val deltas: Map<Char, Int> = mapOf('[' to 1, '{' to 1, ']' to -1, '}' to -1)

internal object JsonDepth {
    fun isWithin(text: String, maxDepth: Int): Boolean {
        var depth = 0
        var deepest = 0
        var isInString = false
        var isEscaped = false
        text.forEach { char ->
            val isAfterEscape = isEscaped
            isEscaped = isInString && !isAfterEscape && char == BACKSLASH
            isInString = isInString != (char == QUOTE && !isAfterEscape)
            val isStructural = !isInString && char != QUOTE
            depth += delta(char).takeIf { isStructural } ?: 0
            deepest = maxOf(a = deepest, b = depth)
        }
        return deepest <= maxDepth
    }

    private fun delta(char: Char): Int = deltas[char] ?: 0
}
