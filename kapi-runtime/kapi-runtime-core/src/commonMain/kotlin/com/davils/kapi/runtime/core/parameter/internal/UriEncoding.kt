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

package com.davils.kapi.runtime.core.parameter.internal

import com.davils.kapi.runtime.core.internal.FieldGrammar
import com.davils.kapi.runtime.core.internal.PercentCoding

private const val UNRESERVED_SYMBOLS: String = "-._~"

private const val RESERVED_IN_QUERY: String = ":/?[]@!$'()*,;"

private const val ESCAPE_LENGTH: Int = 3

internal object UriEncoding {
    fun isUnreserved(char: Char): Boolean = FieldGrammar.isAlphaNumeric(char) || char in UNRESERVED_SYMBOLS

    fun encode(text: String): String = PercentCoding.encode(text, isKept = ::isUnreserved)

    fun encodeQuery(text: String, isReservedAllowed: Boolean): String {
        if (!isReservedAllowed) return encode(text)
        val out = StringBuilder(text.length)
        val pending = StringBuilder()
        var index = 0
        while (index < text.length) {
            val isTriple = PercentCoding.isEscape(text, index)
            val char = text[index]
            val isKept = isTriple || isUnreserved(char) || char in RESERVED_IN_QUERY
            if (isKept) flush(out, pending)
            if (isTriple) out.append(text, index, index + ESCAPE_LENGTH)
            if (isKept && !isTriple) out.append(char)
            if (!isKept) pending.append(char)
            index += ESCAPE_LENGTH.takeIf { isTriple } ?: 1
        }
        flush(out, pending)
        return out.toString()
    }

    private fun flush(out: StringBuilder, pending: StringBuilder) {
        if (pending.isEmpty()) return
        out.append(encode(pending.toString()))
        pending.clear()
    }
}
