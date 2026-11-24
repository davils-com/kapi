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

package com.davils.kapi.codegen.core.emit

private const val ESCAPE: String = "\\"

private const val RAW_QUOTES: String = "\"\"\""

private const val HEX: Int = 16

private const val WIDTH: Int = 4

private val transitions: Map<Lexeme, List<Pair<String, Lexeme>>> = mapOf(
    Lexeme.Code to listOf(
        "/*" to Lexeme.Comment,
        RAW_QUOTES to Lexeme.Raw,
        "\"" to Lexeme.Text,
        "'" to Lexeme.Character,
    ),
    Lexeme.Text to listOf(ESCAPE to Lexeme.Text, "\"" to Lexeme.Code),
    Lexeme.Character to listOf(ESCAPE to Lexeme.Character, "'" to Lexeme.Code),
    Lexeme.Comment to listOf("*/" to Lexeme.Code),
    Lexeme.Raw to listOf(RAW_QUOTES to Lexeme.Code),
)

internal class VisibleText(private val source: String, private val isHidden: (Char) -> Boolean) {
    private val written: StringBuilder = StringBuilder(source.length)

    private var lexeme: Lexeme = Lexeme.Code

    private var index: Int = 0

    fun written(): String {
        while (index < source.length) index += step()
        return written.toString()
    }

    private fun step(): Int {
        val options = transitions.getValue(lexeme)
        val found = options.firstOrNull { (token, _) -> source.startsWith(token, index) }
        val (token, next) = found ?: return character(source[index])
        val width = (token.length + 1).takeIf { token == ESCAPE } ?: token.length
        val end = minOf(a = index + width, b = source.length)
        written.append(source, index, end)
        lexeme = next
        return end - index
    }

    private fun character(char: Char): Int {
        val digits = char.code.toString(HEX).uppercase()
        val escape = "\\u" + digits.padStart(WIDTH, '0')
        val template = "\${'$escape'}".takeIf { lexeme == Lexeme.Raw } ?: escape
        written.append(template.takeIf { isHidden(char) } ?: char.toString())
        return 1
    }
}
