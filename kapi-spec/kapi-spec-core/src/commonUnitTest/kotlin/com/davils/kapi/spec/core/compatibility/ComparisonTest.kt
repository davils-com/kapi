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

import com.davils.kapi.spec.core.policy.ReadLimits
import com.davils.kapi.spec.core.value.NumberValue
import com.davils.kapi.spec.core.value.Value
import com.davils.kore.core.annotation.DavilsInternal
import com.davils.kore.core.validation.violation.ViolationCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

private const val RULE: String = "path-removed"

private const val TWO_CHANGES: Int = 2

private val breaking: Pair<Impact, Impact> = Impact.Breaking to Impact.NonBreaking

private val removed: Change = Change(
    code = ViolationCode("openapi.compatibility.path-removed"),
    client = Impact.Breaking,
    provider = Impact.NonBreaking,
    message = "The path \"/pets\" is removed",
    baseline = at("/paths/~1pets"),
)

private val added: Change = Change(
    code = ViolationCode("openapi.compatibility.path-added"),
    client = Impact.NonBreaking,
    provider = Impact.Breaking,
    message = "The path \"/users\" is added",
    candidate = at("/paths/~1users"),
)

private fun log(configure: ComparisonBuilder.() -> Unit = {}): ChangeLog {
    val builder = ComparisonBuilder()
    builder.configure()
    return ChangeLog("openapi", builder.build())
}

private fun ordered(value: Value): OrderedValue = OrderedValue(value)

class ComparisonTest : FunSpec({
    context("a change") {
        test("is breaking when it breaks either side and shows its code, its impacts and its place") {
            removed.isBreaking shouldBe true
            added.isBreaking shouldBe true
            val deprecated = Change(code = removed.code, client = Impact.Info, provider = Impact.Info, message = "old")
            deprecated.isBreaking shouldBe false
            removed.toString() shouldContain "[openapi.compatibility.path-removed] client=Breaking provider=NonBreaking"
            added.toString() shouldContain "/paths/~1users"
            removed shouldBe removed.copied()
            removed.hashCode() shouldBe removed.copied().hashCode()
            removed shouldNotBe added
            removed.equals(removed.message) shouldBe false
            deprecated.toString() shouldBe "[openapi.compatibility.path-removed] client=Info provider=Info old"
        }
    }

    context("a comparison") {
        test("tells the changes that break clients from those that break providers") {
            val comparison = Comparison(listOf(removed, added))

            comparison.breakingForClients shouldBe listOf(removed)
            comparison.breakingForProviders shouldBe listOf(added)
            comparison.isBreakingForClients shouldBe true
            comparison.isBreakingForProviders shouldBe true
            comparison shouldBe Comparison(listOf(removed, added))
            comparison.hashCode() shouldBe Comparison(listOf(removed, added)).hashCode()
            comparison.toString() shouldBe
                "Comparison(2 changes, 1 breaking for clients, 1 breaking for providers)"
            Comparison(emptyList()).isBreakingForClients shouldBe false
            Comparison(listOf(added)).isBreakingForClients shouldBe false
            Comparison(listOf(removed)).isBreakingForProviders shouldBe false
            comparison.equals(comparison.changes) shouldBe false
            Comparison(emptyList()).isBreakingForProviders shouldBe false
        }
    }

    context("the settings of a comparison") {
        test("take the default limits or those configured") {
            ComparisonSettings.Default.limits shouldBe ReadLimits.Default
            val builder = ComparisonBuilder()
            builder.limits { maximumDepth = TWO_CHANGES }
            val settings = builder.build()

            settings.limits.maximumDepth shouldBe TWO_CHANGES
            settings.toString() shouldContain "depth=2"
            val copy = ComparisonBuilder()
            copy.limits = settings.limits
            copy.build().limits shouldBe settings.limits
        }

        test("refuse changes after they were built") {
            val builder = ComparisonBuilder()
            builder.build()

            shouldThrow<IllegalStateException> { builder.limits = ReadLimits.Default }
            shouldThrow<IllegalStateException> { builder.limits { maximumDepth = 1 } }
        }

        test("refuse limits beyond their ceilings") {
            val builder = ComparisonBuilder()
            builder.limits { maximumDepth = 0 }

            shouldThrowAny { builder.build() }
        }
    }

    context("a change log") {
        test("records every change once and names its code after the prefix") {
            val log = log()
            repeat(TWO_CHANGES) {
                log.report(rule = RULE, impacts = breaking, message = "removed", baseline = at("/a"), candidate = null)
            }

            log.code(RULE).value shouldBe "openapi.compatibility.path-removed"
            log.comparison().changes.size shouldBe 1
        }

        test("reports what it drops beyond the limit of changes") {
            val log = log { limits { maximumDiagnostics = 1 } }
            log.report(rule = RULE, impacts = breaking, message = "one", baseline = null, candidate = null)
            log.report(rule = RULE, impacts = breaking, message = "two", baseline = null, candidate = null)

            val changes = log.comparison().changes
            changes.size shouldBe TWO_CHANGES
            changes.last().code.value shouldBe "openapi.compatibility.incomplete"
            changes.last().message shouldContain "1 further changes were dropped"
        }

        test("counts a change it already holds as dropped once it is full") {
            val log = log { limits { maximumDiagnostics = 1 } }
            log.report(rule = RULE, impacts = breaking, message = "one", baseline = null, candidate = null)
            log.report(rule = RULE, impacts = breaking, message = "one", baseline = null, candidate = null)

            val changes = log.comparison().changes
            changes.last().message shouldContain "1 further changes were dropped"
        }

        test("stops after its steps and reports it once") {
            val log = log { limits { maximumNodes = 1 } }

            log.step(baseline = null, candidate = null) shouldBe true
            log.step(baseline = null, candidate = null) shouldBe false
            log.step(baseline = null, candidate = null) shouldBe false
            val changes = log.comparison().changes
            changes.single().message shouldContain "stopped after 1 steps"
        }
    }

    context("a direction") {
        test("judges narrower and wider data for both sides") {
            Direction.Request.narrowed shouldBe (Impact.Breaking to Impact.NonBreaking)
            Direction.Request.widened shouldBe (Impact.NonBreaking to Impact.Breaking)
            Direction.Response.narrowed shouldBe (Impact.NonBreaking to Impact.Breaking)
            Direction.Response.widened shouldBe (Impact.Breaking to Impact.NonBreaking)
            Direction.Response.label shouldBe "in a response"
        }
    }

    context("an ordered value") {
        test("orders values by their kind and then by their content") {
            val values = listOf(
                Value.Null(),
                Value.Flag(false),
                Value.Flag(true),
                Value.Number(NumberValue.of(1)),
                Value.Number(NumberValue.of(2)),
                Value.Text("a"),
                Value.Sequence(listOf(Value.Text("a"))),
                Value.Sequence(listOf(Value.Text("a"), Value.Text("b"))),
                Value.Mapping(mapOf("a" to Value.Null())),
                Value.Mapping(mapOf("a" to Value.Flag(true))),
                Value.Mapping(mapOf("b" to Value.Null())),
            ).map(::ordered)

            values.zipWithNext().all { (low, high) -> low < high } shouldBe true
            ordered(Value.Null()).compareTo(ordered(Value.Null())) shouldBe 0
            ordered(Value.Text("a")) shouldBe ordered(Value.Text("a"))
            ordered(Value.Text("a")).hashCode() shouldBe Value.Text("a").hashCode()
            ordered(Value.Text("a")).toString() shouldBe "\"a\""
            ordered(Value.Text("a")).equals("a") shouldBe false
        }
    }
})

private fun Change.copied(): Change = Change(
    code = code,
    client = client,
    provider = provider,
    message = message,
    baseline = baseline,
    candidate = candidate,
)
