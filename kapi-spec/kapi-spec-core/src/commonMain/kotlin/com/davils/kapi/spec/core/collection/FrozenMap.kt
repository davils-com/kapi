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

package com.davils.kapi.spec.core.collection

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedIndex

internal class FrozenMap<K : Comparable<K>, V : Any>(source: Map<K, V>) : AbstractMap<K, V>() {
    private val index: OrderedIndex<K> = OrderedIndex()

    private val orderedKeys: List<K>

    private val orderedValues: List<V>

    init {
        val keys = ArrayList<K>(source.size)
        val values = ArrayList<V>(source.size)
        source.forEach { (key, value) ->
            val ordinal = index.ordinalFor(key)
            if (ordinal < keys.size) {
                values[ordinal] = value
                return@forEach
            }
            keys.add(key)
            values.add(value)
        }
        orderedKeys = keys
        orderedValues = values
    }

    override val size: Int get() = orderedKeys.size

    override val entries: Set<Map.Entry<K, V>> = Entries()

    override fun get(key: K): V? = index.ordinalOf(key)?.let(orderedValues::get)

    override fun containsKey(key: K): Boolean = index.ordinalOf(key) != null

    private inner class Entries : AbstractSet<Map.Entry<K, V>>() {
        override val size: Int get() = orderedKeys.size

        override fun contains(element: Map.Entry<K, V>): Boolean = get(element.key) == element.value

        override fun iterator(): Iterator<Map.Entry<K, V>> {
            val ordinals = orderedKeys.indices.asSequence()
            return ordinals.map(::Entry).iterator()
        }
    }

    private inner class Entry(private val ordinal: Int) : Map.Entry<K, V> {
        override val key: K get() = orderedKeys[ordinal]

        override val value: V get() = orderedValues[ordinal]

        override fun equals(other: Any?): Boolean = other is Map.Entry<*, *> && key == other.key && value == other.value

        override fun hashCode(): Int = key.hashCode() xor value.hashCode()

        override fun toString(): String = "$key=$value"
    }
}
