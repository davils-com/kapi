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

package com.davils.kapi.codegen.core.operation

import com.davils.kapi.codegen.core.Keywords
import com.davils.kapi.codegen.core.operation.plan.RateLimitPlan
import com.davils.kapi.spec.core.value.Value
import com.davils.kapi.spec.openapi.model.path.Operation
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

private const val REQUESTS: String = "requests"

private const val PERIOD: String = "period"

private const val PER: String = "per"

private const val CALLER: String = "caller"

private const val MOST_REQUESTS: Int = 1_000_000

private val longestPeriod: Duration = 1.days

private val names: Set<String> = setOf(REQUESTS, PERIOD, PER)

private val keys: Map<String, String> = mapOf(CALLER to "Caller", "principal" to "Principal", "address" to "Address")

internal object RateLimitPlanner {
    fun plan(operation: Operation): RateLimitPlan? = operation.extensions[Keywords.RATE_LIMIT]?.let(::parse)

    fun parse(value: Value): RateLimitPlan? {
        val entries = (value as? Value.Mapping)?.entries.orEmpty()
        val isKnown = names.containsAll(entries.keys)
        val requests = requestsOf(entries[REQUESTS]).takeIf { isKnown }
        val period = periodOf(entries[PERIOD])
        val key = keyOf(entries[PER])
        if (requests == null || period == null || key == null) return null
        return RateLimitPlan(requests = requests, period = period.toIsoString(), key = key)
    }

    private fun requestsOf(value: Value?): Int? {
        val number = value as? Value.Number ?: return null
        val count = number.value.toIntOrNull()
        return count?.takeIf { found -> found in 1..MOST_REQUESTS }
    }

    private fun periodOf(value: Value?): Duration? {
        val text = value as? Value.Text ?: return null
        val period = Duration.parseIsoStringOrNull(text.value)
        return period?.takeIf { found -> found.isPositive() && found <= longestPeriod }
    }

    private fun keyOf(value: Value?): String? {
        val text = value as? Value.Text
        val name = text?.value ?: CALLER.takeIf { value == null }
        return name?.let(keys::get)
    }
}
