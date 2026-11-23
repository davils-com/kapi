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
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.core.source.Source
import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.OpenApi
import com.davils.kapi.spec.openapi.Reading
import com.davils.kapi.spec.openapi.fixture.codes
import com.davils.kapi.spec.openapi.fixture.defined
import com.davils.kapi.spec.openapi.fixture.document
import com.davils.kapi.spec.openapi.fixture.head31
import com.davils.kapi.spec.openapi.fixture.parts
import com.davils.kapi.spec.openapi.fixture.petstoreUri
import com.davils.kapi.spec.openapi.fixture.reading
import com.davils.kapi.spec.openapi.fixture.reading30
import com.davils.kapi.spec.openapi.fixture.reading31
import com.davils.kapi.spec.openapi.fixture.readingWith
import com.davils.kapi.spec.openapi.fixture.schema
import com.davils.kapi.spec.openapi.fixture.yamlOf
import com.davils.kapi.spec.openapi.model.content.Response
import com.davils.kapi.spec.openapi.model.schema.SchemaDefinition
import com.davils.kapi.spec.openapi.resolve
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

private const val PET: String = "    Pet:"

private const val GONE: String = "    Gone:"

private const val SAME: String = "    Same:"

private const val FIRST: String = "    First:"

private const val SECOND: String = "    Second:"

private const val THIRD: String = "    Third:"

private const val IS_GONE: String = "      description: Gone"

private const val IS_END: String = "      description: The end"

private const val THE_END: String = "The end"

private const val TO_GONE: String = "      \$ref: '#/components/responses/Gone'"

private const val TO_FIRST: String = "      \$ref: '#/components/responses/First'"

private const val TO_SECOND: String = "      \$ref: '#/components/responses/Second'"

private const val TO_THIRD: String = "      \$ref: '#/components/responses/Third'"

private const val TO_PET: String = "      \$ref: '#/components/schemas/Pet'"

private const val TO_MODELS: String = "      \$ref: 'models.yaml#/Pet'"

private const val IS_OBJECT: String = "      type: object"

private const val COMPONENTS: String = "components:"

private val schemas: List<String> = listOf(COMPONENTS, "  schemas:")

private val responses: List<String> = listOf(COMPONENTS, "  responses:")

private val chain: List<String> = responses + listOf(FIRST, TO_SECOND, SECOND, TO_THIRD, THIRD, IS_END)

private val models: DocumentUri = DocumentUri("memory:/api/models.yaml")

private val modelsText: String = yamlOf(
    "Pet:",
    "  type: object",
    "  properties:",
    "    owner:",
    "      \$ref: '#/Owner'",
    "Owner:",
    "  type: string",
)

private fun Reading.response(name: String): Referable<Response> {
    val components = document().parts()
    return components.responses.getValue(name)
}

class ReferenceReadingTest : FunSpec({
    context("a reference into another document") {
        test("loads that document and resolves what it points at, and what that refers to in turn") {
            val reading = readingWith(mapOf(models to modelsText), head31 + schemas + listOf(PET, TO_MODELS))
            val document = reading.document()
            val reference = document.schema("Pet").reference.shouldNotBeNull()
            val target = document.resolve(reference).shouldBeInstanceOf<SchemaDefinition>()
            val owner = target.properties.getValue("owner").defined()

            reference.target.document shouldBe models
            reading.sources.map { source -> source.uri } shouldBe listOf(petstoreUri, models)
            document.resolve(owner.reference.shouldNotBeNull()).shouldNotBeNull()
            reading.diagnostics.shouldBeEmpty()
        }

        test("is an error at the reference when that document does not exist") {
            val reading = readingWith(emptyMap(), head31 + schemas + listOf(PET, TO_MODELS))
            val finding = reading.diagnostics.single()
            val location = finding.location.shouldNotBeNull()

            reading.codes() shouldBe listOf(DiagnosticCodes.LOAD_MISSING)
            location.pointer.toString() shouldBe "/components/schemas/Pet/\$ref"
        }
    }

    context("a reference that leads nowhere") {
        test("is an error when nothing is at the pointer") {
            val reading = reading31(schemas + listOf(PET, "      \$ref: '#/components/schemas/Missing'"))

            reading.codes() shouldBe listOf(DiagnosticCodes.REFERENCE_UNRESOLVED)
            reading.diagnostics.single().message shouldContain "points at nothing"
        }

        test("is an error when it is no string") {
            val reading = reading31(responses + listOf(GONE, "      \$ref: 1"))

            reading.codes() shouldBe listOf(DiagnosticCodes.REFERENCE_SYNTAX)
        }

        test("is an error when it is no uri") {
            val reading = reading31(responses + listOf(GONE, "      \$ref: 'a b#/x'"))

            reading.codes() shouldBe listOf(DiagnosticCodes.REFERENCE_SYNTAX)
        }

        test("is an error when its fragment is no JSON pointer") {
            val reading = reading31(responses + listOf(GONE, "      \$ref: '#gone'"))

            reading.codes() shouldBe listOf(DiagnosticCodes.REFERENCE_POINTER)
        }

        test("is an error when it is relative and the document has no place of its own") {
            val lines = head31 + responses + listOf(GONE, "      \$ref: 'models.yaml#/Gone'")
            val reader = OpenApi.reader()
            val source = Source.Text(yamlOf(lines), DocumentUri("urn:petstore"))

            reader.read(source).codes() shouldBe listOf(DiagnosticCodes.REFERENCE_NO_BASE)
        }

        test("is an error when what it points at cannot be read as what is expected") {
            val reading = reading31(
                COMPONENTS,
                "  securitySchemes:",
                "    key:",
                "      \$ref: '#/components/securitySchemes/broken'",
                "    broken:",
                "      type: magic",
            )

            reading.codes() shouldBe listOf(DiagnosticCodes.SECURITY_TYPE)
        }
    }

    context("a target that is used as two kinds") {
        test("is an error at the second use") {
            val shared = listOf("  responses:", GONE, IS_GONE, SAME, TO_GONE)
            val reading = reading31(schemas + listOf(PET, TO_GONE) + shared)

            reading.codes() shouldBe listOf(DiagnosticCodes.REFERENCE_KIND)
            reading.diagnostics.single().message shouldContain "a schema elsewhere and as a response here"
        }
    }

    context("a reference to a reference") {
        test("resolves to what the chain ends at") {
            val reading = reading31(chain)
            val document = reading.document()

            document.resolve(reading.response("First")).shouldNotBeNull().description shouldBe THE_END
            document.resolve(reading.response("Second")).shouldNotBeNull().description shouldBe THE_END
        }

        test("is an error when the chain leads back to itself") {
            val reading = reading31(responses + listOf(FIRST, TO_SECOND, SECOND, TO_FIRST))

            reading.codes().toSet() shouldBe setOf(DiagnosticCodes.REFERENCE_CYCLE)
        }

        test("is an error when the chain is longer than a reading follows") {
            val reading = reading(head31 + chain) { limits { maximumReferenceHops = 1 } }

            reading.codes() shouldBe listOf(DiagnosticCodes.REFERENCE_CHAIN_TOO_LONG)
        }
    }

    context("what stands beside a reference") {
        test("is ignored with a warning in OpenAPI 3.0") {
            val shared = listOf(GONE, IS_GONE, SAME, TO_GONE, "      description: Ignored")
            val nested = listOf("  schemas:", PET, IS_OBJECT, SAME, TO_PET, "      title: Ignored")
            val reading = reading30(responses + shared + nested)
            val sibling = DiagnosticCodes.REFERENCE_SIBLING

            reading.codes() shouldBe listOf(sibling, sibling)
            reading.document()
        }

        test("overrides the summary and the description in OpenAPI 3.1, and is ignored otherwise") {
            val overrides = listOf("      summary: Short", "      description: Long", "      headers: {}")
            val reading = reading31(responses + listOf(GONE, IS_GONE, SAME, TO_GONE) + overrides)
            val same = reading.response("Same").shouldBeInstanceOf<Referable.Referenced>()

            reading.codes() shouldBe listOf(DiagnosticCodes.REFERENCE_SIBLING)
            same.reference.summary shouldBe "Short"
            same.reference.description shouldBe "Long"
        }

        test("is a keyword like any other in a schema of OpenAPI 3.1") {
            val reading = reading31(schemas + listOf(PET, IS_OBJECT, SAME, TO_PET, "      title: Kept"))

            reading.diagnostics.shouldBeEmpty()
            reading.document().schema("Same").title shouldBe "Kept"
        }
    }
})
