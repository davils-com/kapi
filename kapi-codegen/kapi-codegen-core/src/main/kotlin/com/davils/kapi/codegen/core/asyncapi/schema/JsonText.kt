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

package com.davils.kapi.codegen.core.asyncapi.schema

import com.davils.kapi.spec.core.value.Value

private const val HEX_DIGITS: Int = 4

private const val HEX_RADIX: Int = 16

private const val LAST_CONTROL: Int = 0x1F

private const val FIRST_WIDE_CONTROL: Int = 0x7F

private const val LAST_WIDE_CONTROL: Int = 0x9F

private val invisible: Set<Int> = setOf(
    0x061C,
    0x200E,
    0x200F,
    0x2028,
    0x2029,
    0x202A,
    0x202B,
    0x202C,
    0x202D,
    0x202E,
    0x2066,
    0x2067,
    0x2068,
    0x2069,
    0xFEFF,
    0xFFFE,
    0xFFFF,
)

internal object JsonText {
    fun document(title: String, schemas: List<Hoisted>): String {
        val components = Value.Mapping(schemas.associate { hoisted -> hoisted.key to hoisted.translated })
        val info = Value.Mapping(mapOf("title" to Value.Text(title), "version" to Value.Text("1")))
        val root = Value.Mapping(
            mapOf(
                "openapi" to Value.Text("3.1.0"),
                "info" to info,
                "components" to Value.Mapping(mapOf("schemas" to components)),
            ),
        )
        return buildString { write(root) }
    }

    private fun StringBuilder.write(value: Value) {
        when (value) {
            is Value.Null -> append("null")
            is Value.Flag -> append(value.isTrue)
            is Value.Number -> append(value.value.literal)
            is Value.Text -> quoted(value.value)
            is Value.Sequence -> sequence(value)
            is Value.Mapping -> mapping(value)
        }
    }

    private fun StringBuilder.sequence(value: Value.Sequence) {
        append('[')
        value.items.forEachIndexed { index, item ->
            if (index > 0) append(',')
            write(item)
        }
        append(']')
    }

    private fun StringBuilder.mapping(value: Value.Mapping) {
        append('{')
        value.entries.entries.forEachIndexed { index, (key, item) ->
            if (index > 0) append(',')
            quoted(key)
            append(':')
            write(item)
        }
        append('}')
    }

    private fun StringBuilder.quoted(text: String) {
        append('"')
        text.forEach { char -> escaped(char) }
        append('"')
    }

    private fun StringBuilder.escaped(char: Char) {
        val code = char.code
        val isEscaped = code <= LAST_CONTROL || code in FIRST_WIDE_CONTROL..LAST_WIDE_CONTROL || code in invisible
        val isSpecial = char == '"' || char == '\\'
        if (isSpecial) append('\\')
        if (isEscaped) append("\\u" + code.toString(HEX_RADIX).padStart(HEX_DIGITS, '0'))
        if (!isEscaped) append(char)
    }
}
