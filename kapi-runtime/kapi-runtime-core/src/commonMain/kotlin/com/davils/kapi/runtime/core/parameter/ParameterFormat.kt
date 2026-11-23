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
 * How a parameter of an operation is written and read: its name, its [ParameterStyle], `explode` and `allowReserved`
 * of OpenAPI 3.2 §4.12.
 *
 * @property name the name of the parameter.
 * @property style the style.
 * @property isExploded whether `explode` is set, which writes every item or field on its own.
 * @property isReservedAllowed whether `allowReserved` is set, which leaves the reserved characters of RFC 3986 in a
 * query value as they are, except those that would end the value: `#`, `&`, `=` and `+`.
 * @since 1.0.0
 */
public class ParameterFormat(
    public val name: String,
    public val style: ParameterStyle,
    public val isExploded: Boolean,
    public val isReservedAllowed: Boolean = false,
) {
    override fun equals(other: Any?): Boolean = other is ParameterFormat &&
        name == other.name &&
        style == other.style &&
        isExploded == other.isExploded &&
        isReservedAllowed == other.isReservedAllowed

    override fun hashCode(): Int = listOf(name, style, isExploded, isReservedAllowed).hashCode()

    override fun toString(): String = "ParameterFormat(name=${SafeText.text(name)}, style=$style, " +
        "isExploded=$isExploded, isReservedAllowed=$isReservedAllowed)"
}
