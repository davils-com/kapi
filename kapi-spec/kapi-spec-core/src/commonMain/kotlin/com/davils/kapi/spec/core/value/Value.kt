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

package com.davils.kapi.spec.core.value

import com.davils.kapi.spec.core.collection.frozen
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

/**
 * A value written in a document, such as an example, a default, an enum entry or the value of an extension, kept as
 * it was written: numbers stay exact and mappings keep the order of their keys.
 *
 * Values compare by content and never by their [location]. They are immutable and safe to share between threads.
 * [toString] shows text through [SafeText], so a value from an untrusted document is safe to show.
 *
 * @since 1.0.0
 */
public sealed interface Value {
    /**
     * The kind of the value.
     *
     * @since 1.0.0
     */
    public val kind: ValueKind

    /**
     * Where the value was written, or `null` for a value built in code.
     *
     * @since 1.0.0
     */
    public val location: Location?

    /**
     * `null`.
     *
     * @property location where the value was written, or `null`.
     * @since 1.0.0
     */
    public class Null(override val location: Location? = null) : Value {
        override val kind: ValueKind get() = ValueKind.Null

        override fun equals(other: Any?): Boolean = other is Null

        override fun hashCode(): Int = kind.hashCode()

        override fun toString(): String = "null"
    }

    /**
     * `true` or `false`.
     *
     * @property isTrue whether the value is `true`.
     * @property location where the value was written, or `null`.
     * @since 1.0.0
     */
    public class Flag(
        public val isTrue: Boolean,
        override val location: Location? = null,
    ) : Value {
        override val kind: ValueKind get() = ValueKind.Flag

        override fun equals(other: Any?): Boolean = other is Flag && isTrue == other.isTrue

        override fun hashCode(): Int = isTrue.hashCode()

        override fun toString(): String = isTrue.toString()
    }

    /**
     * A number, kept exactly.
     *
     * @property value the number.
     * @property location where the value was written, or `null`.
     * @since 1.0.0
     */
    public class Number(
        public val value: NumberValue,
        override val location: Location? = null,
    ) : Value {
        override val kind: ValueKind get() = ValueKind.Number

        override fun equals(other: Any?): Boolean = other is Number && value == other.value

        override fun hashCode(): Int = value.hashCode()

        override fun toString(): String = value.toString()
    }

    /**
     * A string.
     *
     * @property value the text.
     * @property location where the value was written, or `null`.
     * @since 1.0.0
     */
    public class Text(
        public val value: String,
        override val location: Location? = null,
    ) : Value {
        override val kind: ValueKind get() = ValueKind.Text

        override fun equals(other: Any?): Boolean = other is Text && value == other.value

        override fun hashCode(): Int = value.hashCode()

        override fun toString(): String = SafeText.quoted(value)
    }

    /**
     * An array.
     *
     * @param items the items, which are copied.
     * @property location where the value was written, or `null`.
     * @since 1.0.0
     */
    public class Sequence(
        items: List<Value>,
        override val location: Location? = null,
    ) : Value {
        /**
         * The items in their order.
         *
         * @since 1.0.0
         */
        public val items: List<Value> = items.frozen()

        override val kind: ValueKind get() = ValueKind.Sequence

        override fun equals(other: Any?): Boolean = other is Sequence && items == other.items

        override fun hashCode(): Int = items.hashCode()

        override fun toString(): String = items.toString()
    }

    /**
     * An object.
     *
     * @param entries the entries, which are copied in their order.
     * @property location where the value was written, or `null`.
     * @since 1.0.0
     */
    public class Mapping(
        entries: Map<String, Value>,
        override val location: Location? = null,
    ) : Value {
        /**
         * The entries in the order of their keys in the document.
         *
         * @since 1.0.0
         */
        public val entries: Map<String, Value> = entries.frozen()

        override val kind: ValueKind get() = ValueKind.Mapping

        /**
         * Returns the value of [key].
         *
         * @param key the key.
         * @return the value, or `null` if the object has no such key.
         * @since 1.0.0
         */
        public operator fun get(key: String): Value? = entries[key]

        override fun equals(other: Any?): Boolean = other is Mapping && entries == other.entries

        override fun hashCode(): Int = entries.hashCode()

        override fun toString(): String = entries.entries.joinToString(prefix = "{", postfix = "}") { (key, value) ->
            "${SafeText.quoted(key)}=$value"
        }
    }
}
