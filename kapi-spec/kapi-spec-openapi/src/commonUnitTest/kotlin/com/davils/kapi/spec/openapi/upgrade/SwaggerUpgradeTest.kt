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

package com.davils.kapi.spec.openapi.upgrade

import com.davils.kaml.core.node.get
import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.spec.openapi.DiagnosticCodes
import com.davils.kapi.spec.openapi.Reading
import com.davils.kapi.spec.openapi.Version
import com.davils.kapi.spec.openapi.fixture.codes
import com.davils.kapi.spec.openapi.fixture.document
import com.davils.kapi.spec.openapi.fixture.reading
import com.davils.kapi.spec.openapi.upgrade.fixture.SWAGGER
import com.davils.kapi.spec.openapi.upgrade.fixture.nodeOf
import com.davils.kapi.spec.openapi.upgrade.fixture.nodeOfLines
import com.davils.kapi.spec.openapi.upgrade.fixture.upgraded
import com.davils.kapi.spec.openapi.upgrade.fixture.upgradedOf
import com.davils.kore.core.validation.violation.Severity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val OPENAPI: String = "openapi"

private const val SERVERS: String = "servers"

private const val UPGRADED: String = "3.1.1"

private const val PET: String = "{${'$'}ref: '#/definitions/Pet'}"

private val info: List<String> = listOf("info:", "  title: Petstore", "  version: 1.0.0")

private val complete: List<String> = listOf(
    "basePath: /v1",
    "paths:",
    "  /pets/{id}:",
    "    parameters: [{name: id, in: path, required: true, type: integer, format: int64}]",
    "    put:",
    "      parameters:",
    "        - {name: pet, in: body, required: true, schema: $PET}",
    "      responses:",
    "        '200': {description: The pet, schema: $PET}",
    "        '404': {${'$'}ref: '#/responses/Missing'}",
    "definitions:",
    "  Pet:",
    "    type: object",
    "    properties:",
    "      name: {type: string, x-nullable: true}",
    "      owner: {${'$'}ref: '#/definitions/Pet', x-nullable: true, description: The owner}",
    "responses:",
    "  Missing: {description: Not found}",
)

class SwaggerUpgradeTest : FunSpec({
    context("the version") {
        test("becomes OpenAPI 3.1.1 in place of Swagger 2.0") {
            val upgrade = upgradedOf(info)

            upgrade.root[OPENAPI] shouldBe nodeOf(UPGRADED)
            upgrade.root["swagger"].shouldBeNull()
            upgrade.root["info"] shouldBe nodeOfLines(info)["info"]
        }

        test("is noted as a warning at the field swagger") {
            val note = upgradedOf(info).diagnostics.single()

            note.code shouldBe DiagnosticCodes.VERSION_UPGRADED
            note.severity shouldBe Severity.Warning
            note.location?.pointer shouldBe JsonPointer.of("swagger")
            note.location?.position?.line shouldBe 0
            note.snippet.shouldNotBeNull()
        }

        test("replaces an openapi field the Swagger document declared as well") {
            upgradedOf(listOf("openapi: 3.0.0") + info).root[OPENAPI] shouldBe nodeOf(UPGRADED)
        }

        test("leaves a document of another Swagger version alone") {
            val reading = reading(listOf("swagger: '1.2'") + info)

            reading.codes() shouldBe listOf(DiagnosticCodes.VERSION_SWAGGER)
        }
    }

    context("the servers") {
        test("join every scheme with the host and the base path") {
            val servers = upgraded("host: api.example.com", "basePath: /v2", "schemes: [https, http]").root[SERVERS]

            servers shouldBe nodeOf("- url: https://api.example.com/v2", "- url: http://api.example.com/v2")
        }

        test("keep the host without a scheme as a reference relative to the scheme of the document") {
            upgraded("host: api.example.com").root[SERVERS] shouldBe nodeOf("- url: //api.example.com")
        }

        test("name the base path alone when the document names no host") {
            upgraded("basePath: /v1.56", "schemes: [http]").root[SERVERS] shouldBe nodeOf("- url: /v1.56")
        }

        test("are left out when the document names neither a host nor a base path") {
            upgraded("schemes: [https]").root[SERVERS].shouldBeNull()
        }
    }

    context("the root") {
        test("keeps tags, security, external documentation, extensions and fields it does not know") {
            val lines = listOf(
                "tags: [{name: pets}]",
                "security: [{key: []}]",
                "externalDocs: {url: 'https://docs.example.com'}",
                "x-logo: {url: logo.png}",
                "unknown: 1",
            )

            val root = upgradedOf(lines).root

            listOf("tags", "security", "externalDocs", "x-logo", "unknown").forEach { key ->
                root[key] shouldBe nodeOfLines(lines)[key]
            }
        }

        test("drops the fields that only Swagger knows") {
            val root = upgraded("consumes: [text/plain]", "produces: [text/plain]", "definitions: {}").root

            val dropped = listOf("consumes", "produces", "definitions", "host", "basePath")

            dropped.forEach { key -> root[key].shouldBeNull() }
        }

        test("declares no components when the document has nothing to keep in them") {
            upgradedOf(info).root["components"].shouldBeNull()
        }
    }

    context("a reading") {
        test("reads a complete Swagger document without a finding beyond the upgrade") {
            val reading = reading(listOf(SWAGGER) + info + complete)

            reading.shouldBeInstanceOf<Reading.Read>()
            reading.codes() shouldBe listOf(DiagnosticCodes.VERSION_UPGRADED)
            reading.document().version shouldBe Version.parseOrNull(UPGRADED)
        }
    }
})
