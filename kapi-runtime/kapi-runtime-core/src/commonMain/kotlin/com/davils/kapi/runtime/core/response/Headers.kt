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

package com.davils.kapi.runtime.core.response

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedMap
import com.davils.kore.core.text.SafeText

/**
 * The header fields of a response or a request, read without regard to the case of their names.
 *
 * Names that differ only in case are merged, with their values in the order they came. The fields that carry
 * credentials ([SENSITIVE]) and the names the caller marks as sensitive are masked with
 * [SafeText.REDACTED][com.davils.kore.core.text.SafeText.REDACTED] by [masked] and [toString], which also escape every
 * other name and value. Headers are immutable and safe to share between threads; they are kept in an ordered map, so
 * names chosen by an attacker cannot degrade a lookup.
 *
 * @since 1.0.0
 */
public class Headers internal constructor(entries: Map<String, List<String>>, sensitive: Set<String>) {
    private val fields: OrderedMap<String, List<String>> = merged(entries)

    private val hidden: Set<String> = SENSITIVE + sensitive.map(String::lowercase)

    /**
     * The names of the fields in lower case, in the order they first came.
     *
     * @since 1.0.0
     */
    public val names: Set<String> get() = fields.keys.toSet()

    /**
     * Returns the first value of the field [name].
     *
     * @param name the name, compared without case.
     * @return the first value, or `null` if there is no such field.
     * @since 1.0.0
     */
    public operator fun get(name: String): String? = all(name).firstOrNull()

    /**
     * Returns whether there is a field [name].
     *
     * @param name the name, compared without case.
     * @return `true` if the field is present.
     * @since 1.0.0
     */
    public operator fun contains(name: String): Boolean = name.lowercase() in fields

    /**
     * Returns every value of the field [name].
     *
     * @param name the name, compared without case.
     * @return the values in the order they came, or an empty list.
     * @since 1.0.0
     */
    public fun all(name: String): List<String> = fields[name.lowercase()].orEmpty()

    /**
     * Returns every line of the field [name] joined into one value, as RFC 9110 §5.3 combines them: in the order they
     * came, separated by a comma and a space.
     *
     * Only a field whose grammar is a comma-separated list may be combined this way, such as `Accept`, `Cache-Control`
     * or `Link`; `Set-Cookie` may not, and a field whose value is a single item reads its first line with [get].
     *
     * @param name the name, compared without case.
     * @return the combined value, or `null` if there is no such field.
     * @since 1.0.0
     */
    public fun joined(name: String): String? {
        val lines = all(name)
        return lines.takeIf(List<String>::isNotEmpty)?.joinToString(", ")
    }

    /**
     * Returns every field with its values, unmasked.
     *
     * @return the names in lower case with their values.
     * @since 1.0.0
     */
    public fun toMap(): Map<String, List<String>> = fields.entries.toMap()

    /**
     * Returns every field with the values of sensitive fields masked.
     *
     * @return the names in lower case with their values, each value of a sensitive field replaced by the mask.
     * @since 1.0.0
     */
    public fun masked(): Map<String, List<String>> =
        fields.entries.associate { (name, values) -> name to shown(name, values) }

    override fun equals(other: Any?): Boolean = other is Headers && fields.entries == other.fields.entries

    override fun hashCode(): Int = fields.entries.hashCode()

    override fun toString(): String {
        val shown = fields.entries.map { (name, values) ->
            SafeText.text(name) + "=" + shown(name, values).map(SafeText::text)
        }
        return "Headers($shown)"
    }

    private fun shown(name: String, values: List<String>): List<String> {
        if (name !in hidden) return values
        return values.map { SafeText.REDACTED }
    }

    private fun merged(entries: Map<String, List<String>>): OrderedMap<String, List<String>> {
        val merged = OrderedMap<String, List<String>>()
        entries.forEach { (name, values) ->
            val key = name.lowercase()
            merged[key] = merged[key].orEmpty() + values
        }
        return merged
    }

    /**
     * The fields every header set masks and the empty header set.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * The names of the fields that carry credentials and are always masked.
         *
         * @since 1.0.0
         */
        public val SENSITIVE: Set<String> =
            setOf("authorization", "cookie", "proxy-authorization", "set-cookie", "x-api-key")

        /**
         * The header set without fields.
         *
         * @since 1.0.0
         */
        public val Empty: Headers = Headers(emptyMap(), emptySet())
    }
}
