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

package com.davils.kapi.codegen.core.emit

import com.davils.kapi.codegen.core.model.plan.LimitKind
import com.davils.kapi.codegen.core.model.plan.RulePlan
import com.davils.kapi.codegen.core.model.plan.ScalarKind
import com.davils.kapi.codegen.core.model.toBigDecimal
import com.davils.kapi.spec.core.value.NumberValue
import com.davils.kapi.spec.core.value.Value
import com.squareup.kotlinpoet.CodeBlock

private val limitCodes: Map<LimitKind, String> = mapOf(
    LimitKind.MinLength to "MIN_LENGTH",
    LimitKind.MaxLength to "MAX_LENGTH",
    LimitKind.MinItems to "MIN_ITEMS",
    LimitKind.MaxItems to "MAX_ITEMS",
    LimitKind.MinProperties to "MIN_PROPERTIES",
    LimitKind.MaxProperties to "MAX_PROPERTIES",
)

private val bounds: Map<Pair<Boolean, Boolean>, Pair<String, String>> = mapOf(
    true to true to ("<=" to "greater than"),
    true to false to ("<" to "at least"),
    false to true to (">=" to "less than"),
    false to false to (">" to "at most"),
)

private val lengths: Set<LimitKind> = setOf(LimitKind.MinLength, LimitKind.MaxLength)

internal class RuleCode(private val patterns: PatternTable) {
    fun of(rule: RulePlan, kind: ScalarKind, subject: Subject): CodeBlock = when (rule) {
        is RulePlan.Limit -> limit(rule, subject)
        is RulePlan.Bound -> bound(rule, kind, subject)
        is RulePlan.MultipleOf -> multipleOf(rule, kind, subject)
        is RulePlan.Pattern -> pattern(rule, subject)
        is RulePlan.Constant -> constant(rule, kind, subject)
        RulePlan.Unique -> unique(subject)
    }

    fun failure(condition: CodeBlock, subject: Subject, message: String, code: String): CodeBlock =
        CodeBlock.of("if (%L) fail(%L%S, %T.%L)\n", condition, subject.target, message, Kotlin.codes, code)

    private fun limit(rule: RulePlan.Limit, subject: Subject): CodeBlock {
        val length = CodeBlock.of("%T.length(%L)", Kotlin.constraints, subject.value)
        val measure = length.takeIf { rule.kind in lengths } ?: CodeBlock.of("%L.size", subject.value)
        val operator = "<".takeIf { rule.kind.isMinimum } ?: ">"
        val amount = "at least".takeIf { rule.kind.isMinimum } ?: "at most"
        return failure(
            condition = CodeBlock.of("%L $operator %L", measure, rule.value),
            subject = subject,
            message = "must have $amount ${rule.value} ${rule.kind.unit}",
            code = limitCodes.getValue(rule.kind),
        )
    }

    private fun bound(rule: RulePlan.Bound, kind: ScalarKind, subject: Subject): CodeBlock {
        val (operator, word) = bounds.getValue(rule.isMinimum to rule.isExclusive)
        return failure(
            condition = CodeBlock.of("%L $operator %L", operand(kind, rule.value, subject), literal(kind, rule.value)),
            subject = subject,
            message = "must be $word ${rule.value}",
            code = "MINIMUM".takeIf { rule.isMinimum } ?: "MAXIMUM",
        )
    }

    private fun multipleOf(rule: RulePlan.MultipleOf, kind: ScalarKind, subject: Subject): CodeBlock {
        val isWhole = isWhole(kind, rule.factor)
        val value = wholeOperand(kind, subject).takeIf { isWhole } ?: operand(kind, rule.factor, subject)
        return failure(
            condition = CodeBlock.of(
                "!%T.isMultipleOf(%L, %L)",
                Kotlin.constraints,
                value,
                literal(kind, rule.factor),
            ),
            subject = subject,
            message = "must be a multiple of ${rule.factor}",
            code = "MULTIPLE_OF",
        )
    }

    private fun constant(rule: RulePlan.Constant, kind: ScalarKind, subject: Subject): CodeBlock {
        val text = (rule.value as? Value.Text)?.value
        return failure(
            condition = unequal(rule.value, kind, subject),
            subject = subject,
            message = "must be " + (text ?: rule.value.toString()),
            code = "CONST",
        )
    }

    private fun unequal(value: Value, kind: ScalarKind, subject: Subject): CodeBlock {
        val number = value as? Value.Number
        if (number != null) return unequal(number.value, kind, subject)
        val text = value as? Value.Text
        val literal = CodeBlock.of("%S", text?.value).takeIf { text != null } ?: CodeBlock.of("%L", value)
        return CodeBlock.of("%L != %L", subject.value, literal)
    }

    private fun unequal(number: NumberValue, kind: ScalarKind, subject: Subject): CodeBlock {
        val whole = wholeOperand(kind, subject).takeIf { isWhole(kind, number) }
        return CodeBlock.of("%L != %L", whole ?: operand(kind, number, subject), literal(kind, number))
    }

    private fun wholeOperand(kind: ScalarKind, subject: Subject): CodeBlock =
        subject.value.takeIf { kind == ScalarKind.Long } ?: CodeBlock.of("%L.toLong()", subject.value)

    private fun pattern(rule: RulePlan.Pattern, subject: Subject): CodeBlock = failure(
        condition = CodeBlock.of(
            "!%T.matches(%L, %N)",
            Kotlin.constraints,
            subject.value,
            patterns.name(subject, rule.pattern),
        ),
        subject = subject,
        message = "must match ${rule.pattern}",
        code = "PATTERN",
    )

    private fun unique(subject: Subject): CodeBlock = failure(
        condition = CodeBlock.of("!%T.isUnique(%L)", Kotlin.constraints, subject.value),
        subject = subject,
        message = "must not repeat an item",
        code = "UNIQUE_ITEMS",
    )

    private fun isWhole(kind: ScalarKind, number: NumberValue): Boolean =
        kind.isIntegral && number.toLongOrNull() != null

    private fun operand(kind: ScalarKind, number: NumberValue, subject: Subject): CodeBlock {
        val isPlain = isWhole(kind, number) || kind == ScalarKind.Double
        return subject.value.takeIf { isPlain } ?: CodeBlock.of("%L.toDouble()", subject.value)
    }

    private fun literal(kind: ScalarKind, number: NumberValue): CodeBlock {
        if (!isWhole(kind, number)) return CodeBlock.of("%L", number.toDouble())
        return CodeBlock.of("%LL", number.toBigDecimal().toLong())
    }
}
