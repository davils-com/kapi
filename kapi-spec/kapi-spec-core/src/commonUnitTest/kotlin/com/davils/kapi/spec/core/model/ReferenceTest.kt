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

package com.davils.kapi.spec.core.model

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.spec.core.model.fixture.node
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

private const val PET: String = "#/components/schemas/Pet"

private val api: DocumentUri = DocumentUri("memory:/api.yaml")

private val schemas: JsonPointer = JsonPointer.of("components", "schemas", "Pet")

private val pet: Target = Target(api, schemas)

private val reference: Reference = Reference(PET, pet)

class ReferenceTest : FunSpec({
    context("a target") {
        test("equals the target of the same document and pointer") {
            Target(api, schemas) shouldBe pet
            Target(api, schemas).hashCode() shouldBe pet.hashCode()
            Target(DocumentUri("memory:/other.yaml"), schemas) shouldNotBe pet
            Target(api, JsonPointer.ROOT) shouldNotBe pet
            val text: Any = "$pet"
            (pet == text) shouldBe false
        }

        test("is written as the document and the pointer") {
            pet.toString() shouldBe "memory:/api.yaml#/components/schemas/Pet"
        }
    }

    context("a reference") {
        test("equals the reference of the same text, target, summary and description, wherever it was read") {
            val read = Reference(PET, pet, location = Location(api, JsonPointer.of("paths")))

            read shouldBe reference
            read.hashCode() shouldBe reference.hashCode()
            Reference("#/other", pet) shouldNotBe reference
            Reference(PET, Target(api, JsonPointer.ROOT)) shouldNotBe reference
            Reference(PET, pet, summary = "A pet") shouldNotBe reference
            Reference(PET, pet, description = "A pet") shouldNotBe reference
            val text: Any = PET
            (reference == text) shouldBe false
        }

        test("shows its text escaped and its target") {
            val hostile = Reference("#/a\u001b[31m\nb", pet)

            reference.toString() shouldContain PET
            reference.toString() shouldContain "-> $pet"
            hostile.toString() shouldNotContain "\u001b"
            hostile.toString() shouldNotContain "\n"
        }
    }

    context("a referable") {
        test("is inline or referenced, and equals what holds the same") {
            val inline = Referable.Inline(node("pet"))
            val referenced = Referable.Referenced(reference)

            inline shouldBe Referable.Inline(node("pet"))
            inline.hashCode() shouldBe Referable.Inline(node("pet")).hashCode()
            inline shouldNotBe Referable.Inline(node("toy"))
            referenced shouldBe Referable.Referenced(Reference(PET, pet))
            referenced.hashCode() shouldBe reference.hashCode()
            referenced shouldNotBe Referable.Referenced(Reference("#/other", pet))
            (inline as Referable<*> == referenced) shouldBe false
            (referenced as Referable<*> == inline) shouldBe false
        }

        test("names what it holds") {
            Referable.Inline(node("pet")).toString() shouldBe "Inline(Node(built))"
            Referable.Referenced(reference).toString() shouldBe "Referenced($reference)"
        }
    }
})
