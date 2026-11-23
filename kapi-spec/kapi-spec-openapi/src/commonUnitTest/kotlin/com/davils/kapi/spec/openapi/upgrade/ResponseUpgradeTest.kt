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

import com.davils.kaml.core.node.YamlNode
import com.davils.kaml.core.node.get
import com.davils.kapi.spec.openapi.DiagnosticCodes
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private const val RESPONSES: String = "responses"

private const val OK: String = "description: Ok"

private suspend fun getWith(vararg lines: String, operation: List<String>): Upgraded {
    val indented = operation.map { line -> "      $line" }
    return upgradedOf(lines.asList() + listOf("paths:", "  /pets:", "    get:") + indented)
}

private val Upgraded.get: YamlNode? get() = root["paths"]["/pets"]["get"]

class ResponseUpgradeTest : FunSpec({
    context("the body of a response") {
        test("is given once per type the operation produces") {
            getWith(
                operation = listOf(
                    "produces: [application/json, text/plain]",
                    "responses: {'200': {description: Pets, schema: {type: array}}}",
                ),
            ).get[RESPONSES] shouldBe nodeOf(
                "'200':",
                "  description: Pets",
                "  content: {application/json: {schema: {type: array}}, text/plain: {schema: {type: array}}}",
            )
        }

        test("follows the types the document produces and JSON when nobody names one") {
            getWith(
                "produces: [text/plain]",
                operation = listOf("responses: {'200': {description: Ok, schema: {type: string}}}"),
            ).get[RESPONSES]["200"] shouldBe nodeOf(
                OK,
                "content: {text/plain: {schema: {type: string}}}",
            )
            getWith(operation = listOf("responses: {'200': {description: Ok, schema: {type: file}}}"))
                .get[RESPONSES]["200"] shouldBe
                nodeOf(OK, "content: {application/json: {schema: {type: string, format: binary}}}")
        }

        test("carries the example of its type and adds a type that only an example names") {
            getWith(
                operation = listOf(
                    "responses:",
                    "  '200':",
                    "    description: Ok",
                    "    schema: {type: string}",
                    "    examples: {application/json: one, text/plain: two}",
                ),
            ).get[RESPONSES]["200"] shouldBe nodeOf(
                OK,
                "content:",
                "  application/json: {schema: {type: string}, example: one}",
                "  text/plain: {schema: {type: string}, example: two}",
            )
        }

        test("holds only its example when it has no schema") {
            getWith(
                operation = listOf(
                    "responses: {'404': {description: Missing, examples: {application/json: {message: gone}}}}",
                ),
            ).get[RESPONSES]["404"] shouldBe
                nodeOf("description: Missing", "content: {application/json: {example: {message: gone}}}")
        }

        test("is absent when the response has neither a schema nor an example") {
            getWith(operation = listOf("responses: {'204': {description: Gone}}")).get[RESPONSES]["204"] shouldBe
                nodeOf("description: Gone")
        }
    }

    context("the headers of a response") {
        test("move their type into a schema and write a list in the simple style") {
            getWith(
                operation = listOf(
                    "responses:",
                    "  '200':",
                    "    description: Ok",
                    "    headers:",
                    "      Api-Version: {type: string, description: The version, x-go-name: ApiVersion}",
                    "      X-Ids: {type: array, items: {type: integer}}",
                    "      X-Shared: {${'$'}ref: '#/responses/Shared'}",
                ),
            ).get[RESPONSES]["200"]["headers"] shouldBe nodeOf(
                "Api-Version: {description: The version, x-go-name: ApiVersion, schema: {type: string}}",
                "X-Ids: {schema: {type: array, items: {type: integer}}, style: simple, explode: false}",
                "X-Shared: {${'$'}ref: '#/components/responses/Shared'}",
            )
        }

        test("note a list in another format") {
            getWith(
                operation = listOf(
                    "responses: {'200': {description: Ok, headers: {X-Ids: {type: array, collectionFormat: multi}}}}",
                ),
            ).notes
                .single()
                .code shouldBe DiagnosticCodes.UPGRADE_LOSSY
        }
    }

    context("the responses") {
        test("point into the components when they are references and keep their extensions") {
            val upgrade = getWith(
                "responses: {Missing: {description: Missing, schema: {${'$'}ref: '#/definitions/Error'}}}",
                operation = listOf("responses: {'404': {${'$'}ref: '#/responses/Missing'}, x-kind: list}"),
            )

            upgrade.get[RESPONSES] shouldBe nodeOf(
                "'404': {${'$'}ref: '#/components/responses/Missing'}",
                "x-kind: list",
            )
            upgrade.root["components"][RESPONSES] shouldBe nodeOf(
                "Missing:",
                "  description: Missing",
                "  content: {application/json: {schema: {${'$'}ref: '#/components/schemas/Error'}}}",
            )
        }
    }

    context("the schemes of an operation") {
        test("become its servers on the host of the document") {
            getWith("host: api.example.com", "basePath: /v1", operation = listOf("schemes: [wss]", "responses: {}"))
                .get["servers"] shouldBe nodeOf("- url: wss://api.example.com/v1")
        }

        test("are dropped when the document names no host") {
            val operation = getWith("basePath: /v1", operation = listOf("schemes: [https]", "responses: {}")).get

            operation["servers"].shouldBeNull()
            operation["schemes"].shouldBeNull()
        }
    }
})
