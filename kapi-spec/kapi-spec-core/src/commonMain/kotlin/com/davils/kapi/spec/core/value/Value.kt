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

package com.davils.kapi.spec.core.value

import com.davils.kapi.spec.core.diagnostic.Location

public sealed interface Value {
    public val kind: ValueKind

    public val location: Location?

    public class Null(override val location: Location? = null) : Value {
        override val kind: ValueKind get() = ValueKind.Null

        override fun equals(other: Any?): Boolean = other is Null

        override fun hashCode(): Int = kind.hashCode()

        override fun toString(): String = "null"
    }

    public class Flag(
        public val isTrue: Boolean,
        override val location: Location? = null,
    ) : Value {
        override val kind: ValueKind get() = ValueKind.Flag

        override fun equals(other: Any?): Boolean = other is Flag && isTrue == other.isTrue

        override fun hashCode(): Int = isTrue.hashCode()

        override fun toString(): String = isTrue.toString()
    }

    public class Number(
        public val value: NumberValue,
        override val location: Location? = null,
    ) : Value {
        override val kind: ValueKind get() = ValueKind.Number

        override fun equals(other: Any?): Boolean = other is Number && value == other.value

        override fun hashCode(): Int = value.hashCode()

        override fun toString(): String = value.toString()
    }

    public class Text(
        public val value: String,
        override val location: Location? = null,
    ) : Value {
        override val kind: ValueKind get() = ValueKind.Text

        override fun equals(other: Any?): Boolean = other is Text && value == other.value

        override fun hashCode(): Int = value.hashCode()

        override fun toString(): String = "\"$value\""
    }

    public class Sequence(
        items: List<Value>,
        override val location: Location? = null,
    ) : Value {
        public val items: List<Value> = items.toList()

        override val kind: ValueKind get() = ValueKind.Sequence

        override fun equals(other: Any?): Boolean = other is Sequence && items == other.items

        override fun hashCode(): Int = items.hashCode()

        override fun toString(): String = items.toString()
    }

    public class Mapping(
        entries: Map<String, Value>,
        override val location: Location? = null,
    ) : Value {
        public val entries: Map<String, Value> = entries.toMap()

        override val kind: ValueKind get() = ValueKind.Mapping

        public operator fun get(key: String): Value? = entries[key]

        override fun equals(other: Any?): Boolean = other is Mapping && entries == other.entries

        override fun hashCode(): Int = entries.hashCode()

        override fun toString(): String = entries.toString()
    }
}
