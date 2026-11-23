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

package com.davils.kapi.spec.openapi.read.reference

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kaml.document.pointer.at
import com.davils.kapi.spec.core.load.DocumentLoader
import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.core.model.Target
import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.read.Cursor
import com.davils.kapi.spec.openapi.read.ReadScope
import com.davils.kore.core.annotation.DavilsInternal

internal class TargetResolver(
    private val scope: ReadScope,
    private val loader: DocumentLoader,
) {
    private val targets: MutableMap<Target, Element> = linkedMapOf()

    private val aliases: MutableMap<Target, Target> = linkedMapOf()

    private val requests: MutableMap<Target, PendingReference> = linkedMapOf()

    suspend fun resolve(): Map<Target, Element> {
        var pending = scope.references.next()
        while (pending != null) {
            follow(pending)
            pending = scope.references.next()
        }
        return targets + aliased()
    }

    private suspend fun follow(pending: PendingReference) {
        val target = pending.reference.target
        val first = requests[target]
        if (first == null) {
            requests[target] = pending
            return load(pending)
        }
        if (first.kind == pending.kind) return
        val text = pending.reference.text
        val message = "'$text' is read as ${first.kind.label} elsewhere and as ${pending.kind.label} here"
        pending.from.report(DiagnosticCodes.REFERENCE_KIND, message)
    }

    private suspend fun load(pending: PendingReference) {
        val target = pending.reference.target
        val document = loader.load(target.document, pending.from.location)
        val node = document?.run { root.at(target.pointer) }
        if (document != null && node == null) {
            val message = "'${pending.reference.text}' points at nothing in ${target.document}"
            pending.from.report(DiagnosticCodes.REFERENCE_UNRESOLVED, message)
        }
        if (document == null || node == null) return
        SelfBase.adopt(Cursor(scope = scope, document = document, pointer = JsonPointer.ROOT, node = document.root))
        val cursor = Cursor(scope = scope, document = document, pointer = target.pointer, node = node)
        keep(target, TargetReaders.read(pending.kind, cursor))
    }

    private fun keep(target: Target, read: Referable<Element>?) {
        when (read) {
            is Referable.Inline -> targets[target] = read.value
            is Referable.Referenced -> aliases[target] = read.reference.target
            null -> Unit
        }
    }

    private fun aliased(): Map<Target, Element> {
        val resolved = aliases.keys.mapNotNull { start -> end(start)?.let { element -> start to element } }
        return resolved.toMap()
    }

    private fun end(start: Target): Element? {
        val limit = scope.configuration.limits.maximumReferenceHops
        val chain = generateSequence(start) { target -> aliases[target] }
        val hops = chain.take(limit + 1).toList()
        if (hops.size > limit) {
            chained(start, hops)
            return null
        }
        return targets[hops.last()]
    }

    private fun chained(start: Target, hops: List<Target>) {
        val from = requests.getValue(start).from
        if (hops.distinct().size < hops.size) {
            return from.report(DiagnosticCodes.REFERENCE_CYCLE, "The reference leads back to itself")
        }
        val message = "The reference passes through more references than a reading follows"
        from.report(DiagnosticCodes.REFERENCE_CHAIN_TOO_LONG, message)
    }
}
