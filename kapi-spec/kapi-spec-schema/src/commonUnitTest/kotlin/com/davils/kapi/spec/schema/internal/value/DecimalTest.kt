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

package com.davils.kapi.spec.schema.internal.value

import com.davils.kapi.spec.schema.internal.Meter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private val FREE: Meter = Meter { }

private fun decimal(text: String): Decimal = checkNotNull(Decimal.parse(text)) { "$text is no decimal" }

class DecimalTest : FunSpec({
    test("reads exactly the number grammar of JSON") {
        listOf(
            "0",
            "-0",
            "1",
            "-1.5",
            "1e3",
            "1E+3",
            "2.5e-3",
            "100",
        ).forEach { text -> Decimal.parse(text) shouldNotBe null }
        listOf("", "01", "+1", ".5", "1.", "1e", "1e+", "--1", "0x1", "1_0", "NaN", "Infinity", " 1").forEach { text ->
            Decimal.parse(text) shouldBe null
        }
        Decimal.parse("1".repeat(Decimal.MAXIMUM_LENGTH + 1)) shouldBe null
        Decimal.parse("1e1234567890123456") shouldBe null
        Decimal.parse("1e000000000000000000001") shouldNotBe null
    }

    test("compares and equals by value") {
        decimal("1") shouldBe decimal("1.0")
        decimal("1").hashCode() shouldBe decimal("10e-1").hashCode()
        decimal("-0") shouldBe decimal("0")
        decimal("2") shouldBeGreaterThan decimal("1.99999999999999999999")
        decimal("-2") shouldBeLessThan decimal("-1")
        decimal("1e400") shouldBeGreaterThan decimal("9e399")
        decimal("-1e-400") shouldBeLessThan decimal("0")
        decimal("12.5").toString() shouldBe "125e-1"
        decimal("0.0").canonical shouldBe "0"
        decimal("-3").sign shouldBe -1
    }

    test("knows integers and plain integers") {
        decimal("1.0").isInteger shouldBe true
        decimal("1.0").isPlain shouldBe false
        decimal("1e2").isPlain shouldBe false
        decimal("12").isPlain shouldBe true
        decimal("1.5").isInteger shouldBe false
    }

    test("divides exactly") {
        val cases = listOf(
            "10" to "2" to true, "10" to "4" to false, "0.3" to "0.1" to true, "0.07" to "0.01" to true,
            "1e308" to "0.123456789" to false, "1e308" to "0.5" to true, "0" to "7" to true,
            "7" to "7e-400" to true, "1e-400" to "7e-400" to false, "100" to "1e3" to false,
            "123456789012345678901234567890" to "123456789012345678901" to false,
            "246913578024691357802" to "123456789012345678901" to true,
            "40" to "8" to true, "1e3" to "16" to false, "1e4" to "16" to true, "125" to "25" to true,
        )
        cases.forEach { (operands, expected) ->
            val (value, divisor) = operands
            Divisibility.isMultiple(value = decimal(value), divisor = decimal(divisor), meter = FREE) shouldBe expected
        }
    }

    test("counts code points and escapes pointer tokens") {
        Texts.codePoints("a💩b") shouldBe "a💩b".length - 1
        Texts.codePoints("\uD83D") shouldBe 1
        Texts.escapedToken("a/b~c") shouldBe "a~1b~0c"
    }
})
