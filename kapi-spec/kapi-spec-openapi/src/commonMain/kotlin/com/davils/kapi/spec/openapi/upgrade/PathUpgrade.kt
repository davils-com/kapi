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

private val methods: Set<String> = setOf(
    Keys.GET,
    Keys.PUT,
    Keys.POST,
    Keys.DELETE,
    Keys.OPTIONS,
    Keys.HEAD,
    Keys.PATCH,
)

internal class PathUpgrade(private val context: UpgradeContext) {
    private val operations: OperationUpgrade = OperationUpgrade(context)

    fun upgraded(place: Located): YamlNode {
        val item = place.node as? YamlNode.Mapping ?: return place.node
        val declared = Located.items(item[Keys.PARAMETERS], place.pointer.child(Keys.PARAMETERS))
        val (bodies, plain) = declared.partition { parameter -> context.isBody(parameter.node) }
        val parameters = Trees.sequenceOf(plain.map(context.parameters::upgraded)).takeIf { plain.isNotEmpty() }
        val others = item.entries.filterNot { entry -> Trees.keyOf(entry) == Keys.PARAMETERS }
        val entries = others.map { entry -> entryOf(entry, place, bodies) }
        return Trees.mappingAt(item, Trees.entries(Keys.PARAMETERS to parameters) + entries)
    }

    private fun entryOf(entry: YamlEntry, place: Located, bodies: List<Located>): YamlEntry {
        val key = Trees.keyOf(entry).orEmpty()
        val pointer = place.pointer.child(key).takeIf { key in methods }
        val operation = pointer?.let { found -> operations.upgraded(Located(entry.value, found), bodies) }
        val reference = ReferenceUpgrade.upgradedEntry(entry).takeIf { key == Keys.REFERENCE }
        return operation?.let { upgraded -> YamlEntry(entry.key, upgraded) } ?: reference ?: entry
    }
}
