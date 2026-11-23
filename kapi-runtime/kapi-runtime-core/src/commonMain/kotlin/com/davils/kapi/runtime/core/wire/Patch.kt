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

package com.davils.kapi.runtime.core.wire

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText
import kotlinx.serialization.Serializable

private const val NULL: String = "null"

/**
 * A field of a partial update, such as the body of a JSON merge patch of RFC 7396, which tells three states apart: a
 * field that is left out, one that is cleared with `null` and one that is set to a value.
 *
 * A property of this type must default to [Absent] and be written by a format that leaves defaults out, such as
 * `Json { encodeDefaults = false }`; writing [Absent] fails, so a patch never clears what it meant to leave alone.
 *
 * @since 1.0.0
 */
@Serializable(with = PatchSerializer::class)
public sealed interface Patch<out T> {
    /**
     * The field is left out and keeps its value.
     *
     * @since 1.0.0
     */
    public object Absent : Patch<Nothing> {
        override fun toString(): String = "Absent"
    }

    /**
     * The field is cleared with `null`.
     *
     * @since 1.0.0
     */
    public object Null : Patch<Nothing> {
        override fun toString(): String = "Null"
    }

    /**
     * The field is set to [value].
     *
     * @param T the type of the value.
     * @property value the new value.
     * @since 1.0.0
     */
    public class Value<out T>(public val value: T) : Patch<T> {
        override fun equals(other: Any?): Boolean = other is Value<*> && value == other.value

        override fun hashCode(): Int = value.hashCode()

        override fun toString(): String = "Value(${SafeText.text(value?.toString() ?: NULL)})"
    }

    /**
     * Makes patches.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * Returns the patch that sets [value], or [Null] for `null`.
         *
         * @param T the type of the value.
         * @param value the new value, or `null` to clear the field.
         * @return the patch.
         * @since 1.0.0
         */
        public fun <T : Any> of(value: T?): Patch<T> = value?.let { set -> Value(set) } ?: Null
    }
}
