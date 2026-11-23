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

package com.davils.kapi.codegen.core.naming

private const val DOT: Char = '.'

private const val UNDERSCORE: Char = '_'

private val identifier: Regex = Regex("[A-Za-z_][A-Za-z0-9_]*")

private val typeName: Regex = Regex("[A-Z][A-Za-z0-9]*")

private val packageSegment: Regex = Regex("[a-z][a-z0-9_]*")

private val separators: Regex = Regex("[^A-Za-z0-9]+")

private val camelBoundary: Regex = Regex("(?<=[a-z0-9])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])")

private val keywords: Set<String> = setOf(
    "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in", "interface", "is", "null",
    "object", "package", "return", "super", "this", "throw", "true", "try", "typealias", "typeof", "val", "var",
    "when", "while",
)

internal object Identifiers {
    fun words(raw: String): List<String> {
        val parts = raw.split(separators).flatMap { part -> part.split(camelBoundary) }
        return parts.filter(String::isNotEmpty)
    }

    fun type(raw: String): String {
        val joined = words(raw).joinToString("", transform = ::capitalized)
        return leading(name = joined.ifEmpty { "Value" }, prefix = "Type")
    }

    fun member(raw: String): String {
        val words = words(raw)
        val first = words.firstOrNull().orEmpty()
        val head = first.lowercase().ifEmpty { "value" }
        val tail = words.drop(1).joinToString("", transform = ::capitalized)
        return leading(name = head + tail, prefix = "n")
    }

    fun constant(raw: String): String {
        val joined = words(raw).joinToString(UNDERSCORE.toString(), transform = String::uppercase)
        return leading(name = joined.ifEmpty { "EMPTY" }, prefix = "N")
    }

    fun isIdentifier(raw: String): Boolean {
        val isNamed = identifier.matches(raw) && raw.any { char -> char != UNDERSCORE }
        return isNamed && raw !in keywords
    }

    fun isTypeName(raw: String): Boolean = typeName.matches(raw)

    fun isQualified(raw: String): Boolean = raw.split(DOT).all(::isIdentifier)

    fun isPackage(raw: String): Boolean = raw.split(DOT).all { segment ->
        packageSegment.matches(segment) && segment !in keywords
    }

    private fun capitalized(word: String): String = word.take(1).uppercase() + word.drop(1).lowercase()

    private fun leading(name: String, prefix: String): String {
        if (name.first().isDigit()) return prefix + name
        return name
    }
}
