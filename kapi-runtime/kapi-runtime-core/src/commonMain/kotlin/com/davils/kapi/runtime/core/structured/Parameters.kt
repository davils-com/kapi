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

package com.davils.kapi.runtime.core.structured

import com.davils.kapi.runtime.core.structured.internal.StructuredGrammar
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

/**
 * The parameters of an [Item] or an [InnerList], RFC 9651 §3.1.2: keys with bare items, in order.
 *
 * Every key is a key of RFC 9651: a lower-case letter or `*`, followed by lower-case letters, digits, `_`, `-`, `.`
 * and `*`; a key is named once. Parameters are immutable; [toString] writes their serialized form.
 *
 * @param entries the keys with their values, in order.
 * @throws IllegalArgumentException if a key is no key or is named twice.
 * @since 1.0.0
 */
public class Parameters(entries: List<Pair<String, BareItem>>) {
    /**
     * The keys with their values, in order.
     *
     * @since 1.0.0
     */
    public val entries: List<Pair<String, BareItem>> = entries.toList()

    init {
        val keys = entries.map { (key, _) -> key }
        require(keys.all(StructuredGrammar::isKey)) { "${keys.map(SafeText::quoted)} holds a key that is no key" }
        require(keys.toSet().size == keys.size) { "A key of the parameters is named twice" }
    }

    /**
     * Returns the value of [key].
     *
     * @param key the key.
     * @return the value, or `null` if there is no such parameter.
     * @since 1.0.0
     */
    public operator fun get(key: String): BareItem? = entries.firstOrNull { (candidate, _) -> candidate == key }?.second

    /**
     * Whether there are no parameters.
     *
     * @since 1.0.0
     */
    public val isEmpty: Boolean get() = entries.isEmpty()

    override fun equals(other: Any?): Boolean = other is Parameters && entries == other.entries

    override fun hashCode(): Int = entries.hashCode()

    override fun toString(): String = StructuredGrammar.parametersText(this)

    /**
     * The parameters without entries and the shortcut to make some.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * No parameters.
         *
         * @since 1.0.0
         */
        public val Empty: Parameters = Parameters(emptyList())

        /**
         * Makes parameters of [entries].
         *
         * @param entries the keys with their values, in order.
         * @return the parameters.
         * @throws IllegalArgumentException if a key is no key or is named twice.
         * @since 1.0.0
         */
        public fun of(vararg entries: Pair<String, BareItem>): Parameters = Parameters(entries.toList())
    }
}
