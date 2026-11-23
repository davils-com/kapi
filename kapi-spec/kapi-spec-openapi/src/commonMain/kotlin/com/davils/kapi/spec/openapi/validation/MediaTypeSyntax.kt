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

package com.davils.kapi.spec.openapi.validation

internal object MediaTypeSyntax {
    private const val TOKEN_SYMBOLS: String = "!#\$&^_.+*-"

    private const val SPACES: String = " \t\n\u000B\u000C\r"

    private const val LINE_ENDS: String = "\n\r\u0085  "

    private const val SEPARATOR: Char = '/'

    private const val PARAMETERS: Char = ';'

    fun isValid(text: String): Boolean {
        val typeEnd = tokenEnd(text, 0)
        val subtypeStart = typeEnd + 1
        val hasType = typeEnd > 0 && text.getOrNull(typeEnd) == SEPARATOR
        val subtypeEnd = tokenEnd(text, subtypeStart)
        val hasSubtype = hasType && subtypeEnd > subtypeStart
        return hasSubtype && isParameters(text.substring(subtypeEnd))
    }

    private fun isParameters(rest: String): Boolean {
        val parameters = rest.trimStart { character -> character in SPACES }
        val isList = parameters.startsWith(PARAMETERS) && parameters.none { character -> character in LINE_ENDS }
        return rest.isEmpty() || isList
    }

    private fun tokenEnd(text: String, from: Int): Int {
        var end = from
        while (end < text.length && isToken(text[end])) end += 1
        return end
    }

    private fun isToken(character: Char): Boolean {
        val isLetter = character in 'a'..'z' || character in 'A'..'Z'
        return isLetter || character in '0'..'9' || character in TOKEN_SYMBOLS
    }
}
