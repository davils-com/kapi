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

private const val HASH_FACTOR: Int = 31

/**
 * One name and value of a query or a cookie, as a parameter writes it.
 *
 * @property name the name.
 * @property value the value.
 * @since 1.0.0
 */
public class ParameterEntry(public val name: String, public val value: String) {
    override fun equals(other: Any?): Boolean = other is ParameterEntry && name == other.name && value == other.value

    override fun hashCode(): Int = name.hashCode() * HASH_FACTOR + value.hashCode()

    override fun toString(): String = "${SafeText.text(name)}=${SafeText.text(value)}"
}
