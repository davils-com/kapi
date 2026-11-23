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

package com.davils.kapi.runtime.core.structured

import com.davils.kapi.runtime.core.fieldLimits
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString

private const val FIVE: Long = 5L

private const val DECIMAL: Long = 4_500L

private const val DATE: Long = 1_659_578_233L

private const val LARGEST: Long = 999_999_999_999_999L

private const val TWO: Int = 2

private val roundTrips: List<String> = listOf(
    "sugar, tea, rum",
    "(\"foo\" \"bar\"), (\"baz\"), (\"bat\" \"one\"), ()",
    "(\"foo\";a=1;b=2);lvl=5, (\"bar\" \"baz\");lvl=1",
    "abc;a=1;b=2;cde_456, (ghi;jk=4 l);q=\"9\";r=w",
    "1;a;b=?0",
    "-12.345, 0.0, 999999999999.999, -999999999999999",
    ":cHJldGVuZCB0aGlzIGlzIGJpbmFyeSBjb250ZW50Lg==:",
    "@1659578233",
    "%\"This is intended for display to %c3%bc%c3%bcsers.\"",
    "\"hello \\\"world\\\" \\\\ \"",
    "*foo, text/html, a:b",
)

private val badLists: List<String> = listOf(
    "a,", "a,,b", ",a", "a b", "((1))", "(1 2", "(1)(2)", "a;", "a;A=1", "a;=1", "\"unclosed", "\"a\\x\"",
    "\"tab\tin\"", "1234567890123456", "1234567890123.5", "1.2345", "1.", "-", "--1", ":not base64:", ":YQ",
    "?2", "?", "@1.5", "%\"%C3%BC\"", "%\"%c3\"", "%\"%zz\"", "%nope", "Ä", "#x", "a=",
)

private fun list(text: String): List<Member> = StructuredFields.parseListOrNull(text).shouldNotBeNull()

class StructuredFieldsTest : FunSpec({
    context("the examples of RFC 9651") {
        roundTrips.forEach { text ->
            test("reads and writes back $text") {
                StructuredFields.serializeList(list(text)) shouldBe text
            }
        }

        test("read an item with parameters") {
            val item = StructuredFields.parseItemOrNull("5; foo=bar").shouldNotBeNull()

            item.value shouldBe BareItem.Integer(FIVE)
            item.parameters["foo"] shouldBe BareItem.Token("bar")
            StructuredFields.serialize(item) shouldBe "5;foo=bar"
        }

        test("read a dictionary with byte sequences, flags and inner lists") {
            val dictionary = StructuredFields.parseDictionaryOrNull(
                "en=\"Applepie\", da=:w4ZibGV0w6ZydGUK:, a=?0, b, c;foo=9, rating=1.5, inner=(1 2)",
            ).shouldNotBeNull()

            dictionary["en"] shouldBe Item(BareItem.Text("Applepie"))
            dictionary["da"].shouldBeInstanceOf<Item>().value.shouldBeInstanceOf<BareItem.Bytes>()
                .value shouldBe "Æbletærte\n".encodeToByteString()
            dictionary["b"] shouldBe Item(BareItem.Flag(true))
            dictionary["inner"].shouldBeInstanceOf<InnerList>().items.size shouldBe TWO
            StructuredFields.serialize(dictionary) shouldBe
                "en=\"Applepie\", da=:w4ZibGV0w6ZydGUK:, a=?0, b, c;foo=9, rating=1.5, inner=(1 2)"
        }

        test("read the kinds of bare items") {
            StructuredFields.parseItemOrNull("4.5").shouldNotBeNull().value shouldBe BareItem.Decimal(DECIMAL)
            StructuredFields.parseItemOrNull("@1659578233").shouldNotBeNull().value shouldBe BareItem.Date(DATE)
            StructuredFields.parseItemOrNull("%\"f%c3%bc%c3%bc\"").shouldNotBeNull().value shouldBe
                BareItem.DisplayText("füü")
            StructuredFields.parseItemOrNull(":YQ:").shouldNotBeNull().value shouldBe
                BareItem.Bytes(ByteString(byteArrayOf('a'.code.toByte())))
        }
    }

    context("parsing") {
        test("keeps the last value of a key at the place of the first") {
            val dictionary = StructuredFields.parseDictionaryOrNull("a=1, b=2, a=3").shouldNotBeNull()
            val item = StructuredFields.parseItemOrNull("x;p=1;q=2;p=3").shouldNotBeNull()

            StructuredFields.serialize(dictionary) shouldBe "a=3, b=2"
            StructuredFields.serialize(item) shouldBe "x;p=3;q=2"
        }

        test("ignores spaces around the value and tabs around commas") {
            list("  a,\tb  ") shouldBe listOf(Item(BareItem.Token("a")), Item(BareItem.Token("b")))
            list("") shouldBe emptyList()
            StructuredFields.parseDictionaryOrNull("").shouldNotBeNull().entries shouldBe emptyList()
        }

        badLists.forEach { text ->
            test("refuses the list '$text'") {
                StructuredFields.parseListOrNull(text).shouldBeNull()
            }
        }

        test("refuses what is no item or dictionary") {
            StructuredFields.parseItemOrNull("a, b").shouldBeNull()
            StructuredFields.parseItemOrNull("").shouldBeNull()
            StructuredFields.parseDictionaryOrNull("A=1").shouldBeNull()
            StructuredFields.parseDictionaryOrNull("a=1,").shouldBeNull()
        }

        test("keeps its limits at the limit and refuses one past it") {
            val limits = fieldLimits {
                maxMembers = 3
                maxParameters = 2
                maxFieldLength = 20
            }

            StructuredFields.parseListOrNull("a, b, c", limits).shouldNotBeNull()
            StructuredFields.parseListOrNull("a, b, c, d", limits).shouldBeNull()
            StructuredFields.parseListOrNull("(1 2 3)", limits).shouldNotBeNull()
            StructuredFields.parseListOrNull("(1 2 3 4)", limits).shouldBeNull()
            StructuredFields.parseItemOrNull("a;b;c", limits).shouldNotBeNull()
            StructuredFields.parseItemOrNull("a;b;c;d", limits).shouldBeNull()
            StructuredFields.parseDictionaryOrNull("a, b, c, d", limits).shouldBeNull()
            StructuredFields.parseListOrNull("\"" + "x".repeat(20) + "\"", limits).shouldBeNull()
        }
    }

    context("the types") {
        test("refuse values RFC 9651 cannot carry, so no writer can inject a line break") {
            shouldThrow<IllegalArgumentException> { BareItem.Text("a\r\nX-Injected: 1") }
            shouldThrow<IllegalArgumentException> { BareItem.Text("é") }
            shouldThrow<IllegalArgumentException> { BareItem.Token("1a") }
            shouldThrow<IllegalArgumentException> { BareItem.Token("a b") }
            shouldThrow<IllegalArgumentException> { BareItem.Integer(LARGEST + 1) }
            shouldThrow<IllegalArgumentException> { BareItem.Decimal(LARGEST + 1) }
            shouldThrow<IllegalArgumentException> { BareItem.Date(-LARGEST - 1) }
            shouldThrow<IllegalArgumentException> { BareItem.DisplayText("\uD800") }
            shouldThrow<IllegalArgumentException> { Parameters.of("A" to BareItem.Flag(true)) }
            shouldThrow<IllegalArgumentException> { Parameters.of("a" to BareItem.Flag(true), "a" to BareItem.Flag(false)) }
            shouldThrow<IllegalArgumentException> { Dictionary(listOf("" to Item(BareItem.Flag(true)))) }
            shouldThrow<IllegalArgumentException> {
                Dictionary(listOf("a" to Item(BareItem.Flag(true)), "a" to Item(BareItem.Flag(true))))
            }
        }

        test("write their serialized forms") {
            BareItem.Integer(LARGEST).toString() shouldBe "999999999999999"
            BareItem.Decimal(-DECIMAL).toString() shouldBe "-4.5"
            BareItem.Decimal(1L).toString() shouldBe "0.001"
            BareItem.DisplayText("a\"%ü\n").toString() shouldBe "%\"a%22%25%c3%bc%0a\""
            Item(BareItem.Flag(true), Parameters.of("a" to BareItem.Integer(1L))).toString() shouldBe "?1;a=1"
            InnerList(emptyList(), Parameters.of("a" to BareItem.Flag(true))).toString() shouldBe "();a"
            Parameters.Empty.isEmpty shouldBe true
        }

        test("are equal by their values") {
            val values: List<BareItem> = listOf(
                BareItem.Integer(1L), BareItem.Decimal(1L), BareItem.Text("a"), BareItem.Token("a"),
                BareItem.Bytes(ByteString()), BareItem.Flag(true), BareItem.Date(1L), BareItem.DisplayText("a"),
            )
            values.forEach { value ->
                value shouldBe value
                value.hashCode() shouldBe value.hashCode()
                value shouldNotBe Item(value)
            }
            values.toSet().size shouldBe values.size
            Item(BareItem.Integer(1L)).hashCode() shouldBe Item(BareItem.Integer(1L)).hashCode()
            InnerList(emptyList()) shouldBe InnerList(emptyList())
            InnerList(emptyList()).hashCode() shouldBe InnerList(emptyList()).hashCode()
            Parameters.Empty.hashCode() shouldBe Parameters(emptyList()).hashCode()
            Dictionary(emptyList()) shouldBe Dictionary(emptyList())
            Dictionary(emptyList()).hashCode() shouldBe emptyList<Pair<String, Member>>().hashCode()
            Dictionary(emptyList())["a"].shouldBeNull()
        }
    }
})
