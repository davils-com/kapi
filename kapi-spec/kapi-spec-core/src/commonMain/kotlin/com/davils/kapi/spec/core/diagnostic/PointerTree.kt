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

package com.davils.kapi.spec.core.diagnostic

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedMap

/**
 * The places of a document that findings were reported at, as a tree of the tokens of their JSON Pointers, so that a
 * further check, such as the validation against an official schema, reports only where nothing was reported yet.
 *
 * Every question walks the tokens of one pointer and costs `O(d log n)` for a pointer of depth `d`, whatever the
 * tokens are. The tree is built once and not changed afterwards; it is not safe to share between threads while it is
 * built.
 *
 * @param pointers the places findings were reported at.
 * @since 1.0.0
 */
@DavilsInternal
public class PointerTree(pointers: Iterable<JsonPointer>) {
    private val root: Branch = Branch()

    init {
        pointers.forEach(::add)
    }

    /**
     * Whether a finding was reported at [pointer], above it, beside it as a sibling, or below it.
     *
     * @param pointer the place.
     * @return `true` if [pointer] is related to a place of the tree.
     * @since 1.0.0
     */
    public fun isRelated(pointer: JsonPointer): Boolean {
        val tokens = pointer.tokens
        var branch: Branch? = root
        var depth = 0
        var isRelated = false
        while (branch != null && !isRelated) {
            val current: Branch = branch
            val isAncestor = depth == tokens.size && current.children.size > 0
            val next = tokens.getOrNull(depth)?.let { token -> current.children[token] }
            val isSibling = next == null && depth == tokens.lastIndex && current.hasMarkedChild
            isRelated = current.isMarked || isAncestor || isSibling
            branch = next
            depth += 1
        }
        return isRelated
    }

    /**
     * Whether the tree holds a place below [pointer].
     *
     * @param pointer the place.
     * @return `true` if a place of the tree lies below [pointer].
     * @since 1.0.0
     */
    public fun hasBelow(pointer: JsonPointer): Boolean {
        var branch: Branch? = root
        for (token in pointer.tokens) branch = branch?.let { found -> found.children[token] }
        return branch?.let { found -> found.children.size > 0 } == true
    }

    private fun add(pointer: JsonPointer) {
        var parent: Branch? = null
        var branch = root
        for (token in pointer.tokens) {
            val current = branch
            parent = current
            branch = current.children.getOrPut(token) { Branch() }
        }
        branch.isMarked = true
        parent?.hasMarkedChild = true
    }

    private class Branch {
        val children: OrderedMap<String, Branch> = OrderedMap()

        var isMarked: Boolean = false

        var hasMarkedChild: Boolean = false
    }
}
