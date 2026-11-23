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

package com.davils.kapi.spec.schema.internal.keyword

import com.davils.kapi.spec.schema.internal.evaluate.Frame
import com.davils.kapi.spec.schema.internal.evaluate.Keyword
import com.davils.kapi.spec.schema.internal.value.Canonical
import com.davils.kapi.spec.schema.internal.value.Instance

internal class UniqueItemsKeyword : Keyword("uniqueItems") {
    override fun evaluate(frame: Frame): Boolean {
        val items = (frame.instance as? Instance.ArrayValue ?: return true).items
        val keys = items.map { item -> Canonical.of(item, frame.run) }
        frame.run.charge(items.size.toLong())
        val order = keys.indices.sortedBy { index -> keys[index] }
        val duplicate = (1 until order.size).firstOrNull { position ->
            keys[order[position]] == keys[order[position - 1]]
        }
        val positions = duplicate?.let { position -> listOf(order[position - 1], order[position]).sorted() }
        return positions == null || failed(frame, "The items at ${positions.first()} and ${positions.last()} are equal")
    }
}
