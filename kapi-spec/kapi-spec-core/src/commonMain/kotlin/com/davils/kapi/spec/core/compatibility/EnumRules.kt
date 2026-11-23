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

import com.davils.kapi.spec.core.value.Value
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.collection.OrderedSet

private const val SHOWN: Int = 5

private const val NOTHING: String = "nothing"

internal object EnumRules {
    fun compare(pairing: Pairing) {
        constant(pairing)
        val before = pairing.baseline.enumeration
        val after = pairing.candidate.enumeration
        if (before == null || after == null) return presence(pairing = pairing, before = before, after = after)
        val count = (before.size + after.size).toLong()
        val log = pairing.log
        val baseline = pairing.baseline.location
        val isWithin = log.steps(count = count, baseline = baseline, candidate = pairing.candidate.location)
        if (isWithin) values(pairing = pairing, before = before, after = after)
    }

    private fun presence(pairing: Pairing, before: List<Value>?, after: List<Value>?) {
        if (after != null) {
            val message = "An enum of ${after.size} values restricts a schema ${pairing.label}"
            pairing.report(rule = "enum", impacts = pairing.narrowed, message = message)
        }
        if (before != null) {
            val message = "The enum of ${before.size} values of a schema ${pairing.label} is removed"
            pairing.report(rule = "enum", impacts = pairing.widened, message = message)
        }
    }

    private fun values(pairing: Pairing, before: List<Value>, after: List<Value>) {
        val old = OrderedSet(before.map(::OrderedValue))
        val new = OrderedSet(after.map(::OrderedValue))
        val removed = old.filterNot { value -> value in new }
        val added = new.filterNot { value -> value in old }
        if (removed.isNotEmpty()) {
            val message = "The enum of a schema ${pairing.label} no longer allows ${shown(removed)}"
            pairing.report(rule = "enum-value-removed", impacts = pairing.narrowed, message = message)
        }
        if (added.isNotEmpty()) {
            val message = "The enum of a schema ${pairing.label} now also allows ${shown(added)}"
            pairing.report(rule = "enum-value-added", impacts = pairing.widened, message = message)
        }
    }

    private fun constant(pairing: Pairing) {
        val before = pairing.baseline.constant
        val after = pairing.candidate.constant
        if (before == after) return
        val impacts = pairing.impactsOf(isAdded = before == null, isRemoved = after == null)
        val message = "The const of a schema ${pairing.label} changes from ${before ?: NOTHING} to ${after ?: NOTHING}"
        pairing.report(rule = "const", impacts = impacts, message = message)
    }

    private fun shown(values: List<OrderedValue>): String {
        val more = values.size - SHOWN
        val rest = " and $more more".takeIf { more > 0 }.orEmpty()
        return values.take(SHOWN).joinToString() + rest
    }
}
