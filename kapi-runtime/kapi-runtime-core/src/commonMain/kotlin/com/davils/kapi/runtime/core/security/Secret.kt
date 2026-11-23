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

package com.davils.kapi.runtime.core.security

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

private const val SECRET_HASH: Int = 0x5EC

/**
 * A credential such as a bearer token or an API key, held so that it is never shown by accident.
 *
 * [toString] shows only the mask [SafeText.REDACTED][com.davils.kore.core.text.SafeText.REDACTED], and [hashCode] is
 * the same for every secret, so neither a message, a log of the application nor a hash table reveals it. Two secrets
 * are equal when their texts are; [equals] compares every character whatever the first difference, so the time it
 * takes reveals at most the length. [reveal] hands out the text where it must be sent. A secret is immutable and safe
 * to share between threads; the text stays in memory until it is collected.
 *
 * @param text the text of the secret.
 * @since 1.0.0
 */
public class Secret(private val text: String) {
    /**
     * Returns the text of the secret, to write it where it belongs, such as an `Authorization` field.
     *
     * @return the text.
     * @since 1.0.0
     */
    public fun reveal(): String = text

    override fun equals(other: Any?): Boolean {
        if (other !is Secret || other.text.length != text.length) return false
        var difference = 0
        text.indices.forEach { index -> difference = difference or (text[index].code xor other.text[index].code) }
        return difference == 0
    }

    override fun hashCode(): Int = SECRET_HASH

    override fun toString(): String = SafeText.REDACTED
}
