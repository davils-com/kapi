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

package com.davils.kapi.runtime.core.cache

import com.davils.kapi.runtime.core.FieldLimits
import com.davils.kapi.runtime.core.internal.FieldGrammar
import com.davils.kapi.runtime.core.internal.FieldLists
import com.davils.kapi.runtime.core.internal.FieldScanner

private const val ANY: String = "*"

/**
 * The value of `Vary` of RFC 9110 §12.5.5: the request fields a response depends on, or `*` for factors beyond them.
 *
 * Field names are compared without case and kept in lower case. A value is immutable; [toString] is the field value.
 *
 * @param fields the field names, each a token.
 * @property isAny whether the value is `*`, which no cache can match.
 * @throws IllegalArgumentException if a field name is no token.
 * @since 1.0.0
 */
public class Vary(fields: List<String>, public val isAny: Boolean = false) {
    /**
     * The field names in lower case, without repetitions, in order.
     *
     * @since 1.0.0
     */
    public val fields: List<String> = fields.map(String::lowercase).distinct()

    init {
        require(fields.all(FieldGrammar::isToken)) { "A field name of Vary is a token" }
    }

    /**
     * Returns whether the response depends on the request field [name].
     *
     * @param name the field name, compared without case.
     * @return `true` if [isAny] or [name] is listed.
     * @since 1.0.0
     */
    public operator fun contains(name: String): Boolean = isAny || name.lowercase() in fields

    override fun equals(other: Any?): Boolean = other is Vary && fields == other.fields && isAny == other.isAny

    override fun hashCode(): Int = listOf(fields, isAny).hashCode()

    override fun toString(): String {
        val any = listOf(ANY).takeIf { isAny }
        val members = any.orEmpty() + fields
        return members.joinToString(", ")
    }

    /**
     * Reads `Vary`.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Reads the value of `Vary`.
         *
         * @param text the field value, the lines of a repeated field joined with commas.
         * @param limits the limits: the length of the value and the number of names.
         * @return the value, with [isAny] set if any member is `*`, or `null` if a member is no token or the value
         * exceeds a limit.
         * @since 1.0.0
         */
        public fun parseOrNull(text: String, limits: FieldLimits = FieldLimits.Default): Vary? {
            val names = FieldLists.read(
                text = text,
                maxLength = limits.maxFieldLength,
                maxMembers = limits.maxMembers,
                element = FieldScanner::token,
            )
            val isAny = names?.contains(ANY) == true
            return names?.let { found -> Vary(found.filterNot { name -> name == ANY }, isAny) }
        }
    }
}
