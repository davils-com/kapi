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

import com.squareup.kotlinpoet.FileSpec

private const val INDENT: String = "    "

private const val LINE: Char = '\n'

private const val HEX: Int = 16

private const val WIDTH: Int = 4

private val invisible: Set<CharCategory> = setOf(
    CharCategory.CONTROL,
    CharCategory.FORMAT,
    CharCategory.LINE_SEPARATOR,
    CharCategory.PARAGRAPH_SEPARATOR,
    CharCategory.SURROGATE,
    CharCategory.PRIVATE_USE,
    CharCategory.UNASSIGNED,
)

internal object Sources {
    fun of(file: FileSpec.Builder): String {
        file.indent(INDENT)
        return visible(file.build().toString())
    }

    fun visible(text: String): String {
        val isClean = text.none(::isHidden)
        if (isClean) return text
        val written = StringBuilder(text.length)
        text.forEach { char ->
            val digits = char.code.toString(HEX).uppercase()
            val escape = "\\u" + digits.padStart(WIDTH, '0')
            written.append(escape.takeIf { isHidden(char) } ?: char.toString())
        }
        return written.toString()
    }

    private fun isHidden(char: Char): Boolean = char != LINE && char.category in invisible
}
