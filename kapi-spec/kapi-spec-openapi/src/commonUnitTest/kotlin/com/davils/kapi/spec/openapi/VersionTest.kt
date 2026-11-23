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
import io.kotest.matchers.shouldNotBe

private const val V30: String = "3.0.3"

private const val V31: String = "3.1.0"

private const val V32: String = "3.2.0"

private const val LONGEST: Int = 64

private const val PRE_RELEASE: String = "$V31-"

class VersionTest : FunSpec({
    context("parseOrNull") {
        test("reads the versions of every family") {
            Version.parseOrNull(V30)?.family shouldBe VersionFamily.V30
            Version.parseOrNull(V31)?.family shouldBe VersionFamily.V31
            Version.parseOrNull(V32)?.family shouldBe VersionFamily.V32
            Version.parseOrNull("3.1.1-rc.1")?.literal shouldBe "3.1.1-rc.1"
        }

        test("returns null for every other version") {
            val others = listOf(
                "", "2.0", "3", "3.3.0", "4.0.0", "3.1.x", "v3.1.0", "3.10.0", "3.1.", PRE_RELEASE, "3.1.0-r c",
                "3.1.0-r_c", "3.1.a", "3.1.0-ä", "3.1-", "3.2.0+build",
            )
            others.forEach { text -> Version.parseOrNull(text) shouldBe null }
        }

        test("reads a literal at its longest length and refuses one character more") {
            val longest = PRE_RELEASE + "a".repeat(LONGEST - PRE_RELEASE.length)
            val overlong = "$longest-"

            Version.parseOrNull(longest)?.literal shouldBe longest
            Version.parseOrNull(overlong) shouldBe null
        }

        test("reads every character of a pre-release") {
            val literal = "$PRE_RELEASE.az-AZ.09"

            Version.parseOrNull(literal)?.family shouldBe VersionFamily.V31
        }
    }

    context("a version") {
        test("knows whether it names a patch") {
            Version.parseOrNull(V31)?.hasPatch shouldBe true
            Version.parseOrNull("3.1")?.hasPatch shouldBe false
            Version.parseOrNull("3.1-rc.1")?.hasPatch shouldBe false
        }

        test("equals the version of the same literal") {
            Version.parseOrNull(V32) shouldBe Version.Latest
            Version.parseOrNull(V32).hashCode() shouldBe Version.Latest.hashCode()
            Version.parseOrNull(V31) shouldBe Version.parseOrNull(V31)
            Version.Latest shouldNotBe Version.Upgraded
            val text: Any = V32
            (Version.Latest == text) shouldBe false
        }

        test("renders its literal") {
            Version.Latest.toString() shouldBe Version.Latest.literal
            Version.Latest.family shouldBe VersionFamily.V32
            Version.Upgraded.family shouldBe VersionFamily.V31
            VersionFamily.V30.prefix shouldBe "3.0"
        }
    }
})
