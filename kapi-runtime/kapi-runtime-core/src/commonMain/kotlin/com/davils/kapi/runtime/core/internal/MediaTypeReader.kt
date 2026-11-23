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

private const val SLASH: Char = '/'

private const val SEMICOLON: Char = ';'

private const val COMMA: Char = ','

private const val ASSIGN: Char = '='

private const val CHARSET: String = "charset"

internal object MediaTypeReader {
    const val MAX_LENGTH: Int = 4_096

    const val MAX_PARAMETERS: Int = 64

    fun parse(text: String): MediaTypeParts? {
        if (text.length > MAX_LENGTH) return null
        val scanner = FieldScanner(FieldGrammar.trimmed(text))
        return read(scanner, MAX_PARAMETERS)?.takeIf { scanner.isAtEnd }
    }

    fun read(scanner: FieldScanner, maxParameters: Int): MediaTypeParts? {
        val type = scanner.token()
        val hasSlash = type != null && scanner.consume(SLASH)
        val subtype = scanner.token()?.takeIf { hasSlash }
        val parameters = subtype?.let { parameters(scanner, maxParameters) }
        if (type == null || subtype == null || parameters == null) return null
        return MediaTypeParts(type = type.lowercase(), subtype = subtype.lowercase(), parameters = parameters)
    }

    fun parameters(scanner: FieldScanner, maxParameters: Int): List<Pair<String, String>>? {
        val parameters = ArrayList<Pair<String, String>>()
        var isValid = true
        while (isValid && startsParameter(scanner)) {
            val parameter = parameter(scanner)
            isValid = parameter != null && parameters.size < maxParameters
            parameter?.takeIf { pair -> pair.first.isNotEmpty() }?.let { pair ->
                isValid = isValid && parameters.none { (name, _) -> name == pair.first }
                parameters += pair
            }
        }
        return parameters.takeIf { isValid }
    }

    private fun startsParameter(scanner: FieldScanner): Boolean {
        val mark = scanner.position
        scanner.skipWhitespace()
        val isParameter = scanner.consume(SEMICOLON)
        if (!isParameter) scanner.reset(mark)
        scanner.skipWhitespace()
        return isParameter
    }

    private fun parameter(scanner: FieldScanner): Pair<String, String>? {
        val next = scanner.peek()
        val isEmpty = next == null || next == SEMICOLON || next == COMMA
        if (isEmpty) return "" to ""
        return assigned(scanner)
    }

    private fun assigned(scanner: FieldScanner): Pair<String, String>? {
        val name = scanner.token()?.lowercase()
        val hasValue = name != null && scanner.consume(ASSIGN)
        val value = scanner.tokenOrQuoted()?.takeIf { text ->
            hasValue &&
                FieldGrammar.isSafeValue(
                    text,
                )
        } ?: return null
        val normalized = value.lowercase().takeIf { name == CHARSET } ?: value
        return name.orEmpty() to normalized
    }
}
