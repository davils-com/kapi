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

package com.davils.kapi.spec.schema.internal

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.uri.UriReference

private const val HEX_RADIX: Int = 16

private const val ESCAPE_LENGTH: Int = 3

internal object Uris {
    fun resolve(reference: String, base: String): String? {
        val parsedBase = UriReference.parseOrNull(base) ?: return null
        val parsedReference = UriReference.parseOrNull(reference)
        val resolved = parsedReference?.let { parsedBase.resolve(it) }
        return resolved?.normalize()?.toString()
    }

    fun normalized(uri: String): String? {
        val parsed = UriReference.parseOrNull(uri)?.takeIf { it.isAbsolute }
        val text = parsed?.normalize()?.toString()
        return text?.removeSuffix("#")
    }

    fun withoutFragment(uri: String): String = uri.substringBefore('#')

    fun fragmentOf(uri: String): String = uri.substringAfter('#', missingDelimiterValue = "")

    fun pointerOf(fragment: String): List<String>? {
        val decoded = percentDecoded(fragment) ?: return null
        return JsonPointer.parseOrNull(decoded)?.tokens
    }

    fun percentDecoded(text: String): String? {
        val bytes = ArrayList<Byte>(text.length)
        var index = 0
        var isValid = true
        while (index < text.length && isValid) {
            val escape = text[index] == '%'
            val value = hexAt(text, index + 1).takeIf { escape }
            isValid = !escape || value != null
            val chunk = value?.let { byteArrayOf(it.toByte()) } ?: text[index].toString().encodeToByteArray()
            chunk.forEach { bytes += it }
            index += ESCAPE_LENGTH.takeIf { escape } ?: 1
        }
        return bytes.toByteArray().decodeToString().takeIf { isValid }
    }

    private fun hexAt(text: String, start: Int): Int? {
        val digits = text.substring(start.coerceAtMost(text.length), (start + 2).coerceAtMost(text.length))
        val isComplete = digits.length == 2 && digits.all { it.isHexDigit() }
        return digits.takeIf { isComplete }?.toInt(HEX_RADIX)
    }

    private fun Char.isHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
}
