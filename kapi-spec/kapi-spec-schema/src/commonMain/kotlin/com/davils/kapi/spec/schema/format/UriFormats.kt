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

import com.davils.kapi.spec.schema.pattern.appendPoint
import com.davils.kapi.spec.schema.pattern.charCount
import com.davils.kapi.spec.schema.pattern.codePointAt
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.uri.UriReference

private const val FIRST_UCS: Int = 0xA0
private const val SURROGATES_START: Int = 0xD800
private const val PRIVATE_USE_START: Int = 0xE000
private const val PRIVATE_USE_END: Int = 0xF8FF
private const val NONCHARACTERS_START: Int = 0xFDD0
private const val NONCHARACTERS_END: Int = 0xFDEF
private const val PLANE_MASK: Int = 0xFFFF
private const val PLANE_END: Int = 0xFFFE
private const val TAGS_START: Int = 0xE0000
private const val TAGS_END: Int = 0xE0FFF
private const val PRIVATE_PLANES_START: Int = 0xF0000
private const val HEX_DIGITS: String = "0123456789ABCDEF"
private const val NIBBLE_BITS: Int = 4
private const val NIBBLE_MASK: Int = 0xF
private const val BYTE_MASK: Int = 0xFF
private const val FIRST_GRAPHIC: Int = 0x21
private const val LAST_GRAPHIC: Int = 0x7E
private const val LAST_ASCII: Int = 0x7F
private const val ENCODED_LENGTH: Int = 3
private const val TEMPLATE_OPERATORS: String = "+#./;?&"
private const val RESERVED_OPERATORS: String = "=,!@|"
private const val TEMPLATE_EXCLUDED: String = "\"%<>\\^`{|}"
private const val MAXIMUM_PREFIX_DIGITS: Int = 4

internal object UriFormats {
    fun uri(value: String): String? {
        val reference = UriReference.parseOrNull(value)
        return "a URI has a scheme and follows RFC 3986".takeUnless { reference?.isAbsolute == true }
    }

    fun uriReference(value: String): String? =
        "a URI reference follows RFC 3986".takeIf { UriReference.parseOrNull(value) == null }

    fun iri(value: String): String? {
        val reference = asUri(value)?.let(UriReference::parseOrNull)
        return "an IRI has a scheme and follows RFC 3987".takeUnless { reference?.isAbsolute == true }
    }

    fun iriReference(value: String): String? {
        val reference = asUri(value)?.let(UriReference::parseOrNull)
        return "an IRI reference follows RFC 3987".takeIf { reference == null }
    }

    fun uriTemplate(value: String): String? {
        var index = 0
        var isValid = true
        while (isValid && index < value.length) {
            val isExpression = value[index] == '{'
            if (isExpression) {
                val end = value.indexOf('}', index)
                isValid = end > index && isExpressionBody(value.substring(startIndex = index + 1, endIndex = end))
                index = end + 1
            }
            if (!isExpression) {
                isValid = isLiteral(value, index)
                index += literalLength(value, index)
            }
        }
        return "a URI template follows the syntax of RFC 6570".takeUnless { isValid }
    }

    private fun asUri(value: String): String? {
        val fragmentStart = value.indexOf('#').takeIf { it >= 0 } ?: value.length
        val question = value.indexOf('?')
        val queryStart = question.takeIf { it in 0 until fragmentStart } ?: fragmentStart
        val uri = StringBuilder()
        var index = 0
        var isValid = true
        while (isValid && index < value.length) {
            val point = codePointAt(value, index)
            val isAscii = point <= LAST_ASCII
            val isPrivateInQuery = isPrivateUse(point) && index in queryStart until fragmentStart
            isValid = isAscii || isUcs(point) || isPrivateInQuery
            if (isAscii) uri.append(point.toChar())
            if (!isAscii) percentEncode(uri, point)
            index += charCount(point)
        }
        return uri.toString().takeIf { isValid }
    }

    private fun isUcs(point: Int): Boolean {
        val isInPlane = point and PLANE_MASK < PLANE_END
        val isExcluded = point in SURROGATES_START..PRIVATE_USE_END ||
            point in NONCHARACTERS_START..NONCHARACTERS_END ||
            point in TAGS_START..TAGS_END ||
            point >= PRIVATE_PLANES_START
        return point >= FIRST_UCS && isInPlane && !isExcluded
    }

    private fun isPrivateUse(point: Int): Boolean {
        val isInPlane = point and PLANE_MASK < PLANE_END
        val isInPrivatePlane = point >= PRIVATE_PLANES_START && isInPlane
        return point in PRIVATE_USE_START..PRIVATE_USE_END || isInPrivatePlane
    }

    private fun percentEncode(uri: StringBuilder, point: Int) {
        val character = StringBuilder().appendPoint(point)
        val bytes = character.toString().encodeToByteArray()
        bytes.forEach { byte ->
            val value = byte.toInt() and BYTE_MASK
            uri.append('%')
            uri.append(HEX_DIGITS[value shr NIBBLE_BITS])
            uri.append(HEX_DIGITS[value and NIBBLE_MASK])
        }
    }

    private fun literalLength(value: String, index: Int): Int {
        val isEncoded = value[index] == '%'
        return ENCODED_LENGTH.takeIf { isEncoded } ?: charCount(codePointAt(value, index))
    }

    private fun isLiteral(value: String, index: Int): Boolean {
        val char = value[index]
        val point = codePointAt(value, index)
        val isAsciiLiteral = point in FIRST_GRAPHIC..LAST_GRAPHIC && char !in TEMPLATE_EXCLUDED
        return isEncodedAt(value, index) || isAsciiLiteral || isUcs(point) || isPrivateUse(point)
    }

    private fun isEncodedAt(text: String, index: Int): Boolean {
        val isInside = index + 2 < text.length
        return isInside && text[index] == '%' && isHexDigit(text[index + 1]) && isHexDigit(text[index + 2])
    }

    private fun isExpressionBody(body: String): Boolean {
        val operator = body.firstOrNull()
        if (operator != null && operator in RESERVED_OPERATORS) return false
        val hasOperator = operator != null && operator in TEMPLATE_OPERATORS
        val variables = body.substring(1.takeIf { hasOperator } ?: 0)
        return variables.split(',').all(::isVariable)
    }

    private fun isVariable(variable: String): Boolean {
        val colon = variable.indexOf(':')
        val hasPrefix = colon >= 0
        val isExploded = !hasPrefix && variable.endsWith('*')
        val name = variable.substring(startIndex = 0, endIndex = colon.takeIf { hasPrefix } ?: variable.length)
        val isPrefixValid = !hasPrefix || isPrefix(variable.substring(colon + 1))
        val plainName = name.removeSuffix("*").takeIf { isExploded } ?: name
        return isPrefixValid && isVariableName(plainName)
    }

    private fun isPrefix(modifier: String): Boolean =
        modifier.length in 1..MAXIMUM_PREFIX_DIGITS && isAllDigits(modifier) && !modifier.startsWith('0')

    private fun isVariableName(name: String): Boolean {
        val parts = name.split('.')
        return parts.all { part -> part.isNotEmpty() && isVariableText(part) }
    }

    private fun isVariableText(text: String): Boolean {
        var index = 0
        var isValid = true
        while (isValid && index < text.length) {
            val char = text[index]
            val isEncoded = isEncodedAt(text, index)
            isValid = isEncoded || isLetter(char) || isDigit(char) || char == '_'
            index += ENCODED_LENGTH.takeIf { isEncoded } ?: 1
        }
        return isValid
    }
}
