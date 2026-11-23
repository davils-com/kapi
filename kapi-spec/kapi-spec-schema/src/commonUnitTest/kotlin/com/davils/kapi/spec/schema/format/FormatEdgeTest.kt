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

package com.davils.kapi.spec.schema.format

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private val edges: List<Pair<String, List<Pair<String, Boolean>>>> = listOf(
    "date-time" to listOf(
        "2026-11-23X10:00:00Z" to false,
        "2026-11-23T10:00-00Z" to false,
        "2026-11-23T10:00:00*01:00" to false,
        "2026-11-23T10:00:00+01-00" to false,
        "2026-11-23T10:00:00+1:000" to false,
    ),
    "time" to listOf(
        "10:00:00Z" to true,
        "10-00:00Z" to false,
        "23:59:60Z" to true,
        "10:00:00" to false,
    ),
    "email" to listOf(
        "@example.com" to false,
        """"a\"b"@example.com""" to true,
        """"a\""" + "\u0001\"@example.com" to false,
        "\"ä\"@example.com" to false,
        "${"a".repeat(64)}@${"b".repeat(63)}.${"c".repeat(63)}.${"d".repeat(63)}.${"e".repeat(57)}" to false,
    ),
    "idn-email" to listOf(
        "\"ä\"@example.com" to true,
        "\"\uD800\"@example.com" to false,
    ),
    "int32" to listOf(
        "10e-1" to true,
        "15e-1" to false,
        "1e" to false,
        "1e+" to false,
    ),
    "float" to listOf(
        "1e39" to false,
        "1e400" to false,
        "-3.4e38" to true,
    ),
    "uri" to listOf(
        "http://a/b?c#d" to true,
        "http://a/b#d?c" to true,
    ),
    "iri" to listOf(
        "http://a/﷐" to false,
        "http://a/󠀁" to false,
        "http://a/ä" to true,
    ),
    "uri-template" to listOf(
        "{x:0}" to false,
        "{x:10000}" to false,
        "{x:01}" to false,
        "{x:3}" to true,
        "{a%20b}" to true,
        "{a-b}" to false,
        "{a_b.c}" to true,
    ),
    "hostname" to listOf(
        "ab--cd" to false,
        "xn--ab" to false,
        "a-b" to true,
    ),
    "idn-hostname" to listOf(
        "ÄB" to true,
        "äb" to true,
        "אב" to true,
        "אa" to false,
        "א1١" to false,
        "a‍b" to false,
        "ب‌ب" to true,
        "a‌b" to false,
        "ب‌" to false,
    ),
    "relative-json-pointer" to listOf(
        "0+1/a" to true,
        "0-0" to false,
        "1+01" to false,
        "1+0" to false,
    ),
    "byte" to listOf(
        "ab+/" to true,
        "a-b=" to false,
        "ab*c" to false,
    ),
)

class FormatEdgeTest : FunSpec({
    edges.forEach { (format, cases) ->
        test("$format decides its edge cases") {
            val wrong = cases.filter { (value, isValid) ->
                isValid != (Formats.check(format = format, value = value) == FormatCheck.Valid)
            }
            wrong shouldBe emptyList()
        }
    }
})
