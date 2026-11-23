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

import com.davils.kapi.runtime.core.fieldLimits
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

private const val TOO_HIGH: Int = 1_001

private const val LONG_SUBTYPE: Int = 40

private const val IDENTITY: String = "identity"

private const val GZIP: String = "gzip"

private const val HTML: String = "text/html"

private const val RFC_ACCEPT: String = "text/*;q=0.3, text/plain;q=0.7, text/plain;format=flowed, " +
    "text/plain;format=fixed;q=0.4, */*;q=0.5"

private val html: MediaType = MediaType.parse(HTML)

private val json: MediaType = MediaType.Json

private fun accept(text: String): List<Weighted<MediaRange>> = AcceptFields.parseAccept(text).shouldNotBeNull()

class NegotiationTest : FunSpec({
    context("a quality") {
        test("reads the qvalues of RFC 9110 and writes them shortest") {
            val qualities = listOf("1", "1.", "1.000", "0", "0.5", "0.125", "0.10").map(Quality::parse)

            qualities.map(Quality::toString) shouldBe
                listOf("1", "1", "1", "0", "0.5", "0.125", "0.1")
            Quality.parse("0.001").thousandths shouldBe 1
            Quality.NotAcceptable.isAcceptable shouldBe false
        }

        test("refuses what is no qvalue") {
            listOf("", "2", "1.5", "1.001", "0.1234", "-0", ".5", "0,5", "0.a").forEach { text ->
                Quality.parseOrNull(text).shouldBeNull()
            }
            shouldThrow<IllegalArgumentException> { Quality(TOO_HIGH) }
            shouldThrow<IllegalArgumentException> { Quality.parse("x") }
        }
    }

    context("Accept") {
        test("weighs the media types of the example of RFC 9110 §12.5.1") {
            val accepted = accept(RFC_ACCEPT)
            val expected = mapOf(
                "text/plain;format=flowed" to "1",
                "text/plain" to "0.7",
                HTML to "0.3",
                "image/jpeg" to "0.5",
                "text/plain;format=fixed" to "0.4",
                "text/html;level=3" to "0.3",
            )

            expected.forEach { (type, quality) ->
                Negotiation.qualityOf(accepted, MediaType.parse(type)).toString() shouldBe quality
            }
        }

        test("chooses the offer of the highest quality and the first of equal ones") {
            Negotiation.chooseMediaType(accept("text/html;q=0.5, application/json"), listOf(html, json)) shouldBe json
            Negotiation.chooseMediaType(accept("*/*"), listOf(html, json)) shouldBe html
            Negotiation.chooseMediaType(null, listOf(json, html)) shouldBe json
            Negotiation.chooseMediaType(accept("image/*"), listOf(json)).shouldBeNull()
            Negotiation.chooseMediaType(accept("application/json;q=0, */*"), listOf(json, html)) shouldBe html
        }

        test("skips empty members and ignores the extensions after the weight") {
            val accepted = accept(" , text/html;q=0.5;ext=1 ,, ")

            accepted shouldBe listOf(Weighted(MediaRange.of(HTML), Quality.parse("0.5")))
            accepted.single().toString() shouldBe "text/html;q=0.5"
            accept("") shouldBe emptyList()
        }

        test("refuses a malformed member, a malformed quality and a value above the limits") {
            listOf("text", "text/html;q=2", "text/html;q", "text/html junk", "*/html").forEach { text ->
                AcceptFields.parseAccept(text).shouldBeNull()
            }
            val limits = fieldLimits {
                maxMembers = 2
                maxFieldLength = 40
                maxParameters = 1
            }
            AcceptFields.parseAccept("a/a, b/b", limits).shouldNotBeNull()
            AcceptFields.parseAccept("a/a, b/b, c/c", limits).shouldBeNull()
            AcceptFields.parseAccept("a/" + "a".repeat(LONG_SUBTYPE), limits).shouldBeNull()
            AcceptFields.parseAccept("a/a;x=1", limits).shouldNotBeNull()
            AcceptFields.parseAccept("a/a;x=1;y=2", limits).shouldBeNull()
        }
    }

    context("Accept-Encoding") {
        test("chooses a coding by RFC 9110 §12.5.3") {
            val accepted = AcceptFields.parseAcceptEncoding("gzip;q=1.0, identity; q=0.5, *;q=0").shouldNotBeNull()

            Negotiation.chooseEncoding(accepted, listOf("br", IDENTITY, GZIP)) shouldBe GZIP
            Negotiation.qualityOfEncoding(accepted, "br") shouldBe Quality.NotAcceptable
            Negotiation.qualityOfEncoding(accepted, "IDENTITY").toString() shouldBe "0.5"
        }

        test("keeps identity acceptable unless it is excluded") {
            val empty = AcceptFields.parseAcceptEncoding("").shouldNotBeNull()
            val noIdentity = AcceptFields.parseAcceptEncoding("*;q=0").shouldNotBeNull()

            Negotiation.chooseEncoding(empty, listOf("gzip", IDENTITY)) shouldBe IDENTITY
            Negotiation.chooseEncoding(noIdentity, listOf(IDENTITY)).shouldBeNull()
            Negotiation.chooseEncoding(null, listOf("gzip")) shouldBe "gzip"
        }

        test("refuses a parameter other than the weight") {
            AcceptFields.parseAcceptEncoding("gzip;level=9").shouldBeNull()
            AcceptFields.parseAcceptEncoding("gzip;q=0.5;q=0.1").shouldBeNull()
            AcceptFields.parseAcceptEncoding("g zip").shouldBeNull()
        }
    }

    context("Accept-Language") {
        test("chooses a language by the basic filtering of RFC 4647") {
            val accepted = AcceptFields.parseAcceptLanguage("da, en-gb;q=0.8, en;q=0.7").shouldNotBeNull()

            Negotiation.chooseLanguage(accepted, listOf("en-US", "en-GB", "de")) shouldBe "en-GB"
            Negotiation.qualityOfLanguage(accepted, "en-US").toString() shouldBe "0.7"
            Negotiation.qualityOfLanguage(accepted, "de") shouldBe Quality.NotAcceptable
            Negotiation.chooseLanguage(null, listOf("de")) shouldBe "de"
        }

        test("lets a wildcard cover every tag and a longer range win") {
            val accepted = AcceptFields.parseAcceptLanguage("*;q=0.1, de-CH").shouldNotBeNull()

            Negotiation.chooseLanguage(accepted, listOf("fr", "de-ch-1996")) shouldBe "de-ch-1996"
            Negotiation.qualityOfLanguage(accepted, "fr").toString() shouldBe "0.1"
        }

        test("refuses what is no language range") {
            listOf("123", "toolonglanguage", "en-", "en--gb", "en-toolongsubtag", "en_US").forEach { text ->
                AcceptFields.parseAcceptLanguage(text).shouldBeNull()
            }
        }
    }
})
