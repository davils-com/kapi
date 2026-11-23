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

package com.davils.kapi.spec.schema.internal.compile

import com.davils.kapi.spec.schema.DiagnosticCodes
import com.davils.kapi.spec.schema.internal.evaluate.Graph
import com.davils.kapi.spec.schema.internal.evaluate.Keyword
import com.davils.kapi.spec.schema.internal.evaluate.Node
import com.davils.kapi.spec.schema.internal.evaluate.ResourceInfo
import com.davils.kapi.spec.schema.internal.keyword.FalseKeyword
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedMap
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal class NodeFactory(private val session: Session) {
    private val slots = OrderedMap<String, Int>()

    private val nodes = ArrayList<Node?>()

    private val pending = ArrayDeque<Pair<Int, Target>>()

    private var tracksAnnotations: Boolean = false

    fun nodeAt(document: Int, pointer: List<String>): Int {
        val target = Target(document, pointer)
        val key = Session.keyOf(target)
        return slots[key] ?: allocated(key, target)
    }

    fun constant(value: Boolean, resource: Int, absolute: String): Int {
        val index = reserved(null)
        val keywords = listOf(FalseKeyword()).takeIf { !value }
        nodes[index] = Node(resource, absolute, keywords.orEmpty())
        return index
    }

    fun compileAll() {
        var handled = 0
        drain()
        while (handled < session.locations.size) {
            val entries = session.locations.entries
            entries.drop(handled).forEach { (_, entry) ->
                if (isSchema(entry)) nodeAt(entry.target.document, entry.target.pointer)
            }
            handled = entries.size
            drain()
        }
    }

    private fun isSchema(entry: LocationEntry): Boolean {
        val element = session.elementAt(entry.target)
        val isFlag = flagOf(element) != null && entry.profile.draft.allowsBooleanSchemas
        return element is JsonObject || isFlag
    }

    fun graph(): Graph {
        val compiled = nodes.map(::checkNotNull)
        val resources = session.resources.map { resource -> infoOf(resource) }
        return Graph(compiled, resources, tracksAnnotations)
    }

    private fun infoOf(resource: Resource): ResourceInfo {
        val anchors = OrderedMap<String, Int>()
        resource.dynamicAnchors.entries.forEach { (name, pointer) ->
            anchors[name] = nodeAt(
                resource.document,
                pointer,
            )
        }
        val root = nodeAt(resource.document, resource.pointer).takeIf { resource.recursiveAnchor }
        return ResourceInfo(resource.uri, anchors, root)
    }

    private fun allocated(key: String, target: Target): Int {
        val index = reserved(target)
        slots[key] = index
        pending.addLast(index to target)
        return index
    }

    private fun reserved(target: Target?): Int {
        val maximum = session.limits.maximumSubschemas
        if (nodes.size >= maximum) {
            session.abort(DiagnosticCodes.LIMIT_EXCEEDED, "The schema has more than $maximum subschemas", target)
        }
        nodes += null
        return nodes.size - 1
    }

    private fun drain() {
        while (pending.isNotEmpty()) {
            val (index, target) = pending.removeFirst()
            nodes[index] = compiled(target)
        }
    }

    private fun compiled(target: Target): Node {
        val element = checkNotNull(session.elementAt(target)) { "A subschema is compiled only where a value is" }
        val entry = session.locationOf(target) ?: detached(target)
        val keywords = keywordsOf(element, target, entry)
        tracksAnnotations = tracksAnnotations || keywords.any { it.isUnevaluated }
        return Node(entry.resource, session.absoluteOf(entry.resource, target.pointer), keywords)
    }

    private fun keywordsOf(element: JsonElement, target: Target, entry: LocationEntry): List<Keyword> {
        val members = element as? JsonObject ?: return flagKeywords(element, target, entry)
        val context = KeywordContext(this, session, target, members, entry.resource, entry.profile)
        val overrides = entry.profile.draft.refOverridesSiblings && "\$ref" in members
        val names = listOf("\$ref").takeIf { overrides } ?: members.keys
        val keywords = names.mapNotNull { name ->
            val rule = KeywordRules.ruleOf(name, entry.profile)
            rule?.compile?.invoke(context, members.getValue(name))
        }
        return keywords.sortedBy { it.isUnevaluated }
    }

    private fun flagKeywords(element: JsonElement, target: Target, entry: LocationEntry): List<Keyword> {
        val flag = flagOf(element)
        val isAllowed = flag != null && entry.profile.draft.allowsBooleanSchemas
        if (!isAllowed) {
            session.report(DiagnosticCodes.NOT_A_SCHEMA, "The value is no schema of the dialect", target)
        }
        val keywords = listOf(FalseKeyword()).takeIf { flag == false }
        return keywords.orEmpty()
    }

    private fun flagOf(element: JsonElement?): Boolean? {
        val content = (element as? JsonPrimitive)?.takeIf { !it.isString }?.content
        val isFlag = content == "true" || content == "false"
        return (content == "true").takeIf { isFlag }
    }

    private fun detached(target: Target): LocationEntry {
        val ancestor = (target.pointer.size - 1 downTo 0).firstNotNullOf { length ->
            session.locationOf(Target(target.document, target.pointer.subList(0, length)))
        }
        val element = checkNotNull(session.elementAt(target))
        val wasStrict = session.isStrict
        session.isStrict = false
        val scanned = runCatching {
            Scanner(session, session.documents[target.document]).scan(
                element = element,
                pointer = target.pointer,
                resource = ancestor.resource,
                profile = ancestor.profile,
                isRoot = false,
            )
        }
        session.isStrict = wasStrict
        val failure = scanned.exceptionOrNull()
        failure?.takeIf { it !is MetaSchemaNeeded }?.let { throw it }
        return session.locationOf(target) ?: LocationEntry(target, ancestor.resource, ancestor.profile)
    }
}
