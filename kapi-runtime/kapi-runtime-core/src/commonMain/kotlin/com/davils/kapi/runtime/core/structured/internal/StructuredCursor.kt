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

package com.davils.kapi.runtime.core.structured.internal

import com.davils.kapi.runtime.core.internal.FieldGrammar

private const val SPACE: Char = ' '

internal class StructuredCursor(private val text: String) {
    var position: Int = 0
        private set

    var isFailed: Boolean = false
        private set

    val isAtEnd: Boolean get() = position >= text.length

    fun peek(): Char? = text.getOrNull(position)

    fun next(): Char? {
        val char = peek()
        position += 1
        return char
    }

    fun skip(count: Int) {
        position += count
    }

    fun ahead(count: Int): String = text.substring(
        startIndex = minOf(a = position, b = text.length),
        endIndex = minOf(a = position + count, b = text.length),
    )

    fun consume(char: Char): Boolean {
        if (peek() != char) return false
        position += 1
        return true
    }

    fun span(predicate: (Char) -> Boolean): String {
        val start = position
        while (position < text.length && predicate(text[position])) position += 1
        return text.substring(startIndex = start, endIndex = position)
    }

    fun skipSpaces() {
        while (peek() == SPACE) position += 1
    }

    fun skipWhitespace() {
        while (peek()?.let(FieldGrammar::isWhitespace) == true) position += 1
    }

    fun fail() {
        isFailed = true
        position = text.length
    }
}
