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

package com.davils.kapi.codegen.core.sample

import com.davils.kapi.codegen.core.model.plan.RulePlan
import com.davils.kapi.codegen.core.model.plan.ScalarKind
import com.davils.kapi.codegen.core.model.toBigDecimal
import com.davils.kapi.codegen.core.sample.plan.SampleValue
import java.math.BigDecimal
import java.math.RoundingMode

private val two: BigDecimal = BigDecimal(2)

private val ranges: Map<ScalarKind, ClosedRange<BigDecimal>> = mapOf(
    ScalarKind.Int to BigDecimal(Int.MIN_VALUE)..BigDecimal(Int.MAX_VALUE),
    ScalarKind.Long to BigDecimal(Long.MIN_VALUE)..BigDecimal(Long.MAX_VALUE),
    ScalarKind.UnsignedLong to BigDecimal.ZERO..BigDecimal(ULong.MAX_VALUE.toString()),
)

private val seen: Map<ScalarKind, (BigDecimal) -> BigDecimal> = mapOf(
    ScalarKind.Float to { value -> decimal(value.toFloat().toDouble()) },
    ScalarKind.Double to { value -> decimal(value.toDouble()) },
)

private fun decimal(value: Double): BigDecimal = BigDecimal(value.toString())

internal object SampleNumbers {
    fun number(value: BigDecimal, kind: ScalarKind): SampleValue.Number? {
        val plain = value.stripTrailingZeros()
        val range = ranges[kind] ?: return SampleValue.Number(plain, kind)
        val isWhole = plain.scale() <= 0 && plain in range
        return SampleValue.Number(plain, kind).takeIf { isWhole }
    }

    fun seen(value: BigDecimal, kind: ScalarKind): BigDecimal {
        val read = seen[kind] ?: return value
        return read(value)
    }

    fun factor(rule: RulePlan.MultipleOf, kind: ScalarKind): BigDecimal {
        val exact = rule.factor.toBigDecimal()
        if (kind.isIntegral) return exact
        return decimal(exact.toDouble())
    }

    fun candidates(kind: ScalarKind, rules: List<RulePlan>): List<SampleValue> {
        val bounds = rules.filterIsInstance<RulePlan.Bound>()
        val lows = bounds.filter(RulePlan.Bound::isMinimum).map { bound -> bound.value.toBigDecimal() }
        val highs = bounds.filterNot(RulePlan.Bound::isMinimum).map { bound -> bound.value.toBigDecimal() }
        val factors = rules.filterIsInstance<RulePlan.MultipleOf>().map { rule -> rule.factor.toBigDecimal().abs() }
        val step = factors.firstOrNull { factor -> factor.signum() > 0 } ?: BigDecimal.ONE
        val middles = lows.flatMap { low -> highs.map { high -> (low + high).divide(two) } }
        val anchors = listOf(BigDecimal.ZERO) + lows + highs + middles
        val all = anchors + anchors.flatMap { anchor -> around(anchor, step) }
        return all.mapNotNull { value -> number(value, kind) }.distinct()
    }

    private fun around(anchor: BigDecimal, step: BigDecimal): List<BigDecimal> {
        val multiple = anchor.divide(step, 0, RoundingMode.FLOOR) * step
        return listOf(multiple, multiple + step, multiple - step, anchor + step, anchor - step)
    }
}
