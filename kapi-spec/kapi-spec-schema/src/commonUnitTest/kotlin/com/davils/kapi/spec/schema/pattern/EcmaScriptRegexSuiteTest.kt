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

private const val FRENCH: String =
    "Les hivers de mon enfance étaient des saisons longues, longues. Nous vivions en trois lieux: " +
        "l'école, l'église et la patinoire; mais la vraie vie était sur la patinoire."
private const val NKO_DIGIT_ZERO: String = "߀"
private const val BENGALI_DIGITS: String = "৪২"
private const val ZERO_WIDTH_SPACE: String = "​"
private const val EM_SPACE: String = " "
private const val E_ACUTE: String = "é"
private const val SYMBOLS: String = "-%#"

private val suite: List<Pair<String, List<Pair<String, Boolean>>>> = listOf(
    "^abc\$" to listOf("abc" to true, "abc\n" to false),
    "^\\t\$" to listOf("\\t" to false, "\u0009" to true),
    "^\\cC\$" to listOf("\\cC" to false, "\u0003" to true),
    "^\\cc\$" to listOf("\\cc" to false, "\u0003" to true),
    "^\\d\$" to listOf("0" to true, NKO_DIGIT_ZERO to false, "৪" to false),
    "^\\D\$" to listOf(NKO_DIGIT_ZERO to true, "0" to false),
    "^\\w\$" to listOf("a" to true, E_ACUTE to false),
    "^\\W\$" to listOf(E_ACUTE to true, "a" to false),
    "^\\s\$" to listOf(
        " " to true,
        "\t" to true,
        "\u000B" to true,
        "\u000C" to true,
        " " to true,
        "﻿" to true,
        " " to true,
        " " to true,
        "\n" to true,
        "\r" to true,
        EM_SPACE to true,
        " " to true,
        "　" to true,
        ZERO_WIDTH_SPACE to false,
        "\u0085" to false,
        "-" to false,
    ),
    "^\\S\$" to listOf(EM_SPACE to false, ZERO_WIDTH_SPACE to true, "-" to true),
    "\\p{Letter}cole" to listOf(FRENCH to true),
    "\\wcole" to listOf(FRENCH to false),
    "[a-z]cole" to listOf(FRENCH to false),
    "^\\d+\$" to listOf("42" to true, SYMBOLS to false, BENGALI_DIGITS to false),
    "^\\p{digit}+\$" to listOf("42" to true, SYMBOLS to false, BENGALI_DIGITS to true),
    "^\\p{L}" to listOf("${E_ACUTE}cole" to true),
    "^🐲*\$" to listOf("🐲🐲" to true, "🐉" to false),
    "^[a-zA-Z0-9_-]+\$" to listOf("Snake_case-word" to true),
    "([abc])+\\s+\$" to listOf("abc  " to true),
)

class EcmaScriptRegexSuiteTest : FunSpec({
    context("the cases of optional/ecmascript-regex.json of the JSON Schema Test Suite") {
        suite.forEachIndexed { index, (source, inputs) ->
            test("case $index") {
                val pattern = compiled(source)

                inputs.map { (input, _) -> pattern.containsMatchIn(input) } shouldBe inputs.map { it.second }
            }
        }
    }

    context("the regexes the suite treats as invalid") {
        test("\\a is no control escape of ECMA-262") {
            refused("\\a").reason shouldBe PatternRefusal.Syntax
        }

        test("an unclosed group is invalid") {
            refused("^(abc]").reason shouldBe PatternRefusal.Syntax
        }

        test("\\Z is no anchor of ECMA-262") {
            refused("^abc\\Z").reason shouldBe PatternRefusal.Syntax
        }
    }
})
