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

package com.davils.kapi.spec.openapi.write

import com.davils.kaml.core.node.YamlEntry
import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.yamlMapping
import com.davils.kaml.core.node.yamlScalar
import com.davils.kaml.core.node.yamlSequence
import com.davils.kapi.spec.core.model.Element

internal class Out {
    private val entries: MutableList<YamlEntry> = mutableListOf()

    fun node(key: String, node: YamlNode?) {
        entries += listOfNotNull(node).map { value -> YamlEntry(yamlScalar(key), value) }
    }

    operator fun set(key: String, value: String?) {
        node(key, value?.let { text -> yamlScalar(text) })
    }

    fun flag(key: String, isSet: Boolean) {
        node(key, yamlScalar(true).takeIf { isSet })
    }

    fun choice(key: String, isSet: Boolean?) {
        node(key, isSet?.let { value -> yamlScalar(value) })
    }

    fun count(key: String, value: Int?) {
        node(key, value?.let { count -> yamlScalar(count) })
    }

    fun texts(key: String, values: List<String>) {
        node(key, Nodes.texts(values).takeIf { values.isNotEmpty() })
    }

    fun <T> list(key: String, values: List<T>, write: (T) -> YamlNode) {
        node(key, yamlSequence(values.map(write)).takeIf { values.isNotEmpty() })
    }

    fun <T> map(key: String, values: Map<String, T>, write: (T) -> YamlNode) {
        node(key, Nodes.mapping(values, write).takeIf { values.isNotEmpty() })
    }

    fun build(element: Element): YamlNode {
        element.extensions.entries.forEach { (name, value) -> node(name.value, Nodes.of(value)) }
        return yamlMapping(entries)
    }
}
