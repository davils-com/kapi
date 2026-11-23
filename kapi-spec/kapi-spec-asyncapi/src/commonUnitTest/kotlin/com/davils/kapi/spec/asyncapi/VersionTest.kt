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

package com.davils.kapi.spec.asyncapi

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val LATEST: String = "3.0.0"

class VersionTest : FunSpec({
    context("a version") {
        test("is read with its family, with or without a patch and a pre-release") {
            listOf(LATEST, "3.0", "3.0.1", "3.0.0-rc1", "3.0.0-beta.2-x").forEach { text ->
                val version = Version.parseOrNull(text).shouldNotBeNull()

                version.family shouldBe VersionFamily.V30
                version.literal shouldBe text
                version.toString() shouldBe text
            }
        }

        test("knows whether it names a patch") {
            Version.parseOrNull(LATEST).shouldNotBeNull().hasPatch shouldBe true
            Version.parseOrNull("3.0").shouldNotBeNull().hasPatch shouldBe false
            Version.parseOrNull("3.0-rc1").shouldNotBeNull().hasPatch shouldBe false
        }

        test("refuses every text that is no version of AsyncAPI 3.0") {
            val refused = listOf(
                "2.6.0", "3.1.0", "3", "3.0.", "3.0.x", "3.0.0-", "3.0.0-a_b", "30.0",
                "3.0" +
                    "0".repeat(
                        64,
                    ),
            )

            refused.forEach { text -> Version.parseOrNull(text).shouldBeNull() }
        }

        test("is equal to a version of the same text") {
            Version.parseOrNull(LATEST) shouldBe Version.Latest
            Version.parseOrNull(LATEST).hashCode() shouldBe Version.Latest.hashCode()
            Version.parseOrNull("3.0.1") shouldNotBe Version.Latest
            VersionFamily.V30.isAtLeast(VersionFamily.V30) shouldBe true
        }
    }
})
