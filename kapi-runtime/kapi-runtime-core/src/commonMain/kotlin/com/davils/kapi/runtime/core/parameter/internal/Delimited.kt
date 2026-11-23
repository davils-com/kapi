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

package com.davils.kapi.runtime.core.parameter.internal

import com.davils.kapi.runtime.core.parameter.ParameterShape
import com.davils.kapi.runtime.core.parameter.ParameterValue
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedMap

private const val ASSIGN: Char = '='

internal object Delimited {
    const val COMMA: String = ","

    const val MAX_PARTS: Int = 1_024

    fun parts(
        value: ParameterValue,
        isExploded: Boolean,
        escape: (String) -> String = { text -> text },
    ): List<String> = when (value) {
        is ParameterValue.Text -> listOf(escape(value.value))
        is ParameterValue.Items -> value.values.map(escape)
        is ParameterValue.Fields -> fieldParts(value.values, isExploded, escape)
    }

    fun value(text: String, shape: ParameterShape, separator: String, isExploded: Boolean): ParameterValue? =
        when (shape) {
            ParameterShape.Text -> ParameterValue.Text(text)
            ParameterShape.Items -> split(text, separator)?.let(ParameterValue::Items)
            ParameterShape.Fields -> split(text, separator)?.let { parts -> fields(parts, isExploded) }
        }

    fun fields(parts: List<String>, isExploded: Boolean): ParameterValue.Fields? {
        val pairs = when (isExploded) {
            true -> assigned(parts)
            false -> alternating(parts)
        }
        return pairs?.let(::unique)?.let(ParameterValue::Fields)
    }

    fun unique(pairs: List<Pair<String, String>>): Map<String, String>? {
        val seen = OrderedMap<String, String>()
        pairs.forEach { (key, value) -> seen[key] = value }
        return seen.entries.toMap().takeIf { seen.size == pairs.size }
    }

    fun split(text: String, separator: String): List<String>? {
        if (text.isEmpty()) return emptyList()
        val parts = text.split(separator, limit = MAX_PARTS + 1)
        return parts.takeIf { parts.size <= MAX_PARTS }
    }

    private fun fieldParts(fields: Map<String, String>, isExploded: Boolean, escape: (String) -> String): List<String> {
        if (isExploded) return fields.map { (key, value) -> escape(key) + ASSIGN + escape(value) }
        return fields.flatMap { (key, value) -> listOf(escape(key), escape(value)) }
    }

    private fun alternating(parts: List<String>): List<Pair<String, String>>? {
        if (parts.size % 2 != 0) return null
        return parts.chunked(2) { (key, value) -> key to value }
    }

    private fun assigned(parts: List<String>): List<Pair<String, String>>? {
        if (parts.any { part -> ASSIGN !in part }) return null
        return parts.map { part -> part.substringBefore(ASSIGN) to part.substringAfter(ASSIGN) }
    }
}
