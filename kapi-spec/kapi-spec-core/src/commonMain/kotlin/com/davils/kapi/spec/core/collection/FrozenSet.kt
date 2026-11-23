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

internal class FrozenSet<T : Comparable<T>>(source: Iterable<T>) : AbstractSet<T>() {
    private val index: OrderedIndex<T> = OrderedIndex()

    private val items: List<T>

    init {
        val distinct = ArrayList<T>()
        source.forEach { item ->
            val ordinal = index.ordinalFor(item)
            if (ordinal == distinct.size) distinct.add(item)
        }
        items = FrozenList(distinct)
    }

    override val size: Int get() = items.size

    override fun contains(element: T): Boolean = index.ordinalOf(element) != null

    override fun iterator(): Iterator<T> = items.iterator()
}
