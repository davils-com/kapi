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

private const val HALF: Int = 500

private const val BEYOND: Int = 1_001

private const val TEXT: String = "text"

private const val PLAIN: String = "plain"

private const val ENGLISH: String = "en"

private val half: Quality = Quality(HALF)

private val refusedMediaTypes: List<String> = listOf(
    "text/plain; =x",
    "text/plain; a\"x\"",
    "text/plain; a=\"é\"",
    "text/",
    "/plain",
)

class NegotiationValuesTest : FunSpec({
    context("a media type") {
        refusedMediaTypes.forEach { text ->
            test("refuses '$text'") {
                MediaType.parseOrNull(text).shouldBeNull()
            }
        }

        test("reads an empty parameter at the end") {
            MediaType.parseOrNull("text/plain;") shouldBe MediaType.TextPlain
        }

        test("is made only of concrete parts") {
            shouldThrow<IllegalArgumentException> { MediaType.of(type = TEXT, subtype = "*") }
            shouldThrow<IllegalArgumentException> { MediaType.of(type = TEXT, subtype = "a b") }
            shouldThrow<IllegalArgumentException> { MediaType.of(type = "*", subtype = PLAIN) }
        }

        test("differs in its type, its subtype and its parameters") {
            val plain = MediaType.of(type = TEXT, subtype = PLAIN)

            plain shouldNotBe MediaType.of(type = "image", subtype = PLAIN)
            plain shouldNotBe MediaType.of(type = TEXT, subtype = "html")
            plain shouldNotBe plain.withParameter(name = "charset", value = "utf-8")
            plain.equals(TEXT) shouldBe false
        }

        test("names the types of the framing formats") {
            MediaType.Cbor.essence shouldBe "application/cbor"
            MediaType.NdJson.essence shouldBe "application/x-ndjson"
            MediaType.JsonLines.essence shouldBe "application/jsonl"
            MediaType.JsonSequence.essence shouldBe "application/json-seq"
            MediaType.EventStream.essence shouldBe "text/event-stream"
        }
    }

    context("a media range") {
        test("refuses a wildcard inside the type") {
            MediaRange.parseOrNull("t*/*").shouldBeNull()
        }

        test("differs in its type, its subtype and its parameters") {
            val any = MediaRange.of("text/*")

            any shouldNotBe MediaRange.of("image/*")
            any shouldNotBe MediaRange.of("text/plain")
            any shouldNotBe MediaRange.of("text/*; a=1")
            any.equals(TEXT) shouldBe false
        }
    }

    context("the Accept fields") {
        test("refuse a member without a value") {
            AcceptFields.parseAcceptEncoding("gzip, ;q=1").shouldBeNull()
            AcceptFields.parseAcceptLanguage("en, ;q=1").shouldBeNull()
            AcceptFields.parseAccept("text/plain;, text/html").shouldNotBeNull().size shouldBe 2
        }

        test("refuse a language whose primary subtag is empty or longer than eight letters") {
            AcceptFields.isLanguageRange("-a") shouldBe false
            AcceptFields.isLanguageRange("abcdefghi") shouldBe false
            AcceptFields.isLanguageRange("en-") shouldBe false
            AcceptFields.isLanguageRange("en-GB") shouldBe true
        }
    }

    context("a quality") {
        test("lies between 0 and 1000 thousandths") {
            shouldThrow<IllegalArgumentException> { Quality(-1) }
            shouldThrow<IllegalArgumentException> { Quality(BEYOND) }
        }

        test("is written with its significant decimals") {
            half.toString() shouldBe "0.5"
            Quality.Highest.toString() shouldBe "1"
            Quality.NotAcceptable.toString() shouldBe "0"
            Quality.NotAcceptable.isAcceptable shouldBe false
            half.isAcceptable shouldBe true
        }
    }

    context("a weighted value") {
        test("differs in its value and its quality") {
            val weighted = Weighted(ENGLISH, half)

            weighted shouldBe Weighted(ENGLISH, half)
            weighted.hashCode() shouldBe Weighted(ENGLISH, half).hashCode()
            weighted shouldNotBe Weighted("de", half)
            weighted shouldNotBe Weighted(ENGLISH)
            weighted.equals(ENGLISH) shouldBe false
        }
    }
})
