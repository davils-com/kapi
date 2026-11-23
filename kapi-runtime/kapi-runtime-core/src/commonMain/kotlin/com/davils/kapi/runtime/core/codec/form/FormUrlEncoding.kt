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

package com.davils.kapi.runtime.core.codec.form

import com.davils.kapi.runtime.core.internal.FieldGrammar
import com.davils.kapi.runtime.core.internal.PercentCoding
import com.davils.kapi.runtime.core.internal.Utf8
import com.davils.kore.core.outcome.Outcome

private const val SEPARATOR: Char = '&'

private const val ASSIGN: Char = '='

private const val KEPT_SYMBOLS: String = "*-._"

/**
 * Writes and reads `application/x-www-form-urlencoded` as the WHATWG URL standard defines it, with the strictness of
 * the runtime.
 *
 * [encode] keeps ASCII letters, digits and `*-._`, writes a space as `+` and percent-encodes every other octet of the
 * UTF-8 form of a name or value, so its result carries no character a URL query or a body could misread. [decode]
 * refuses what the standard would repair: a percent sign that starts no percent-encoded octet and octets that are no
 * UTF-8. It keeps the limits it is handed before it decodes a field. The functions keep no state and are safe to call
 * from any thread.
 *
 * @since 1.0.0
 */
public object FormUrlEncoding {
    /**
     * Writes [fields] as `application/x-www-form-urlencoded`.
     *
     * @param fields the names and values in the order to write them; a name may repeat.
     * @return the encoded text, such as `a=1&b=x+y`.
     * @since 1.0.0
     */
    public fun encode(fields: List<Pair<String, String>>): String =
        fields.joinToString(SEPARATOR.toString()) { (name, value) -> encodePart(name) + ASSIGN + encodePart(value) }

    /**
     * Reads [text] as `application/x-www-form-urlencoded`.
     *
     * Empty sequences between ampersands are skipped, a field without `=` has the empty value, and `+` stands for a
     * space.
     *
     * @param text the body or query, which may come from an attacker.
     * @param limits the limits to keep.
     * @return the names and values in the order they were written, or the reason the text was refused.
     * @since 1.0.0
     */
    public fun decode(
        text: String,
        limits: FormLimits = FormLimits.Default,
    ): Outcome<List<Pair<String, String>>, FormFailure> {
        if (text.length > limits.maxLength) return Outcome.Failure(FormFailure.TooLong)
        val reader = Reader(text, limits)
        return reader.read()
    }

    /**
     * Reads the UTF-8 [bytes] as `application/x-www-form-urlencoded` like [decode].
     *
     * The body is read as UTF-8 first and refused with [FormFailure.NotUtf8] if it is none.
     *
     * @param bytes the body, which may come from an attacker.
     * @param limits the limits to keep.
     * @return the names and values, or the reason the body was refused.
     * @since 1.0.0
     */
    public fun decode(
        bytes: ByteArray,
        limits: FormLimits = FormLimits.Default,
    ): Outcome<List<Pair<String, String>>, FormFailure> {
        val text = bytes.takeIf { content -> content.size <= limits.maxLength }?.let(Utf8::decodeOrNull)
        val failure = FormFailure.NotUtf8.takeIf { bytes.size <= limits.maxLength } ?: FormFailure.TooLong
        if (text == null) return Outcome.Failure(failure)
        return decode(text, limits)
    }

    internal fun encodePart(text: String): String = PercentCoding.encode(text, isKept = ::isKept, isSpacePlus = true)

    private fun isKept(char: Char): Boolean = FieldGrammar.isAlphaNumeric(char) || char in KEPT_SYMBOLS

    private class Reader(private val text: String, private val limits: FormLimits) {
        private val fields: MutableList<Pair<String, String>> = ArrayList()

        private var failure: FormFailure? = null

        fun read(): Outcome<List<Pair<String, String>>, FormFailure> {
            var start = 0
            while (failure == null && start <= text.length) {
                val end = text.indexOf(SEPARATOR, start).takeIf { index -> index >= 0 } ?: text.length
                if (end > start) field(text.substring(startIndex = start, endIndex = end))
                start = end + 1
            }
            val refused = failure?.let { reason -> Outcome.Failure(reason) }
            return refused ?: Outcome.Success(fields.toList())
        }

        private fun field(sequence: String) {
            val name = sequence.substringBefore(ASSIGN)
            val value = sequence.substringAfter(ASSIGN, "")
            val isTooLong = name.length > limits.maxFieldLength || value.length > limits.maxFieldLength
            failure = FormFailure.TooManyFields.takeIf { fields.size >= limits.maxFields }
                ?: FormFailure.FieldTooLong.takeIf { isTooLong }
            if (failure != null) return
            val decodedName = decoded(name)
            val decodedValue = decoded(value)
            if (decodedName != null && decodedValue != null) fields += decodedName to decodedValue
        }

        private fun decoded(part: String): String? {
            val bytes = PercentCoding.decodeBytes(part, isPlusSpace = true)
            val text = bytes?.let(Utf8::decodeOrNull)
            failure = failure ?: FormFailure.MalformedEscape.takeIf { bytes == null }
                ?: FormFailure.NotUtf8.takeIf { text == null }
            return text
        }
    }
}
