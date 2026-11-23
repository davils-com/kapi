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
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

private const val MESSAGE_BOUND: Int = 400

private val syntaxErrors: List<String> = listOf(
    "(?<a>x)(?<a>y)", "(?:(?<a>x)|(?<a>y))(?<a>z)", "(a", "a)", "[z-a]", "*a", "a**", "a{2,1}", "a{", "a{1", "a{,2}",
    "\\a", "\\-", "[\\d-z]", "[a-\\d]", "\\1", "(a)\\2", "\\k<a>", "(?<a>.)\\k<b>", "\\k", "(?=a)*", "^*", "\$+",
    "\\b*", "(?i)a", "(?i-i:a)", "(?ii:a)", "(?-:a)", "(?x:a)", "\\p{Foo}", "\\p{L", "\\pL", "\\p{Script=Foo}",
    "\\p{gc=Foo}", "\\p{Lu=x}", "\\p{=L}", "\\p{L-u}", "\\p{}", "\\p{Foo=Latin}", "\\p{scx=Foo}", "\\c1", "\\c",
    "\\x4", "\\xg0", "\\u12", "\\u{110000}", "\\u{}", "\\u{1F600", "\\u{123456789}", "\\00", "\\01", "[\\1]", "[\\k]",
    "[\\B]", "]", "}", "{", "a{1}{2}", "a?+", "[", "[a", "\\", "[\\", "(?", "(?<", "(?<1a>x)", "(?<a", "(?<a-b>x)",
    "\\Z", "\\z", "\\A", "\\e", "\\h", "\\Q", "[\\p{L}-z]", "[a-\\p{L}]", "x{99999999999,99999999998}",
)

private val validSources: List<String> = listOf(
    "(?:(?<a>x)|(?<a>y))", "(?<a>x)|(?<a>y)", "(?:(?<a>x)|(?<a>y))|(?<a>z)", "[\\b]", "a??", "(?<\$_>x)",
    "(?<é>x)", "(?<a\\u{62}>x)", "(?<a‍b>x)", "()", "(?:)", "|", "a|", "|a", "/", "\\/", "[\\-]", "\\u{0}",
    "\\u{00000000000041}", "\\p{digit}", "\\P{Any}", "\\p{sc=Grek}", "[]", "[^]",
    "\\cZ", "\\ca", "[\\cA]", "[\\0]", "[\\x41-\\x5A]", "[\\u{1F600}-\\u{1F64F}]",
)

private val refusals: List<Triple<String, PatternRefusal, Int>> = listOf(
    Triple("(a)\\1", PatternRefusal.Backreference, 3),
    Triple("(?<a>.)\\k<a>", PatternRefusal.Backreference, 7),
    Triple("(?<\\u0061>x)\\k<a>", PatternRefusal.Backreference, 12),
    Triple("(?<a>a)|\\k<a>", PatternRefusal.Backreference, 8),
    Triple("\\k<a>(?<a>a)", PatternRefusal.Backreference, 0),
    Triple("\\1(a)", PatternRefusal.Backreference, 0),
    Triple("x(?=a)", PatternRefusal.Lookaround, 1),
    Triple("(?!a)", PatternRefusal.Lookaround, 0),
    Triple("(?<=a)b", PatternRefusal.Lookaround, 0),
    Triple("b(?<!a)", PatternRefusal.Lookaround, 1),
    Triple("(?i:a)", PatternRefusal.Flags, 0),
    Triple("a(?-i:a)", PatternRefusal.Flags, 1),
    Triple("(?ms-i:a)", PatternRefusal.Flags, 0),
    Triple("\\p{Emoji}", PatternRefusal.UnsupportedProperty, 0),
    Triple("[\\P{Emoji_Presentation}]", PatternRefusal.UnsupportedProperty, 1),
    Triple("\\p{scx=Latn}", PatternRefusal.UnsupportedProperty, 0),
    Triple("\\p{Script_Extensions=Greek}", PatternRefusal.UnsupportedProperty, 0),
    Triple("\\p{ID_Start}", PatternRefusal.UnsupportedProperty, 0),
    Triple("x{99999999998,99999999999}", PatternRefusal.TooManyStates, 0),
)

class PatternRefusalTest : FunSpec({
    context("a syntax error is refused as one") {
        syntaxErrors.forEachIndexed { index, source ->
            test("syntax error $index is refused") {
                val refusal = refused(source)

                refusal.reason shouldBe PatternRefusal.Syntax
                refusal.reason.isValidSyntax shouldBe false
            }
        }
    }

    context("a valid source compiles") {
        validSources.forEachIndexed { index, source ->
            test("valid source $index compiles") {
                compiled(source).source shouldBe source
            }
        }
    }

    context("a valid construct the engine cannot run is refused with its reason and offset") {
        refusals.forEachIndexed { index, (source, reason, offset) ->
            test("refusal $index is $reason at $offset") {
                val refusal = refused(source)

                refusal.reason shouldBe reason
                refusal.offset shouldBe offset
                refusal.reason.isValidSyntax shouldBe true
            }
        }
    }

    context("the whole source is read before an unsupported construct is refused") {
        test("a later syntax error wins over an earlier back reference") {
            refused("(a)\\1(").reason shouldBe PatternRefusal.Syntax
        }

        test("the first unsupported construct is reported") {
            val refusal = refused("a(?=b)(c)\\1")

            refusal.reason shouldBe PatternRefusal.Lookaround
            refusal.offset shouldBe 1
        }
    }

    context("syntax errors are located") {
        test("an unclosed group is located at its parenthesis") {
            refused("ab(cd").offset shouldBe 2
        }

        test("a lone parenthesis is located where it stands") {
            refused("abc)").offset shouldBe "abc".length
        }

        test("a range out of order is located at its start") {
            refused("x[z-a]").offset shouldBe 2
        }

        test("a dangling quantifier is located at the quantifier") {
            refused("a|*").offset shouldBe 2
        }

        test("a quantifier in the wrong order is located at its brace") {
            refused("ab{3,2}").offset shouldBe 2
        }
    }

    context("the message of a refusal") {
        test("names the offset and shows the source escaped") {
            val refusal = refused("\u001Bab(")

            refusal.message shouldContain "offset 3"
            refusal.message shouldNotContain "\u001B"
        }

        test("bounds the source it shows") {
            val source = "a".repeat(1000) + ")"

            val message = refused(source).message

            (message.length < MESSAGE_BOUND) shouldBe true
            message shouldContain "chars)"
        }
    }

    context("a refused compilation") {
        test("is equal to an equal refusal and shows its parts") {
            val first = refused("(a")
            val second = refused("(a")

            first shouldBe second
            first.hashCode() shouldBe second.hashCode()
            first.equals(Pattern.compile("a")) shouldBe false
            first.toString() shouldContain "reason=Syntax"
        }
    }
})
