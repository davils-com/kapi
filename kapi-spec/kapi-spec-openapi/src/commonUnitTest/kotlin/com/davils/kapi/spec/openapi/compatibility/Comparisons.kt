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

package com.davils.kapi.spec.openapi.compatibility

import com.davils.kapi.spec.core.compatibility.Change
import com.davils.kapi.spec.core.compatibility.ComparisonBuilder
import com.davils.kapi.spec.openapi.Document
import com.davils.kapi.spec.openapi.OpenApi
import com.davils.kapi.spec.openapi.fixture.readable
import com.davils.kapi.spec.openapi.fixture.reading31
import com.davils.kapi.spec.openapi.fixture.reading32

private const val PREFIX: String = "openapi.compatibility."

internal fun ruleOf(change: Change): String = change.code.value.removePrefix(PREFIX)

internal suspend fun documentOf(body: String): Document = reading31(body.lines()).readable()

internal suspend fun Pair<String, String>.changes(configure: ComparisonBuilder.() -> Unit = {}): List<Change> {
    val baseline = documentOf(first)
    val candidate = documentOf(second)
    val comparison = OpenApi.compare(baseline = baseline, candidate = candidate, configure = configure)
    return comparison.changes
}

internal suspend fun Pair<String, String>.rules(configure: ComparisonBuilder.() -> Unit = {}): List<String> =
    changes(configure).map(::ruleOf)

internal suspend fun Pair<String, String>.rules32(): List<String> {
    val baseline = reading32(first.lines()).readable()
    val candidate = reading32(second.lines()).readable()
    val comparison = OpenApi.compare(baseline = baseline, candidate = candidate)
    return comparison.changes.map(::ruleOf)
}
