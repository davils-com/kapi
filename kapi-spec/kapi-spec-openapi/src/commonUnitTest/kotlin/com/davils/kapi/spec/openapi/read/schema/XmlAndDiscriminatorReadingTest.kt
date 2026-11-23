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

package com.davils.kapi.spec.openapi.read.schema

import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.OpenApi
import com.davils.kapi.spec.openapi.fixture.codes
import com.davils.kapi.spec.openapi.fixture.document
import com.davils.kapi.spec.openapi.fixture.readable
import com.davils.kapi.spec.openapi.fixture.reading31
import com.davils.kapi.spec.openapi.fixture.reading32
import com.davils.kapi.spec.openapi.fixture.rewritten
import com.davils.kapi.spec.openapi.fixture.schema
import com.davils.kapi.spec.openapi.model.schema.XmlNodeType
import com.davils.kore.core.validation.violation.Severity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private val schemas: List<String> = listOf(
    "components:",
    "  schemas:",
    "    Pet:",
    "      type: object",
    "      discriminator: {propertyName: kind, mapping: {cat: Cat}, defaultMapping: Other}",
    "      xml: {name: pet, nodeType: element}",
    "    Cat: {type: object}",
    "    Other: {type: object}",
)

private fun xml(fields: String): List<String> = listOf("components:", "  schemas:", "    Pet: {xml: {$fields}}")

class XmlAndDiscriminatorReadingTest : FunSpec({
    context("a schema of OpenAPI 3.2") {
        test("reads the default mapping of its discriminator and the node type of its xml") {
            val reading = reading32(schemas)
            val pet = reading.document().schema("Pet")

            reading.diagnostics.shouldBeEmpty()
            pet.discriminator.shouldNotBeNull().defaultMapping shouldBe "Other"
            pet.xml.shouldNotBeNull().nodeType shouldBe XmlNodeType.Element
        }

        test("is written with them and reads back") {
            val document = reading32(schemas).document()
            val written = OpenApi.write(document)

            written shouldContain "defaultMapping: Other"
            written shouldContain "nodeType: element"
            document.rewritten() shouldBe document
        }

        test("reads every node type") {
            XmlNodeType.entries.forEach { type ->
                val reading = reading32(xml("nodeType: ${type.keyword}"))

                reading.diagnostics.shouldBeEmpty()
                reading.document().schema("Pet").xml?.nodeType shouldBe type
            }
            XmlNodeType.fromKeyword("Element").shouldBeNull()
        }

        test("reports a node type it does not know") {
            val reading = reading32(xml("nodeType: comment"))

            reading.codes() shouldBe listOf(DiagnosticCodes.SCHEMA_XML_NODE_TYPE)
        }

        test("warns about attribute and wrapped beside a node type") {
            val reading = reading32(xml("nodeType: attribute, attribute: true, wrapped: false"))
            val xml = reading.document().schema("Pet").xml.shouldNotBeNull()

            reading.codes() shouldBe List(2) { DiagnosticCodes.SCHEMA_XML_DEPRECATED }
            reading.diagnostics.map { finding -> finding.severity }.toSet() shouldBe setOf(Severity.Warning)
            xml.isAttribute shouldBe true
        }

        test("keeps attribute and wrapped without a node type") {
            reading32(xml("attribute: true")).diagnostics.shouldBeEmpty()
        }
    }

    context("a schema before OpenAPI 3.2") {
        test("has no default mapping and no node type") {
            val reading = reading31(schemas)
            val pet = reading.readable().schema("Pet")

            reading.codes() shouldBe List(2) { DiagnosticCodes.FIELD_VERSION }
            pet.discriminator?.defaultMapping.shouldBeNull()
            pet.xml?.nodeType.shouldBeNull()
        }
    }
})
