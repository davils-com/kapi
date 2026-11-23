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

package com.davils.kapi.runtime.core.header

import com.davils.kapi.runtime.core.structured.BareItem
import com.davils.kapi.runtime.core.structured.StructuredFields
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.identity.generator.UuidV4Generator
import com.davils.kore.core.text.SafeText
import kotlin.jvm.JvmInline

private const val MOST_LENGTH: Int = 255

/**
 * The value of `Idempotency-Key` of draft-ietf-httpapi-idempotency-key-header: a String of RFC 9651 that names one
 * attempt of a non-idempotent request, such as `"8e03978e-40d5-43e8-bc93-6894a57f9324"`.
 *
 * A key has 1 to 255 printable ASCII characters. The draft asks for keys with enough randomness that no one can guess
 * them: [random] makes one from a version 4 UUID. A server compares keys of one client exactly. [toString] is the
 * field value.
 *
 * @property value the key without quotes.
 * @throws IllegalArgumentException if [value] is empty, longer than 255 characters or holds a character outside
 * printable ASCII, such as a line break.
 * @since 1.0.0
 */
@JvmInline
public value class IdempotencyKey(public val value: String) {
    init {
        require(value.length in 1..MOST_LENGTH && value.all { char -> char in ' '..'~' }) {
            "${SafeText.quoted(value)} is no idempotency key of 1 to 255 printable characters"
        }
    }

    override fun toString(): String = BareItem.Text(value).toString()

    /**
     * Reads and makes keys.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Returns a new key from 122 random bits of a version 4 UUID.
         *
         * @return the key.
         * @since 1.0.0
         */
        public fun random(): IdempotencyKey = IdempotencyKey(UuidV4Generator.next().toString())

        /**
         * Reads the value of `Idempotency-Key`, a String of RFC 9651; parameters are allowed and ignored.
         *
         * @param text the field value, which may come from an attacker.
         * @return the key, or `null` if [text] is no String or the String is empty or longer than 255 characters,
         * which a server answers with 400 (Bad Request).
         * @since 1.0.0
         */
        public fun parseOrNull(text: String): IdempotencyKey? {
            val item = StructuredFields.parseItemOrNull(text)?.value as? BareItem.Text
            return item?.value?.takeIf { key -> key.length in 1..MOST_LENGTH }?.let(::IdempotencyKey)
        }
    }
}
