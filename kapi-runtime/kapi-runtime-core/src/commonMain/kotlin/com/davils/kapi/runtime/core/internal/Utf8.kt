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

private const val ASCII_LIMIT: Int = 0x80

private const val BYTE: Int = 0xFF

internal object Utf8 {
    fun decodeOrNull(bytes: ByteArray): String? = try {
        bytes.decodeToString(throwOnInvalidSequence = true)
    } catch (_: CharacterCodingException) {
        null
    }

    fun asciiOrNull(bytes: ByteArray): String? {
        if (bytes.any { byte -> byte.toInt() and BYTE >= ASCII_LIMIT }) return null
        return latin1(bytes)
    }

    fun latin1(bytes: ByteArray): String {
        val chars = CharArray(bytes.size) { index -> (bytes[index].toInt() and BYTE).toChar() }
        return chars.concatToString()
    }

    fun codePointCount(text: String): Int {
        var count = 0
        var index = 0
        while (index < text.length) {
            val isPair = text[index].isHighSurrogate() && index + 1 < text.length && text[index + 1].isLowSurrogate()
            index += 2.takeIf { isPair } ?: 1
            count += 1
        }
        return count
    }
}
