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

package com.davils.kapi.spec.asyncapi.compatibility

import com.davils.kapi.spec.asyncapi.Document
import com.davils.kapi.spec.asyncapi.model.schema.Schema
import com.davils.kapi.spec.asyncapi.model.security.SecurityScheme
import com.davils.kapi.spec.asyncapi.resolve
import com.davils.kapi.spec.core.compatibility.ChangeLog
import com.davils.kapi.spec.core.compatibility.Direction
import com.davils.kapi.spec.core.compatibility.Impact
import com.davils.kapi.spec.core.compatibility.SchemaComparison
import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.Referable
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

internal val clientBreaking: Pair<Impact, Impact> = Impact.Breaking to Impact.NonBreaking

internal val providerBreaking: Pair<Impact, Impact> = Impact.NonBreaking to Impact.Breaking

internal val bothBreaking: Pair<Impact, Impact> = Impact.Breaking to Impact.Breaking

internal val neitherBreaking: Pair<Impact, Impact> = Impact.NonBreaking to Impact.NonBreaking

internal fun shown(text: String): String = SafeText.quoted(text)

internal class Context(val baseline: Document, val candidate: Document, val log: ChangeLog) {
    private val schemas: SchemaComparison = SchemaComparison(log)

    fun report(rule: String, impacts: Pair<Impact, Impact>, message: String, before: Element?, after: Element?) {
        log.report(
            rule = rule,
            impacts = impacts,
            message = message,
            baseline = before?.location,
            candidate = after?.location,
        )
    }

    fun schemas(before: Referable<Schema>?, after: Referable<Schema>?, direction: Direction) {
        val old = resolvedIn(document = baseline, referable = before) as? Schema
        val new = resolvedIn(document = candidate, referable = after) as? Schema
        if (old == null || new == null) return
        schemas.compare(
            baseline = ValueView.of(schema = old, document = baseline),
            candidate = ValueView.of(schema = new, document = candidate),
            direction = direction,
        )
    }

    fun resolved(before: Referable<Element>?, after: Referable<Element>?): Both<Element?> = Both(
        before = resolvedIn(document = baseline, referable = before),
        after = resolvedIn(document = candidate, referable = after),
    )

    private fun resolvedIn(document: Document, referable: Referable<Element>?): Element? = when (referable) {
        is Referable.Inline -> referable.value
        is Referable.Referenced -> document.resolve(referable.reference)
        null -> null
    }

    fun security(label: String, requirements: Both<List<Referable<SecurityScheme>>>, owners: Both<Element>) {
        val isOpenBefore = requirements.before.isEmpty()
        val isOpenAfter = requirements.after.isEmpty()
        if (isOpenBefore && !isOpenAfter) {
            report(
                rule = "security-added",
                impacts = clientBreaking,
                message = "$label requires security",
                before = owners.before,
                after = owners.after,
            )
        }
        if (!isOpenBefore && isOpenAfter) {
            report(
                rule = "security-removed",
                impacts = providerBreaking,
                message = "$label no longer requires security",
                before = owners.before,
                after = owners.after,
            )
        }
    }
}
