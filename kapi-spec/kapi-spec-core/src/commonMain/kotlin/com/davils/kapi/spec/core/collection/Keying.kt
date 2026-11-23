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
 * Returns the pairs as an immutable map in their order, like `toMap`, but keyed through a [KeyedMap]; a later pair
 * replaces the value of an earlier one with the same key and keeps its place.
 *
 * @param K the type of the keys.
 * @param V the type of the values.
 * @return the immutable map.
 * @since 1.0.0
 */
@DavilsInternal
public fun <K : Comparable<K>, V : Any> Iterable<Pair<K, V>>.toKeyedMap(): Map<K, V> {
    val map = KeyedMap<K, V>()
    forEach { (key, value) -> map[key] = value }
    return map.frozen()
}

/**
 * Returns the pairs [transform] makes of the items as an immutable map in their order, like `associate`, but keyed
 * through a [KeyedMap].
 *
 * @param T the type of the items.
 * @param K the type of the keys.
 * @param V the type of the values.
 * @param transform makes the key and the value of an item.
 * @return the immutable map.
 * @since 1.0.0
 */
@DavilsInternal
public fun <T, K : Comparable<K>, V : Any> Iterable<T>.associateKeyed(transform: (T) -> Pair<K, V>): Map<K, V> =
    map(transform).toKeyedMap()

/**
 * Returns the items keyed by [key] as an immutable map in their order, like `associateBy`, but keyed through a
 * [KeyedMap]; a later item replaces an earlier one with the same key.
 *
 * @param T the type of the items.
 * @param K the type of the keys.
 * @param key names the key of an item.
 * @return the immutable map.
 * @since 1.0.0
 */
@DavilsInternal
public fun <T : Any, K : Comparable<K>> Iterable<T>.associateByKeyed(key: (T) -> K): Map<K, T> =
    associateKeyed { item -> key(item) to item }

/**
 * Returns the items grouped by [key] as an immutable map of immutable lists, in the order of the first item of each
 * group, like `groupBy`, but keyed through a [KeyedMap].
 *
 * @param T the type of the items.
 * @param K the type of the keys.
 * @param key names the group of an item.
 * @return the immutable map.
 * @since 1.0.0
 */
@DavilsInternal
public fun <T : Any, K : Comparable<K>> Iterable<T>.groupByKeyed(key: (T) -> K): Map<K, List<T>> {
    val groups = KeyedMap<K, MutableList<T>>()
    forEach { item ->
        val group = groups.getOrPut(key(item)) { mutableListOf() }
        group += item
    }
    val frozen = KeyedMap<K, List<T>>()
    groups.forEach { (name, items) -> frozen[name] = items.frozen() }
    return frozen.frozen()
}

/**
 * Returns the first item of every [key] in their order, like `distinctBy`, but keyed through a [KeyedMap].
 *
 * @param T the type of the items.
 * @param K the type of the keys.
 * @param key names the key of an item.
 * @return the immutable list of distinct items.
 * @since 1.0.0
 */
@DavilsInternal
public fun <T : Any, K : Comparable<K>> Iterable<T>.distinctByKeyed(key: (T) -> K): List<T> {
    val firsts = KeyedMap<K, T>()
    forEach { item -> firsts.getOrPut(key(item)) { item } }
    return firsts.values.frozen()
}

/**
 * Returns the entries of this map that [predicate] accepts as an immutable map in their order, like `filter`, but
 * keyed through a [KeyedMap].
 *
 * @param K the type of the keys.
 * @param V the type of the values.
 * @param predicate whether an entry is kept.
 * @return the immutable map.
 * @since 1.0.0
 */
@DavilsInternal
public fun <K : Comparable<K>, V : Any> Map<K, V>.filterKeyed(predicate: (K, V) -> Boolean): Map<K, V> {
    val kept = entries.filter { (key, value) -> predicate(key, value) }
    return kept.associateKeyed { (key, value) -> key to value }
}
