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

package com.davils.kapi.spec.openapi.read.reference

import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.OpenApi
import com.davils.kapi.spec.openapi.fixture.codes
import com.davils.kapi.spec.openapi.fixture.document
import com.davils.kapi.spec.openapi.fixture.head31
import com.davils.kapi.spec.openapi.fixture.head32
import com.davils.kapi.spec.openapi.fixture.petstoreUri
import com.davils.kapi.spec.openapi.fixture.readable
import com.davils.kapi.spec.openapi.fixture.readingWith
import com.davils.kapi.spec.openapi.fixture.rewritten
import com.davils.kapi.spec.openapi.fixture.schema
import com.davils.kapi.spec.openapi.fixture.yamlOf
import com.davils.kapi.spec.openapi.model.schema.SchemaDefinition
import com.davils.kapi.spec.openapi.resolve
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

private const val REMOTE: String = "https://api.example.com/v2/openapi.yaml"

private val nested: DocumentUri = DocumentUri("memory:/api/v2/models.yaml")

private val models: DocumentUri = DocumentUri("memory:/api/models.yaml")

private val modelText: String = yamlOf("Pet: {type: object}")

private fun schemas(vararg pet: String): List<String> =
    listOf("components:", "  schemas:", "    Pet: {${pet.joinToString()}}", "    Toy: {type: string}")

private fun documentWith(self: String, vararg pet: String): List<String> =
    head32 + "\$self: '$self'" + schemas(*pet)

class SelfReadingTest : FunSpec({
    context("the \$self of a document") {
        test("is read and written back") {
            val reading = readingWith(emptyMap(), documentWith(REMOTE, "type: object"))
            val document = reading.document()

            reading.diagnostics.shouldBeEmpty()
            document.self shouldBe REMOTE
            OpenApi.write(document) shouldContain "\$self: $REMOTE"
            document.rewritten() shouldBe document
        }

        test("names the document itself, so a reference to it stays in the document") {
            val reading = readingWith(emptyMap(), documentWith(REMOTE, "\$ref: '$REMOTE#/components/schemas/Toy'"))
            val document = reading.document()
            val reference = document.schema("Pet").reference.shouldNotBeNull()

            reading.diagnostics.shouldBeEmpty()
            reference.target.document shouldBe petstoreUri
            document.resolve(reference).shouldBeInstanceOf<SchemaDefinition>()
        }

        test("keeps a reference by fragment in the document") {
            val reading = readingWith(emptyMap(), documentWith(REMOTE, "\$ref: '#/components/schemas/Toy'"))
            val reference = reading.document().schema("Pet").reference.shouldNotBeNull()

            reading.diagnostics.shouldBeEmpty()
            reference.target.document shouldBe petstoreUri
        }

        test("is the base of relative references, which a reading still refuses to load from the network") {
            val reading = readingWith(mapOf(models to modelText), documentWith(REMOTE, "\$ref: 'models.yaml#/Pet'"))
            val reference = reading.readable().schema("Pet").reference.shouldNotBeNull()

            reading.codes() shouldBe listOf(DiagnosticCodes.LOAD_REMOTE)
            reference.target.document shouldBe DocumentUri("https://api.example.com/v2/models.yaml")
        }

        test("resolves against the address the document was read from when it is relative") {
            val documents = mapOf(nested to modelText, models to modelText)
            val reading = readingWith(documents, documentWith("v2/openapi.yaml", "\$ref: 'models.yaml#/Pet'"))
            val reference = reading.document().schema("Pet").reference.shouldNotBeNull()

            reading.diagnostics.shouldBeEmpty()
            reference.target.document shouldBe nested
            reading.document().resolve(reference).shouldNotBeNull()
        }

        test("is an error when it holds a fragment or is no URI reference") {
            listOf("$REMOTE#top", "http://[broken").forEach { self ->
                val reading = readingWith(emptyMap(), documentWith(self, "\$ref: '#/components/schemas/Toy'"))
                val reference = reading.readable().schema("Pet").reference.shouldNotBeNull()

                reading.codes() shouldBe listOf(DiagnosticCodes.DOCUMENT_SELF)
                reference.target.document shouldBe petstoreUri
            }
        }

        test("is the base of a document that a reference leads to") {
            val shared = yamlOf(
                "openapi: 3.2.0",
                "\$self: 'v2/shared.yaml'",
                "Pet: {\$ref: 'models.yaml#/Pet'}",
            )
            val documents = mapOf(DocumentUri("memory:/api/shared.yaml") to shared, nested to modelText)
            val reading = readingWith(documents, head32 + schemas("\$ref: 'shared.yaml#/Pet'"))

            reading.diagnostics.shouldBeEmpty()
            reading.document().targets.keys.map { target -> target.document } shouldBe listOf(
                DocumentUri("memory:/api/shared.yaml"),
                nested,
            )
        }

        test("is no field before OpenAPI 3.2 and changes no reference there") {
            val lines = head31 + "\$self: '$REMOTE'" + schemas("\$ref: '#/components/schemas/Toy'")
            val reading = readingWith(emptyMap(), lines)
            val document = reading.readable()

            reading.codes() shouldBe listOf(DiagnosticCodes.FIELD_VERSION)
            document.self shouldBe null
            document.schema("Pet").reference?.target?.document shouldBe petstoreUri
        }
    }
})
