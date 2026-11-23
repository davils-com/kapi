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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val TEXT_1: String = "\u000A"
private const val TEXT_2: String = "\uD83D\uDE00"
private const val TEXT_3: String = "\u00E9"
private const val TEXT_4: String = "\u0009"
private const val TEXT_5: String = "\u200B"
private const val TEXT_6: String = "\u0085"
private const val TEXT_7: String = "\u0003"
private const val TEXT_8: String = "\u00C9cole"
private const val TEXT_9: String = "\u00C9"
private const val TEXT_10: String = "\uDBFF\uDFFF"
private const val TEXT_11: String = "\u0378"
private const val TEXT_12: String = "\u01C5"
private const val TEXT_13: String = "\u20AC"
private const val TEXT_14: String = "a\u000Ab"
private const val TEXT_15: String = "\uDC00"

private val cases: List<Pair<String, List<Pair<String, Boolean>>>> = listOf(
    "abc" to listOf("xxabcxx" to true, "ab" to false),
    "^abc\$" to listOf("abc" to true, "abc\u000A" to false),
    "^abc" to listOf("xabc" to false),
    "abc\$" to listOf("abcx" to false),
    "a|b" to listOf("b" to true, "c" to false),
    "(a|b)c" to listOf("bc" to true),
    "(?:a|bc)+\$" to listOf("abcbca" to true, "abcb" to false),
    "a*" to listOf("" to true),
    "^a*\$" to listOf("aaaa" to true),
    "^a+\$" to listOf("" to false),
    "^a?b\$" to listOf("b" to true, "aab" to false),
    "^a{3}\$" to listOf("aaa" to true, "aa" to false),
    "^a{2,}\$" to listOf("aaaaa" to true, "a" to false),
    "^a{2,3}\$" to listOf("aaaa" to false, "aaa" to true),
    "^a{0}\$" to listOf("" to true),
    "^a{0,0}b\$" to listOf("b" to true),
    "^a*?\$" to listOf("aaa" to true),
    "^a+?b\$" to listOf("aab" to true),
    "^a{2,3}?\$" to listOf("aa" to true),
    "^(?:ab){2}\$" to listOf("abab" to true, "ab" to false),
    "." to listOf(TEXT_1 to false, "\u000D" to false, "\u2028" to false, "x" to true),
    "^.\$" to listOf(TEXT_2 to true, "\uD83D" to true),
    "^..\$" to listOf(TEXT_2 to false),
    """^\uD83D\uDE00$""" to listOf(TEXT_2 to true),
    "^\\u{1F600}\$" to listOf(TEXT_2 to true),
    """^[\u{1F600}-\u{1F64F}]$""" to listOf("\uD83D\uDE10" to true, "\uD83D\uDE50" to false),
    "^\\uD83D\$" to listOf(TEXT_2 to false),
    "\\uD83D" to listOf(TEXT_2 to false),
    "^[\uD83D\uDE00]\$" to listOf(TEXT_2 to true),
    "^[a-z]+\$" to listOf("hello" to true, "Hello" to false),
    "^[^a-z]+\$" to listOf("HELLO" to true, "HeLLO" to false),
    "^[]\$" to listOf("" to false),
    "[]" to listOf("a" to false),
    "^[^]\$" to listOf(TEXT_1 to true),
    "^[-a]\$" to listOf("-" to true),
    "^[a-]\$" to listOf("-" to true),
    "^[\\d-]\$" to listOf("-" to true),
    "^[\\w.]+\$" to listOf("a.b_c" to true),
    "^[\\b]\$" to listOf("\u0008" to true),
    "^[\\-]\$" to listOf("-" to true),
    "^[\\]]\$" to listOf("]" to true),
    "^[[]\$" to listOf("[" to true),
    "^\\d+\$" to listOf("0123456789" to true),
    "^\\d\$" to listOf("\u0660" to false),
    "^\\D\$" to listOf("a" to true),
    "^\\w\$" to listOf("_" to true, TEXT_3 to false),
    "^\\W\$" to listOf(TEXT_3 to true),
    "^\\s\$" to listOf(
        " " to true,
        TEXT_4 to true,
        "\u000B" to true,
        "\u000C" to true,
        "\u00A0" to true,
        "\uFEFF" to true,
        "\u2029" to true,
        "\u2003" to true,
        TEXT_5 to false,
        TEXT_6 to false,
        "\u180E" to false,
        "\u1680" to true,
    ),
    "^\\S\$" to listOf(TEXT_5 to true),
    "\\bfoo\\b" to listOf("a foo b" to true, "afoo" to false),
    "\\Bfoo" to listOf("afoo" to true, "foo" to false),
    "^\\b\$" to listOf("" to false),
    "^\\B\$" to listOf("" to true),
    "\\b" to listOf(TEXT_3 to false),
    "a\\b" to listOf("a\u00E9" to true),
    "^\\t\$" to listOf(TEXT_4 to true, "\\t" to false),
    """^\n\r\v\f$""" to listOf("\u000A\u000D\u000B\u000C" to true),
    "^\\0\$" to listOf("\u0000" to true),
    "^\\x41\$" to listOf("A" to true),
    "^\\u0041\$" to listOf("A" to true),
    "^\\cC\$" to listOf(TEXT_7 to true),
    "^\\cc\$" to listOf(TEXT_7 to true),
    "^\\cJ\$" to listOf(TEXT_1 to true),
    "^\\.\$" to listOf("." to true),
    "^\\/\$" to listOf("/" to true),
    "^/\$" to listOf("/" to true),
    """^\*\+\?$""" to listOf("*+?" to true),
    """^\$\^$""" to listOf("\$^" to true),
    """^\{\}\(\)\[\]\|\\$""" to listOf("{}()[]|\\" to true),
    """^(?<year>\d{4})-(?<month>\d{2})$""" to listOf("2026-11" to true),
    "^(?<a>x)(?<b>y)\$" to listOf("xy" to true),
    "^\\p{L}+\$" to listOf(TEXT_8 to true),
    "^\\p{Letter}cole" to listOf(TEXT_8 to true),
    "\\wcole" to listOf(TEXT_8 to false),
    "[a-z]cole" to listOf(TEXT_8 to false),
    "^\\p{Lu}\$" to listOf(TEXT_9 to true),
    "^\\p{Ll}\$" to listOf(TEXT_9 to false),
    "^\\P{Lu}\$" to listOf(TEXT_3 to true),
    "^\\p{digit}+\$" to listOf("\u09EA\u09E8" to true),
    "^\\p{Nd}+\$" to listOf("42" to true),
    "^\\p{N}\$" to listOf("\u00BD" to true),
    "^\\p{gc=Lu}\$" to listOf("A" to true),
    "^\\p{General_Category=Decimal_Number}\$" to listOf("\u0663" to true),
    "^\\p{Script=Greek}+\$" to listOf("\u03B1\u03B2\u03B3" to true),
    "^\\p{sc=Grek}\$" to listOf("a" to false),
    "^\\p{Script=Han}\$" to listOf("\u4E2D" to true),
    "^\\p{sc=Cyrl}\$" to listOf("\u0416" to true),
    "^\\p{Script=Latin}\$" to listOf("\u00DF" to true),
    "^\\p{Script=Common}\$" to listOf("1" to true),
    "^\\p{sc=Zinh}\$" to listOf("\u0300" to true),
    "^\\p{Any}\$" to listOf(TEXT_10 to true),
    "^\\p{ASCII}\$" to listOf("\u0080" to false, "\u007F" to true),
    "^\\p{Assigned}\$" to listOf(TEXT_11 to false),
    "^\\p{Cn}\$" to listOf(TEXT_11 to true),
    "^\\p{Co}\$" to listOf("\uE000" to true),
    "^\\p{Cs}\$" to listOf("\uD800" to true),
    "^\\p{AHex}+\$" to listOf("aF09" to true),
    "^\\p{Hex}\$" to listOf("\uFF21" to true),
    "^\\p{White_Space}\$" to listOf(TEXT_6 to true),
    "^\\p{Alpha}\$" to listOf("\u0345" to true),
    "^\\p{Lower}\$" to listOf("\u00AA" to true),
    "^\\p{Upper}\$" to listOf("\u2160" to true),
    "^\\p{Ideo}\$" to listOf("\u3007" to true),
    """^[\p{L}\p{Nd}]+$""" to listOf("abc123" to true),
    "^[^\\p{L}]+\$" to listOf("123" to true),
    "^\\p{LC}\$" to listOf(TEXT_12 to true),
    "^\\p{Lt}\$" to listOf(TEXT_12 to true),
    "^\\p{Zs}\$" to listOf("\u3000" to true),
    "^\\p{P}+\$" to listOf("!?." to true),
    "^\\p{S}\$" to listOf(TEXT_13 to true),
    "^\\p{Sc}\$" to listOf(TEXT_13 to true),
    "^\\p{M}\$" to listOf("\u0301" to true),
    "^\\p{C}\$" to listOf(TEXT_5 to true),
    "(a+)+\$" to listOf("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa!" to false),
    "^(a|a)*\$" to listOf("aaaaaaaaaaaaaaaaaaaaaaaaaab" to false),
    "^(a*)*\$" to listOf("aaaa" to true),
    "^(?:)*\$" to listOf("" to true),
    "^(?:a?)*b\$" to listOf("aab" to true),
    "^(?:a|)+\$" to listOf("aaa" to true),
    "x*" to listOf("yyy" to true),
    "" to listOf("" to true, "abc" to true),
    "^\$" to listOf("" to true, "a" to false),
    "\$" to listOf("abc" to true),
    "^" to listOf("abc" to true),
    "a{1,2}b{0,1}c" to listOf("aabc" to true),
    "^(?:a{2}){2}\$" to listOf("aaaa" to true, "aaa" to false),
    """^[\s\S]*$""" to listOf(TEXT_14 to true),
    "^.*\$" to listOf(TEXT_14 to false),
    """^[\x00-\x7F]+$""" to listOf("ascii" to true),
    """^[\u0000-\u{10FFFF}]$""" to listOf(TEXT_10 to true),
    """^[\uD800-\uDFFF]$""" to listOf(TEXT_15 to true),
    "^\\uDC00\$" to listOf(TEXT_15 to true),
    "^\\uD83Dx\$" to listOf("\uD83Dx" to true),
    "[^a]" to listOf(TEXT_2 to true),
    "^[^a]\$" to listOf(TEXT_2 to true),
    "^\\u{0000000041}\$" to listOf("A" to true),
    """^[\0-\cA]$""" to listOf("\u0001" to true),
    "^-\$" to listOf("-" to true),
    "^,\$" to listOf("," to true),
    "^a(?:b|c(?:d|e)f)g\$" to listOf("acefg" to true, "acfg" to false),
)

class PatternMatchTest : FunSpec({
    context("a pattern searches its input as RegExp with the u flag does") {
        cases.forEachIndexed { index, (source, inputs) ->
            test("pattern $index answers every input like RegExp") {
                val pattern = compiled(source)

                inputs.map { (input, _) -> pattern.containsMatchIn(input) } shouldBe inputs.map { it.second }
            }
        }
    }
})
