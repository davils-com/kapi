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

package com.davils.kapi.runtime.core.cache

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val AGE: Long = 60L

private const val ACCEPT: String = "accept"

private const val PUBLIC: String = "public"

private fun control(text: String): CacheControl = CacheControl.parseOrNull(text).shouldNotBeNull()

class CacheValuesTest : FunSpec({
    context("Cache-Control") {
        test("has no delta for a directive without an argument or with an empty one") {
            control("max-age").maxAge.shouldBeNull()
            control("max-age=\"\"").maxAge.shouldBeNull()
            control(PUBLIC).maxAge.shouldBeNull()
        }

        test("has no field names for a directive without an argument and skips empty ones") {
            control("no-cache").noCacheFields shouldBe emptyList()
            control(PUBLIC).privateFields shouldBe emptyList()
            control("no-cache=\", Accept,\"").noCacheFields shouldBe listOf(ACCEPT)
        }

        test("differs in its directives") {
            val control = CacheControl.of(CacheDirective.maxAge(AGE))

            control shouldNotBe CacheControl.of(CacheDirective.maxAge(0L))
            control.equals(CacheDirective.maxAge(AGE)) shouldBe false
        }
    }

    context("a directive") {
        test("differs in its name and its argument") {
            val directive = CacheDirective.maxAge(AGE)

            directive shouldNotBe CacheDirective.minFresh(AGE)
            directive shouldNotBe CacheDirective.maxAge(0L)
            directive.equals(AGE) shouldBe false
            CacheDirective.flag(PUBLIC).hashCode() shouldBe CacheDirective(PUBLIC).hashCode()
        }
    }

    context("Vary") {
        test("differs in its fields and in any") {
            val vary = Vary(listOf(ACCEPT))

            vary shouldBe Vary(listOf("Accept"))
            vary shouldNotBe Vary(listOf("origin"))
            vary shouldNotBe Vary(listOf(ACCEPT), isAny = true)
            vary.equals(ACCEPT) shouldBe false
        }
    }
})
