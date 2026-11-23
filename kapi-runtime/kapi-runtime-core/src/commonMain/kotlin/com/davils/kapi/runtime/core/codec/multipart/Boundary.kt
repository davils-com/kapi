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

package com.davils.kapi.runtime.core.codec.multipart

import com.davils.kapi.runtime.core.internal.FieldGrammar
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.identity.generator.UuidV4Generator
import com.davils.kore.core.text.SafeText
import kotlin.jvm.JvmInline

private const val MOST_LENGTH: Int = 70

private const val SYMBOLS: String = "'()+_,-./:=? "

private const val PREFIX: String = "kapi-"

/**
 * The boundary of a multipart body of RFC 2046 §5.1.1, such as the `boundary` parameter of `multipart/form-data`.
 *
 * A boundary has 1 to 70 characters of the set RFC 2046 allows, letters, digits and `'()+_,-./:=? `, and does not end
 * with a space; it can neither break a header line nor be confused with one. A writer should use [random], whose
 * boundary comes from the secure randomness of the platform, so no content chosen by an attacker can contain it.
 *
 * @property value the text of the boundary, without the leading dashes of a delimiter.
 * @throws IllegalArgumentException if [value] is no boundary.
 * @since 1.0.0
 */
@JvmInline
public value class Boundary(public val value: String) {
    init {
        require(isBoundary(value)) { "${SafeText.quoted(value)} is no multipart boundary" }
    }

    override fun toString(): String = value

    /**
     * Makes boundaries.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Returns a new boundary of 41 characters from 122 random bits of a version 4 UUID.
         *
         * @return the boundary.
         * @since 1.0.0
         */
        public fun random(): Boundary = Boundary(PREFIX + UuidV4Generator.next())

        /**
         * Reads [text] as a boundary.
         *
         * @param text the text, such as the `boundary` parameter of a received `Content-Type`.
         * @return the boundary, or `null` if [text] is none.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String): Boundary? = text.takeIf(::isBoundary)?.let(::Boundary)

        private fun isBoundary(text: String): Boolean {
            val isSized = text.length in 1..MOST_LENGTH && !text.endsWith(' ')
            return isSized && text.all { char -> FieldGrammar.isAlphaNumeric(char) || char in SYMBOLS }
        }
    }
}
