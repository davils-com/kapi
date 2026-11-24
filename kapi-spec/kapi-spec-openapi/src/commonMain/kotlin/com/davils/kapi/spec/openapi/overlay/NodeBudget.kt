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

import com.davils.kaml.core.node.YamlNode

internal object NodeBudget {
    fun count(root: YamlNode, ceiling: Long): Long {
        val pending = ArrayDeque<YamlNode>()
        pending.addLast(root)
        var counted = 0L
        while (pending.isNotEmpty() && counted <= ceiling) {
            counted += 1
            childrenOf(pending.removeLast()).forEach(pending::addLast)
        }
        return counted
    }

    fun isWithin(root: YamlNode, maximumNodes: Long, maximumDepth: Int): Boolean {
        val pending = ArrayDeque<Pair<YamlNode, Int>>()
        pending.addLast(root to 1)
        var counted = 0L
        var isWithin = true
        while (pending.isNotEmpty() && isWithin) {
            val (node, depth) = pending.removeLast()
            counted += 1
            isWithin = counted <= maximumNodes && depth <= maximumDepth
            childrenOf(node).forEach { child -> pending.addLast(child to depth + 1) }
        }
        return isWithin
    }

    private fun childrenOf(node: YamlNode): List<YamlNode> = when (node) {
        is YamlNode.Mapping -> node.entries.flatMap { entry -> listOf(entry.key, entry.value) }
        is YamlNode.Sequence -> node.items
        is YamlNode.Scalar -> emptyList()
    }
}
