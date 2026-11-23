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

/**
 * The value of an extension read as a type.
 *
 * @param T the type the value was read as.
 * @since 1.0.0
 */
public sealed interface ExtensionReading<out T> {
    /**
     * The element has no such extension.
     *
     * @since 1.0.0
     */
    public data object Absent : ExtensionReading<Nothing>

    /**
     * The extension holds a value of the type.
     *
     * @param T the type of the value.
     * @property value the value.
     * @since 1.0.0
     */
    public class Present<out T : Any>(public val value: T) : ExtensionReading<T> {
        override fun equals(other: Any?): Boolean = other is Present<*> && value == other.value

        override fun hashCode(): Int = value.hashCode()

        override fun toString(): String = "Present(value=$value)"
    }

    /**
     * The extension holds a value of another kind.
     *
     * @property found the value the extension holds.
     * @property expected the kind of value that was asked for.
     * @since 1.0.0
     */
    public class Mistyped(
        public val found: Value,
        public val expected: ValueKind,
    ) : ExtensionReading<Nothing> {
        override fun equals(other: Any?): Boolean =
            other is Mistyped && found == other.found && expected == other.expected

        override fun hashCode(): Int = 31 * found.hashCode() + expected.hashCode()

        override fun toString(): String = "Mistyped(found=${found.kind}, expected=$expected)"
    }
}
