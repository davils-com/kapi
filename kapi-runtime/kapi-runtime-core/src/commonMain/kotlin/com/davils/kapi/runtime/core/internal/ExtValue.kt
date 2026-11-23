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

private const val ATTR_SYMBOLS: String = "!#$&+-.^_`|~"

private const val UTF_8: String = "utf-8"

private const val LATIN_1: String = "iso-8859-1"

private const val QUOTE: Char = '\''

private const val PARTS: Int = 3

internal object ExtValue {
    fun isAttrChar(char: Char): Boolean = FieldGrammar.isAlphaNumeric(char) || char in ATTR_SYMBOLS

    fun encode(text: String): String = "UTF-8''" + PercentCoding.encode(text, isKept = ::isAttrChar)

    fun decode(text: String): String? {
        val parts = text.split(QUOTE, limit = PARTS)
        val charset = parts.first().lowercase()
        val value = parts.getOrNull(2)
        val isValid = parts.size == PARTS && value != null && value.all { char -> isAttrChar(char) || char == '%' }
        val bytes = value?.takeIf { isValid }?.let(PercentCoding::decodeBytes) ?: return null
        return Utf8.decodeOrNull(bytes).takeIf { charset == UTF_8 } ?: Utf8.latin1(bytes).takeIf { charset == LATIN_1 }
    }
}
