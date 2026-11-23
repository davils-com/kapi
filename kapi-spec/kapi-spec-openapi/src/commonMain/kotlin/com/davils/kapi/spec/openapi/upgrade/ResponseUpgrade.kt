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

package com.davils.kapi.spec.openapi.upgrade

import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.get
import com.davils.kapi.spec.openapi.read.shape.Keys
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedSet

private val replaced: Set<String> = setOf(Keys.SCHEMA, Keys.HEADERS, Keys.EXAMPLES)

internal class ResponseUpgrade(
    private val schemas: SimpleSchemaUpgrade,
    private val styles: CollectionStyles,
) {
    fun upgraded(place: Located, produces: List<String>): YamlNode {
        if (ReferenceUpgrade.referenceOf(place.node) != null) return ReferenceUpgrade.upgradedNode(place.node)
        val kept = Trees.without(place.node, replaced)
        val declared = place.node[Keys.HEADERS]
        val headers = declared?.let { found -> headersOf(Located(found, place.pointer.child(Keys.HEADERS))) }
        return Trees.mappingAt(
            place.node,
            kept + Trees.entries(Keys.HEADERS to headers) + contentOf(place.node, produces),
        )
    }

    private fun headersOf(place: Located): YamlNode = Trees.mapped(place.node) { name, header ->
        headerOf(Located(header, place.pointer.child(name)))
    }

    private fun headerOf(place: Located): YamlNode {
        if (ReferenceUpgrade.referenceOf(place.node) != null) return ReferenceUpgrade.upgradedNode(place.node)
        val kept = Trees.without(place.node, SimpleSchemaUpgrade.keys + SwaggerKeys.COLLECTION_FORMAT)
        val schema = Trees.entries(Keys.SCHEMA to schemas.schemaOf(place))
        return Trees.mappingAt(place.node, kept + schema + styles.styleOf(place))
    }

    private fun contentOf(response: YamlNode, produces: List<String>): List<YamlEntry> {
        val schema = response[Keys.SCHEMA]?.let(SchemaUpgrade::upgraded)
        val examples = Trees.entriesOf(response[Keys.EXAMPLES])
        if (schema == null && examples.isEmpty()) return emptyList()
        val types = OrderedSet(produces + examples.mapNotNull(Trees::keyOf)).toList()
        val media = types.map { type ->
            val example = Trees.valueOf(examples, type)
            Trees.entry(type, Trees.mappingOf(Trees.entries(Keys.SCHEMA to schema, Keys.EXAMPLE to example)))
        }
        return Trees.entries(Keys.CONTENT to Trees.mappingOf(media))
    }
}
