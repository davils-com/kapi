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


package com.davils.kapi.codegen.core.sample

internal class SampleComponents(private val edges: Map<String, List<String>>) {
    fun of(): Map<String, Int> {
        val component = mutableMapOf<String, Int>()
        val reversed = reversed()
        val roots = finished().asReversed()
        roots.forEach { root -> spread(root, reversed, component) }
        return component
    }

    private fun reversed(): Map<String, List<String>> {
        val pairs = edges.flatMap { (from, targets) -> targets.map { target -> target to from } }
        return pairs.groupBy({ (target, _) -> target }, { (_, from) -> from })
    }

    private fun spread(root: String, reversed: Map<String, List<String>>, component: MutableMap<String, Int>) {
        val id = component.size
        val pending = ArrayDeque(listOf(root))
        while (pending.isNotEmpty()) {
            val node = pending.removeLast()
            if (component.putIfAbsent(node, id) == null) pending += reversed[node].orEmpty()
        }
    }

    private fun finished(): List<String> {
        val seen = mutableSetOf<String>()
        val order = mutableListOf<String>()
        edges.keys.sorted().forEach { start -> walk(start, seen, order) }
        return order
    }

    private fun walk(start: String, seen: MutableSet<String>, order: MutableList<String>) {
        if (!seen.add(start)) return
        val stack = ArrayDeque(listOf(start to targets(start)))
        while (stack.isNotEmpty()) {
            val (node, rest) = stack.last()
            val next = rest.asSequence().firstOrNull(seen::add)
            if (next != null) stack.addLast(next to targets(next))
            if (next == null) {
                stack.removeLast()
                order += node
            }
        }
    }

    private fun targets(node: String): Iterator<String> = edges[node].orEmpty().iterator()
}
