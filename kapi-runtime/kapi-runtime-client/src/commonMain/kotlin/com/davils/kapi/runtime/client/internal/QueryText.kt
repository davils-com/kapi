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

package com.davils.kapi.runtime.client.internal

import com.davils.kapi.runtime.client.call.Call
import io.ktor.http.URLBuilder

private const val UNRESERVED_SYMBOLS: String = "-._~"

private const val HEX: String = "0123456789ABCDEF"

private const val NIBBLE: Int = 4

private const val LOW_NIBBLE: Int = 0x0F

private const val BYTE: Int = 0xFF

internal object QueryText {
    fun encoded(text: String): String? {
        val bytes = text.encodeToByteArray().takeIf { isWellFormed(text) } ?: return null
        return bytes.joinToString(separator = "") { byte -> escaped(byte.toInt() and BYTE) }
    }

    fun built(url: String, call: Call): String {
        val builder = URLBuilder(url)
        val queryString = call.queryString?.takeIf(String::isNotEmpty)
        queryString?.let { text -> builder.encodedParameters.appendAll(text, emptyList()) }
        call.query.forEach { entry -> builder.parameters.append(name = entry.name, value = entry.value) }
        return builder.buildString()
    }

    private fun escaped(value: Int): String {
        val char = value.toChar()
        if (isUnreserved(char)) return char.toString()
        return "%" + HEX[value shr NIBBLE] + HEX[value and LOW_NIBBLE]
    }

    private fun isUnreserved(char: Char): Boolean =
        char in 'a'..'z' || char in 'A'..'Z' || char in '0'..'9' || char in UNRESERVED_SYMBOLS

    private fun isWellFormed(text: String): Boolean {
        var index = 0
        var isValid = true
        while (index < text.length && isValid) {
            val char = text[index]
            val isPair = char.isHighSurrogate() && index + 1 < text.length && text[index + 1].isLowSurrogate()
            isValid = isPair || !char.isSurrogate()
            index += 2.takeIf { isPair } ?: 1
        }
        return isValid
    }
}
