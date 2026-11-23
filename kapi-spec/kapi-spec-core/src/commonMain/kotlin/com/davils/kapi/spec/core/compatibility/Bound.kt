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

import com.davils.kapi.spec.core.value.NumberValue
import com.davils.kore.core.annotation.DavilsInternal

internal class Bound(
    private val rule: String,
    private val keyword: String,
    private val isUpper: Boolean,
    private val read: (SchemaView) -> Pair<NumberValue?, Boolean>,
) {
    fun compare(pairing: Pairing) {
        val before = read(pairing.baseline)
        val after = read(pairing.candidate)
        val tightness = tightness(before = before, after = after)
        if (tightness == 0) return
        val impacts = pairing.narrowed.takeIf { tightness > 0 } ?: pairing.widened
        val message = "The $keyword of a schema ${pairing.label} changes from ${shown(before)} to ${shown(after)}"
        pairing.report(rule = rule, impacts = impacts, message = message)
    }

    private fun tightness(before: Pair<NumberValue?, Boolean>, after: Pair<NumberValue?, Boolean>): Int {
        val old = before.first
        val new = after.first
        if (old == null || new == null) return (new != null).compareTo(old != null)
        return ordered(old = old, new = new, exclusivity = after.second.compareTo(before.second))
    }

    private fun ordered(old: NumberValue, new: NumberValue, exclusivity: Int): Int {
        val order = old.compareTo(new)
        val values = order.takeIf { isUpper } ?: -order
        return values.takeIf { values != 0 } ?: exclusivity
    }

    private fun shown(bound: Pair<NumberValue?, Boolean>): String {
        val value = bound.first ?: return "nothing"
        val exclusive = " exclusive".takeIf { bound.second }.orEmpty()
        return "$value$exclusive"
    }
}
