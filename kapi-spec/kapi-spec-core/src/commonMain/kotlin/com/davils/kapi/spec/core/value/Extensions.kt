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

public class Extensions(entries: Map<ExtensionName, Value>) {
    public val entries: Map<ExtensionName, Value> = entries.toMap()

    public val names: Set<ExtensionName> get() = entries.keys

    public val isEmpty: Boolean get() = entries.isEmpty()

    public operator fun get(name: ExtensionName): Value? = entries[name]

    public fun flag(name: ExtensionName): ExtensionReading<Boolean> =
        read(name, ValueKind.Flag) { value -> (value as? Value.Flag)?.isTrue }

    public fun text(name: ExtensionName): ExtensionReading<String> =
        read(name, ValueKind.Text) { value -> (value as? Value.Text)?.value }

    public fun number(name: ExtensionName): ExtensionReading<NumberValue> =
        read(name, ValueKind.Number) { value -> (value as? Value.Number)?.value }

    public fun texts(name: ExtensionName): ExtensionReading<List<String>> = read(name, ValueKind.Sequence, ::textsOf)

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

    public companion object {
        public val EMPTY: Extensions = Extensions(emptyMap())
    }
}
