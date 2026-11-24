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

package com.davils.kapi.spec.openapi.overlay

import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.withEntries
import com.davils.kaml.core.node.withItems

internal object OverlayMerge {
    fun merged(target: YamlNode, update: YamlNode): YamlNode? = when (target) {
        is YamlNode.Mapping -> (update as? YamlNode.Mapping)?.let { changes -> mapping(target, changes) }
        is YamlNode.Sequence -> target.withItems(target.items + update)
        is YamlNode.Scalar -> null
    }

    private fun mapping(target: YamlNode.Mapping, update: YamlNode.Mapping): YamlNode.Mapping {
        val kept = target.entries.map { entry -> changed(entry, update[entry.key]) }
        val added = update.entries.filter { change -> target[change.key] == null }
        return target.withEntries(kept + added)
    }

    private fun changed(entry: YamlEntry, change: YamlNode?): YamlEntry {
        if (change == null) return entry
        return YamlEntry(key = entry.key, value = deep(value = entry.value, change = change))
    }

    private fun deep(value: YamlNode, change: YamlNode): YamlNode {
        if (value !is YamlNode.Mapping || change !is YamlNode.Mapping) return change
        return mapping(target = value, update = change)
    }
}
