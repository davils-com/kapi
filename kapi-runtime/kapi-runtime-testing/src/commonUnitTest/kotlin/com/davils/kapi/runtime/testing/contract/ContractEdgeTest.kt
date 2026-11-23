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

import com.davils.kaml.document.pointer.JsonPointer
import com.davils.kapi.runtime.testing.fixture.PETSTORE
import com.davils.kapi.runtime.testing.fixture.documentOf
import com.davils.kapi.runtime.testing.mock.RecordedExchange
import com.davils.kapi.runtime.testing.mock.RecordedRequest
import com.davils.kapi.runtime.testing.mock.RecordedResponse
import com.davils.kore.core.validation.report.ValidationException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.ktor.http.Headers
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString

private const val HOST: String = "https://api.example.com"

private const val JSON: String = "application/json"

private const val CONTENT_TYPE: String = "Content-Type"

private const val UNKNOWN: String = "operation-unknown"

private const val BODY_INVALID: String = "body-invalid"

private const val OPERATION: String = "GET /x"

private const val PETS: String = "/v1/pets"

private const val WEIGHTS: String = "/v3/weights"

private const val RECORD: String = """{"id":1,"name":"Rex"}"""

private val LEGACY: String =
    """
        |openapi: 3.0.3
        |info:
        |  title: Legacy
        |  version: 1.0.0
        |servers:
        |  - url: https://{host}/v2
        |    variables:
        |      host:
        |        default: api.example.com
        |  - url: /v3
        |  - url: https://api.example.com/{version}
        |    variables:
        |      version:
        |        default: v4
        |paths:
        |  /weights:
        |    post:
        |      requestBody:
        |        content:
        |          application/json:
        |            schema:
        |              type: object
        |              properties:
        |                weight: { type: number, minimum: 0, exclusiveMinimum: true, nullable: true, example: 3 }
        |      responses:
        |        "200":
        |          description: Fine
    """.trimMargin()

private val MODERN: String =
    """
        |openapi: 3.2.0
        |info:
        |  title: Modern
        |  version: 1.0.0
        |jsonSchemaDialect: https://json-schema.org/draft/2020-12/schema
        |paths:
        |  /shared:
        |    ${'$'}ref: "#/components/pathItems/Shared"
        |  /copies:
        |    additionalOperations:
        |      COPY:
        |        responses:
        |          "200":
        |            description: Copied
        |  /odd:
        |    get:
        |      responses:
        |        "200":
        |          description: Odd
        |          content:
        |            application/json:
        |              schema: { ${'$'}ref: "#/components/schemas/Odd" }
        |  /deep:
        |    get:
        |      responses:
        |        "200":
        |          description: Deep
        |          content:
        |            application/json:
        |              schema:
        |                type: array
        |                items: { type: array, items: { type: array, items: { type: integer } } }
        |components:
        |  schemas:
        |    Odd:
        |      ${'$'}schema: "https://example.com/unknown"
        |      type: string
        |  pathItems:
        |    Shared:
        |      get:
        |        responses:
        |          "200":
        |            description: Shared
    """.trimMargin()

private fun exchange(
    method: String,
    path: String,
    request: Pair<String?, String> = null to "",
    status: Int = 200,
    response: Pair<String?, String> = null to "",
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
    content.first?.let { type -> headersOf(name = CONTENT_TYPE, value = type) } ?: Headers.Empty

private suspend fun codesOf(text: String, vararg exchanges: RecordedExchange): List<String> {
    val report = contractCheck(documentOf(text)).check(exchanges.toList())
    return report.codes.map { code -> code.value.substringAfterLast('.') }
}

class ContractEdgeTest : FunSpec({
    context("an OpenAPI 3.0 document") {
        test("is checked in the dialect of OpenAPI 3.0, below the paths of its servers that name no variable") {
            val codes = codesOf(
                LEGACY,
                exchange(method = "POST", path = WEIGHTS, request = JSON to """{"weight":null}"""),
                exchange(method = "POST", path = WEIGHTS, request = JSON to """{"weight":0}"""),
                exchange(method = "POST", path = "/v2/weights", request = JSON to """{"weight":1}"""),
                exchange(method = "POST", path = "/v3/weights"),
                exchange(method = "POST", path = "/v4/weights"),
                exchange(method = "POST", path = "/v3"),
                exchange(method = "POST", path = "/v3x/weights"),
            )

            codes shouldBe listOf(BODY_INVALID, UNKNOWN, UNKNOWN, UNKNOWN)
        }
    }

    context("an OpenAPI 3.2 document") {
        test("follows referenced path items and additional operations") {
            val codes = codesOf(
                MODERN,
                exchange(method = "GET", path = "/shared"),
                exchange(method = "COPY", path = "/copies"),
                exchange(method = "MOVE", path = "/copies"),
            )

            codes shouldBe listOf("method-undeclared")
        }

        test("reports a schema that does not compile") {
            val odd = exchange(method = "GET", path = "/odd", response = JSON to "\"text\"")

            codesOf(MODERN, odd) shouldBe listOf("schema-invalid")
        }

        test("reports a validation that stops at a limit") {
            val deep = exchange(method = "GET", path = "/deep", response = JSON to "[[[1]]]")
            val check = contractCheck(documentOf(MODERN)) { limits { maximumEvaluationDepth = 2 } }

            check.check(deep).codes.map { code -> code.value } shouldBe listOf(ContractCodes.VALIDATION_REFUSED.value)
        }
    }

    context("bodies and headers") {
        test("are reported when missing, untyped, invalid or malformed") {
            val codes = codesOf(
                PETSTORE,
                exchange(method = "POST", path = PETS, status = 201),
                exchange(method = "POST", path = PETS, request = null to "{}", status = 201),
                exchange(
                    method = "GET",
                    path = "/v1/pets/records",
                    response = "application/x-ndjson" to """{"id":0}""" + "\n{\n",
                ),
                exchange(method = "GET", path = "/v1/pets/1/events", response = "text/event-stream" to "data: x\n\n"),
                exchange(method = "GET", path = "/v1/pets/1/events", response = "no type" to "data: x\n\n"),
                exchange(method = "DELETE", path = "/v1/pets/1", request = JSON to "{}", status = 204),
                exchange(method = "POST", path = PETS, request = JSON to RECORD, status = 500),
            )

            codes shouldBe listOf(
                "body-missing",
                "content-type-missing",
                BODY_INVALID,
                BODY_INVALID,
                "body-malformed",
                "content-type-missing",
                "body-undeclared",
                "status-undocumented",
            )
        }

        test("check the headers of a response against their schemas") {
            val request = RecordedRequest(
                method = "GET",
                url = Url("$HOST/v1/pets"),
                headers = headersOf(name = "Accept", value = JSON),
                body = ByteString(),
            )
            val response = RecordedResponse(
                status = 200,
                headers = Headers.build {
                    append(name = CONTENT_TYPE, value = JSON)
                    append(name = "X-Total", value = "many")
                },
                body = "[]".encodeToByteString(),
            )
            val report = contractCheck(documentOf(PETSTORE)).check(RecordedExchange(request, response))

            report.codes shouldBe listOf(ContractCodes.HEADER_INVALID)
            report.findings.single().subject shouldBe "header X-Total"
        }
    }

    context("the configuration of a check") {
        test("refuses a base path that is no path") {
            val document = documentOf(PETSTORE)

            shouldThrow<ValidationException> { contractCheck(document) { basePath = "v1/" } }
            contractCheck(document) { basePath = "/v1" }.toString() shouldBe
                "ContractCheck(basePath=\"/v1\", isFormatAsserted=false)"
        }

        test("asserts formats on request") {
            val check = contractCheck(documentOf(PETSTORE)) { isFormatAsserted = true }

            check.isFormatAsserted shouldBe true
            check.basePath shouldBe ""
            check.document shouldBe documentOf(PETSTORE)
        }
    }

    context("findings and reports") {
        test("are values that render their text escaped") {
            val finding = ContractFinding(
                code = ContractCodes.BODY_INVALID,
                message = "bad\nline",
                exchange = 1,
                side = ExchangeSide.Response,
                operation = OPERATION,
                subject = "body",
                instance = JsonPointer.of("a"),
            )
            val same = ContractFinding(
                code = ContractCodes.BODY_INVALID,
                message = "bad\nline",
                exchange = 1,
                side = ExchangeSide.Response,
                operation = OPERATION,
                subject = "body",
                instance = JsonPointer.of("a"),
            )
            val report = ContractReport(listOf(finding), exchanges = 2)

            finding shouldBe same
            finding.hashCode() shouldBe same.hashCode()
            finding shouldNotBe ContractFinding(
                code = ContractCodes.BODY_INVALID,
                message = "other",
                exchange = 1,
                side = ExchangeSide.Response,
                operation = OPERATION,
                subject = "body",
            )
            finding.render() shouldContain "#1 response GET /x, body at /a: [kapi.testing.contract.body-invalid]"
            finding.toString() shouldContain "ContractFinding(#1"
            report shouldBe ContractReport(listOf(same), exchanges = 2)
            report.hashCode() shouldBe ContractReport(listOf(same), exchanges = 2).hashCode()
            report shouldNotBe ContractReport(emptyList(), exchanges = 2)
            report.toString() shouldBe "ContractReport(exchanges=2, findings=1)"
            report.equals("report") shouldBe false
            finding.equals("finding") shouldBe false
            ContractReport(emptyList(), exchanges = 0).assertConforms()
        }
    }
})
