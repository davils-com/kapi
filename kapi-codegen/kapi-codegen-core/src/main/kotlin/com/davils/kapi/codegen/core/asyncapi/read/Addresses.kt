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

package com.davils.kapi.codegen.core.asyncapi.read

import com.davils.kapi.codegen.core.asyncapi.plan.PathPart

private const val LITERAL_SYMBOLS: String = "/-._~!$&'()+,;=:@%"

private const val NAME_SYMBOLS: String = "_-."

private const val MOST_ADDRESS_LENGTH: Int = 2_048

private const val ASCII_LETTERS_AND_DIGITS: String = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

internal object Addresses {
    fun parts(address: String, parameter: (String) -> PathPart.Parameter): List<PathPart>? {
        val rooted = address.takeIf { text -> text.startsWith('/') } ?: "/$address"
        val pieces = rooted.split('{')
        val literals = mutableListOf<PathPart>()
        val head = pieces.first()
        val isShaped = rooted.length <= MOST_ADDRESS_LENGTH && isLiteral(head)
        literals += PathPart.Literal(head)
        val rest = pieces.drop(1).map { piece -> templated(piece, parameter) }
        val isValid = isShaped && rest.none { parts -> parts == null }
        rest.forEach { parts -> literals += parts.orEmpty() }
        return literals.takeIf { isValid }
    }

    private fun templated(piece: String, parameter: (String) -> PathPart.Parameter): List<PathPart>? {
        val name = piece.substringBefore('}', missingDelimiterValue = "")
        val tail = piece.substringAfter('}', missingDelimiterValue = "")
        val isNamed = name.isNotEmpty() && name.all(::isNameChar) && '}' in piece
        val isValid = isNamed && isLiteral(tail)
        val parts = listOf(parameter(name)) + listOf(PathPart.Literal(tail)).filter { tail.isNotEmpty() }
        return parts.takeIf { isValid }
    }

    private fun isLiteral(text: String): Boolean =
        text.all { char -> isAsciiLetterOrDigit(char) || char in LITERAL_SYMBOLS }

    private fun isNameChar(char: Char): Boolean = isAsciiLetterOrDigit(char) || char in NAME_SYMBOLS

    private fun isAsciiLetterOrDigit(char: Char): Boolean = char in ASCII_LETTERS_AND_DIGITS
}
