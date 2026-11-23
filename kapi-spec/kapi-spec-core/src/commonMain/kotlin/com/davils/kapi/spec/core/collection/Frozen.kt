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

package com.davils.kapi.spec.core.collection

import com.davils.kore.core.annotation.DavilsInternal

/**
 * Returns an immutable copy of this map that keeps the order of its entries.
 *
 * The copy looks its keys up through a balanced tree in `O(log n)` whatever the keys are, so keys of a document whose
 * hash codes collide cannot slow it down, which the hash maps of Wasm cannot withstand. It cannot be changed through a
 * cast: on the JVM every mutator throws [UnsupportedOperationException], on Wasm the cast fails. A map this function
 * returned is returned as it is. The copy is safe to share between threads.
 *
 * @param K the type of the keys.
 * @param V the type of the values.
 * @return the immutable copy.
 * @since 1.0.0
 */
@DavilsInternal
public fun <K : Comparable<K>, V : Any> Map<K, V>.frozen(): Map<K, V> {
    if (this is FrozenMap<K, V>) return this
    return FrozenMap(this)
}

/**
 * Returns an immutable copy of this collection as a list in its order of iteration.
 *
 * The copy cannot be changed through a cast: on the JVM every mutator throws [UnsupportedOperationException], on Wasm
 * the cast fails. A list this function returned is returned as it is. The copy is safe to share between threads.
 *
 * @param T the type of the items.
 * @return the immutable copy.
 * @since 1.0.0
 */
@DavilsInternal
public fun <T> Collection<T>.frozen(): List<T> {
    if (this is FrozenList<T>) return this
    return FrozenList(this)
}

/**
 * Returns an immutable set of the distinct items of this collection in their order of iteration.
 *
 * The set finds an item through a balanced tree in `O(log n)` whatever the items are. It cannot be changed through a
 * cast: on the JVM every mutator throws [UnsupportedOperationException], on Wasm the cast fails. A set this function
 * returned is returned as it is. The set is safe to share between threads.
 *
 * @param T the type of the items.
 * @return the immutable set.
 * @since 1.0.0
 */
@DavilsInternal
public fun <T : Comparable<T>> Iterable<T>.frozenSet(): Set<T> {
    if (this is FrozenSet<T>) return this
    return FrozenSet(this)
}
