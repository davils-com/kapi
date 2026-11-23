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
import com.davils.kapi.spec.asyncapi.fixture.codes
import com.davils.kapi.spec.asyncapi.fixture.parts
import com.davils.kapi.spec.asyncapi.fixture.reading
import com.davils.kapi.spec.asyncapi.fixture.readingOf
import com.davils.kapi.spec.asyncapi.model.schema.Schema
import com.davils.kapi.spec.asyncapi.model.schema.SchemaFormat
import com.davils.kapi.spec.core.model.Referable
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val MISSING: Int = 4

private const val VERSION: String = "asyncapi: 3.0.0"

private const val COMPONENTS: String = "components:"

class EdgeReadingTest : FunSpec({
    context("a field") {
        test("that must be a string and is an object is refused and counts as missing") {
            val reading = reading(listOf(VERSION, "info:", "  title: {}", "  version: 1.0.0"))

            reading.codes() shouldContain DiagnosticCodes.FIELD_TYPE
            reading.codes() shouldContain DiagnosticCodes.FIELD_MISSING
        }

        test("that must be a list or a map of strings refuses other values") {
            val reading = readingOf(
                """
                    |components:
                    |  serverVariables:
                    |    region:
                    |      enum: [1]
                    |  securitySchemes:
                    |    oauth:
                    |      type: oauth2
                    |      flows:
                    |        implicit:
                    |          authorizationUrl: https://example.com/authorize
                    |          availableScopes:
                    |            read: 1
                """.trimMargin(),
            )

            reading.codes() shouldBe listOf(DiagnosticCodes.FIELD_TYPE, DiagnosticCodes.FIELD_TYPE)
        }

        test("that is required is reported for every object that misses it") {
            val reading = readingOf(
                """
                    |components:
                    |  tags:
                    |    tag:
                    |      description: No name
                    |  externalDocs:
                    |    docs:
                    |      description: No address
                    |  correlationIds:
                    |    id:
                    |      description: No location
                    |  replyAddresses:
                    |    address:
                    |      description: No location
                """.trimMargin(),
            )

            val missing = reading.codes().filter { code -> code == DiagnosticCodes.FIELD_MISSING }
            val expressions = reading.codes().filter { code -> code == DiagnosticCodes.RUNTIME_EXPRESSION }

            missing.size shouldBe MISSING
            expressions.size shouldBe 2
        }

        test("of a license and of the document are required") {
            reading(listOf(VERSION)).codes() shouldBe listOf(DiagnosticCodes.FIELD_MISSING)
            val license = listOf(VERSION, "info:", "  title: A", "  version: '1'", "  license: {url: x}")

            reading(license).codes() shouldBe listOf(DiagnosticCodes.FIELD_MISSING)
        }
    }

    context("an object") {
        test("written as something else is refused") {
            readingOf(
                listOf(COMPONENTS, "  servers:", "    production: []"),
            ).codes() shouldBe listOf(DiagnosticCodes.FIELD_TYPE)
        }

        test("of a multi-format schema with a format that is no string keeps the format of AsyncAPI") {
            val reading = readingOf(
                listOf(COMPONENTS, "  schemas:", "    Any:", "      schemaFormat: 1", "      schema: {}"),
            )
            val any = reading.parts().schemas.getValue("Any")
            val schema = any.shouldBeInstanceOf<Referable.Inline<Schema>>()

            reading.codes() shouldBe listOf(DiagnosticCodes.FIELD_MISSING, DiagnosticCodes.FIELD_TYPE)
            schema.value.format shouldBe SchemaFormat.AsyncApi
        }

        test("of a schema takes a reference that is no string as a keyword") {
            val reading = readingOf(
                """
                    |components:
                    |  schemas:
                    |    Pet:
                    |      type: object
                    |      properties:
                    |        name:
                    |          ${'$'}ref: 1
                """.trimMargin(),
            )

            reading.diagnostics.shouldBeEmpty()
        }
    }

    context("a reference") {
        test("that is an object is no reference") {
            readingOf(listOf(COMPONENTS, "  messages:", "    Event:", "      \$ref: {}")).codes() shouldBe
                listOf(DiagnosticCodes.REFERENCE_SYNTAX)
        }

        test("to an object that cannot be read resolves to nothing") {
            val reading = readingOf(
                """
                    |servers:
                    |  production:
                    |    host: example.com
                    |    protocol: ws
                    |    security:
                    |      - ${'$'}ref: '#/components/securitySchemes/bad'
                    |components:
                    |  securitySchemes:
                    |    bad:
                    |      type: nope
                """.trimMargin(),
            )

            reading.codes() shouldBe listOf(DiagnosticCodes.SECURITY_TYPE)
        }
    }
})
