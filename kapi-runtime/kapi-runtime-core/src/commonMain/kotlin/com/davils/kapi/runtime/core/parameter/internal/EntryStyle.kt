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

package com.davils.kapi.runtime.core.parameter.internal

import com.davils.kapi.runtime.core.parameter.ParameterEntry
import com.davils.kapi.runtime.core.parameter.ParameterFormat
import com.davils.kapi.runtime.core.parameter.ParameterReading
import com.davils.kapi.runtime.core.parameter.ParameterShape
import com.davils.kapi.runtime.core.parameter.ParameterStyle
import com.davils.kapi.runtime.core.parameter.ParameterValue

private const val OPEN: Char = '['

private const val CLOSE: Char = ']'

internal object EntryStyle {
    fun entries(format: ParameterFormat, value: ParameterValue): List<ParameterEntry> = when (value) {
        is ParameterValue.Text -> listOf(ParameterEntry(name = format.name, value = value.value))
        is ParameterValue.Items -> items(format, value.values)
        is ParameterValue.Fields -> fields(format, value)
    }

    fun read(
        format: ParameterFormat,
        entries: Map<String, List<String>>,
        shape: ParameterShape,
        names: Set<String>,
    ): ParameterReading = when (shape) {
        ParameterShape.Text -> single(entries[format.name], ParameterValue::Text)
        ParameterShape.Items -> items(format, entries)
        ParameterShape.Fields -> fields(format, entries, names)
    }

    private fun items(format: ParameterFormat, values: List<String>): List<ParameterEntry> {
        if (format.isExploded) return values.map { item -> ParameterEntry(name = format.name, value = item) }
        return joined(format, values)
    }

    private fun fields(format: ParameterFormat, value: ParameterValue.Fields): List<ParameterEntry> {
        val isDeep = format.style == ParameterStyle.DeepObject
        if (!isDeep && !format.isExploded) return joined(format, Delimited.parts(value, isExploded = false))
        return value.values.map { (key, item) -> ParameterEntry(name = keyOf(format, key, isDeep), value = item) }
    }

    private fun keyOf(format: ParameterFormat, key: String, isDeep: Boolean): String {
        if (isDeep) return format.name + OPEN + key + CLOSE
        return key
    }

    private fun joined(format: ParameterFormat, parts: List<String>): List<ParameterEntry> {
        if (parts.isEmpty()) return emptyList()
        return listOf(ParameterEntry(name = format.name, value = parts.joinToString(separator(format.style))))
    }

    private fun separator(style: ParameterStyle): String = when (style) {
        ParameterStyle.SpaceDelimited -> " "

        ParameterStyle.PipeDelimited -> "|"

        ParameterStyle.Simple,
        ParameterStyle.Label,
        ParameterStyle.Matrix,
        ParameterStyle.Form,
        ParameterStyle.DeepObject,
        ParameterStyle.Cookie,
        -> Delimited.COMMA
    }

    private fun single(values: List<String>?, read: (String) -> ParameterValue?): ParameterReading {
        if (values == null) return ParameterReading.Absent
        val value = values.singleOrNull()?.let(read)
        return value.toReading()
    }

    private fun items(format: ParameterFormat, entries: Map<String, List<String>>): ParameterReading {
        val values = entries[format.name]
        val exploded = values?.takeIf { format.isExploded }
        if (exploded != null) return explodedItems(exploded)
        return single(values) { text -> delimited(format, text, ParameterShape.Items) }
    }

    private fun explodedItems(values: List<String>): ParameterReading {
        val bounded = values.takeIf { found -> found.size <= Delimited.MAX_PARTS }
        return bounded?.let(ParameterValue::Items).toReading()
    }

    private fun fields(
        format: ParameterFormat,
        entries: Map<String, List<String>>,
        names: Set<String>,
    ): ParameterReading {
        val isDeep = format.style == ParameterStyle.DeepObject
        val isJoined = !isDeep && !format.isExploded
        if (!isJoined) return spread(members(format = format, entries = entries, names = names, isDeep = isDeep))
        return single(entries[format.name]) { text -> delimited(format, text, ParameterShape.Fields) }
    }

    private fun delimited(format: ParameterFormat, text: String, shape: ParameterShape): ParameterValue? =
        Delimited.value(text = text, shape = shape, separator = separator(format.style), isExploded = false)

    private fun members(
        format: ParameterFormat,
        entries: Map<String, List<String>>,
        names: Set<String>,
        isDeep: Boolean,
    ): Map<String, List<String>> {
        if (!isDeep) return entries.filterKeys { key -> key in names }
        val prefix = format.name + OPEN
        val deep = entries.filterKeys { key -> key.startsWith(prefix) && key.endsWith(CLOSE) }
        return deep.mapKeys { (key, _) -> key.substring(startIndex = prefix.length, endIndex = key.length - 1) }
    }

    private fun spread(members: Map<String, List<String>>): ParameterReading {
        if (members.isEmpty()) return ParameterReading.Absent
        val isPlain = members.all { (key, values) -> values.size == 1 && OPEN !in key && CLOSE !in key }
        val fields = members.takeIf { isPlain }?.mapValues { (_, values) -> values.first() }
        return fields?.let(ParameterValue::Fields).toReading()
    }
}
