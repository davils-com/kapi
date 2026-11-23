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

package com.davils.kapi.spec.schema.pattern

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

private const val LONG_INPUT: Int = 10_000
private val SLOW_COMPILATION: Duration = 5.seconds
private const val MEDIUM_INPUT: Int = 5_000
private const val WORD_COUNT: Int = 2_000
private const val OPTIONAL_COUNT: Int = 30
private const val FEW_STATES: Int = 50
private const val NESTED_PLUS: String = "(a+)+\$"
private const val COUNTED_OPTIONALS: String = "^(?:a?){30}a{30}\$"
private const val GRINNING_FACE: String = "😀"
private const val BETWEEN_AS: String = "aabba"

private val catastrophic: List<Pair<String, String>> = listOf(
    NESTED_PLUS to "a".repeat(LONG_INPUT) + "!",
    "^(a|a)*\$" to "a".repeat(LONG_INPUT) + "!",
    "^(a|aa)+\$" to "a".repeat(LONG_INPUT) + "!",
    "(a*)*b" to "a".repeat(LONG_INPUT),
    "(x+x+)+y" to "x".repeat(LONG_INPUT),
    "^(([a-z])+.)+[A-Z]([a-z])+\$" to "a".repeat(LONG_INPUT) + "!",
    "^(\\w+\\s?)*\$" to "word ".repeat(WORD_COUNT) + "!",
    "(.*a){20}" to "a".repeat(MEDIUM_INPUT),
    COUNTED_OPTIONALS to "a".repeat(OPTIONAL_COUNT),
)

class PatternSafetyTest : FunSpec({
    context("no input makes a pattern backtrack catastrophically") {
        catastrophic.forEachIndexed { index, (source, input) ->
            test("pattern $index finishes well under a second") {
                val pattern = compiled(source)
                val start = TimeSource.Monotonic.markNow()

                pattern.containsMatchIn(input)
                pattern.matchesEntirely(input)

                start.elapsedNow() shouldBeLessThan 1.seconds
            }
        }

        test("many property escapes at the hard source length compile quickly in bounded memory") {
            val limits = patternLimits {
                maximumSourceLength = PatternLimitsBuilder.HARD_SOURCE_LENGTH
                maximumStates = PatternLimitsBuilder.HARD_STATES
            }
            val escapes = """\p{L}\P{Lu}\p{sc=Grek}"""
            val count = PatternLimitsBuilder.HARD_SOURCE_LENGTH / escapes.length - 1
            val start = TimeSource.Monotonic.markNow()

            compiled("[${escapes.repeat(count)}]", limits).containsMatchIn("é") shouldBe true
            compiled(escapes.repeat(count), limits).containsMatchIn("é") shouldBe false

            start.elapsedNow() shouldBeLessThan SLOW_COMPILATION
        }

        test("the classic nested quantifier answers correctly") {
            val pattern = compiled(NESTED_PLUS)

            pattern.containsMatchIn("a".repeat(LONG_INPUT) + "!") shouldBe false
            pattern.containsMatchIn("!" + "a".repeat(LONG_INPUT)) shouldBe true
        }

        test("exponential counted optionals answer correctly") {
            compiled(COUNTED_OPTIONALS).matchesEntirely("a".repeat(OPTIONAL_COUNT)) shouldBe true
            compiled(COUNTED_OPTIONALS).matchesEntirely("a".repeat(OPTIONAL_COUNT * 2 + 1)) shouldBe false
        }
    }

    context("surrogate pairs") {
        test("count as one code point in the source and the input") {
            compiled("^.\$").containsMatchIn(GRINNING_FACE) shouldBe true
            compiled("^..\$").containsMatchIn(GRINNING_FACE) shouldBe false
            compiled("^\uD83D\uDE00{2}\$").containsMatchIn("\uD83D\uDE00\uD83D\uDE00") shouldBe true
            compiled("^[\uD83D\uDE00-\uD83D\uDE4F]\$").containsMatchIn("\uD83D\uDE10") shouldBe true
        }

        test("are never split by a lone surrogate of the pattern") {
            compiled("\\uD83D").containsMatchIn(GRINNING_FACE) shouldBe false
            compiled("\uD83D").containsMatchIn(GRINNING_FACE) shouldBe false
            compiled("^\\uD83D\$").containsMatchIn("\uD83D") shouldBe true
        }

        test("leave a lone surrogate of the input a code point of its own") {
            compiled("^\\uDE00\$").matchesEntirely("\uDE00") shouldBe true
            compiled("^.x\$").matchesEntirely("\uD83Dx") shouldBe true
            compiled("^\\p{Cs}+\$").matchesEntirely("\uDE00\uD83D") shouldBe true
        }

        test("combine escaped surrogate halves into one code point") {
            compiled("^\\uD83D\\uDE00\$").matchesEntirely(GRINNING_FACE) shouldBe true
            compiled("^[\\uD83D\\uDE00]\$").matchesEntirely(GRINNING_FACE) shouldBe true
            compiled("^\\u{1F600}\$").matchesEntirely(GRINNING_FACE) shouldBe true
        }
    }

    context("search and whole matches") {
        test("a search finds a match anywhere unless anchored") {
            compiled("b+").containsMatchIn(BETWEEN_AS) shouldBe true
            compiled("^b+").containsMatchIn(BETWEEN_AS) shouldBe false
            compiled("b+\$").containsMatchIn(BETWEEN_AS) shouldBe false
        }

        test("a whole match needs the whole input") {
            compiled("b+").matchesEntirely("bbb") shouldBe true
            compiled("b+").matchesEntirely("abbb") shouldBe false
            compiled("b+").matchesEntirely("bbba") shouldBe false
            compiled("a|ab").matchesEntirely("ab") shouldBe true
            compiled("").matchesEntirely("") shouldBe true
            compiled("").matchesEntirely("a") shouldBe false
            compiled("a*").containsMatchIn("") shouldBe true
        }

        test("a lazy quantifier answers like a greedy one") {
            compiled("^a+?\$").matchesEntirely("aaa") shouldBe true
            compiled("a{2,}?").matchesEntirely("aaaa") shouldBe true
        }
    }

    context("a compiled pattern") {
        test("keeps its source and limits") {
            val limits = patternLimits { maximumStates = FEW_STATES }
            val pattern = compiled("a+", limits)

            pattern.source shouldBe "a+"
            pattern.limits shouldBe limits
        }

        test("is equal to a pattern of the same source and limits") {
            compiled("a+") shouldBe compiled("a+")
            compiled("a+").hashCode() shouldBe compiled("a+").hashCode()
            (compiled("a+") == compiled("a*")) shouldBe false
            (compiled("a+") == compiled("a+", patternLimits { maximumStates = FEW_STATES })) shouldBe false
            compiled("a+").equals("a+") shouldBe false
            Pattern.compile("a+") shouldBe Pattern.compile("a+")
            Pattern.compile("a+").hashCode() shouldBe Pattern.compile("a+").hashCode()
            Pattern.compile("a+").toString() shouldBe "Compiled(Pattern(\"a+\"))"
        }

        test("shows its source escaped") {
            val shown = compiled("a\u001B\n").toString()

            shown shouldNotContain "\u001B"
            shown shouldContain "\\n"
        }
    }
})
