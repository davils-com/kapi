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

import com.davils.kapi.codegen.core.model.plan.LimitKind
import com.davils.kapi.codegen.core.model.plan.RulePlan
import com.davils.kapi.codegen.core.model.plan.ScalarKind
import com.davils.kapi.codegen.core.model.toBigDecimal
import com.davils.kapi.codegen.core.sample.plan.SampleSpan
import com.davils.kapi.runtime.core.pattern.Pattern
import com.davils.kapi.runtime.core.pattern.PatternCompilation
import com.davils.kapi.runtime.core.validation.Constraints
import java.math.BigDecimal

internal object SampleRules {
    fun text(value: String, rules: List<RulePlan>): Boolean {
        val length = value.codePointCount(beginIndex = 0, endIndex = value.length)
        val patterns = rules.filterIsInstance<RulePlan.Pattern>()
        val isMatching = patterns.all { rule -> matches(value, rule.pattern) }
        val isSized = fits(size = length, rules = rules, least = LimitKind.MinLength, most = LimitKind.MaxLength)
        return isMatching && isSized
    }

    fun number(value: BigDecimal, kind: ScalarKind, rules: List<RulePlan>): Boolean {
        val bounds = rules.filterIsInstance<RulePlan.Bound>()
        val factors = rules.filterIsInstance<RulePlan.MultipleOf>()
        val isBounded = bounds.all { rule -> isBounded(rule, value) }
        return isBounded && factors.all { rule -> isMultiple(SampleNumbers.factor(rule, kind), value) }
    }

    fun items(size: Int, rules: List<RulePlan>): Boolean =
        fits(size = size, rules = rules, least = LimitKind.MinItems, most = LimitKind.MaxItems)

    fun entries(size: Int, rules: List<RulePlan>): Boolean =
        fits(size = size, rules = rules, least = LimitKind.MinProperties, most = LimitKind.MaxProperties)

    private fun fits(size: Int, rules: List<RulePlan>, least: LimitKind, most: LimitKind): Boolean {
        val span = SampleSpan.of(rules, least, most)
        val nearest = size.coerceAtLeast(span.least)
        return nearest.coerceAtMost(span.most) == size
    }

    private fun matches(value: String, source: String): Boolean {
        val compiled = Pattern.compile(source) as? PatternCompilation.Compiled
        return compiled?.let { found -> Constraints.matches(value, found.pattern) } == true
    }

    private fun isBounded(rule: RulePlan.Bound, value: BigDecimal): Boolean {
        val order = value.compareTo(rule.value.toBigDecimal())
        val signed = order.takeIf { rule.isMinimum } ?: -order
        if (rule.isExclusive) return signed > 0
        return signed >= 0
    }

    private fun isMultiple(factor: BigDecimal, value: BigDecimal): Boolean {
        if (factor.signum() == 0) return false
        return value.remainder(factor).signum() == 0
    }
}
