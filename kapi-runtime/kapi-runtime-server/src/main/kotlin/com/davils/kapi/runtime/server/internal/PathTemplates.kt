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

package com.davils.kapi.runtime.server.internal

import com.davils.kapi.runtime.core.header.FieldSyntax
import com.davils.kore.core.annotation.DavilsInternal

private const val NAME_SYMBOLS: String = "_-."

private const val MOST_TEMPLATE_LENGTH: Int = 2_048

internal object PathTemplates {
    fun isMethod(text: String): Boolean = FieldSyntax.isToken(text)

    fun isTemplate(text: String): Boolean {
        val isShaped = text.startsWith('/') && text.length <= MOST_TEMPLATE_LENGTH && '?' !in text && '#' !in text
        return isShaped && text.split('/').all(::isSegment)
    }

    fun names(template: String): List<String> = template.split('/').flatMap(::namesOf)

    private fun isSegment(segment: String): Boolean {
        val opened = segment.count { char -> char == '{' }
        val closed = segment.count { char -> char == '}' }
        val isPlain = opened == 0 && closed == 0
        val isTemplated = opened == closed && namesOf(segment).size == opened && isBalanced(segment)
        return segment.none(::isSpecial) && (isPlain || isTemplated)
    }

    private fun isBalanced(segment: String): Boolean {
        var depth = 0
        var isValid = true
        segment.forEach { char ->
            if (char == '{') depth += 1
            if (char == '}') depth -= 1
            isValid = isValid && depth in 0..1
        }
        return isValid && depth == 0
    }

    private fun namesOf(segment: String): List<String> {
        val parts = segment.split('{').drop(1)
        val names = parts.map { part -> part.substringBefore('}', missingDelimiterValue = "") }
        return names.filter { name -> name.isNotEmpty() && name.all(::isNameChar) }
    }

    private fun isSpecial(char: Char): Boolean =
        char == '*' || char == '?' || char.isWhitespace() || char.code < ' '.code

    private fun isNameChar(char: Char): Boolean = isAsciiLetterOrDigit(char) || char in NAME_SYMBOLS

    private fun isAsciiLetterOrDigit(char: Char): Boolean = char in 'a'..'z' || char in 'A'..'Z' || char in '0'..'9'
}
