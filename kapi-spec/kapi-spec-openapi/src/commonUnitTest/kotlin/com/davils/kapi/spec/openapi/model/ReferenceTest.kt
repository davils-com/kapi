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

package com.davils.kapi.spec.openapi.model

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.diagnostic.Location
import com.davils.kapi.spec.core.model.Element
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.core.model.Reference
import com.davils.kapi.spec.core.model.Target
import com.davils.kapi.spec.openapi.model.info.ContactBuilder
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val TEXT: String = "#/components/schemas/Pet"

private const val SUMMARY: String = "A pet"

private val pet: Target = Target(
    document = DocumentUri.MEMORY,
    pointer = JsonPointer.of("components", "schemas", "Pet"),
)

private val here: Location = Location(DocumentUri.MEMORY, JsonPointer.ROOT)

class ReferenceTest : FunSpec({
    context("a target") {
        test("renders the document and the pointer") {
            pet.toString() shouldBe "memory:/document#/components/schemas/Pet"
        }
    }

    context("a reference") {
        test("keeps what was written, where it points and what it overrides") {
            val reference = Reference(
                text = TEXT,
                target = pet,
                summary = SUMMARY,
                description = "A pet of the store",
                location = here,
            )

            reference.text shouldBe TEXT
            reference.target shouldBe pet
            reference.summary shouldBe SUMMARY
            reference.description shouldBe "A pet of the store"
            reference.location shouldBe here
            reference.toString() shouldBe "Reference(\"$TEXT\" -> $pet)"
        }

        test("equals a reference to the same target wherever it was written") {
            val reference = Reference(TEXT, pet, location = here)
            val same = Reference(TEXT, pet)

            reference shouldBe same
            reference.hashCode() shouldBe same.hashCode()
            reference shouldNotBe Reference(TEXT, pet, summary = SUMMARY)
            reference.equals(TEXT) shouldBe false
        }
    }

    context("a referable") {
        test("is either the element itself or a reference to it") {
            val contact = ContactBuilder().build()
            val inline: Referable<Element> = Referable.Inline(contact)
            val reference: Referable<Element> =
                Referable.Referenced(Reference(TEXT, pet))

            inline shouldBe Referable.Inline(contact)
            reference shouldNotBe inline
        }
    }
})
