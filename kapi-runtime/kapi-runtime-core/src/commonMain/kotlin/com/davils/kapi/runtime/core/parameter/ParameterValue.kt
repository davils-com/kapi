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

package com.davils.kapi.runtime.core.parameter

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

/**
 * The value of a parameter before it is written or after it is read, in one of the three [ParameterShape]s.
 *
 * Values are immutable. [toString] shows their texts escaped.
 *
 * @since 1.0.0
 */
public sealed interface ParameterValue {
    /**
     * A single value as text.
     *
     * @property value the text.
     * @since 1.0.0
     */
    public class Text(public val value: String) : ParameterValue {
        override fun equals(other: Any?): Boolean = other is Text && value == other.value

        override fun hashCode(): Int = value.hashCode()

        override fun toString(): String = "Text(${SafeText.text(value)})"
    }

    /**
     * An array of values as texts.
     *
     * @param values the texts in order.
     * @since 1.0.0
     */
    public class Items(values: List<String>) : ParameterValue {
        /**
         * The texts in order.
         *
         * @since 1.0.0
         */
        public val values: List<String> = values.toList()

        override fun equals(other: Any?): Boolean = other is Items && values == other.values

        override fun hashCode(): Int = values.hashCode()

        override fun toString(): String = "Items(${values.map(SafeText::text)})"
    }

    /**
     * An object of named values as texts.
     *
     * @param values the names with their texts, in order.
     * @since 1.0.0
     */
    public class Fields(values: Map<String, String>) : ParameterValue {
        /**
         * The names with their texts, in order.
         *
         * @since 1.0.0
         */
        public val values: Map<String, String> = values.toMap()

        override fun equals(other: Any?): Boolean = other is Fields && values == other.values

        override fun hashCode(): Int = values.hashCode()

        override fun toString(): String =
            "Fields(${values.entries.map { (name, value) -> SafeText.text(name) + "=" + SafeText.text(value) }})"
    }
}
