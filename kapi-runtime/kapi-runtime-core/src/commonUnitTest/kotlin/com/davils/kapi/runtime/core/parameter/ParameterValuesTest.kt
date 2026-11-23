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

package com.davils.kapi.runtime.core.parameter

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val NAME: String = "color"

private const val BLUE: String = "blue"

private val form: ParameterFormat = ParameterFormat(name = NAME, style = ParameterStyle.Form, isExploded = false)

private val blue: ParameterValue = ParameterValue.Text(BLUE)

class ParameterValuesTest : FunSpec({
    context("cookies in the form style") {
        test("keep a comma between items") {
            form.cookieTextOrNull(ParameterValue.Items(listOf(BLUE, "a b"))) shouldBe "color=blue,a%20b"
        }

        test("skip pairs without a name or without a value") {
            form.readCookie("=x; color; color=blue", ParameterShape.Text) shouldBe ParameterReading.Present(blue)
        }

        test("are malformed when several values are malformed") {
            form.readCookie("color=%zz; other=%zz", ParameterShape.Text) shouldBe ParameterReading.Malformed
        }
    }

    context("a format") {
        test("differs in each of its parts") {
            form shouldNotBe ParameterFormat(name = "other", style = ParameterStyle.Form, isExploded = false)
            form shouldNotBe ParameterFormat(name = NAME, style = ParameterStyle.Simple, isExploded = false)
            form shouldNotBe ParameterFormat(name = NAME, style = ParameterStyle.Form, isExploded = true)
            form shouldNotBe ParameterFormat(
                name = NAME,
                style = ParameterStyle.Form,
                isExploded = false,
                isReservedAllowed = true,
            )
            form.equals(NAME) shouldBe false
        }
    }

    context("a value") {
        test("differs by kind and by content") {
            val items = ParameterValue.Items(listOf(BLUE))
            val fields = ParameterValue.Fields(mapOf(NAME to BLUE))

            items.equals(blue) shouldBe false
            items shouldNotBe ParameterValue.Items(emptyList())
            fields.equals(items) shouldBe false
            fields shouldNotBe ParameterValue.Fields(emptyMap())
            blue.equals(items) shouldBe false
        }
    }

    context("an entry") {
        test("differs in its name and its value") {
            val entry = ParameterEntry(name = NAME, value = BLUE)

            entry shouldNotBe ParameterEntry(name = "other", value = BLUE)
            entry shouldNotBe ParameterEntry(name = NAME, value = "red")
            entry.equals(NAME) shouldBe false
        }
    }
})
