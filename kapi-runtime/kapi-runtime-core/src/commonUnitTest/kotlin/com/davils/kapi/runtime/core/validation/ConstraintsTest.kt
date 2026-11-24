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

package com.davils.kapi.runtime.core.validation

import com.davils.kapi.runtime.core.pattern.Pattern
import com.davils.kapi.runtime.core.pattern.patternLimits
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotContainDuplicates
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith

private const val SIX: Long = 6L

private const val EXPLOSIVE_LENGTH: Int = 10_000

private const val FOUR: Long = 4L

private const val SEVEN: Long = 7L

private const val THIRD: Double = 0.3

private const val TENTH: Double = 0.1

private const val QUARTER: Double = 0.25

private const val COLLIDING_BITS: Int = 12

private const val COLLIDING: Int = 1 shl COLLIDING_BITS

private fun decimal(text: String): Decimal = Decimal.parse(text)

class ConstraintsTest : FunSpec({
    context("length") {
        test("counts characters and not the units they are stored in") {
            Constraints.length("") shouldBe 0
            Constraints.length("ab") shouldBe 2
            Constraints.length("a😀") shouldBe 2
            Constraints.length("\uD83D") shouldBe 1
        }
    }

    context("isMultipleOf") {
        test("divides whole numbers") {
            Constraints.isMultipleOf(value = SIX, factor = 2L) shouldBe true
            Constraints.isMultipleOf(value = SIX, factor = FOUR) shouldBe false
            Constraints.isMultipleOf(value = SIX, factor = 0L) shouldBe false
        }

        test("divides decimals the way they are written") {
            Constraints.isMultipleOf(value = THIRD, factor = TENTH) shouldBe true
            Constraints.isMultipleOf(value = THIRD, factor = QUARTER) shouldBe false
            Constraints.isMultipleOf(value = 1e-5, factor = 1e-6) shouldBe true
            Constraints.isMultipleOf(value = decimal("12.5"), factor = decimal("2.5")) shouldBe true
        }

        test("holds for nothing that is not a number or has no factor") {
            Constraints.isMultipleOf(value = Double.NaN, factor = TENTH) shouldBe false
            Constraints.isMultipleOf(value = THIRD, factor = Double.POSITIVE_INFINITY) shouldBe false
            Constraints.isMultipleOf(value = THIRD, factor = 0.0) shouldBe false
        }
    }

    context("a decimal") {
        test("is the same number however it is written") {
            decimal("1.0") shouldBe decimal("1")
            decimal("10E-1") shouldBe decimal("1")
            decimal("-0") shouldBe decimal("0")
            decimal("1.50").hashCode() shouldBe decimal("1.5").hashCode()
            decimal("1.5") shouldNotBe decimal("-1.5")
            decimal("1").equals("1") shouldBe false
        }

        test("is written in its shortest plain or scientific form") {
            listOf("0", "-12.50", "0.001", "1E3", "1E+30", "1.5E-30", "123.456").map { text ->
                decimal(text).toString()
            } shouldBe listOf("0", "-12.5", "0.001", "1000", "1E30", "15E-31", "123.456")
            Decimal.of(-SEVEN).toString() shouldBe "-7"
        }

        test("compares exactly") {
            decimal("0.1") shouldBeLessThan decimal("0.10000000000000000001")
            decimal("-2") shouldBeLessThan decimal("-1.5")
            decimal("1E2") shouldBeGreaterThan decimal("99.999")
            decimal("0").compareTo(decimal("0")) shouldBe 0
            decimal("-1").sign shouldBe -1
            decimal("0").sign shouldBe 0
            decimal("2E1").isInteger shouldBe true
            decimal("2.5").isInteger shouldBe false
        }

        test("divides exactly with large digits and exponents") {
            decimal("123456789012345678901234567890").isMultipleOf(decimal("1234567890")) shouldBe true
            decimal("123456789012345678901234567891").isMultipleOf(decimal("1234567890")) shouldBe false
            decimal("1E900").isMultipleOf(decimal("7")) shouldBe false
            decimal("7E900").isMultipleOf(decimal("7")) shouldBe true
            decimal("0").isMultipleOf(decimal("3")) shouldBe true
            decimal("0.5").isMultipleOf(decimal("1")) shouldBe false
            decimal("5").isMultipleOf(decimal("0")) shouldBe false
        }

        test("refuses what is no JSON number and what exceeds its limits") {
            val malformed =
                listOf("", "-", "01", "1.", ".5", "+1", "1e", "1e+", "0x1", "1 ", "NaN", "Infinity", "1E1001")

            malformed.forEach { text ->
                Decimal.parseOrNull(text).shouldBeNull()
            }
            Decimal.parseOrNull("1E1000") shouldBe decimal("1E1000")
            Decimal.parseOrNull("1E99999999").shouldBeNull()
            Decimal.parseOrNull("1" + "0".repeat(Decimal.MAX_TEXT_LENGTH)).shouldBeNull()
            Decimal.ofOrNull(Double.NaN).shouldBeNull()
            shouldThrow<IllegalArgumentException> { Decimal.parse("x") }
        }
    }

    context("matches") {
        test("finds the pattern anywhere in the text, as JSON Schema does") {
            val pattern = Pattern.compileOrThrow("[0-9]{3}")

            Constraints.matches("ab123cd", pattern) shouldBe true
            Constraints.matches("12", pattern) shouldBe false
            Constraints.matches("", Pattern.compileOrThrow("^$")) shouldBe true
        }

        test("refuses a text longer than the input limit of the pattern instead of reading it") {
            val pattern = Pattern.compileOrThrow("a", patternLimits { maximumInputLength = 2 })

            Constraints.matches("aa", pattern) shouldBe true
            Constraints.matches("aaa", pattern) shouldBe false
        }

        test("stays linear on input that makes backtracking engines explode") {
            val pattern = Pattern.compileOrThrow("^(a+)+$")

            Constraints.matches("a".repeat(EXPLOSIVE_LENGTH) + "!", pattern) shouldBe false
        }
    }

    context("isUnique") {
        test("tells whether every item appears once") {
            Constraints.isUnique(listOf("a", "b")) shouldBe true
            Constraints.isUnique(listOf("a", "a")) shouldBe false
            Constraints.isUnique(emptyList<String>()) shouldBe true
        }

        test("sorts items of one comparable class, so numbers equal in value repeat and colliding text stays fast") {
            val halves = listOf("Aa", "BB")
            val colliding = List(COLLIDING) { index ->
                (0 until COLLIDING_BITS).joinToString("") { bit -> halves[index shr bit and 1] }
            }

            Constraints.isUnique(listOf(decimal("1.0"), decimal("1.00"))) shouldBe false
            Constraints.isUnique(listOf(SIX, FOUR, SEVEN)) shouldBe true
            Constraints.isUnique(listOf(SIX, FOUR, SIX)) shouldBe false
            Constraints.isUnique(colliding) shouldBe true
            Constraints.isUnique(colliding + colliding.last()) shouldBe false
        }

        test("compares other items by equals") {
            Constraints.isUnique(listOf(1, "1")) shouldBe true
            Constraints.isUnique(listOf(listOf(1), listOf(1))) shouldBe false
            Constraints.isUnique(listOf(null, null)) shouldBe false
        }
    }

    context("the codes of the constraints") {
        test("are unique and share one namespace") {
            ConstraintViolationCodes.all.shouldNotContainDuplicates()
            ConstraintViolationCodes.all.forEach { code -> code.value shouldStartWith "kapi.runtime.constraint." }
        }
    }
})
