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
import com.davils.kaml.core.node.string
import com.davils.kapi.spec.openapi.read.shape.Keys

private const val FRAGMENT: Char = '#'
private const val SLASH: String = "/"
private const val ESCAPED_SLASH: String = "~1"
private const val ESCAPED_TILDE: String = "~0"
private const val TILDE: String = "~"

private val renames: List<Pair<String, String>> = listOf(
    "/${SwaggerKeys.DEFINITIONS}/" to "/${Keys.COMPONENTS}/${Keys.SCHEMAS}/",
    "/${Keys.PARAMETERS}/" to "/${Keys.COMPONENTS}/${Keys.PARAMETERS}/",
    "/${Keys.RESPONSES}/" to "/${Keys.COMPONENTS}/${Keys.RESPONSES}/",
)

internal object ReferenceUpgrade {
    fun referenceOf(node: YamlNode?): String? = node[Keys.REFERENCE].string()

    fun upgraded(reference: String): String {
        val start = reference.indexOf(FRAGMENT)
        val tail = reference.substring(start + 1)
        val fragment = tail.takeIf { start >= 0 }.orEmpty()
        val rename = renames.firstOrNull { (from, _) -> fragment.startsWith(from) } ?: return reference
        val document = reference.substring(0, start)
        return "$document$FRAGMENT${rename.second}${fragment.removePrefix(rename.first)}"
    }

    fun upgradedEntry(entry: YamlEntry): YamlEntry {
        val reference = entry.value.string() ?: return entry
        val upgraded = upgraded(reference)
        return entry.takeIf { upgraded == reference } ?: YamlEntry(entry.key, Trees.textOf(upgraded))
    }

    fun upgradedNode(node: YamlNode): YamlNode {
        val entries = Trees.entriesOf(node)
        return Trees.mappingAt(node, entries.map(::upgradedSibling))
    }

    fun localName(reference: String, section: String): String? {
        val prefix = "$FRAGMENT/$section/"
        val token = reference.removePrefix(prefix)
        val isLocal = reference.startsWith(prefix) && !token.contains(SLASH)
        val unescaped = token.replace(ESCAPED_SLASH, SLASH)
        return unescaped.replace(ESCAPED_TILDE, TILDE).takeIf { isLocal }
    }

    private fun upgradedSibling(entry: YamlEntry): YamlEntry {
        if (Trees.keyOf(entry) != Keys.REFERENCE) return entry
        return upgradedEntry(entry)
    }
}
