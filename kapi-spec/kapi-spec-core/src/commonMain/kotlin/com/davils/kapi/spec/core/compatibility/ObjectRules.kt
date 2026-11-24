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

package com.davils.kapi.spec.core.compatibility

import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedSet
import com.davils.kore.core.text.SafeText

private val removal: Pair<Impact, Impact> = Impact.Breaking to Impact.NonBreaking

private val addition: Pair<Impact, Impact> = Impact.NonBreaking to Impact.NonBreaking

internal class ObjectRules(
    private val peek: (SchemaView) -> SchemaView,
    private val nested: (Pairing) -> Unit,
) {
    fun compare(pairing: Pairing) {
        val old = pairing.baseline.properties
        val new = pairing.candidate.properties
        val names = old.size + new.size + pairing.baseline.required.size + pairing.candidate.required.size
        val isWithin = pairing.log.steps(
            count = names.toLong(),
            baseline = pairing.baseline.location,
            candidate = pairing.candidate.location,
        )
        if (isWithin) properties(pairing = pairing, old = old, new = new)
    }

    private fun properties(pairing: Pairing, old: Map<String, SchemaView>, new: Map<String, SchemaView>) {
        val before = OrderedSet(pairing.baseline.required)
        val after = OrderedSet(pairing.candidate.required)
        old.forEach { (name, schema) -> if (name !in new) removed(pairing = pairing, name = name, schema = schema) }
        new.forEach { (name, schema) ->
            if (name !in old) added(pairing = pairing, name = name, schema = schema, isRequired = name in after)
        }
        old.forEach { (name, schema) ->
            val found = new[name] ?: return@forEach
            val property = Property(name = name, isRequiredBefore = name in before, isRequiredAfter = name in after)
            common(pairing = pairing, property = property, baseline = schema, candidate = found)
        }
        val undeclared = after.filterNot { name -> name in before || name in new }
        undeclared.forEach { name -> required(pairing = pairing, name = name) }
        additional(pairing)
    }

    private fun removed(pairing: Pairing, name: String, schema: SchemaView) {
        if (isIgnored(pairing.direction, schema)) return
        report(pairing = pairing, rule = "property-removed", impacts = removal, name = name, what = "removed")
    }

    private fun added(pairing: Pairing, name: String, schema: SchemaView, isRequired: Boolean) {
        if (isIgnored(pairing.direction, schema)) return
        val rule = "required-property-added".takeIf { isRequired } ?: "property-added"
        val impacts = pairing.narrowed.takeIf { isRequired } ?: addition
        val what = "added as required".takeIf { isRequired } ?: "added"
        report(pairing = pairing, rule = rule, impacts = impacts, name = name, what = what)
    }

    private fun common(pairing: Pairing, property: Property, baseline: SchemaView, candidate: SchemaView) {
        val isRelevant = !isIgnored(pairing.direction, candidate)
        val isNowRequired = property.isRequiredAfter && !property.isRequiredBefore
        val isNowOptional = property.isRequiredBefore && !property.isRequiredAfter
        if (isRelevant && isNowRequired) required(pairing = pairing, name = property.name)
        if (isRelevant && isNowOptional) {
            report(
                pairing = pairing,
                rule = "property-optional",
                impacts = pairing.widened,
                name = property.name,
                what = "optional",
            )
        }
        nested(pairing.child(baseline = baseline, candidate = candidate))
    }

    private fun required(pairing: Pairing, name: String) {
        val impacts = pairing.narrowed
        report(pairing = pairing, rule = "property-required", impacts = impacts, name = name, what = "required")
    }

    private fun additional(pairing: Pairing) {
        val before = pairing.baseline.additionalProperties
        val after = pairing.candidate.additionalProperties
        val isClosedBefore = before?.isAccepting == false
        val isClosedAfter = after?.isAccepting == false
        if (isClosedAfter && !isClosedBefore) {
            val message = "An object ${pairing.label} no longer allows properties it does not name"
            pairing.report(rule = "additional-properties", impacts = pairing.narrowed, message = message)
        }
        if (isClosedBefore && !isClosedAfter) {
            val message = "An object ${pairing.label} now allows properties it does not name"
            pairing.report(rule = "additional-properties", impacts = pairing.widened, message = message)
        }
        if (before != null && after != null) nested(pairing.child(baseline = before, candidate = after))
    }

    private fun isIgnored(direction: Direction, schema: SchemaView): Boolean {
        val resolved = peek(schema)
        return when (direction) {
            Direction.Request -> resolved.isReadOnly
            Direction.Response -> resolved.isWriteOnly
        }
    }

    private fun report(pairing: Pairing, rule: String, impacts: Pair<Impact, Impact>, name: String, what: String) {
        val message = "The property ${SafeText.quoted(name)} of an object ${pairing.label} is $what"
        pairing.report(rule = rule, impacts = impacts, message = message)
    }

    private data class Property(val name: String, val isRequiredBefore: Boolean, val isRequiredAfter: Boolean)
}
