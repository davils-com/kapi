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
import com.davils.kaml.core.node.isNull
import com.davils.kaml.core.node.string
import com.davils.kaml.core.node.yamlNull
import com.davils.kapi.spec.openapi.read.shape.Keys

private val annotations: Set<String> = setOf(
    Keys.TITLE,
    Keys.DESCRIPTION,
    Keys.DEFAULT,
    Keys.EXAMPLE,
    Keys.READ_ONLY,
    Keys.WRITE_ONLY,
    Keys.DEPRECATED,
    Keys.EXTERNAL_DOCS,
    Keys.XML,
)

internal object Nullability {
    fun nullable(schema: YamlNode.Mapping): YamlNode.Mapping {
        val type = Trees.valueOf(schema.entries, Keys.TYPE).string()
        val isReference = Trees.valueOf(schema.entries, Keys.REFERENCE) != null
        if (type == null || isReference) return wrapped(schema)
        return typed(schema, type)
    }

    private fun typed(schema: YamlNode.Mapping, type: String): YamlNode.Mapping {
        val entries = schema.entries
        val types = Trees.sequenceOf(listOf(Trees.textOf(type), Trees.textOf(SwaggerKeys.NULL)))
        val enumeration = Trees.valueOf(entries, Keys.ENUM)?.let(::withNull)
        val replacements = mapOf(Keys.TYPE to types) + listOfNotNull(enumeration?.let { values -> Keys.ENUM to values })
        return Trees.mappingAt(schema, Trees.replaced(entries, replacements))
    }

    private fun withNull(values: YamlNode): YamlNode {
        val sequence = values as? YamlNode.Sequence ?: return values
        val hasNull = sequence.items.any { item -> item.isNull }
        return values.takeIf { hasNull } ?: Trees.sequenceOf(sequence.items + yamlNull())
    }

    private fun wrapped(schema: YamlNode.Mapping): YamlNode.Mapping {
        val outer = schema.entries.filter(::isAnnotation)
        val inner = schema.entries.filterNot(::isAnnotation)
        if (inner.isEmpty()) return Trees.mappingAt(schema, outer)
        val absent = Trees.mappingOf(Trees.entries(Keys.TYPE to Trees.textOf(SwaggerKeys.NULL)))
        val alternatives = Trees.sequenceOf(listOf(Trees.mappingAt(schema, inner), absent))
        return Trees.mappingAt(schema, outer + Trees.entry(Keys.ANY_OF, alternatives))
    }

    private fun isAnnotation(entry: YamlEntry): Boolean {
        val isNamed = Trees.keyOf(entry) in annotations
        return isNamed || Trees.isExtension(entry)
    }
}
