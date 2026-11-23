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
import com.davils.kaml.core.node.get
import com.davils.kapi.spec.openapi.read.shape.Keys

internal class SimpleSchemaUpgrade(private val styles: CollectionStyles) {
    fun schemaOf(place: Located): YamlNode? {
        val entries = Trees.only(place.node, keys)
        if (entries.isEmpty()) return null
        val items = itemsOf(place)
        return SchemaUpgrade.upgraded(Trees.mappingAt(place.node, entries.map { entry -> withItems(entry, items) }))
    }

    private fun itemsOf(place: Located): YamlNode? {
        val items = place.node[Keys.ITEMS] ?: return null
        val nested = Located(items, place.pointer.child(Keys.ITEMS))
        styles.checkNested(nested)
        return schemaOf(nested) ?: items
    }

    private fun withItems(entry: YamlEntry, items: YamlNode?): YamlEntry {
        if (items == null || Trees.keyOf(entry) != Keys.ITEMS) return entry
        return YamlEntry(entry.key, items)
    }

    companion object {
        val keys: Set<String> = setOf(
            Keys.TYPE,
            Keys.FORMAT,
            Keys.ITEMS,
            Keys.DEFAULT,
            Keys.MAXIMUM,
            Keys.EXCLUSIVE_MAXIMUM,
            Keys.MINIMUM,
            Keys.EXCLUSIVE_MINIMUM,
            Keys.MAX_LENGTH,
            Keys.MIN_LENGTH,
            Keys.PATTERN,
            Keys.MAX_ITEMS,
            Keys.MIN_ITEMS,
            Keys.UNIQUE_ITEMS,
            Keys.ENUM,
            Keys.MULTIPLE_OF,
        )
    }
}
