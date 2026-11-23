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

package com.davils.kapi.spec.core.value

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val TOO_LONG: Int = 513

private const val ANSWER: Long = 42L

private const val HALF_OF_FIVE: Double = 2.5

private const val SEVEN: Long = 7L

private fun number(text: String): NumberValue = checkNotNull(NumberValue.parseOrNull(text))

class NumberValueTest : FunSpec({
    context("parseOrNull") {
        test("reads what JSON calls a number and keeps its literal") {
            listOf("0", "-1", "12", "1.50", "-0.25", "1e3", "2.5E-3", "1e+2").forEach { text ->
                number(text).literal shouldBe text
                number(text).toString() shouldBe text
            }
        }

        test("refuses what JSON does not call a number") {
            listOf("", "01", "+1", ".5", "1.", "0x1F", "0o17", ".inf", ".nan", "1_000", "one", " 1").forEach { text ->
                NumberValue.parseOrNull(text) shouldBe null
            }
        }

        test("refuses a literal or an exponent that is too large to work with") {
            NumberValue.parseOrNull("1".repeat(TOO_LONG)) shouldBe null
            NumberValue.parseOrNull("1e5000") shouldBe null
            NumberValue.parseOrNull("1e-5000") shouldBe null
            number("1e4000").isIntegral shouldBe true
        }
    }

    context("a number") {
        test("compares by its value and not by how it was written") {
            number("1.0") shouldBe number("1")
            number("1.0").hashCode() shouldBe number("1").hashCode()
            number("1e2") shouldBe number("100")
            number("1") shouldNotBe number("1.5")
            number("2") shouldBeGreaterThan number("1.5")
            number("1").equals("1") shouldBe false
        }

        test("knows whether it is integral") {
            number("3").isIntegral shouldBe true
            number("3.00").isIntegral shouldBe true
            number("3.5").isIntegral shouldBe false
        }

        test("converts to a long only when it is one") {
            number("42").toLongOrNull() shouldBe ANSWER
            number("4.2e1").toLongOrNull() shouldBe ANSWER
            number("-9223372036854775808").toLongOrNull() shouldBe Long.MIN_VALUE
            number("9223372036854775808").toLongOrNull() shouldBe null
            number("-9223372036854775809").toLongOrNull() shouldBe null
            number("0.5").toLongOrNull() shouldBe null
        }

        test("converts to an int only when it is one") {
            number("2147483647").toIntOrNull() shouldBe Int.MAX_VALUE
            number("2147483648").toIntOrNull() shouldBe null
            number("-2147483649").toIntOrNull() shouldBe null
            number("0.5").toIntOrNull() shouldBe null
        }

        test("converts to a double and keeps its literal") {
            number("2.5").toDouble() shouldBe HALF_OF_FIVE
            number("2.50").literal shouldBe "2.50"
        }
    }

    context("of") {
        test("wraps a long") {
            NumberValue.of(SEVEN) shouldBe number("7")
            NumberValue.of(-SEVEN).literal shouldBe "-7"
        }
    }
})
