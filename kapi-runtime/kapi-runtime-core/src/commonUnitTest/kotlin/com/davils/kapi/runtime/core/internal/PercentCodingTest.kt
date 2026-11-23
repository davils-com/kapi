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

package com.davils.kapi.runtime.core.internal

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private const val SEVEN: String = "7"

class PercentCodingTest : FunSpec({
    context("percent-decoding") {
        test("keeps characters beyond ASCII as UTF-8") {
            PercentCoding.decode("ü") shouldBe "ü"
        }

        test("knows the hexadecimal digits in both cases") {
            listOf('0', '9', 'a', 'f', 'A', 'F').all(PercentCoding::isHex) shouldBe true
            listOf('/', ':', '@', 'G', '`', 'g').any(PercentCoding::isHex) shouldBe false
        }
    }

    context("an extended value") {
        test("is read in UTF-8 and in ISO-8859-1") {
            ExtValue.decode("UTF-8''%C3%BC") shouldBe "ü"
            ExtValue.decode("iso-8859-1'de'%FC") shouldBe "ü"
        }

        test("is refused without three parts, with a character outside attr-char or a malformed escape") {
            ExtValue.decode("UTF-8'x").shouldBeNull()
            ExtValue.decode("UTF-8''a b").shouldBeNull()
            ExtValue.decode("UTF-8''%zz").shouldBeNull()
        }

        test("is refused when it is no UTF-8 or names another charset") {
            ExtValue.decode("UTF-8''%FF").shouldBeNull()
            ExtValue.decode("UTF-16''a").shouldBeNull()
        }
    }

    context("decimal digits") {
        test("divide when the remainder is longer than the divisor") {
            DecimalDigits.isDividedBy(digits = "14", zeros = 0, divisor = SEVEN) shouldBe true
            DecimalDigits.isDividedBy(digits = "15", zeros = 0, divisor = SEVEN) shouldBe false
        }

        test("compare by length before digits") {
            DecimalDigits.compareMagnitude(left = "9", right = "10") shouldBe -1
            DecimalDigits.compareMagnitude(left = "10", right = "9") shouldBe 1
        }
    }
})
