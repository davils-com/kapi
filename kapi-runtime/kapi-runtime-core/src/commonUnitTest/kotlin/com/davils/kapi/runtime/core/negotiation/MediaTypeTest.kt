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

package com.davils.kapi.runtime.core.negotiation

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

private const val PROBLEM: String = "application/problem+json"

private const val JSON: String = "application/json"

private const val TOO_LONG: Int = 5_000

private const val TEXT_FAMILY: String = "text/*"

private const val TEXT_PLAIN: String = "text/plain"

private const val QUOTED_BOUNDARY: String = """multipart/form-data; boundary="a b\"c""""

private const val MANY_PARAMETERS: Int = 65

class MediaTypeTest : FunSpec({
    context("a media type") {
        test("is read with its parameters, names and charset in lower case, other values as written") {
            val type = MediaType.parse(" Text/HTML ; Charset=\"UTF-8\" ;; level=One ")

            type.type shouldBe "text"
            type.subtype shouldBe "html"
            type.charset shouldBe "utf-8"
            type.parameter("LEVEL") shouldBe "One"
            type.parameter("missing").shouldBeNull()
            type.essence shouldBe "text/html"
            type.toString() shouldBe "text/html; charset=utf-8; level=One"
        }

        test("names its structured syntax suffix") {
            MediaType.parse(PROBLEM).suffix shouldBe "json"
            MediaType.parse(JSON).suffix.shouldBeNull()
            MediaType.ProblemJson.essence shouldBe PROBLEM
        }

        test("writes a value that is no token as a quoted string") {
            MediaType.parse(QUOTED_BOUNDARY).toString() shouldBe QUOTED_BOUNDARY
        }

        test("is equal by its parts and changes only into a copy") {
            val type = MediaType.parse(JSON)
            val withCharset = type.withParameter(name = "Charset", value = "UTF-8")

            withCharset shouldBe MediaType.parse("application/json; charset=utf-8")
            withCharset.hashCode() shouldBe MediaType.parse("application/json;charset=utf-8").hashCode()
            withCharset.withoutParameters() shouldBe type
            type shouldNotBe withCharset
            type.equals(JSON) shouldBe false
            MediaType.of(type = "Application", subtype = "JSON") shouldBe type
        }

        test("refuses what is no media type") {
            val refused = listOf(
                "", "json", "application/", "/json", "a b/c", "*/*", TEXT_FAMILY, "text/plain; a",
                "text/plain; a=\"open", "text/plain; a=1; A=2", "text/plain; a=\"é\"", "text/plain extra",
                "text/plain; a=b\r\nX: y",
            )
            refused.forEach { text -> MediaType.parseOrNull(text).shouldBeNull() }
            MediaType.parseOrNull("text/plain;").shouldNotBeNull()
            MediaType.parseOrNull("x/" + "a".repeat(TOO_LONG)).shouldBeNull()
            MediaType.parseOrNull("a/b" + ";p=1".repeat(MANY_PARAMETERS)).shouldBeNull()
            MediaType.parseOrNull("a/b" + ";p=1".repeat(1)).shouldNotBeNull()
            shouldThrow<IllegalArgumentException> { MediaType.parse("json") }.message shouldContain "no media type"
        }

        test("refuses parts and values it could not write") {
            shouldThrow<IllegalArgumentException> { MediaType.of(type = "text", subtype = "*") }
            shouldThrow<IllegalArgumentException> {
                MediaType.of(
                    type = "text",
                    subtype = "plain",
                    parameters = listOf("a" to "1", "A" to "2"),
                )
            }
            shouldThrow<IllegalArgumentException> { MediaType.parse(JSON).withParameter(name = "a", value = "x\r\ny") }
            shouldThrow<IllegalArgumentException> { MediaType.parse(JSON).withParameter(name = "a b", value = "x") }
        }
    }

    context("a media range") {
        test("matches the type it names, whatever its case and its parameters") {
            MediaRange.Json.matches(JSON) shouldBe true
            MediaRange.Json.matches("Application/JSON; charset=utf-8") shouldBe true
            MediaRange.of("application/json; charset=utf-8").matches(JSON) shouldBe true
            MediaRange.Json.matches(PROBLEM) shouldBe false
        }

        test("matches everything when it is a wildcard") {
            MediaRange.Any.matches("image/png") shouldBe true
            MediaRange.Any.toString() shouldBe "*/*"
        }

        test("matches a family") {
            val text = MediaRange.of(TEXT_FAMILY)

            text.matches(TEXT_PLAIN) shouldBe true
            text.matches("image/png") shouldBe false
        }

        test("matches a structured suffix") {
            val json = MediaRange.of("application/*+json")

            json.matches(PROBLEM) shouldBe true
            json.matches(JSON) shouldBe false
            json.matches("text/problem+json") shouldBe false
            MediaRange.of("*/*+json").matches(PROBLEM) shouldBe true
        }

        test("matches no missing type and no malformed one") {
            MediaRange.Any.matches(null as String?) shouldBe false
            MediaRange.Any.matches("nonsense") shouldBe false
        }

        test("includes a type only with every parameter of the range") {
            val range = MediaRange.of("text/plain; format=flowed")

            range.includes(MediaType.parse("text/plain; format=flowed; charset=utf-8")) shouldBe true
            range.includes(MediaType.parse(TEXT_PLAIN)) shouldBe false
            MediaRange.of(MediaType.parse(TEXT_PLAIN)) shouldBe MediaRange.of(TEXT_PLAIN)
        }

        test("ranks the more specific range first") {
            val ranges = listOf("*/*", TEXT_FAMILY, "application/*+json", TEXT_PLAIN, "text/plain; a=1")
            val ranks = ranges.map { text -> MediaRange.of(text).specificity }

            ranks shouldBe ranks.sorted()
            ranks.toSet().size shouldBe ranges.size
        }

        test("refuses what is no range") {
            listOf("", "json", "application/", "/json", "a b/c", "*/json", "a*/b").forEach { text ->
                val failure = shouldThrow<IllegalArgumentException> { MediaRange.of(text) }

                failure.message shouldContain "is no media range"
            }
            MediaRange.of(TEXT_FAMILY).hashCode() shouldBe MediaRange.of("TEXT/*").hashCode()
            MediaRange.of(TEXT_FAMILY).equals(TEXT_FAMILY) shouldBe false
        }
    }
})
