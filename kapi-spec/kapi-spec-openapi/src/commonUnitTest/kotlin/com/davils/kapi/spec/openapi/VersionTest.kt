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

package com.davils.kapi.spec.openapi

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class VersionTest : FunSpec({
    context("parseOrNull") {
        test("reads the versions of every family") {
            Version.parseOrNull("3.0.3")?.family shouldBe VersionFamily.V30
            Version.parseOrNull("3.1.0")?.family shouldBe VersionFamily.V31
            Version.parseOrNull("3.2.0")?.family shouldBe VersionFamily.V32
            Version.parseOrNull("3.1.1-rc.1")?.literal shouldBe "3.1.1-rc.1"
        }

        test("returns null for every other version") {
            listOf(
                "", "2.0", "3", "3.3.0", "4.0.0", "3.1.x", "v3.1.0", "3.10.0", "3.1.", "3.1.0-", "3.1.0-r c",
                "3.1.0" +
                    "-".repeat(
                        64,
                    ),
                        ).forEach { text ->
                Version.parseOrNull(text) shouldBe null
            }
        }
    }

    context("a version") {
        test("knows whether it names a patch") {
            Version.parseOrNull("3.1.0")?.hasPatch shouldBe true
            Version.parseOrNull("3.1")?.hasPatch shouldBe false
            Version.parseOrNull("3.1-rc.1")?.hasPatch shouldBe false
        }

        test("equals the version of the same literal") {
            Version.parseOrNull("3.2.0") shouldBe Version.Latest
            Version.parseOrNull("3.2.0").hashCode() shouldBe Version.Latest.hashCode()
        }

        test("renders its literal") {
            Version.Latest.toString() shouldBe Version.Latest.literal
            Version.Latest.family shouldBe VersionFamily.V32
            Version.Upgraded.family shouldBe VersionFamily.V31
            VersionFamily.V30.prefix shouldBe "3.0"
        }
    }
})
