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

package com.davils.kapi.spec.schema.internal

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

private const val LIMIT: Int = 1024

class EnginesTest : FunSpec({
    context("the patterns of a schema") {
        test("search the string like ECMA-262 does") {
            val pattern = Patterns.compile("^[a-z]+\\d$", maximumSourceLength = LIMIT, maximumInputLength = LIMIT)

            pattern.shouldNotBeNull().invoke("abc1") shouldBe true
            pattern("abc") shouldBe false
        }

        test("are refused when they need backtracking or exceed the source limit") {
            Patterns.compile("(a)\\1", maximumSourceLength = LIMIT, maximumInputLength = LIMIT).shouldBeNull()
            Patterns
                .compile("a".repeat(LIMIT + 1), maximumSourceLength = LIMIT, maximumInputLength = LIMIT)
                .shouldBeNull()
        }
    }

    context("the format assertions of a schema") {
        test("tell valid, invalid and unknown formats apart") {
            FormatAssertions.check(format = "ipv4", value = "192.0.2.1") shouldBe true
            FormatAssertions.check(format = "ipv4", value = "192.0.2.01") shouldBe false
            FormatAssertions.check(format = "x-unknown", value = "anything").shouldBeNull()
        }
    }
})
