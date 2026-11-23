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

package com.davils.kapi.runtime.core.pattern

import com.davils.kore.core.validation.report.ValidationException
import com.davils.kore.core.validation.violation.ViolationCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

private const val LIMIT_COUNT: Int = 4
private const val FEW_STATES: Int = 5
private const val SHORT_SOURCE: Int = 8
private const val STATES_BELOW_DEFAULT: Int = PatternLimitsBuilder.DEFAULT_STATES - 1
private const val DEEPEST_OFFSET: Int = 3
private const val DEEP_NESTING: Int = 500

class PatternLimitsTest : FunSpec({
    context("the default limits") {
        test("are valid and carry every default") {
            val limits = PatternLimits.Default

            val report = PatternLimitsBuilder().validate()

            report.violations.shouldBeEmpty()
            limits.maximumSourceLength shouldBe PatternLimitsBuilder.DEFAULT_SOURCE_LENGTH
            limits.maximumStates shouldBe PatternLimitsBuilder.DEFAULT_STATES
            limits.maximumInputLength shouldBe PatternLimitsBuilder.DEFAULT_INPUT_LENGTH
            limits.maximumNesting shouldBe PatternLimitsBuilder.DEFAULT_NESTING
        }

        test("render every limit") {
            PatternLimits.Default.toString() shouldBe
                "PatternLimits(source=1024, states=10000, input=1048576, nesting=64)"
        }
    }

    context("the builder") {
        test("accepts every hard ceiling") {
            val limits = patternLimits {
                maximumSourceLength = PatternLimitsBuilder.HARD_SOURCE_LENGTH
                maximumStates = PatternLimitsBuilder.HARD_STATES
                maximumInputLength = PatternLimitsBuilder.HARD_INPUT_LENGTH
                maximumNesting = PatternLimitsBuilder.HARD_NESTING
            }

            limits.maximumStates shouldBe PatternLimitsBuilder.HARD_STATES
        }

        test("reports every limit above its ceiling or below one at once") {
            val builder = PatternLimitsBuilder()
            builder.maximumSourceLength = PatternLimitsBuilder.HARD_SOURCE_LENGTH + 1
            builder.maximumStates = 0
            builder.maximumInputLength = PatternLimitsBuilder.HARD_INPUT_LENGTH + 1
            builder.maximumNesting = -1

            val violations = builder.validate().violations

            violations.map { it.code } shouldBe List(LIMIT_COUNT) { ViolationCode.OUT_OF_RANGE }
        }

        test("throws from the DSL for a limit out of range") {
            val failure = shouldThrow<ValidationException> { patternLimits { maximumStates = 0 } }

            failure.message shouldContain "maximumStates"
        }
    }

    context("equality") {
        test("compares every limit") {
            val first = patternLimits { maximumStates = FEW_STATES }
            val second = patternLimits { maximumStates = FEW_STATES }

            first shouldBe second
            first.hashCode() shouldBe second.hashCode()
            first shouldNotBe PatternLimits.Default
            first shouldNotBe patternLimits {
                maximumStates = FEW_STATES
                maximumNesting = 2
            }
            first shouldNotBe patternLimits {
                maximumStates = FEW_STATES
                maximumInputLength = 2
            }
            first shouldNotBe patternLimits {
                maximumStates = FEW_STATES
                maximumSourceLength = 2
            }
            first.equals("limits") shouldBe false
        }
    }

    context("the length of the source") {
        val limits = patternLimits { maximumSourceLength = SHORT_SOURCE }

        test("compiles a source at the limit") {
            compiled("abcdefgh", limits).source shouldBe "abcdefgh"
        }

        test("refuses a source one past the limit before reading it") {
            val refusal = refused("abcdefgh(", limits)

            refusal.reason shouldBe PatternRefusal.TooLong
            refusal.offset shouldBe SHORT_SOURCE
            refusal.reason.isValidSyntax shouldBe false
        }
    }

    context("the number of states") {
        test("compiles a counted repetition at the limit") {
            compiled("a{9999}").matchesEntirely("a".repeat(STATES_BELOW_DEFAULT)) shouldBe true
        }

        test("refuses a counted repetition one past the limit") {
            val refusal = refused("a{10000}")

            refusal.reason shouldBe PatternRefusal.TooManyStates
            refusal.reason.isValidSyntax shouldBe true
        }

        test("refuses nested counted repetitions before expanding them") {
            refused("(a{1000}){1000}").reason shouldBe PatternRefusal.TooManyStates
            refused("((a{1000}){1000}){1000}").reason shouldBe PatternRefusal.TooManyStates
            refused("(?:(?:(?:a{999999999}){999999999}){999999999}){999999999}").reason shouldBe
                PatternRefusal.TooManyStates
        }

        test("counts unbounded and optional repetitions") {
            val limits = patternLimits { maximumStates = 6 }

            compiled("a{1,}b", limits).containsMatchIn("aab") shouldBe true
            refused("a{1,}bc", limits).reason shouldBe PatternRefusal.TooManyStates
            compiled("a{0,2}", limits).matchesEntirely("aa") shouldBe true
            refused("a{0,3}", limits).reason shouldBe PatternRefusal.TooManyStates
            compiled("a|b", limits).matchesEntirely("b") shouldBe true
            refused("a|b|c", limits).reason shouldBe PatternRefusal.TooManyStates
        }
    }

    context("the nesting of groups") {
        val limits = patternLimits { maximumNesting = 3 }

        test("compiles groups nested to the limit") {
            compiled("(((a)))", limits).matchesEntirely("a") shouldBe true
            refused("(?:(?=a)a)", patternLimits { maximumNesting = 2 }).reason shouldBe PatternRefusal.Lookaround
        }

        test("refuses groups nested one past the limit") {
            val refusal = refused("((((a))))", limits)

            refusal.reason shouldBe PatternRefusal.TooDeep
            refusal.offset shouldBe DEEPEST_OFFSET
            refusal.reason.isValidSyntax shouldBe false
        }

        test("refuses lookarounds nested one past the limit") {
            refused("((((?=a))))", limits).reason shouldBe PatternRefusal.TooDeep
        }

        test("refuses deep nesting under the default limits without exhausting the stack") {
            refused("(".repeat(DEEP_NESTING) + ")".repeat(DEEP_NESTING)).reason shouldBe PatternRefusal.TooDeep
        }
    }

    context("the length of an input") {
        val pattern = compiled("a+", patternLimits { maximumInputLength = 4 })

        test("matches an input at the limit") {
            pattern.containsMatchIn("baaa") shouldBe true
            pattern.matchesEntirely("aaaa") shouldBe true
        }

        test("throws for an input one past the limit") {
            val search = shouldThrow<IllegalArgumentException> { pattern.containsMatchIn("aaaaa") }
            val whole = shouldThrow<IllegalArgumentException> { pattern.matchesEntirely("aaaaa") }

            search.message shouldContain "at most 4 code units"
            whole.message shouldContain "but had 5"
        }
    }
})
