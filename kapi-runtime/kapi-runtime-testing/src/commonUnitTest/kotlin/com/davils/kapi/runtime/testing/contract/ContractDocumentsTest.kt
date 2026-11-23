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

package com.davils.kapi.runtime.testing.contract

import com.davils.kapi.runtime.testing.fixture.documentOf
import com.davils.kapi.runtime.testing.mock.RecordedExchange
import com.davils.kapi.runtime.testing.mock.RecordedRequest
import com.davils.kapi.runtime.testing.mock.RecordedResponse
import com.davils.kapi.spec.core.diagnostic.DocumentUri
import com.davils.kapi.spec.core.source.Source
import com.davils.kapi.spec.openapi.OpenApi
import com.davils.kapi.spec.openapi.Reading
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.Headers
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.io.bytestring.encodeToByteString

private const val HOST: String = "https://api.example.com"

private const val JSON: String = "application/json"

private const val PARAMETER: String = "parameter"

private const val UNRESOLVED: String = "reference-unresolved"

private const val INVALID: String = "parameter-invalid"

private const val EVERYTHING: String = "/everything"

private const val UPLOAD: String = "/upload"

private const val BROKEN_PATH: String = "/broken"

private const val CREATED: Int = 201

private const val ACCEPTED: Int = 202

private const val NO_CONTENT: Int = 204

private val BROKEN: String =
    """
        |openapi: 3.2.0
        |info:
        |  title: Broken
        |  version: 1.0.0
        |paths:
        |  /things:
        |    ${'$'}ref: "other.yaml#/Item"
        |  /broken:
        |    parameters:
        |      - ${'$'}ref: "other.yaml#/Param"
        |    post:
        |      requestBody:
        |        ${'$'}ref: "other.yaml#/Body"
        |      responses:
        |        "200":
        |          ${'$'}ref: "other.yaml#/Response"
        |        "201":
        |          description: Headers
        |          headers:
        |            X-A:
        |              ${'$'}ref: "other.yaml#/Header"
        |          content:
        |            application/json:
        |              ${'$'}ref: "other.yaml#/Media"
        |        "202":
        |          description: Schema
        |          content:
        |            application/json:
        |              schema:
        |                ${'$'}ref: "other.yaml#/Schema"
    """.trimMargin()

private val SHAPES: String =
    """
        |openapi: 3.2.0
        |info:
        |  title: Shapes
        |  version: 1.0.0
        |servers:
        |  - url: https://api.example.com
        |  - url: v5
        |paths:
        |  /everything:
        |    get:
        |      responses:
        |        "200":
        |          description: Everything
        |          content:
        |            application/xml: {}
        |            application/json:
        |              schema: { ${'$'}ref: "#/components/schemas/Everything" }
        |  /upload:
        |    put:
        |      requestBody:
        |        content:
        |          application/xml: {}
        |          image/*: {}
        |          "*/*": {}
        |      responses:
        |        "204":
        |          description: Stored
        |  /numbers/{id}:
        |    parameters:
        |      - name: id
        |        in: path
        |        required: true
        |        schema: { ${'$'}ref: "#/components/schemas/Id" }
        |    get:
        |      parameters:
        |        - name: strict
        |          in: query
        |          schema:
        |            ${'$'}ref: "#/components/schemas/Id"
        |            type: integer
        |        - name: words
        |          in: query
        |          schema: { type: string }
        |        - name: list
        |          in: query
        |          schema: { type: array }
        |        - name: point
        |          in: query
        |          explode: false
        |          schema: { type: object }
        |      responses:
        |        "200":
        |          description: Fine
        |components:
        |  schemas:
        |    Id: { ${'$'}ref: "#/components/schemas/Count" }
        |    Count: { type: integer, minimum: 1 }
        |    Everything:
        |      ${'$'}id: https://example.com/everything
        |      ${'$'}comment: every keyword
        |      title: Everything
        |      description: Every keyword at once
        |      type: object
        |      required: [name]
        |      minProperties: 1
        |      maxProperties: 20
        |      dependentRequired: { name: [size] }
        |      dependentSchemas: { size: { required: [name] } }
        |      propertyNames: { minLength: 1 }
        |      patternProperties: { "^x-": { type: string } }
        |      additionalProperties: true
        |      unevaluatedProperties: true
        |      ${'$'}defs:
        |        Small: { type: integer, maximum: 10, exclusiveMinimum: 0 }
        |      properties:
        |        name: { type: string, minLength: 1, maxLength: 10, pattern: "^[a-z]+$", format: hostname, deprecated: true }
        |        size: { ${'$'}ref: "#/components/schemas/Everything/${'$'}defs/Small" }
        |        ratio: { type: number, multipleOf: 0.5, readOnly: true, default: 1.5, examples: [0.5] }
        |        kind: { enum: [cat, dog, null, 1, true, { a: [1] }] }
        |        fixed: { const: fixed, writeOnly: true }
        |        tags:
        |          type: array
        |          minItems: 0
        |          maxItems: 5
        |          uniqueItems: true
        |          prefixItems: [{ type: string }]
        |          items: { type: string }
        |          contains: { const: pet }
        |          minContains: 0
        |          maxContains: 3
        |          unevaluatedItems: true
        |        blob: { type: string, contentEncoding: base64, contentMediaType: image/png, contentSchema: { type: string } }
        |        choice:
        |          allOf: [{ type: [string, integer] }]
        |          anyOf: [{ type: string }, { type: integer }]
        |          oneOf: [{ type: string }, { type: integer }]
        |          not: { type: boolean }
        |          if: { type: string }
        |          then: { minLength: 1 }
        |          else: { minimum: 0 }
        |        anything: true
        |        nothing: false
    """.trimMargin()

private fun exchange(
    method: String,
    path: String,
    request: Pair<String?, String> = null to "",
    response: Pair<String?, String> = null to "",
    status: Int = 200,
): RecordedExchange = RecordedExchange(
    request = RecordedRequest(
        method = method,
        url = Url(HOST + path),
        headers = headersOf(request),
        body = request.second.encodeToByteString(),
    ),
    response = RecordedResponse(
        status = status,
        headers = headersOf(response),
        body = response.second.encodeToByteString(),
    ),
)

private fun headersOf(content: Pair<String?, String>): Headers =
    content.first?.let { type -> headersOf(name = "Content-Type", value = type) } ?: Headers.Empty

private suspend fun findingsOf(text: String, vararg exchanges: RecordedExchange): List<Pair<String, String>> {
    val report = contractCheck(documentOf(text)).check(exchanges.toList())
    return report.findings.map { finding -> finding.subject to finding.code.value.substringAfterLast('.') }
}

class ContractDocumentsTest : FunSpec({
    context("a document whose references do not resolve") {
        test("reports every reference the check needs") {
            val reading = OpenApi.reader().read(Source.Text(BROKEN, DocumentUri("memory:/api/broken.yaml")))
            val partial = reading.shouldBeInstanceOf<Reading.Invalid>().partial
            val check = contractCheck(partial)
            val exchanges = listOf(
                exchange(method = "GET", path = "/things"),
                exchange(method = "POST", path = BROKEN_PATH, request = JSON to "{}"),
                exchange(method = "POST", path = BROKEN_PATH, status = CREATED, response = JSON to "{}"),
                exchange(method = "POST", path = BROKEN_PATH, status = ACCEPTED, response = JSON to "{}"),
            )

            val report = check.check(exchanges)

            report.findings.map { finding ->
                finding.subject to finding.code.value.substringAfterLast(
                    '.',
                )
            } shouldBe listOf(
                "method" to "method-undeclared",
                PARAMETER to UNRESOLVED,
                "body" to UNRESOLVED,
                "status 200" to UNRESOLVED,
                PARAMETER to UNRESOLVED,
                "body" to UNRESOLVED,
                "header X-A" to UNRESOLVED,
                "body" to UNRESOLVED,
                PARAMETER to UNRESOLVED,
                "body" to UNRESOLVED,
                "body" to "schema-invalid",
            )
        }
    }

    context("a document without paths") {
        test("knows no operation") {
            val text = listOf(
                "openapi: 3.1.0",
                "info:",
                "  title: Empty",
                "  version: 1.0.0",
                "components:",
                "  schemas:",
                "    A: { type: string }",
            ).joinToString(separator = "\n")

            findingsOf(text, exchange(method = "GET", path = "/")) shouldBe listOf("path" to "operation-unknown")
        }
    }

    context("schemas with every keyword") {
        test("validate what fits and report what does not") {
            val fits = """{"name":"rex","size":3,"ratio":1.5,"kind":"cat","fixed":"fixed","tags":["a","pet"],""" +
                """"blob":"eA==","choice":"x","anything":1,"x-y":"z"}"""
            val breaks = """{"name":"Rex","size":10,"ratio":0.3,"kind":"cow","fixed":"other","tags":["a","a"],""" +
                """"choice":true,"nothing":1,"x-y":2}"""

            val findings = findingsOf(
                SHAPES,
                exchange(method = "GET", path = EVERYTHING, response = JSON to fits),
                exchange(method = "GET", path = EVERYTHING, response = "application/xml" to "<a/>"),
                exchange(method = "GET", path = EVERYTHING, response = JSON to breaks),
            )

            findings.distinct() shouldBe listOf("body" to "body-invalid")
        }
    }

    context("media types") {
        test("match exactly, by their type and by any type") {
            val findings = findingsOf(
                SHAPES,
                exchange(method = "PUT", path = UPLOAD, request = "image/png" to "png", status = NO_CONTENT),
                exchange(method = "PUT", path = UPLOAD, request = "text/plain" to "text", status = NO_CONTENT),
                exchange(method = "PUT", path = UPLOAD, request = "application/xml" to "<a/>", status = NO_CONTENT),
            )

            findings shouldBe emptyList()
        }
    }

    context("parameters whose schemas refer to others") {
        test("are converted by the types the references lead to") {
            val findings = findingsOf(
                SHAPES,
                exchange(method = "GET", path = "/numbers/3?strict=2&words=123&list=a,b&point=x,1,y"),
                exchange(method = "GET", path = "/numbers/0?strict=x&words=true"),
            )

            findings shouldBe listOf(
                "path parameter id" to INVALID,
                "query parameter strict" to INVALID,
                "query parameter strict" to INVALID,
            )
        }
    }
})
