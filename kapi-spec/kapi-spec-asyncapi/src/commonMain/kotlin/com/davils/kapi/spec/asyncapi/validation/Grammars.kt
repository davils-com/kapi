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

package com.davils.kapi.spec.asyncapi.validation

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kore.core.annotation.DavilsInternal

private const val MESSAGE: String = "${'$'}message."

private const val FRAGMENT: Char = '#'

private const val SLASH: Char = '/'

private const val QUOTE: Char = '"'

internal object Grammars {
    private val sources: Set<String> = setOf("header", "payload")

    private val tokenSymbols: Set<Char> = "!#${'$'}%&'*+-.^_`|~".toSet()

    private val componentSymbols: Set<Char> = setOf('.', '-', '_')

    private val identifierSymbols: Set<Char> = setOf('-', '_')

    fun isRuntimeExpression(text: String): Boolean {
        if (!text.startsWith(MESSAGE)) return false
        val rest = text.removePrefix(MESSAGE)
        val source = rest.substringBefore(FRAGMENT)
        val hasFragment = FRAGMENT in rest
        val fragment = rest.substringAfter(FRAGMENT, missingDelimiterValue = "")
        val isPointer = !hasFragment || JsonPointer.parseOrNull(fragment) != null
        return source in sources && isPointer
    }

    fun isMediaType(text: String): Boolean {
        val parts = text.split(';')
        val type = parts.first()
        val isType = type.count { character -> character == SLASH } == 1 && type.split(SLASH).all(::isToken)
        return isType && parts.drop(1).all(::isParameter)
    }

    fun isComponentKey(text: String): Boolean =
        text.isNotEmpty() && text.all { character -> isAlphanumeric(character) || character in componentSymbols }

    fun isIdentifier(text: String): Boolean =
        text.isNotEmpty() && text.all { character -> isAlphanumeric(character) || character in identifierSymbols }

    private fun isParameter(text: String): Boolean {
        val trimmed = text.trim()
        val name = trimmed.substringBefore('=')
        val value = trimmed.substringAfter('=', missingDelimiterValue = "")
        val isQuoted = value.length > 1 && value.first() == QUOTE && value.last() == QUOTE
        return '=' in trimmed && isToken(name) && (isQuoted || isToken(value))
    }

    private fun isToken(text: String): Boolean =
        text.isNotEmpty() && text.all { character -> isAlphanumeric(character) || character in tokenSymbols }

    private fun isAlphanumeric(character: Char): Boolean =
        character in 'a'..'z' || character in 'A'..'Z' || character in '0'..'9'
}
