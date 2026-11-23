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

private val both: Pair<Impact, Impact> = Impact.Breaking to Impact.Breaking

private val informed: Pair<Impact, Impact> = Impact.Info to Impact.Info

private const val NOTHING: String = "nothing"

internal object ScalarRules {
    private val bounds: List<Bound> = listOf(
        Bound(rule = "max-length", keyword = "maxLength", isUpper = true) { view ->
            view.maxLength to false
        },
        Bound(rule = "min-length", keyword = "minLength", isUpper = false) { view ->
            view.minLength to false
        },
        Bound(rule = "maximum", keyword = "maximum", isUpper = true) { view ->
            view.maximum to view.isMaximumExclusive
        },
        Bound(rule = "minimum", keyword = "minimum", isUpper = false) { view ->
            view.minimum to view.isMinimumExclusive
        },
        Bound(rule = "max-items", keyword = "maxItems", isUpper = true) { view ->
            view.maxItems to false
        },
        Bound(rule = "min-items", keyword = "minItems", isUpper = false) { view ->
            view.minItems to false
        },
        Bound(rule = "max-properties", keyword = "maxProperties", isUpper = true) { view ->
            view.maxProperties to false
        },
        Bound(rule = "min-properties", keyword = "minProperties", isUpper = false) { view ->
            view.minProperties to false
        },
    )

    fun compare(pairing: Pairing): Boolean {
        val shape = Shape.of(pairing)
        when (shape) {
            Shape.Opaque -> opaque(pairing)
            Shape.Fixed -> fixed(pairing)
            Shape.Keywords -> keywords(pairing)
        }
        return shape == Shape.Keywords
    }

    private fun keywords(pairing: Pairing) {
        TypeRules.compare(pairing)
        bounds.forEach { bound -> bound.compare(pairing) }
        texts(pairing = pairing, rule = "pattern", keyword = "pattern") { view -> view.pattern }
        texts(pairing = pairing, rule = "format", keyword = "format") { view -> view.format }
        texts(pairing = pairing, rule = "multiple-of", keyword = "multipleOf") { view -> view.multipleOf?.literal }
        unique(pairing)
        EnumRules.compare(pairing)
        discriminator(pairing)
        deprecated(pairing)
    }

    private fun opaque(pairing: Pairing) {
        if (pairing.baseline.opaque == pairing.candidate.opaque) return
        val message = "A schema ${pairing.label} changes in a format the comparison does not read, so nothing is " +
            "known about its compatibility"
        pairing.report(rule = "opaque", impacts = both, message = message)
    }

    private fun fixed(pairing: Pairing) {
        val before = Shape.rankOf(pairing.baseline)
        val after = Shape.rankOf(pairing.candidate)
        if (before == after) return
        val impacts = pairing.narrowed.takeIf { after < before } ?: pairing.widened
        val message = "A schema ${pairing.label} changes from ${Shape.nameOf(before)} to ${Shape.nameOf(after)}"
        pairing.report(rule = "accepts", impacts = impacts, message = message)
    }

    private fun texts(pairing: Pairing, rule: String, keyword: String, read: (SchemaView) -> String?) {
        val before = read(pairing.baseline)
        val after = read(pairing.candidate)
        if (before == after) return
        val impacts = pairing.impactsOf(isAdded = before == null, isRemoved = after == null)
        val message = "The $keyword of a schema ${pairing.label} changes from ${shown(before)} to ${shown(after)}"
        pairing.report(rule = rule, impacts = impacts, message = message)
    }

    private fun unique(pairing: Pairing) {
        val isUniqueBefore = pairing.baseline.hasUniqueItems
        val isUniqueAfter = pairing.candidate.hasUniqueItems
        if (isUniqueBefore == isUniqueAfter) return
        val impacts = pairing.narrowed.takeIf { isUniqueAfter } ?: pairing.widened
        val message = "The uniqueItems of an array ${pairing.label} changes from $isUniqueBefore to $isUniqueAfter"
        pairing.report(rule = "unique-items", impacts = impacts, message = message)
    }

    private fun discriminator(pairing: Pairing) {
        val before = pairing.baseline.discriminator
        val after = pairing.candidate.discriminator
        if (before == after) return
        val message = "The discriminator of a schema ${pairing.label} changes from ${shown(before)} to ${shown(after)}"
        pairing.report(rule = "discriminator", impacts = both, message = message)
    }

    private fun deprecated(pairing: Pairing) {
        val isNew = pairing.candidate.isDeprecated && !pairing.baseline.isDeprecated
        val message = "A schema ${pairing.label} is deprecated"
        if (isNew) pairing.report(rule = "deprecated", impacts = informed, message = message)
    }

    private fun shown(text: String?): String = text?.let(SafeText::quoted) ?: NOTHING
}
