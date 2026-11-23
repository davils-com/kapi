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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val ONE: String = "1"

private val one: Decimal = Decimal.parse(ONE)

private val minusOne: Decimal = Decimal.parse("-1")

private val zero: Decimal = Decimal.parse("0")

class DecimalOrderTest : FunSpec({
    context("ordering") {
        test("orders by sign before magnitude") {
            (minusOne < one) shouldBe true
            (one > minusOne) shouldBe true
            (zero < one) shouldBe true
            (minusOne < zero) shouldBe true
            (Decimal.parse("-2") < minusOne) shouldBe true
        }
    }

    context("equality") {
        test("differs in sign, digits and exponent") {
            one shouldNotBe minusOne
            one shouldNotBe Decimal.parse("2")
            one shouldNotBe Decimal.parse("10")
            one.equals(ONE) shouldBe false
        }
    }

    context("exponents") {
        test("read zeros and a plus sign") {
            Decimal.parse("1e000") shouldBe one
            Decimal.parse("1E+0") shouldBe one
            Decimal.parse("1e-0") shouldBe one
        }

        test("refuse an exponent beyond 1000 in both directions and one without digits") {
            Decimal.parseOrNull("1e1001").shouldBeNull()
            Decimal.parseOrNull("1e-1001").shouldBeNull()
            Decimal.parseOrNull("1e").shouldBeNull()
            Decimal.parseOrNull("1e-").shouldBeNull()
        }
    }
})
