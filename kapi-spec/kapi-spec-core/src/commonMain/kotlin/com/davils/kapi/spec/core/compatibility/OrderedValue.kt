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

package com.davils.kapi.spec.core.compatibility

import com.davils.kapi.spec.core.value.Value

internal class OrderedValue(val value: Value) : Comparable<OrderedValue> {
    override fun compareTo(other: OrderedValue): Int = compare(left = value, right = other.value)

    override fun equals(other: Any?): Boolean = other is OrderedValue && value == other.value

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value.toString()

    private fun compare(left: Value, right: Value): Int {
        val kinds = left.kind.compareTo(right.kind)
        if (kinds != 0) return kinds
        return sameKind(left = left, right = right)
    }

    private fun sameKind(left: Value, right: Value): Int = when (left) {
        is Value.Null -> 0
        is Value.Flag -> left.isTrue.compareTo((right as Value.Flag).isTrue)
        is Value.Number -> left.value.compareTo((right as Value.Number).value)
        is Value.Text -> left.value.compareTo((right as Value.Text).value)
        is Value.Sequence -> sequences(left = left.items, right = (right as Value.Sequence).items)
        is Value.Mapping -> mappings(left = left.entries, right = (right as Value.Mapping).entries)
    }

    private fun sequences(left: List<Value>, right: List<Value>): Int {
        val orders = left.zip(right) { one, two -> compare(left = one, right = two) }
        val first = orders.firstOrNull { order -> order != 0 }
        return first ?: left.size.compareTo(right.size)
    }

    private fun mappings(left: Map<String, Value>, right: Map<String, Value>): Int {
        val leftKeys = left.keys.sorted()
        val rightKeys = right.keys.sorted()
        val keys = sequences(left = leftKeys.map(Value::Text), right = rightKeys.map(Value::Text))
        if (keys != 0) return keys
        return sequences(left = leftKeys.mapNotNull(left::get), right = rightKeys.mapNotNull(right::get))
    }
}
