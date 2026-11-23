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
import com.davils.kapi.spec.openapi.read.shape.Keys

internal object SchemaUpgrade {
    private val nested: Set<String> = setOf(Keys.ADDITIONAL_PROPERTIES, Keys.NOT, Keys.ITEMS)

    private val lists: Set<String> = setOf(Keys.ALL_OF, Keys.ANY_OF, Keys.ONE_OF)

    private val transforms: Map<String, (YamlNode) -> YamlNode> by lazy {
        val schemas = nested.associateWith { _ -> ::schemaOrItems }
        val alternatives = lists.associateWith { _ -> ::each }
        val keywords = mapOf(
            Keys.PROPERTIES to ::properties,
            Keys.REFERENCE to ::reference,
            Keys.DISCRIMINATOR to ::discriminator,
            Keys.TYPE to ::type,
        )
        schemas + alternatives + keywords
    }

    fun upgraded(node: YamlNode): YamlNode {
        val mapping = node as? YamlNode.Mapping ?: return node
        return schemaOf(mapping)
    }

    private fun schemaOf(mapping: YamlNode.Mapping): YamlNode.Mapping {
        val entries = mapping.entries.mapNotNull(::entryOf)
        val maximum = bounded(entries, Keys.MAXIMUM, Keys.EXCLUSIVE_MAXIMUM)
        val bounded = bounded(maximum, Keys.MINIMUM, Keys.EXCLUSIVE_MINIMUM)
        val schema = Trees.mappingAt(mapping, binary(mapping, bounded))
        val isNullable = Trees.isTrue(Trees.valueOf(mapping.entries, SwaggerKeys.NULLABLE))
        return schema.takeUnless { isNullable } ?: Nullability.nullable(schema)
    }

    private fun entryOf(entry: YamlEntry): YamlEntry? {
        val key = Trees.keyOf(entry)
        val transform = key?.let(transforms::get)
        val upgraded = transform?.let { change -> YamlEntry(entry.key, change(entry.value)) } ?: entry
        return upgraded.takeUnless { key == SwaggerKeys.NULLABLE }
    }

    private fun schemaOrItems(value: YamlNode): YamlNode {
        if (value is YamlNode.Sequence) return each(value)
        return upgraded(value)
    }

    private fun each(value: YamlNode): YamlNode {
        val sequence = value as? YamlNode.Sequence ?: return value
        return Trees.sequenceAt(sequence, sequence.items.map(::upgraded))
    }

    private fun properties(value: YamlNode): YamlNode = Trees.mapped(value) { _, schema -> upgraded(schema) }

    private fun reference(value: YamlNode): YamlNode {
        val reference = value.string() ?: return value
        val upgraded = ReferenceUpgrade.upgraded(reference)
        return value.takeIf { upgraded == reference } ?: Trees.textOf(upgraded)
    }

    private fun discriminator(value: YamlNode): YamlNode {
        val property = value.string() ?: return value
        return Trees.mappingOf(Trees.entries(Keys.PROPERTY_NAME to Trees.textOf(property)))
    }

    private fun type(value: YamlNode): YamlNode {
        if (value.string() != SwaggerKeys.FILE) return value
        return Trees.textOf(SwaggerKeys.STRING)
    }

    private fun binary(original: YamlNode.Mapping, entries: List<YamlEntry>): List<YamlEntry> {
        val isFile = Trees.valueOf(original.entries, Keys.TYPE).string() == SwaggerKeys.FILE
        val hasFormat = Trees.valueOf(original.entries, Keys.FORMAT) != null
        if (!isFile || hasFormat) return entries
        return entries + Trees.entry(Keys.FORMAT, Trees.textOf(SwaggerKeys.BINARY))
    }

    private fun bounded(entries: List<YamlEntry>, limit: String, exclusive: String): List<YamlEntry> {
        val isExclusive = Trees.valueOf(entries, exclusive).boolean() ?: return entries
        val bound = Trees.valueOf(entries, limit)?.takeIf { isExclusive }
        val replacement = bound?.let { value -> Trees.entry(exclusive, value) }
        val removed = setOfNotNull(exclusive, limit.takeIf { replacement != null })
        val kept = entries.filterNot { entry -> Trees.keyOf(entry) in removed }
        return kept + listOfNotNull(replacement)
    }
}
