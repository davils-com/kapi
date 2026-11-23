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
import com.davils.kore.core.annotation.DavilsInternal

/**
 * The specification extensions of an element, the fields whose names start with `x-`, in the order they were written.
 *
 * The typed readers, such as [flag] and [text], tell an absent extension from one that holds a value of another kind,
 * so a tool can report a mistyped extension instead of ignoring it. Extensions are immutable and safe to share
 * between threads.
 *
 * @param entries the extensions and their values, which are copied.
 * @since 1.0.0
 */
public class Extensions(entries: Map<ExtensionName, Value>) {
    /**
     * The extensions and their values.
     *
     * @since 1.0.0
     */
    public val entries: Map<ExtensionName, Value> = entries.frozen()

    /**
     * The names of the extensions.
     *
     * @since 1.0.0
     */
    public val names: Set<ExtensionName> get() = entries.keys

    /**
     * Whether the element has no extension.
     *
     * @since 1.0.0
     */
    public val isEmpty: Boolean get() = entries.isEmpty()

    /**
     * Returns the value of the extension [name].
     *
     * @param name the name of the extension.
     * @return its value, or `null` if the element has no such extension.
     * @since 1.0.0
     */
    public operator fun get(name: ExtensionName): Value? = entries[name]

    /**
     * Reads the extension [name] as `true` or `false`.
     *
     * @param name the name of the extension.
     * @return the flag, [ExtensionReading.Absent], or [ExtensionReading.Mistyped] for a value of another kind.
     * @since 1.0.0
     */
    public fun flag(name: ExtensionName): ExtensionReading<Boolean> =
        read(name, ValueKind.Flag) { value -> (value as? Value.Flag)?.isTrue }

    /**
     * Reads the extension [name] as text.
     *
     * @param name the name of the extension.
     * @return the text, [ExtensionReading.Absent], or [ExtensionReading.Mistyped] for a value of another kind.
     * @since 1.0.0
     */
    public fun text(name: ExtensionName): ExtensionReading<String> =
        read(name, ValueKind.Text) { value -> (value as? Value.Text)?.value }

    /**
     * Reads the extension [name] as a number.
     *
     * @param name the name of the extension.
     * @return the number, [ExtensionReading.Absent], or [ExtensionReading.Mistyped] for a value of another kind.
     * @since 1.0.0
     */
    public fun number(name: ExtensionName): ExtensionReading<NumberValue> =
        read(name, ValueKind.Number) { value -> (value as? Value.Number)?.value }

    /**
     * Reads the extension [name] as a list of texts.
     *
     * @param name the name of the extension.
     * @return the texts, [ExtensionReading.Absent], or [ExtensionReading.Mistyped] for a value that is no array of
     * strings only.
     * @since 1.0.0
     */
    public fun texts(name: ExtensionName): ExtensionReading<List<String>> = read(name, ValueKind.Sequence, ::textsOf)

    /**
     * Reads the extension [name] as an object.
     *
     * @param name the name of the extension.
     * @return the entries of the object, [ExtensionReading.Absent], or [ExtensionReading.Mistyped] for a value of
     * another kind.
     * @since 1.0.0
     */
    public fun mapping(name: ExtensionName): ExtensionReading<Map<String, Value>> =
        read(name, ValueKind.Mapping) { value -> (value as? Value.Mapping)?.entries }

    override fun equals(other: Any?): Boolean = other is Extensions && entries == other.entries

    override fun hashCode(): Int = entries.hashCode()

    override fun toString(): String = entries.toString()

    private fun <T : Any> read(name: ExtensionName, expected: ValueKind, convert: (Value) -> T?): ExtensionReading<T> {
        val value = entries[name] ?: return ExtensionReading.Absent
        return typed(value, expected, convert(value))
    }

    private fun <T : Any> typed(value: Value, expected: ValueKind, converted: T?): ExtensionReading<T> {
        if (converted == null) return ExtensionReading.Mistyped(value, expected)
        return ExtensionReading.Present(converted)
    }

    private fun textsOf(value: Value): List<String>? {
        val sequence = value as? Value.Sequence ?: return null
        val texts = sequence.items.mapNotNull { item -> (item as? Value.Text)?.value }
        return texts.takeIf { it.size == sequence.items.size }
    }

    /**
     * Holds the empty set of extensions.
     *
     * @since 1.0.0
     */
    public companion object {
        /**
         * No extensions.
         *
         * @since 1.0.0
         */
        public val EMPTY: Extensions = Extensions(emptyMap())
    }
}
