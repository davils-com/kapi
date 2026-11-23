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
 * A Dictionary of RFC 9651 §3.2: keys with members, in order.
 *
 * Every key is a key of RFC 9651 and named once. A dictionary is immutable; [toString] writes its serialized form, in
 * which a member that is the item `?1` is written by its key and parameters alone.
 *
 * @param entries the keys with their members, in order.
 * @throws IllegalArgumentException if a key is no key or is named twice.
 * @since 1.0.0
 */
public class Dictionary(entries: List<Pair<String, Member>>) {
    /**
     * The keys with their members, in order.
     *
     * @since 1.0.0
     */
    public val entries: List<Pair<String, Member>> = entries.toList()

    init {
        val keys = entries.map { (key, _) -> key }
        require(keys.all(StructuredGrammar::isKey)) { "${keys.map(SafeText::quoted)} holds a key that is no key" }
        require(keys.toSet().size == keys.size) { "A key of the dictionary is named twice" }
    }

    /**
     * Returns the member of [key].
     *
     * @param key the key.
     * @return the member, or `null` if there is no such key.
     * @since 1.0.0
     */
    public operator fun get(key: String): Member? = entries.firstOrNull { (candidate, _) -> candidate == key }?.second

    override fun equals(other: Any?): Boolean = other is Dictionary && entries == other.entries

    override fun hashCode(): Int = entries.hashCode()

    override fun toString(): String = StructuredGrammar.dictionaryText(this)
}
