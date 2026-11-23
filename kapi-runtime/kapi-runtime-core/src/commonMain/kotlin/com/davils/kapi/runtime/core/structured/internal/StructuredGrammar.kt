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
import com.davils.kapi.runtime.core.structured.BareItem
import com.davils.kapi.runtime.core.structured.Dictionary
import com.davils.kapi.runtime.core.structured.Item
import com.davils.kapi.runtime.core.structured.Parameters
import kotlinx.io.bytestring.ByteString
import kotlin.io.encoding.Base64
import kotlin.math.abs

private const val MOST_INTEGER: Long = 999_999_999_999_999L

private const val SCALE: Long = 1_000L

private const val FRACTION_DIGITS: Int = 3

private const val KEY_SYMBOLS: String = "_-.*"

private const val TOKEN_EXTRA: String = ":/"

private const val HEX: String = "0123456789abcdef"

private const val NIBBLE: Int = 4

private const val LOW: Int = 0xF

private const val BYTE_MASK: Int = 0xFF

private const val FIRST_PRINTABLE: Char = ' '

private const val LAST_PRINTABLE: Char = '~'

internal object StructuredGrammar {
    val INTEGER_RANGE: LongRange = -MOST_INTEGER..MOST_INTEGER

    fun isPrintable(char: Char): Boolean = char in FIRST_PRINTABLE..LAST_PRINTABLE

    fun isKeyStart(char: Char): Boolean = char in 'a'..'z' || char == '*'

    fun isKeyChar(char: Char): Boolean = char in 'a'..'z' || FieldGrammar.isDigit(char) || char in KEY_SYMBOLS

    fun isKey(text: String): Boolean {
        val first = text.firstOrNull() ?: return false
        return isKeyStart(first) && text.all(::isKeyChar)
    }

    fun isTokenStart(char: Char): Boolean = FieldGrammar.isAlpha(char) || char == '*'

    fun isTokenChar(char: Char): Boolean = FieldGrammar.isTokenChar(char) || char in TOKEN_EXTRA

    fun isToken(text: String): Boolean {
        val first = text.firstOrNull() ?: return false
        return isTokenStart(first) && text.all(::isTokenChar)
    }

    fun isWellFormed(text: String): Boolean {
        var index = 0
        var isValid = true
        while (isValid && index < text.length) {
            val char = text[index]
            val isPair = char.isHighSurrogate() && text.getOrNull(index + 1)?.isLowSurrogate() == true
            isValid = isPair || !char.isSurrogate()
            index += 2.takeIf { isPair } ?: 1
        }
        return isValid
    }

    fun decimalText(thousandths: Long): String {
        val sign = "-".takeIf { thousandths < 0 }.orEmpty()
        val magnitude = abs(thousandths)
        val fraction = (magnitude % SCALE).toString().padStart(FRACTION_DIGITS, '0')
        val trimmed = fraction.trimEnd('0').ifEmpty { "0" }
        return "$sign${magnitude / SCALE}.$trimmed"
    }

    fun quoted(text: String): String {
        val out = StringBuilder(text.length + 2)
        out.append('"')
        text.forEach { char ->
            if (char == '"' || char == '\\') out.append('\\')
            out.append(char)
        }
        out.append('"')
        return out.toString()
    }

    fun bytesText(bytes: ByteString): String = ":" + Base64.Default.encode(bytes.toByteArray()) + ":"

    fun displayText(text: String): String {
        val out = StringBuilder("%\"")
        text.encodeToByteArray().forEach { byte ->
            val code = byte.toInt() and BYTE_MASK
            val char = code.toChar()
            val isPlain = isPrintable(char) && char != '%' && char != '"'
            if (isPlain) out.append(char)
            if (!isPlain) escape(out, code)
        }
        out.append('"')
        return out.toString()
    }

    private fun escape(out: StringBuilder, code: Int) {
        out.append('%')
        out.append(HEX[code shr NIBBLE])
        out.append(HEX[code and LOW])
    }

    fun parametersText(parameters: Parameters): String =
        parameters.entries.joinToString("") { (key, value) -> ";" + key + valueText(value) }

    fun dictionaryText(dictionary: Dictionary): String = dictionary.entries.joinToString(", ") { (key, member) ->
        val isTrue = member is Item && member.value == BareItem.Flag(true)
        key + ("=$member".takeUnless { isTrue } ?: member.parameters.toString())
    }

    private fun valueText(value: BareItem): String {
        if (value == BareItem.Flag(true)) return ""
        return "=$value"
    }
}
