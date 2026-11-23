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

package com.davils.kapi.spec.openapi.validation

import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.model.info.Tag

internal object TagRules {
    fun check(scope: RuleScope) {
        val tags = scope.document.tags
        val named = tags.distinctBy(Tag::name).associateBy(Tag::name)
        val unknown = tags.filter { tag -> tag.parent?.let { parent -> parent !in named } == true }
        unknown.forEach { tag ->
            val message = "The tag ${tag.name} names the parent ${tag.parent}, which is no tag of the document"
            scope.report(DiagnosticCodes.TAG_PARENT_UNKNOWN, message, tag)
        }
        val cyclic = cyclic(named)
        val looping = tags.filter { tag -> tag.parent != null && tag.name in cyclic }
        looping.forEach { tag ->
            val message = "The parents of the tag ${tag.name} lead back to it"
            scope.report(DiagnosticCodes.TAG_PARENT_CYCLE, message, tag)
        }
    }

    private fun cyclic(named: Map<String, Tag>): Set<String> {
        val done = mutableSetOf<String>()
        val cyclic = mutableSetOf<String>()
        named.keys.forEach { start -> walk(start, named, done, cyclic) }
        return cyclic
    }

    private fun walk(start: String, named: Map<String, Tag>, done: MutableSet<String>, cyclic: MutableSet<String>) {
        val path = mutableListOf<String>()
        val places = mutableMapOf<String, Int>()
        var current: String? = start
        while (current != null && current in named && current !in done && current !in places) {
            places[current] = path.size
            path += current
            current = named[current]?.parent
        }
        val loop = current?.let(places::get)
        loop?.let { index -> cyclic += path.subList(index, path.size) }
        done += path
    }
}
