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

package com.davils.kapi.spec.asyncapi.read

import com.davils.kapi.spec.asyncapi.DiagnosticCodes
import com.davils.kapi.spec.asyncapi.Document
import com.davils.kapi.spec.asyncapi.fixture.codes
import com.davils.kapi.spec.asyncapi.fixture.document
import com.davils.kapi.spec.asyncapi.fixture.readable
import com.davils.kapi.spec.asyncapi.fixture.readingOf
import com.davils.kapi.spec.asyncapi.model.message.Message
import com.davils.kapi.spec.asyncapi.model.schema.Schema
import com.davils.kapi.spec.asyncapi.model.schema.SchemaFormat
import com.davils.kapi.spec.core.model.Referable
import com.davils.kapi.spec.core.value.Value
import com.davils.kore.core.validation.violation.Severity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private fun Document.schema(name: String): Schema {
    val components = components.shouldNotBeNull()
    return components.schemas
        .getValue(name)
        .shouldBeInstanceOf<Referable.Inline<Schema>>()
        .value
}

private suspend fun formatOf(text: String): SchemaFormat {
    val reading = readingOf(
        """
            |components:
            |  schemas:
            |    Any:
            |      schemaFormat: '$text'
            |      schema: {}
        """.trimMargin(),
    )
    return reading.readable().schema("Any").format
}

class SchemaReadingTest : FunSpec({
    context("a schema without a format") {
        test("is a schema of AsyncAPI kept as it was written") {
            val document = readingOf(
                """
                    |components:
                    |  schemas:
                    |    Name:
                    |      type: string
                    |      maxLength: 10
                    |    Anything: true
                """.trimMargin(),
            ).document()
            val name = document.schema("Name")

            name.format shouldBe SchemaFormat.AsyncApi
            name.schemaFormat shouldBe null
            name.definition.shouldBeInstanceOf<Value.Mapping>()["type"] shouldBe Value.Text("string")
            document.schema("Anything").definition shouldBe Value.Flag(true)
        }

        test("is refused when it is neither an object nor a boolean") {
            readingOf("components:\n  schemas:\n    Name: text").codes() shouldBe listOf(DiagnosticCodes.FIELD_TYPE)
        }

        test("resolves the references inside it, but not those in data or among the names of properties") {
            val document = readingOf(
                """
                    |components:
                    |  schemas:
                    |    Pet:
                    |      type: object
                    |      properties:
                    |        ${'$'}ref:
                    |          type: string
                    |        owner:
                    |          ${'$'}ref: '#/components/schemas/Owner'
                    |        tags:
                    |          type: array
                    |          items:
                    |            - ${'$'}ref: '#/components/schemas/Owner'
                    |      default:
                    |        ${'$'}ref: '#/nowhere'
                    |      enum:
                    |        - ${'$'}ref: '#/nowhere'
                    |    Owner:
                    |      type: string
                """.trimMargin(),
            ).document()

            document.targets.keys.map { target -> target.pointer.toString() } shouldBe listOf(
                "/components/schemas/Owner",
            )
        }
    }

    context("a multi-format schema") {
        test("recognises every format kapi knows by its media type and version") {
            val formats = mapOf(
                "application/vnd.aai.asyncapi;version=3.0.0" to SchemaFormat.AsyncApi,
                "application/vnd.aai.asyncapi+json;version=2.6.0" to SchemaFormat.AsyncApi,
                "Application/Vnd.Aai.Asyncapi+YAML; version=3.0.0" to SchemaFormat.AsyncApi,
                "application/schema+json;version=draft-07" to SchemaFormat.JsonSchema,
                "application/schema+yaml;version=\"draft-07\"" to SchemaFormat.JsonSchema,
                "application/vnd.oai.openapi;version=3.0.0" to SchemaFormat.OpenApi,
                "application/vnd.oai.openapi+yaml;version=3.0.3" to SchemaFormat.OpenApi,
                "application/vnd.apache.avro;version=1.9.0" to SchemaFormat.Avro,
                "application/vnd.apache.avro+yaml" to SchemaFormat.Avro,
                "application/raml+yaml;version=1.0" to SchemaFormat.Raml,
                "application/vnd.google.protobuf;version=3" to SchemaFormat.Protobuf,
                "application/vnd.google.protobuf" to SchemaFormat.Protobuf,
                "application/schema+json;version=draft-04" to SchemaFormat.Unknown,
                "application/vnd.oai.openapi;version=3.1.0" to SchemaFormat.Unknown,
                "application/vnd.apache.avro;version=2.0" to SchemaFormat.Unknown,
                "application/xml" to SchemaFormat.Unknown,
            )

            formats.forEach { (text, format) -> formatOf(text) shouldBe format }
        }

        test("warns about a format kapi does not know and keeps the schema") {
            val reading = readingOf(
                """
                    |components:
                    |  schemas:
                    |    Order:
                    |      schemaFormat: application/xml
                    |      schema: '<xs:schema/>'
                """.trimMargin(),
            )
            val document = reading.document()

            reading.codes() shouldBe listOf(DiagnosticCodes.SCHEMA_FORMAT_UNKNOWN)
            reading.diagnostics.single().severity shouldBe Severity.Warning
            document.schema("Order").definition shouldBe Value.Text("<xs:schema/>")
        }

        test("needs its schema and resolves the references of a JSON Schema") {
            val reading = readingOf(
                """
                    |components:
                    |  schemas:
                    |    Missing:
                    |      schemaFormat: application/schema+json;version=draft-07
                    |    Linked:
                    |      schemaFormat: application/schema+json;version=draft-07
                    |      schema:
                    |        ${'$'}ref: '#/components/schemas/Plain'
                    |    Avro:
                    |      schemaFormat: application/vnd.apache.avro;version=1.9.0
                    |      schema:
                    |        ${'$'}ref: 'not followed'
                    |    Plain:
                    |      type: object
                """.trimMargin(),
            )

            reading.codes() shouldBe listOf(DiagnosticCodes.FIELD_MISSING)
            reading.diagnostics.single().message shouldBe "A multi-format schema needs the field schema"
        }

        test("is the payload and the headers of a message") {
            val document = readingOf(
                """
                    |components:
                    |  messages:
                    |    Event:
                    |      headers:
                    |        schemaFormat: application/schema+json;version=draft-07
                    |        schema:
                    |          type: object
                    |      payload:
                    |        schemaFormat: application/vnd.apache.avro;version=1.9.0
                    |        schema:
                    |          type: record
                    |          name: Event
                    |          fields: []
                """.trimMargin(),
            ).document()
            val message = document.components
                .shouldNotBeNull()
                .messages
                .getValue("Event")
            val read = message.shouldBeInstanceOf<Referable.Inline<Message>>()

            val headers = read.value.headers.shouldNotBeNull()
            val payload = read.value.payload.shouldNotBeNull()

            headers.shouldBeInstanceOf<Referable.Inline<Schema>>().value.format shouldBe SchemaFormat.JsonSchema
            payload.shouldBeInstanceOf<Referable.Inline<Schema>>().value.format shouldBe SchemaFormat.Avro
            readingOf(
                """
                    |components:
                    |  messages:
                    |    Event:
                    |      payload: {}
                """.trimMargin(),
            ).diagnostics.shouldBeEmpty()
        }
    }
})
