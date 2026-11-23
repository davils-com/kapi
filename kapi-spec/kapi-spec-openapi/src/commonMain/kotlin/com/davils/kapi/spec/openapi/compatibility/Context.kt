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

package com.davils.kapi.spec.openapi.compatibility

import com.davils.kapi.spec.core.compatibility.ChangeLog
import com.davils.kapi.spec.core.compatibility.Direction
import com.davils.kapi.spec.core.compatibility.Impact
import com.davils.kapi.spec.core.compatibility.SchemaComparison
import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.openapi.Document
import com.davils.kapi.spec.openapi.model.schema.Schema
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.text.SafeText

internal val clientBreaking: Pair<Impact, Impact> = Impact.Breaking to Impact.NonBreaking

internal val providerBreaking: Pair<Impact, Impact> = Impact.NonBreaking to Impact.Breaking

internal val bothBreaking: Pair<Impact, Impact> = Impact.Breaking to Impact.Breaking

internal val neitherBreaking: Pair<Impact, Impact> = Impact.NonBreaking to Impact.NonBreaking

internal val informing: Pair<Impact, Impact> = Impact.Info to Impact.Info

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

    fun schemas(before: Schema?, after: Schema?, direction: Direction) {
        if (before == null || after == null) return
        schemas.compare(
            baseline = DefinitionView.viewOf(schema = before, document = baseline),
            candidate = DefinitionView.viewOf(schema = after, document = candidate),
            direction = direction,
        )
    }
}
