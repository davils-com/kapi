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

import com.davils.kapi.spec.schema.format.FormatCheck.Valid
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf

private const val FORMAT_COUNT: Int = 26
private const val HOSTILE: String = "\u001B[31m<script>"
private const val IDN_HOSTNAME: String = "idn-hostname"
private const val JSON_POINTER: String = "json-pointer"

private val cases: List<Pair<String, List<Pair<String, Boolean>>>> = listOf(
    "date-time" to listOf(
        "2026-11-23T10:00:00Z" to true,
        "2026-11-23t10:00:00.5+01:30" to true,
        "2026-11-23T10:00:00" to false,
        "2026-11-23T10:00:00.Z" to false,
        "2026-11-23 10:00:00Z" to false,
        "2026-11-23T24:00:00Z" to false,
        "2026-11-23T10:60:00Z" to false,
        "2026-11-23T10:00:00+24:00" to false,
        "2026-11-23T10:00:00+01:60" to false,
        "2026-11-23T10:00:00+0100" to false,
        "2016-12-31T23:59:60Z" to true,
        "2016-12-31T22:59:60-01:00" to true,
        "2016-12-31T00:59:60+01:00" to true,
        "2016-12-31T23:59:61Z" to false,
        "2016-12-31T23:59:60+01:00" to false,
    ),
    "date" to listOf(
        "2000-02-29" to true,
        "1900-02-29" to false,
        "2024-02-29" to true,
        "2023-02-29" to false,
        "2026-04-31" to false,
        "2026-12-31" to true,
        "2026-00-10" to false,
        "2026-01-00" to false,
        "+2026-01-01" to false,
        "2026-1-01" to false,
        "" to false,
    ),
    "time" to listOf(
        "00:00:00Z" to true,
        "23:59:59.999999999-12:00" to true,
        "23:59:60Z" to true,
        "12:00:00" to false,
        "12:00:00z" to true,
        "12:00Z" to false,
        "12:00:00.Z" to false,
        "12:00:00 Z" to false,
    ),
    "duration" to listOf(
        "P1Y2M3DT4H5M6S" to true,
        "P1Y" to true,
        "P1M1D" to true,
        "PT1M1S" to true,
        "PT1H1M" to true,
        "P1W" to true,
        "P1Y1D" to false,
        "PT1H1S" to false,
        "P1W1D" to false,
        "P" to false,
        "PT" to false,
        "P1YT" to false,
        "1Y" to false,
        "P1.5Y" to false,
        "p1y" to false,
        "P1Y2Y" to false,
        "PT1H2H" to false,
        "PTT1H" to false,
    ),
    "email" to listOf(
        "a@example.com" to true,
        """"quoted\"pair"@example.com""" to true,
        "\"a b\"@example.com" to true,
        "a@[IPv6:2001:db8::1]" to true,
        "a@[192.0.2.1]" to true,
        "a@[256.0.0.1]" to false,
        "a@[tag:general]" to false,
        "a@" to false,
        "@example.com" to false,
        "a..b@example.com" to false,
        "ä@example.com" to false,
        "a@exa_mple.com" to false,
        "a@example.com." to false,
        "\"unterminated@example.com" to false,
        "${"a".repeat(65)}@example.com" to false,
        "${"a".repeat(64)}@example.com" to true,
        "a@${"b".repeat(63)}.${"c".repeat(63)}.${"d".repeat(63)}.${"e".repeat(60)}" to true,
        "a@${"b".repeat(63)}.${"c".repeat(63)}.${"d".repeat(63)}.${"e".repeat(61)}" to false,
    ),
    "idn-email" to listOf(
        "ä@example.com" to true,
        "ä@bücher.example" to true,
        "\"ä b\"@example.com" to true,
        "a@bücher..example" to false,
        "\uD800@example.com" to false,
    ),
    "hostname" to listOf(
        "example.com" to true,
        "EXAMPLE.COM" to true,
        "a" to true,
        "xn--bcher-kva.example" to true,
        "XN--BCHER-KVA.example" to true,
        "xn--bcher-kva-.example" to false,
        "xn--a" to false,
        "xn--abc" to false,
        "ab--c" to false,
        "a.b-.c" to false,
        "bücher.example" to false,
        "${"a".repeat(63)}.com" to true,
        "${"a".repeat(64)}.com" to false,
        "${"a".repeat(63)}.${"b".repeat(63)}.${"c".repeat(63)}.${"d".repeat(61)}" to true,
        "${"a".repeat(63)}.${"b".repeat(63)}.${"c".repeat(63)}.${"d".repeat(62)}" to false,
        "xn--mgbh0fb.xn--kgbechtv" to true,
    ),
    IDN_HOSTNAME to listOf(
        "bücher.example" to true,
        "BÜCHER.example" to true,
        "例。テスト" to true,
        "ａｂｃ.example" to true,
        "a­b" to true,
        "Ⅳ" to false,
        "١٢" to false,
        "אב.example" to true,
        "אa" to false,
        "ا١" to true,
        "ا١۱" to false,
        "à" to true,
        "̀a" to false,
        "-bücher" to false,
        "bü--cher" to false,
        "ü".repeat(60) to false,
        "${"ü".repeat(40)}.example" to true,
        "\uD800" to false,
        "͸" to false,
        "" to false,
        "😀" to false,
        "ᄀ" to false,
        "ب‌ب" to true,
        "a‌b" to false,
        "क्‍ष" to true,
        "‍" to false,
        "l·l" to true,
        "·" to false,
        "α͵β" to true,
        "͵" to false,
        "א״ב" to true,
        "״" to false,
        "・ア" to true,
        "・" to false,
        "ب٠١" to true,
        "۰۱" to true,
        "ـ" to false,
        "ß" to true,
    ),
    "ipv4" to listOf(
        "0.0.0.0" to true,
        "255.255.255.255" to true,
        "256.0.0.0" to false,
        "1.2.3" to false,
        "1.2.3.4." to false,
        "01.2.3.4" to false,
        "1.2.3.-4" to false,
        "1.2.3.4 " to false,
        "١.2.3.4" to false,
        "1234.1.1.1" to false,
    ),
    "ipv6" to listOf(
        "::" to true,
        "::1" to true,
        "1::" to true,
        "1:2:3:4:5:6:7:8" to true,
        "1:2:3:4:5:6:7::" to true,
        "::2:3:4:5:6:7:8" to true,
        "1:2:3:4:5:6:1.2.3.4" to true,
        "::ffff:1.2.3.4" to true,
        "1:2:3:4:5:6:7:1.2.3.4" to false,
        "1:2:3:4:5:6:7:8:9" to false,
        "1:::2" to false,
        "1::2::3" to false,
        ":1:2:3:4:5:6:7" to false,
        "12345::" to false,
        "1.2.3.4" to false,
        "fe80::1%eth0" to false,
        "::1.2.3" to false,
        "::01.2.3.4" to false,
        "g::" to false,
        "" to false,
        "1:2:3:4:5:6:7:8:".repeat(4) to false,
    ),
    "uri" to listOf(
        "https://example.com/a?b#c" to true,
        "urn:isbn:0451450523" to true,
        "/relative" to false,
        "https://example.com/é" to false,
        "https://exa mple.com" to false,
    ),
    "uri-reference" to listOf(
        "/relative" to true,
        "" to true,
        "#fragment" to true,
        "\\\\windows" to false,
        "a b" to false,
    ),
    "iri" to listOf(
        "https://bücher.example/é?é#é" to true,
        "https://example.com/?" to true,
        "https://example.com/" to false,
        "https://example.com/#" to false,
        "https://example.com/￾" to false,
        "https://example.com/\u0085" to false,
        "/é" to false,
        "https://[::1]/" to true,
        "https://[é::1]/" to false,
    ),
    "iri-reference" to listOf(
        "/é" to true,
        "é" to true,
        "#é" to true,
        "\uD800" to false,
        "a b" to false,
    ),
    "uuid" to listOf(
        "123e4567-e89b-12d3-a456-426614174000" to true,
        "123E4567-E89B-12D3-A456-426614174000" to true,
        "123e4567e89b12d3a456426614174000" to false,
        "123e4567-e89b-12d3-a456-42661417400g" to false,
        "123e4567-e89b-12d3-a456-4266141740000" to false,
        "123e4567-e89b-12d3-a456_426614174000" to false,
    ),
    "uri-template" to listOf(
        "https://example.com/{id}" to true,
        "{+path}/x{?a,b*}{#frag}{.ext}{/seg}{;p}{&q}" to true,
        "{var:30}" to true,
        "{var:9999}" to true,
        "{var:10000}" to false,
        "{var:0}" to false,
        "{var:3*}" to false,
        "{a.b}" to true,
        "{a..b}" to false,
        "{%41}" to true,
        "{%4}" to false,
        "{=a}" to false,
        "{}" to false,
        "{a" to false,
        "a}" to false,
        "%41{a}" to true,
        "%4" to false,
        "a b" to false,
        "é{a}" to true,
    ),
    JSON_POINTER to listOf(
        "" to true,
        "/" to true,
        "/a~0b~1c" to true,
        "/a~2" to false,
        "/a~" to false,
        "a" to false,
    ),
    "relative-json-pointer" to listOf(
        "0" to true,
        "1/a" to true,
        "2#" to true,
        "0+1/a" to true,
        "3-2#" to true,
        "0+0" to false,
        "0-01" to false,
        "-1" to false,
        "01" to false,
        "0#/a" to false,
        "" to false,
    ),
    "regex" to listOf(
        "^[a-z]+$" to true,
        "(a)\\1" to true,
        "(?<=a)b" to true,
        "\\p{Emoji}" to true,
        "a{1000000}" to true,
        "(" to false,
        "\\p{Unknown_Property}" to false,
        "(".repeat(300) + ")".repeat(300) to false,
    ),
    "int32" to listOf(
        "0" to true,
        "-0" to true,
        "2147483647" to true,
        "2147483648" to false,
        "-2147483648" to true,
        "-2147483649" to false,
        "1.0" to true,
        "1.5" to false,
        "1e3" to true,
        "2.147483647e9" to true,
        "2.147483648e9" to false,
        "100e-2" to true,
        "1e-1" to false,
        "1e999999999" to false,
        "0e999999999" to true,
        "01" to false,
        "1." to false,
        ".5" to false,
        "+1" to false,
        "one" to false,
        "" to false,
    ),
    "int64" to listOf(
        "9223372036854775807" to true,
        "9223372036854775808" to false,
        "-9223372036854775808" to true,
        "-9223372036854775809" to false,
        "92233720368547758070e-1" to true,
    ),
    "float" to listOf(
        "1.5" to true,
        "-3.4e38" to true,
        "3.5e38" to false,
        "1e-50" to true,
        "NaN" to false,
        "1e" to false,
    ),
    "double" to listOf(
        "1.7976931348623157e308" to true,
        "1e309" to false,
        "-1E+2" to true,
        "Infinity" to false,
    ),
    "byte" to listOf(
        "" to true,
        "Zm9v" to true,
        "Zm9vYg==" to true,
        "Zm9vYmE=" to true,
        "Zm9vYg=" to false,
        "Zm9vY===" to false,
        "Zm9v\n" to false,
        "Zm=9v" to false,
        "Zm9-" to false,
    ),
    "binary" to listOf("\u0000\uD800" to true),
    "password" to listOf("" to true),
)

class FormatsTest : FunSpec({
    context("every format answers its cases") {
        cases.forEach { (format, formatCases) ->
            test("$format answers its cases") {
                val wrong = formatCases.filter { (value, isValid) ->
                    isValid != (Formats.check(format = format, value = value) == Valid)
                }

                wrong.shouldBeEmpty()
            }
        }
    }

    context("the known formats") {
        test("are every format of JSON Schema and OpenAPI") {
            Formats.names.size shouldBe FORMAT_COUNT
            Formats.names shouldContainAll listOf("date-time", IDN_HOSTNAME, JSON_POINTER, "int64")
        }

        test("are named exactly") {
            Formats.check(format = "Date", value = "2026-01-01") shouldBe FormatCheck.Unknown
            Formats.check(format = "unknown", value = "x") shouldBe FormatCheck.Unknown
        }
    }

    context("the length of a value") {
        test("is checked at the limit") {
            val value = "/" + "a".repeat(Formats.MAXIMUM_VALUE_LENGTH - 1)

            Formats.check(format = JSON_POINTER, value = value) shouldBe Valid
        }

        test("is refused one past the limit without reading the value") {
            val value = "/" + "a".repeat(Formats.MAXIMUM_VALUE_LENGTH)

            Formats.check(format = JSON_POINTER, value = value).shouldBeInstanceOf<FormatCheck.Invalid>()
        }

        test("does not bound base64, binary and password") {
            val value = "QUFB".repeat(Formats.MAXIMUM_VALUE_LENGTH)

            Formats.check(format = "byte", value = value) shouldBe Valid
            Formats.check(format = "binary", value = value) shouldBe Valid
            Formats.check(format = "password", value = value) shouldBe Valid
        }

        test("keeps long hostnames cheap") {
            Formats.check(format = IDN_HOSTNAME, value = "ü".repeat(Formats.MAXIMUM_VALUE_LENGTH)) shouldNotBe Valid
            Formats.check(format = "hostname", value = "a.".repeat(Formats.MAXIMUM_VALUE_LENGTH / 2)) shouldNotBe Valid
        }
    }

    context("a reason") {
        test("never repeats the value") {
            val results = Formats.names.map { format -> Formats.check(format = format, value = HOSTILE) }
            val reasons = results.filterIsInstance<FormatCheck.Invalid>()

            reasons.forEach { invalid -> invalid.reason shouldNotContain "script" }
        }

        test("is part of the equality of an invalid result") {
            val first = Formats.check(format = "date", value = "x")
            val second = Formats.check(format = "date", value = "y")

            first shouldBe second
            first.hashCode() shouldBe second.hashCode()
            first shouldNotBe FormatCheck.Invalid("other")
            first.toString() shouldBe "Invalid(reason=${(first as FormatCheck.Invalid).reason})"
            first.equals("x") shouldBe false
        }

        test("names the results") {
            Valid.toString() shouldBe "Valid"
            FormatCheck.Unknown.toString() shouldBe "Unknown"
        }
    }
})
