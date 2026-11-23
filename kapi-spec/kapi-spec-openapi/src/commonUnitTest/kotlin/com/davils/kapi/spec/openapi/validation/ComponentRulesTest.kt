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
import com.davils.kapi.spec.openapi.fixture.reading31
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private val oracle: Regex = Regex("[a-zA-Z0-9._-]+")

private val names: List<String> = listOf(
    "",
    "Pet",
    "pet.v1",
    "pet_v1-beta",
    "...",
    "-",
    "Pet Store",
    "Pet/Store",
    "Pet~1",
    "Pét",
    "Pet\n",
    "😀",
    "١",
    "Pet$",
)

class ComponentRulesTest : FunSpec({
    context("the name of a component") {
        test("is what the regular expression of a component name accepts") {
            names.forEach { name -> ComponentRules.isName(name) shouldBe oracle.matches(name) }
        }

        test("is reported when it holds another character") {
            val reading = reading31("components:", "  schemas:", "    'Pet Store': {type: object}")

            reading.codes() shouldBe listOf(DiagnosticCodes.COMPONENT_NAME)
        }
    }
})
