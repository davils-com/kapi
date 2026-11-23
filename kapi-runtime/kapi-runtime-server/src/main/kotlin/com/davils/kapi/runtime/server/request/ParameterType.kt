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

package com.davils.kapi.runtime.server.request

import kotlinx.serialization.DeserializationStrategy

/**
 * How the text of a parameter becomes a value: its deserializer and whether its JSON is a string.
 *
 * A string type, such as a `String` or an enumeration, reads the text as it came; any other type, such as a number or
 * a flag, reads it as a JSON literal, so `5` is a number and `"5"` is not.
 *
 * @param T the type of the value.
 * @property deserializer the deserializer of the value.
 * @property isQuoted whether the JSON of the value is a string.
 * @since 1.0.0
 */
public class ParameterType<T : Any>(
    public val deserializer: DeserializationStrategy<T>,
    public val isQuoted: Boolean,
) {
    private val parts: List<Any> get() = listOf(deserializer, isQuoted)

    override fun equals(other: Any?): Boolean = other is ParameterType<*> && parts == other.parts

    override fun hashCode(): Int = parts.hashCode()

    override fun toString(): String = "ParameterType(${deserializer.descriptor.serialName}, isQuoted=$isQuoted)"
}
