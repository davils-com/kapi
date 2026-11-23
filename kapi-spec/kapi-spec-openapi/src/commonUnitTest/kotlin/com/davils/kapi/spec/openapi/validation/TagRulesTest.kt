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

package com.davils.kapi.spec.openapi.validation

import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.fixture.codes
import com.davils.kapi.spec.openapi.fixture.reading32
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

private const val TAGS: String = "tags:"

class TagRulesTest : FunSpec({
    context("the parent of a tag") {
        test("names another tag of the document") {
            val reading = reading32(TAGS, "  - {name: pets, kind: nav}", "  - {name: cats, parent: pets}")

            reading.diagnostics.shouldBeEmpty()
        }

        test("is an error when it names no tag") {
            val reading = reading32(TAGS, "  - {name: cats, parent: pets}")
            val location = reading.diagnostics.single().location.shouldNotBeNull()

            reading.codes() shouldBe listOf(DiagnosticCodes.TAG_PARENT_UNKNOWN)
            location.pointer.toString() shouldBe "/tags/0"
        }

        test("is an error when the parents lead back to the tag") {
            val reading = reading32(
                TAGS,
                "  - {name: a, parent: b}",
                "  - {name: b, parent: c}",
                "  - {name: c, parent: a}",
                "  - {name: d, parent: a}",
                "  - {name: e, parent: e}",
            )
            val places = reading.diagnostics.map { finding -> finding.location?.pointer.toString() }

            reading.codes().toSet() shouldBe setOf(DiagnosticCodes.TAG_PARENT_CYCLE)
            places shouldBe listOf("/tags/0", "/tags/1", "/tags/2", "/tags/4")
        }

        test("is checked for a long chain of tags without a cycle") {
            val chain = (0 until 2000).map { index -> "  - {name: t$index, parent: t${index + 1}}" }
            val reading = reading32(listOf(TAGS) + chain + "  - {name: t2000}")

            reading.diagnostics.shouldBeEmpty()
        }
    }
})
