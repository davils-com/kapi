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

package com.davils.kapi.spec.openapi.validation

import com.davils.kapi.spec.core.collection.KeyedMap
import com.davils.kapi.spec.core.collection.associateByKeyed
import com.davils.kapi.spec.core.collection.distinctByKeyed
import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.model.info.Tag
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedSet

internal object TagRules {
    fun check(scope: RuleScope) {
        val tags = scope.document.tags
        val named = tags.distinctByKeyed(Tag::name).associateByKeyed(Tag::name)
        tags.forEach { tag -> unknownParent(scope, tag, named) }
        val cyclic = cyclic(named)
        val looping = tags.filter { tag -> tag.parent != null && tag.name in cyclic }
        looping.forEach { tag ->
            val message = "The parents of the tag ${tag.name} lead back to it"
            scope.report(DiagnosticCodes.TAG_PARENT_CYCLE, message, tag)
        }
    }

    private fun cyclic(named: Map<String, Tag>): Set<String> {
        val done = OrderedSet<String>()
        val cyclic = OrderedSet<String>()
        named.keys.forEach { start -> walk(start = start, named = named, done = done, cyclic = cyclic) }
        return cyclic
    }

    private fun walk(start: String, named: Map<String, Tag>, done: OrderedSet<String>, cyclic: OrderedSet<String>) {
        val path = mutableListOf<String>()
        val places = KeyedMap<String, Int>()
        var current: String? = start
        while (current != null && isNext(current, named, done) && current !in places) {
            places[current] = path.size
            path += current
            current = named[current]?.parent
        }
        val loop = current?.let(places::get)
        loop?.let { index -> path.drop(index).forEach(cyclic::add) }
        path.forEach(done::add)
    }

    private fun isNext(name: String, named: Map<String, Tag>, done: Set<String>): Boolean =
        name in named && name !in done

    private fun unknownParent(scope: RuleScope, tag: Tag, named: Map<String, Tag>) {
        val parent = tag.parent ?: return
        if (parent in named) return
        val message = "The tag ${tag.name} names the parent $parent, which is no tag of the document"
        scope.report(DiagnosticCodes.TAG_PARENT_UNKNOWN, message, tag)
    }
}
