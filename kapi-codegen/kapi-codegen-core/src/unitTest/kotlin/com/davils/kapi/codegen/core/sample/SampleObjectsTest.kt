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


package com.davils.kapi.codegen.core.sample

import com.davils.kapi.codegen.core.Generation
import com.davils.kapi.codegen.core.fixture.UNMATCHED
import com.davils.kapi.codegen.core.fixture.mockGenerationOf
import com.davils.kapi.codegen.core.fixture.samplesOf
import com.davils.kapi.codegen.core.fixture.schemas
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf

private const val REF: String = "\$ref: '#/components/schemas/"
private const val MAPPING: String = "mapping: {cat: '#/components/schemas/Cat', hound: '#/components/schemas/Dog'}"

private val animal: List<String> = listOf(
    "Animal:",
    "  oneOf: [{${REF}Cat'}, {${REF}Dog'}]",
    "  discriminator: {propertyName: kind, $MAPPING}",
)

private fun member(name: String, kind: String): List<String> =
    listOf("$name:", "  type: object", "  required: [kind]", "  properties:", "    kind: $kind")

private fun paying(expiry: String, required: String = ""): List<String> = listOf(
    "Pet:",
    "  type: object",
    "  required: [$required]",
    "  properties:",
    "    card: {type: string}",
    "    expiry: $expiry",
    "  dependentRequired: {card: [expiry]}",
)

class SampleObjectsTest : FunSpec({
    context("a member of a union") {
        test("carries the value the discriminator maps to it") {
            val text = "{type: string}"
            val samples = samplesOf(animal + member("Cat", text) + member("Dog", text))

            samples shouldContain "kind = \"cat\","
            samples shouldContain "kind = \"hound\","
        }

        test("carries that value as the entry of an enumeration") {
            val cat = member("Cat", "{type: string, enum: [tiger, cat]}")
            val dog = member("Dog", "{type: string, enum: [wolf]}")
            val samples = samplesOf(animal + cat + dog)

            samples shouldContain "kind = PetstoreCatKind.CAT,"
            samples shouldContain "kind = PetstoreDogKind.WOLF,"
        }
    }

    context("a property that requires another") {
        test("is filled together with the one it requires") {
            val samples = samplesOf(paying("{type: string}"))

            samples shouldContain "card = \"string\","
            samples shouldContain "expiry = \"string\","
        }

        test("is left out when there is no value for the one it requires") {
            samplesOf(paying(UNMATCHED)) shouldContain "get() = PetstorePet()"
        }

        test("leaves the model without a sample when it must be there itself") {
            val generation = mockGenerationOf(schemas(paying(UNMATCHED, required = "card")))
            val generated = generation.shouldBeInstanceOf<Generation.Generated>()

            generated.diagnostics.single().message shouldContain "what its dependentRequired names"
            generated.files.single { file -> file.path.endsWith("Samples.kt") }.content shouldNotContain "val pet"
        }
    }
})
