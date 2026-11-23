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

package com.davils.kapi.runtime.core.structured.internal

import com.davils.kapi.runtime.core.internal.FieldGrammar
import com.davils.kapi.runtime.core.internal.Utf8
import com.davils.kapi.runtime.core.structured.BareItem
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.ByteStringBuilder
import kotlin.io.encoding.Base64

private const val MOST_INTEGER_DIGITS: Int = 15

private const val MOST_WHOLE_DIGITS: Int = 12

private const val MOST_FRACTION_DIGITS: Int = 3

private const val SCALE: Long = 1_000L

private const val RADIX: Int = 16

private const val HEX_LENGTH: Int = 2

private const val BASE64_SYMBOLS: String = "+/="

private const val SPACE: Char = ' '

private val placeholder: BareItem = BareItem.Flag(false)

private val base64: Base64 = Base64.Default.withPadding(Base64.PaddingOption.PRESENT_OPTIONAL)

internal class BareItemReader(private val cursor: StructuredCursor) {
    private val readers: Map<Char, () -> BareItem> = buildMap {
        for (digit in '0'..'9') this[digit] = ::number
        this['-'] = ::number
        for (letter in ('a'..'z') + ('A'..'Z') + '*') this[letter] = ::token
        this['"'] = ::string
        this[':'] = ::bytes
        this['?'] = ::flag
        this['@'] = ::date
        this['%'] = ::displayString
    }

    fun read(): BareItem {
        val reader = cursor.peek()?.let(readers::get) ?: return failed()
        return reader()
    }

    private fun number(): BareItem {
        val isNegative = cursor.consume('-')
        val whole = cursor.span(FieldGrammar::isDigit)
        val hasPoint = cursor.consume('.')
        val fractionDigits = cursor.span(FieldGrammar::isDigit)
        val fraction = fractionDigits.takeIf { hasPoint }.orEmpty()
        val isValid = whole.isNotEmpty() && hasPoint == fraction.isNotEmpty()
        val isBounded = isValid && whole.length <= MOST_INTEGER_DIGITS && isDecimalBounded(hasPoint, whole, fraction)
        if (!isBounded) return failed()
        val sign = (-1L).takeIf { isNegative } ?: 1L
        return numberOf(sign, whole, fraction.takeIf { hasPoint })
    }

    private fun isDecimalBounded(hasPoint: Boolean, whole: String, fraction: String): Boolean =
        !hasPoint || (whole.length <= MOST_WHOLE_DIGITS && fraction.length <= MOST_FRACTION_DIGITS)

    private fun numberOf(sign: Long, whole: String, fraction: String?): BareItem {
        if (fraction == null) return BareItem.Integer(sign * whole.toLong())
        val padded = fraction.padEnd(MOST_FRACTION_DIGITS, '0')
        return BareItem.Decimal(sign * (whole.toLong() * SCALE + padded.toLong()))
    }

    private fun string(): BareItem {
        cursor.skip(1)
        val out = StringBuilder()
        var isOpen = true
        while (isOpen && !cursor.isFailed) {
            val char = cursor.next()
            isOpen = char != '"'
            if (isOpen) stringChar(out, char)
        }
        val text = out.toString()
        return BareItem.Text(text.takeUnless { cursor.isFailed }.orEmpty())
    }

    private fun stringChar(out: StringBuilder, char: Char?) {
        val shown = escapedOf(char)
        if (shown == null || !StructuredGrammar.isPrintable(shown)) cursor.fail()
        shown?.let(out::append)
    }

    private fun escapedOf(char: Char?): Char? {
        if (char != '\\') return char
        return cursor.next()?.takeIf { found -> found == '"' || found == '\\' }
    }

    private fun token(): BareItem = BareItem.Token(cursor.span(StructuredGrammar::isTokenChar))

    private fun bytes(): BareItem {
        cursor.skip(1)
        val encoded = cursor.span { char -> FieldGrammar.isAlphaNumeric(char) || char in BASE64_SYMBOLS }
        if (!cursor.consume(':')) cursor.fail()
        val decoded = try {
            base64.decode(encoded)
        } catch (_: IllegalArgumentException) {
            null
        }
        if (decoded == null) cursor.fail()
        return BareItem.Bytes(ByteString(decoded ?: ByteArray(0)))
    }

    private fun flag(): BareItem {
        cursor.skip(1)
        val char = cursor.next()
        if (char != '0' && char != '1') cursor.fail()
        return BareItem.Flag(char == '1')
    }

    private fun date(): BareItem {
        cursor.skip(1)
        val number = number() as? BareItem.Integer
        return number?.let { found -> BareItem.Date(found.value) } ?: failed()
    }

    private fun displayString(): BareItem {
        cursor.skip(1)
        if (!cursor.consume('"')) cursor.fail()
        val out = ByteStringBuilder()
        var isOpen = !cursor.isFailed
        while (isOpen && !cursor.isFailed) {
            val char = cursor.next()
            isOpen = char != '"'
            if (isOpen) displayChar(out, char)
        }
        val bytes = out.toByteString()
        val text = Utf8.decodeOrNull(bytes.toByteArray())
        if (text == null) cursor.fail()
        return BareItem.DisplayText(text.takeUnless { cursor.isFailed }.orEmpty())
    }

    private fun displayChar(out: ByteStringBuilder, char: Char?) {
        if (char != '%') {
            if (char == null || !StructuredGrammar.isPrintable(char)) cursor.fail()
            out.append((char ?: SPACE).code.toByte())
            return
        }
        val hex = cursor.ahead(HEX_LENGTH)
        val isHex = hex.length == HEX_LENGTH && hex.all { digit -> FieldGrammar.isDigit(digit) || digit in 'a'..'f' }
        if (!isHex) cursor.fail()
        cursor.skip(HEX_LENGTH)
        out.append((hex.takeIf { isHex }?.toInt(RADIX) ?: 0).toByte())
    }

    private fun failed(): BareItem {
        cursor.fail()
        return placeholder
    }
}
