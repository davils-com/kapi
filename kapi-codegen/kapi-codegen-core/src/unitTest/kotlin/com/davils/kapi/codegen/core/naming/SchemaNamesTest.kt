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

package com.davils.kapi.codegen.core.naming

import com.davils.kapi.codegen.core.CodegenCodes
import com.davils.kapi.codegen.core.fixture.generator
import com.davils.kapi.codegen.core.fixture.read
import com.davils.kapi.codegen.core.fixture.schemas
import com.davils.kapi.codegen.core.pipeline.Findings
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private const val STRING: String = "  type: string"

private suspend fun named(
    lines: List<String>,
    findings: Findings = Findings(),
    prefix: String = "",
): Map<String, String> {
    val options = generator { typePrefix = prefix }.options
    return SchemaNames(options, NameTable(findings)).name(read(schemas(lines)).document)
}

class SchemaNamesTest : FunSpec({
    context("name") {
        test("names every schema after the prefix, the api and its key, in the order of the keys") {
            val names = named(listOf("pet-store:", STRING, "Order:", STRING))

            names.toList() shouldBe listOf("Order" to "PetstoreOrder", "pet-store" to "PetstorePetStore")
        }

        test("follows a name the document declares") {
            named(listOf("Pet:", STRING, "  x-kapi-name: Animal")) shouldBe mapOf("Pet" to "PetstoreAnimal")
        }

        test("leaves the prefix out when there is none") {
            named(listOf("Pet:", STRING), prefix = "") shouldBe mapOf("Pet" to "PetstorePet")
        }

        test("falls back to the key when the declared name is no identifier") {
            named(listOf("Pet:", STRING, "  x-kapi-name: 1st")) shouldBe mapOf("Pet" to "PetstorePet")
        }

        test("names nothing without schemas") {
            val findings = Findings()

            val names = SchemaNames(generator().options, NameTable(findings))

            names.name(read(listOf("paths: {}")).document) shouldBe emptyMap()
            findings.sorted().shouldBeEmpty()
        }
    }

    context("a collision") {
        test("is an error that names both schemas and how to part them") {
            val findings = Findings()
            named(listOf("pet_store:", STRING, "PetStore:", STRING), findings)
            val diagnostic = findings.sorted().single()

            diagnostic.code shouldBe CodegenCodes.NAME_COLLISION
            diagnostic.message shouldContain "The schema pet_store becomes PetstorePetStore"
            diagnostic.message shouldContain "which The schema PetStore at /components/schemas/PetStore already is"
            diagnostic.message shouldContain "x-kapi-name"
        }

        test("is found when two names differ in case only") {
            val findings = Findings()
            named(listOf("Pet:", STRING, "  x-kapi-name: PEt", "Other:", STRING, "  x-kapi-name: Pet"), findings)

            findings.hasErrors shouldBe true
        }

        test("is gone once one of them has a name of its own") {
            val findings = Findings()
            named(listOf("pet_store:", STRING, "  x-kapi-name: Shop", "PetStore:", STRING), findings)

            findings.hasErrors shouldBe false
        }
    }
})
