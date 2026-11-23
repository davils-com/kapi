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

package com.davils.kapi.spec.openapi.upgrade

import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.boolean
import com.davils.kaml.core.node.string
import com.davils.kaml.core.node.withEntries
import com.davils.kaml.core.node.withItems
import com.davils.kaml.core.node.yamlMapping
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.core.node.yamlSequence

internal object Trees {
    fun keyOf(entry: YamlEntry): String? = entry.key.string()

    fun entry(key: String, value: YamlNode): YamlEntry = YamlEntry(yamlScalar(key), value)

    fun entries(vararg parts: Pair<String, YamlNode?>): List<YamlEntry> =
        parts.mapNotNull { (key, value) -> value?.let { present -> entry(key, present) } }

    fun mappingOf(entries: List<YamlEntry>): YamlNode.Mapping = yamlMapping(entries)

    fun mappingOrNull(entries: List<YamlEntry>): YamlNode.Mapping? {
        if (entries.isEmpty()) return null
        return mappingOf(entries)
    }

    fun sequenceOf(items: List<YamlNode>): YamlNode.Sequence = yamlSequence(items)

    fun mappingAt(origin: YamlNode?, entries: List<YamlEntry>): YamlNode.Mapping {
        val mapping = origin as? YamlNode.Mapping ?: return mappingOf(entries)
        return mapping.withEntries(entries)
    }

    fun sequenceAt(origin: YamlNode.Sequence, items: List<YamlNode>): YamlNode.Sequence = origin.withItems(items)

    fun textOf(value: String): YamlNode.Scalar = yamlScalar(value)

    fun entriesOf(node: YamlNode?): List<YamlEntry> {
        val mapping = node as? YamlNode.Mapping ?: return emptyList()
        return mapping.entries
    }

    fun only(node: YamlNode?, keys: Set<String>): List<YamlEntry> =
        entriesOf(node).filter { entry -> keyOf(entry) in keys }

    fun without(node: YamlNode?, keys: Set<String>): List<YamlEntry> =
        entriesOf(node).filterNot { entry -> keyOf(entry) in keys }

    fun extensions(node: YamlNode?): List<YamlEntry> = entriesOf(node).filter(::isExtension)

    fun isExtension(entry: YamlEntry): Boolean {
        val key = keyOf(entry) ?: return false
        return key.startsWith(SwaggerKeys.EXTENSION_PREFIX)
    }

    fun texts(node: YamlNode?): List<String>? {
        val sequence = node as? YamlNode.Sequence ?: return null
        return sequence.items.mapNotNull { item -> item.string() }
    }

    fun isTrue(node: YamlNode?): Boolean = node.boolean() == true

    fun valueOf(entries: List<YamlEntry>, key: String): YamlNode? =
        entries.firstOrNull { entry -> keyOf(entry) == key }?.value

    fun replaced(entries: List<YamlEntry>, replacements: Map<String, YamlNode>): List<YamlEntry> =
        entries.map { entry -> replacedEntry(entry, replacements) }

    fun mapped(node: YamlNode?, transform: (String, YamlNode) -> YamlNode): YamlNode.Mapping =
        mappingAt(node, entriesOf(node).map { entry -> transformed(entry, transform) })

    private fun replacedEntry(entry: YamlEntry, replacements: Map<String, YamlNode>): YamlEntry {
        val replacement = keyOf(entry)?.let(replacements::get) ?: return entry
        return YamlEntry(entry.key, replacement)
    }

    private fun transformed(entry: YamlEntry, transform: (String, YamlNode) -> YamlNode): YamlEntry {
        val key = keyOf(entry)?.takeUnless { isExtension(entry) } ?: return entry
        return YamlEntry(entry.key, transform(key, entry.value))
    }
}
