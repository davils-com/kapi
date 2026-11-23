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
import com.davils.kore.core.collection.OrderedMap

/**
 * A mutable map whose keys come from a document, kept in the order they were first put.
 *
 * Every lookup and every insertion walks the balanced tree of Kore's `OrderedMap` and costs `O(log n)` whatever the
 * keys are, so a document cannot slow it down with keys whose hash codes collide, which the hash maps of Wasm cannot
 * withstand. A key that is removed and put again keeps its first place. The map is not safe to share between threads;
 * [frozen] turns it into a map that is.
 *
 * @param K the type of the keys.
 * @param V the type of the values.
 * @since 1.0.0
 */
@DavilsInternal
public class KeyedMap<K : Comparable<K>, V : Any> : AbstractMutableMap<K, V>() {
    private val store: OrderedMap<K, V> = OrderedMap()

    override val size: Int get() = store.size

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> get() = Entries()

    override fun get(key: K): V? = store[key]

    override fun containsKey(key: K): Boolean = key in store

    override fun put(key: K, value: V): V? {
        val previous = store[key]
        store[key] = value
        return previous
    }

    override fun remove(key: K): V? = store.remove(key)

    override fun clear() {
        store.clear()
    }

    private inner class Entries : AbstractMutableSet<MutableMap.MutableEntry<K, V>>() {
        override val size: Int get() = store.size

        override fun add(element: MutableMap.MutableEntry<K, V>): Boolean {
            val previous = put(element.key, element.value)
            return previous != element.value
        }

        override fun contains(element: MutableMap.MutableEntry<K, V>): Boolean = store[element.key] == element.value

        override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> = EntryIterator(store.keys)
    }

    private inner class EntryIterator(private val keys: List<K>) : MutableIterator<MutableMap.MutableEntry<K, V>> {
        private var next: Int = 0

        override fun hasNext(): Boolean = next < keys.size

        override fun next(): MutableMap.MutableEntry<K, V> {
            if (!hasNext()) throw NoSuchElementException("The map has no further entry")
            val key = keys[next]
            next += 1
            return Entry(key)
        }

        override fun remove() {
            check(next > 0) { "No entry was returned to remove" }
            store.remove(keys[next - 1])
        }
    }

    private inner class Entry(override val key: K) : MutableMap.MutableEntry<K, V> {
        override val value: V get() = checkNotNull(store[key]) { "The entry was removed from the map" }

        override fun setValue(newValue: V): V {
            val previous = value
            store[key] = newValue
            return previous
        }

        override fun equals(other: Any?): Boolean =
            other is Map.Entry<*, *> && key == other.key && store[key] == other.value

        override fun hashCode(): Int = key.hashCode() xor value.hashCode()

        override fun toString(): String = "$key=$value"
    }
}
