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

private const val BEYOND: Long = 1_000_000_000_000_000L

private const val KEY: String = "a"

private const val HIGH: Char = '\uD83D'

private const val LOW: Char = '\uDE00'

private val one: BareItem = BareItem.Integer(1L)

private val two: BareItem = BareItem.Integer(2L)

private val refusedItems: List<String> = listOf(
    "!",
    "\"a\\",
    "%\"abc",
    "%\"%c\"",
    "%\"\u0001\"",
    "(",
    "(a ",
    "a;{=1",
    "a;A=1",
)

class StructuredValuesTest : FunSpec({
    context("reading") {
        refusedItems.forEach { text ->
            test("refuses the item '${text.filter { char -> char >= ' ' }}'") {
                StructuredFields.parseListOrNull(text).shouldBeNull()
            }
        }

        test("refuses an empty item") {
            StructuredFields.parseItemOrNull("").shouldBeNull()
        }

        test("reads both flags") {
            StructuredFields.parseItemOrNull("?1").shouldNotBeNull().value shouldBe BareItem.Flag(true)
            StructuredFields.parseItemOrNull("?0").shouldNotBeNull().value shouldBe BareItem.Flag(false)
        }

        test("reads keys that start with a star and hold symbols") {
            val item = StructuredFields.parseItemOrNull("a;*k_e-y.1*=1").shouldNotBeNull()

            item.parameters["*k_e-y.1*"] shouldBe one
        }

        test("refuses an inner list above the limit of members") {
            val limits = fieldLimits { maxMembers = 1 }

            StructuredFields.parseListOrNull("(1)", limits).shouldNotBeNull()
            StructuredFields.parseListOrNull("(1 2)", limits).shouldBeNull()
        }
    }

    context("a bare item") {
        test("keeps integers, decimals and dates within 15 digits") {
            shouldThrow<IllegalArgumentException> { BareItem.Integer(BEYOND) }
            shouldThrow<IllegalArgumentException> { BareItem.Integer(-BEYOND) }
            shouldThrow<IllegalArgumentException> { BareItem.Decimal(BEYOND) }
            shouldThrow<IllegalArgumentException> { BareItem.Decimal(-BEYOND) }
            shouldThrow<IllegalArgumentException> { BareItem.Date(BEYOND) }
            shouldThrow<IllegalArgumentException> { BareItem.Date(-BEYOND) }
        }

        test("refuses an empty token") {
            shouldThrow<IllegalArgumentException> { BareItem.Token("") }
        }

        test("keeps surrogate pairs of a display string and refuses lone surrogates") {
            BareItem.DisplayText("$HIGH$LOW").toString() shouldBe "%\"%f0%9f%98%80\""
            shouldThrow<IllegalArgumentException> { BareItem.DisplayText(HIGH.toString()) }
            shouldThrow<IllegalArgumentException> { BareItem.DisplayText("$HIGH$KEY") }
            shouldThrow<IllegalArgumentException> { BareItem.DisplayText(LOW.toString()) }
        }
    }

    context("the members") {
        test("differ in their values and their parameters") {
            val parameters = Parameters.of(KEY to one)
            val item = Item(one, parameters)
            val inner = InnerList(listOf(item), parameters)

            item shouldNotBe Item(two, parameters)
            item shouldNotBe Item(one)
            item.equals(one) shouldBe false
            inner shouldNotBe InnerList(listOf(Item(two)), parameters)
            inner shouldNotBe InnerList(listOf(item))
            inner.equals(item) shouldBe false
            parameters.equals(item) shouldBe false
            Dictionary(listOf(KEY to item)).equals(parameters) shouldBe false
        }
    }
})
