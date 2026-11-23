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

package com.davils.kapi.runtime.core.internal

private const val QUOTE: Char = '"'

private const val BACKSLASH: Char = '\\'

internal class FieldScanner(private val text: String) {
    var position: Int = 0
        private set

    val isAtEnd: Boolean get() = position >= text.length

    fun peek(): Char? = text.getOrNull(position)

    fun reset(mark: Int) {
        position = mark
    }

    fun skipWhitespace() {
        while (!isAtEnd && FieldGrammar.isWhitespace(text[position])) position += 1
    }

    fun consume(char: Char): Boolean {
        if (peek() != char) return false
        position += 1
        return true
    }

    fun consume(literal: String): Boolean {
        if (!text.startsWith(literal, position)) return false
        position += literal.length
        return true
    }

    fun token(): String? = span(FieldGrammar::isTokenChar)

    fun span(predicate: (Char) -> Boolean): String? {
        val start = position
        while (!isAtEnd && predicate(text[position])) position += 1
        return text.substring(startIndex = start, endIndex = position).takeIf(String::isNotEmpty)
    }

    fun tokenOrQuoted(isUnicode: Boolean = false): String? {
        if (!consume(QUOTE)) return token()
        return quotedString(isUnicode)
    }

    private fun quotedString(isUnicode: Boolean): String? {
        val out = StringBuilder()
        var isOpen = true
        var isValid = true
        while (isOpen && isValid) {
            val char = peek()
            position += 1
            isOpen = char != QUOTE
            isValid = char != null && appended(out, char, isUnicode)
        }
        return out.toString().takeIf { isValid }
    }

    private fun appended(out: StringBuilder, char: Char, isUnicode: Boolean): Boolean {
        if (char == QUOTE) return true
        val shown = escaped(char)?.takeIf { found -> isUnicode || !FieldGrammar.isBeyondLatin(found) }
        shown?.let(out::append)
        return shown != null
    }

    private fun isQuotedChar(char: Char): Boolean = FieldGrammar.isQuotable(char) || FieldGrammar.isBeyondLatin(char)

    private fun escaped(char: Char): Char? {
        if (char != BACKSLASH) return char.takeIf(::isQuotedChar)
        val next = peek()?.takeIf(FieldGrammar::isQuotable)
        if (next != null) position += 1
        return next
    }
}
