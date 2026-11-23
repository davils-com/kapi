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

package com.davils.kapi.spec.schema.internal.value

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedIndex

internal sealed interface Instance {
    val type: String

    object NullValue : Instance {
        override val type: String get() = "null"
    }

    class BooleanValue(val value: Boolean) : Instance {
        override val type: String get() = "boolean"
    }

    class NumberValue(val value: Decimal) : Instance {
        override val type: String get() = "integer".takeIf { value.isInteger } ?: "number"
    }

    class StringValue(val value: String) : Instance {
        override val type: String get() = "string"
    }

    class ArrayValue(val items: List<Instance>) : Instance {
        override val type: String get() = "array"
    }

    class ObjectValue(val keys: List<String>, val values: List<Instance>) : Instance {
        private var index: OrderedIndex<String>? = null

        override val type: String get() = "object"

        fun indexOf(key: String): Int? {
            val existing = index ?: indexed()
            return existing.ordinalOf(key)
        }

        private fun indexed(): OrderedIndex<String> {
            val created = OrderedIndex<String>()
            keys.forEach { key -> created.ordinalFor(key) }
            index = created
            return created
        }
    }
}
