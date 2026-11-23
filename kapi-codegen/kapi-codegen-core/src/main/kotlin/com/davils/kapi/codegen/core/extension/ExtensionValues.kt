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


package com.davils.kapi.codegen.core.extension

import com.davils.kapi.spec.core.value.Value
import com.davils.kapi.codegen.core.naming.Identifiers
import kotlin.time.Duration

private const val TYPE: String = "type"

private const val STYLE: String = "style"

private val typeKeys: Set<String> = setOf(TYPE, "serializer")

private val timeoutKeys: Set<String> = setOf("request", "connect", "socket")

private val paginationKeys: Set<String> = setOf(STYLE, "parameter", "items", "next", "size")

private val paginationStyles: Set<String> = setOf("cursor", "offset", "page", "link")

private val streamKinds: Set<String> = setOf("records", "events", "bytes")

internal object ExtensionValues {
    fun isFlag(value: Value): Boolean = value is Value.Flag

    fun isIdentifier(value: Value): Boolean = Identifiers.isIdentifier(textOf(value))

    fun isLabel(value: Value): Boolean = textOf(value).isNotBlank()

    fun isTypeReference(value: Value): Boolean {
        val entries = entriesOf(value, typeKeys)
        val isMapped = entries != null && TYPE in entries && entries.values.all(::isQualified)
        return isMapped || isQualified(value)
    }

    fun isIdentifiers(value: Value): Boolean {
        val items = (value as? Value.Sequence)?.items.orEmpty()
        val names = items.map(::textOf).filter(Identifiers::isIdentifier)
        return names.isNotEmpty() && names.size == items.size && names.distinct().size == names.size
    }

    fun isTimeout(value: Value): Boolean {
        val entries = entriesOf(value, timeoutKeys)
        val isMapped = entries != null && entries.isNotEmpty() && entries.values.all(::isDuration)
        return isMapped || isDuration(value)
    }

    fun isPagination(value: Value): Boolean {
        val entries = entriesOf(value, paginationKeys).orEmpty()
        val style = entries[STYLE]?.let(::textOf).orEmpty()
        return style in paginationStyles && entries.values.all(::isLabel)
    }

    fun isStream(value: Value): Boolean = textOf(value) in streamKinds

    private fun isQualified(value: Value): Boolean = Identifiers.isQualified(textOf(value))

    private fun isDuration(value: Value): Boolean {
        val duration = Duration.parseIsoStringOrNull(textOf(value))
        return duration != null && duration.isPositive() && duration.isFinite()
    }

    private fun textOf(value: Value): String {
        val text = value as? Value.Text
        return text?.value.orEmpty()
    }

    private fun entriesOf(value: Value, keys: Set<String>): Map<String, Value>? {
        val entries = (value as? Value.Mapping)?.entries
        return entries?.takeIf { found -> keys.containsAll(found.keys) }
    }
}
