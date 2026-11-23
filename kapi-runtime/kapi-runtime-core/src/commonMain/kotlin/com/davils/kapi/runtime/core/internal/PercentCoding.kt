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

import kotlinx.io.bytestring.ByteStringBuilder

private const val HEX: String = "0123456789ABCDEF"

private const val NIBBLE: Int = 4

private const val LOW: Int = 0xF

private const val BYTE: Int = 0xFF

private const val ASCII_LIMIT: Int = 0x80

private const val PERCENT: Char = '%'

private const val PLUS: Char = '+'

private const val SPACE: Char = ' '

private const val ESCAPE_LENGTH: Int = 3

private const val RADIX: Int = 16

internal object PercentCoding {
    fun encode(text: String, isKept: (Char) -> Boolean, isSpacePlus: Boolean = false): String {
        val out = StringBuilder(text.length)
        text.encodeToByteArray().forEach { byte -> append(out, byte.toInt() and BYTE, isKept, isSpacePlus) }
        return out.toString()
    }

    fun decodeBytes(text: String, isPlusSpace: Boolean = false): ByteArray? {
        val out = ByteStringBuilder(text.length)
        var index = 0
        var isValid = true
        while (isValid && index < text.length) {
            val char = text[index]
            val isEscaped = char == PERCENT
            isValid = !isEscaped || isEscape(text, index)
            if (isEscaped && isValid) out.append(escapedAt(text, index).toByte())
            if (!isEscaped) appendPlain(out, char, isPlusSpace)
            index += ESCAPE_LENGTH.takeIf { isEscaped } ?: 1
        }
        val bytes = out.toByteString()
        return bytes.toByteArray().takeIf { isValid }
    }

    fun decode(text: String, isPlusSpace: Boolean = false): String? {
        val bytes = decodeBytes(text, isPlusSpace) ?: return null
        return Utf8.decodeOrNull(bytes)
    }

    fun isEscape(text: String, index: Int): Boolean {
        val isLongEnough = index + ESCAPE_LENGTH <= text.length
        return isLongEnough && text[index] == PERCENT && isHex(text[index + 1]) && isHex(text[index + 2])
    }

    fun isHex(char: Char): Boolean = FieldGrammar.isDigit(char) || char in 'a'..'f' || char in 'A'..'F'

    private fun escapedAt(text: String, index: Int): Int =
        text[index + 1].digitToInt(RADIX) * RADIX + text[index + 2].digitToInt(RADIX)

    private fun appendPlain(out: ByteStringBuilder, char: Char, isPlusSpace: Boolean) {
        if (char.code < ASCII_LIMIT) {
            out.append((SPACE.takeIf { isPlusSpace && char == PLUS } ?: char).code.toByte())
            return
        }
        out.append(char.toString().encodeToByteArray())
    }

    private fun append(out: StringBuilder, code: Int, isKept: (Char) -> Boolean, isSpacePlus: Boolean) {
        val char = code.toChar()
        val isPlainAscii = code < ASCII_LIMIT && isKept(char)
        if (isPlainAscii) {
            out.append(char)
            return
        }
        if (isSpacePlus && char == SPACE) {
            out.append(PLUS)
            return
        }
        out.append(PERCENT)
        out.append(HEX[code shr NIBBLE])
        out.append(HEX[code and LOW])
    }
}
