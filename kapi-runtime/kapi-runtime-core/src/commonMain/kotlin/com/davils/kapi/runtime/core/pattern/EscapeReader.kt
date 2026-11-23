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

package com.davils.kapi.runtime.core.pattern

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedMap

private const val SYNTAX_CHARACTERS: String = "^$\\.*+?()[]{}|/"
private const val CONTROL_ESCAPES: String = "fnrtv"
private const val CONTROL_VALUES: String = "\u000C\n\r\t\u000B"
private const val SET_ESCAPES: String = "dDsSwW"
private const val CONTROL_LETTER_MODULUS: Int = 32
private const val HEX_ESCAPE_DIGITS: Int = 2
private const val MAXIMUM_REFERENCE_DIGITS: Int = 9
private const val PROPERTY_NAME_CHARACTERS: String = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz_"
private const val PROPERTY_VALUE_CHARACTERS: String = PROPERTY_NAME_CHARACTERS + "0123456789"
private const val REFERENCE_DETAIL: String = "a back reference cannot run in the linear engine"
private const val UNSUPPORTED_PROPERTY_DETAIL: String = "a property escape names a property without a table"

internal class EscapeReader(private val cursor: PatternCursor, private val scan: GroupScan) {
    private val properties: OrderedMap<String, PropertyLookup> = OrderedMap()

    fun reference(start: Int): PatternNode? = decimalReference(start) ?: namedReference(start)

    fun escapeSet(start: Int): CodePointSet? {
        val char = cursor.current
        val isSetEscape = char in SET_ESCAPES || char == 'p' || char == 'P'
        if (!isSetEscape) return null
        cursor.index++
        val set = classEscapeSet(char.lowercaseChar()) ?: property(start)
        return set.complement().takeIf { char.isUpperCase() } ?: set
    }

    fun characterEscape(start: Int): Int {
        val value = controlEscape() ?: controlLetter(start) ?: nullEscape(start) ?: hexEscape(start)
        val escaped = value ?: unicodeEscape(start) ?: identityEscape()
        return escaped ?: cursor.fail(start, "the escape is not allowed in patterns")
    }

    private fun decimalReference(start: Int): PatternNode? {
        val isReference = !cursor.isAtEnd && cursor.current in '1'..'9'
        if (!isReference) return null
        val number = cursor.digits()
        val isKnown = number.length <= MAXIMUM_REFERENCE_DIGITS && number.toInt() <= scan.captures
        if (!isKnown) cursor.fail(start, "a back reference names a group the pattern does not have")
        cursor.record(PatternRefusal.Backreference, start, REFERENCE_DETAIL)
        return PatternNode.Empty
    }

    private fun namedReference(start: Int): PatternNode? {
        if (!cursor.accept("k")) return null
        val name = readGroupName(cursor.source, cursor.index + 1).takeIf { cursor.isAt('<') }
        val isKnown = name != null && name.value in scan.names
        if (name == null || !isKnown) cursor.fail(start, "\\k names no group of the pattern")
        cursor.index = name.end
        cursor.record(PatternRefusal.Backreference, start, REFERENCE_DETAIL)
        return PatternNode.Empty
    }

    private fun classEscapeSet(kind: Char): CodePointSet? = mapOf(
        'd' to CharacterSets.digits,
        's' to CharacterSets.space,
        'w' to CharacterSets.word,
    )[kind]

    private fun property(start: Int): CodePointSet {
        if (!cursor.accept("{")) cursor.fail(start, "a property escape needs a name in braces such as \\p{L}")
        val nameStart = cursor.index
        while (!cursor.isAtEnd && !cursor.isAt('}')) cursor.index++
        if (cursor.isAtEnd) cursor.fail(start, "a property escape is never closed")
        val text = cursor.source.substring(startIndex = nameStart, endIndex = cursor.index)
        cursor.index++
        if (!isPropertyShape(text)) cursor.fail(start, "a property escape has no valid name")
        val lookup = properties[text] ?: PropertyNames.lookup(text)
        properties[text] = lookup
        return when (lookup) {
            is PropertyLookup.Found -> lookup.set
            PropertyLookup.Unsupported -> unsupportedProperty(start)
            PropertyLookup.Unknown -> cursor.fail(start, "a property escape names no property of ECMA-262")
        }
    }

    private fun unsupportedProperty(start: Int): CodePointSet {
        cursor.record(PatternRefusal.UnsupportedProperty, start, UNSUPPORTED_PROPERTY_DETAIL)
        return CodePointSet.Empty
    }

    private fun isPropertyShape(text: String): Boolean {
        val name = text.substringBefore('=')
        val value = text.substringAfter('=', missingDelimiterValue = "")
        val hasValue = text.contains('=')
        val isNameValid = name.isNotEmpty() && name.all { it in PROPERTY_NAME_CHARACTERS }
        val isValueValid = !hasValue || (value.isNotEmpty() && value.all { it in PROPERTY_VALUE_CHARACTERS })
        return isNameValid && isValueValid
    }

    private fun controlEscape(): Int? {
        val position = CONTROL_ESCAPES.indexOf(cursor.current)
        if (position < 0) return null
        cursor.index++
        return CONTROL_VALUES[position].code
    }

    private fun controlLetter(start: Int): Int? {
        if (!cursor.accept("c")) return null
        val isLetter = !cursor.isAtEnd && isAsciiLetter(cursor.current)
        if (!isLetter) cursor.fail(start, "\\c needs an ASCII letter")
        val letter = cursor.current
        cursor.index++
        return letter.code % CONTROL_LETTER_MODULUS
    }

    private fun nullEscape(start: Int): Int? {
        if (!cursor.accept("0")) return null
        val isFollowedByDigit = !cursor.isAtEnd && isAsciiDigit(cursor.current)
        if (isFollowedByDigit) cursor.fail(start, "an octal escape is not allowed in patterns")
        return 0
    }

    private fun hexEscape(start: Int): Int? {
        if (!cursor.accept("x")) return null
        val value = readHex(cursor.source, start = cursor.index, count = HEX_ESCAPE_DIGITS)
        cursor.index += HEX_ESCAPE_DIGITS
        return value ?: cursor.fail(start, "\\x needs two hexadecimal digits")
    }

    private fun unicodeEscape(start: Int): Int? {
        if (!cursor.isAt('u')) return null
        val scanned = readUnicodeEscape(cursor.source, cursor.index)
        val escape = scanned ?: cursor.fail(start, "\\u needs four hexadecimal digits or a code point in braces")
        cursor.index = escape.end
        return escape.value
    }

    private fun identityEscape(): Int? {
        val char = cursor.current
        if (char !in SYNTAX_CHARACTERS) return null
        cursor.index++
        return char.code
    }
}
