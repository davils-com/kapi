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
import com.davils.kore.core.text.SafeText

private const val NULL: String = "null"

private const val INTEGER: String = "integer"

private const val NUMBER: String = "number"

private const val ANY: String = "any type"

internal object TypeRules {
    fun compare(pairing: Pairing) {
        val before = pairing.baseline.types
        val after = pairing.candidate.types
        if (before == after) return
        val removed = before.orEmpty().filterNot { type -> isCovered(type = type, types = after) }
        val added = after.orEmpty().filterNot { type -> isCovered(type = type, types = before) }
        val isNarrowed = after != null && (before == null || removed.isNotEmpty())
        val isWidened = before != null && (after == null || added.isNotEmpty())
        val message = "The types of a schema ${pairing.label} change from ${shown(before)} to ${shown(after)}"
        val rule = ruleOf(isNarrowed = isNarrowed, isWidened = isWidened, removed = removed, added = added)
        val impacts = impactsOf(pairing = pairing, isNarrowed = isNarrowed, isWidened = isWidened)
        impacts?.let { found -> pairing.report(rule = rule, impacts = found, message = message) }
    }

    private fun ruleOf(isNarrowed: Boolean, isWidened: Boolean, removed: List<String>, added: List<String>): String {
        val isNullOnly = (removed == listOf(NULL) && !isWidened) || (added == listOf(NULL) && !isNarrowed)
        val nullable = "nullable-removed".takeIf { isNarrowed } ?: "nullable-added"
        val type = "type-narrowed".takeIf { isNarrowed } ?: "type-widened"
        val rule = nullable.takeIf { isNullOnly } ?: type
        return "type-changed".takeIf { isNarrowed && isWidened } ?: rule
    }

    private fun impactsOf(pairing: Pairing, isNarrowed: Boolean, isWidened: Boolean): Pair<Impact, Impact>? {
        val changed = Impact.Breaking to Impact.Breaking
        if (isNarrowed && isWidened) return changed
        return pairing.direction.narrowed.takeIf { isNarrowed } ?: pairing.direction.widened.takeIf { isWidened }
    }

    private fun isCovered(type: String, types: Set<String>?): Boolean =
        types == null || type in types || (type == INTEGER && NUMBER in types)

    private fun shown(types: Set<String>?): String {
        val sorted = types?.sorted() ?: return ANY
        return sorted.joinToString(prefix = "[", postfix = "]", transform = SafeText::quoted)
    }
}
