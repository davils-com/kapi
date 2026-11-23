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

import com.davils.kapi.spec.core.value.Value
import com.davils.kapi.codegen.core.model.plan.LimitKind
import com.davils.kapi.codegen.core.model.plan.RulePlan
import com.davils.kapi.codegen.core.model.plan.ScalarKind
import com.davils.kapi.codegen.core.model.plan.TypeUse
import com.davils.kapi.codegen.core.sample.plan.SampleSpan
import com.davils.kapi.codegen.core.sample.plan.SampleValue
import com.davils.kapi.codegen.core.model.toBigDecimal

private const val MOMENT: String = "2026-01-01T00:00:00Z"

private val texts: List<String> = listOf("string", "STRING", "a", "A", "0")

private val fixed: Map<ScalarKind, List<SampleValue>> = mapOf(
    ScalarKind.Flag to listOf(SampleValue.Flag(false), SampleValue.Flag(true)),
    ScalarKind.Instant to listOf(SampleValue.Moment(MOMENT)),
    ScalarKind.Bytes to listOf(SampleValue.Bytes),
    ScalarKind.Json to listOf(SampleValue.Json),
)

private val converters: Map<ScalarKind, (Value) -> SampleValue?> = mapOf(
    ScalarKind.Text to { value -> (value as? Value.Text)?.let { text -> SampleValue.Text(text.value) } },
    ScalarKind.Flag to { value -> (value as? Value.Flag)?.let { flag -> SampleValue.Flag(flag.isTrue) } },
    ScalarKind.Instant to { _ -> null },
    ScalarKind.Bytes to { _ -> null },
    ScalarKind.Json to { _ -> null },
)

internal object SampleScalars {
    fun of(kind: ScalarKind, type: TypeUse): SampleValue? {
        val constants = type.rules.filterIsInstance<RulePlan.Constant>().map { rule -> converted(rule.value, kind) }
        val hinted = type.hints.mapNotNull { hint -> converted(hint, kind) }
        val candidates = constants.filterNotNull() + hinted + made(kind, type.rules)
        return candidates.firstOrNull { candidate ->
            constants.all { constant -> constant == candidate } && candidate.meets(type.rules)
        }
    }

    private fun made(kind: ScalarKind, rules: List<RulePlan>): List<SampleValue> {
        if (kind == ScalarKind.Text) return texts(rules)
        return fixed[kind] ?: SampleNumbers.candidates(kind, rules)
    }

    private fun texts(rules: List<RulePlan>): List<SampleValue> {
        val span = SampleSpan.of(rules, LimitKind.MinLength, LimitKind.MaxLength)
        val fitted = texts.map { text -> text.padEnd(span.least, text.last()).take(span.most) }
        return fitted.distinct().map(SampleValue::Text)
    }

    private fun converted(value: Value, kind: ScalarKind): SampleValue? {
        val convert = converters[kind] ?: return numbered(value, kind)
        return convert(value)
    }

    private fun numbered(value: Value, kind: ScalarKind): SampleValue? {
        val number = value as? Value.Number ?: return null
        return SampleNumbers.number(number.value.toBigDecimal(), kind)
    }
}
