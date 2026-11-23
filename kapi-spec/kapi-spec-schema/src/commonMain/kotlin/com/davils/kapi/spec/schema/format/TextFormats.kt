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

package com.davils.kapi.spec.schema.format

import com.davils.kapi.runtime.core.pattern.Pattern
import com.davils.kore.core.annotation.DavilsInternal

private const val UUID_LENGTH: Int = 36
private const val UUID_HYPHENS: String = "8 13 18 23"
private const val BASE64_QUANTUM: Int = 4
private const val MAXIMUM_PADDING: Int = 2

internal object TextFormats {
    private val hyphens: List<Int> = UUID_HYPHENS.split(' ').map(String::toInt)

    fun uuid(value: String): String? {
        val isEveryCharacterValid = value.indices.all { index ->
            (index in hyphens && value[index] == '-') || (index !in hyphens && isHexDigit(value[index]))
        }
        val isShaped = value.length == UUID_LENGTH && isEveryCharacterValid
        return "a UUID has the form 8-4-4-4-12 in hexadecimal digits".takeUnless { isShaped }
    }

    fun base64(value: String): String? {
        val content = value.trimEnd('=')
        val padding = value.length - content.length
        val isAlphabet = content.all { isLetter(it) || isDigit(it) || it == '+' || it == '/' }
        val isShaped = value.length % BASE64_QUANTUM == 0 && padding <= MAXIMUM_PADDING && isAlphabet
        return "base64 has groups of four characters of A-Z, a-z, 0-9, + and /, padded with =".takeUnless { isShaped }
    }

    fun regex(value: String): String? {
        val issue = Pattern.syntaxIssue(value)
        return issue?.let { "the regular expression is invalid $it" }
    }
}
