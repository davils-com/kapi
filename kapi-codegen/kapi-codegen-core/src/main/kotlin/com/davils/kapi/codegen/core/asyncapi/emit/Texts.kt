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

package com.davils.kapi.codegen.core.asyncapi.emit

import com.squareup.kotlinpoet.CodeBlock

private const val HEX_DIGITS: Int = 4

private const val HEX_RADIX: Int = 16

private const val LAST_CONTROL: Int = 0x1F

private const val FIRST_WIDE_CONTROL: Int = 0x7F

private const val LAST_WIDE_CONTROL: Int = 0x9F

private const val MOST_KDOC_LENGTH: Int = 2_000

private val invisible: Set<Int> = setOf(
    0x00AD,
    0x061C,
    0x180E,
    0x200B,
    0x200C,
    0x200D,
    0x200E,
    0x200F,
    0x2028,
    0x2029,
    0x202A,
    0x202B,
    0x202C,
    0x202D,
    0x202E,
    0x2060,
    0x2066,
    0x2067,
    0x2068,
    0x2069,
    0xFEFF,
    0xFFF9,
    0xFFFA,
    0xFFFB,
)

private val formatting: Map<Char, String> = mapOf(
    '·' to "&#183;",
    '♢' to "&#9826;",
    '@' to "&#64;",
    '%' to "&#37;",
)

internal object Texts {
    fun literal(text: String): CodeBlock {
        val runs = runsOf(text)
        val code = CodeBlock.builder()
        if (runs.isEmpty()) code.add("%S", "")
        runs.forEachIndexed { index, run ->
            if (index > 0) code.add("·+·")
            if (run.isSafe) code.add("%S", run.text)
            if (!run.isSafe) code.add("%L", escaped(run.text))
        }
        return code.build()
    }

    fun kdoc(text: String): String {
        val lines = text.take(MOST_KDOC_LENGTH).lines()
        val visible = lines.joinToString(separator = "\n").map { char -> shownInKdoc(char) }
        val closed = visible.joinToString(separator = "").replace(oldValue = "*/", newValue = "*&#47;")
        return closed.replace(oldValue = "/*", newValue = "&#47;*").trim()
    }

    private fun shownInKdoc(char: Char): String {
        val code = char.code
        val isNewline = char == '\n'
        val isHidden = !isNewline && !char.isSurrogate() && isUnsafe(code)
        return formatting[char] ?: ("\\u" + hex(code)).takeIf { isHidden } ?: char.toString()
    }

    private fun runsOf(text: String): List<Run> {
        val runs = mutableListOf<Run>()
        text.forEach { char ->
            val isSafe = !isUnsafe(char.code)
            val last = runs.lastOrNull()?.takeIf { run -> run.isSafe == isSafe }
            if (last == null) runs += Run(StringBuilder().append(char), isSafe)
            last?.run { builder.append(char) }
        }
        return runs
    }

    private fun escaped(text: String): String =
        text.map { char -> "\\u" + hex(char.code) }.joinToString(separator = "", prefix = "\"", postfix = "\"")

    private fun isUnsafe(code: Int): Boolean = code <= LAST_CONTROL ||
        code in FIRST_WIDE_CONTROL..LAST_WIDE_CONTROL ||
        code in invisible ||
        code in Char.MIN_SURROGATE.code..Char.MAX_SURROGATE.code

    private fun hex(code: Int): String = code.toString(HEX_RADIX).padStart(HEX_DIGITS, '0')

    private class Run(val builder: StringBuilder, val isSafe: Boolean) {
        val text: String get() = builder.toString()
    }
}
