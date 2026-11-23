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

internal class Pairing(
    val baseline: SchemaView,
    val candidate: SchemaView,
    val direction: Direction,
    val depth: Int,
    val log: ChangeLog,
) {
    val label: String get() = direction.label

    val narrowed: Pair<Impact, Impact> get() = direction.narrowed

    val widened: Pair<Impact, Impact> get() = direction.widened

    fun child(baseline: SchemaView, candidate: SchemaView): Pairing =
        Pairing(baseline = baseline, candidate = candidate, direction = direction, depth = depth + 1, log = log)

    fun resolved(baseline: SchemaView, candidate: SchemaView): Pairing =
        Pairing(baseline = baseline, candidate = candidate, direction = direction, depth = depth, log = log)

    fun impactsOf(isAdded: Boolean, isRemoved: Boolean): Pair<Impact, Impact> {
        if (isAdded) return narrowed
        return widened.takeIf { isRemoved } ?: (Impact.Breaking to Impact.Breaking)
    }

    fun report(rule: String, impacts: Pair<Impact, Impact>, message: String) {
        log.report(
            rule = rule,
            impacts = impacts,
            message = message,
            baseline = baseline.location,
            candidate = candidate.location,
        )
    }

    fun incomplete(message: String) {
        log.incomplete(message = message, baseline = baseline.location, candidate = candidate.location)
    }
}
