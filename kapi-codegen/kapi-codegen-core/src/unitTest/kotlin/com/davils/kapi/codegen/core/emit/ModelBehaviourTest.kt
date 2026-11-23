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

package com.davils.kapi.codegen.core.emit

import com.davils.kapi.codegen.core.fixture.KotlinCompilation
import com.davils.kapi.codegen.core.fixture.Scenarios
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val SCENARIO: String = "com.example.petstore.Scenario"

private val behaviour: List<String> = listOf(
    "equal true",
    "same hash true",
    "differs false",
    "copy PetstoreCat(name=felix, kind=cat, lives=9)",
    "kept PetstoreCat(name=tom, kind=cat, lives=9)",
    "other type false",
    "null false",
    "masked PetstoreAccountWrite(email=a@b.c, password=****)",
    "nan true",
    "zero false",
    "valid true",
    "invalid [kapi.runtime.constraint.min-length]",
)

class ModelBehaviourTest : FunSpec({
    context("a generated model, a regular class") {
        test("is equal by its values, copied with named arguments and shown with sensitive values masked") {
            val result = KotlinCompilation.loaded(Scenarios.files("models")) { loader ->
                val type = loader.loadClass(SCENARIO)
                val scenario = type.getConstructor().newInstance()
                type.getMethod("run").invoke(scenario)
            }

            result shouldBe behaviour
        }
    }
})
