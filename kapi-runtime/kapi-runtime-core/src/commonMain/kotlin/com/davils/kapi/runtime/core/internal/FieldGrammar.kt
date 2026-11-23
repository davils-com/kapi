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

private const val TOKEN_SYMBOLS: String = "!#$%&'*+-.^_`|~"

private const val SPACE: Char = ' '

private const val TAB: Char = '\t'

private const val QUOTE: Char = '"'

private const val BACKSLASH: Char = '\\'

private const val DELETE: Char = '\u007F'

private const val LAST_OBSOLETE: Char = 'ÿ'

private const val FIRST_OBSOLETE: Char = '\u0080'

internal object FieldGrammar {
    fun isTokenChar(char: Char): Boolean = isAlphaNumeric(char) || char in TOKEN_SYMBOLS

    fun isToken(text: String): Boolean = text.isNotEmpty() && text.all(::isTokenChar)

    fun isAlphaNumeric(char: Char): Boolean = isAlpha(char) || isDigit(char)

    fun isAlpha(char: Char): Boolean = char in 'a'..'z' || char in 'A'..'Z'

    fun isDigit(char: Char): Boolean = char in '0'..'9'

    fun isWhitespace(char: Char): Boolean = char == SPACE || char == TAB

    fun isVisible(char: Char): Boolean = char in '!'..'~'

    fun isVisibleOrSpace(char: Char): Boolean = char == SPACE || isVisible(char)

    fun isQuotedText(char: Char): Boolean {
        val isPrintable = isWhitespace(char) || isVisible(char) || isObsolete(char)
        return isPrintable && char != QUOTE && char != BACKSLASH
    }

    fun isQuotable(char: Char): Boolean = isWhitespace(char) || isVisible(char) || isObsolete(char)

    fun isObsolete(char: Char): Boolean = char in FIRST_OBSOLETE..LAST_OBSOLETE

    fun isBeyondLatin(char: Char): Boolean = char > LAST_OBSOLETE

    fun trimmed(text: String): String = text.trim(::isWhitespace)

    fun isSafeValue(text: String): Boolean = text.all { char -> isWhitespace(char) || isVisible(char) }

    fun isControl(char: Char): Boolean = (char < SPACE && char != TAB) || char == DELETE

    fun quote(text: String): String {
        require(text.all { char -> isWhitespace(char) || isVisible(char) }) {
            "A quoted string may hold only visible ASCII characters, spaces and tabs"
        }
        val out = StringBuilder(text.length + 2)
        out.append(QUOTE)
        text.forEach { char ->
            if (char == QUOTE || char == BACKSLASH) out.append(BACKSLASH)
            out.append(char)
        }
        out.append(QUOTE)
        return out.toString()
    }

    fun tokenOrQuoted(text: String): String {
        if (isToken(text)) return text
        return quote(text)
    }
}
