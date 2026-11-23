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

package com.davils.kapi.runtime.core.parameter.fixture

import com.davils.kapi.runtime.core.parameter.ParameterEntry
import com.davils.kapi.runtime.core.parameter.ParameterFormat
import com.davils.kapi.runtime.core.parameter.ParameterStyle
import com.davils.kapi.runtime.core.parameter.ParameterValue

internal const val COLOR: String = "color"

internal const val BLUE: String = "blue"

internal const val LISTED: String = "blue,black,brown"

internal const val FLAT: String = "R,100,G,200,B,150"

internal const val ASSIGNED: String = "R=100,G=200,B=150"

internal val text: ParameterValue = ParameterValue.Text(BLUE)

internal val items: ParameterValue = ParameterValue.Items(listOf(BLUE, "black", "brown"))

internal val fields: ParameterValue = ParameterValue.Fields(mapOf("R" to "100", "G" to "200", "B" to "150"))

internal val fieldNames: Set<String> = setOf("R", "G", "B")

internal fun format(
    style: ParameterStyle,
    isExploded: Boolean = false,
    isReservedAllowed: Boolean = false,
): ParameterFormat = ParameterFormat(COLOR, style, isExploded, isReservedAllowed)

internal fun entry(value: String, name: String = COLOR): ParameterEntry = ParameterEntry(name, value)

internal fun query(entries: List<ParameterEntry>): Map<String, List<String>> =
    entries.groupBy(ParameterEntry::name, ParameterEntry::value)
