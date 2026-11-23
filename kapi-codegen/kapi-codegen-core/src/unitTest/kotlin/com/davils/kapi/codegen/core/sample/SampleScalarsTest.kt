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

import com.davils.kapi.spec.core.value.NumberValue
import com.davils.kapi.spec.core.value.Value
import com.davils.kapi.codegen.core.model.plan.LimitKind
import com.davils.kapi.codegen.core.model.plan.RulePlan
import com.davils.kapi.codegen.core.model.plan.ScalarKind
import com.davils.kapi.codegen.core.model.plan.TypeShape
import com.davils.kapi.codegen.core.model.plan.TypeUse
import com.davils.kapi.codegen.core.sample.plan.SampleValue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

private const val DIGITS: String = "^[0-9]{4}$"
private const val BILLIONS: Long = 3_000_000_000L
private const val LONG: Int = 8
private const val SHORT: Int = 3

private fun sample(
    kind: ScalarKind,
    rules: List<RulePlan> = emptyList(),
    hints: List<Any> = emptyList(),
): SampleValue? {
    val values = hints.map(::valueOf)
    return SampleScalars.of(kind, TypeUse(TypeShape.Scalar(kind), rules = rules, hints = values))
}

private fun valueOf(hint: Any): Value {
    val texts = listOfNotNull(hint as? String).map { text -> Value.Text(text) }
    val flags = listOfNotNull(hint as? Boolean).map { isTrue -> Value.Flag(isTrue) }
    return (texts + flags).firstOrNull() ?: Value.Number(numberOf(hint.toString()))
}

private fun numberOf(text: String): NumberValue = checkNotNull(NumberValue.parseOrNull(text))

private fun text(vararg rules: RulePlan, hints: List<Any> = emptyList()): SampleValue? =
    sample(ScalarKind.Text, rules.toList(), hints)

private fun number(kind: ScalarKind, vararg rules: RulePlan, hints: List<Any> = emptyList()): SampleValue? =
    sample(kind, rules.toList(), hints)

private fun least(value: String, isExclusive: Boolean = false): RulePlan =
    RulePlan.Bound(isMinimum = true, value = numberOf(value), isExclusive = isExclusive)

private fun most(value: String, isExclusive: Boolean = false): RulePlan =
    RulePlan.Bound(isMinimum = false, value = numberOf(value), isExclusive = isExclusive)

private fun multipleOf(value: String): RulePlan = RulePlan.MultipleOf(numberOf(value))

private fun constant(value: Any): RulePlan = RulePlan.Constant(valueOf(value))

private fun made(value: String, kind: ScalarKind): SampleValue =
    SampleValue.Number(BigDecimal(value).stripTrailingZeros(), kind)

class SampleScalarsTest : FunSpec({
    context("a text") {
        test("is the word string when nothing limits it") {
            text() shouldBe SampleValue.Text("string")
        }

        test("is padded to the least and cut to the most characters it may have") {
            text(RulePlan.Limit(LimitKind.MinLength, LONG)) shouldBe SampleValue.Text("stringgg")
            text(RulePlan.Limit(LimitKind.MaxLength, SHORT)) shouldBe SampleValue.Text("str")
        }

        test("is another spelling when the pattern refuses the first") {
            text(RulePlan.Pattern("^[A-Z]+$")) shouldBe SampleValue.Text("STRING")
            text(RulePlan.Pattern("^[0-9]$")) shouldBe SampleValue.Text("0")
        }

        test("is the first example that keeps the rules") {
            val limit = RulePlan.Limit(LimitKind.MaxLength, SHORT)

            text(limit, hints = listOf("toolong", "Rex", "Max")) shouldBe SampleValue.Text("Rex")
        }

        test("counts the characters of an example and not its code units") {
            val limit = RulePlan.Limit(LimitKind.MaxLength, 2)

            text(limit, hints = listOf("😀😀")) shouldBe SampleValue.Text("😀😀")
        }

        test("passes over an example that is shorter than it must be") {
            val limit = RulePlan.Limit(LimitKind.MinLength, SHORT)

            text(limit, hints = listOf("ab", "abc")) shouldBe SampleValue.Text("abc")
        }

        test("passes over an example of another kind") {
            text(hints = listOf(BILLIONS, true)) shouldBe SampleValue.Text("string")
        }

        test("is its constant, and nothing when the constant is no text or breaks a rule") {
            text(constant("card")) shouldBe SampleValue.Text("card")
            text(constant("card"), RulePlan.Limit(LimitKind.MaxLength, 2)) shouldBe null
            text(constant(BILLIONS)) shouldBe null
        }

        test("is nothing when no spelling matches the pattern") {
            text(RulePlan.Pattern(DIGITS)) shouldBe null
        }
    }

    context("a number") {
        test("is zero when nothing limits it") {
            val kinds = listOf(ScalarKind.Int, ScalarKind.Long, ScalarKind.Float, ScalarKind.Double)

            kinds.map { kind -> number(kind) } shouldBe kinds.map { kind -> made("0", kind) }
        }

        test("is the least it may be when zero is too little") {
            number(ScalarKind.Int, least("3")) shouldBe made("3", ScalarKind.Int)
            number(ScalarKind.Int, least("3", isExclusive = true)) shouldBe made("4", ScalarKind.Int)
            number(ScalarKind.Double, least("0", isExclusive = true)) shouldBe made("1", ScalarKind.Double)
        }

        test("is the most it may be when zero is too much") {
            number(ScalarKind.Long, most("-2")) shouldBe made("-2", ScalarKind.Long)
            number(ScalarKind.Long, most("-2", isExclusive = true)) shouldBe made("-3", ScalarKind.Long)
        }

        test("is the nearest multiple inside its bounds") {
            number(ScalarKind.Int, least("3"), multipleOf("5")) shouldBe made("5", ScalarKind.Int)
            number(ScalarKind.Int, most("-3"), multipleOf("5")) shouldBe made("-5", ScalarKind.Int)
            number(ScalarKind.Double, least("1"), multipleOf("0.5")) shouldBe made("1", ScalarKind.Double)
        }

        test("lies between two bounds that leave no whole number") {
            val low = least("0", isExclusive = true)
            val high = most("1", isExclusive = true)

            number(ScalarKind.Double, low, high) shouldBe made("0.5", ScalarKind.Double)
            number(ScalarKind.Int, low, high) shouldBe null
        }

        test("is the first example that keeps the rules") {
            number(
                ScalarKind.Int,
                most("5"),
                hints = listOf("9", "2").map(::BigDecimal),
            ) shouldBe made("2", ScalarKind.Int)
        }

        test("passes over an example its type cannot hold") {
            number(ScalarKind.Int, hints = listOf(BILLIONS, BigDecimal("1.5"), "x")) shouldBe made("0", ScalarKind.Int)
            number(ScalarKind.Long, hints = listOf(BILLIONS)) shouldBe made("3000000000", ScalarKind.Long)
        }

        test("is checked as the float the generated code holds") {
            number(ScalarKind.Float, least("0.1"), multipleOf("0.5")) shouldBe made("0.5", ScalarKind.Float)
            number(ScalarKind.Float, least("0.1"), most("0.4"), multipleOf("0.1")) shouldBe null
            number(
                ScalarKind.Double,
                least("0.1"),
                most("0.4"),
                multipleOf("0.1"),
            ) shouldBe made("0.1", ScalarKind.Double)
        }

        test("is its constant, and nothing when the constant does not fit its type") {
            number(ScalarKind.Int, constant(BigDecimal("3"))) shouldBe made("3", ScalarKind.Int)
            number(ScalarKind.Int, constant(BigDecimal("1.5"))) shouldBe null
        }

        test("is nothing when no candidate keeps every rule") {
            number(ScalarKind.Int, least("1"), most("2"), multipleOf("5")) shouldBe null
            number(ScalarKind.Int, multipleOf("0")) shouldBe null
        }
    }

    context("a flag") {
        test("is false, the example or the constant") {
            sample(ScalarKind.Flag) shouldBe SampleValue.Flag(false)
            sample(ScalarKind.Flag, hints = listOf("yes", true)) shouldBe SampleValue.Flag(true)
            sample(ScalarKind.Flag, listOf(constant(true))) shouldBe SampleValue.Flag(true)
        }
    }

    context("an instant, bytes and JSON") {
        test("are one fixed value each, whatever the examples say") {
            val hints = listOf("2020-02-02T02:02:02Z")

            sample(ScalarKind.Instant, hints = hints) shouldBe SampleValue.Moment("2026-01-01T00:00:00Z")
            sample(ScalarKind.Bytes, hints = hints) shouldBe SampleValue.Bytes
            sample(ScalarKind.Json, hints = hints) shouldBe SampleValue.Json
        }

        test("are nothing when a constant is asked of them") {
            sample(ScalarKind.Instant, listOf(constant("2020-02-02T02:02:02Z"))) shouldBe null
            sample(ScalarKind.Json, listOf(constant("x"))) shouldBe null
        }
    }
})
